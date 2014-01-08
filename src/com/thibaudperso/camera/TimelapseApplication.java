package com.thibaudperso.camera;

import com.thibaudperso.camera.io.WifiHandler;
import com.thibaudperso.camera.model.DeviceManager;
import com.thibaudperso.camera.sdk.CameraIO;

import android.app.Application;

public class TimelapseApplication extends Application {

	private CameraIO mCameraIO;
	private DeviceManager mDeviceManager;
	private WifiHandler mWifiHandler;
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		mCameraIO = new CameraIO();
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
