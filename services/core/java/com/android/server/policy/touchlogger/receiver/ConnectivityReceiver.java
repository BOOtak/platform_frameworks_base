package com.android.server.policy.touchlogger.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import com.android.server.policy.touchlogger.helper.UploadGesturesScheduler;
import com.android.server.policy.touchlogger.service.UploadReceiver;

public class ConnectivityReceiver extends BroadcastReceiver {

    private final long SLEEP_TIMEOUT = 500;  // ms
    private final String TAG = "TouchLogger.receiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            ConnectivityManager cm =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            if (activeNetwork != null && activeNetwork.isConnectedOrConnecting()) {
                if (UploadGesturesScheduler.getInstance(context).needUploadGestures()) {
                    if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                        try {
                            while (!activeNetwork.isConnected()) {
                                Thread.sleep(SLEEP_TIMEOUT);
                            }
                        } catch (InterruptedException e) {
                            Log.w(TAG, "Interrupted. Trying to send logs anyway.");
                        }
                        Intent sendLogsIntent = new Intent(context, UploadReceiver.class);
                        context.startService(sendLogsIntent);
                    }
                }
            }
        }
    }
}
