package com.thibaudperso.sonycamera.timelapse.fragments;

import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.TextView;

import com.thibaudperso.sonycamera.R;
import com.thibaudperso.sonycamera.sdk.CameraIO;
import com.thibaudperso.sonycamera.timelapse.StepFragment;

public class TimelapseSettingsFragment extends StepFragment {

	public static String PREFERENCES_INITIAL_DELAY = "initial-delay";
	public static String PREFERENCES_INTERVAL_TIME = "interval-time";
	public static String PREFERENCES_FRAMES_COUNT = "frames-count";
	public static String PREFERENCES_LAST_IMAGE_REVIEW = "last-image-review";
	
	public static int DEFAULT_INITIAL_DELAY = 0;
	public static int DEFAULT_INTERVAL_TIME = 10;
	public static int DEFAULT_FRAMES_COUNT = -1;
	public static boolean DEFAULT_LAST_IMAGE_REVIEW = false;
	
	private EditText initialDelay;
	private EditText intervalTime;
	private EditText framesCount;
	private CompoundButton showImageReview;
	private CheckBox framesCountUnlimited;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		View resView = inflater.inflate(R.layout.fragment_timelapse_settings, container, false);
		
		
		initialDelay = (EditText) resView.findViewById(R.id.initialDelay);
		intervalTime = (EditText) resView.findViewById(R.id.intervalTime);
		framesCount = (EditText) resView.findViewById(R.id.framesCount);
		framesCountUnlimited = (CheckBox) resView.findViewById(R.id.framesCountUnlimited);
		TextView framesCountUnlimitedText = (TextView) resView.findViewById(R.id.framesCountUnlimitedText);
		showImageReview = (CompoundButton) resView.findViewById(R.id.showImageReview);
		
		framesCount.setFocusable(!framesCountUnlimited.isChecked());

		initialDelay.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			@Override
			public void afterTextChanged(Editable s) {
				checkAllFormValidity();
			}
		});

		intervalTime.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			@Override
			public void afterTextChanged(Editable s) {
				checkAllFormValidity();
			}
		});

		framesCount.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			@Override
			public void afterTextChanged(Editable s) {
				checkAllFormValidity();
			}
		});		

		framesCountUnlimited.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

				framesCount.setEnabled(!isChecked);
				framesCount.setFocusable(!isChecked);
				framesCount.setFocusableInTouchMode(!isChecked);

				checkAllFormValidity();

			}
		});


		framesCountUnlimitedText.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				framesCountUnlimited.toggle();
				framesCount.setEnabled(!framesCountUnlimited.isChecked());
				framesCount.setFocusable(!framesCountUnlimited.isChecked());
				framesCount.setFocusableInTouchMode(!framesCountUnlimited.isChecked());

				checkAllFormValidity();
			}
		});
		
		checkAllFormValidity();
		
		return resView;
	}
	

	/*
	 * Check form validity
	 */

	private boolean checkInitialDelay() {
		try {
			if(Integer.valueOf(initialDelay.getText().toString()) < 0) {
				initialDelay.setError(getString(R.string.timelapse_settings_positive_integer_error));
				return false;
			}
		} catch(NumberFormatException e) {
			initialDelay.setError(getString(R.string.timelapse_settings_positive_integer_error));
			return false;
		}

		initialDelay.setError(null);
		return true;
	}


	private boolean checkIntervalTime() {
		try {
			int value = Integer.valueOf(intervalTime.getText().toString());
			if(value <= 0) {
				intervalTime.setError(getString(R.string.timelapse_settings_positive_integer_error));
				return false;
			}
			if(value < CameraIO.MIN_TIME_BETWEEN_CAPTURE) {
				intervalTime.setError(getString(R.string.timelapse_settings_minimum_time_error, CameraIO.MIN_TIME_BETWEEN_CAPTURE));
				return false;
			}
		} catch(NumberFormatException e) {
			intervalTime.setError(getString(R.string.timelapse_settings_positive_integer_error));
			return false;
		}

		intervalTime.setError(null);
		return true;
	}

	private boolean checkFramesCount() {

		if(framesCountUnlimited.isChecked()) {
			framesCount.setError(null);
			return true;
		}

		try {
			if(Integer.valueOf(framesCount.getText().toString()) <= 0) {
				framesCount.setError(getString(R.string.timelapse_settings_positive_integer_error));
				return false;
			}
		} catch(NumberFormatException e) {
			framesCount.setError(getString(R.string.timelapse_settings_positive_integer_error));
			return false;
		}

		framesCount.setError(null);
		return true;
	}

	private void checkAllFormValidity() {

		setStepCompleted(checkInitialDelay() && checkFramesCount() && checkIntervalTime());

	}

	@Override
	public void onExitFragment() {
		super.onExitFragment();
		
		try {
			Editor preferencesEditor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
			
			preferencesEditor.putInt(PREFERENCES_INITIAL_DELAY, Integer.parseInt(initialDelay.getText().toString()));
			preferencesEditor.putInt(PREFERENCES_INTERVAL_TIME, Integer.parseInt(intervalTime.getText().toString()));
			preferencesEditor.putInt(PREFERENCES_FRAMES_COUNT, framesCountUnlimited.isChecked() ? -1 : Integer.parseInt(framesCount.getText().toString()));
			preferencesEditor.putBoolean(PREFERENCES_LAST_IMAGE_REVIEW, showImageReview.isChecked());
	
			preferencesEditor.apply();
		
		} catch(NumberFormatException ignored) {}
	}
	
	@Override
	public Spanned getInformation() {
		return Html.fromHtml(getString(R.string.timelapse_settings_information_message));
	}
	
}
