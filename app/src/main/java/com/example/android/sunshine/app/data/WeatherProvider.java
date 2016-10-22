package com.example.android.sunshine.app.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

import com.example.android.sunshine.app.data.WeatherContract.WeatherEntry;
import com.example.android.sunshine.app.data.WeatherContract.LocationEntry;

import java.util.Arrays;

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

public class WeatherProvider extends ContentProvider {
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
        // weather INNER JOIN location ON weather.location_id = location_id.
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
    private static final String locationSettingWithStartDateSelection =
            LocationEntry.TABLE_NAME + "." + LocationEntry.COLUMN_LOCATION_SETTING + " = ? AND " +
                    WeatherEntry.COLUMN_DATE + " >= ?";

    // location.location_setting = ? AND date = ?
    private static final String locationSettingAndDaySelection =
            LocationEntry.TABLE_NAME + "." + LocationEntry.COLUMN_LOCATION_SETTING + " = ? AND " +
                    WeatherEntry.COLUMN_DATE + " = ?";

    private Cursor getWeatherByLocationSetting(Uri uri, String[] projection, String sortOrder) {
        String locationSetting = WeatherEntry.getLocationSettingFromUri(uri);
        long startDate = WeatherEntry.getStartDateFromUri(uri);

        String[] selectionArgs;
        String selection;

        if (startDate == 0) {
            selection = locationSettingSelection;
            selectionArgs = new String[] {locationSetting};
        } else {
            selectionArgs = new String[] {locationSetting, Long.toString(startDate)};
            selection = locationSettingWithStartDateSelection;
        }

        return weatherByLocationSettingQueryBuilder.query(dbHelper.getReadableDatabase(),
            projection,
            selection,
            selectionArgs,
            null,
            null,
            sortOrder
        );
    }

    private Cursor getWeatherByLocationSettingAndDate(Uri uri, String[] projection, String sortOrder) {
        String locationSetting = WeatherEntry.getLocationSettingFromUri(uri);
        long date = WeatherEntry.getDateFromUri(uri);

        String[] selectionArgs = new String[] {locationSetting, Long.toString(date)};
        String selection = locationSettingAndDaySelection;

        return weatherByLocationSettingQueryBuilder.query(dbHelper.getReadableDatabase(),
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );
    }

    static UriMatcher buildUriMatcher() {
        // 1) The code passed into the constructor represents the code to return for the root
        // URI. It's common to use NO_MATCH as the code for thie case
        final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = WeatherContract.CONTENT_AUTHORITY;

        // 2) Use the addURI function to match each of the types using the constants from
        // WeatherContract to help define the types to the UriMatcher

        // com.example.android.sunshine.app/weather
        uriMatcher.addURI(authority, WeatherContract.PATH_WEATHER, WEATHER);
        // com.example.android.sunshine.app/weather/*
        uriMatcher.addURI(authority, WeatherContract.PATH_WEATHER + "/*", WEATHER_WITH_LOCATION);
        // com.example.android.sunshine.app/weather/*/#
        uriMatcher.addURI(authority, WeatherContract.PATH_WEATHER + "/*/#", WEATHER_WITH_LOCATION_AND_DATE);
        // com.example.android.sunshine.app/location
        uriMatcher.addURI(authority, WeatherContract.PATH_LOCATION, LOCATION);

        // 3) Return the new matcher;
        return uriMatcher;
    }

    @Override
    public boolean onCreate() {
        dbHelper = new WeatherDbHelper(getContext());
        return true;
    }

