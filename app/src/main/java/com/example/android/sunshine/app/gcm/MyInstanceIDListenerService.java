package com.example.android.sunshine.app.gcm;

import android.content.Intent;
import com.google.android.gms.iid.InstanceIDListenerService;

/**
 * Created by hnoct on 1/11/2017.
 */

public class MyInstanceIDListenerService extends InstanceIDListenerService {
    private static final String TAG = "MyInstanceIDLS";

    /**
     * Called if InstanceID token has been updated. This may occur if the security of the previous
     * token has been compromised. This call is initiated by InstanceID provider
     */
    @Override
    public void onTokenRefresh() {
        Intent intent = new Intent(this, RegistrationIntentService.class);
        startService(intent);
    }
}
