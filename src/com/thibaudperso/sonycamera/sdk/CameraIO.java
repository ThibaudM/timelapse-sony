package com.thibaudperso.sonycamera.sdk;

import org.json.JSONArray;
import org.json.JSONObject;

import android.util.Log;

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

	public static enum ZoomDirection { IN, OUT };
	public static enum ZoomAction { START, STOP };

	public static int MIN_TIME_BETWEEN_CAPTURE = 1;

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

	public void startLiveView(final StartLiveviewListener listener) {

		mCameraWS.sendRequest("startLiveview", new JSONArray(), new CameraWSListener() {

			@Override
			public void cameraResponse(JSONArray jsonResponse) {
				if(listener == null) {
					return;
				}

				try {
					listener.onResult(jsonResponse.getString(0));
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

	public void stopLiveView() {

		mCameraWS.sendRequest("stopLiveview", new JSONArray(), null);
	}


	public void actZoom(final ZoomDirection zoomDir) {

		JSONArray params = new JSONArray().put(zoomDir == ZoomDirection.IN ? "in" : "out").put("1shot");
		mCameraWS.sendRequest("actZoom", params, null);
	}

	public void actZoom(final ZoomDirection zoomDir, final ZoomAction zoomAct) {

		JSONArray params = new JSONArray().put(zoomDir == ZoomDirection.IN ? "in" : "out").
				put(zoomAct == ZoomAction.START ? "start" : "stop");
		mCameraWS.sendRequest("actZoom", params, null);
	}

	
	public void setFlash(final boolean enableFlash) {

		JSONArray params = new JSONArray().put(enableFlash ? "true" : "false");
		mCameraWS.sendRequest("setFlashMode", params, new CameraWSListener() {
			
			@Override
			public void cameraResponse(JSONArray jsonResponse) {
				Log.v("DEBUG", "ok: "+jsonResponse);
			}
			
			@Override
			public void cameraError(JSONObject jsonResponse) {
				Log.v("DEBUG", "err: "+jsonResponse);				
			}
		});
	}
}
