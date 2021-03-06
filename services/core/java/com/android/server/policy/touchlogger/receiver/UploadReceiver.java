package com.android.server.policy.touchlogger.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import com.android.server.policy.touchlogger.helper.UploadGesturesScheduler;
import com.android.server.policy.touchlogger.task.UploadGesturesTask;

public class UploadReceiver extends BroadcastReceiver {

    private final String TAG = "TouchLogger.network";
    public static final String UPLOAD_GESTURES = "org.leyfer.thesis.UPLOAD_GESTURES";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Handle intent");
        UploadGesturesScheduler.getInstance(context).setNeedUploadGestures(true);
        ConnectivityManager cm =
                (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.isConnected()) {
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                Log.d(TAG, "Send data");
                uploadGestures();
                UploadGesturesScheduler.getInstance(context).setNeedUploadGestures(false);
            }
        }
    }

    private void  uploadGestures() {
        new UploadGesturesTask().execute();
    }
}
