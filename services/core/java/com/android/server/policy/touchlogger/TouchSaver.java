package com.android.server.policy.touchlogger;

import android.content.Context;
import com.android.server.policy.touchlogger.helper.GestureBuffer;
import com.android.server.policy.touchlogger.task.SaveGesturesTask;

import org.json.JSONObject;

public class TouchSaver {
    private static TouchSaver ourInstance;
    private final int maxBufferSize = 2 << 17;
    private final Context mContext;

    private GestureBuffer gestureBuffer = new GestureBuffer();

    private TouchSaver(Context mContext) {
        this.mContext = mContext;
    }

    public static TouchSaver getInstance(Context context) {
        if (ourInstance == null) {
            ourInstance = new TouchSaver(context);
        }
        return ourInstance;
    }

    public void saveGesture(JSONObject gesture) {
        gestureBuffer.addGesture(gesture);
        if (gestureBuffer.getBufferSize() >= maxBufferSize) {
            new SaveGesturesTask(mContext).doInBackground(new GestureBuffer(gestureBuffer));
            gestureBuffer.clear();
        }
    }
}
