package com.example.android.sunshine.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.util.Log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by hnoct on 10/20/2016.
 * A helper class that is used to retrieve user preferences such as preferred location or units of
 * temperature as well as format them into user-readable {@link String}
 */

public class Utility {
    private static final String LOG_TAG = Utility.class.getSimpleName();

    public static final String FORMAT_DATE = "yyyyMMdd";

    /**
     * Helper method for converting the date in the database to a human-readable String
     * @param context Context used for resource utilization
     * @param dateInMillis date in milliseconds
     *
     * @return human-readable date String
     */
    public static String getFriendlyDayString(Context context, long dateInMillis) {
        Time time = new Time();
        time.setToNow();

        // Convert current day and date passed into Julian day to compare how far apart they are
        int currentJulianDay = Time.getJulianDay(System.currentTimeMillis(), time.gmtoff);
        int dateJulianDay = Time.getJulianDay(dateInMillis, time.gmtoff);

        if (dateJulianDay == currentJulianDay) {
            // If the date passed is today, display 'Today Oct 22'
            String todayString = context.getString(R.string.today);
            return context.getString(
                    R.string.format_full_friendly_date,
                    todayString,
                    getFormattedMonthDay(context, dateInMillis)
            );
        } else if (dateJulianDay == currentJulianDay + 1) {
            // If the date is tomorrow, display 'Tomorrow'
            return getDayName(context, dateInMillis);
        } else if (dateJulianDay <= currentJulianDay + 7) {
            // If the date is in the following 5 days, display the day of the week
            return getDayName(context, dateInMillis);
        } else {
            // If the date is past that, display 'Fri Oct 27'
            SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
            return shortenedDateFormat.format(dateInMillis);
        }
    }

    /**
     * Helper method for retrieving the name of the day of the week from the database date
     *
     * @param context Context for resource utilization
     * @param dateInMillis date in milliseconds
     *
     * @return name of the day of the week
     */
    public static String getDayName(Context context, long dateInMillis) {
        Time time = new Time();
        time.setToNow();
        int currentJulianDay = Time.getJulianDay(System.currentTimeMillis(), time.gmtoff);
        int dateJulianDay = Time.getJulianDay(dateInMillis, time.gmtoff);
        String dayName = "";

        if (dateJulianDay == currentJulianDay) {
            dayName = context.getString(R.string.today);
        } else if (dateJulianDay == currentJulianDay + 1) {
            dayName = context.getString(R.string.tomorrow);
        } else {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEEE");
            dayName = simpleDateFormat.format(dateInMillis);
        }
        return dayName;
    }

