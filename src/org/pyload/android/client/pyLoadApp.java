package org.pyload.android.client;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

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
	private Handler mHandler;
	private TaskQueue taskQueue;
	private Throwable lastException;
	public SharedPreferences prefs;
	public ConnectivityManager cm;

	private pyLoad main;

	static final String clientVersion = "0.4.7";

	public void init(pyLoad main) {
		this.main = main;

		mHandler = new Handler();
		HashMap<Throwable, Runnable> map = new HashMap<Throwable, Runnable>();
		map.put(new TException(), handleException);
		map.put(new WrongLogin(), handleException);
		map.put(new TTransportException(), handleException);
		map.put(new WrongServer(), handleException);

		taskQueue = new TaskQueue(this, mHandler, map);

		startTaskQueue();
	}

	public String verboseBool(boolean state) {
		if (state)
			return getString(R.string.on);
		else
			return getString(R.string.off);
	}

	public String formatSize(long size) {
		double format = size;
		int steps = 0;
		String[] sizes = { "B", "KiB", "MiB", "GiB", "TiB" };
		while (format > 1000) {
			format /= 1024.0;
			steps++;
		}
		return String.format("%.2f %s", format, sizes[steps]);
	}

	boolean login() throws TException {

		String host = prefs.getString("host", "10.0.2.2");
		int port = Integer.parseInt(prefs.getString("port", "7227"));
		String username = prefs.getString("username", "User");
		String password = prefs.getString("password", "pwhere");

		// TODO: better exception handling
		TTransport trans;
		try {
			if (prefs.getBoolean("ssl", false)) {
				SSLContext ctx;
				final TrustManager[] trustAllCerts = { new AllTrustManager() };
				try {
					ctx = SSLContext.getInstance("TLS");
					ctx.init(null, trustAllCerts, null);
					Log.d("pyLoad", "SSL Context created");
				} catch (NoSuchAlgorithmException e) {
					throw new TException(e);
				} catch (KeyManagementException e) {
					throw new TException(e);
				}
				// timeout 8000ms
				trans = TSSLTransportFactory.createClient(
						ctx.getSocketFactory(), host, port, 8000);
			} else {
				trans = new TSocket(host, port, 8000);
				trans.open();
			}
		} catch (TTransportException e) {
			throw new TException(e);
		}

		TProtocol iprot = new TBinaryProtocol(trans);

		client = new Client(iprot);
		boolean login = client.login(username, password);

		return login;
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
			if (!server.equals(clientVersion))
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
		Log.d("pyLoad", "Exception caught");

		if (lastException instanceof TTransportException) {
			Toast t = Toast.makeText(this, R.string.lost_connection,
					Toast.LENGTH_SHORT);
			t.show();
		} else if (lastException instanceof WrongLogin) {
			Toast t = Toast.makeText(this, R.string.bad_login,
					Toast.LENGTH_SHORT);
			t.show();
		} else if (lastException instanceof TException) {
			Toast t = Toast.makeText(this, R.string.no_connection,
					Toast.LENGTH_SHORT);
			t.show();
		} else if (lastException instanceof WrongServer) {
			Toast t = Toast.makeText(this, String.format(
					getString(R.string.old_server), clientVersion),
					Toast.LENGTH_SHORT);
			t.show();
		}

		setProgress(false);
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
		if (info != null) {
			return true;
		}
		return false;
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

	public void setProgress(boolean state) {
		main.setProgressBarIndeterminateVisibility(state);
	}

}
