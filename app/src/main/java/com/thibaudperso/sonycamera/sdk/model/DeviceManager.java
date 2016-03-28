package com.thibaudperso.sonycamera.sdk.model;

import android.content.SharedPreferences;
import android.content.res.XmlResourceParser;
import android.preference.PreferenceManager;

import com.thibaudperso.sonycamera.R;
import com.thibaudperso.sonycamera.timelapse.TimelapseApplication;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DeviceManager {

	private static final String ns = null;
	private static final String CAMERA_ID_PREFERENCE = "pref_camera_id";
	
	private TimelapseApplication mApplication;

	private List<Device> mDevicesList;
	private Device mSelectedDevice;

	public DeviceManager(TimelapseApplication application) {
		this.mApplication = application;
		
		mDevicesList = new ArrayList<>();

		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(application);
		int currentCameraId = preferences.getInt(CAMERA_ID_PREFERENCE, -1);
		
		parseCameraModels(application.getResources().getXml(R.xml.devices));
		
		for(Device device : mDevicesList) {
			if(device.getId() == currentCameraId) {
				mSelectedDevice = device;
				mApplication.getCameraIO().setDevice(device);
			}
		}
		
	}


	public List<Device> getDevices() {
		return mDevicesList;	
	}

	public Device getSelectedDevice() {
		return mSelectedDevice;
	}

	public void setSelectedDevice(Device selectedDevice) {
		// Be sure it's not a copy
		for(Device device : mDevicesList) {
			if(device.equals(selectedDevice)) {

				final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mApplication);
				preferences.edit().putInt(CAMERA_ID_PREFERENCE, device.getId()).apply();
				
				mSelectedDevice = device;
				
				mApplication.getCameraIO().setDevice(device);

				return;
			}
		}
	}
	
	
	/*
	 * XML parser
	 */

	private List<Device> parseCameraModels(XmlResourceParser parser) {

		mDevicesList.clear();

		try {

			//parser.require(XmlPullParser.START_TAG, ns, "cameras");
			while (parser.next() != XmlPullParser.END_DOCUMENT) {
				if (parser.getEventType() != XmlPullParser.START_TAG) {
					continue;
				}
				String name = parser.getName();

				if (name.equals("camera")) {
					mDevicesList.add(readCamera(parser));
				}
			}  

		} catch (Exception e) { 
			e.printStackTrace();
		}

		return mDevicesList;
	}

	private Device readCamera(XmlPullParser parser) throws XmlPullParserException, IOException {

		parser.require(XmlPullParser.START_TAG, ns, "camera");
		
		int id = -1;
		String deviceModel = null;
		String webService = null;
		String needInitString = parser.getAttributeValue(ns, "needInit");
		boolean needInit = needInitString != null && "true".equals(needInitString);
		
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = parser.getName();
			switch (name) {
				case "id":
					id = readId(parser);
					break;
				case "model":
					deviceModel = readModel(parser);
					break;
				case "webservice":
					webService = readWebService(parser);
					break;
				default:
					skip(parser);
					break;
			}
		}

		return new Device(id, deviceModel, webService, needInit);
	}

	private int readId(XmlPullParser parser) throws IOException, XmlPullParserException {
		int id = -1;

		parser.require(XmlPullParser.START_TAG, ns, "id");
		try {
			id = Integer.parseInt(readText(parser));
		} catch(Exception ignored) {}
		parser.require(XmlPullParser.END_TAG, ns, "id");

		return id;
	}

	private String readModel(XmlPullParser parser) throws IOException, XmlPullParserException {

		parser.require(XmlPullParser.START_TAG, ns, "model");
		String model = readText(parser);
		parser.require(XmlPullParser.END_TAG, ns, "model");

		return model;
	}

	private String readWebService(XmlPullParser parser) throws IOException, XmlPullParserException {

		parser.require(XmlPullParser.START_TAG, ns, "webservice");
		String webService = readText(parser);
		parser.require(XmlPullParser.END_TAG, ns, "webservice");

		return webService;
	}

	private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
		String result = "";
		if (parser.next() == XmlPullParser.TEXT) {
			result = parser.getText();
			parser.nextTag();
		}
		return result;
	}

	private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
		if (parser.getEventType() != XmlPullParser.START_TAG) {
			throw new IllegalStateException();
		}
		int depth = 1;
		while (depth != 0) {
			switch (parser.next()) {
			case XmlPullParser.END_TAG:
				depth--;
				break;
			case XmlPullParser.START_TAG:
				depth++;
				break;
			}
		}
	}

}
