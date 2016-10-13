package com.example.android.sunshine.app.data;

import android.provider.BaseColumns;
import android.text.format.Time;

/**
 * Created by hnoct on 10/12/2016.
 *
 * This class will be used to store the names of all the columns in the database for easy, universal
 * access from any class or activity. Note how all values and methods are public and static.
 */

public class WeatherContract {
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
        public static final String TABLE_NAME = "location";

        // The string used to query OWM as the location query
        public static final String COLUMN_LOCATION_SETTING = "location_setting";

        // Human-readable location provided by OWM API
        public static final String COLUMN_CITY_NAME = "city_name";

        // Used to pinpoint the location when we open the intent for Maps
        public static final String COLUMN_COORD_LONG = "coord_long";
        public static final String COLUMN_COORD_LAT = "coord_lat";

    };

    /*
     * Inner class to define the columns of the weather table
     */
    public static final class WeatherEntry implements BaseColumns {
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
    }


}
