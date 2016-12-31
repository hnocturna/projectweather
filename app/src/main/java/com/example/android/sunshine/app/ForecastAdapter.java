package com.example.android.sunshine.app;

import android.content.Context;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.android.sunshine.app.data.WeatherContract.WeatherEntry;

/**
 * Created by hnoct on 10/20/2016.
 * {@link ForecastAdapter} exposes a list of weather forecasts from a {@link Cursor} to a
 * {@link android.widget.ArrayAdapter}
 */

public class ForecastAdapter extends CursorAdapter {
    private final String LOG_TAG = ForecastAdapter.class.getSimpleName();
    private final static int VIEW_TYPE_TODAY = 0;
    private final static int VIEW_TYPE_FUTURE = 1;
    private boolean useTodayLayout = true;

    @Override
    public int getItemViewType(int position) {
        if (position == 0 && useTodayLayout) {
            return VIEW_TYPE_TODAY;
        } else {
            return VIEW_TYPE_FUTURE;
        }
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    public ForecastAdapter(Context context, Cursor cursor, int flags) {
        super(context, cursor, flags);
    }

    /*
     * Helper method that formats the high and low temperatures into human-readable String
     *
     * deprecated
     */
//    private String formatHighLows(double high, double low) {
//        boolean isMetric = Utility.isMetric(mContext);  // Not 100% sure, but mContext is probably set by the constructor in the super.
//        String highLowStr = Utility.formatTemperature(high, isMetric) + "/" + Utility.formatTemperature(low, isMetric);
//        return highLowStr;
//    }

    /*
     * Converts a row of data from the cursor to UX readable text
     *
     * Deprecated
     */
//    private String convertCursorRowToUXFormat(Cursor cursor) {
//        // Get row indices for each column from the cursor
////        int idx_max_temp = cursor.getColumnIndex(WeatherEntry.COLUMN_MAX_TEMP);
////        int idx_min_temp = cursor.getColumnIndex(WeatherEntry.COLUMN_MIN_TEMP);
////        int idx_date = cursor.getColumnIndex(WeatherEntry.COLUMN_DATE);
////        int idx_short_desc = cursor.getColumnIndex(WeatherEntry.COLUMN_SHORT_DESC);
//
//        String highAndLow = formatHighLows(
//                cursor.getDouble(ForecastFragment.COL_MAX_TEMP),
//                cursor.getDouble(ForecastFragment.COL_MIN_TEMP)
//        );
//
//        return Utility.formatDate(cursor.getLong(ForecastFragment.COL_WEATHER_DATE)) +
//                " - " + cursor.getString(ForecastFragment.COL_SHORT_DESC) +
//                " - " + highAndLow;
//    }

    /*
     * Selects the layout to populate;
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        int viewType = getItemViewType(cursor.getPosition());
        int layoutId = -1;

        if (viewType == VIEW_TYPE_TODAY) {
            layoutId = R.layout.list_item_forecast_today;
        } else {
            layoutId = R.layout.list_item_forecast;
        }
        View view = LayoutInflater.from(context).inflate(layoutId, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);
        return view;
    }

    public void setUseTodayLayout(boolean useTodayLayout) {
        this.useTodayLayout = useTodayLayout;
    }

    /*
     * Populates the layout with values from our cursor
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // Parse the date from the Cursor into a user-readable date format
        int viewType = getItemViewType(cursor.getPosition());

        int weatherId = cursor.getInt(ForecastFragment.COL_WEATHER_CONDITION_ID);
        String date = Utility.getFriendlyDayString(
                context,
                cursor.getLong(ForecastFragment.COL_WEATHER_DATE)
        );

        // Convert the temperature to fahrenheit if necessary
        String highTemperature = Utility.formatTemperature(
                context,
                cursor.getDouble(ForecastFragment.COL_MAX_TEMP),
                Utility.isMetric(context)
        );
        String lowTemperature = Utility.formatTemperature(
                context,
                cursor.getDouble(ForecastFragment.COL_MIN_TEMP),
                Utility.isMetric(context)
        );

        // Retrieve the ViewHolder pattern from the tag passed from newView
        ViewHolder viewHolder = (ViewHolder) view.getTag();

        // Set the information for each item in the ViewHolder
        viewHolder.dateView.setText(date);
        viewHolder.descriptionView.setText(cursor.getString(ForecastFragment.COL_SHORT_DESC));
        viewHolder.highView.setText(highTemperature);
        viewHolder.lowView.setText(lowTemperature);

        if (viewType == VIEW_TYPE_TODAY) {
            viewHolder.iconView.setImageResource(Utility.getArtResourceForWeatherCondition(weatherId));
        } else {
            viewHolder.iconView.setImageResource(Utility.getIconResourceForWeatherCondition(weatherId));
        }
    }

    private static class ViewHolder {
        final ImageView iconView;
        final TextView dateView;
        final TextView descriptionView;
        final TextView highView;
        final TextView lowView;

        ViewHolder(View view) {
            iconView = (ImageView) view.findViewById(R.id.list_item_icon);
            dateView = (TextView) view.findViewById(R.id.list_item_date_text);
            descriptionView = (TextView) view.findViewById(R.id.list_item_detail_text);
            highView = (TextView) view.findViewById(R.id.list_item_high_text);
            lowView = (TextView) view.findViewById(R.id.list_item_low_text);
        }
    }
}
