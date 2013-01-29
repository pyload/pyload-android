package org.pyload.android.client;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

import javax.net.ssl.*;

import android.annotation.TargetApi;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import org.apache.http.conn.ssl.BrowserCompatHostnameVerifier;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSSLTransportFactory;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.pyload.android.client.components.TabHandler;
import org.pyload.android.client.exceptions.WrongLogin;
import org.pyload.android.client.exceptions.WrongServer;
import org.pyload.android.client.module.AllTrustManager;
import org.pyload.android.client.module.GuiTask;
import org.pyload.android.client.module.TaskQueue;
import org.pyload.thrift.Pyload.Client;

import android.app.Application;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.widget.Toast;

public class pyLoadApp extends Application {

	private Client client;

	// setted by main activity
	private TaskQueue taskQueue;
	private Throwable lastException;
	public SharedPreferences prefs;
	public ConnectivityManager cm;

	private pyLoad main;

	private static final String[] clientVersion = {"0.4.8", "0.4.9"};

	public void init(pyLoad main) {
		this.main = main;

		HashMap<Throwable, Runnable> map = new HashMap<Throwable, Runnable>();
		map.put(new TException(), handleException);
		map.put(new WrongLogin(), handleException);
		map.put(new TTransportException(), handleException);
		map.put(new WrongServer(), handleException);

        taskQueue = new TaskQueue(this, new Handler(), map);
		startTaskQueue();
	}

	public String verboseBool(boolean state) {
		if (state)
			return getString(R.string.on);
		else
			return getString(R.string.off);
	}

	private boolean login() throws TException {

		// replace protocol, some user also enter it
		String host = prefs.getString("host", "10.0.2.2").replaceFirst("^[a-zA-z]+://", "");
		int port = Integer.parseInt(prefs.getString("port", "7227"));
		String username = prefs.getString("username", "User");
		String password = prefs.getString("password", "pwhere");

		// TODO: better exception handling
		TTransport trans;
		try {
			if (prefs.getBoolean("ssl", false)) {
				SSLContext ctx;
				TrustManager[] trustManagers;
				try {
					if (prefs.getBoolean("ssl_validate", true)) {
						TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
						tmf.init((KeyStore) null);
						trustManagers = tmf.getTrustManagers();
					} else {
						trustManagers = new TrustManager[1];
						trustManagers[0] = new AllTrustManager();
					}
					ctx = SSLContext.getInstance("TLS");
					ctx.init(null, trustManagers, null);
					Log.d("pyLoad", "SSL Context created");
				} catch (NoSuchAlgorithmException e) {
					throw new TException(e);
				} catch (KeyStoreException e) {
					throw new TException(e);
				} catch (KeyManagementException e) {
					throw new TException(e);
				}
				// timeout 8000ms
				trans = TSSLTransportFactory.createClient(ctx.getSocketFactory(), host, port, 8000);
				if (prefs.getBoolean("ssl_validate", true)) {
					X509HostnameVerifier verifier = new BrowserCompatHostnameVerifier();
					try {
						verifier.verify(host, (SSLSocket) ((TSocket) trans).getSocket());
					} catch (IOException e) {
						throw new TException(e);
					}
					// TODO: check OCSP/CRL
				}
			} else {
				trans = new TSocket(host, port, 8000);
				trans.open();
			}
		} catch (TTransportException e) {
			throw new TException(e);
		}

		TProtocol iprot = new TBinaryProtocol(trans);

		client = new Client(iprot);
		return client.login(username, password);
	}

	public Client getClient() throws TException, WrongLogin {

		if (client == null) {
			Log.d("pyLoad", "Creating new Client");
			boolean loggedin = login();
			if (!loggedin) {
				client = null;
				throw new WrongLogin();
			}

			String server = client.getServerVersion();
			boolean match = false;
			
			for (String version : clientVersion)
				if(server.equals(version))
					match = true;
			
			if (!match)
				throw new WrongServer();

		}
		return client;
	}

	public void addTask(GuiTask task) {
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

		if (lastException instanceof TTransportException) {
			Toast t = Toast.makeText(this, R.string.lost_connection,
					Toast.LENGTH_SHORT);
			t.show();
		} else if (lastException instanceof WrongLogin) {
			Toast t = Toast.makeText(this, R.string.bad_login,
					Toast.LENGTH_SHORT);
			t.show();
		} else if (lastException instanceof TException) {
            Throwable tr = findException(lastException);

            Toast t;
            if (tr instanceof SSLHandshakeException)
                t = Toast.makeText(this, R.string.certificate_error, Toast.LENGTH_SHORT);
            else if(tr instanceof SocketTimeoutException)
                t = Toast.makeText(this, R.string.connect_timeout, Toast.LENGTH_SHORT);
            else if(tr instanceof ConnectException)
                t = Toast.makeText(this, R.string.connect_error, Toast.LENGTH_SHORT);
            else if(tr instanceof SocketException)
                t = Toast.makeText(this, R.string.socket_error, Toast.LENGTH_SHORT);
            else
                t = Toast.makeText(this, getString(R.string.no_connection)+ " " + tr.getMessage(), Toast.LENGTH_SHORT);

			t.show();
		} else if (lastException instanceof WrongServer) {
			Toast t = Toast.makeText(this, String.format(
					getString(R.string.old_server), clientVersion[clientVersion.length-1]),
					Toast.LENGTH_SHORT);
			t.show();
		}

		setProgress(false);
	}

    /**
     * Retrieves first root exception on stack of several TExceptions.
     * @return the first exception not a TException or the last TException
     */
    private Throwable findException(Throwable e) {
        // will not terminate when cycles occur, hopefully nobody cycle exception causes
        while (e instanceof TException) {
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

    @TargetApi(5)
    private void setIndeterminateProgress(boolean state) {
        main.setProgressBarIndeterminateVisibility(state);
    }

    @TargetApi(11)
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
        return android.os.Build.VERSION.SDK_INT >= 11;
    }

}
