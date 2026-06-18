package org.pyload.android.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import android.app.NotificationManager;
import android.content.res.Configuration;
import android.view.*;

import org.pyload.android.client.components.FragmentTabsPager;
import org.pyload.android.client.dialogs.AccountDialog;
import org.pyload.android.client.fragments.CollectorFragment;
import org.pyload.android.client.fragments.OverviewFragment;
import org.pyload.android.client.fragments.QueueFragment;
import org.pyload.android.client.module.Eula;
import org.pyload.android.client.module.GuiTask;
import org.pyload.android.openapi.api.PyLoadRestApi;
import org.pyload.android.openapi.models.ApiAddPackagePostRequest;
import org.pyload.android.openapi.models.ApiSetPackageDataPostRequest;
import org.pyload.android.openapi.models.Destination;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.TabHost;
import androidx.core.view.MenuItemCompat;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class pyLoad extends FragmentTabsPager {

    private pyLoadApp app;

    // keep reference to set indeterminateProgress
    private MenuItem refreshItem;

    /**
     * Called when the activity is first created.
     */

    public void onCreate(Bundle savedInstanceState) {

        Log.d("pyLoad", "Starting pyLoad App");

        app = (pyLoadApp) getApplicationContext();
        app.prefs = PreferenceManager.getDefaultSharedPreferences(this);
        initLocale();

        super.onCreate(savedInstanceState);
        Eula.show(this);

        app.cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        app.init(this);

        Resources res = getResources(); // Resource object to get Drawables
        TabHost.TabSpec spec; // Resusable TabSpec for each tab
        String title;

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
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = getIntent();
        String action = intent.getAction();
        Uri data = intent.getData();

        // we got a SHARE intent
        if (Intent.ACTION_SEND.equals(action)) {
            Intent addURL = new Intent(app, AddLinksActivity.class);
            addURL.putExtra("url", intent.getStringExtra(Intent.EXTRA_TEXT));
            addURL.putExtra("name", intent.getStringExtra(Intent.EXTRA_SUBJECT));
            startActivityForResult(addURL, AddLinksActivity.NEW_PACKAGE);
            intent.setAction(Intent.ACTION_MAIN);

            // we got a VIEW intent
        } else if (Intent.ACTION_VIEW.equals(action) && data != null) {
            if (intent.getScheme().startsWith("http") || intent.getScheme().contains("ftp")) {
                Intent addURL = new Intent(app, AddLinksActivity.class);
                addURL.putExtra("url", data.toString());
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
        Intent intent = getIntent();
        //app.setCaptchaNotificationShown(intent.getBooleanExtra("CaptchaNotification", false));
        if (intent.getBooleanExtra("CaptchaNotification", false)) {
            NotificationManager notificationManager = (NotificationManager) app.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(0);
        }
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
        inflater.inflate(R.menu.menu, menu);
        refreshItem = menu.findItem(R.id.refresh);

        MenuItemCompat.setShowAsAction(refreshItem, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
        MenuItemCompat.setShowAsAction(menu.findItem(R.id.add_links),
                MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.add_links) {
            startActivityForResult(new Intent(app, AddLinksActivity.class),
                    AddLinksActivity.NEW_PACKAGE);

            return true;

        } else if (itemId == R.id.refresh) {
            app.resetClient();
            app.refreshTab();

            return true;

        } else if (itemId == R.id.settings) {
            Intent settingsActivity = new Intent(app, Preferences.class);
            startActivity(settingsActivity);

            return true;

        } else if (itemId == R.id.show_accounts) {
            AccountDialog accountsList = new AccountDialog();
            accountsList.show(getSupportFragmentManager(), "accountsDialog");

            return true;

        } else if (itemId == R.id.remote_settings) {
            Intent serverConfigActivity = new Intent(app, RemoteSettings.class);
            startActivity(serverConfigActivity);

            return true;

        } else if (itemId == R.id.restart_failed) {
            app.addTask(new GuiTask(new Runnable() {
                public void run() {
                    PyLoadRestApi client = app.getClient();
                    app.executeNetworkCall(client.apiRestartFailedPost());
                }
            }, app.handleSuccess));

            return true;
        }

        return super.onOptionsItemSelected(item);
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
                        final Uri filepathUri = Uri.parse(filepath);

                        if (data.getIntExtra("dest", 0) == 0)
                            dest = Destination.QUEUE;
                        else
                            dest = Destination.COLLECTOR;

                        final ArrayList<String> links = new ArrayList<String>();
                        for (String link_row : link_array)
                            for (String link : link_row.trim().split(" "))
                                if (!link.equals(""))
                                    links.add(link);

                        final String password = data.getStringExtra("password");

                        app.addTask(new GuiTask(new Runnable() {

                            public void run() {
                                PyLoadRestApi client = app.getClient();

                                if (links.size() > 0) {
                                    ApiAddPackagePostRequest addPackageRequest = new ApiAddPackagePostRequest()
                                            .name(name)
                                            .links(links)
                                            .dest(dest);
                                    int pid = app.executeNetworkCall(client.apiAddPackagePost(addPackageRequest));

                                    if (password != null && !password.equals("")) {
                                        HashMap<String, Object> opts = new HashMap<>();
                                        opts.put("password", password);

                                        ApiSetPackageDataPostRequest setPackageDataRequest = new ApiSetPackageDataPostRequest()
                                                .packageId(pid)
                                                .data(opts);
                                        app.executeNetworkCall(client.apiSetPackageDataPost(setPackageDataRequest));
                                    }
                                }
                                if (!filepath.equals("")) {
                                    try (InputStream inputStream = getContentResolver().openInputStream(filepathUri);
                                         ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                                        byte[] buffer = new byte[1024];
                                        int length;
                                        while ((length = inputStream.read(buffer)) != -1) {
                                            outputStream.write(buffer, 0, length);
                                        }

                                        byte[] fileBytes = outputStream.toByteArray();

                                        if (fileBytes.length > 1048576) { // 1 MB
                                            throw new IOException("File size too large");
                                        }

                                        RequestBody body = RequestBody.create(null, fileBytes);
                                        MultipartBody.Part multipartBody = MultipartBody.Part.createFormData("data", filename, body);
                                        client.apiUploadContainerPost(filename, multipartBody).execute();
                                    } catch (IOException e) {
                                        Log.e("pyLoad", "Error when uploading file", e);
                                        throw new RuntimeException(e);
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

    /**
     * Sets the locale defined in config.
     */
    private void initLocale() {

        String language = app.prefs.getString("language", "");
        Locale locale;
        if ("".equals(language))
            locale = Locale.getDefault();
        else
            locale = new Locale(language);

        Log.d("pyLoad", "Change locale to: " + locale);
        Configuration config = new Configuration(getResources().getConfiguration());
        config.locale = locale;
        getResources().updateConfiguration(config,
                getResources().getDisplayMetrics());
    }

    public void setCaptchaResult(final int tid, final String result) {
        app.addTask(new GuiTask(new Runnable() {

            public void run() {
                PyLoadRestApi client = app.getClient();
                Log.d("pyLoad", "Send Captcha result: " + tid + " " + result);
                app.executeNetworkCall(client.apiSetCaptchaResultPost(tid, result));
            }
        }));

    }

    public MenuItem getRefreshItem() {
        return refreshItem;
    }
}
