package com.thibaudperso.sonycamera.timelapse;

import android.app.Application;

import com.thibaudperso.sonycamera.sdk.CameraAPI;
import com.thibaudperso.sonycamera.sdk.model.Device;
import com.thibaudperso.sonycamera.timelapse.control.DeviceManager;
import com.thibaudperso.sonycamera.timelapse.control.connection.StateMachineConnection;
import com.thibaudperso.sonycamera.timelapse.control.connection.WifiHandler;
import com.thibaudperso.sonycamera.timelapse.model.TimelapseData;

public class TimelapseApplication extends Application {

	private DeviceManager mDeviceManager;
	private TimelapseData mTimelapseData;
	private StateMachineConnection mStateMachineConnection;
	private CameraAPI mCameraAPI;
	private WifiHandler mWifiHandler;

	@Override
	public void onCreate() {
		super.onCreate();

		mCameraAPI = new CameraAPI(this);
		mWifiHandler = new WifiHandler(this);
		mDeviceManager = new DeviceManager(this);
		mTimelapseData = new TimelapseData();
		mStateMachineConnection = new StateMachineConnection(this);

		synchronized (this) {
			mCameraAPI.setDevice(mDeviceManager.getSelectedDevice());
			mDeviceManager.addDeviceChangedListener(new DeviceManager.DeviceChangedListener() {
				@Override
				public void onNewDevice(Device device) {
					mCameraAPI.setDevice(device);
				}
			});
		}
	}


	/*
	 * A shortcut for an access to camera ws
	 */
	public CameraAPI getCameraAPI() {
		return mCameraAPI;
	}


	public DeviceManager getDeviceManager() {
		return mDeviceManager;
	}

	public WifiHandler getWifiHandler() {
		return mWifiHandler;
	}

	public TimelapseData getTimelapseData() {
		return mTimelapseData;
	}

	public StateMachineConnection getStateMachineConnection() {
		return mStateMachineConnection;
	}
}
