package com.example.android.sunshine.app.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

/**
 * Created by hnoct on 11/9/2016.
 */

public class SunshineAuthenticatorService extends Service {
    // Instance field to store authenticator object
    private SunshineAuthenticator authenticator;

    @Override
    public void onCreate() {
        authenticator = new SunshineAuthenticator(this);
    }

    /**
     * When system binds to this Service to make the RPC call, return the authenticator's IBinder.
     * @param intent
     * @return Gets the Binder thread for the SunshineAuthenticator. Binder threads are replicas
     * of the thread used for the Service since multiple processes could be trying to access
     * information from the Service
     */
    @Override
    public IBinder onBind(Intent intent) {
        return authenticator.getIBinder();
    }
}
