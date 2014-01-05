package com.thibaudperso.camera;

import com.thibaudperso.camera.io.WifiHandler;
import com.thibaudperso.camera.sdk.CameraManager;

import android.app.Application;

public class TimelapseApplication extends Application {

	private CameraManager mCameraManager;
	private WifiHandler mWifiHandler;
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		mCameraManager = new CameraManager();
		mWifiHandler = new WifiHandler(this);
	}
	
	
	public CameraManager getCameraManager() {
		return mCameraManager;
	}
	
	public WifiHandler getWifiHandler() {
		return mWifiHandler;
	}
	
}
