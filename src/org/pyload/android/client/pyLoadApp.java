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
import org.pyload.thrift.Pyload.Client;

import android.app.Application;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

public class pyLoadApp extends Application {

	private Client client;

	// setted by main activity
	private Handler mHandler;
	private TaskQueue taskQueue;
	private Throwable lastException;
	SharedPreferences prefs;
	ConnectivityManager cm;

	private pyLoad main;
	private OverviewActivity overview;
	private QueueActivity queue;
	private CollectorActivity collector;

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

		
		TTransport trans;
		if(prefs.getBoolean("ssl", false)){
			SSLContext ctx;
			final TrustManager[] trustAllCerts = {new AllTrustManager()};
			try {
				ctx = SSLContext.getInstance("TLS");
				ctx.init(null, trustAllCerts, null);
				Log.d("pyLoad", "SSL Context created");
			} catch (NoSuchAlgorithmException e) {
				throw new TException(e);
			} catch (KeyManagementException e) {
				throw new TException(e);
			}
			trans = TSSLTransportFactory.createClient(ctx.getSocketFactory(), host, port, 10000);			
		}else{
			trans = new TSocket(host, port);
			trans.open();
		}
		
		TProtocol iprot = new TBinaryProtocol(trans);

		client = new Client(iprot);
		boolean login = false;
		try {
			login = client.login(username, password);
		} catch (TException e) {
			Log.e("pyLoad", "Login failed", e);

			client = null;
			throw e;
		}
		
		String server = client.getServerVersion();
		if (!server.equals("0.4.4") && !server.equals("0.4.5")) throw new WrongServer();

		return login;
	}

	public Client getClient() throws TException, WrongLogin {

		if (client == null) {
			boolean loggedin = login();
			if (!loggedin) {
				client = null;
				throw new WrongLogin();
			}
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
		@Override
		public void run() {
			onException();
		}
	};

	public void onException() {
		client = null;

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
		} else if(lastException instanceof WrongServer){
			Toast t = Toast.makeText(this, R.string.old_server, Toast.LENGTH_SHORT);
			t.show();
		}
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
		
		refreshTab(main.getCurrentTab());
	}

	public void setOverview(OverviewActivity overview) {
		this.overview = overview;
	}

	public void setQueue(QueueActivity queue) {
		this.queue = queue;
	}

	public void setCollector(CollectorActivity collector) {
		this.collector = collector;
	}

	public void refreshTab(int index) {
				
		switch (index) {
		case 0:
			overview.refresh();
			break;
		case 1:
			queue.refresh();
			break;
		case 2:
			collector.refresh();

		default:
			break;
		}
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
		client = null;		
	}

}
