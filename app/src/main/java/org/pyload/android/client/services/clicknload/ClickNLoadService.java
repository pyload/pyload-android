package org.pyload.android.client.services.clicknload;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.pyload.android.client.pyLoadApp;

import java.util.concurrent.Executors;

public class ClickNLoadService extends Service {

    private final static String LOGTAG = "ClickNLoad Service";

    private ClickNLoadTask clickNLoadTask;
    private int port;

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {

        if (intent != null) {
            String action = intent.getAction();
            port = intent.getExtras().getInt("port");
            switch(action) {
                case "START": startService(); break;
                case "STOP": stopService(); break;
                default: Log.d(LOGTAG, "This should never happen. No action in the received intent");
            }
        } else {
            Log.d(LOGTAG, "with a null intent. It has been probably restarted by the system.");
        }

        return Service.START_STICKY;
    }

    private void stopService() {
        clickNLoadTask.stop();
    }

    public void startService() {
        clickNLoadTask = new ClickNLoadTask(port, (pyLoadApp) getApplicationContext());
        Executors.newSingleThreadExecutor().submit(clickNLoadTask);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
