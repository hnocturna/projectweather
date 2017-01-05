package com.example.android.sunshine.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.example.android.sunshine.app.data.WeatherContract;
import com.example.android.sunshine.app.data.WeatherContract.WeatherEntry;
import com.example.android.sunshine.app.data.WeatherContract.LocationEntry;
import com.example.android.sunshine.app.sync.SunshineSyncAdapter;

/**
 * A placeholder fragment containing a simple view.
 */
public class ForecastFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, SharedPreferences.OnSharedPreferenceChangeListener {

    final private String LOG_TAG = ForecastFragment.class.getSimpleName();
    private static final int FORECAST_LOADER = 0;
    private static final String SELECTED_KEY = "SELECTION";

    // Member Variables
    private ForecastAdapter forecastAdapter;
    private int cursorPosition;
    private ListView listView;

    private boolean useTodayLayout = true;

    // Project used when querying data from the database so that column indices are constant. This
    // allows the Cursor.get functions to utilize static variables instead of getColumnIndex.
    public static final String[] FORECAST_COLUMNS = {
            // The ID column needs to be differentiated between the two tables since the query
            // command will utilize an INNER JOIN command to combine both tables.
            WeatherEntry.TABLE_NAME + "." + WeatherEntry._ID,
            WeatherEntry.COLUMN_DATE,
            WeatherEntry.COLUMN_SHORT_DESC,
            WeatherEntry.COLUMN_MAX_TEMP,
            WeatherEntry.COLUMN_MIN_TEMP,
            LocationEntry.COLUMN_LOCATION_SETTING,
            WeatherEntry.COLUMN_WEATHER_ID,
            LocationEntry.COLUMN_COORD_LAT,
            LocationEntry.COLUMN_COORD_LONG
    };

    // Column indices to be used with Cursor when retrieving data from these columns. Tied to
    // FORECAST_COLUMNS and must be changed accordingly when projection is altered.
    static final int COL_WEATHER_ID = 0;
    static final int COL_WEATHER_DATE = 1;
    static final int COL_SHORT_DESC = 2;
    static final int COL_MAX_TEMP = 3;
    static final int COL_MIN_TEMP = 4;
    static final int COL_LOCATION_SETTING = 5;
    static final int COL_WEATHER_CONDITION_ID = 6;
    static final int COL_COORD_LAT = 7;
    static final int COL_COORD_LONG = 8;

