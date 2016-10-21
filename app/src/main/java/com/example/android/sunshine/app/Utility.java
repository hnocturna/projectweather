package com.example.android.sunshine.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.text.DateFormat;
import java.util.Date;

/**
 * Created by hnoct on 10/20/2016.
 * A helper class that is used to retrieve user preferences such as preferred location or units of
 * temperature as well as format them into user-readable {@link String}
 */

public class Utility {

    /*
     * Returns the user-specified location located in SharedPreferences
     */
    public static String getPreferredLocation(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(context.getString(R.string.pref_location_key),
                context.getString(R.string.pref_location_default));
    }

    /*
     * Returns the user-stored units preference for temperature
     */
    public static boolean isMetric(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(context.getString(R.string.pref_units_key),
                context.getString(R.string.pref_units_metric))
                .equals(context.getString(R.string.pref_units_metric));
    }

    /*
     * Converts the temperature from metric to fahrenheit if isMetric is false
     */
    static String formatTemperature(double temperature, boolean isMetric) {
        double temp;
        if (!isMetric) {
            temp = 1.8 * temperature + 32;
        } else {
            temp = temperature;
        }
        return String.format("%.0f", temp);
    }

    /*
     * Formats the date into a user-readable String
     */
    static String formatDate(long dateInMillis) {
        Date date = new Date(dateInMillis);
        return DateFormat.getDateInstance().format(date);
    }
}
