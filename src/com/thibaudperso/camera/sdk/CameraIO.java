package com.thibaudperso.camera.sdk;

import org.json.JSONArray;
import org.json.JSONObject;

import com.thibaudperso.camera.core.CameraWS;
import com.thibaudperso.camera.core.CameraWSListener;
import com.thibaudperso.camera.core.TestConnectionListener;
import com.thibaudperso.camera.model.Device;

/**
 * 
 * @author Thibaud Michel
 *
 */
public class CameraIO {

	public static int MIN_TIME_BETWEEN_CAPTURE = 5;

	private CameraWS mCameraWS;

	public CameraIO() {

		mCameraWS = new CameraWS();

	}

	public void setDevice(Device device) {
		mCameraWS.setWSUrl(device.getWebService());
	}

	public void takePicture(final TakePictureListener listener) {

		mCameraWS.sendRequest("actTakePicture", new JSONArray(), new CameraWSListener() {

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
		
		mCameraWS.testConnection(timeout, listener);
	
	}

}
