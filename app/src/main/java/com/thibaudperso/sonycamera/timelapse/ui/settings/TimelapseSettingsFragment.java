package com.thibaudperso.sonycamera.timelapse.ui.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;

import com.thibaudperso.sonycamera.R;
import com.thibaudperso.sonycamera.timelapse.control.TimelapseSettings;

import java.util.Locale;

public class TimelapseSettingsFragment extends Fragment {


    private SharedPreferences mPreferences;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_timelapse_settings, container, false);

        final EditText initialDelayEditText = (EditText) rootView.findViewById(R.id.initialDelay);
        final EditText intervalTimeEditText = (EditText) rootView.findViewById(R.id.intervalTime);
        final EditText framesCountEditText = (EditText) rootView.findViewById(R.id.framesCount);
        final CheckBox framesCountUnlimitedCheckBox = (CheckBox) rootView.findViewById(R.id.framesCountUnlimited);
        final CheckBox showImageReviewCheckBox = (CheckBox) rootView.findViewById(R.id.showImageReview);


        initialDelayEditText.setText(String.format(Locale.US, "%d",
                mPreferences.getInt(TimelapseSettings.PREFERENCES_INITIAL_DELAY,
                        TimelapseSettings.DEFAULT_INITIAL_DELAY)));
        initialDelayEditText.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                setPreference(TimelapseSettings.PREFERENCES_INITIAL_DELAY, s.length() == 0 ?
                        TimelapseSettings.DEFAULT_INITIAL_DELAY : Integer.parseInt(s.toString()));
            }
        });

        intervalTimeEditText.setText(String.format(Locale.US, "%d",
                mPreferences.getInt(TimelapseSettings.PREFERENCES_INTERVAL_TIME,
                        TimelapseSettings.DEFAULT_INTERVAL_TIME)));
        intervalTimeEditText.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                setPreference(TimelapseSettings.PREFERENCES_INTERVAL_TIME, s.length() == 0 ?
                        TimelapseSettings.DEFAULT_INTERVAL_TIME : Integer.parseInt(s.toString()));
            }
        });


        int framesCount = mPreferences.getInt(TimelapseSettings.PREFERENCES_FRAMES_COUNT,
                TimelapseSettings.DEFAULT_FRAMES_COUNT);
        boolean framesCountUnlimited = framesCount == 0;

        framesCountEditText.setText(framesCountUnlimited ? "" :
                String.format(Locale.US, "%d", framesCount));
        framesCountEditText.setEnabled(!framesCountUnlimited);
        framesCountEditText.setFocusable(!framesCountUnlimited);
        framesCountEditText.setFocusableInTouchMode(!framesCountUnlimited);
        framesCountEditText.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                setPreference(TimelapseSettings.PREFERENCES_FRAMES_COUNT, s.length() == 0 ?
                        TimelapseSettings.DEFAULT_FRAMES_COUNT : Integer.parseInt(s.toString()));
            }
        });

        framesCountUnlimitedCheckBox.setChecked(framesCountUnlimited);
        framesCountUnlimitedCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                framesCountEditText.setEnabled(!isChecked);
                framesCountEditText.setFocusable(!isChecked);
                framesCountEditText.setFocusableInTouchMode(!isChecked);

                setPreference(TimelapseSettings.PREFERENCES_FRAMES_COUNT, isChecked ? 0 :
                        framesCountEditText.length() == 0 ?
                                TimelapseSettings.DEFAULT_FRAMES_COUNT :
                                Integer.parseInt(framesCountEditText.getText().toString()));

            }
        });

        showImageReviewCheckBox.setChecked(mPreferences.getBoolean(
                TimelapseSettings.PREFERENCES_LAST_PICTURE_REVIEW,
                TimelapseSettings.DEFAULT_LAST_PICTURE_REVIEW));
        showImageReviewCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setPreference(TimelapseSettings.PREFERENCES_LAST_PICTURE_REVIEW, isChecked);
            }
        });

//        rootView.findViewById(R.id.timelapseSettingsPrevious).setOnClickListener(
//                new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        getActivity().onBackPressed();
//                    }
//                });

        rootView.findViewById(R.id.timelapseSettingsNext).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), CameraSettingsActivity.class);
                startActivity(intent);
            }
        });

        return rootView;
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


}
