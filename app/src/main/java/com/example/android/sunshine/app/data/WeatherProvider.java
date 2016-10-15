package com.example.android.sunshine.app.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.Nullable;

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
    private static final String locationSettingAndDateSelection =
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
        String selection = locationSettingAndDateSelection;

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
        UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        // 2) Use the addURI function to match each of the types using the constants from
        // WeatherContract to help define the types to the UriMatcher

        // com.example.android.sunshine.app/weather
        uriMatcher.addURI(WeatherContract.CONTENT_AUTHORITY, WeatherEntry.TABLE_NAME, WEATHER);
        // com.example.android.sunshine.app/location
        uriMatcher.addURI(WeatherContract.CONTENT_AUTHORITY, LocationEntry.TABLE_NAME, LOCATION);
        // com.example.android.sunshine.app/weather/location/date#
        uriMatcher.addURI(WeatherContract.CONTENT_AUTHORITY,  WeatherEntry.TABLE_NAME + "/" +
            LocationEntry.TABLE_NAME + "/" + WeatherEntry.COLUMN_DATE,
            WEATHER_WITH_LOCATION_AND_DATE);
        // com.example.android.sunshine.app/weather
        uriMatcher.addURI(WeatherContract.CONTENT_AUTHORITY, WeatherEntry.TABLE_NAME, WEATHER);
        // com.example.android.sunshine.app/weather/location
        uriMatcher.addURI(WeatherContract.CONTENT_AUTHORITY, WeatherEntry.TABLE_NAME + "/" +
                LocationEntry.TABLE_NAME , WEATHER_WITH_LOCATION);
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
                return LocationEntry.CONTENT_ITEM_TYPE;
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
            // weather/*/*
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
                cursor = null;
                break;
            }
            // location
            case LOCATION: {
                cursor = null;
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown URI: " + uri);
        }
        // TODO: Write something about what this does
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
                if (_id != -1) {
                    returnUri = WeatherEntry.buildWeatherUri(_id);
                } else {
                    throw new SQLException("Failed to insert row into " + uri);
                }
                break;
            }
            case LOCATION: {
                long _id = db.insert(LocationEntry.TABLE_NAME, null, contentValues);

                if (_id != -1) {
                    returnUri = LocationEntry.buildLocationUri(_id);
                } else {
                    throw new SQLException("Failed to insert row into " + uri);
                }
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown URI: " + uri);
        }

        // TODO: Write something about this command.
        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int delete(Uri uri, String s, String[] strings) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String s, String[] strings) {
        return 0;
    }

    private void normalizeDate(ContentValues contentValues) {
        if (contentValues.containsKey(WeatherEntry.COLUMN_DATE)) {
            long dateValue = contentValues.getAsLong(WeatherEntry.COLUMN_DATE);
            contentValues.put(WeatherEntry.COLUMN_DATE, dateValue);
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
                // TODO: Still not sure what this command does yet
                getContext().getContentResolver().notifyChange(uri, null);
                return returnCount;
            }
            // TODO: Write something about the default statement
            default:
                return super.bulkInsert(uri, values);
        }
    }

    /*
     * Method is specifically for assisting the testing framework run smoothly
     */
    @Override
    public void shutdown() {
        dbHelper.close();
        super.shutdown();
    }
}
