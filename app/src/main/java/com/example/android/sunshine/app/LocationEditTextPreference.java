package com.example.android.sunshine.app;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.widget.Button;
import android.widget.EditText;

/**
 * Created by hnoct on 1/5/2017.
 */

public class LocationEditTextPreference extends EditTextPreference {
    // Constants
    private final int DEFAULT_MIN_LOCATION_LENGTH = 2;

    // Member Variables
    int mMinLength;

    public LocationEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray typedArray = context.obtainStyledAttributes(
                attrs,
                R.styleable.LocationEditTextPreference,
                0, 0);
        try {
            mMinLength = typedArray.getInteger(R.styleable.LocationEditTextPreference_minLength,
                    DEFAULT_MIN_LOCATION_LENGTH);
        } finally {
            typedArray.recycle();
        }
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);
        // Get the text being edited
        EditText editText = getEditText();

        // Add a listener to detect when the text has changed
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int count, int after) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                // Set whether the positive button is enabled or not depending on whether the length
                // of the input text is greater than or less than mMinLength

                // Get the dialog
                Dialog dialog = getDialog();
                if (dialog instanceof AlertDialog) {

                    // Cast the dialog as an AlertDialog
                    AlertDialog alertDialog = (AlertDialog) dialog;

                    // Retrieve the positive button
                    Button positiveButton = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);

                    // Disable the positive button if input text is less than mMinLength; enable if
                    // it is greater
                    if (editable.length() < mMinLength) {
                        positiveButton.setEnabled(false);
                    } else {
                        positiveButton.setEnabled(true);
                    }
                }
            }
        });
    }
}
