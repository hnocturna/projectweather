package com.example.android.sunshine.app;

import android.content.Context;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.android.sunshine.app.data.WeatherContract.WeatherEntry;

/**
 * Created by hnoct on 10/20/2016.
 * {@link ForecastAdapter} exposes a list of weather forecasts from a {@link Cursor} to a
 * {@link android.widget.ArrayAdapter}
 */

public class ForecastAdapter extends RecyclerView.Adapter<ForecastAdapter.ForecastAdapterViewHolder> {
    private final String LOG_TAG = ForecastAdapter.class.getSimpleName();
    private final static int VIEW_TYPE_TODAY = 0;
    private final static int VIEW_TYPE_FUTURE = 1;
    private boolean useTodayLayout = true;

    // Member Variables
    private Cursor mCursor;
    final private Context mContext;
    final private TextView mEmptyView;
    private ForecastAdapterOnClickHandler mForecastAdapterOnClickHandler;

    @Override
    public int getItemViewType(int position) {
        if (position == 0 && useTodayLayout) {
            return VIEW_TYPE_TODAY;
        } else {
            return VIEW_TYPE_FUTURE;
        }
    }

    @Override
    public int getItemCount() {
        if (mCursor == null) {
            return 0;
        }
        return mCursor.getCount();
    }

    public Cursor getCursor() {
        return mCursor;
    }

    public Cursor swapCursor(Cursor newCursor) {
        if (mCursor != newCursor) {
            mCursor = newCursor;
            notifyDataSetChanged();
        }
        if (getItemCount() <= 0) {
            mEmptyView.setVisibility(View.VISIBLE);
        } else {
            mEmptyView.setVisibility(View.GONE);
        }
        return mCursor;
    }

    public ForecastAdapter(Context context, ForecastAdapterOnClickHandler clickHandler, TextView emptyView) {
        mContext = context;
        mForecastAdapterOnClickHandler = clickHandler;
        mEmptyView = emptyView;
    }


    public void setUseTodayLayout(boolean useTodayLayout) {
        this.useTodayLayout = useTodayLayout;
    }

    @Override
    public ForecastAdapterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (parent instanceof RecyclerView) {
            int layoutId = -1;
            switch (viewType) {
                case VIEW_TYPE_TODAY: {
                    layoutId = R.layout.list_item_forecast_today;
                    break;
                }
                case VIEW_TYPE_FUTURE: {
                    layoutId = R.layout.list_item_forecast;
                    break;
                }
            }
            View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
            view.setFocusable(true);
            return new ForecastAdapterViewHolder(view);
        } else {
            throw new RuntimeException("Not bound to RecyclerViewSelection");
        }
    }

    @Override
    public void onBindViewHolder(ForecastAdapterViewHolder viewHolder, int position) {
        // Parse the date from the Cursor into a user-readable date format
        mCursor.moveToPosition(position);
        int weatherId = mCursor.getInt(ForecastFragment.COL_WEATHER_CONDITION_ID);
        String date = Utility.getFriendlyDayString(
                mContext,
                mCursor.getLong(ForecastFragment.COL_WEATHER_DATE)
        );

        // Convert the temperature to fahrenheit if necessary
        String highTemperature = Utility.formatTemperature(
                mContext,
                mCursor.getDouble(ForecastFragment.COL_MAX_TEMP),
                Utility.isMetric(mContext)
        );
        String lowTemperature = Utility.formatTemperature(
                mContext,
                mCursor.getDouble(ForecastFragment.COL_MIN_TEMP),
                Utility.isMetric(mContext)
        );

        // Get weather description from weather condition ID to be used in content description
        String weatherDescription = Utility.getStringForWeatherCondition(mContext, weatherId);

        // Set the information for each item in the ViewHolder
        viewHolder.dateView.setText(date);
        viewHolder.descriptionView.setText(mCursor.getString(ForecastFragment.COL_SHORT_DESC));
        viewHolder.highView.setText(highTemperature);
        viewHolder.lowView.setText(lowTemperature);

        // Set content descriptions. Icon does not require content description because it would be
        // repetitive and it's not individually selectable anyways
        viewHolder.descriptionView.setContentDescription(mContext.getString(R.string.a11y_forecast,
                weatherDescription));
        viewHolder.highView.setContentDescription(mContext.getString(R.string.a11y_high_temp,
                highTemperature));
        viewHolder.lowView.setContentDescription(mContext.getString(R.string.a11y_low_temp,
                lowTemperature));

        int defaultImage;
        switch (getItemViewType(position)) {
            case VIEW_TYPE_TODAY:
                defaultImage = Utility.getArtResourceForWeatherCondition(weatherId);
                break;
            case VIEW_TYPE_FUTURE:
                defaultImage = Utility.getIconResourceForWeatherCondition(weatherId);
                break;
        }

        // Utilize online resource if icon exists, otherwise fall back to integrated icons
        Glide.with(mContext)
                .load(Utility.getArtUrlForWeatherCondition(mContext, weatherId))
                .error(Utility.getArtResourceForWeatherCondition(weatherId))
                .into(viewHolder.iconView);
    }