    @Override
    public String getType(Uri uri) {
        final int match = uriMatcher.match(uri);

        switch (match) {
            case WEATHER_WITH_LOCATION_AND_DATE:
                return WeatherEntry.CONTENT_ITEM_TYPE;
            case WEATHER_WITH_LOCATION:
                return WeatherEntry.CONTENT_TYPE;
            case WEATHER:
                return WeatherEntry.CONTENT_TYPE;
            case LOCATION:
                return LocationEntry.CONTENT_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown URI: " + uri);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor cursor;
        switch (uriMatcher.match(uri)) {
            // weather/*/#
            case WEATHER_WITH_LOCATION_AND_DATE: {
                cursor = getWeatherByLocationSettingAndDate(uri, projection, sortOrder);
                break;
            }
            // weather/*
            case WEATHER_WITH_LOCATION: {
                cursor = getWeatherByLocationSetting(uri, projection, sortOrder);
                break;
            }
            // weather
            case WEATHER: {
                cursor = dbHelper.getReadableDatabase().query(
                        WeatherEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            // location
            case LOCATION: {
                cursor = dbHelper.getReadableDatabase().query(
                        LocationEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown URI: " + uri);
        }
        // Notifies any content observers/descendants that the information at the specified URI
        // has changed. This ensures any pages with refer to data that has been updated, shows the
        // updated information.
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        final SQLiteDatabase db = dbHelper.getWritableDatabase();
        final int match = uriMatcher.match(uri);
        Uri returnUri;

        switch (match) {
            case WEATHER: {
                // Normalize date to Julian Day
                normalizeDate(contentValues);
                long _id = db.insert(WeatherEntry.TABLE_NAME, null, contentValues);

                // If insert is successful, build the URI for the row
                if (_id > 0) {
                    returnUri = WeatherEntry.buildWeatherUri(_id);
                } else {
                    throw new SQLException("Failed to insert row into " + uri);
                }
                break;
            }
            case LOCATION: {
                long _id = db.insert(LocationEntry.TABLE_NAME, null, contentValues);

                if (_id > 0) {
                    returnUri = LocationEntry.buildLocationUri(_id);
                } else {
                    throw new SQLException("Failed to insert row into " + uri);
                }
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown URI: " + uri);
        }

        // Notifies any content observers/descendants that the information at the specified URI
        // has changed. This ensures any pages with refer to data that has been updated, shows the
        // updated information.
        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = dbHelper.getWritableDatabase();
        final int match = uriMatcher.match(uri);

        int rows = 0;

        // If there is no selection, that means all rows are being deleted. Setting it to "1"
        // will allow the function to return the number of rows being deleted
        if (selection == null) {
            selection = "1";
        }

        switch (match) {
            case WEATHER: {
                rows = db.delete(
                        WeatherEntry.TABLE_NAME,
                        selection,
                        selectionArgs
                );
                break;
            }
            case LOCATION: {
                rows = db.delete(
                        LocationEntry.TABLE_NAME,
                        selection,
                        selectionArgs
                );
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        // Return the number of rows updated
        return rows;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = dbHelper.getWritableDatabase();
        final int match = uriMatcher.match(uri);

        int rows = 0;

        switch (match) {
            case WEATHER: {
                normalizeDate(contentValues);
                rows = db.update(
                        WeatherEntry.TABLE_NAME,
                        contentValues,
                        selection,
                        selectionArgs
                );
                break;
            }
            case LOCATION: {
                normalizeDate(contentValues);
                rows = db.update(
                        LocationEntry.TABLE_NAME,
                        contentValues,
                        selection,
                        selectionArgs
                );
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        // Return the number of rows updated
        return rows;
    }

    /*
     * If the Content Values being inserted includes a date, it needs to be normalized to the
     * Julian Day.
     *
     * See WeatherContract.normalizeDate()
     */
    private void normalizeDate(ContentValues contentValues) {
        if (contentValues.containsKey(WeatherEntry.COLUMN_DATE)) {
            // Retrieve the date being inserted in the Content Values
            long dateValue = contentValues.getAsLong(WeatherEntry.COLUMN_DATE);
            // Replace the date with the normalized date and return it as part of the new Content
            // Values
            contentValues.put(WeatherEntry.COLUMN_DATE, WeatherContract.normalizeDate(dateValue));
        }
    }

    /*
     * Method for inserting multiple rows simultaneously without the constant costly I/O operations
     * to the database slowing down the system
     */
    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        final SQLiteDatabase db = dbHelper.getWritableDatabase();
        final int match = uriMatcher.match(uri);

        switch(match) {
            // Only weather entries can be inserted in bulk
            case WEATHER: {
                // Prepare the database for inserts
                db.beginTransaction();
                // Count the number of rows successfully inserted to be returned by the method
                int returnCount = 0;
                try {
                    // Insert each row individually utilizing the Content Values
                    for (ContentValues contentValues : values) {
                        // Normalize date of the Content Value to the Julian Day
                        normalizeDate(contentValues);
                        long _id = db.insert(WeatherEntry.TABLE_NAME, null, contentValues);
                        // If the insert is successful, increase the returnCount
                        if (_id != -1) {
                            returnCount++;
                        }
                    }
                    // Ends the single row to be inserted
                    db.setTransactionSuccessful();
                } finally {
                    // Tells the database to write all the rows sequentially in one I/O action
                    db.endTransaction();
                }
                getContext().getContentResolver().notifyChange(uri, null);
                return returnCount;
            }
            default:
                return super.bulkInsert(uri, values);
        }
    }

    /*
     * Method is specifically for assisting the testing framework run smoothly
     */
    @Override
    public void shutdown() {
        // dbHelper.close();
        super.shutdown();
    }
}
