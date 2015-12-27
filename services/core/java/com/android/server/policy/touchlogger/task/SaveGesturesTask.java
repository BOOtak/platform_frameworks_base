package com.android.server.policy.touchlogger.task;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import com.android.server.policy.touchlogger.helper.GestureBuffer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

public class SaveGesturesTask extends AsyncTask<GestureBuffer, Void, Void> {

    private final String LOG_DATA_DIRNAME = "gesture_data";
    private final String TAG = "TouchLogger/saveGesture";

    @Override
    public Void doInBackground(GestureBuffer... buffers) {
        for (GestureBuffer buffer : buffers) {
            long millis = System.currentTimeMillis();
            String filename = String.format("data_%d.log", millis);

            Environment.setUserRequired(false);
            File dataDirectory = Environment.getDataDirectory();
            File logDataDirectory = new File(String.format("%s/%s", dataDirectory.getAbsolutePath(), LOG_DATA_DIRNAME));
            if (!logDataDirectory.exists()) {
                try {
                    if (!logDataDirectory.mkdirs()) {
                        Log.e(TAG, "Unable to create folder");
                        return null;
                    }
                } catch (SecurityException e) {
                    Log.e(TAG, "checkRead failed, check your perimssions");
                    return null;
                }
            }

            File outputFile = new File(logDataDirectory, filename);
            FileOutputStream outputStream;
            try {
                outputStream = new FileOutputStream(outputFile);

                try {
                    outputStream.write(buffer.toString().getBytes(Charset.defaultCharset()));
                    outputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, String.format("Unable to write data to file: %s", e.getMessage()));
                }
            } catch (FileNotFoundException e) {
                Log.e(TAG, String.format("Unable to find file: %s", e.getMessage()));
            }

            if (isCancelled()) {
                break;
            }
        }
        return null;
    }
}
