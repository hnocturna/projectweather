package com.example.android.sunshine.app.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.android.sunshine.app.data.WeatherContract.WeatherEntry;
import com.example.android.sunshine.app.data.WeatherContract.LocationEntry;

/**
 * Created by hnoct on 10/12/2016.
 *
 * Class containing all the methods utilized in storing, accessing, and managing weather data in
 * the locally created SQLite database
 */

public class WeatherDbHelper extends SQLiteOpenHelper {
    // Keeps track of the database version. Needs to be incremented any time the schema is updated
    private static final int DATABASE_VERSION = 1;

    // The name of the database file as it will be on the phone's storage
    static final String DATABASE_NAME = "weather.db";

    public WeatherDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        final String SQL_CREATE_WEATHER_TABLE = "CREATE TABLE" + WeatherEntry.TABLE_NAME + " (" +
                // Auto-increment the primary key of the weather entries as it will be assumed that
                // the user will want information on dates 'following' the initial date being
                // queried
                WeatherEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                // The foreign key in the location database
                // TODO: Create location database
                WeatherEntry.COLUMN_LOC_KEY + " INTEGER NOT NULL, " +
                WeatherEntry.COLUMN_DATE + " INTEGER NOT NULL, " +
                WeatherEntry.COLUMN_SHORT_DESC + " INTEGER NOT NULL, " +
                WeatherEntry.COLUMN_WEATHER_ID + " INTEGER NOT NULL, " +

                WeatherEntry.COLUMN_MIN_TEMP + " INTEGER NOT NULL, " +
                WeatherEntry.COLUMN_MAX_TEMP + " INTEGER NOT NULL, " +

                WeatherEntry.COLUMN_HUMIDITY + " REAL NOT NULL, " +
                WeatherEntry.COLUMN_PRESSURE + " REAL NOT NULL, " +
                WeatherEntry.COLUMN_WIND_SPEED + " REAL NOT NULL, " +
                WeatherEntry.COLUMN_DEGREES + " REAL NOT NULL, " +

                // Set up the location column as foreign key to the location table
                "FOREIGN KEY (" + WeatherEntry.COLUMN_LOC_KEY + ") REFERENCES " +
                LocationEntry.TABLE_NAME + "(" + LocationEntry._ID + "), " +

                // Ensure each entry is unique by setting the date as unique and replacing the
                // entire entry in case there is a conflict
                "UNIQUE ( " + WeatherEntry.COLUMN_DATE + ", " +
                WeatherEntry.COLUMN_LOC_KEY + ") ON CONFLICT REPLACE);";

        sqLiteDatabase.execSQL(SQL_CREATE_WEATHER_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        // Since this only caches weather data from online, there is no need to preserve the data
        // when changing version numbers, so they are merely discarded and tables are created using
        // the updated schema
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + WeatherEntry.TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + LocationEntry.TABLE_NAME);
        onCreate(sqLiteDatabase);
    }
}
