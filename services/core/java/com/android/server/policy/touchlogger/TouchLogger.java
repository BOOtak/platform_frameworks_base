package com.android.server.policy.touchlogger;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.WindowManagerPolicy;
import com.android.server.policy.touchlogger.receiver.ConnectivityReceiver;
import com.android.server.policy.touchlogger.receiver.UploadReceiver;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;


public class TouchLogger implements WindowManagerPolicy.PointerEventListener {
    public static final String TAG = "TouchLogger";
    public static final String LOG_DATA_DIRNAME = "gesture_data";


    private final Context mContext;
    private JSONObject mGesture;
    private JSONArray mEvents;
    private long mStartGestureTime;
    private UploadReceiver mUploadReceiver;

    private final Map<Integer, String>mPrefixMap = new HashMap<Integer, String>() {{
        put(MotionEvent.ACTION_DOWN, "DOWN");
        put(MotionEvent.ACTION_UP, "UP");
        put(MotionEvent.ACTION_MOVE, "MOVE");
        put(MotionEvent.ACTION_CANCEL, "CANCEL");
        put(MotionEvent.ACTION_OUTSIDE, "OUTSIDE");
        put(MotionEvent.ACTION_POINTER_DOWN, "POINTER_DOWN");
        put(MotionEvent.ACTION_POINTER_UP, "POINTER_UP");
        put(MotionEvent.ACTION_HOVER_MOVE, "HOVER_MOVE");
        put(MotionEvent.ACTION_HOVER_ENTER, "HOVER_ENTER");
        put(MotionEvent.ACTION_HOVER_EXIT, "HOVER_EXIT");
        put(MotionEvent.ACTION_SCROLL, "SCROLL");
    }};

    public TouchLogger(Context context) {
        mContext = context;
        mGesture = new JSONObject();
        mEvents = new JSONArray();

        setAlarm(context);
        registerNetworkReceiver(context);
    }

    private void setAlarm(Context context) {
        IntentFilter filter = new IntentFilter(UploadReceiver.UPLOAD_GESTURES);
        mUploadReceiver = new UploadReceiver();
        context.registerReceiver(mUploadReceiver, filter);

        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent uploadGesturesIntent = new Intent(UploadReceiver.UPLOAD_GESTURES);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, uploadGesturesIntent, 0);

        // TODO: make more reasonable periods
        alarmMgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME,
                30 * 60 * 1000,
                30 * 60 * 1000,
                pendingIntent);

        Log.d(TAG, "Start alarm clock");
    }

    private void registerNetworkReceiver(Context context) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

        BroadcastReceiver connectivityReceiver = new ConnectivityReceiver();
        context.registerReceiver(connectivityReceiver, filter);
    }

    @Override
    public void onPointerEvent(MotionEvent event) {
        int pointerCount = event.getPointerCount();
        int rawAction = event.getAction();
        int action = rawAction & MotionEvent.ACTION_MASK;

        try {
            String prefix = mPrefixMap.get(action);
            if (prefix == null) {
                prefix = Integer.toString(rawAction);
            }

            JSONObject motionEvent = new JSONObject();
            motionEvent.put("prefix", prefix);
            motionEvent.put("timestamp", System.currentTimeMillis());
            if (action == MotionEvent.ACTION_POINTER_DOWN || action == MotionEvent.ACTION_POINTER_UP) {
                motionEvent.put("action_pointer_index", (rawAction & MotionEvent.ACTION_POINTER_INDEX_MASK)
                        >> MotionEvent.ACTION_POINTER_INDEX_SHIFT);
            }

            JSONArray pointers = new JSONArray();
            for (int i = 0; i < pointerCount; i++) {
                final int id = event.getPointerId(i);
                final MotionEvent.PointerCoords coords = new MotionEvent.PointerCoords();
                event.getPointerCoords(i, coords);

                JSONObject pointer = new JSONObject()
                        .put("index", i)
                        .put("id", id)
                        .put("x", coords.x)
                        .put("y", coords.y)
                        .put("pressure", coords.pressure);
                pointers.put(pointer);
            }

            motionEvent.put("pointers", pointers);
            mEvents.put(motionEvent);

            if (action == MotionEvent.ACTION_DOWN) {
                mStartGestureTime = System.currentTimeMillis();
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                mGesture.put("timestamp", System.currentTimeMillis());
                mGesture.put("length", System.currentTimeMillis() - mStartGestureTime);
                mGesture.put("events", mEvents);
                TouchSaver.getInstance(mContext).saveGesture(mGesture);
                mEvents = new JSONArray();
                mGesture = new JSONObject();
            }
        } catch (JSONException e) {
            Log.e(TAG, String.format("Unable to add build gesture: %s", e.toString()));
        }
    }
}
