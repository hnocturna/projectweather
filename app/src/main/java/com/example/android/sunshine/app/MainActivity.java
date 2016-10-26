package com.example.android.sunshine.app;

import android.content.Intent;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;


public class MainActivity extends ActionBarActivity implements ForecastFragment.Callback{
    private final String LOG_TAG = MainActivity.class.getSimpleName();
    private final String DETAILFRAGMENT_TAG = "DFTAG";
    private String location;
    private boolean twoPane;
    private Uri uri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.weather_detail_container, new DetailsFragment(), DETAILFRAGMENT_TAG)
                        .commit();
            }
            ForecastFragment forecastFragment = (ForecastFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.fragment_forecast);
            forecastFragment.setUseTodayLayout(!twoPane);
        }
        location = Utility.getPreferredLocation(this);
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
        if (id == R.id.action_map) {
            openPreferredLocationInMap();
        }

        return super.onOptionsItemSelected(item);
    }

    private void openPreferredLocationInMap() {
        String location = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(getString(R.string.pref_location_key),
                        getString(R.string.pref_location_default)
                );
        Uri geoLocation = Uri.parse("geo:0,0").buildUpon()
                .appendQueryParameter("q", location)
                .build();

        Intent intent = new Intent(Intent.ACTION_VIEW, geoLocation);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Log.d(LOG_TAG, "Couldn't call " + location + "; no map application found!");
        }

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
            detailsFragment.onLocationChanged(Utility.getPreferredLocation(this));

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
            Intent intent = new Intent(this, DetailsActivity.class)
                    .setData(dateUri);
            startActivity(intent);
        }
    }
}
