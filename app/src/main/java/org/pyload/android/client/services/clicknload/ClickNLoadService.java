package org.pyload.android.client.services.clicknload;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Base64;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import org.pyload.android.client.R;
import org.pyload.android.client.pyLoad;
import org.pyload.android.client.pyLoadApp;
import org.pyload.android.client.module.GuiTask;
import org.pyload.android.client.module.Utils;
import org.pyload.android.openapi.api.PyLoadRestApi;
import org.pyload.android.openapi.model.ApiAddPackagePostRequest;
import org.pyload.android.openapi.model.ApiSetPackageDataPostRequest;
import org.pyload.android.openapi.model.Destination;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class ClickNLoadService extends Service {

    private static final String TAG = "ClickNLoadService";
    private static final int PORT = 9666;
    private ServerThread serverThread;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(1, createNotification());
        if (serverThread == null || !serverThread.isAlive()) {
            serverThread = new ServerThread();
            serverThread.start();
        }
        return START_STICKY;
    }

    private Notification createNotification() {
        String channelId = "clicknload_channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    getString(R.string.clicknload),
                    NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        Intent notificationIntent = new Intent(this, pyLoad.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, channelId)
                .setContentTitle(getString(R.string.clicknload))
                .setContentText(getString(R.string.clicknload_service_running))
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .build();
    }

    @Override
    public void onDestroy() {
        if (serverThread != null) {
            serverThread.stopServer();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private class ServerThread extends Thread {
        private ServerSocket serverSocket;
        private boolean running = true;

        public void stopServer() {
            running = false;
            try {
                if (serverSocket != null) {
                    serverSocket.close();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error closing server socket", e);
            }
        }

        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(PORT);
                Log.i(TAG, "Server started on port " + PORT);
                while (running) {
                    Socket socket = serverSocket.accept();
                    handleClient(socket);
                }
            } catch (Exception e) {
                if (running) {
                    Log.e(TAG, "Server error", e);
                }
            }
        }

        private void handleClient(Socket socket) {
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                     OutputStream out = socket.getOutputStream()) {

                    String line = reader.readLine();
                    if (line == null) return;

                    Log.d(TAG, "ClickNLoad Request: " + line);
                    String[] parts = line.split(" ");
                    if (parts.length < 2) return;
                    String method = parts[0];
                    String rawPath = parts[1];
                    
                    URI uri = new URI(rawPath);
                    String path = uri.getPath();

                    if (method.equals("GET") || method.equals("OPTIONS")) {
                        if ("/jdcheck.js".equals(path)) {
                            String body = method.equals("GET") ? "jdownloader=true;\r\nvar version='42707';\r\n" : "";
                            sendResponse(out, body, "application/javascript");
                        } else if ("/flash/".equals(path)) {
                            sendResponse(out, "JDownloader");
                        } else {
                            sendResponse(out, "pyLoad Android CNL2", "text/plain");
                        }
                    } else if (method.equals("POST") && ("/flash/addcrypted2".equals(path) || "/flash/addcrypted".equals(path) || "/flash/add".equals(path))) {
                        // Handle CNL (v1, v2, and Plain CNL2)
                        int contentLength = 0;
                        while ((line = reader.readLine()) != null && !line.isEmpty()) {
                            if (line.toLowerCase().startsWith("content-length:")) {
                                contentLength = Integer.parseInt(line.substring(15).trim());
                            }
                        }

                        char[] body = new char[contentLength];
                        reader.read(body, 0, contentLength);
                        String bodyStr = new String(body);

                        Map<String, String> params = Utils.parseQueryParams(bodyStr);
                        String crypted = params.get("crypted");
                        String jk = params.get("jk");
                        String urls = params.get("urls");

                        String packageName = params.get("package");
                        if (packageName == null || packageName.isEmpty()) {
                            packageName = params.get("source");
                        }
                        if (packageName == null || packageName.isEmpty()) {
                            packageName = params.get("referer");
                        }
                        if (packageName == null || packageName.isEmpty()) {
                            packageName = "ClickNLoad Package";
                        }

                        String password = params.get("passwords");

                        if ("/flash/add".equals(path) && urls != null) {
                            addLinks(urls, packageName, password);
                            sendResponse(out, "success");
                        } else if ("/flash/addcrypted".equals(path) && crypted != null) {
                            addDLCContainer(crypted, packageName);
                            sendResponse(out, "success");
                        } else if ("/flash/addcrypted2".equals(path) && crypted != null && jk != null) {
                            addEncryptedLinks(crypted, jk, packageName, password);
                            sendResponse(out, "success");
                        } else {
                            sendResponse(out, "failed");
                        }
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Error handling client", e);
                } finally {
                    try {
                        socket.close();
                    } catch (Exception ignored) {}
                }
            }).start();
        }

        private void sendResponse(OutputStream out, String body) throws Exception {
            sendResponse(out, body, "text/plain");
        }

        private void sendResponse(OutputStream out, String body, String contentType) throws Exception {
            String response = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: " + contentType + "\r\n" +
                    "Content-Length: " + body.length() + "\r\n" +
                    "Access-Control-Allow-Origin: *\r\n" +
                    "Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n" +
                    "Access-Control-Allow-Headers: Content-Type\r\n" +
                    "\r\n" +
                    body;
            out.write(response.getBytes());
        }
    }

    private void addLinks(String urls, String packageName, String password) {
        String[] linkArray = urls.split("\n");
        ArrayList<String> linkList = new ArrayList<>();
        for (String link : linkArray) {
            String trimmed = link.trim();
            if (!trimmed.isEmpty()) {
                linkList.add(trimmed);
            }
        }

        if (!linkList.isEmpty()) {
            executeAddPackage(linkList, packageName, password);
        }
    }

    private void addDLCContainer(String content, String packageName) {
        pyLoadApp app = (pyLoadApp) getApplicationContext();
        app.addTask(new GuiTask(() -> {
            PyLoadRestApi client = app.getClient();

            int destVal = Integer.parseInt(app.prefs.getString("clicknload_dest", "1"));
            Destination dest = destVal == 0 ? Destination.QUEUE : Destination.COLLECTOR;

            byte[] fileBytes = content.replace(" ", "+").getBytes();
            String filename = packageName.toLowerCase().endsWith(".dlc") ? packageName : packageName + ".dlc";

            okhttp3.RequestBody body = okhttp3.RequestBody.Companion.create(fileBytes, null);
            okhttp3.MultipartBody.Part multipartBody = okhttp3.MultipartBody.Part.createFormData("data", filename, body);

            app.executeNetworkCall(client.apiUploadContainerPost(filename, multipartBody, dest));
        }, app.handleSuccess));
    }

    private void addEncryptedLinks(String crypted, String jk, String packageName, String password) {
        try {
            String keyHex = decryptJK(jk);
            if (keyHex == null) {
                Log.e(TAG, "Could not decrypt jk: " + jk);
                return;
            }

            byte[] key = Utils.hexToBytes(keyHex);
            byte[] data = Base64.decode(crypted.replace(" ", "+"), Base64.DEFAULT);

            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(key);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

            byte[] decrypted = cipher.doFinal(data);
            String links = new String(decrypted).replace("\0", "").trim();
            
            // Links are separated by newlines
            String[] linkArray = links.split("\n");
            ArrayList<String> linkList = new ArrayList<>();
            for (String link : linkArray) {
                String trimmed = link.trim();
                if (!trimmed.isEmpty()) {
                    linkList.add(trimmed);
                }
            }

            if (!linkList.isEmpty()) {
                executeAddPackage(linkList, packageName, password);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error decrypting CNL2", e);
        }
    }

    private void executeAddPackage(ArrayList<String> linkList, String packageName, String password) {
        pyLoadApp app = (pyLoadApp) getApplicationContext();
        app.addTask(new GuiTask(() -> {
            PyLoadRestApi client = app.getClient();

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

    private String decryptJK(String jk) {
        Pattern p = Pattern.compile("return\\s+['\"]([^'\"]+)['\"]");
        Matcher m = p.matcher(jk);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }
}
