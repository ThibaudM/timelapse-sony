package com.thibaudperso.sonycamera.timelapse;

import android.app.Application;

import com.thibaudperso.sonycamera.io.WifiHandler;
import com.thibaudperso.sonycamera.sdk.CameraIO;
import com.thibaudperso.sonycamera.sdk.model.DeviceManager;

public class TimelapseApplication extends Application {

	private CameraIO mCameraIO;
	private DeviceManager mDeviceManager;
	private WifiHandler mWifiHandler;
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		mCameraIO = new CameraIO(this);
		mDeviceManager = new DeviceManager(this);
		mWifiHandler = new WifiHandler(this);
	}
	
	
	public CameraIO getCameraIO() {
		return mCameraIO;
	}
	
	public WifiHandler getWifiHandler() {
		return mWifiHandler;
	}
	
	public DeviceManager getDeviceManager() {
		return mDeviceManager;
	}
}
