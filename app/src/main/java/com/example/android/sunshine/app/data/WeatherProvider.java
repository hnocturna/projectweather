package com.example.android.sunshine.app.data;

import android.content.UriMatcher;
import android.database.sqlite.SQLiteQueryBuilder;
import com.example.android.sunshine.app.data.WeatherContract.WeatherEntry;
import com.example.android.sunshine.app.data.WeatherContract.LocationEntry;

/**
 * Created by hnoct on 10/13/2016.
 *
 * In the Udacity lesson, they utilize:
 *
 *      m-prefix for non-public, non-static field names
 *      s-prefix for static field names
 *
 * per the AOSP Code Style Guidelines, which is not recommended for individual Android apps, so I
 * will be dropping the convention and manually adding my personal field values in all test cases as
 * those are the only classes I am copying from their Github. All other classes are  being written
 * along-side to better understand Java coding.
 */

public class WeatherProvider {
    // The URI Matcher used by the content provider
    // TODO: Create method for buildUriMatcher()
    private static final UriMatcher uriMatcher = buildUriMatcher();
    private WeatherDbHelper dbHelper;

    static final int WEATHER = 100;
    static final int WEATHER_WITH_LOCATION = 101;
    static final int WEATHER_WITH_LOCATION_AND_DATE = 102;
    static final int LOCATION = 300;

    private static final SQLiteQueryBuilder weatherByLocationSettingQueryBuilder;

    // Sets up the value for the weatherByLocationSettingQueryBuilder because it is final and cannot
    // be edited using a return value from another method.
    static {
        weatherByLocationSettingQueryBuilder = new SQLiteQueryBuilder();

        // Builds a SQLite query with an INNER JOIN, eg:
        // weather INNER JOIN location ON weather.location_id = location_id;
        // INNER JOIN only returns data that has a match from both tables
        weatherByLocationSettingQueryBuilder.setTables(
                WeatherEntry.TABLE_NAME + " INNER JOIN " +
                LocationEntry.TABLE_NAME +
                " ON " + WeatherEntry.TABLE_NAME +
                "." + WeatherEntry.COLUMN_LOC_KEY +
                " = " + LocationEntry.TABLE_NAME +
                "." + LocationEntry._ID
        );
    }

    // location.location_setting = ?
    private static final String locationSettingSelection =
            LocationEntry.TABLE_NAME + "." + LocationEntry.COLUMN_LOCATION_SETTING + " = ?";

    // location.location_setting ? AND date >= ?
    private static final String locationSettingwithStartDateSelection =
            LocationEntry.TABLE_NAME + "." + LocationEntry.COLUMN_LOCATION_SETTING + " = ? AND " +
                    WeatherEntry.COLUMN_DATE + " >= ?";

    // location.location_setting = ? AND date = ?
    private static final String locationSettingAndDateSelection =
            LocationEntry.TABLE_NAME + "." + LocationEntry.COLUMN_LOCATION_SETTING + " = ? AND " +
                    WeatherEntry.COLUMN_DATE + " = ?";


}
