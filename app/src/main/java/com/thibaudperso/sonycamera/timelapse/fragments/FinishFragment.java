package com.thibaudperso.sonycamera.timelapse.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.thibaudperso.sonycamera.R;
import com.thibaudperso.sonycamera.timelapse.StepFragment;

public class FinishFragment extends StepFragment {

	private FinishFragmentListener mListener;

	private TextView finishTextView;
	private TextView finishWithErrorsTextView;
	private TextView framesOverlapTextView;
	private View framesOverlapLayout;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		View resView = inflater.inflate(R.layout.fragment_finish, container, false);
		
		ImageView restartImage = (ImageView) resView.findViewById(R.id.finishRestartImage);
		TextView restartMessage = (TextView) resView.findViewById(R.id.finishRestartMessage);
	
		restartImage.setOnClickListener(onRestartClickListener);
		restartMessage.setOnClickListener(onRestartClickListener);
		
		finishTextView = (TextView) resView.findViewById(R.id.finishMessage);
		finishWithErrorsTextView = (TextView) resView.findViewById(R.id.finishWithErrorsMessage);
		framesOverlapTextView = (TextView) resView.findViewById(R.id.finishFramesOverlappingTextView);
		framesOverlapLayout = resView.findViewById(R.id.finishFramesOverlappingLayout);
		
		return resView;
	}
	
	public void setListener(FinishFragmentListener listener) {
		this.mListener = listener;
	}
	
	private OnClickListener onRestartClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			if(mListener != null) {
				mListener.onRestartProcess();
			}
		}
	};
	
	public void onEnterFragment() {
		super.onEnterFragment();
		setStepCompleted(true);
		
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
		boolean framesOverlapped = preferences.getBoolean(CaptureFragment.PREFERENCES_FRAMES_OVERLAPPED, false);

		finishTextView.setVisibility(framesOverlapped ? TextView.GONE : TextView.VISIBLE);
		finishWithErrorsTextView.setVisibility(framesOverlapped ? TextView.VISIBLE : TextView.GONE);
		
		framesOverlapTextView.setText(Html.fromHtml(getString(R.string.finish_summary_overlapping_frames_message)));
		framesOverlapLayout.setVisibility(framesOverlapped? TextView.VISIBLE : TextView.GONE);
		
	}
	
	@Override
	public void onExitFragment() {
		super.onExitFragment();
		setStepCompleted(false);
	}
	
	@Override
	public Spanned getInformation() {
		return null;
	}
	
}