    /*
     * Helper method for formatting the date in Month day format.
     * e.g. Jun 3 or Dec 12
     *
     * @param context Context for resource utilization
     * @param dateInMillis date in milliseconds
     *
     * @return human-readable date
     */
    public static String getFormattedMonthDay(Context context, long dateInMillis) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMMM dd");
        String monthDayString = simpleDateFormat.format(dateInMillis);
        return monthDayString;
    }

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

    public static String getFormattedWind(Context context, float windSpeed, float degrees) {
        boolean isMetric = isMetric(context);
        int windFormat;
        if (isMetric) {
            windFormat = R.string.format_wind_kmh;
        } else {
            windFormat = R.string.format_wind_mph;
            windSpeed *= 0.621371192237734f;
        }

        // From wind direction in degrees, determine compass direction as a string (e.g NW)
        // You know what's fun, writing really long if/else statements with tons of possible
        // conditions.  Seriously, try it!
        String direction = "Unknown";
        if (degrees >= 337.5 || degrees < 22.5) {
            direction = "N";
        } else if (degrees >= 22.5 && degrees < 67.5) {
            direction = "NE";
        } else if (degrees >= 67.5 && degrees < 112.5) {
            direction = "E";
        } else if (degrees >= 112.5 && degrees < 157.5) {
            direction = "SE";
        } else if (degrees >= 157.5 && degrees < 202.5) {
            direction = "S";
        } else if (degrees >= 202.5 && degrees < 247.5) {
            direction = "SW";
        } else if (degrees >= 247.5 && degrees < 292.5) {
            direction = "W";
        } else if (degrees >= 292.5 && degrees < 337.5) {
            direction = "NW";
        }

        return context.getString(windFormat, windSpeed, direction);
    }

    /*
     * Converts the temperature from metric to fahrenheit if isMetric is false
     */
    static String formatTemperature(Context context, double temperature, boolean isMetric) {
        double temp;
        if (!isMetric) {
            temp = 1.8 * temperature + 32;
        } else {
            temp = temperature;
        }
        return context.getString(R.string.format_temperature, temp);
    }

    /*
     * Formats the date into a user-readable String
     */
    static String formatDate(long dateInMillis) {
        Date date = new Date(dateInMillis);
        return DateFormat.getDateInstance().format(date);
    }

    /**
     * Helper method to provide the icon resource id according to the weather condition id returned
     * by the OpenWeatherMap call.
     * @param weatherId from OpenWeatherMap API response
     * @return resource id for the corresponding icon. -1 if no relation is found.
     */
    public static int getIconResourceForWeatherCondition(int weatherId) {
        // Based on weather code data found at:
        // http://bugs.openweathermap.org/projects/api/wiki/Weather_Condition_Codes
        if (weatherId >= 200 && weatherId <= 232) {
            return R.drawable.ic_storm;
        } else if (weatherId >= 300 && weatherId <= 321) {
            return R.drawable.ic_light_rain;
        } else if (weatherId >= 500 && weatherId <= 504) {
            return R.drawable.ic_rain;
        } else if (weatherId == 511) {
            return R.drawable.ic_snow;
        } else if (weatherId >= 520 && weatherId <= 531) {
            return R.drawable.ic_rain;
        } else if (weatherId >= 600 && weatherId <= 622) {
            return R.drawable.ic_snow;
        } else if (weatherId >= 701 && weatherId <= 761) {
            return R.drawable.ic_fog;
        } else if (weatherId == 761 || weatherId == 781) {
            return R.drawable.ic_storm;
        } else if (weatherId == 800) {
            return R.drawable.ic_clear;
        } else if (weatherId == 801) {
            return R.drawable.ic_light_clouds;
        } else if (weatherId >= 802 && weatherId <= 804) {
            return R.drawable.ic_cloudy;
        }
        return -1;
    }

    /**
     * Helper method to provide the art resource id according to the weather condition id returned
     * by the OpenWeatherMap call.
     * @param weatherId from OpenWeatherMap API response
     * @return resource id for the corresponding image. -1 if no relation is found.
     */
    public static int getArtResourceForWeatherCondition(int weatherId) {
        // Based on weather code data found at:
        // http://bugs.openweathermap.org/projects/api/wiki/Weather_Condition_Codes
        if (weatherId >= 200 && weatherId <= 232) {
            return R.drawable.art_storm;
        } else if (weatherId >= 300 && weatherId <= 321) {
            return R.drawable.art_light_rain;
        } else if (weatherId >= 500 && weatherId <= 504) {
            return R.drawable.art_rain;
        } else if (weatherId == 511) {
            return R.drawable.art_snow;
        } else if (weatherId >= 520 && weatherId <= 531) {
            return R.drawable.art_rain;
        } else if (weatherId >= 600 && weatherId <= 622) {
            return R.drawable.art_rain;
        } else if (weatherId >= 701 && weatherId <= 761) {
            return R.drawable.art_fog;
        } else if (weatherId == 761 || weatherId == 781) {
            return R.drawable.art_storm;
        } else if (weatherId == 800) {
            return R.drawable.art_clear;
        } else if (weatherId == 801) {
            return R.drawable.art_light_clouds;
        } else if (weatherId >= 802 && weatherId <= 804) {
            return R.drawable.art_clouds;
        }
        return -1;
    }
}
