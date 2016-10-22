package com.example.android.sunshine.app;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.android.sunshine.app.data.WeatherContract.WeatherEntry;

import org.apache.http.impl.DefaultHttpServerConnection;

public class DetailsActivity extends ActionBarActivity {
    private static String forecastStr;
    private static final String LOG_TAG = DetailsActivity.class.getSimpleName();
    private static final String FORECAST_SHARE_HASHTAG = "#SunShineApp";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new DetailFragment())
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.detailfragment, menu);
        // Get share menu item then attach the ShareActionProvider to set the intent
        MenuItem menuItem = menu.findItem(R.id.action_share);
        ShareActionProvider shareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);

        // Attach an intent to the ShareActionProvider
        if (shareActionProvider != null) {
            shareActionProvider.setShareIntent(createShareIntent());
        } else {
            Log.d(LOG_TAG, "Share Action Provider is null?");
        }

        return true;
    }

    /*
     * Helper method for creating the Share Intent
     */
    private Intent createShareIntent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET)    // Sets the intent to return to the app one the share action is complete
                .setType("text/plain")                                      // Tells Android that you are sharing plain text
                .putExtra(Intent.EXTRA_TEXT, forecastStr + " " + FORECAST_SHARE_HASHTAG);
        return shareIntent;
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

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class DetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
        private static final String LOG_TAG = DetailFragment.class.getSimpleName();
        private static final int DETAILS_LOADER = 0;

        private static final String[] DETAILS_COLUMNS = new String[]{
                WeatherEntry.TABLE_NAME + "." + WeatherEntry._ID,
                WeatherEntry.COLUMN_DATE,
                WeatherEntry.COLUMN_SHORT_DESC,
                WeatherEntry.COLUMN_MIN_TEMP,
                WeatherEntry.COLUMN_MAX_TEMP
        };

        static final int COL_WEATHER_ID = 0;
        static final int COL_DATE = 1;
        static final int COL_WEATHER_SHORT_DESC = 2;
        static final int COL_WEATHER_MIN = 3;
        static final int COL_WEATHER_MAX = 4;

        public DetailFragment() {
            setHasOptionsMenu(true);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            View rootView = inflater.inflate(R.layout.fragment_detail, container, false);


//            // Deprecated
//
//            Intent intent = getActivity().getIntent();
//            Uri weatherUri = null;
//
//            if (intent != null) {
//                weatherUri = intent.getData();
//                forecastStr = weatherUri.toString();
//            } else {
//                return rootView;
//            }
//
//            forecastTextView = (TextView) rootView.findViewById(R.id.detail_text);
//            forecastTextView.setText(forecastStr);

            return rootView;
        }

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            Log.v(LOG_TAG, "In onCreateLoader");
            // Retrieve the URI from the intent passed from the ForecastFragment
            Intent intent = getActivity().getIntent();
            Uri weatherWithLocationAndDateUri = intent.getData();

            // Return CursorLoader with the URI and the projection created in the DetailsFragment
            return new CursorLoader(
                    getActivity(),
                    weatherWithLocationAndDateUri,
                    DETAILS_COLUMNS,
                    null,
                    null,
                    null
            );


        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
            Log.v(LOG_TAG, "In onLoadFinished");
            Log.v(LOG_TAG, "Row returned by cursor: " + cursor.moveToFirst());
            // If cursor doesn't return any data, finish
            if (!cursor.moveToFirst()) {
                return;
            }

            // The cursor data needs to be converted to user-readable String using the methods in
            // Utility.class
            boolean isMetric = Utility.isMetric(getActivity());
            String date = Utility.formatDate(cursor.getLong(COL_DATE));
            String description = cursor.getString(COL_WEATHER_SHORT_DESC);
            String highTemperatures = Utility.formatTemperature(
                    cursor.getDouble(COL_WEATHER_MAX),
                    isMetric
            );
            String lowTemperatures = Utility.formatTemperature(
                    cursor.getDouble(COL_WEATHER_MIN),
                    isMetric
            );

            String weatherString = String.format("%s - %s - %s/%s", date, description, highTemperatures, lowTemperatures);

            // Set the TextView in the DetailsFragment to the String that was just generated
            TextView textView = (TextView) getView().findViewById(R.id.detail_text);
            textView.setText(weatherString);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {

        }

        @Override
        public void onActivityCreated(@Nullable Bundle savedInstanceState) {
            getLoaderManager().initLoader(DETAILS_LOADER, null, this);
            super.onActivityCreated(savedInstanceState);
        }
    }

}
