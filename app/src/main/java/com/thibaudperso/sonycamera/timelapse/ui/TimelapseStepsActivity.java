package com.thibaudperso.sonycamera.timelapse.ui;

import android.annotation.TargetApi;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.thibaudperso.sonycamera.R;
import com.thibaudperso.sonycamera.io.NFCHandler;
import com.thibaudperso.sonycamera.io.WifiHandler;
import com.thibaudperso.sonycamera.io.WifiListener;
import com.thibaudperso.sonycamera.sdk.CameraIO;
import com.thibaudperso.sonycamera.timelapse.StepCompletedListener;
import com.thibaudperso.sonycamera.timelapse.StepFragment;
import com.thibaudperso.sonycamera.timelapse.TimelapseApplication;
import com.thibaudperso.sonycamera.timelapse.fragments.CameraSettingsFragment;
import com.thibaudperso.sonycamera.timelapse.fragments.CaptureFragment;
import com.thibaudperso.sonycamera.timelapse.fragments.CaptureFragmentListener;
import com.thibaudperso.sonycamera.timelapse.fragments.ConnectionFragment;
import com.thibaudperso.sonycamera.timelapse.fragments.FinishFragment;
import com.thibaudperso.sonycamera.timelapse.fragments.FinishFragmentListener;
import com.thibaudperso.sonycamera.timelapse.fragments.TimelapseSettingsFragment;

import java.util.List;


