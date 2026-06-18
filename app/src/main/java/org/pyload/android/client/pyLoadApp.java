package org.pyload.android.client;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import org.apache.http.conn.ssl.BrowserCompatHostnameVerifier;
import org.pyload.android.client.components.TabHandler;
import org.pyload.android.client.exceptions.WrongLogin;
import org.pyload.android.client.exceptions.WrongServer;
import org.pyload.android.client.module.AllTrustManager;
import org.pyload.android.client.module.GuiTask;
import org.pyload.android.client.module.TaskQueue;
import org.pyload.android.openapi.ApiClient;
import org.pyload.android.openapi.api.PyLoadRestApi;
import org.pyload.android.openapi.auth.ApiKeyAuth;
import org.pyload.android.openapi.models.ServerStatus;

import javax.net.ssl.*;

import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;

public class pyLoadApp extends Application {

	private PyLoadRestApi client;

	// set by main activity
	private TaskQueue taskQueue;
	private Throwable lastException;
	public SharedPreferences prefs;
	public ConnectivityManager cm;

	private pyLoad main;
	
	private boolean captchaNotificationShown;

	private static final String[] clientVersion = {"0.5"};

	public void init(pyLoad main) {
		this.main = main;

		HashMap<Throwable, Runnable> exceptionMap = new HashMap<Throwable, Runnable>();
		exceptionMap.put(new WrongLogin(), handleException);
		exceptionMap.put(new WrongServer(), handleException);
		exceptionMap.put(new RuntimeException(), handleException);

        taskQueue = new TaskQueue(this, new Handler(), exceptionMap);
        startTaskQueue();
	}

	public String verboseBool(boolean state) {
		if (state)
			return getString(R.string.on);
		else
			return getString(R.string.off);
	}

