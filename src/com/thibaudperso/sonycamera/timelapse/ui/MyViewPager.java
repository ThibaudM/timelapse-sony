package com.thibaudperso.sonycamera.timelapse.ui;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class MyViewPager extends ViewPager {

//	private boolean isStepCompleted = false;
//	private float lastX = 0;

	public MyViewPager(Context context) {
		super(context);
	}

	public MyViewPager(Context context, AttributeSet attrs) {
		super(context, attrs);
	}


	
	@Override
	public boolean onTouchEvent(MotionEvent event) {

//		if(isStepCompleted || detectSwipeToRight(event)) {
//			return super.onTouchEvent(event);
//		}
		
		return false;
	}

//	public void setStepCompleted(boolean stepCompleted) {
//		this.isStepCompleted = stepCompleted;
//	}


	// Detects the direction of swipe. Right or left. 
	// Returns true if swipe is in right direction
//	private boolean detectSwipeToRight(MotionEvent event){
//
//		boolean isSwipeToRight = true;
//
//		switch (event.getAction()) {
//		case MotionEvent.ACTION_DOWN: {
//			lastX = event.getX();
//			return true;
//		}
//
//		case MotionEvent.ACTION_MOVE: {
//
//			if (lastX > event.getX()) {
//				isSwipeToRight = false;
//			} else {
//				isSwipeToRight = true;
//			}
//
//			lastX = event.getX();
//			break;
//		}
//		}
//
//		return isSwipeToRight;
//
//	}
}
