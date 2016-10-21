package com.example.android.sunshine.app;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.android.sunshine.app.data.WeatherContract.WeatherEntry;

/**
 * Created by hnoct on 10/20/2016.
 * {@link ForecastAdapter} exposes a list of weather forecasts from a {@link Cursor} to a
 * {@link android.widget.ArrayAdapter}
 */

public class ForecastAdapter extends CursorAdapter {

    public ForecastAdapter(Context context, Cursor cursor, int flags) {
        super(context, cursor, flags);
    }

    /*
     * Helper method that formats the high and low temperatures into human-readable String
     */
    private String formatHighLows(double high, double low) {
        boolean isMetric = Utility.isMetric(mContext);  // Not 100% sure, but mContext is probably set by the constructor in the super.
        String highLowStr = Utility.formatTemperature(high, isMetric) + "/" + Utility.formatTemperature(low, isMetric);
        return highLowStr;
    }

    /*
     * Converts a row of data from the cursor to UX readable text
     */
    private String convertCursorRowToUXFormat(Cursor cursor) {
        // Get row indices for each column from the cursor
        int idx_max_temp = cursor.getColumnIndex(WeatherEntry.COLUMN_MAX_TEMP);
        int idx_min_temp = cursor.getColumnIndex(WeatherEntry.COLUMN_MIN_TEMP);
        int idx_date = cursor.getColumnIndex(WeatherEntry.COLUMN_DATE);
        int idx_short_desc = cursor.getColumnIndex(WeatherEntry.COLUMN_SHORT_DESC);

        String highAndLow = formatHighLows(
                cursor.getDouble(idx_max_temp),
                cursor.getDouble(idx_min_temp)
        );

        return Utility.formatDate(cursor.getLong(idx_date)) +
                " - " + cursor.getString(idx_short_desc) +
                " - " + highAndLow;
    }

    /*
     * Selects the layout to populate;
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_forecast, parent, false);
        return view;
    }

    /*
     * Populates the layout with values from our cursor
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView textView = (TextView) view.findViewById(R.id.list_item_forecast_textview);
        textView.setText(convertCursorRowToUXFormat(cursor));
    }
}