	private boolean checkAuth() {
		// replace protocol, some user also enter it
		String host = prefs.getString("host", "10.0.2.2").replaceFirst("^[a-zA-z]+://", "");
		int port = Integer.parseInt(prefs.getString("port", "8000"));
		String apiKey = prefs.getString("api_key", "");

        ApiClient apiClient = new ApiClient();
		apiClient.getOkBuilder()
				.connectTimeout(8, TimeUnit.SECONDS)
				.readTimeout(8, TimeUnit.SECONDS);

		boolean useSsl = prefs.getBoolean("ssl", false);
		if (useSsl) {
			boolean validateSsl = prefs.getBoolean("ssl_validate", true);
			TrustManager[] trustManagers;
			try {
				if (validateSsl) {
					TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
					tmf.init((KeyStore) null);
					trustManagers = tmf.getTrustManagers();
				} else {
					trustManagers = new TrustManager[1];
					trustManagers[0] = new AllTrustManager();
				}
				SSLContext sslContext = SSLContext.getInstance("TLS");
				sslContext.init(null, trustManagers, new SecureRandom());
				Log.d("pyLoad", "SSL Context created");

				apiClient.getOkBuilder().sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustManagers[0]);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

			if (validateSsl) {
				apiClient.getOkBuilder().hostnameVerifier(new BrowserCompatHostnameVerifier());
			} else {
				apiClient.getOkBuilder().hostnameVerifier((hostname, session) -> true);
			}
		}

		String protocol = useSsl ? "https://" : "http://";
		String baseUrl = protocol + host + ":" + port + "/";

		apiClient.createDefaultAdapter();
		Retrofit.Builder retrofit = apiClient.getAdapterBuilder().baseUrl(baseUrl);
		retrofit.converterFactories().remove(0);
		apiClient.setAdapterBuilder(retrofit);

		boolean authSuccessful;
		try {
			ApiKeyAuth apiKeyAuth = new ApiKeyAuth("header", "X-API-Key");
			apiKeyAuth.setApiKey(apiKey);
			apiClient.addAuthorization("ApiKeyAuth", apiKeyAuth);

			PyLoadRestApi pyLoadRestApi = apiClient.createService(PyLoadRestApi.class);

			Response<ServerStatus> serverStatus = pyLoadRestApi.apiStatusServerGet().execute();
			authSuccessful = serverStatus.isSuccessful();
			if (authSuccessful) {
				client = pyLoadRestApi;
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		return authSuccessful;
	}

	public PyLoadRestApi getClient() throws WrongLogin, WrongServer {

		if (client == null) {
			Log.d("pyLoad", "Creating new Client");
			boolean authSuccessful = checkAuth();
			if (!authSuccessful) {
				client = null;
				throw new WrongLogin();
			}

            String server = executeNetworkCall(client.apiGetServerVersionGet());
            boolean match = false;
			
			for (String version : clientVersion)
				if (server.startsWith(version)) {
					match = true;
					break;
				}
			
			if (!match)
				throw new WrongServer();

		}
		return client;
	}

	public <T> T executeNetworkCall(Call<T> call) throws RuntimeException {
		Response<T> response;
		try {
			response = call.execute();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		if (response.isSuccessful()) {
			return response.body();
		} else {
			String errorMsg = "HTTP error: " + response.code() + " - " + response.message();
			throw new RuntimeException(errorMsg);
		}
	}

	public void addTask(GuiTask task)
	{
		taskQueue.addTask(task);
	}

	public void startTaskQueue() {
		taskQueue.start();
	}

	final public Runnable handleException = new Runnable() {
		public void run() {
			onException();
		}
	};

	public void onException() {
		client = null;
        // The task queue will log an error with exception

		String errorMessage;
		if (lastException instanceof WrongLogin)
			errorMessage = getString(R.string.bad_login);
		else if (lastException instanceof WrongServer)
			errorMessage = String.format(getString(R.string.old_server), clientVersion[clientVersion.length - 1]);
		else if (lastException instanceof RuntimeException) {
			Throwable tr = findException(lastException);
			if (tr instanceof SSLHandshakeException)
				errorMessage = getString(R.string.certificate_error);
			else if (tr instanceof SocketTimeoutException)
				errorMessage = getString(R.string.connect_timeout);
			else if (tr instanceof ConnectException)
				errorMessage = getString(R.string.connect_error);
			else if (tr instanceof SocketException)
				errorMessage = getString(R.string.socket_error);
			else
				errorMessage = getString(R.string.no_connection) + " " + tr.getMessage();
		} else if (lastException != null)
			errorMessage = lastException.getMessage();
		else
			errorMessage = getString(R.string.error);

		Toast t = Toast.makeText(this, errorMessage, Toast.LENGTH_LONG);
		t.show();

		setProgress(false);
	}

	/**
	 * Retrieves first root exception on stack of several RuntimeException.
	 * @return the first exception not a RuntimeException or the last RuntimeException
	 */
	private Throwable findException(Throwable e) {
		// will not terminate when cycles occur, hopefully nobody cycle exception causes
		while (e instanceof RuntimeException) {
			if (e.getCause() == null) break;
			if (e.getCause() == e) break; // just to avoid loop
			e = e.getCause();
		}

		return e;
	}

	final public Runnable handleSuccess = new Runnable() {
		@Override
		public void run() {
			onSuccess();
		}
	};

	public void onSuccess() {
		Toast t = Toast.makeText(this, R.string.success, Toast.LENGTH_SHORT);
		t.show();

		refreshTab();
	}

	public void refreshTab() {
		Fragment frag = main.getCurrentFragment();

		Log.d("pyLoad", "Refreshing Tab: " + frag);

		if (frag != null)
			((TabHandler) frag).onSelected();
	}

	public boolean isCurrentTab(int pos) {
		return main.getCurrentTab() == pos;
	}

	public pyLoad getMain() {
		return main;
	}

	public boolean hasConnection() {
		NetworkInfo info = cm.getActiveNetworkInfo();
		// TODO investigate network states, info etc
		return info != null;
	}

	public void clearTasks() {
		taskQueue.clear();
	}

	public void setLastException(Throwable t) {
		lastException = t;
	}

	public void resetClient() {
		Log.d("pyLoad", "Client resetted");
		client = null;
	}

    /**
     * Enables and disables the progress indicator.
     *
     * The indicator depends on the user's Android version.
     * pre-actionBar devices: Window.FEATURE_INDETERMINATE_PROGRESS
     * actionBar devices: set refreshAction's view to a progress wheel (Gmail like)
     *
     * @param state
     */
	public void setProgress(boolean state) {
        if (isActionBarAvailable()) {
            setIndeterminateProgress(main.getRefreshItem(), state);
        } else {
            setIndeterminateProgress(state);
        }
	}

    private void setIndeterminateProgress(boolean state) {
        main.setProgressBarIndeterminateVisibility(state);
    }

    private void setIndeterminateProgress(MenuItem item, boolean state) {
        if (item == null) {
            return;
        }

        if (state) {
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View progress = inflater.inflate(R.layout.progress_wheel, null);

            main.getRefreshItem().setActionView(progress);

        } else {
            item.setActionView(null);
        }
    }

    public static boolean isActionBarAvailable() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }
    
    public void setCaptchaNotificationShown(boolean val)
    {
    	captchaNotificationShown = val;
    }
    
    public boolean getCaptchaNotificationShown()
    {
    	return captchaNotificationShown;
    }

}
