package com.example.android.sunshine.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.example.android.sunshine.app.sync.SunshineSyncAdapter;

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

    /**
     * Overload of the formatTemperature method for access in other classes
     * @param context for accessing SharedPreferences
     * @param temperature from database
     * @return temperature formatted in either metric or imperial based on user preferences
     */
    public static String formatTemperature(Context context, double temperature) {
        return formatTemperature(context, temperature, isMetric(context));
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

    /**
     * Checks whether there is active network connection
     * @param context interface for global context
     * @return true if network is available, else false
     */
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo= connectivityManager.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnected()) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Resets the location status {@link SunshineSyncAdapter#setLocationStatus} to unknown when the
     * location settings is changed by the user so that its validation status can be properly
     * displayed
     * @param context interface for global context
     */
    public static void resetLocationStatus(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(context.getString(R.string.pref_location_status_key),
                SunshineSyncAdapter.LOCATION_STATUS_UNKNOWN);
        editor.apply();
    }

    /**
     * Checks location status as set by {@link SunshineSyncAdapter#setLocationStatus}
     *
     * @param context interface for global context
     * @return location status integer type
     */
    @SuppressWarnings("ResourceType")
    public static @SunshineSyncAdapter.LocationStatus int getLocationStatus(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getInt(context.getString(R.string.pref_location_status_key),
                SunshineSyncAdapter.LOCATION_STATUS_UNKNOWN);
    }

    /**
     * Helper method for retrieving the string depending on the weather condition id
     * @param context interface for global ocntext
     * @param weatherId from OpenWeatherMap API
     * @return string for the weather condition. null if not found.
     */
    public static String getStringForWeatherCondition(Context context, int weatherId) {
        // Based on weather code data found at:
        // http://bugs.openweathermap.org/projects/api/wiki/Weather_Condition_Codes
        int stringId;
        if (weatherId >= 200 && weatherId <= 232) {
            stringId = R.string.condition_2xx;
        } else if (weatherId >= 300 && weatherId <= 321) {
            stringId = R.string.condition_3xx;
        } else switch(weatherId) {
            case 500:
                stringId = R.string.condition_500;
                break;
            case 501:
                stringId = R.string.condition_501;
                break;
            case 502:
                stringId = R.string.condition_502;
                break;
            case 503:
                stringId = R.string.condition_503;
                break;
            case 504:
                stringId = R.string.condition_504;
                break;
            case 511:
                stringId = R.string.condition_511;
                break;
            case 520:
                stringId = R.string.condition_520;
                break;
            case 531:
                stringId = R.string.condition_531;
                break;
            case 600:
                stringId = R.string.condition_600;
                break;
            case 601:
                stringId = R.string.condition_601;
                break;
            case 602:
                stringId = R.string.condition_602;
                break;
            case 611:
                stringId = R.string.condition_611;
                break;
            case 612:
                stringId = R.string.condition_612;
                break;
            case 615:
                stringId = R.string.condition_615;
                break;
            case 616:
                stringId = R.string.condition_616;
                break;
            case 620:
                stringId = R.string.condition_620;
                break;
            case 621:
                stringId = R.string.condition_621;
                break;
            case 622:
                stringId = R.string.condition_622;
                break;
            case 701:
                stringId = R.string.condition_701;
                break;
            case 711:
                stringId = R.string.condition_711;
                break;
            case 721:
                stringId = R.string.condition_721;
                break;
            case 731:
                stringId = R.string.condition_731;
                break;
            case 741:
                stringId = R.string.condition_741;
                break;
            case 751:
                stringId = R.string.condition_751;
                break;
            case 761:
                stringId = R.string.condition_761;
                break;
            case 762:
                stringId = R.string.condition_762;
                break;
            case 771:
                stringId = R.string.condition_771;
                break;
            case 781:
                stringId = R.string.condition_781;
                break;
            case 800:
                stringId = R.string.condition_800;
                break;
            case 801:
                stringId = R.string.condition_801;
                break;
            case 802:
                stringId = R.string.condition_802;
                break;
            case 803:
                stringId = R.string.condition_803;
                break;
            case 804:
                stringId = R.string.condition_804;
                break;
            case 900:
                stringId = R.string.condition_900;
                break;
            case 901:
                stringId = R.string.condition_901;
                break;
            case 902:
                stringId = R.string.condition_902;
                break;
            case 903:
                stringId = R.string.condition_903;
                break;
            case 904:
                stringId = R.string.condition_904;
                break;
            case 905:
                stringId = R.string.condition_905;
                break;
            case 906:
                stringId = R.string.condition_906;
                break;
            case 951:
                stringId = R.string.condition_951;
                break;
            case 952:
                stringId = R.string.condition_952;
                break;
            case 953:
                stringId = R.string.condition_953;
                break;
            case 954:
                stringId = R.string.condition_954;
                break;
            case 955:
                stringId = R.string.condition_955;
                break;
            case 956:
                stringId = R.string.condition_956;
                break;
            case 957:
                stringId = R.string.condition_957;
                break;
            case 958:
                stringId = R.string.condition_958;
                break;
            case 959:
                stringId = R.string.condition_959;
                break;
            case 960:
                stringId = R.string.condition_960;
                break;
            case 961:
                stringId = R.string.condition_961;
                break;
            case 962:
                stringId = R.string.condition_962;
                break;
            default:
                return context.getString(R.string.condition_unknown, weatherId);
        }
        return context.getString(stringId);
    }

    /**
     * Helper method for retreiving the correct URL for art resources from a third party website
     * @param context interface for global context
     * @param weatherId weather condition code from Open Weather Map
     * @return String URL for the online location of the art resource
     */
    public static String getArtUrlForWeatherCondition(Context context, int weatherId) {
        // Get the user's preference for icon pack
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String iconPackSelection = prefs.getString(
                context.getString(R.string.pref_icons_key),
                context.getString(R.string.pref_icons_colored)
        );

        // Get the URL based on the user's selection
        String iconPackUrl = iconPackSelection.equals(context.getString(R.string.pref_icons_colored))
                ? context.getString(R.string.pref_icon_colored_url)
                : context.getString(R.string.pref_icon_mono_url);

        if (weatherId >= 200 && weatherId <=232) {
            return String.format(iconPackUrl, "storm");
        } else if (weatherId >= 500 && weatherId <= 531) {
            return String.format(iconPackUrl, "rain");
        } else if (weatherId  >= 600 && weatherId <=622) {
            return String.format(iconPackUrl, "snow");
        } else if (weatherId == 721 || weatherId == 741) {
            return String.format(iconPackUrl, "fog");
        } else if (weatherId == 800) {
            return String.format(iconPackUrl, "clear");
        } else if (weatherId == 801) {
            return String.format(iconPackUrl, "light_clouds");
        } else if (weatherId >= 802 && weatherId <= 804) {
            return String.format(iconPackUrl, "clouds");
        } else {
            return null;
        }
    }
}
