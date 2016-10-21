package com.example.android.sunshine.app.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

import com.example.android.sunshine.app.data.WeatherContract.WeatherEntry;

/**
 * Created by hnoct on 10/19/2016.
 */

public class DatabaseUtils {
    public static void cursorRowToContentValues(Cursor cursor, ContentValues contentValues) {
        String maxTemperature = cursor.getString(cursor.getColumnIndex(WeatherEntry.COLUMN_MAX_TEMP));
        String minTemperature = cursor.getString(cursor.getColumnIndex(WeatherEntry.COLUMN_MIN_TEMP));
        String date = cursor.getString(cursor.getColumnIndex(WeatherEntry.COLUMN_DATE));
        String description = cursor.getString(cursor.getColumnIndex(WeatherEntry.COLUMN_SHORT_DESC));

        contentValues.put(WeatherEntry.COLUMN_MAX_TEMP, maxTemperature);
        contentValues.put(WeatherEntry.COLUMN_MIN_TEMP, minTemperature);
        contentValues.put(WeatherEntry.COLUMN_DATE, date);
        contentValues.put(WeatherEntry.COLUMN_SHORT_DESC, description);
    }
}
