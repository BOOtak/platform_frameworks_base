package com.android.server.policy.touchlogger;

import java.util.ArrayList;
import java.util.List;

public class GestureBuffer {
    private List<String> mGestures = new ArrayList<String>();
    private int bufferSize = 0;

    public void addGesture(String gesture) {
        mGestures.add(gesture);
        bufferSize += gesture.length();
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public void clear() {
        bufferSize = 0;
        mGestures.clear();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (String gesture: mGestures) {
            sb.append(gesture);
        }
        return sb.toString();
    }
}
