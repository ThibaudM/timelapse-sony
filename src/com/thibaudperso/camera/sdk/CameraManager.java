package com.thibaudperso.camera.sdk;

import org.json.JSONArray;
import org.json.JSONObject;

import com.thibaudperso.camera.core.CameraIO;
import com.thibaudperso.camera.core.CameraIOListener;
import com.thibaudperso.camera.core.TestConnectionListener;

/**
 * 
 * @author Thibaud Michel
 *
 */
public class CameraManager {

	public static int MIN_TIME_BETWEEN_CAPTURE = 7;

	private CameraIO mCameraIO;

	public CameraManager() {

		mCameraIO = new CameraIO();

	}


	public void takePicture(final TakePictureListener listener) {

		mCameraIO.sendRequest("actTakePicture", new JSONArray(), new CameraIOListener() {

			@Override
			public void cameraResponse(JSONArray jsonResponse) {
				String url;
				try {
					url = jsonResponse.getJSONArray(0).getString(0);
					listener.onResult(url);
				} catch (Exception e) {
					listener.onError(e.getMessage());
				}
			}

			@Override
			public void cameraError(JSONObject jsonResponse) {
				listener.onError("Error");
			}
		});


	}

	public void testConnection(int timeout, TestConnectionListener listener) {
		
		mCameraIO.testConnection(timeout, listener);
	
	}

}
