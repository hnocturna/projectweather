package com.example.android.sunshine.app;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.android.sunshine.app.data.WeatherContract.WeatherEntry;

/**
 * A placeholder fragment containing a simple view.
 */
public class DetailsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String LOG_TAG = DetailsFragment.class.getSimpleName();
    private static final int DETAILS_LOADER = 0;
    public static String DETAIL_URI = "DETAIL_URI";
    private static Uri uri;

    private static final String[] DETAILS_COLUMNS = new String[]{
            WeatherEntry.TABLE_NAME + "." + WeatherEntry._ID,
            WeatherEntry.COLUMN_DATE,
            WeatherEntry.COLUMN_SHORT_DESC,
            WeatherEntry.COLUMN_MIN_TEMP,
            WeatherEntry.COLUMN_MAX_TEMP,
            WeatherEntry.COLUMN_HUMIDITY,
            WeatherEntry.COLUMN_WIND_SPEED,
            WeatherEntry.COLUMN_DEGREES,
            WeatherEntry.COLUMN_PRESSURE,
            WeatherEntry.COLUMN_WEATHER_ID
    };

    static final int COL_WEATHER_ID = 0;
    static final int COL_DATE = 1;
    static final int COL_WEATHER_SHORT_DESC = 2;
    static final int COL_WEATHER_MIN = 3;
    static final int COL_WEATHER_MAX = 4;
    static final int COL_WEATHER_HUMIDITY = 5;
    static final int COL_WEATHER_WIND_SPEED = 6;
    static final int COL_WEATHER_DEGREES = 7;
    static final int COL_WEATHER_PRESSURE = 8;
    static final int COL_WEATHER_CONDITION_ID = 9;

    TextView dayText;
    TextView dateText;
    TextView descriptionText;
    TextView highText;
    TextView lowText;
    TextView humidityText;
    TextView windText;
    TextView pressureText;
    ImageView iconView;

    public DetailsFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);

        // Views are set as member variables to prevent waste of resources from constantly
        // traversing the view tree in the onLoadFinished of the Loader
        dayText = (TextView) rootView.findViewById(R.id.detail_day_text);
        dateText = (TextView) rootView.findViewById(R.id.detail_date_text);
        descriptionText = (TextView) rootView.findViewById(R.id.detail_description_text);
        highText = (TextView) rootView.findViewById(R.id.detail_high_text);
        lowText = (TextView) rootView.findViewById(R.id.detail_low_text);
        humidityText = (TextView) rootView.findViewById(R.id.detail_humidity_text);
        windText = (TextView) rootView.findViewById(R.id.detail_wind_text);
        pressureText = (TextView) rootView.findViewById(R.id.detail_pressure_text);
        iconView = (ImageView) rootView.findViewById(R.id.detail_icon);

        if (getArguments() != null) {
            Log.v(LOG_TAG, "Bundle found!");
            uri = getArguments().getParcelable(DETAIL_URI);
        } else {
            Log.v(LOG_TAG, "No Bundle found!");
        }

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

    void onLocationChanged(String newLocation) {
        if (uri != null) {
            long date = WeatherEntry.getDateFromUri(uri);
            uri = WeatherEntry.buildWeatherLocationWithDate(newLocation, date);
            getLoaderManager().restartLoader(DETAILS_LOADER, null, this);
        }
    }
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.v(LOG_TAG, "In onCreateLoader");
        if (uri == null) {
            return null;
        }

        // Return CursorLoader with the URI and the projection created in the DetailsFragment
        return new CursorLoader(
                getActivity(),
                uri,
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
        Context context = getActivity();
        boolean isMetric = Utility.isMetric(context);
        long columnDate = cursor.getLong(COL_DATE);

        int weatherId = cursor.getInt(COL_WEATHER_CONDITION_ID);
        String day = Utility.getDayName(context, columnDate);
        String date = Utility.getFormattedMonthDay(context, columnDate);
        String description = cursor.getString(COL_WEATHER_SHORT_DESC);
        String highTemperatures = Utility.formatTemperature(
                getActivity(),
                cursor.getDouble(COL_WEATHER_MAX),
                isMetric
        );
        String lowTemperatures = Utility.formatTemperature(
                getActivity(),
                cursor.getDouble(COL_WEATHER_MIN),
                isMetric
        );
        String humidity = context.getString(R.string.format_humidity, cursor.getFloat(COL_WEATHER_HUMIDITY));
        String wind = Utility.getFormattedWind(
                context,
                cursor.getFloat(COL_WEATHER_WIND_SPEED),
                cursor.getFloat(COL_WEATHER_DEGREES)
        );
        String pressure = context.getString(R.string.format_pressure, cursor.getFloat(COL_WEATHER_PRESSURE));
        int weatherArt = Utility.getArtResourceForWeatherCondition(weatherId);

        // Set the Views in the DetailsFragment to the Strings extracted from the Cursor
        dayText.setText(day);
        dateText.setText(date);
        descriptionText.setText(description);
        highText.setText(highTemperatures);
        lowText.setText(lowTemperatures);
        humidityText.setText(humidity);
        windText.setText(wind);
        pressureText.setText(pressure);
        iconView.setImageResource(weatherArt);
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
