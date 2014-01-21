package com.thibaudperso.sonycamera.sdk;

import org.json.JSONArray;
import org.json.JSONObject;

import com.thibaudperso.sonycamera.sdk.core.CameraWS;
import com.thibaudperso.sonycamera.sdk.core.CameraWSListener;
import com.thibaudperso.sonycamera.sdk.core.TestConnectionListener;
import com.thibaudperso.sonycamera.sdk.model.Device;

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
				
				if(listener == null) {
					return;
				}
				
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
				if(listener == null) {
					return;
				}
				
				listener.onError("Error");
			}
		});


	}

	public void initWebService(final InitWebServiceListener listener) {

		mCameraWS.sendRequest("startRecMode", new JSONArray(), new CameraWSListener() {

			@Override
			public void cameraResponse(JSONArray jsonResponse) {
				if(listener == null) {
					return;
				}

				listener.onResult();
			}

			@Override
			public void cameraError(JSONObject jsonResponse) {
				if(listener == null) {
					return;
				}
				
				listener.onError("Error");
			}
		});

	}

	public void getVersion(final GetVersionListener listener) {

		mCameraWS.sendRequest("getVersions", new JSONArray(), new CameraWSListener() {

			@Override
			public void cameraResponse(JSONArray jsonResponse) {
				if(listener == null) {
					return;
				}

				int version;
				try {
					version = jsonResponse.getJSONArray(0).getInt(0);
					listener.onResult(version);
				} catch (Exception e) {
					listener.onError(e.getMessage());
				}
			}

			@Override
			public void cameraError(JSONObject jsonResponse) {
				if(listener == null) {
					return;
				}
				
				listener.onError("Error");
			}
		});

	}

	public void testConnection(int timeout, final TestConnectionListener listener) {

		// Not enough
		// mCameraWS.testConnection(timeout, listener);

		mCameraWS.sendRequest("getVersions", new JSONArray(), new CameraWSListener() {
			
			@Override
			public void cameraResponse(JSONArray jsonResponse) {
				listener.cameraConnected(true);				
			}
			
			@Override
			public void cameraError(JSONObject jsonResponse) {
				listener.cameraConnected(false);				
			}
		}, timeout);
		
	}

}
