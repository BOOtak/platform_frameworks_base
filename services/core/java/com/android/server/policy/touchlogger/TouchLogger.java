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


    private final FastStringBuilder mText = new FastStringBuilder();
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
        alarmMgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                60 * 1000,
                60 * 1000,
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

//            logCoords(action, i, coords, id, event);
            }

            motionEvent.put("pointers", pointers);
            mEvents.put(motionEvent);

            if (action == MotionEvent.ACTION_DOWN) {
                mStartGestureTime = System.currentTimeMillis();
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                mGesture.put("timestamp", System.currentTimeMillis());
                mGesture.put("length", System.currentTimeMillis() - mStartGestureTime);
                mGesture.put("events", mEvents);
                String logString = mGesture.toString();
                Log.d(TAG, String.valueOf(logString.length()));
                TouchSaver.getInstance(mContext).saveGesture(logString);
                mEvents = new JSONArray();
                mGesture = new JSONObject();
            }
//            Log.d(TAG, motionEvent.toString());
        } catch (JSONException e) {
            Log.e(TAG, String.format("Unable to add build gesture: %s", e.toString()));
        }
    }

    private void logCoords(int action, int index, MotionEvent.PointerCoords coords, int id, MotionEvent event) {
        final int toolType = event.getToolType(index);
        final int buttonState = event.getButtonState();
        final String prefix;
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                prefix = "DOWN";
                break;
            case MotionEvent.ACTION_UP:
                prefix = "UP";
                break;
            case MotionEvent.ACTION_MOVE:
                prefix = "MOVE";
                break;
            case MotionEvent.ACTION_CANCEL:
                prefix = "CANCEL";
                break;
            case MotionEvent.ACTION_OUTSIDE:
                prefix = "OUTSIDE";
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                if (index == ((action & MotionEvent.ACTION_POINTER_INDEX_MASK)
                        >> MotionEvent.ACTION_POINTER_INDEX_SHIFT)) {
                    prefix = "DOWN";
                } else {
                    prefix = "MOVE";
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
                if (index == ((action & MotionEvent.ACTION_POINTER_INDEX_MASK)
                        >> MotionEvent.ACTION_POINTER_INDEX_SHIFT)) {
                    prefix = "UP";
                } else {
                    prefix = "MOVE";
                }
                break;
            case MotionEvent.ACTION_HOVER_MOVE:
                prefix = "HOVER MOVE";
                break;
            case MotionEvent.ACTION_HOVER_ENTER:
                prefix = "HOVER ENTER";
                break;
            case MotionEvent.ACTION_HOVER_EXIT:
                prefix = "HOVER EXIT";
                break;
            case MotionEvent.ACTION_SCROLL:
                prefix = "SCROLL";
                break;
            default:
                prefix = Integer.toString(action);
                break;
        }

        Log.i(TAG, mText.clear()
                .append(" id ").append(id + 1)
                .append(": ")
                .append(prefix)
                .append(" (").append(coords.x, 3).append(", ").append(coords.y, 3)
                .append(") Pressure=").append(coords.pressure, 3)
                .append(" Size=").append(coords.size, 3)
                .append(" TouchMajor=").append(coords.touchMajor, 3)
                .append(" TouchMinor=").append(coords.touchMinor, 3)
                .append(" ToolMajor=").append(coords.toolMajor, 3)
                .append(" ToolMinor=").append(coords.toolMinor, 3)
                .append(" Orientation=").append((float)(coords.orientation * 180 / Math.PI), 1)
                .append("deg")
                .append(" Tilt=").append((float)(
                        coords.getAxisValue(MotionEvent.AXIS_TILT) * 180 / Math.PI), 1)
                .append("deg")
                .append(" Distance=").append(coords.getAxisValue(MotionEvent.AXIS_DISTANCE), 1)
                .append(" VScroll=").append(coords.getAxisValue(MotionEvent.AXIS_VSCROLL), 1)
                .append(" HScroll=").append(coords.getAxisValue(MotionEvent.AXIS_HSCROLL), 1)
                .append(" BoundingBox=[(")
                .append(event.getAxisValue(MotionEvent.AXIS_GENERIC_1), 3)
                .append(", ").append(event.getAxisValue(MotionEvent.AXIS_GENERIC_2), 3).append(")")
                .append(", (").append(event.getAxisValue(MotionEvent.AXIS_GENERIC_3), 3)
                .append(", ").append(event.getAxisValue(MotionEvent.AXIS_GENERIC_4), 3)
                .append(")]")
                .append(" ToolType=").append(MotionEvent.toolTypeToString(toolType))
                .append(" ButtonState=").append(MotionEvent.buttonStateToString(buttonState))
                .toString());
    }
}
