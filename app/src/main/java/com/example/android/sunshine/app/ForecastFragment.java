package com.example.android.sunshine.app;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.os.AsyncTaskCompat;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.apache.http.impl.conn.tsccm.RefQueueWorker;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class ForecastFragment extends Fragment {

    private ArrayAdapter<String> forecastAdapter;
    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            // Temporary debug button to test FetchWeatherTask
            new FetchWeatherTask().execute("92646");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        // Deprecated
        String[] forecastArray = {
                "Today - Sunny - 88/63",
                "Tomorrow - Foggy - 70/46",
                "Weds - Cloudy - 72/63",
                "Thurs - Apocaplyptic - 168/-273.12",
                "Fri - Snowing - 32/17",
                "Sat - Sleet - 27/12",
                "Sun - Sunny - 77/65",
        };

        List<String> weekForecast = new ArrayList<>(Arrays.asList(forecastArray));

        forecastAdapter = new ArrayAdapter<>(
                getActivity(),
                R.layout.list_item_forecast,
                R.id.list_item_forecast_textview,
                weekForecast
        );

        ListView listView = (ListView) rootView.findViewById(R.id.listview_forecast);
        listView.setAdapter(forecastAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Context context = getActivity();
                String forecast = forecastAdapter.getItem(i);

                Intent intent = new Intent(context, DetailActivity.class)
                        .putExtra(Intent.EXTRA_TEXT, forecast);
                startActivity(intent);
            }
        });


        return rootView;
    }


    public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {
        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

        /*
         * Converts UNIX timestamp from JSON to human readable date format
         */
        private String getReadableDateString(long time) {
            SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
            return shortenedDateFormat.format(time);
        }

        /*
         * Prepares the weather for high/low presentation
         */
        private String formatHighLows(double high, double low) {
            // User probably doesn't care about fractions of a degree.
            long roundedHigh = Math.round(high);
            long roundedLow = Math.round(low);

            String highLowStr = roundedHigh + "/" + roundedLow;
            return highLowStr;
        }

        /*
         * Parse the JSON string returned from the FetchWeatherTask and pull the required data
         * from the JSON Object
         */
        private String[] getWeatherDataFromString(String forecastJsonStr, int numDays) throws JSONException {
            // These are the names of the JSON objects that need to be extracted.
            final String OWM_LIST = "list";
            final String OWM_WEATHER = "weather";
            final String OWM_TEMPERATURE = "main";
            final String OWM_MAX = "temp_max";
            final String OWM_MIN = "temp_min";
            final String OWM_DESCRIPTION = "main";

            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

            // Time needs to be normalized since the UNIX timestamp in the JSON String is set to UTC
            Time dayTime = new Time();
            dayTime.setToNow();

            // Start at local date
            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

            // Now we work in UTC
            dayTime = new Time();

            // Utilize iteration to go through the number of days specified and store the outcome
            // in an array to be returned by this method
            String[] resultStrs = new String[numDays];

            for (int i = 0; i < numDays; i++) {
                // Store the outcome as "Day, description, high/low"
                JSONObject dayObject = weatherArray.getJSONObject(i);

                // Get the min/max temperature of the day
                JSONObject temperatureObject =  dayObject.getJSONObject(OWM_TEMPERATURE);
                Double highTemperature = temperatureObject.getDouble(OWM_MAX);
                Double lowTemperature = temperatureObject.getDouble(OWM_MIN);

                // Get the weather description for the day
                JSONObject weatherObject = dayObject.getJSONArray(OWM_WEATHER).getJSONObject(0);
                String description = weatherObject.getString(OWM_DESCRIPTION);

                // Parse temperature in high/low, rounding fractions of a degree
                String highLowStr = formatHighLows(highTemperature, lowTemperature);

                // Set the day/time utilizing the dayTime object created above and parse it to
                // human readable date/time
                long dateTime = dayTime.setJulianDay(julianStartDay + i);
                String day = getReadableDateString(dateTime);

                resultStrs[i] = day + " - " + description + " - " + highLowStr;
            }

            return resultStrs;
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

                Uri.Builder builder = new Uri.Builder();
                builder.scheme("http")
                        .authority("api.openweathermap.org")
                        .appendPath("data")
                        .appendPath("2.5")
                        .appendPath("forecast")
                        .appendPath("daily?")
                        .appendQueryParameter(QUERY_PARAM, zipCode)
                        .appendQueryParameter(FORMAT_PARAMS, format)
                        .appendQueryParameter(UNITS_PARAM, units)
                        .appendQueryParameter(DAYS_PARAMS, Integer.toString(numDays))
                        .appendQueryParameter(API_PARAM, BuildConfig.OPEN_WEATHER_MAP_API_KEY);

                String urlString = builder.build().toString();
                URL url = new URL(urlString);

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
                while((line = reader.readLine()) != null) {
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
                        Log.e(LOG_TAG, "Error closing stream" , e);
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
}
