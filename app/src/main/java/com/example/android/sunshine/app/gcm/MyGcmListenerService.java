package com.example.android.sunshine.app.gcm;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.example.android.sunshine.app.MainActivity;
import com.example.android.sunshine.app.R;
import com.google.android.gms.gcm.GcmListenerService;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by hnoct on 1/11/2017.
 */

public class MyGcmListenerService extends GcmListenerService {
    // Constants
    private static final String TAG = "MyGcmListenerService";

    private static final String EXTRA_DATA = "data";
    private static final String EXTRA_WEATHER = "weather";
    private static final String EXTRA_LOCATION = "location";

    public static final int NOTIFICATION_ID = 1;

    @Override
    public void onMessageReceived(String from, Bundle data) {
        Log.v(TAG, "In onMessageReceived");
        if (!data.isEmpty()) {
            Log.i(TAG, "Received: " + data.toString());
            try {
                String senderId = getString(R.string.gcm_defaultSenderId);
                if (senderId.length() == 0) {
                    Toast.makeText(this, "SenderID string needs to be set", Toast.LENGTH_LONG).show();
                }
                // Ensure that message is coming from the correct server
                if (senderId.equals(from)) {
                    // Retrieve the data from the JSONObject sent by GCM and create an appropriate alert
                    // JSONObject jsonData = new JSONObject(data.getString(EXTRA_DATA));
                    String weather = data.getString(EXTRA_WEATHER);
                    String location = data.getString(EXTRA_LOCATION);

                    String alert = String.format(getString(R.string.gcm_weather_alert), weather, location);

                    // Create notification from the alert
                    sendNotification(alert);
                }
            } catch (Exception e) {
                // Don't care if nothing is retrieved from GCM since it's not core functionality
            }
            Log.i(TAG, "Received: " + data.toString());
        }
    }

    /**
     * Helper method for creating and firing the notification utilizing the alert message from GCM
     * @param message alert message parsed from JSON data from GCM
     */
    public void sendNotification(String message) {
        // Get the NotificationManager service that will fire the notification
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        PendingIntent contentIntent =
                PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);

        // Utilize app icon as small icon so that users understand where the notification is coming
        // from and a bitmap as the large icon. Storm will be used in this case to infer severity
        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.drawable.art_storm);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.art_clear)
                .setLargeIcon(largeIcon)
                .setContentTitle("Severe Weather Alert!")
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(contentIntent);

        // Fire the notification
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }
}
