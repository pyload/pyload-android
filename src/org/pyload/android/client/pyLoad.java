package org.pyload.android.client;

import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;

import org.pyload.android.client.module.Eula;
import org.pyload.android.client.module.GuiTask;
import org.pyload.thrift.Destination;
import org.pyload.thrift.PackageDoesNotExists;
import org.pyload.thrift.Pyload.Client;

import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.widget.TabHost;

public class pyLoad extends TabActivity {

	private pyLoadApp app;
	private TabHost tabHost;

	/** Called when the activity is first created. */
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.main);

		Eula.show(this);

		app = (pyLoadApp) getApplicationContext();

		Log.d("pyLoad", "Starting pyLoad App");

		app.prefs = PreferenceManager.getDefaultSharedPreferences(this);
		app.cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		app.init(this);

		Resources res = getResources(); // Resource object to get Drawables
		tabHost = getTabHost(); // The activity TabHost
		TabHost.TabSpec spec; // Resusable TabSpec for each tab
		Intent intent; // Reusable Intent for each tab

		int tab_pyload, tab_queue, tab_collector;
		if (app.prefs.getBoolean("invert_tabs", false)) {
			tab_pyload = R.drawable.ic_tab_pyload_inverted;
			tab_queue = R.drawable.ic_tab_queue_inverted;
			tab_collector = R.drawable.ic_tab_collector_inverted;
		} else {
			tab_pyload = R.drawable.ic_tab_pyload;
			tab_queue = R.drawable.ic_tab_queue;
			tab_collector = R.drawable.ic_tab_collector;
		}

		// Create an Intent to launch an Activity for the tab (to be reused)
		intent = new Intent().setClass(this, OverviewActivity.class);

		// Initialize a TabSpec for each tab and add it to the TabHost
		spec = tabHost.newTabSpec("Overview")
				.setIndicator("Overview", res.getDrawable(tab_pyload))
				.setContent(intent);
		tabHost.addTab(spec);

		intent = new Intent().setClass(this, QueueActivity.class);
		String queue = app.getString(R.string.queue);
		spec = tabHost.newTabSpec(queue)
				.setIndicator(queue, res.getDrawable(tab_queue))
				.setContent(intent);
		tabHost.addTab(spec);

		String collector = app.getString(R.string.collector);
		intent = new Intent().setClass(this, CollectorActivity.class);
		spec = tabHost.newTabSpec(collector)
				.setIndicator(collector, res.getDrawable(tab_collector))
				.setContent(intent);
		tabHost.addTab(spec);

		tabHost.setCurrentTab(0);
	}

	
	protected void onStart() {
		super.onStart();
		Intent intent = getIntent();
		Uri data = intent.getData();

		// we got an intent
		if (data != null) {
			if (intent.getScheme().startsWith("http")) {
				Intent addURL = new Intent(app, AddLinksActivity.class);
				addURL.putExtra("dlcurl", data.toString());
				startActivityForResult(addURL, AddLinksActivity.NEW_PACKAGE);
			} else if (intent.getScheme().equals("file")) {
				Intent addURL = new Intent(app, AddLinksActivity.class);
				addURL.putExtra("dlcpath", data.getPath());
				startActivityForResult(addURL, AddLinksActivity.NEW_PACKAGE);
			}
			intent.setData(null);
		}
	}

	
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.add_links:

			startActivityForResult(new Intent(app, AddLinksActivity.class),
					AddLinksActivity.NEW_PACKAGE);

			return true;

		case R.id.refresh:

			TabHost tabHost = getTabHost();
			int tab = tabHost.getCurrentTab();

			app.resetClient();
			app.refreshTab(tab);

			return true;

		case R.id.settings:
			Intent settingsActivity = new Intent(app, Preferences.class);
			startActivity(settingsActivity);

			return true;

		case R.id.toggle_server:

			app.addTask(new GuiTask(new Runnable() {

				
				public void run() {
					Client client = app.getClient();
					client.togglePause();
				}
			}, app.handleSuccess));

			return true;

		case R.id.toggle_reconnect:

			app.addTask(new GuiTask(new Runnable() {

				
				public void run() {
					Client client = app.getClient();
					client.toggleReconnect();
				}
			}, app.handleSuccess));

			return true;

		
		case R.id.toggle_limit_speed:
			
			app.addTask(new GuiTask(new Runnable() {
				
				
				public void run() {
					Client client = app.getClient();
					
					String limitspeed = client.getConfigValue("download", "limit_speed", "core");

					if(limitspeed.equals("True")) 
						{client.setConfigValue("download", "limit_speed", "False", "core");}
					else
						{client.setConfigValue("download", "limit_speed", "True", "core");}
				}
			}, app.handleSuccess));
		
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		switch (requestCode) {
		case AddLinksActivity.NEW_PACKAGE:
			switch (resultCode) {
			case RESULT_OK:
				final String name = data.getStringExtra("name");
				final String[] link_array = data.getStringExtra("links").trim()
						.split("\n");
				final Destination dest;
				final String filepath = data.getStringExtra("filepath");
				final String filename = data.getStringExtra("filename");

				if (data.getIntExtra("dest", 0) == 0)
					dest = Destination.Queue;
				else
					dest = Destination.Collector;

				final ArrayList<String> links = new ArrayList<String>();
				for (String link_row : link_array)
					for (String link : link_row.trim().split(" "))
						if (!link.equals(""))
							links.add(link);

				final String password = data.getStringExtra("password");

				app.addTask(new GuiTask(new Runnable() {

					
					public void run() {
						Client client = app.getClient();

						if (links.size() > 0) {
							int pid = client.addPackage(name, links, dest);

							if (password != null && !password.equals("")) {

								HashMap<String, String> opts = new HashMap<String, String>();
								opts.put("password", password);

								try {
									client.setPackageData(pid, opts);
								} catch (PackageDoesNotExists e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
						}
						if (filename != null && !filepath.equals("")) {

							File file = new File(filepath);
							try {
								if (file.length() > (1 << 20))
									throw new Exception("File size to large");
								FileInputStream is = new FileInputStream(file);
								ByteBuffer buffer = ByteBuffer
										.allocate((int) file.length());

								while (is.getChannel().read(buffer) > 0)
									;
								buffer.rewind();
								is.close();
								client.uploadContainer(filename, buffer);

							} catch (Throwable e) {
								Log.e("pyLoad", "Error when uploading file", e);
							}
						}

					}
				}, app.handleSuccess));
				break;
			default:
				break;
			}
			break;

		default:
			super.onActivityResult(requestCode, resultCode, data);
		}

	}

	
	protected void onNewIntent(Intent intent) {
		Log.d("pyLoad", "got Intent");
		super.onNewIntent(intent);
	}

	public int getCurrentTab() {
		return tabHost.getCurrentTab();
	}
}