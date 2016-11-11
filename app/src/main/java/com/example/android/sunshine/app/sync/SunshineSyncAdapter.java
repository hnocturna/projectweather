package com.example.android.sunshine.app.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;

import com.example.android.sunshine.app.BuildConfig;
import com.example.android.sunshine.app.R;
import com.example.android.sunshine.app.Utility;
import com.example.android.sunshine.app.data.WeatherContract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Vector;

/**
 * Created by hnoct on 11/9/2016.
 */

public class SunshineSyncAdapter extends AbstractThreadedSyncAdapter {
    private final String LOG_TAG = SunshineSyncAdapter.class.getSimpleName();
    private Context context;

    // Interval at which to sync with weather, in seconds (60 seconds x 180 min = 3 hours)
    public static final int SYNC_INTERVAL = 60 * 180;
    public static final int SYNC_FLEXTIME = SYNC_INTERVAL / 3;

    public SunshineSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        this.context = context;
    }

    /**
     * Method that is called when sync occurs, updating the weather database with the data from
     * OpenWeatherMap
     */
    @Override
    public void onPerformSync(Account account, Bundle bundle, String s, ContentProviderClient contentProviderClient, SyncResult syncResult) {
        Log.d(LOG_TAG, "onPerformSync called.");
        String locationQuery = Utility.getPreferredLocation(context);

        // Defined outside of the try-catch block so they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        // Will contain the raw JSON response as a string.
        String forecastJsonStr = null;

        // Additional variables that will temporarily be held here until they are user configurable
        String format = "json";
        String units = "metric";
        int numDays = 14;

        try {
            // Construct the URL for the OpenWeatherMap query
            // Possible parameters are available at OWM's forecast API page at:
            // http://openweathermap.org/API#forecast
            final String QUERY_PARAM = "q";
            final String FORMAT_PARAMS = "mode";
            final String UNITS_PARAM = "units";
            final String DAYS_PARAMS = "cnt";
            final String API_PARAM = "APPID";

            Uri forecastUri = Uri.parse("http://api.openweathermap.org/data/2.5/forecast/daily?")
                    .buildUpon()
                    .appendQueryParameter(QUERY_PARAM, locationQuery)
                    .appendQueryParameter(FORMAT_PARAMS, format)
                    .appendQueryParameter(UNITS_PARAM, units)
                    .appendQueryParameter(DAYS_PARAMS, Integer.toString(numDays))
                    .appendQueryParameter(API_PARAM, BuildConfig.OPEN_WEATHER_MAP_API_KEY)
                    .build();

            URL url = new URL(forecastUri.toString());

            // Create the request to OpenWeatherMap and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into string
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                return;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                // Adding a new line isn't necessary for JSON because it won't affect parsing.
                // Makes debugging easier!
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                // Empty stream. Can't parse nothing.
                return;
            }

            forecastJsonStr = buffer.toString();
            // Parse the JSON String
            try {
                getWeatherDataFromString(forecastJsonStr, locationQuery);
            } catch (JSONException e) {
                Log.d(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }

        } catch (IOException e) {
            // Unable to get weather data, so no need to parse.
            Log.e(LOG_TAG, "Error ", e);
            e.printStackTrace();
            return;
        } catch (Exception e) {
            Log.e(LOG_TAG, e.toString());
        } finally {
            // Close opened resources
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }
        return;
    }

    /**
     * Helper method to have the sync adapter sync immediately
     * @param context The context used to access the account service
     */
    public static void syncImmediately(Context context) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(getSyncAccount(context), context.getString(R.string.content_authority), bundle);
    }

    /**
     * Helper method for checking whether an account exists. If it does not, this method creates a
     * dummy account. If an account is created, onAccountCreated method is called to initialize it
     *
     * @param context used to access account service
     * @return fake account
     */
    public static Account getSyncAccount(Context context) {
        // Retrieve instance of Android AccountManager
        AccountManager accountManager = (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        // Create the account type and default account
        Account newAccount = new Account(context.getString(R.string.app_name), context.getString(R.string.sync_account_type));

        // If the password doesn't exist, the account doesn't exist
        if (accountManager.getPassword(newAccount) == null) {
            /*
             * Add the account and account type, no password or user data.
             * If successful, return the Account object, otherwise report an error
             */
            if (!accountManager.addAccountExplicitly(newAccount, "", null)) {
                return null;
            }
            onAccountCreated(newAccount, context);
        }
        return newAccount;
    }

    public static void configurePeriodicSync(Context context, int syncInterval, int flexTime) {
        Account account = getSyncAccount(context);
        String authority = context.getString(R.string.content_authority);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // Enable inexact timers in periodic sync for Kitkat and above
            SyncRequest request = new SyncRequest.Builder()
                    .syncPeriodic(syncInterval, flexTime)
                    .setSyncAdapter(account, authority)
                    .setExtras(new Bundle())
                    .build();
            ContentResolver.requestSync(request);
        } else {
            // Otherwise we must use an exact timer
            ContentResolver.addPeriodicSync(account, authority, new Bundle(), syncInterval);
        }
    }

    /**
     * Helper method for when a new account is created to set up sync settings and being first sync
     * @param newAccount dummy account
     * @param context for accessing string parameters and passing to called methods
     */
    private static void onAccountCreated(Account newAccount, Context context) {
        // Since an acocunt has been created
        SunshineSyncAdapter.configurePeriodicSync(context, SYNC_INTERVAL, SYNC_FLEXTIME);

        // Enable periodic sync
        ContentResolver.setSyncAutomatically(newAccount, context.getString(R.string.content_authority), true);

        // Sync
        syncImmediately(context);
    }

    public static void intitializeSyncAdapter(Context context) {
        getSyncAccount(context);
    }

    /*
     * Adds a location value to the Location Table
     */
    long addLocation(String locationSetting, String cityName, double lat, double lon) {
        long _id;

        // Query the database to check if the location already exists
        Cursor locationCursor =  context.getContentResolver().query(
                WeatherContract.LocationEntry.CONTENT_URI,
                new String[] {WeatherContract.LocationEntry._ID},
                WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ?",
                new String[] {locationSetting},
                null
        );

        if (locationCursor.moveToFirst()) {
            // If cursor is able to move to first then that means the location already exists in the
            // database. In this case, just get the _id column for the returned row.
            _id = locationCursor.getLong(locationCursor.getColumnIndex(WeatherContract.LocationEntry._ID));
            Log.v(LOG_TAG, "Location " + locationSetting + " exists in table!");
        } else {
            // First create a ContentValues and populate it with the data to be inserted
            ContentValues locationValues = new ContentValues();
            locationValues.put(WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING, locationSetting);
            locationValues.put(WeatherContract.LocationEntry.COLUMN_CITY_NAME, cityName);
            locationValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LAT, lat);
            locationValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LONG, lon);

            // Retrieve the URI of the row that was added
            Uri insertedUri = context.getContentResolver().insert(
                    WeatherContract.LocationEntry.CONTENT_URI,
                    locationValues
            );

            // Parse the id from the URI returned from the insert function
            _id = ContentUris.parseId(insertedUri);
            Log.v(LOG_TAG, "New location " + locationSetting + " added to database!");
        }

        return _id;
    };


    /**
     * Parse the JSON string returned from the FetchWeatherTask and pull the required data
     * from the JSON Object
     */
    private String[] getWeatherDataFromString(String forecastJsonStr, String locationSetting) throws JSONException {
        // These are the names of the JSON objects that need to be extracted.
        // Array that holds all weather values
        final String OWM_LIST = "list";

        // Weather values
        final String OWM_WEATHER = "weather";
        final String OWM_WEATHER_ID = "id";
        final String OWM_DESCRIPTION = "main";

        // Temperature values
        final String OWM_TEMPERATURE = "temp";
        final String OWM_MAX = "max";
        final String OWM_MIN = "min";

        // Miscellaneous weather values
        final String OWM_PRESSURE = "pressure";
        final String OWM_HUMIDITY = "humidity";
        final String OWM_WIND_SPEED = "speed";
        final String OWM_DEGREES = "deg";

        // Latitude & Longitude values
        final String OWM_COORDINATES = "coord";
        final String OWM_LONGITUDE = "lon";
        final String OWM_LATITUDE = "lat";

        // Location values
        final String OWM_LOCATION = "city";
        final String OWM_LOCATION_SETTING = "id";
        final String OWM_CITY_NAME = "name";

        try {
            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

            // Location values only only need to be inserted once. Retrieve the values and utilize the
            // addLocation method to insert the location into the database.

            // Get location values
            JSONObject cityObject = forecastJson.getJSONObject(OWM_LOCATION);
            String cityName = cityObject.getString(OWM_CITY_NAME);

            // Get longitude & latitude
            JSONObject coordObject = cityObject.getJSONObject(OWM_COORDINATES);
            double lat = coordObject.getDouble(OWM_LATITUDE);
            double lon = coordObject.getDouble(OWM_LONGITUDE);

            // Utilize addLocation method to insert location
            long locationId = addLocation(locationSetting, cityName, lat, lon);

            // Time needs to be normalized since the UNIX timestamp in the JSON String is set to UTC
            Time dayTime = new Time();
            dayTime.setToNow();

            // Start at local date
            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

            // Now we work in UTC
            dayTime = new Time();

            // Utilize iteration to go through the number of days specified and store the outcome
            // in an array to be returned by this method
            Vector<ContentValues> contentValuesVector = new Vector<>(weatherArray.length());
            String[] resultStrs = new String[weatherArray.length()];

            for (int i = 0; i < weatherArray.length(); i++) {
                // Get the individual JSONObject day from the JSONArray
                JSONObject dayObject = weatherArray.getJSONObject(i);

                // Get the min/max temperature of the day
                JSONObject temperatureObject = dayObject.getJSONObject(OWM_TEMPERATURE);
                Double highTemperature = temperatureObject.getDouble(OWM_MAX);
                Double lowTemperature = temperatureObject.getDouble(OWM_MIN);

                // Get the weather description for the day
                JSONObject weatherObject = dayObject.getJSONArray(OWM_WEATHER).getJSONObject(0);
                String description = weatherObject.getString(OWM_DESCRIPTION);
                int weatherId = weatherObject.getInt(OWM_WEATHER_ID);

                // Get miscellaneous weather attributes
                double pressure = dayObject.getDouble(OWM_PRESSURE);
                double humidity = dayObject.getDouble(OWM_HUMIDITY);
                double windSpeed = dayObject.getDouble(OWM_WIND_SPEED);
                double degrees = dayObject.getDouble(OWM_DEGREES);

                // Set the day/time utilizing the dayTime object created above and parse it to
                // human readable date/time
                long dateTime = dayTime.setJulianDay(julianStartDay + i);

                // Add all the values to the ContentValues and then add it to the Vector
                ContentValues cv = new ContentValues();
                cv.put(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID, weatherId);
                cv.put(WeatherContract.WeatherEntry.COLUMN_DATE, dateTime);
                cv.put(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP, highTemperature);
                cv.put(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP, lowTemperature);
                cv.put(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC, description);
                cv.put(WeatherContract.WeatherEntry.COLUMN_PRESSURE, pressure);
                cv.put(WeatherContract.WeatherEntry.COLUMN_HUMIDITY, humidity);
                cv.put(WeatherContract.WeatherEntry.COLUMN_WIND_SPEED, windSpeed);
                cv.put(WeatherContract.WeatherEntry.COLUMN_DEGREES, degrees);
                cv.put(WeatherContract.WeatherEntry.COLUMN_LOC_KEY, locationId);

                contentValuesVector.add(cv);
            }

            int rows = 0;   // Stores the number of rows successfully inserted into the database

            if (contentValuesVector.size() > 0) {
                // Bulk insert takes an array of ContentValues so the Vector needs to be converted
                // to an Array
                ContentValues[] cvArray = new ContentValues[weatherArray.length()];
                contentValuesVector.toArray(cvArray);
                rows = context.getContentResolver().bulkInsert(WeatherContract.WeatherEntry.CONTENT_URI, cvArray);
            }

            Log.v(LOG_TAG, "FetchWeatherTask complete. " + rows + " inserted into database!");

            return resultStrs;

        } catch (JSONException e) {
            Log.d(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }
        return null;
    }

}
