package com.example.android.sunshine.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.example.android.sunshine.app.data.WeatherContract;
import com.example.android.sunshine.app.gcm.RegistrationIntentService;
import com.example.android.sunshine.app.sync.SunshineSyncAdapter;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;


public class MainActivity extends ActionBarActivity implements ForecastFragment.Callback{
    // Constants
    private final String LOG_TAG = MainActivity.class.getSimpleName();
    private final String DETAILFRAGMENT_TAG = "DFTAG";
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    public static final String SENT_TOKEN_TO_SERVER = "sentTokenToServer";

    // Member Variables
    private String location;
    public static boolean twoPane;
    private Uri uri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set the custom Material toolbar as the ActionBar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Apply styling to Toolbar
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        // Check whether there are two panes in the MainActivity (the layout will be decided based
        // on the smallest width of the device.
        if (findViewById(R.id.weather_detail_container) == null) {
            // If there is one pane, nothing needs to be done except set the boolean to false
            twoPane = false;
            getSupportActionBar().setElevation(0f);
        } else {
            // If there are two panes:
            // Set the boolean to true
            twoPane = true;
            // Populate the container with a new DetailsFragment
            if (savedInstanceState == null) {       // If the screen is rotated, the system will automatically restore the DetailsFragment that was there before the rotation
                // Otherwise, set DetailFragment to today's details
                DetailsFragment detailsFragment = new DetailsFragment();
                Cursor cursor = getContentResolver().query(WeatherContract.WeatherEntry.CONTENT_URI,
                        null,
                        null,
                        null,
                        WeatherContract.WeatherEntry.COLUMN_DATE + " ASC"
                );
                if (cursor.moveToFirst()) {
                    // If database is populated, then select today, otherwise, open a blank DetailsFragment
                    long todayDate = cursor.getLong(cursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_DATE));
                    Uri todayUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(
                            Utility.getPreferredLocation(this),
                            todayDate
                    );
                    Bundle args = new Bundle();
                    args.putParcelable(DetailsFragment.DETAIL_URI, todayUri);
                    detailsFragment.setArguments(args);
                }
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.weather_detail_container, detailsFragment, DETAILFRAGMENT_TAG)
                        .commit();
            }
            ForecastFragment forecastFragment = (ForecastFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.fragment_forecast);
            forecastFragment.setUseTodayLayout(!twoPane);
        }
        location = Utility.getPreferredLocation(this);

        SunshineSyncAdapter.initializeSyncAdapter(this);

        if (checkPlayServices()) {
            // Check if app is already registered
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

            // Check if token was retrieved
            boolean sentToken = prefs.getBoolean(SENT_TOKEN_TO_SERVER, false);
            if (!sentToken) {
                // If token was not retrieved, attempt to register app
                Intent intent = new Intent(this, RegistrationIntentService.class);
                startService(intent);
            } else {
                String token = prefs.getString("token", "");
                Log.i(LOG_TAG, "GCM Registration Token: " + token);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        // If the stored location is different than the location in the SharedPreferences, update
        // the data being displayed with the new location and then set the current location to the
        // one stored in the SharedPreferences
        if (!location.equals(Utility.getPreferredLocation(this))) {
            // Call the onLocationChanged method in the instanced ForecastFragment to update the
            // weather
            ForecastFragment forecastFragment = (ForecastFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.fragment_forecast);
            if (forecastFragment != null) {
                forecastFragment.onLocationChanged();
            }
            DetailsFragment detailsFragment = (DetailsFragment) getSupportFragmentManager()
                    .findFragmentByTag(DETAILFRAGMENT_TAG);
            if (detailsFragment != null) {
                detailsFragment.onLocationChanged(Utility.getPreferredLocation(this));
            }
            // Set the current location to the location in SharedPreferences
            location = Utility.getPreferredLocation(this);
        }
        super.onResume();
    }

    /**
     * Callback interface from ForecastFragment that passes the data from the item selected back
     * to the MainActivity so that it can correctly pass the data to either the DetailsFragment
     * in the master/detail flow or start a new DetailsActivity depending on whether the app
     * is in two-pane or one-pane mode
     *
     * @param dateUri The URI of the date and location selected in the ForecastFragment
      */
    @Override
    public void onItemSelected(Uri dateUri) {
        uri = dateUri;
        if (twoPane) {
            // Pass the URI as a Bundle argument. Bundle arguments cannot be changed once they have
            // been passed. This is useful because if the fragment is destroyed on rotation, the
            // bundle will remain the same
            Bundle args = new Bundle();
            args.putParcelable(DetailsFragment.DETAIL_URI, dateUri);

            DetailsFragment detailsFragment = new DetailsFragment();
            detailsFragment.setArguments(args);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.weather_detail_container, detailsFragment, DETAILFRAGMENT_TAG)
                    .commit();
        } else {
            // If single pane, start a new DetailsActivity by passing in the URI through the intent
            Log.v(LOG_TAG, "twoPane is false?");
            Intent intent = new Intent(this, DetailsActivity.class)
                    .setData(dateUri);
            startActivity(intent);
        }
    }

    /**
     * Helper method for checking whether Google Play Services is installed on the users phone so
     * that Google Cloud Messaging features can be enabled.
     * @return true if GPS is installed, false if it is not installed
     */
    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(LOG_TAG, "Google Play Services not installed. This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }
}
