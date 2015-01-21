package com.thibaudperso.sonycamera.timelapse;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;

/**
 * Countdown based on android.os.CountDownTimer
 * 
 * @author Thibaud Michel
 *
 */
@SuppressLint("HandlerLeak")
public abstract class MyCountDownTicks {

	/**
	 * The number of ticks in the future 
	 */
	private int mRemainingTicks;

	/**
	 * The interval in millis that the user receives callbacks
	 */
	private final long mCountdownInterval;
	/**
	 * If the counter shoud wait for a tickProcessed() call before firing a new tick
	 */
    private final boolean mWaitForTicksProcessing;
	

	
	/**
	 * If set to true, it will works has a normal counter with interval time
	 */
	private boolean isUnlimited;
	
	/**
	 * The number of ticks in the future when unlimited
	 */
	private int mNumberOfTicks;
	
	/**
	 * Current tick has been processed
	 */
	private boolean currentTickProcessed = true;
	/**
	 * States if the handler is waiting for the last tick to be processed
	 */
	private boolean isWaitingForTickProcessing = false;
	
	/**
	 * @param numberOfTicks The number of ticks in the future from the call
	 *   to {@link #start()} until the countdown is done and {@link #onFinish()}
	 *   is called. If set to -1, countdown is an unlimited normal counter, you 
	 *   have to call {@link #cancel()} to stop it.
	 * @param countDownInterval The interval along the way to receive
	 *   {@link #onTick(long)} callbacks.
	 */
	public MyCountDownTicks(int numberOfTicks, long countDownInterval) {
		this(numberOfTicks, countDownInterval, false);
	}
	/**
	 * @param numberOfTicks The number of ticks in the future from the call
	 *   to {@link #start()} until the countdown is done and {@link #onFinish()}
	 *   is called. If set to -1, countdown is an unlimited normal counter, you 
	 *   have to call {@link #cancel()} to stop it.
	 * @param countDownInterval The interval along the way to receive
	 *   {@link #onTick(long)} callbacks.
	 * @param waitForTicksProcessing If true, {@link #tickProcessed()} has to be
	 *   called before a further callback will happen. If {@link #tickProcessed()}
	 *   is called after a new callback should already have happened, it will happen
	 *   immediately.
	 */
	public MyCountDownTicks(int numberOfTicks, long countDownInterval, boolean waitForTicksProcessing){

		isUnlimited = numberOfTicks == -1;
		mNumberOfTicks = 0;
		
		mRemainingTicks = numberOfTicks;
		mCountdownInterval = countDownInterval;
		
		mWaitForTicksProcessing = waitForTicksProcessing;
	}

	/**
	 * Cancel the countdown.
	 */
	public final void cancel() {
		mHandler.removeMessages(MSG);
		currentTickProcessed = true;
		isWaitingForTickProcessing = false;
	}

	/**
	 * Start the countdown.
	 */
	public synchronized final MyCountDownTicks start() {
		if (!isUnlimited && mRemainingTicks <= 0) {
			onFinish();
			return this;
		}
		mHandler.sendMessage(mHandler.obtainMessage(MSG));
		return this;
	}
	/**
	 * Fires a tick and resets the boolean that registers if this tick was processed
	 * @param remainingTicks
	 */
	private void fireTick(int remainingTicks){
		currentTickProcessed = false;
		onTick(remainingTicks);
	}
	/**
	 * Callback fired on regular interval.
	 * @param remainingTicks The amount of ticks until finished.
	 */
	public abstract void onTick(int remainingTicks);

	/**
	 * Way to notify the Handler about a successfully processed Tick.
	 * No further Tick will be happening until the last was processed.
	 */
	public void tickProcessed(){
		synchronized (MyCountDownTicks.this){
			currentTickProcessed = true;
			//if the handler was waiting for this tick to be processed, fire event
			if(isWaitingForTickProcessing)
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						mHandler.handleMessage(null);
					}
				});
		}
	}
	/**
	 * Callback fired when the time is up.
	 */
	public abstract void onFinish();


	private static final int MSG = 1;


	// handles counting down
	@SuppressLint("HandlerLeak")
	private final Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			
			synchronized (MyCountDownTicks.this) {
				//if the current (=last) tick has been processed, can fire a new one
				if(!mWaitForTicksProcessing || currentTickProcessed){
					isWaitingForTickProcessing = false;
					//job finished?
					if(mRemainingTicks == 0){
						onFinish();
						return;
					}
					
					if(isUnlimited) {
						mNumberOfTicks++;
						fireTick(mNumberOfTicks);
					} else {
						mRemainingTicks--;
						fireTick(mRemainingTicks);
						//if mustn't wait for tick to finish: fire onFinish() already now
						if(mRemainingTicks == 0 && !mWaitForTicksProcessing){
							onFinish();
							return;
						}
					}
					sendMessageDelayed(obtainMessage(MSG), mCountdownInterval);
					
				} else
					isWaitingForTickProcessing = true;
			}
		}
	};
	
}