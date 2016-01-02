package com.android.server.policy.touchlogger.task;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;
import com.android.server.policy.touchlogger.TouchLogger;
import com.android.server.policy.touchlogger.helper.GestureBuffer;

import javax.crypto.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import org.json.JSONObject;
import org.json.JSONException;

public class SaveGesturesTask extends AsyncTask<GestureBuffer, Void, Void> {

    private final String TAG = "TouchLogger/saveGesture";
    private final Context mContext;

    private final String publicKeyPem =
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA3RTlanDGDOcGuDp/SQBc" +
            "e4Qi3IipasyS7gk0JLMDYWB9Qql6/By7d2enhErMAGTnPcA2mIaJdINAFO+rXcw/" +
            "ANQ158XhqFRn+zKXdpw2nw8SV9s1iZEY33Wg8NNXKA2g6bwPXfywVEaQVM2lePW7" +
            "MY9Sdus7w9cdtOUv+DYAZouZt1u3F0sKkvxFaGxVQYYvV6CbosAM8lnZzzYIaid/" +
            "z6lhviBxN+q+nq2aDDxwkOJvaO+oWN/WI/aq66pVV3Xvp4+l86P4B3BNbFIci/U5" +
            "fuQfxKF1QCSB1R/yj/BEhojAAFQuOEPpTNAwRyBeyS0yEjIzShdwmDlraCexrpcH" +
            "oQIDAQAB";

    public SaveGesturesTask(Context context) {
        mContext = context;
    }

    @Override
    public Void doInBackground(GestureBuffer... buffers) {
        for (GestureBuffer buffer : buffers) {
            long millis = System.currentTimeMillis();
            String filename = String.format("data_%d.log", millis);

            Environment.setUserRequired(false);
            File dataDirectory = Environment.getDataDirectory();
            File logDataDirectory = new File(String.format("%s/%s",
                    dataDirectory.getAbsolutePath(), TouchLogger.LOG_DATA_DIRNAME));
            if (!logDataDirectory.exists()) {
                if (!logDataDirectory.mkdirs()) {
                    Log.e(TAG, "Unable to create folder");
                    return null;
                }
            }

            byte[] touchData = buffer.toString().getBytes(Charset.defaultCharset());
            String IV;
            String encData;
            String encryptedKey;
            try {
                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                SecretKey sessionKey = generateSessionKey();
                cipher.init(Cipher.ENCRYPT_MODE, sessionKey);
                IV = Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP);
                encData = Base64.encodeToString(cipher.doFinal(touchData), Base64.NO_WRAP);
                PublicKey publicKey = getPublicKey();
                encryptedKey = Base64.encodeToString(encryptData(publicKey, sessionKey.getEncoded()), Base64.NO_WRAP);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
                return null;
            }

            JSONObject data = new JSONObject();
            try {
                data.put("device_id", Settings.Secure.getString(mContext.getContentResolver(),
                        Settings.Secure.ANDROID_ID))
                        .put("sessionkey", encryptedKey)
                        .put("iv", IV)
                        .put("data", encData);
            } catch (JSONException e) {
                Log.e(TAG, "Unable to form data, exiting.");
                return null;
            }

            File outputFile = new File(logDataDirectory, filename);
            FileOutputStream outputStream;
            try {
                outputStream = new FileOutputStream(outputFile);
                outputStream.write(data.toString().getBytes(Charset.defaultCharset()));
                outputStream.close();
            } catch (FileNotFoundException e) {
                Log.e(TAG, String.format("Unable to find file: %s", e.getMessage()));
            } catch (IOException e) {
                Log.e(TAG, String.format("Unable to write data to file: %s", e.getMessage()));
            }

            if (isCancelled()) {
                break;
            }
        }
        return null;
    }

    private PublicKey getPublicKey() throws InvalidKeySpecException, NoSuchAlgorithmException {
        byte [] encoded = Base64.decode(publicKeyPem, Base64.NO_WRAP);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(keySpec);
    }

    private byte[] encryptData(PublicKey publicKey, byte[] data) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(data);
    }

    private SecretKey generateSessionKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128);
        return keyGen.generateKey();
    }
}
