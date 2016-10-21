package com.example.android.sunshine.app.data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.format.Time;
import android.util.Log;

/**
 * Created by hnoct on 10/12/2016.
 *
 * This class will be used to store the names of all the columns in the database for easy, universal
 * access from any class or activity. Note how all values and methods are public and static.
 */

public class WeatherContract {
    // The "content authority" is basically the equivalent of a domain name for application URIs
    public static final String CONTENT_AUTHORITY = "com.example.android.sunshine.app";

    // Create the base URI utilizing the content authority
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    // Paths are like directories. In this case, we need the directories of the weather and
    // location. These will translate to the weather and location tables of our database
    public static final String PATH_WEATHER = "weather";
    public static final String PATH_LOCATION = "location";

     /*
      * Helper method to normalize all dates in the database to the Julian day at UTC
      */
    public static long normalizeDate(long startDate) {
        Time time = new Time();
        // Set the time to the first day pulled from OWM
        time.set(startDate);

        // Normalize the dates to UTC instead of GMT
        int julianDay = Time.getJulianDay(startDate, time.gmtoff);

        // Sets the correct Julian day according to UTC date retrieved above
        return time.setJulianDay(julianDay);
    }

    /*
     * Inner class to define the columns of the location table
     */

    public static final class LocationEntry implements BaseColumns {
        // The URI for location data
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_LOCATION).build();

        // For entire directories with multiple items
        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_LOCATION;
        // For single items
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_LOCATION;

        public static final String TABLE_NAME = "location";

        // The string used to query OWM as the location query
        public static final String COLUMN_LOCATION_SETTING = "location_setting";

        // Human-readable location provided by OWM API
        public static final String COLUMN_CITY_NAME = "city_name";

        // Used to pinpoint the location when we open the intent for Maps
        public static final String COLUMN_COORD_LONG = "coord_long";
        public static final String COLUMN_COORD_LAT = "coord_lat";

        // TODO: Write something here about this method once we find out what it does exactly
        public static Uri buildLocationUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

    };

    /*
     * Inner class to define the columns of the weather table
     */
    public static final class WeatherEntry implements BaseColumns {
        // The URI for weather data
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_WEATHER).build();

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_WEATHER;

        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_WEATHER;

        public static final String TABLE_NAME = "weather";

        // Column with the foreign key to the location table
        public static final String COLUMN_LOC_KEY = "location_id";
        // Date in milliseconds since the epoch <- Udacity's term. Not sure what this means?
        // Perhaps since beginning of Julian Period?
        public static final String COLUMN_DATE = "date";
        //Weather id from OWM to identify the icon to be used
        public static final String COLUMN_WEATHER_ID = "weather_id";


        // Short and long description of the weather
        public static final String COLUMN_SHORT_DESC = "short_desc";
        // public static final String COLUMN_LONG_DESC = "long_desc"; // Currently not used in
        // Udacity lesson, so it's commented out

        // Min and max temperatures of the day
        public static final String COLUMN_MIN_TEMP = "min";
        public static final String COLUMN_MAX_TEMP = "max";

        // Humidity stored as a float representing percentage
        public static final String COLUMN_HUMIDITY = "humidity";

        // Pressure stored as a float representing percentage
        public static final String COLUMN_PRESSURE = "pressure";

        // Wind speed stored as float in MPH
        public static final String COLUMN_WIND_SPEED = "wind";

        // Direction of the wind stored in meteorological degrees (e.g. 180° is south)
        public static final String COLUMN_DEGREES = "degrees";

        // TODO: Write something here about this method once we find out what it does exactly
        public static Uri buildWeatherUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        public static Uri buildWeatherLocation(String locationSetting) {
            return CONTENT_URI.buildUpon()
                    .appendPath(locationSetting)
                    .build();
        }

        /*
         * Returns the weather for all dates beginning from the start date associated with a
         * location
         */
        public static Uri buildWeatherLocationWithStartDate(
                String locationSetting, long startDate) {
            long normalizedDate = normalizeDate(startDate);
            // Query parameter specifies that we want to return multiple times instead of a single
            // date
            Uri uri = CONTENT_URI.buildUpon()
                    .appendPath(locationSetting)
                    .appendQueryParameter(COLUMN_DATE, Long.toString(normalizedDate))
                    .build();
            return uri;
        }

        /*
         * Returns a single weather item for the date and location specified
         */
        public static Uri buildWeatherLocationWithDate(String locationSetting, long date) {
            long normalizedDate = normalizeDate(date);
            return CONTENT_URI.buildUpon()
                    .appendPath(locationSetting)
                    .appendPath(Long.toString(normalizedDate))
                    .build();
        }

        /*
         * Returns the location setting from a given URI
         */
        public static String getLocationSettingFromUri(Uri uri) {
            // Location is always the second segment after the "weather table location" segment
            return uri.getPathSegments().get(1);
        }

        /*
         * Returns the date specified given a URI pointing to a single item of weather data given
         * the date and location setting
         */
        public static long getDateFromUri(Uri uri) {
            // Date is always the third segment after the weather table location and the location
            // setting segments
            return Long.parseLong(uri.getPathSegments().get(2));
        }

        /*
         * Returns the first date passed as a query parameters for a URI that requests the weather
         * data for multiple dates of a given location
         */
        public static long getStartDateFromUri(Uri uri) {
            String dateString = uri.getQueryParameter(COLUMN_DATE);
            if (dateString != null && dateString.length() > 0) {
                return Long.parseLong(dateString);
            } else {
                return 0;
            }
        }
    }


}
