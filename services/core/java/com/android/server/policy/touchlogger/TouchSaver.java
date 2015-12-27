package com.android.server.policy.touchlogger;

import com.android.server.policy.touchlogger.helper.GestureBuffer;
import com.android.server.policy.touchlogger.task.SaveGesturesTask;

public class TouchSaver {
    private static TouchSaver ourInstance = new TouchSaver();
    private final int maxBufferSize = 2 << 17;

    private GestureBuffer gestureBuffer = new GestureBuffer();

    public static TouchSaver getInstance() {
        return ourInstance;
    }

    private TouchSaver() {
    }

    public void saveGesture(String gesture) {
        gestureBuffer.addGesture(gesture);
        if (gestureBuffer.getBufferSize() >= maxBufferSize) {
            new SaveGesturesTask().doInBackground(gestureBuffer);
            gestureBuffer.clear();
        }
    }
}
