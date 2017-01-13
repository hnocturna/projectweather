package com.example.android.sunshine.app.gcm;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.example.android.sunshine.app.MainActivity;
import com.example.android.sunshine.app.R;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import java.io.IOException;

/**
 * Created by hnoct on 1/11/2017.
 */

public class RegistrationIntentService extends IntentService {
    private static final String TAG = "RegIntentService";

    public RegistrationIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        try {
            // "synchronized" ensures that if multiple refresh operations occur simultaneously,
            // they are run sequentially
            synchronized(TAG) {
                // Initial call goes to network to retrieve token, subsequent calls will be local
                InstanceID instanceID = InstanceID.getInstance(this);
                instanceID.deleteInstanceID();
                instanceID = InstanceID.getInstance(this);
                // Retrieve token
                // gcm_defaultSenderId comes from API console
                String senderId = getString(R.string.gcm_defaultSenderId);
                if (senderId.length() != 0) {
                    String token = instanceID.getToken(
                            senderId,
                            GoogleCloudMessaging.INSTANCE_ID_SCOPE,
                            null
                    );
                    prefs.edit().putString("token", token).apply();
                    sendRegistrationToServer(token);
                }

                // Store boolean in SharedPreferences to let the rest of the application know that
                // token has been successfully retrieved
                prefs.edit().putBoolean(MainActivity.SENT_TOKEN_TO_SERVER, true).apply();
            }
        } catch (IOException e) {
            Log.d(TAG, "Failed to complete token refresh", e);

            // If failed to get token, ensure that the app will try again at a later time by setting
            // the boolean to false
            prefs.edit().putBoolean(MainActivity.SENT_TOKEN_TO_SERVER, false).apply();
        }
    }

    private void sendRegistrationToServer(String token) {
        Log.i(TAG, "GCM Registration Token: " + token);
    }
}