public class TimelapseStepsActivity extends FragmentActivity implements StepCompletedListener, WifiListener,
FinishFragmentListener, CaptureFragmentListener {


	/**
	 * The pager widget, which handles animation and allows swiping horizontally to access previous
	 * and next wizard steps.
	 */
	private ViewPager mPager;

	/**
	 * The pager adapter, which provides the pages to the view pager widget.
	 */
	private ScreenSlidePagerAdapter mPagerAdapter;

	private FinishFragment mFinishFragment = new FinishFragment();
	private CaptureFragment mCaptureFragment = new CaptureFragment();

	private StepFragment[] fragmentsArray = new StepFragment[] { new ConnectionFragment(), new CameraSettingsFragment(),
			new TimelapseSettingsFragment(), mCaptureFragment, mFinishFragment };

	private NfcAdapter mNfcAdapter;
	private WifiHandler mWifiHandler;

	private int lastPositionSelected = -1;
	private boolean isStepCompleted = false;

	private TextView informationTextView;
	private ImageView informationImage;
	
	private boolean doubleBackToExitPressedOnce = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mFinishFragment.setListener(this);
		mCaptureFragment.setListener(this);

		mWifiHandler = ((TimelapseApplication) getApplication()).getWifiHandler();
		mWifiHandler.addListener(this);

		setContentView(R.layout.activity_timelapse_steps);

		informationTextView = (TextView) findViewById(R.id.informationText);
		informationImage = (ImageView) findViewById(R.id.informationImage);

		retrieveNfcAdapter();

		// Instantiate a ViewPager and a PagerAdapter.
		mPager = (ViewPager) findViewById(R.id.pager);
		mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
		mPager.setAdapter(mPagerAdapter);

		mPager.post(new Runnable() {

			@Override
			public void run() {
				pageSelected(0);
			}
		});

		mPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {

			@Override
			public void onPageSelected(int position) {
				pageSelected(position);
			}
		});

		informationImage.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				informationTextView.setVisibility(informationTextView.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
			}
		});

		// Remove information when you click on the screen
		findViewById(android.R.id.content).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				informationTextView.setVisibility(View.GONE);
			}
		});
	}

	@TargetApi(10)
	private void retrieveNfcAdapter() {
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD_MR1) {
			mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		mWifiHandler.register();
	}

	@Override
	protected void onResume() {
		super.onResume();

		if(mNfcAdapter != null) {
			mNfcAdapter.enableForegroundDispatch(this, NFCHandler.getPendingIntent(this),
					NFCHandler.getIntentFilterArray(), NFCHandler.getTechListArray());
		}
	}

	@Override
	protected void onPause() {
		super.onPause();

		if(mNfcAdapter != null) {
			mNfcAdapter.disableForegroundDispatch(this);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		exit();
	}

	private void exit() {

		if(lastPositionSelected != -1) {
			fragmentsArray[lastPositionSelected].onExitFragment();
		}

		//disconnect from camera's wifi network
		if(mWifiHandler != null) {
			mWifiHandler.disconnect();
			mWifiHandler.removeListener(this);
		}

	}

	/*
	 * Try to catch NFC intent
	 */
	@Override
	public void onNewIntent(Intent intent) {

		try {
			Pair<String, String> cameraWifiSettings = NFCHandler.parseIntent(intent);
			if (cameraWifiSettings != null) {
				mWifiHandler.createIfNeededThenConnectToWifi(cameraWifiSettings.first, cameraWifiSettings.second);
			}

			mPager.setCurrentItem(0);

		} catch (Exception e) {
			Toast.makeText(this, R.string.connection_nfc_error, Toast.LENGTH_LONG).show();
		}

	}

	private void pageSelected(int position) {

		if(lastPositionSelected != -1) {
			fragmentsArray[lastPositionSelected].setStepCompletedListener(null);
			fragmentsArray[lastPositionSelected].onExitFragment();
		}

		fragmentsArray[position].onEnterFragment();
		fragmentsArray[position].setStepCompletedListener(this);
		setStepCompleted(fragmentsArray[position].isStepCompleted());

		setGuideBar(position);
		setInformation(fragmentsArray[position]);

		invalidateOptionsMenu();

		lastPositionSelected = position;
	}

	private void setInformation(StepFragment fragment) {

		informationImage.setVisibility(fragment.getInformation() == null ? View.GONE : View.VISIBLE);
		informationTextView.setVisibility(View.GONE);
		informationTextView.setText(fragment.getInformation());

	}

	private void setGuideBar(int fragmentPosition) {

		(findViewById(R.id.guideStep1)).setBackgroundResource(fragmentPosition == 0 ? R.drawable.blue_bullet : R.drawable.black_bullet);
		(findViewById(R.id.guideStep2)).setBackgroundResource(fragmentPosition == 1 ? R.drawable.blue_bullet : R.drawable.black_bullet);
		(findViewById(R.id.guideStep3)).setBackgroundResource(fragmentPosition == 2 ? R.drawable.blue_bullet : R.drawable.black_bullet);
		(findViewById(R.id.guideStep4)).setBackgroundResource(fragmentPosition == 3 ? R.drawable.blue_bullet : R.drawable.black_bullet);
		(findViewById(R.id.guideStep5)).setBackgroundResource(fragmentPosition == 4 ? R.drawable.blue_bullet : R.drawable.black_bullet);


		((TextView) findViewById(R.id.guideTitle)).setText(
				fragmentPosition == 0 ? R.string.guidebar_title_connection :
					fragmentPosition == 1 ? R.string.guidebar_title_camera_settings :
						fragmentPosition == 2 ? R.string.guidebar_title_timelapse_settings :
							fragmentPosition == 3 ? R.string.guidebar_title_capture :
								R.string.guidebar_title_finish);

	}




	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.activity_screen_slide, menu);

		MenuItem previousItem = menu.findItem(R.id.action_previous);

		previousItem.setEnabled(mPager.getCurrentItem() > 0 &&
				mPager.getCurrentItem() != mPagerAdapter.getCount() - 1);

		// Add either a "next" or "finish" button to the action bar, depending on which page
		// is currently selected.

		int resNextTitle;
		if(mPager.getCurrentItem() == mPagerAdapter.getCount() - 1) {
			resNextTitle = R.string.action_finish;
		} else if(mPager.getCurrentItem() == 2) {
			resNextTitle = R.string.action_start;
		} else if(mPager.getCurrentItem() == 3) {
			resNextTitle = R.string.action_stop;
		} else {
			resNextTitle = R.string.action_next;
		}

		MenuItem nextItem = menu.findItem(R.id.action_next);
		nextItem.setTitle(resNextTitle);
		nextItem.setEnabled(isStepCompleted);
		nextItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

		MenuItem keepDisplayAwakeItem = menu.findItem(R.id.action_keep_display_on);
		//only visible and enabled on the CaptureFragment page
		boolean isCapturePage = mPager.getCurrentItem() == 3;
		keepDisplayAwakeItem.setVisible(isCapturePage);
		keepDisplayAwakeItem.setEnabled(isCapturePage);
		keepDisplayAwakeItem.setChecked(mCaptureFragment.isKeepDisplayOn());
		return true;
	}


	private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {


		public ScreenSlidePagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			return fragmentsArray[position];
		}

		@Override
		public int getCount() {
			return fragmentsArray.length;
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_previous:
			mPager.setCurrentItem(mPager.getCurrentItem() - 1);
			return true;

		case R.id.action_next:
			if(mPager.getCurrentItem() == mPagerAdapter.getCount() - 1) {
				//tell the camera we're done
				CameraIO mCameraIO = ((TimelapseApplication) this.getApplication()).getCameraIO();
				if(mCameraIO != null)
					mCameraIO.closeConnection();
				//continue with closing procedure, including wifi disconnection
				finish();
				return true;
			}
			mPager.setCurrentItem(mPager.getCurrentItem() + 1);
			return true;

		case R.id.action_keep_display_on:
			//toggle state, doesn't happen automatically!
			item.setChecked(!item.isChecked());
			mCaptureFragment.setKeepDisplayOn(item.isChecked());
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onBackPressed() {
		//exit only on second press
		if(doubleBackToExitPressedOnce){
			if (mPager.getCurrentItem() == 0) {
				// If the user is currently looking at the first step, allow the system to handle the
				// Back button. This calls finish() on this activity and pops the back stack.
				super.onBackPressed();
				exit();
			} else {
				// Otherwise, select the previous step.
				int newItem = mPager.getCurrentItem() - 1;
				//CaptureFragment can't be reached by BackPressed, jump to CaptureSettings instead 
				if(newItem == 3)
					newItem--;
				mPager.setCurrentItem(newItem);
			}
		} else {
			//first press is okay
			this.doubleBackToExitPressedOnce = true;
			Toast.makeText(this, R.string.connection_close_app, Toast.LENGTH_SHORT).show();
			//reset first press if 2 seconds have passed without further press
		    new android.os.Handler().postDelayed(new Runnable() {
		        @Override
		        public void run() {
		            doubleBackToExitPressedOnce=false;                       
		        }
		    }, 2000);
		}
	}

	@Override
	public void stepCompleted(boolean isCompleted) {
		setStepCompleted(isCompleted);
		invalidateOptionsMenu();
	}

	private void setStepCompleted(boolean isCompleted) {
		this.isStepCompleted = isCompleted;
	}

	@Override
	public void onWifiConnecting(String ssid) { }

	@Override
	public void onWifiConnected(String ssid) { }

	@Override
	public void onWifiDisconnected() {
		if(mPager != null) {
			mPager.setCurrentItem(0);
		}
	}

	@Override
	public void onWifiStartScan() { }

	@Override
	public void onWifiScanFinished(List<ScanResult> sonyCameraScanResults,
			List<WifiConfiguration> sonyCameraWifiConfiguration) { }



	@Override
	public void onRestartProcess() {
		if(mPager != null) {
			mPager.setCurrentItem(1);
		}
	}

	@Override
	public void onCaptureFinished() {
		if(mPager != null) {
			mPager.setCurrentItem(5);
		}
	}

}
