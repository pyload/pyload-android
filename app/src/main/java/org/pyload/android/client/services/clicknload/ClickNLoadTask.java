package org.pyload.android.client.services.clicknload;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import org.pyload.android.client.R;
import org.pyload.android.client.pyLoadApp;
import org.pyload.thrift.Destination;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ClickNLoadTask implements Runnable{

    private final static String LOGTAG= "ClickNLoadTask";

    private volatile boolean stopped = false;
    private int port;
    private pyLoadApp app;

    public ClickNLoadTask(int port, pyLoadApp app){
        this.port = port;
        this.app = app;

        app.prefs.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                //stop if clicknload gets disabled
                if(key.equals("check_box_clicknload") && !sharedPreferences.getBoolean(key, true)){
                    stop();
                }
            }
        });
    }

    @Override
    public void run() {
        Socket clientSocket = null;
        BufferedReader in;
        PrintStream out;
        ServerSocket serverSocket = null;

        while (!stopped) {

            try {
                if (serverSocket != null) {
                    serverSocket.close();
                    clientSocket.close();
                }
                serverSocket = new ServerSocket(port);
                clientSocket = serverSocket.accept();

                Log.d(LOGTAG, "Receiving ClickNLoad Event");
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintStream(clientSocket.getOutputStream());
            } catch (IOException e) {
                Log.e(LOGTAG, "Socket could not be opened", e);
                stop();
                break;
            }

            try {
                String input;
                while ((input = in.readLine()) != null) {
                    if (input.startsWith("source")) {
                        List<String> urlList = getURLParamsAsMap(input).get("urls");
                        app.getClient().addPackage("TestName", urlList, Destination.Collector);
                        app.showToast(String.format(getLocalizedResources(app).getString(R.string.clicknload_toast_msg), urlList.size()), Toast.LENGTH_LONG);
                    }
                }
                out.println("success");
            } catch (Exception e) {
                Log.e(LOGTAG, "Data could not be parsed");
                break;
            }
        }
    }

    public static Map<String, List<String>> getURLParamsAsMap(String parameters) throws UnsupportedEncodingException {
        final Map<String, List<String>> parameterMap = new LinkedHashMap<>();
        final String[] pairs = parameters.split("&");
        for (String pair : pairs) {
            final int idx = pair.indexOf("=");
            final String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), "UTF-8") : pair;
            if (!parameterMap.containsKey(key)) {
                parameterMap.put(key, new LinkedList<String>());
            }
            final String[] values = idx > 0 && pair.length() > idx + 1 ? URLDecoder.decode(pair.substring(idx + 1), "UTF-8").split("\\r?\\n") : new String[0];
            for (String value : values) {
                parameterMap.get(key).add(value);
            }
        }
        return parameterMap;
    }

    public void stop() {
        stopped = true;
    }

    Resources getLocalizedResources(Context context) {
        Configuration conf = context.getResources().getConfiguration();
        conf = new Configuration(conf);
        conf.setLocale(Locale.getDefault());
        Context localizedContext = context.createConfigurationContext(conf);
        return localizedContext.getResources();
    }
}
