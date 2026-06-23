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
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.Manifest;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuItemCompat;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class pyLoad extends FragmentTabsPager {

    private pyLoadApp app;

    // keep reference to set indeterminateProgress
    private MenuItem refreshItem;

    private final ActivityResultLauncher<Intent> addLinksLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        handleNewPackageResult(result.getData());
                    }
                }
            }
    );

    /**
     * Called when the activity is first created.
     */

    public void onCreate(Bundle savedInstanceState) {

        Log.d("pyLoad", "Starting pyLoad App");

        super.onCreate(savedInstanceState);

        app = (pyLoadApp) getApplicationContext();
        app.prefs = getSharedPreferences(getPackageName() + "_preferences", Context.MODE_PRIVATE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        Eula.show(this);

        app.cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        app.init(this);

        String title;

        title = getString(R.string.overview);
        mTabsAdapter.addTab(title, null, OverviewFragment.class, null);

        title = getString(R.string.queue);
        mTabsAdapter.addTab(title, null, QueueFragment.class, null);

        title = getString(R.string.collector);
        mTabsAdapter.addTab(title, null, CollectorFragment.class, null);
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
            addLinksLauncher.launch(addURL);
            intent.setAction(Intent.ACTION_MAIN);

            // we got a VIEW intent
        } else if (Intent.ACTION_VIEW.equals(action) && data != null) {
            if (intent.getScheme().startsWith("http") || intent.getScheme().contains("ftp")) {
                Intent addURL = new Intent(app, AddLinksActivity.class);
                addURL.putExtra("url", data.toString());
                addLinksLauncher.launch(addURL);
            } else if (intent.getScheme().equals("file")) {
                Intent addURL = new Intent(app, AddLinksActivity.class);
                addURL.putExtra("dlcpath", data.getPath());
                addLinksLauncher.launch(addURL);
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

        refreshItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        menu.findItem(R.id.add_links).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.add_links) {
            addLinksLauncher.launch(new Intent(app, AddLinksActivity.class));

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
        } else if (itemId == R.id.clear_finished) {
            app.addTask(new GuiTask(new Runnable() {
                public void run() {
                    PyLoadRestApi client = app.getClient();
                    app.executeNetworkCall(client.apiDeleteFinishedPost());
                }
            }, app.handleSuccess));

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void handleNewPackageResult(Intent data) {
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
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.d("pyLoad", "got Intent");
        super.onNewIntent(intent);
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
