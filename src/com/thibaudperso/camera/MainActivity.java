package com.thibaudperso.camera;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.thibaudperso.camera.core.TestConnectionListener;
import com.thibaudperso.camera.sdk.CameraManager;
import com.thibaudperso.timelapse.R;

/**
 * 
 * @author Thibaud Michel
 *
 */
public class MainActivity extends Activity {

	protected Thread lapseTimeThread;
	private ImageView deviceConnectionRefresh;
	private EditText initialDelay;
	private EditText intervalTime;
	private EditText framesCount;
	private CheckBox showImageReview;
	private MenuItem startMenuItem;

	private CameraManager mCameraManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mCameraManager = ((TimelapseApplication) getApplication()).getCameraManager();
		
		setContentView(R.layout.activity_main);
		
		deviceConnectionRefresh = (ImageView) findViewById(R.id.deviceConnectionRefresh);
		initialDelay = (EditText) findViewById(R.id.initialDelay);
		intervalTime = (EditText) findViewById(R.id.interval);
		framesCount = (EditText) findViewById(R.id.framesCount);
		showImageReview = (CheckBox) findViewById(R.id.showImageReview);
		
		initialDelay.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			@Override
			public void afterTextChanged(Editable s) {
				checkInitialDelay();
			}
		});
		
		intervalTime.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			@Override
			public void afterTextChanged(Editable s) {
				checkIntervalTime();
			}
		});
		
		framesCount.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			@Override
			public void afterTextChanged(Editable s) {
				checkFramesCount();
			}
		});		
		
		deviceConnectionRefresh.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				checkConnection();
			}
		});
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main_actions, menu);
	    
	    startMenuItem = menu.findItem(R.id.start_timelapse);
	    
	    startMenuItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				startTimelapse();
				return true;
			}
		});
	    
	    return super.onCreateOptionsMenu(menu);
	}
	
	private void startTimelapse() {
		
		if(!checkAllFormValidity()) {
			return;
		}
		
		mCameraManager.testConnection(500, new TestConnectionListener() {
			
			@Override
			public void cameraConnected(boolean isConnected) {

				if(isConnected) {
					Intent timelapseIntent = new Intent(getApplicationContext(), TimelapseActivity.class);
					timelapseIntent.putExtra(TimelapseActivity.EXTRA_INITIAL_DELAY, Integer.parseInt(initialDelay.getText().toString()));
					timelapseIntent.putExtra(TimelapseActivity.EXTRA_INTERVAL_TIME, Integer.parseInt(intervalTime.getText().toString()));
					timelapseIntent.putExtra(TimelapseActivity.EXTRA_FRAMES_COUNT, Integer.parseInt(framesCount.getText().toString()));
					timelapseIntent.putExtra(TimelapseActivity.EXTRA_LAST_IMAGE_REVIEW, showImageReview.isChecked());
					startActivity(timelapseIntent);
					
					return;
				}
				
				runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						Toast.makeText(getApplicationContext(), R.string.device_disconnected, Toast.LENGTH_SHORT).show();	
					}
				});
				
			}
		});
		
		return;
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		checkConnection();
		
	}

	
	private void checkConnection() {
		mCameraManager.testConnection(500, new TestConnectionListener() {
			
			@Override
			public void cameraConnected(final boolean isConnected) {
				
				runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						TextView deviceConnectedMessage = (TextView) findViewById(R.id.deviceConnectedMessage);
						deviceConnectedMessage.setText(isConnected ? R.string.device_connected : R.string.device_disconnected);						
					}
				});
			}
		});
	}
	
	private boolean checkInitialDelay() {
		try {
			if(Integer.valueOf(initialDelay.getText().toString()) < 0) {
				initialDelay.setError(getString(R.string.form_positive_integer_error));
				return false;
			}
		} catch(NumberFormatException e) {
			initialDelay.setError(getString(R.string.form_positive_integer_error));
			return false;
		}
		
		initialDelay.setError(null);
		return true;
	}
	
	
	private boolean checkIntervalTime() {
		try {
			int value = Integer.valueOf(intervalTime.getText().toString());
			if(value <= 0) {
				intervalTime.setError(getString(R.string.form_positive_integer_error));
				return false;
			}
			if(value < CameraManager.MIN_TIME_BETWEEN_CAPTURE) {
				intervalTime.setError(getString(R.string.minimum_time_error, CameraManager.MIN_TIME_BETWEEN_CAPTURE));
				return false;
			}
		} catch(NumberFormatException e) {
			intervalTime.setError(getString(R.string.form_positive_integer_error));
			return false;
		}
		
		intervalTime.setError(null);
		return true;
	}
	
	private boolean checkFramesCount() {
		try {
			if(Integer.valueOf(framesCount.getText().toString()) <= 0) {
				framesCount.setError(getString(R.string.form_positive_integer_error));
				return false;
			}
		} catch(NumberFormatException e) {
			framesCount.setError(getString(R.string.form_positive_integer_error));
			return false;
		}
		
		framesCount.setError(null);
		return true;
	}

	private boolean checkAllFormValidity() {

		return checkInitialDelay() && checkFramesCount() && checkIntervalTime();
		
	}

}
