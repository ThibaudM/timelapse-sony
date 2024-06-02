package com.thibaudperso.sonycamera.timelapse.ui.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.TextView;

import com.thibaudperso.sonycamera.R;
import com.thibaudperso.sonycamera.timelapse.Constants;
import com.thibaudperso.sonycamera.timelapse.model.IntervalometerSettings;
import com.thibaudperso.sonycamera.timelapse.service.IntervalometerService;
import com.thibaudperso.sonycamera.timelapse.ui.processing.ProcessingActivity;

import static com.thibaudperso.sonycamera.R.id.settings_initial_delay;
import static com.thibaudperso.sonycamera.R.id.settings_interval_time;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;


public class SettingsFragment extends Fragment {


    private SharedPreferences mPreferences;
    
    private EditText mInitialDelayEditText;
    private EditText mIntervalTimeEditText;
    private EditText mFramesCountEditText;
    private CheckBox mFramesCountUnlimitedCheckBox;
    private TextView mErrorTextView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_settings, container, false);

        ((TextView) rootView.findViewById(R.id.settings_message)).setText(Html.fromHtml
                (getString(R.string.settings_message)));

        mErrorTextView = (TextView) rootView.findViewById(R.id.settings_error);

        /*
         * Initial delay field
         */

        mInitialDelayEditText = (EditText) rootView.findViewById(settings_initial_delay);
        mInitialDelayEditText.setText(String.valueOf(
                mPreferences.getInt(Constants.PREFERENCES_INITIAL_DELAY,
                        Constants.DEFAULT_INITIAL_DELAY)));
        createEditTextChangedListener(mInitialDelayEditText, new TextChangedListener() {
            @Override
            public void onTextChanged(String text) {
                setPreference(Constants.PREFERENCES_INITIAL_DELAY, getInitialDelay());
            }
        });



        /*
         * Interval time field
         */

        mIntervalTimeEditText = (EditText) rootView.findViewById(settings_interval_time);
        mIntervalTimeEditText.setText(String.valueOf(
                mPreferences.getInt(Constants.PREFERENCES_INTERVAL_TIME,
                        Constants.DEFAULT_INTERVAL_TIME)));
        createEditTextChangedListener(mIntervalTimeEditText, new TextChangedListener() {
            @Override
            public void onTextChanged(String text) {
                setPreference(Constants.PREFERENCES_INTERVAL_TIME, getIntervalTime());
            }
        });


        /*
         * Frames count fields
         */

        int framesCount = mPreferences.getInt(Constants.PREFERENCES_FRAMES_COUNT,
                Constants.DEFAULT_FRAMES_COUNT);
        boolean framesCountUnlimited = framesCount == 0;

        mFramesCountEditText = (EditText) rootView.findViewById(R.id.settings_frames_count);
        mFramesCountEditText.setText(framesCountUnlimited ? "" : String.valueOf(framesCount));
        mFramesCountEditText.setEnabled(!framesCountUnlimited);
        mFramesCountEditText.setFocusable(!framesCountUnlimited);
        mFramesCountEditText.setFocusableInTouchMode(!framesCountUnlimited);
        createEditTextChangedListener(mFramesCountEditText, new TextChangedListener() {
            @Override
            public void onTextChanged(String text) {
                setPreference(Constants.PREFERENCES_FRAMES_COUNT, getFramesCount());
            }
        });

        mFramesCountUnlimitedCheckBox = (CheckBox) rootView.findViewById(R.id.settings_frames_count_unlimited);
        mFramesCountUnlimitedCheckBox.setChecked(framesCountUnlimited);
        mFramesCountUnlimitedCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                mFramesCountEditText.setEnabled(!isChecked);
                mFramesCountEditText.setFocusable(!isChecked);
                mFramesCountEditText.setFocusableInTouchMode(!isChecked);

                setPreference(Constants.PREFERENCES_FRAMES_COUNT, getFramesCount());

            }
        });


        /*
         * Navigation buttons
         */
        rootView.findViewById(R.id.settings_previous).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        getActivity().onBackPressed();
                    }
                });

        rootView.findViewById(R.id.settings_next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(checkFields()) {
                    startService();
                }
            }
        });

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mErrorTextView.setVisibility(View.GONE);
    }

    private boolean checkFields() {

        if(getInitialDelay() < 0) {
            mErrorTextView.setVisibility(View.VISIBLE);
            mErrorTextView.setText(R.string.settings_initial_delay_positive_integer_error);
            return false;
        }

        if(getIntervalTime() <= 0) {
            mErrorTextView.setVisibility(View.VISIBLE);
            mErrorTextView.setText(R.string.settings_interval_time_positive_integer_error);
            return false;
        }

        if(getFramesCount() < 0) {
            mErrorTextView.setVisibility(View.VISIBLE);
            mErrorTextView.setText(R.string.settings_frames_count_positive_integer_error);
            return false;
        }

        mErrorTextView.setVisibility(View.GONE);
        return true;
    }


    private int getInitialDelay() {
        try {
            return Integer.parseInt(mInitialDelayEditText.getText().toString());
        } catch (NumberFormatException ignored) {
            return Constants.DEFAULT_INITIAL_DELAY;
        }
    }

    private int getIntervalTime() {
        try {
            return Integer.parseInt(mIntervalTimeEditText.getText().toString());
        } catch (NumberFormatException ignored) {
            return Constants.DEFAULT_INTERVAL_TIME;
        }
    }

    private int getFramesCount() {
        try {
            if(mFramesCountUnlimitedCheckBox.isChecked())
                return 0;
            return Integer.parseInt(mFramesCountEditText.getText().toString());
        } catch (NumberFormatException ignored) {
            return Constants.DEFAULT_FRAMES_COUNT;
        }
    }


    private void startService() {

//        ActivityCompat.finishAffinity(getActivity());
        Intent intent = new Intent(getContext(), ProcessingActivity.class);
        startActivity(intent);

        IntervalometerSettings settings = new IntervalometerSettings();
        settings.initialDelay = getInitialDelay();
        settings.intervalTime = getIntervalTime();
        settings.framesCount = getFramesCount();

        Intent serviceIntent = new Intent(getActivity(), IntervalometerService.class);
        serviceIntent.setAction(IntervalometerService.ACTION_START);
        serviceIntent.putExtra(IntervalometerService.EXTRA_SETTINGS, settings);
        getActivity().startService(serviceIntent);

    }

    private void setPreference(String prefName, Object prefValue) {

        SharedPreferences.Editor editor = mPreferences.edit();
        if (prefValue instanceof Integer) {
            editor.putInt(prefName, (Integer) prefValue);
        } else if (prefValue instanceof Float) {
            editor.putFloat(prefName, (Float) prefValue);
        } else if (prefValue instanceof Boolean) {
            editor.putBoolean(prefName, (Boolean) prefValue);
        }
        editor.apply();
    }


    /*
     * Shortcut for afterTextChanged listener
     */

    interface TextChangedListener {
        void onTextChanged(String text);
    }

    private void createEditTextChangedListener(EditText editText, final TextChangedListener listener) {
        editText.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                listener.onTextChanged(s.toString());
            }
        });
    }

}
