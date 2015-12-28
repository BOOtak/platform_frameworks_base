package com.android.server.policy.touchlogger.helper;

import java.util.ArrayList;
import java.util.List;

public class GestureBuffer {
    private List<String> mGesturesList;
    private int mBufferSize;

    public GestureBuffer(List<String> gesturesList, int bufferSize) {
        mGesturesList = new ArrayList<String>(gesturesList);
        mBufferSize = bufferSize;
    }

    public GestureBuffer(GestureBuffer buffer) {
        this(buffer.getGesturesList(), buffer.getBufferSize());
    }

    public void addGesture(String gesture) {
        mGesturesList.add(gesture);
        mBufferSize += gesture.length();
    }

    public List<String> getGesturesList() {
        return mGesturesList;
    }

    public int getBufferSize() {
        return mBufferSize;
    }

    public void clear() {
        mBufferSize = 0;
        mGesturesList.clear();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (String gesture: mGesturesList) {
            sb.append(gesture);
        }
        return sb.toString();
    }
}
