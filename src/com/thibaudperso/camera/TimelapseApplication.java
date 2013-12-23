package com.thibaudperso.camera;

import com.thibaudperso.camera.sdk.CameraManager;

import android.app.Application;

public class TimelapseApplication extends Application {

	private CameraManager mCameraManager;
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		mCameraManager = new CameraManager();
	}
	
	
	public CameraManager getCameraManager() {
		return mCameraManager;
	}
	
}
