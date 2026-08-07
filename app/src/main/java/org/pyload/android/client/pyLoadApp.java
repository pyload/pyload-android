package org.pyload.android.client;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.snackbar.Snackbar;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import org.pyload.android.client.components.TabHandler;
import org.pyload.android.client.exceptions.WrongLogin;
import org.pyload.android.client.exceptions.WrongPathPrefix;
import org.pyload.android.client.exceptions.WrongServer;
import org.pyload.android.client.module.AllTrustManager;
import org.pyload.android.client.module.GuiTask;
import org.pyload.android.client.module.TaskQueue;
import org.pyload.android.client.services.ClickNLoadService;
import org.pyload.android.openapi.ApiClient;
import org.pyload.android.openapi.api.PyLoadRestApi;
import org.pyload.android.openapi.auth.ApiKeyAuth;
import org.pyload.android.openapi.model.ServerStatus;

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

	public static final String CHANNEL_ID = "pyload_channel";

	private PyLoadRestApi client;

	// set by main activity
	private TaskQueue taskQueue;
	private Throwable lastException;
	public SharedPreferences prefs;
	public ConnectivityManager cm;

	private pyLoad main;
	private Activity currentActivity;

	private boolean captchaNotificationShown;
	private int activityCount = 0;

	private int consecutiveConnectionErrors = 0;
	private boolean pollingPaused = false;
	private Snackbar persistentSnackbar = null;

	private static final String[] clientVersion = {"0.5"};

	@Override
	public void onCreate() {
		super.onCreate();

		prefs = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
		String theme = prefs.getString("theme", "system");
		applyTheme(theme);

		cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

		createNotificationChannel();

		HashMap<Throwable, Runnable> exceptionMap = new HashMap<Throwable, Runnable>();
		exceptionMap.put(new WrongLogin(), handleException);
		exceptionMap.put(new WrongPathPrefix(), handleException);
		exceptionMap.put(new WrongServer(), handleException);
		exceptionMap.put(new RuntimeException(), handleException);

		taskQueue = new TaskQueue(this, new Handler(Looper.getMainLooper()), exceptionMap);
		startTaskQueue();

		if (prefs.getBoolean("clicknload", false)) {
			Intent intent = new Intent(this, ClickNLoadService.class);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				startForegroundService(intent);
			} else {
				startService(intent);
			}
		}

		registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
			@Override
			public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}

			@Override
			public void onActivityStarted(Activity activity) {
				activityCount++;
			}

			@Override
			public void onActivityResumed(Activity activity) {
				currentActivity = activity;
				if (pollingPaused) {
					showCenteredSnackbar(getString(R.string.polling_paused_error), Snackbar.LENGTH_INDEFINITE);
				}
			}

			@Override
			public void onActivityPaused(Activity activity) {
				if (currentActivity == activity) {
					currentActivity = null;
				}
				if (persistentSnackbar != null) {
					persistentSnackbar.dismiss();
					persistentSnackbar = null;
				}
			}

			@Override
			public void onActivityStopped(Activity activity) {
				activityCount--;
			}

			@Override
			public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

			@Override
			public void onActivityDestroyed(Activity activity) {}
		});
	}

	public static void applyTheme(String theme) {
		switch (theme) {
			case "light":
				AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
				break;
			case "dark":
				AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
				break;
			case "system":
			default:
				AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
				break;
		}
	}

	public void init(pyLoad main) {
		this.main = main;
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
				apiClient.getOkBuilder().hostnameVerifier(HttpsURLConnection.getDefaultHostnameVerifier());
			} else {
				apiClient.getOkBuilder().hostnameVerifier((hostname, session) -> true);
			}
		}

		String protocol = useSsl ? "https://" : "http://";
		String pathPrefix = prefs.getString("path_prefix", "");
		if (!pathPrefix.startsWith("/") && !pathPrefix.isEmpty()) {
			pathPrefix = "/" + pathPrefix;
		}
		if (pathPrefix.endsWith("/")) {
			pathPrefix = pathPrefix.substring(0, pathPrefix.length() - 1);
		}

		String baseUrl = protocol + host + ":" + port + pathPrefix + "/";

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
			} else if (serverStatus.code() == 404) {
				throw new WrongPathPrefix();
			}
		} catch (WrongPathPrefix e) {
			throw e;
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
		else if (lastException instanceof WrongPathPrefix)
			errorMessage = getString(R.string.bad_path);
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

		if (isAppInForeground()) {
			if (pollingPaused) {
				return;
			}
			if (isConnectionError(lastException)) {
				consecutiveConnectionErrors++;
				if (consecutiveConnectionErrors >= 3) {
					pollingPaused = true;
					showCenteredSnackbar(getString(R.string.polling_paused_error), Snackbar.LENGTH_INDEFINITE);
				} else {
					showCenteredSnackbar(errorMessage, Snackbar.LENGTH_LONG);
				}
			} else {
				showCenteredSnackbar(errorMessage, Snackbar.LENGTH_LONG);
			}
		}

		setProgress(false);
	}

	private boolean isConnectionError(Throwable t) {
		Throwable tr = findException(t);
		return tr instanceof SocketTimeoutException ||
				tr instanceof SocketException ||
				tr instanceof SSLHandshakeException;
	}

	private void showCenteredSnackbar(Object message, int length) {
		if (currentActivity == null) {
			if (message instanceof Integer) {
				Toast.makeText(this, (Integer) message, length).show();
			} else {
				Toast.makeText(this, (String) message, length).show();
			}
			return;
		}

		Snackbar snackbar;
		if (message instanceof Integer) {
			snackbar = Snackbar.make(currentActivity.findViewById(android.R.id.content), (Integer) message, length);
		} else {
			snackbar = Snackbar.make(currentActivity.findViewById(android.R.id.content), (String) message, length);
		}

		if (length == Snackbar.LENGTH_INDEFINITE) {
			if (persistentSnackbar != null && persistentSnackbar.isShown()) {
				// Don't recreate if already showing the same indefinite snackbar for this activity
				return;
			}
			persistentSnackbar = snackbar;
		}

		View snackbarView = snackbar.getView();
		TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
		if (textView != null) {
			textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
			textView.setGravity(Gravity.CENTER_HORIZONTAL);
		}
		snackbar.setTextMaxLines(10);
		snackbar.show();
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
		consecutiveConnectionErrors = 0;
		if (pollingPaused) {
			pollingPaused = false;
			if (persistentSnackbar != null) {
				persistentSnackbar.dismiss();
				persistentSnackbar = null;
			}
		}

		if (isAppInForeground()) {
			showCenteredSnackbar(R.string.success, Snackbar.LENGTH_SHORT);
		}

		refreshTab();
	}

	public void refreshTab() {
		if (isPollingPaused()) {
			return;
		}
		Fragment frag = main.getCurrentFragment();

		Log.d("pyLoad", "Refreshing Tab: " + frag);

		if (frag instanceof TabHandler) {
			((TabHandler) frag).refresh();
		}
	}

	public boolean isCurrentTab(int pos) {
		return main.getCurrentTab() == pos;
	}

	public pyLoad getMain() {
		return main;
	}

	public boolean hasConnection() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			Network network = cm.getActiveNetwork();
			if (network == null) return false;
			NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
			return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
		} else {
			@SuppressWarnings("deprecation")
			Network[] networks = cm.getAllNetworks();
			for (Network n : networks) {
				NetworkCapabilities capabilities = cm.getNetworkCapabilities(n);
				if (capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
					return true;
				}
			}
			return false;
		}
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
		consecutiveConnectionErrors = 0;
		pollingPaused = false;
		if (persistentSnackbar != null) {
			persistentSnackbar.dismiss();
			persistentSnackbar = null;
		}
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
		if (main == null) {
			return;
		}
		setIndeterminateProgress(main.getRefreshItem(), state);
	}

    private void setIndeterminateProgress(MenuItem item, boolean state) {
        if (item == null) {
            return;
        }

        if (state) {
            LayoutInflater inflater = LayoutInflater.from(main);
            View progress = inflater.inflate(R.layout.progress_wheel, null);

            main.getRefreshItem().setActionView(progress);

        } else {
            item.setActionView(null);
        }
    }

    public boolean isAppInForeground() {
        return activityCount > 0;
    }

    public void setCaptchaNotificationShown(boolean val)
    {
    	captchaNotificationShown = val;
    }
    
    public boolean getCaptchaNotificationShown()
    {
    	return captchaNotificationShown;
    }

	private void createNotificationChannel() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			CharSequence name = getString(R.string.captcha_notification_channel_name);
			String description = getString(R.string.captcha_notification_channel_description);
			int importance = NotificationManager.IMPORTANCE_HIGH;
			NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
			channel.setDescription(description);
			NotificationManager notificationManager = getSystemService(NotificationManager.class);
			notificationManager.createNotificationChannel(channel);
		}
	}

	public boolean isPollingPaused() {
		return pollingPaused;
	}

}