    public ForecastFragment() {
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        getLoaderManager().initLoader(FORECAST_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_KEY)) {
            cursorPosition = savedInstanceState.getInt(SELECTED_KEY);
        }

        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (cursorPosition != ListView.INVALID_POSITION) {
            outState.putInt(SELECTED_KEY, cursorPosition);
        }
        super.onSaveInstanceState(outState);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        /* if (id == R.id.action_refresh) {
            // Temporary debug button to test FetchWeatherTask
            updateWeather();
            return true;
        } */
        if (id == R.id.action_map) {
            openPreferredLocationInMap();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        // Get location from user preferences
        String locationSetting = PreferenceManager.getDefaultSharedPreferences(getActivity())
                .getString(getString(R.string.pref_location_key),
                        getString(R.string.pref_location_default)
                );

        // Set sort order: by date, ascending
        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";

        // Build the URI to query the database
        Uri weatherByLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(
                locationSetting,
                System.currentTimeMillis()
        );

        return new CursorLoader(getActivity(),
                weatherByLocationUri,
                FORECAST_COLUMNS,
                null,
                null,
                sortOrder
        );
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        forecastAdapter.swapCursor(null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        forecastAdapter.swapCursor(cursor);
        if (cursorPosition != ListView.INVALID_POSITION) {
            listView.smoothScrollToPosition(cursorPosition);
        }
        updateEmptyView();
    }

    /*
     * Helper method to update the weather and reset the loader
     */
    public void onLocationChanged() {
        updateWeather();
        getLoaderManager().restartLoader(FORECAST_LOADER, null, this);
    }

    /**
     * Updates the empty view attached to the ListView to inform the user why there is no data
     * being displayed
     */
    public void updateEmptyView() {
        if (forecastAdapter.getCount() == 0) {
            // Show message detailing why the Adapter is empty
            TextView emptyText = (TextView) getView().findViewById(R.id.listview_forecast_empty);
            int message = R.string.empty_forecast_list;
            @SunshineSyncAdapter.LocationStatus int locationStatus = Utility.getLocationStatus(getActivity());
            switch (locationStatus) {
                case SunshineSyncAdapter.LOCATION_STATUS_SERVER_DOWN:
                    message = R.string.empty_forecast_list_server_down;
                    break;
                case SunshineSyncAdapter.LOCATION_STATUS_SERVER_INVALID:
                    message = R.string.empty_forecast_list_server_error;
                    break;
                case SunshineSyncAdapter.LOCATION_INVALID:
                    message = R.string.empty_forecast_list_location_invalid;
//                case SunshineSyncAdapter.LOCATION_STATUS_OKAY:
//                    break;
//                case SunshineSyncAdapter.LOCATION_STATUS_UNKNOWN:
//                    break;
                default:
                    if (!Utility.isNetworkAvailable(getActivity())) {
                        message = R.string.empty_forecast_list_no_network;
                    }
            }
            emptyText.setText(message);
        }
    }

    /**
     * Updates the weather utilizing a background service
     */
    private void updateWeather() {
        SunshineSyncAdapter.syncImmediately(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        // Deprecated
//        String[] forecastArray = {
//                "Today - Sunny - 88/63",
//                "Tomorrow - Foggy - 70/46",
//                "Weds - Cloudy - 72/63",
//                "Thurs - Apocaplyptic - 168/-273.12",
//                "Fri - Snowing - 32/17",
//                "Sat - Sleet - 27/12",
//                "Sun - Sunny - 77/65",
//        };
//
//        List<String> weekForecast = new ArrayList<>(Arrays.asList(forecastArray));
//
//        // Retrieve the location from SharedPreferences
//        String locationSetting = Utility.getPreferredLocation(getActivity());
//
//        // Sort order: by date, ascending
//        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";
//
//        // Build the URI for querying the database
//        Uri weatherByLocationAndStartDateUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(locationSetting,
//                System.currentTimeMillis());
//
//        Log.v(LOG_TAG, "URI: " + weatherByLocationAndStartDateUri);
//
//        Cursor cursor = getActivity().getContentResolver().query(weatherByLocationAndStartDateUri,
//                null,
//                null,
//                null,
//                sortOrder
//        );
//
//        Log.v(LOG_TAG, "Cursor found " + cursor.getCount() + " rows");
//        Log.v(LOG_TAG, "sortOrder: " + sortOrder);
//
        forecastAdapter = new ForecastAdapter(getActivity(), null, 0);
        forecastAdapter.setUseTodayLayout(useTodayLayout);

        // Get a reference to the ListView and attach the adapter
        listView = (ListView) rootView.findViewById(R.id.listview_forecast);
        TextView emptyText = (TextView) rootView.findViewById(R.id.listview_forecast_empty);
        listView.setEmptyView(emptyText);
        listView.setAdapter(forecastAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Cursor cursor = (Cursor) adapterView.getItemAtPosition(i);
                cursorPosition = i;
                if (cursor != null) {
                    // If the cursor does not return null, build the URI for the location and date
                    // selected
                    String locationSetting = Utility.getPreferredLocation(getActivity());

                    ((Callback) getActivity()).onItemSelected(WeatherEntry.buildWeatherLocationWithDate(
                            locationSetting,
                            cursor.getLong(COL_WEATHER_DATE)
                    ));
                }

            }
        });

        return rootView;
    }

    public void setUseTodayLayout(boolean useTodayLayout) {
        this.useTodayLayout = useTodayLayout;
        if (forecastAdapter != null) {
            forecastAdapter.setUseTodayLayout(useTodayLayout);
        }
    }

    private void openPreferredLocationInMap() {
        Cursor cursor = forecastAdapter.getCursor();

        if (cursor.moveToFirst()) {
            String posLong = cursor.getString(COL_COORD_LONG);
            String posLat = cursor.getString(COL_COORD_LAT);

            String geo = "geo:" + posLat + "," + posLong;
            Log.v(LOG_TAG, "Map query: " + geo);
            Uri geoLocation = Uri.parse(geo);

            Intent intent = new Intent(Intent.ACTION_VIEW, geoLocation);
            if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Log.d(LOG_TAG, "Couldn't call " + geoLocation.toString() + "; no map application found!");
            }
        }
    }

    public interface Callback {
        void onItemSelected(Uri dateUri);
    }

    @Override
    public void onResume() {
        // Register OnSharedPreferenceChangedListener
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.registerOnSharedPreferenceChangeListener(this);
        super.onResume();
    }

    @Override
    public void onPause() {
        // Unregister OnSharedPreferenceChangedListener
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.pref_location_status_key))) {
            updateEmptyView();
        }
    }
}
