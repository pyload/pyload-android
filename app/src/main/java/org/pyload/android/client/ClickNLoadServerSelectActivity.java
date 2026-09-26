package org.pyload.android.client;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.pyload.android.client.models.Server;
import org.pyload.android.client.module.GuiTask;
import org.pyload.android.client.module.LanguageUtils;
import org.pyload.android.client.module.ServerManager;
import org.pyload.android.openapi.api.PyLoadRestApi;
import org.pyload.android.openapi.model.ApiAddPackagePostRequest;
import org.pyload.android.openapi.model.ApiSetPackageDataPostRequest;
import org.pyload.android.openapi.model.Destination;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class ClickNLoadServerSelectActivity extends AppCompatActivity {

    public static final String EXTRA_TYPE = "type";
    public static final String EXTRA_PACKAGE_NAME = "package_name";
    public static final String EXTRA_PASSWORD = "password";
    public static final String EXTRA_LINKS = "links";
    public static final String EXTRA_DLC_CONTENT = "dlc_content";

    public static final String TYPE_LINKS = "links";
    public static final String TYPE_DLC = "dlc";

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LanguageUtils.attachBaseContext(newBase));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent == null) {
            finish();
            return;
        }

        List<Server> servers = ServerManager.getInstance(this).getServers();
        if (servers.isEmpty()) {
            finish();
            return;
        }

        String packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME);
        if (packageName == null || packageName.isEmpty()) {
            packageName = "ClickNLoad Package";
        }

        if (servers.size() == 1) {
            Server singleServer = servers.get(0);
            processPayload(intent, singleServer, packageName);
            finish();
            return;
        }

        CharSequence[] items = new CharSequence[servers.size()];
        for (int i = 0; i < servers.size(); i++) {
            Server s = servers.get(i);
            items[i] = s.getName() + " (" + s.getFormattedUrl() + ")";
        }

        final String finalPackageName = packageName;
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.clicknload) + ": " + getString(R.string.choose_server))
                .setItems(items, (dialog, which) -> {
                    Server selectedServer = servers.get(which);
                    processPayload(intent, selectedServer, finalPackageName);
                    finish();
                })
                .setOnCancelListener(dialog -> finish())
                .setNegativeButton(R.string.cancel, (dialog, which) -> finish())
                .show();
    }

    private void processPayload(Intent intent, Server targetServer, String packageName) {
        pyLoadApp app = (pyLoadApp) getApplicationContext();
        String type = intent.getStringExtra(EXTRA_TYPE);
        String password = intent.getStringExtra(EXTRA_PASSWORD);

        if (TYPE_DLC.equals(type)) {
            String content = intent.getStringExtra(EXTRA_DLC_CONTENT);
            if (content != null) {
                app.addTask(new GuiTask(() -> {
                    PyLoadRestApi client = app.getClientForServer(targetServer);

                    int destVal = Integer.parseInt(app.prefs.getString("clicknload_dest", "1"));
                    Destination dest = destVal == 0 ? Destination.QUEUE : Destination.COLLECTOR;

                    byte[] fileBytes = content.replace(" ", "+").getBytes();
                    String filename = packageName.toLowerCase().endsWith(".dlc") ? packageName : packageName + ".dlc";

                    RequestBody body = RequestBody.Companion.create(fileBytes, null);
                    MultipartBody.Part multipartBody = MultipartBody.Part.createFormData("data", filename, body);

                    app.executeNetworkCall(client.apiUploadContainerPost(filename, multipartBody, dest));
                }, app.handleSuccess));
            }
        } else if (TYPE_LINKS.equals(type)) {
            ArrayList<String> linkList = intent.getStringArrayListExtra(EXTRA_LINKS);
            if (linkList != null && !linkList.isEmpty()) {
                app.addTask(new GuiTask(() -> {
                    PyLoadRestApi client = app.getClientForServer(targetServer);

                    ApiAddPackagePostRequest request = new ApiAddPackagePostRequest()
                            .name(packageName)
                            .links(linkList)
                            .dest(Destination.COLLECTOR);
                    int pid = app.executeNetworkCall(client.apiAddPackagePost(request));

                    if (password != null && !password.isEmpty()) {
                        HashMap<String, Object> opts = new HashMap<>();
                        opts.put("password", password);

                        ApiSetPackageDataPostRequest setPackageDataRequest = new ApiSetPackageDataPostRequest()
                                .packageId(pid)
                                .data(opts);
                        app.executeNetworkCall(client.apiSetPackageDataPost(setPackageDataRequest));
                    }

                    int destVal = Integer.parseInt(app.prefs.getString("clicknload_dest", "1"));
                    Destination dest = destVal == 0 ? Destination.QUEUE : Destination.COLLECTOR;

                    if (dest == Destination.QUEUE) {
                        app.executeNetworkCall(client.apiPushToQueuePost(pid));
                    }
                }, app.handleSuccess));
            }
        }
    }
}
