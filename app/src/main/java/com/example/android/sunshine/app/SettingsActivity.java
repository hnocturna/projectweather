package com.example.android.sunshine.app;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import com.example.android.sunshine.app.data.WeatherContract;
import com.example.android.sunshine.app.sync.SunshineSyncAdapter;

/*
 * Activity that displays a list of user-configurable settings
 */
public class SettingsActivity extends PreferenceActivity
        implements Preference.OnPreferenceChangeListener, SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_general);

        // Attach OnPreferenceChangeListener so the UI summary can be updated on change
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_location_key)));
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_units_key)));
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_notifications_key)));
    }

    @Override
    protected void onResume() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);
        super.onResume();
    }

    @Override
    protected void onPause() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    /*
             * Attach listener to summary so it's always updated with the current preference value.
             * Fire once to initialize the summary when the activity is launched.
             */
    private void bindPreferenceSummaryToValue(Preference preference) {
        // Set listener to watch for value changes
        preference.setOnPreferenceChangeListener(this);

        // Set the preference summaries
        if (preference.equals(findPreference(getString(R.string.pref_notifications_key)))) {
            // Notification summary is set by the summary value depending on the checkbox
            onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(this)
                            .getString((String) preference.getSummary(), ""));
        } else {
            onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(this)
                            .getString(preference.getKey(), ""));
        }

    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        setPreferenceSummary(preference, value);
        return true;
    }

    private void setPreferenceSummary(Preference preference, Object value) {
        String stringValue = value.toString();
        String key = preference.getKey();

        if (preference instanceof ListPreference) {
            // For list preferences, look up the correct display value in the entries list
            ListPreference listPreference = (ListPreference) preference;
            int prefIndex = listPreference.findIndexOfValue(stringValue);
            if (prefIndex >= 0) {
                preference.setSummary(listPreference.getEntries()[prefIndex]);
            }
        } else if (key.equals(getString(R.string.pref_location_key))) {
            @SunshineSyncAdapter.LocationStatus int locationStatus = Utility.getLocationStatus(this);
            switch (locationStatus) {
                case SunshineSyncAdapter.LOCATION_STATUS_OKAY:
                    preference.setSummary(stringValue);
                    break;
                case SunshineSyncAdapter.LOCATION_INVALID:
                    preference.setSummary(getString((R.string.pref_location_error_description), stringValue));
                    break;
                case SunshineSyncAdapter.LOCATION_STATUS_UNKNOWN:
                    preference.setSummary(getString((R.string.pref_location_unknown_description), stringValue));
                    break;
                default:
                    preference.setSummary(stringValue);
            }
        } else {
            preference.setSummary(stringValue);
        }

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.pref_location_key))) {
            // If location is changed, location status needs to be reset and attempt another sync
            Utility.resetLocationStatus(this);
            SunshineSyncAdapter.syncImmediately(this);
        } else if (key.equals(getString(R.string.pref_units_key))) {
            // If units are changed, notify change so that the CursorLoader updates the cursor
            getContentResolver().notifyChange(WeatherContract.WeatherEntry.CONTENT_URI, null);
        } else if (key.equals(getString(R.string.pref_location_status_key))) {
            Preference locationPreference = findPreference(getString(R.string.pref_location_key));
            bindPreferenceSummaryToValue(locationPreference);
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public Intent getParentActivityIntent() {
        return super.getParentActivityIntent().addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }
}
