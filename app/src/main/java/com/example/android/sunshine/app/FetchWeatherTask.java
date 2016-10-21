package com.example.android.sunshine.app;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.util.Log;
import android.widget.ArrayAdapter;

import com.example.android.sunshine.app.data.DatabaseUtils;
import com.example.android.sunshine.app.data.WeatherDbHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Vector;

import com.example.android.sunshine.app.data.WeatherContract.LocationEntry;
import com.example.android.sunshine.app.data.WeatherContract.WeatherEntry;

/**
 * Created by hnoct on 10/13/2016.
 */
public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {
    private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();
    Context context;
    ArrayAdapter<String> forecastAdapter;
    WeatherDbHelper dbHelper;

    public FetchWeatherTask(Context context, ArrayAdapter<String> forecastAdapter) {
        this.context = context;
        this.forecastAdapter = forecastAdapter;
        dbHelper = new WeatherDbHelper(context);
    }

    /*
     * Adds a location value to the Location Table
     */
    long addLocation(String locationSetting, String cityName, double lat, double lon) {
        long _id;

        // Query the database to check if the location already exists
        Cursor locationCursor = context.getContentResolver().query(
                LocationEntry.CONTENT_URI,
                new String[] {LocationEntry._ID},
                LocationEntry.COLUMN_LOCATION_SETTING + " = ?",
                new String[] {locationSetting},
                null
        );

        if (locationCursor.moveToFirst()) {
            // If cursor is able to move to first then that means the location already exists in the
            // database. In this case, just get the _id column for the returned row.
            _id = locationCursor.getLong(locationCursor.getColumnIndex(LocationEntry._ID));
        } else {
            // First create a Content Value and populate it with the data to be inserted
            ContentValues locationValues = new ContentValues();
            locationValues.put(LocationEntry.COLUMN_LOCATION_SETTING, locationSetting);
            locationValues.put(LocationEntry.COLUMN_CITY_NAME, cityName);
            locationValues.put(LocationEntry.COLUMN_COORD_LAT, lat);
            locationValues.put(LocationEntry.COLUMN_COORD_LONG, lon);

            // Retrieve the URI of the row that was added
            Uri insertedUri = context.getContentResolver().insert(
                    LocationEntry.CONTENT_URI,
                    locationValues
            );

            // Parse the id from the URI returned from the insert function
            _id = ContentUris.parseId(insertedUri);
        }

        return _id;
    };

    /*
     * Converts temperature units from default metric to imperial units.
     * Preserves all the data being metric when stored into database
     */
    private double metricToImperial(double temperature) {
        return (temperature * 1.8) + 32;
    }

    /*
     * Converts UNIX timestamp from JSON to human readable date format
     */
    private String getReadableDateString(long time) {
        SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
        return shortenedDateFormat.format(time);
    }

    /*
     * Prepares the weather for high/low presentation in string format
     */
    private String formatHighLows(double high, double low) {
        // Check what units the user has specified
        String units = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.pref_units_key),
                        context.getString(R.string.pref_units_default));

        // If units selected is imperial, then convert to imperial units prior to converting to
        // string
        if (units.equals(context.getString(R.string.pref_units_imperial))) {
            high = metricToImperial(high);
            low = metricToImperial(low);
        } else if (!units.equals(context.getString(R.string.pref_units_default))) {
            Log.d(LOG_TAG, "Unit type not found: " + units);
        }

        // User probably doesn't care about fractions of a degree.
        long roundedHigh = Math.round(high);
        long roundedLow = Math.round(low);

        String highLowStr = roundedHigh + "/" + roundedLow;
        return highLowStr;
    }

    /*
     * Extract data from ContentValues and parse it into a String format that the primitive UX will
     * recognize
     */
    private String[] convertContentValuesToUXFormat(Vector<ContentValues> contentValuesVector) {
        String[] weatherArray = new String[contentValuesVector.size()];

        for (int i = 0; i < contentValuesVector.size(); i++) {
            // Retrieve a single ContentValues
            ContentValues contentValues = contentValuesVector.elementAt(i);

            // Retrieve the data from within the ContentValues
            double maxTemperature = contentValues.getAsDouble(WeatherEntry.COLUMN_MAX_TEMP);
            double minTemperature = contentValues.getAsDouble(WeatherEntry.COLUMN_MIN_TEMP);
            long date = contentValues.getAsLong(WeatherEntry.COLUMN_DATE);
            String description = contentValues.getAsString(WeatherEntry.COLUMN_SHORT_DESC);

            // Parse the high and low temperatures into readable format
            String highAndLow = formatHighLows(maxTemperature, minTemperature);

            // Parse the date into readable format and combine to form the full String
            weatherArray[i] = getReadableDateString(date) +
                    " - " + description +
                    " - " + highAndLow;

        }
        return weatherArray;
    }

    /*
     * Parse the JSON string returned from the FetchWeatherTask and pull the required data
     * from the JSON Object
     */
    private String[] getWeatherDataFromString(String forecastJsonStr, int numDays) throws JSONException {
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
            String locationSetting = cityObject.getString(OWM_LOCATION_SETTING);

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
            Vector<ContentValues> contentValuesVector = new Vector<>(numDays);
            String[] resultStrs = new String[numDays];

            for (int i = 0; i < numDays; i++) {
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

                // Parse temperature in high/low, rounding fractions of a degree
                String highLowStr = formatHighLows(highTemperature, lowTemperature);

                // Set the day/time utilizing the dayTime object created above and parse it to
                // human readable date/time
                long dateTime = dayTime.setJulianDay(julianStartDay + i);
                String day = getReadableDateString(dateTime);

                resultStrs[i] = day + " - " + description + " - " + highLowStr;

                // Add all the values to the ContentValues and then add it to the Vector
                ContentValues cv = new ContentValues();
                cv.put(WeatherEntry.COLUMN_WEATHER_ID, weatherId);
                cv.put(WeatherEntry.COLUMN_DATE, dateTime);
                cv.put(WeatherEntry.COLUMN_MAX_TEMP, highTemperature);
                cv.put(WeatherEntry.COLUMN_MIN_TEMP, lowTemperature);
                cv.put(WeatherEntry.COLUMN_SHORT_DESC, description);
                cv.put(WeatherEntry.COLUMN_PRESSURE, pressure);
                cv.put(WeatherEntry.COLUMN_HUMIDITY, humidity);
                cv.put(WeatherEntry.COLUMN_WIND_SPEED, windSpeed);
                cv.put(WeatherEntry.COLUMN_DEGREES, degrees);
                cv.put(WeatherEntry.COLUMN_LOC_KEY, locationId);

                contentValuesVector.add(cv);
            }

            int rows = 0;   // Stores the number of rows successfully inserted into the database

            if (contentValuesVector.size() > 0) {
                // Bulk insert takes an array of ContentValues so the Vector needs to be converted
                // to an Array
                ContentValues[] cvArray = new ContentValues[numDays];
                contentValuesVector.toArray(cvArray);
                rows = context.getContentResolver().bulkInsert(WeatherEntry.CONTENT_URI, cvArray);
            }

            // Query the database for the data that was just stored to display it in the primitive UX
            // currently being used and also to check that the data was correctly and successfully
            // inserted

            // Set the sortOrder Ascending, by date
            String sortOrder = WeatherEntry.COLUMN_DATE + " ASC";

            // Retrieve the URI for the weatherForLocationWithStartDate
            Uri weatherForLocationUri = WeatherEntry.buildWeatherLocationWithStartDate(
                    locationSetting,
                    System.currentTimeMillis()
            );

            // Store the queried values into a new ContentValues Vector and utilize a helper method to
            // parse it back into a string value for the UX
            Cursor cursor = context.getContentResolver().query(
                    weatherForLocationUri,
                    null,
                    null,
                    null,
                    sortOrder
            );

            // Create a new Vector initialized with the number of rows the cursor returns
            contentValuesVector = new Vector<>(cursor.getCount());

            if (cursor.moveToFirst()) {
                // If the cursor returns rows, convert the cursor data to ContentValues and add it
                // to the Vector
                do {
                    ContentValues cv = new ContentValues();
                    DatabaseUtils.cursorRowToContentValues(cursor, cv);
                    contentValuesVector.add(cv);
                } while (cursor.moveToNext());
            }

            Log.v(LOG_TAG, "FetchWeatherTask complete. " + rows + " inserted into database!");

            resultStrs = convertContentValuesToUXFormat(contentValuesVector);
            return resultStrs;

        } catch (JSONException e) {
            Log.d(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected String[] doInBackground(String... params) {
        // Defined outside of the try-catch block so they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        // Will contain the raw JSON response as a string.
        String forecastJsonStr = null;

        // Additional variables that will temporarily be held here until they are user configurable
        String format = "json";
        String units = "metric";
        int numDays = 7;

        try {
            // Construct the URL for the OpenWeatherMap query
            // Possible parameters are available at OWM's forecast API page at:
            // http://openweathermap.org/API#forecast
            if (params.length == 0) {
                // No zip code passed to the task, so there is nothing to pull.
                return null;
            }

            final String QUERY_PARAM = "q";
            final String FORMAT_PARAMS = "mode";
            final String UNITS_PARAM = "units";
            final String DAYS_PARAMS = "cnt";
            final String API_PARAM = "APPID";
            String zipCode = params[0];

            // Deprecated
//            Uri.Builder builder = new Uri.Builder();
//            builder.scheme("http")
//                    .authority("api.openweathermap.org")
//                    .appendPath("data")
//                    .appendPath("2.5")
//                    .appendPath("forecast")
//                    .appendPath("daily?")
//                    .appendQueryParameter(QUERY_PARAM, zipCode)
//                    .appendQueryParameter(FORMAT_PARAMS, format)
//                    .appendQueryParameter(UNITS_PARAM, units)
//                    .appendQueryParameter(DAYS_PARAMS, Integer.toString(numDays))
//                    .appendQueryParameter(API_PARAM, BuildConfig.OPEN_WEATHER_MAP_API_KEY);

            Uri forecastUri = Uri.parse("http://api.openweathermap.org/data/2.5/forecast/daily?")
                    .buildUpon()
                    .appendQueryParameter(QUERY_PARAM, zipCode)
                    .appendQueryParameter(FORMAT_PARAMS, format)
                    .appendQueryParameter(UNITS_PARAM, units)
                    .appendQueryParameter(DAYS_PARAMS, Integer.toString(numDays))
                    .appendQueryParameter(API_PARAM, BuildConfig.OPEN_WEATHER_MAP_API_KEY)
                    .build();

//            String urlString = builder.build().toString();
            URL url = new URL(forecastUri.toString());
            // Log.v(LOG_TAG, forecastUri.toString());

            // Deprecated
            // URL url = new URL("http://api.openweathermap.org/data/2.5/forecast/daily?q=90028&mode=json&units=metric&cnt=7" + "&APPID=" + API_KEY);

            // Create the request to OpenWeatherMap and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into string
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                return null;
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
                return null;
            }

            forecastJsonStr = buffer.toString();
            // Parse the JSON String
            try {
                return getWeatherDataFromString(forecastJsonStr, numDays);
            } catch (JSONException e) {
                Log.d(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }

        } catch (IOException e) {
            // Unable to get weather data, so no need to parse.
            Log.e(LOG_TAG, "Error ", e);
            e.printStackTrace();
            return null;
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
        return null;
    }

    @Override
    protected void onPostExecute(String[] result) {
        /*
         * Update the forecastAdapter with the new data from OpenWeatherMap
         */
        if (result != null) {
            forecastAdapter.clear();
            for (String s : result) {
                forecastAdapter.add(s);
            }
        }
    }
}
