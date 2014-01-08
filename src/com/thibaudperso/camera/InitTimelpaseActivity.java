package com.thibaudperso.camera;

import java.util.List;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.thibaudperso.camera.core.TestConnectionListener;
import com.thibaudperso.camera.io.NFCHandler;
import com.thibaudperso.camera.io.WifiHandler;
import com.thibaudperso.camera.io.WifiListener;
import com.thibaudperso.camera.model.Device;
import com.thibaudperso.camera.model.DeviceManager;
import com.thibaudperso.camera.sdk.CameraIO;
import com.thibaudperso.timelapse.R;

/**
 * 
 * @author Thibaud Michel
 *
 */
public class InitTimelpaseActivity extends Activity implements WifiListener {

	protected Thread lapseTimeThread;
	private ImageView deviceConnectionRefresh;
	private EditText initialDelay;
	private EditText intervalTime;
	private EditText framesCount;
	private CompoundButton showImageReview;
	private CheckBox framesCountUnlimited;
	private TextView framesCountUnlimitedText;

	private CameraIO mCameraIO;
	private DeviceManager mDeviceManager;
	private NfcAdapter mNfcAdapter;
	private WifiHandler mWifiHandler;

	private AlertDialog alertDialogChooseNetworkConnection;
	private AlertDialog alertDialogChooseNetworkCreation;
	private AlertDialog alertDialogAskForPassword;
	private AlertDialog alertDialogChooseCameraModel;

	/**
	 * Application Life Cycle
	 */

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mCameraIO = ((TimelapseApplication) getApplication()).getCameraIO();
		mDeviceManager = ((TimelapseApplication) getApplication()).getDeviceManager();

		mWifiHandler = ((TimelapseApplication) getApplication()).getWifiHandler();
		mWifiHandler.addListener(this);

		retrieveNfcAdapter();

		setContentView(R.layout.activity_init_timelapse);

		deviceConnectionRefresh = (ImageView) findViewById(R.id.connectionRefresh);
		initialDelay = (EditText) findViewById(R.id.initialDelay);
		intervalTime = (EditText) findViewById(R.id.intervalTime);
		framesCount = (EditText) findViewById(R.id.framesCount);
		framesCountUnlimited = (CheckBox) findViewById(R.id.framesCountUnlimited);
		framesCountUnlimitedText = (TextView) findViewById(R.id.framesCountUnlimitedText);
		showImageReview = (CompoundButton) findViewById(R.id.showImageReview);

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

		framesCountUnlimited.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

