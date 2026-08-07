package org.pyload.android.client;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.pyload.android.client.components.FragmentTabsPager;
import org.pyload.android.client.components.TabHandler;
import org.pyload.android.client.dialogs.AccountDialog;
import org.pyload.android.client.fragments.AbstractPackageFragment;
import org.pyload.android.client.fragments.CollectorFragment;
import org.pyload.android.client.fragments.OverviewFragment;
import org.pyload.android.client.fragments.QueueFragment;
import org.pyload.android.client.module.Eula;
import org.pyload.android.client.module.GuiTask;
import org.pyload.android.client.services.ClickNLoadService;
import org.pyload.android.openapi.api.PyLoadRestApi;
import org.pyload.android.openapi.model.ApiAddPackagePostRequest;
import org.pyload.android.openapi.model.ApiSetPackageDataPostRequest;
import org.pyload.android.openapi.model.Destination;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class pyLoad extends FragmentTabsPager {

    private pyLoadApp app;

    // keep reference to set indeterminateProgress
    private MenuItem refreshItem;
    private MenuItem searchItem;
    private View captchaBanner;

    private OnBackPressedCallback onBackPressedCallback;

    private boolean captchaAvailable = false;
    private boolean lastCaptchaState = false;

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Runnable runCaptchaUpdate = new Runnable() {
        public void run() {
            if (app.isPollingPaused()) return;
            PyLoadRestApi client = app.getClient();
            if (app.prefs.getBoolean("pull_captcha", true)) {
                captchaAvailable = app.executeNetworkCall(client.apiIsCaptchaWaitingGet());
                if (!captchaAvailable) {
                    app.setCaptchaNotificationShown(false);
                }
            }
        }
    };

    private final Runnable onCaptchaDataReceived = new Runnable() {
        public void run() {
            if (app.isPollingPaused() || !app.prefs.getBoolean("pull_captcha", true)) {
                captchaBanner.setVisibility(View.GONE);
                return;
            }

            if (captchaAvailable) {
                captchaBanner.setVisibility(View.VISIBLE);
                if (!lastCaptchaState) {
                    showCaptchaNotification();
                    lastCaptchaState = true;
                }
            } else {
                captchaBanner.setVisibility(View.GONE);
                lastCaptchaState = false;
            }
        }
    };

    private final Runnable mCaptchaTimeTask = new Runnable() {
        public void run() {
            if (app.isPollingPaused()) {
                return;
            }
            checkCaptcha();
            int interval;
            try {
                interval = Integer.parseInt(app.prefs.getString("refresh_rate", "5"));
            } catch (NumberFormatException e) {
                interval = 5;
            }
            mHandler.postDelayed(this, (long) interval * 1000);
        }
    };

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

        captchaBanner = findViewById(R.id.captcha_banner_container);
        captchaBanner.setOnClickListener(v -> {
            Intent intent = new Intent(this, CaptchaActivity.class);
            startActivity(intent);
        });

        onBackPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (searchItem != null && searchItem.isActionViewExpanded()) {
                    searchItem.collapseActionView();
                } else {
                    new MaterialAlertDialogBuilder(pyLoad.this)
                            .setTitle(R.string.exit_confirm_title)
                            .setMessage(R.string.exit_confirm_message)
                            .setPositiveButton(R.string.yes, (dialog, which) -> finish())
                            .setNegativeButton(R.string.no, null)
                            .show();
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);
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
            String scheme = intent.getScheme();
            if (scheme != null && (scheme.startsWith("http") || scheme.contains("ftp") || scheme.equals("magnet"))) {
                Intent addURL = new Intent(app, AddLinksActivity.class);
                addURL.putExtra("url", data.toString());
                addLinksLauncher.launch(addURL);
            } else if (scheme != null && (scheme.equals("file") || scheme.equals("content"))) {
                Intent addURL = new Intent(app, AddLinksActivity.class);
                addURL.putExtra("filepath", data.toString());
                addLinksLauncher.launch(addURL);
            }
            intent.setData(null);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = getIntent();
        if (intent.getBooleanExtra("CaptchaNotification", false)) {
            intent.removeExtra("CaptchaNotification");
            NotificationManager notificationManager = (NotificationManager) app.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(0);

            Intent captchaIntent = new Intent(this, CaptchaActivity.class);
            startActivity(captchaIntent);
        }

        if (app.prefs.getBoolean("keep_screen_on", true)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        mHandler.post(mCaptchaTimeTask);
        if (!app.isPollingPaused()) {
            app.refreshTab();
        }

        if (app.prefs.getBoolean("clicknload", false)) {
            Intent clicknloadIntent = new Intent(this, ClickNLoadService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(clicknloadIntent);
            } else {
                startService(clicknloadIntent);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mHandler.removeCallbacks(mCaptchaTimeTask);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        refreshItem = menu.findItem(R.id.refresh);
        searchItem = menu.findItem(R.id.search);

        if (searchItem != null) {
            searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
                @Override
                public boolean onMenuItemActionExpand(MenuItem item) {
                    onBackPressedCallback.setEnabled(true);
                    MenuItem addLinks = menu.findItem(R.id.add_links);
                    if (addLinks != null) {
                        addLinks.setVisible(false);
                    }
                    return true;
                }

                @Override
                public boolean onMenuItemActionCollapse(MenuItem item) {
                    onBackPressedCallback.setEnabled(false);
                    MenuItem addLinks = menu.findItem(R.id.add_links);
                    if (addLinks != null) {
                        addLinks.setVisible(true);
                    }
                    return true;
                }
            });

            SearchView searchView = (SearchView) searchItem.getActionView();
            if (searchView != null) {
                searchView.setIconifiedByDefault(true);

                // Remove the underline from the search box
                int searchPlateId = androidx.appcompat.R.id.search_plate;
                View searchPlate = searchView.findViewById(searchPlateId);
                if (searchPlate != null) {
                    searchPlate.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                }

                searchView.setSubmitButtonEnabled(false);

                // Access the inner EditText to handle focus and clicks
                View searchAutoComplete = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
                if (searchAutoComplete != null) {
                    searchAutoComplete.setOnTouchListener((v, event) -> {
                        if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                            // Force focus reset to trigger keyboard if it was dismissed
                            if (v.isFocused()) {
                                v.clearFocus();
                                v.requestFocus();
                            }
                            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            if (imm != null) {
                                imm.showSoftInput(v, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
                            }
                        }
                        return false;
                    });

                    searchAutoComplete.setOnKeyListener((v, keyCode, event) -> {
                        if (keyCode == KeyEvent.KEYCODE_BACK) {
                            if (event.getAction() == KeyEvent.ACTION_UP) {
                                if (searchItem != null && searchItem.isActionViewExpanded()) {
                                    searchItem.collapseActionView();
                                }
                            }
                            // Always consume back key when search is expanded to prevent default SearchView behavior
                            return searchItem != null && searchItem.isActionViewExpanded();
                        }
                        return false;
                    });
                }

                searchView.setOnSearchClickListener(v -> {
                    // Immediate focus and open keyboard
                    searchView.requestFocus();
                    android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.showSoftInput(searchView.findFocus(), android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
                    }
                });

                searchView.setOnCloseListener(() -> {
                    return false;
                });

                // Force the 'X' button to close the search view entirely
                int closeBtnId = androidx.appcompat.R.id.search_close_btn;
                View closeBtn = searchView.findViewById(closeBtnId);
                if (closeBtn != null) {
                    // Prevent it from being grayed out/semi-transparent when empty
                    closeBtn.setAlpha(1.0f);
                    closeBtn.setEnabled(true);

                    // Apply the same color as the magnifier icon
                    if (closeBtn instanceof ImageView) {
                        android.util.TypedValue typedValue = new android.util.TypedValue();
                        getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true);
                        int color = typedValue.data;
                        ((ImageView) closeBtn).setColorFilter(color);
                    }

                    closeBtn.setOnClickListener(v -> {
                        searchItem.collapseActionView();
                    });
                }

                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        if (query != null && !query.trim().isEmpty()) {
                            searchView.clearFocus();
                        }
                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        Fragment frag = getCurrentFragment();
                        if (frag instanceof TabHandler) {
                            ((TabHandler) frag).onSearch(newText);
                        }
                        return true;
                    }
                });
            }
        }

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
        String linksStr = data.getStringExtra("links");
        if (linksStr == null) return;
        final String[] link_array = linksStr.trim().split("\n");
        final Destination dest;
        final String filepath = data.getStringExtra("filepath");
        final String filename = data.getStringExtra("filename");
        final Uri filepathUri = Uri.parse(filepath);

        if (data.getIntExtra("dest", 0) == 0)
            dest = Destination.QUEUE;
        else
            dest = Destination.COLLECTOR;

        final ArrayList<String> links = new ArrayList<>();
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

                        RequestBody body = RequestBody.Companion.create(fileBytes, null);
                        MultipartBody.Part multipartBody = MultipartBody.Part.createFormData("data", filename, body);
                        client.apiUploadContainerPost(filename, multipartBody, dest).execute();
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
        setIntent(intent);
        if ("SET_CAPTCHA_RESULT".equals(intent.getAction())) {
            int tid = intent.getIntExtra("tid", -1);
            String result = intent.getStringExtra("result");
            if (tid != -1 && result != null) {
                setCaptchaResult(tid, result);
            }
        }
    }

    public void setCaptchaResult(final int tid, final String result) {
        app.addTask(new GuiTask(new Runnable() {

            public void run() {
                PyLoadRestApi client = app.getClient();
                Log.d("pyLoad", "Send Captcha result: " + tid + " " + result);
                app.executeNetworkCall(client.apiSetCaptchaResultPost(tid, result));
            }
        }, () -> {
            app.onSuccess();
            // Check for next captcha immediately after success
            checkCaptcha();
        }));

    }

    private void checkCaptcha() {
        if (!app.hasConnection() || app.isPollingPaused()) return;
        app.addTask(new GuiTask(runCaptchaUpdate, onCaptchaDataReceived));
    }

    private void showCaptchaNotification() {
        if (!app.getCaptchaNotificationShown()) {
            app.setCaptchaNotificationShown(true);
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(app, pyLoadApp.CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setContentTitle(getString(R.string.captcha_notification))
                    .setContentText(getString(R.string.captcha_notification_desc))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setLocalOnly(true)
                    .setAutoCancel(true);

            Intent notificationIntent = new Intent(app, pyLoad.class);
            notificationIntent.putExtra("CaptchaNotification", true);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }
            PendingIntent contentIntent = PendingIntent.getActivity(app, 0, notificationIntent, flags);
            mBuilder.setContentIntent(contentIntent);
            NotificationManager mNotificationManager = (NotificationManager) app.getSystemService(Context.NOTIFICATION_SERVICE);
            Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            mBuilder.setSound(alarmSound);
            mNotificationManager.notify(0, mBuilder.build());
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem search = menu.findItem(R.id.search);
        if (search != null) {
            Fragment frag = getCurrentFragment();
            // Overview, Queue, and Collector tabs support searching
            boolean visible = frag instanceof OverviewFragment || frag instanceof AbstractPackageFragment;
            search.setVisible(visible);

            SearchView searchView = (SearchView) search.getActionView();
            if (searchView != null && !searchView.isIconified()) {
                // Reset search query and trigger listeners to refresh the list
                searchView.setQuery("", true);
                searchView.setIconified(true);
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    public MenuItem getRefreshItem() {
        return refreshItem;
    }

    @SuppressWarnings("unused")
    public MenuItem getSearchItem() {
        return searchItem;
    }
}
