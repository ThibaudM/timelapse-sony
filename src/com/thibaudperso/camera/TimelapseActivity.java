package com.thibaudperso.camera;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.thibaudperso.camera.sdk.CameraManager;
import com.thibaudperso.camera.sdk.TakePictureListener;
import com.thibaudperso.timelapse.R;

/**
 * 
 * @author Thibaud Michel
 *
 */
public class TimelapseActivity extends Activity {

	public static String EXTRA_INITIAL_DELAY = "initial-delay";
	public static String EXTRA_INTERVAL_TIME = "interval-time";
	public static String EXTRA_FRAMES_COUNT = "frames-count";
	public static String EXTRA_LAST_IMAGE_REVIEW = "last-image-review";

	public static int INITIAL_DELAY_DEFAULT = 0; 
	public static int INTERVAL_TIME_DEFAULT = 10; 
	public static int INTERVAL_FRAMES_COUNT = 100; 
	public static boolean INTERVAL_LAST_IMAGE_REVIEW = true;

	private final static String TIME_FORMAT = "hh:mm";

	private CameraManager mCameraManager;
	
	private TextView framesCountDownValue;
	private ProgressBar progressBar;
	private TextView progressValue;
	
	private MyCountDownTicks mCountDownPictures;
	private MyCountDownTicks mInitialCountDown;
	
	private boolean showLastFramePreview;
	private int timeLapseDuration;
	private int initialDelay;
	private int intervalTime;
	private int framesCount;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mCameraManager = ((TimelapseApplication) getApplication()).getCameraManager();
		
		setContentView(R.layout.activity_timelapse);

		Intent currentIntent = getIntent();
		if(currentIntent == null) {
			finish();
			return;
		}
		
		// Retrieve and calculate data
		initialDelay = currentIntent.getIntExtra(EXTRA_INITIAL_DELAY, INITIAL_DELAY_DEFAULT);
		intervalTime = currentIntent.getIntExtra(EXTRA_INTERVAL_TIME, INTERVAL_TIME_DEFAULT);
		framesCount = currentIntent.getIntExtra(EXTRA_FRAMES_COUNT, INTERVAL_FRAMES_COUNT);
		timeLapseDuration = intervalTime * (framesCount - 1);
		showLastFramePreview = currentIntent.getBooleanExtra(EXTRA_LAST_IMAGE_REVIEW, INTERVAL_LAST_IMAGE_REVIEW);

		/*
		 * Set activity fields
		 */
		Calendar beginEndCalendar = Calendar.getInstance();
		beginEndCalendar.add(Calendar.SECOND, initialDelay);
		String beginTime = new SimpleDateFormat(TIME_FORMAT, Locale.getDefault()).format(beginEndCalendar.getTime());
		((TextView) findViewById(R.id.beginValue)).setText(beginTime);
		
		((TextView) findViewById(R.id.durationValue)).setText(String.valueOf(timeLapseDuration)+"s");
		
		beginEndCalendar.add(Calendar.SECOND, timeLapseDuration);
		String endTime = new SimpleDateFormat(TIME_FORMAT, Locale.getDefault()).format(beginEndCalendar.getTime());
		((TextView) findViewById(R.id.endValue)).setText(endTime);

		progressBar = (ProgressBar) findViewById(R.id.progressBar);
		progressBar.setProgress(0);
		progressBar.setMax(framesCount);
		
		progressValue = (TextView) findViewById(R.id.progressValue);
		progressValue.setText(getString(R.string.progress_default));

		framesCountDownValue = (TextView) findViewById(R.id.framesCountDownValue);
		framesCountDownValue.setText(String.valueOf(framesCount));

		final TextView timelapseCountdownBeforeStart = (TextView) findViewById(R.id.timelapseCountdownBeforeStartText);

		this.registerReceiver(this.myBatteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		
		/*
		 * Show start in message 
		 */
		if(initialDelay != 0) {
			timelapseCountdownBeforeStart.setVisibility(View.VISIBLE);
			Animation hideAnimation = AnimationUtils.loadAnimation(this, R.anim.message_from_bottom_show);
			timelapseCountdownBeforeStart.setAnimation(hideAnimation);
		}

		mInitialCountDown = new MyCountDownTicks(initialDelay, 1000) {

			public void onTick(int remainingTicks) {
				timelapseCountdownBeforeStart.setText(String.format(getString(R.string.timelapse_countdown_before_start_message), 
						remainingTicks));
			}

			public void onFinish() {

				/*
				 * Remove start in message
				 */
				if(initialDelay != 0) {
					timelapseCountdownBeforeStart.setVisibility(View.GONE);
					Animation hideAnimation = AnimationUtils.loadAnimation(TimelapseActivity.this, R.anim.message_from_bottom_hide);
					timelapseCountdownBeforeStart.setAnimation(hideAnimation);
				}
				
				// Start timelapse
				startTimeLapse();
			}

		}.start();
	}

	/**
	 * Handler for battery state
	 */
	private BroadcastReceiver myBatteryReceiver = new BroadcastReceiver(){

		@Override
		public void onReceive(Context arg0, Intent arg1) {
			int bLevel = arg1.getIntExtra("level", 0);
			
			((TextView) findViewById(R.id.batteryValue)).setText(String.valueOf(bLevel)+"%");
		} 
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.timelapse_actions, menu);
		
	    MenuItem endMenuItem = menu.findItem(R.id.stop_timelapse);

	    endMenuItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				finish();
				return true;
			}
		});
		
		return super.onCreateOptionsMenu(menu);
	}


	private void startTimeLapse() {

		mCountDownPictures = new MyCountDownTicks(framesCount, intervalTime*1000) {

			public void onTick(int remainingFrames) {

				/*
				 * Update activity fields
				 */
				framesCountDownValue.setText(String.valueOf(remainingFrames));

				int progress = framesCount - remainingFrames;
				float progressPercent = (float) progress / framesCount * 100;
				progressBar.setProgress(progress);
				progressValue.setText(new DecimalFormat("#.##").format(progressPercent) + "%" );

				takePicture();
			}

			public void onFinish() {

				Toast.makeText(getApplicationContext(), R.string.timelapse_finished, Toast.LENGTH_LONG).show();

			}
		}.start();

	}

	private void takePicture() {

		mCameraManager.takePicture(new TakePictureListener() {

			@Override
			public void onResult(String url) {

				if(!showLastFramePreview) {
					return;
				}

				Bitmap preview = Utils.downloadBitmap(url);				
				setPreviewImage(preview);					
			}

			@Override
			public void onError(String error) {
				// Do nothing
			}
		});
	}


	private void setPreviewImage(final Bitmap preview) {

		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				((ImageView) findViewById(R.id.lastFramePreview)).setImageBitmap(preview);						
			}
		});

	}

	@Override
	protected void onStop() {
		super.onStop();

		if(mInitialCountDown != null) {
			mInitialCountDown.cancel();
		}
		
		if(mCountDownPictures != null) {
			mCountDownPictures.cancel();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		this.unregisterReceiver(myBatteryReceiver);

	}

}
