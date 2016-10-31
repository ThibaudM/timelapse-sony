package com.thibaudperso.sonycamera.timelapse;

import android.app.Application;

import com.thibaudperso.sonycamera.timelapse.control.io.IOHandler;
import com.thibaudperso.sonycamera.timelapse.control.io.WifiHandler;
import com.thibaudperso.sonycamera.sdk.CameraAPI;
import com.thibaudperso.sonycamera.sdk.model.DeviceManager;

public class TimelapseApplication extends Application {

	private DeviceManager mDeviceManager;
	private IOHandler mIOHandler;
	
	@Override
	public void onCreate() {
		super.onCreate();

		mIOHandler = new IOHandler(this);
		mDeviceManager = new DeviceManager(this);
	}


	public IOHandler getIOHandler() {
		return mIOHandler;
	}

	/*
	 * A shortcut for an access to camera ws
	 */
	public CameraAPI getCameraAPI() {
		return mIOHandler.getCameraAPI();
	}


	public DeviceManager getDeviceManager() {
		return mDeviceManager;
	}

	public WifiHandler getWifiHandler() {
		return mIOHandler.getWifiHandler();
	}
}