				framesCount.setEnabled(!isChecked);

			}
		});

		deviceConnectionRefresh.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mWifiHandler.checkForConnection();
				setConnectionInformation(R.string.connection_info_scan_networks);
			}
		});

		framesCountUnlimitedText.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				framesCountUnlimited.toggle();
				framesCount.setEnabled(!framesCountUnlimited.isChecked());
			}
		});


		if(mDeviceManager.getSelectedDevice() == null) {
			askForDeviceModel();
		}

	}


	@TargetApi(10)
	private void retrieveNfcAdapter() {
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD_MR1) {
			mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.init_timelapse_actions, menu);


		menu.findItem(R.id.start_timelapse).setOnMenuItemClickListener(new OnMenuItemClickListener() {

			@Override
			public boolean onMenuItemClick(MenuItem item) {
				startTimelapse();
				return true;
			}
		});

		menu.findItem(R.id.switch_device).setOnMenuItemClickListener(new OnMenuItemClickListener() {

			@Override
			public boolean onMenuItemClick(MenuItem item) {
				askForDeviceModel();
				return true;
			}
		});

		return super.onCreateOptionsMenu(menu);
	}

	private void startTimelapse() {

		if(!checkAllFormValidity()) {
			return;
		}

		mCameraIO.testConnection(500, new TestConnectionListener() {

			@Override
			public void cameraConnected(boolean isConnected) {

				if(isConnected) {
					Intent timelapseIntent = new Intent(getApplicationContext(), TimelapseActivity.class);
					timelapseIntent.putExtra(TimelapseActivity.EXTRA_INITIAL_DELAY, Integer.parseInt(initialDelay.getText().toString()));
					timelapseIntent.putExtra(TimelapseActivity.EXTRA_INTERVAL_TIME, Integer.parseInt(intervalTime.getText().toString()));
					timelapseIntent.putExtra(TimelapseActivity.EXTRA_FRAMES_COUNT, framesCountUnlimited.isChecked() ? -1 : 
						Integer.parseInt(framesCount.getText().toString()));
					timelapseIntent.putExtra(TimelapseActivity.EXTRA_LAST_IMAGE_REVIEW, showImageReview.isChecked());
					startActivity(timelapseIntent);

					return;
				}
			}
		});

		return;
	}

	@Override
	protected void onStart() {
		super.onStart();
		mWifiHandler.register();

		mWifiHandler.checkForConnection();
		setConnectionInformation(R.string.connection_info_scan_networks);
	}

	@TargetApi(10)
	@Override
	protected void onResume() {
		super.onResume();
		if(mNfcAdapter != null) {
			mNfcAdapter.enableForegroundDispatch(this, NFCHandler.getPendingIntent(this), 
					NFCHandler.getIntentFilterArray(), NFCHandler.getTechListArray());
		}
	}

	@TargetApi(10)
	@Override
	public void onPause() {
		super.onPause();
		if(mNfcAdapter != null) {
			mNfcAdapter.disableForegroundDispatch(this);
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		exit();
	}

	/*
	 * Try to catch NFC intent
	 */
	@Override
	public void onNewIntent(Intent intent) {

		try {
			Pair<String, String> cameraWifiSettings = NFCHandler.parseIntent(intent);
			mWifiHandler.createIfNeededThenConnectToWifi(cameraWifiSettings.first, cameraWifiSettings.second);

		} catch (Exception e) {
			Toast.makeText(this, R.string.connection_nfc_error, Toast.LENGTH_LONG).show();
		} 

	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		if(keyCode == KeyEvent.KEYCODE_BACK) {
			exit();
		}

		return super.onKeyDown(keyCode, event);
	}


	private void exit() {

		mWifiHandler.reconnectToLastWifi();
		mWifiHandler.unregister();

		if(alertDialogChooseNetworkConnection != null) {
			alertDialogChooseNetworkConnection.cancel();
		}
		if(alertDialogChooseNetworkCreation != null) {
			alertDialogChooseNetworkCreation.cancel();
		}
		if(alertDialogAskForPassword != null) {
			alertDialogAskForPassword.cancel();
		}
	}






	/*
	 * Check form validity
	 */

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
			if(value < CameraIO.MIN_TIME_BETWEEN_CAPTURE) {
				intervalTime.setError(getString(R.string.minimum_time_error, CameraIO.MIN_TIME_BETWEEN_CAPTURE));
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

		if(framesCountUnlimited.isChecked()) {
			return true;
		}

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


	/*
	 * Handle network information 
	 */	

	private void setConnectionInformation(int resourceId) {
		TextView tv = (TextView) findViewById(R.id.connectionInfoMessage);
		tv.setText(resourceId);
	}

	private void setConnectionInformation(String message) {
		TextView tv = (TextView) findViewById(R.id.connectionInfoMessage);
		tv.setText(message);
	}

	@Override
	public void onWifiConnecting(String ssid) {
		setConnectionInformation(String.format(getString(R.string.connection_info_wifi_connecting), ssid));
	}

	@Override
	public void onWifiConnected(String ssid) {
		setConnectionInformation(String.format(getString(R.string.connection_info_wifi_connected), ssid));
		checkWSConnection();
	}

	@Override
	public void onWifiDisconnected() {
		setConnectionInformation(R.string.connection_info_wifi_disconnected);	
	}

	@Override
	public void onWifiScanFinished(List<ScanResult> sonyCameraScanResults,
			List<WifiConfiguration> sonyCameraWifiConfiguration) {
		
		/*
		 * No Sony Camera network found in scan 
		 */
		if(sonyCameraScanResults.size() == 0) {
			setConnectionInformation(R.string.connection_info_wifi_not_found);
		}

		/*
		 * No Sony Camera network registered on this phone but we found only one in scan 
		 */
		else if(sonyCameraWifiConfiguration.size() == 0 && sonyCameraScanResults.size() == 1) {
			askForNetworkPasswordThenConnect(sonyCameraScanResults.get(0));
		}

		/*
		 * No Sony Camera network registered on this phone but we found more than one in scan 
		 */
		else if(sonyCameraWifiConfiguration.size() == 0) {
			selectNetworkForCreation(sonyCameraScanResults);
		}

		/*
		 * There is only one Sony Camera known network connected
		 */
		else if(sonyCameraWifiConfiguration.size() == 1) {
			mWifiHandler.connectToNetworkId(sonyCameraWifiConfiguration.get(0).networkId);
		}

		/*
		 * There is more than one Sony Camera known network connected
		 */
		else {
			selectNetworkForConnection(sonyCameraWifiConfiguration);
		}

	}


	private void checkWSConnection() {
		mCameraIO.testConnection(500, new TestConnectionListener() {

			@Override
			public void cameraConnected(final boolean isConnected) {

				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						if(isConnected) {
							setConnectionInformation(R.string.connection_info_ok);	
						} else {
							setConnectionInformation(R.string.connection_info_ws_failed);
						}
					}
				});
			}
		});
	}



	/*
	 * Handle network prompts 
	 */

	private void askForNetworkPasswordThenConnect(final ScanResult scanResult) {

		final EditText input = new EditText(this);

		alertDialogAskForPassword = new AlertDialog.Builder(this)
		.setTitle(String.format(getString(R.string.connection_enter_password), scanResult.SSID))
		.setView(input)
		.setPositiveButton(R.string.connection_enter_password_ok, 
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {

				String value = input.getText().toString(); 
				mWifiHandler.createIfNeededThenConnectToWifi(scanResult.SSID, value);

			}
		})
		.setNegativeButton(R.string.connection_enter_password_cancel, 
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				// Do nothing.
			}
		}).show();

	}

	private void selectNetworkForCreation(final List<ScanResult> scanResults) {

		final ListView listView = new ListView(this);

		ListAdapter adapter = new ArrayAdapter<ScanResult>(this, 
				android.R.layout.simple_list_item_1, scanResults) {

			public View getView(int position, View convertView, ViewGroup parent) {

				View view = super.getView(position, convertView, parent);
				TextView textView = (TextView) view.findViewById(android.R.id.text1);
				textView.setText(((ScanResult) getItem(position)).SSID);
				return textView;
			}
		};

		listView.setAdapter(adapter);
		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, final View view,
					int position, long id) {

				ScanResult scanResult = (ScanResult) parent.getItemAtPosition(position);
				askForNetworkPasswordThenConnect(scanResult);
			}
		});

		alertDialogChooseNetworkCreation = new AlertDialog.Builder(this)
		.setTitle(R.string.connection_choose_network)
		.setView(listView)
		.setNegativeButton(R.string.connection_choose_network_cancel, 
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				// Do nothing.
			}
		}).show();

	}

	private void selectNetworkForConnection(final List<WifiConfiguration> wifiConfigurations) {

		final ListView listView = new ListView(this);

		ListAdapter adapter = new ArrayAdapter<WifiConfiguration>(this, 
				android.R.layout.simple_list_item_1, wifiConfigurations) {

			public View getView(int position, View convertView, ViewGroup parent) {

				View view = super.getView(position, convertView, parent);
				TextView textView = (TextView) view.findViewById(android.R.id.text1);
				textView.setText(((WifiConfiguration) getItem(position)).SSID);
				return textView;
			}
		};

		listView.setAdapter(adapter);
		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, final View view,
					int position, long id) {

				WifiConfiguration wc = (WifiConfiguration) parent.getItemAtPosition(position);
				mWifiHandler.connectToNetworkId(wc.networkId);
			}
		});

		alertDialogChooseNetworkConnection = new AlertDialog.Builder(this)
		.setTitle(R.string.connection_choose_network)
		.setView(listView)
		.setNegativeButton(R.string.connection_choose_network_cancel, 
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				// Do nothing.
			}
		}).show();

	}


	private void askForDeviceModel() {

		boolean cancelable = mDeviceManager.getSelectedDevice() != null;

		final ListView listView = new ListView(this);
		listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

		ArrayAdapter<Device> adapter = new ArrayAdapter<Device>(this, 
				android.R.layout.simple_list_item_single_choice, mDeviceManager.getDevices()){

			public View getView(int position, View convertView, ViewGroup parent) {

				final CheckedTextView view = (CheckedTextView) super.getView(position, convertView, parent);

				if(((Device) getItem(position)).equals(mDeviceManager.getSelectedDevice())) {
					listView.setItemChecked(position, true);
				}

				return view;
			}
		};
		adapter.sort(Device.COMPARE_BY_DEVICEMODEL);

		listView.setAdapter(adapter);

		AlertDialog.Builder alertDialogBuilderChooseCameraModel = 
				new AlertDialog.Builder(this)
		.setTitle(R.string.choose_camera_model)
		.setView(listView)
		.setPositiveButton(R.string.choose_camera_model_ok, 
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {

				int checkedItemPosition = listView.getCheckedItemPosition();
				if(checkedItemPosition < 0) {
					return;
				}
				Device device = (Device) listView.getItemAtPosition(checkedItemPosition);
				mDeviceManager.setSelectedDevice(device);
				checkWSConnection();
			}
		});



		if(cancelable) {
			alertDialogBuilderChooseCameraModel.setNegativeButton(R.string.choose_camera_model_cancel, 
					new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {		
					// Do nothing.
				}
			});
		} else {
			alertDialogBuilderChooseCameraModel.setCancelable(false);
		}

		alertDialogChooseCameraModel = alertDialogBuilderChooseCameraModel.show();
		
		if(!cancelable) {
			alertDialogChooseCameraModel.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
		}

		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				alertDialogChooseCameraModel.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);				
			}
		});

	}
}
