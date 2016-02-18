package com.thibaudperso.sonycamera.timelapse;

import android.app.Application;

import com.thibaudperso.sonycamera.io.WifiHandler;
import com.thibaudperso.sonycamera.sdk.CameraAPI;
import com.thibaudperso.sonycamera.sdk.model.DeviceManager;

public class TimelapseApplication extends Application {

	private CameraAPI mCameraAPI;
	private DeviceManager mDeviceManager;
	private WifiHandler mWifiHandler;
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		mCameraAPI = new CameraAPI(this);
		mDeviceManager = new DeviceManager(this);
		mWifiHandler = new WifiHandler(this);
	}
	
	
	public CameraAPI getCameraAPI() {
		return mCameraAPI;
	}
	
	public WifiHandler getWifiHandler() {
		return mWifiHandler;
	}
	
	public DeviceManager getDeviceManager() {
		return mDeviceManager;
	}
}