//    /*
//     * Populates the layout with values from our cursor
//     */
//    @Override
//    public void bindView(View view, Context context, Cursor cursor) {
//        // Parse the date from the Cursor into a user-readable date format
//        int viewType = getItemViewType(cursor.getPosition());
//
//        int weatherId = cursor.getInt(ForecastFragment.COL_WEATHER_CONDITION_ID);
//        String date = Utility.getFriendlyDayString(
//                context,
//                cursor.getLong(ForecastFragment.COL_WEATHER_DATE)
//        );
//
//        // Convert the temperature to fahrenheit if necessary
//        String highTemperature = Utility.formatTemperature(
//                context,
//                cursor.getDouble(ForecastFragment.COL_MAX_TEMP),
//                Utility.isMetric(context)
//        );
//        String lowTemperature = Utility.formatTemperature(
//                context,
//                cursor.getDouble(ForecastFragment.COL_MIN_TEMP),
//                Utility.isMetric(context)
//        );
//
//        // Get weather description from weather condition ID to be used in content description
//        String weatherDescription = Utility.getStringForWeatherCondition(context, weatherId);
//
//        // Retrieve the ViewHolder pattern from the tag passed from newView
//        ForecastAdapterViewHolder viewHolder = (ForecastAdapterViewHolder) view.getTag();
//
//        // Set the information for each item in the ViewHolder
//        viewHolder.dateView.setText(date);
//        viewHolder.descriptionView.setText(cursor.getString(ForecastFragment.COL_SHORT_DESC));
//        viewHolder.highView.setText(highTemperature);
//        viewHolder.lowView.setText(lowTemperature);
//
//        // Set content descriptions. Icon does not require content description because it would be
//        // repetitive and it's not individually selectable anyways
//        viewHolder.descriptionView.setContentDescription(context.getString(R.string.a11y_forecast,
//                weatherDescription));
//        viewHolder.highView.setContentDescription(context.getString(R.string.a11y_high_temp,
//                highTemperature));
//        viewHolder.lowView.setContentDescription(context.getString(R.string.a11y_low_temp,
//                lowTemperature));
//
////        if (viewType == VIEW_TYPE_TODAY) {
////            viewHolder.iconView.setImageResource(Utility.getArtResourceForWeatherCondition(weatherId));
////        } else {
////            viewHolder.iconView.setImageResource(Utility.getIconResourceForWeatherCondition(weatherId));
////        }
//
//        // Utilize online resource if icon exists, otherwise fall back to integrated icons
//        Glide.with(context)
//                .load(Utility.getArtUrlForWeatherCondition(context, weatherId))
//                .error(Utility.getArtResourceForWeatherCondition(weatherId))
//                .into(viewHolder.iconView);
//
//    }

    public interface ForecastAdapterOnClickHandler {
        void onClick(long date, ForecastAdapterViewHolder viewHolder);
    }

    public class ForecastAdapterViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        final ImageView iconView;
        final TextView dateView;
        final TextView descriptionView;
        final TextView highView;
        final TextView lowView;

        ForecastAdapterViewHolder(View view) {
            super(view);
            iconView = (ImageView) view.findViewById(R.id.list_item_icon);
            dateView = (TextView) view.findViewById(R.id.list_item_date_text);
            descriptionView = (TextView) view.findViewById(R.id.list_item_detail_text);
            highView = (TextView) view.findViewById(R.id.list_item_high_text);
            lowView = (TextView) view.findViewById(R.id.list_item_low_text);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            int adapterPosition = getAdapterPosition();
            mCursor.moveToPosition(adapterPosition);
            long date = mCursor.getLong(mCursor.getColumnIndex(WeatherEntry.COLUMN_DATE));
            mForecastAdapterOnClickHandler.onClick(date, this);
        }
    }
}
