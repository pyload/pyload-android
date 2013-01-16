package org.pyload.android.client;

import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;

import org.pyload.android.client.components.FragmentTabsPager;
import org.pyload.android.client.fragments.AccountFragment;
import org.pyload.android.client.fragments.CollectorFragment;
import org.pyload.android.client.fragments.OverviewFragment;
import org.pyload.android.client.fragments.QueueFragment;
import org.pyload.android.client.fragments.SettingsFragment;
import org.pyload.android.client.module.Eula;
import org.pyload.android.client.module.GuiTask;
import org.pyload.thrift.Destination;
import org.pyload.thrift.PackageDoesNotExists;
import org.pyload.thrift.Pyload.Client;

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
import android.widget.TabHost;

public class pyLoad extends FragmentTabsPager {

	private pyLoadApp app;

	/** Called when the activity is first created. */

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		// setContentView(R.layout.main);

		Eula.show(this);

		app = (pyLoadApp) getApplicationContext();

		Log.d("pyLoad", "Starting pyLoad App");

		app.prefs = PreferenceManager.getDefaultSharedPreferences(this);
		app.cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		app.init(this);

		Resources res = getResources(); // Resource object to get Drawables
		// tabHost = getTabHost(); // The activity TabHost
		TabHost.TabSpec spec; // Resusable TabSpec for each tab
		String title;

		int tab_pyload, tab_queue, tab_collector, tab_settings, tab_accounts;
		if (app.prefs.getBoolean("invert_tabs", false)) {
			tab_pyload = R.drawable.ic_tab_pyload_inverted;
			tab_queue = R.drawable.ic_tab_queue_inverted;
			tab_collector = R.drawable.ic_tab_collector_inverted;
			tab_settings = R.drawable.ic_tab_settings;
			tab_accounts = R.drawable.ic_tab_accounts;
		} else {
			tab_pyload = R.drawable.ic_tab_pyload;
			tab_queue = R.drawable.ic_tab_queue;
			tab_collector = R.drawable.ic_tab_collector;
			tab_settings = R.drawable.ic_tab_settings;
			tab_accounts = R.drawable.ic_tab_accounts;
		}

		title = getString(R.string.overview);
		spec = mTabHost.newTabSpec(title).setIndicator(title,
				res.getDrawable(tab_pyload));
		mTabsAdapter.addTab(spec, OverviewFragment.class, null);

		title = getString(R.string.queue);
		spec = mTabHost.newTabSpec(title).setIndicator(title,
				res.getDrawable(tab_queue));
		mTabsAdapter.addTab(spec, QueueFragment.class, null);

		title = getString(R.string.collector);
		spec = mTabHost.newTabSpec(title).setIndicator(title,
				res.getDrawable(tab_collector));
		mTabsAdapter.addTab(spec, CollectorFragment.class, null);

		title = getString(R.string.settings);
		spec = mTabHost.newTabSpec(title).setIndicator(title,
				res.getDrawable(tab_settings));
		mTabsAdapter.addTab(spec, SettingsFragment.class, null);

		title = getString(R.string.accounts);
		spec = mTabHost.newTabSpec(title).setIndicator(title,
				res.getDrawable(tab_accounts));
		mTabsAdapter.addTab(spec, AccountFragment.class, null);

	}

	@Override
	protected void onStart() {
		super.onStart();
		Intent intent = getIntent();
		Uri data = intent.getData();

		// startActivity(new Intent(app, FragmentTabsPager.class));

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

	@Override
	protected void onResume() {
		super.onResume();
		app.refreshTab();
	}

	@Override
	protected void onPause() {
		super.onPause();
		app.clearTasks();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        if (android.os.Build.VERSION.SDK_INT >= 11) {
            inflater.inflate(R.menu.menu_bar, menu);
        } else {
            inflater.inflate(R.menu.menu, menu);
        }
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.add_links:

			startActivityForResult(new Intent(app, AddLinksActivity.class),
					AddLinksActivity.NEW_PACKAGE);

			return true;

		case R.id.refresh:

			app.resetClient();
			app.refreshTab();

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

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
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

	@Override
	protected void onNewIntent(Intent intent) {
		Log.d("pyLoad", "got Intent");
		super.onNewIntent(intent);
	}

	public void setCaptchaResult(final short tid, final String result) {
		app.addTask(new GuiTask(new Runnable() {

			public void run() {
				Client client = app.getClient();
				Log.d("pyLoad", "Send Captcha result: " + tid + " " + result);
				client.setCaptchaResult(tid, result);

			}
		}));

	}
}