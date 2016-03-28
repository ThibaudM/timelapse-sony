package com.thibaudperso.sonycamera.timelapse;


import android.support.v4.app.Fragment;
import android.text.Spanned;

public abstract class StepFragment extends Fragment {

	private StepCompletedListener mStepCompletedListener;
	private boolean mStepCompleted;
	protected boolean mIsActive;
	
	private boolean fragmentStarted = false;
	private boolean onEnterFragmentTooFast = false;
	
	public StepFragment() {
		this.mStepCompleted = false;
		this.mIsActive = false;
	}
	
	public void setStepCompletedListener(StepCompletedListener stepCompletedListener) {
		this.mStepCompletedListener = stepCompletedListener;
	}
	
	protected void setStepCompleted(boolean isStepCompleted) {
		this.mStepCompleted = isStepCompleted;
				
		if(this.mStepCompletedListener != null) {
			this.mStepCompletedListener.stepCompleted(isStepCompleted);
		}
		
	}
	
	public boolean isStepCompleted() {
		return this.mStepCompleted;
	}
	
	public void onEnterFragment() {
		
		if(!fragmentStarted) {
			onEnterFragmentTooFast = true;
		}
		
		mIsActive = true;
	}
	public void onExitFragment() {
		mIsActive = false;
	}
	
	@Override
	public void onStart() {
		super.onStart();
		fragmentStarted = true;
		if(onEnterFragmentTooFast) {
			onEnterFragment();
		}
	}
	
	@Override
	public void onStop() {
		super.onStop();
		fragmentStarted = false;
		onEnterFragmentTooFast = false;
	}
	
	
	public abstract Spanned getInformation();

}
