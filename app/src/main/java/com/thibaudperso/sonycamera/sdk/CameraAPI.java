package com.thibaudperso.sonycamera.sdk;

import android.content.Context;

import com.thibaudperso.sonycamera.sdk.model.Device;
import com.thibaudperso.sonycamera.sdk.model.PictureResponse;

import org.json.JSONArray;

/**
 * @author Thibaud Michel
 */
public class CameraAPI {

    public enum ZoomDirection {IN, OUT}

    public enum ZoomAction {START, STOP}

    private CameraWS mCameraWS;


    private boolean mIsDeviceInitialized = false;

    public CameraAPI(Context context) {

        mCameraWS = new CameraWS(context);

    }

    public void setDevice(Device device) {
        mCameraWS.setWSUrl(device.getWebService());
    }

    public void initializeWS() {
        if (mIsDeviceInitialized) {
            return;
        }

        initWebService(new InitWebServiceListener() {
            @Override
            public void onResult(CameraWS.ResponseCode responseCode) {
                if (responseCode != CameraWS.ResponseCode.OK) {
                    return;
                }
                setShootMode("still");
                mIsDeviceInitialized = true;
            }
        });
    }

    /**
     * Sets the shoot mode, "still" or "movie". This needs to be set to "still"
     * on some camcorders, because they default to video.
     *
     * @param mode either "still" or "movie".
     */
    public void setShootMode(String mode) {
        JSONArray params = new JSONArray().put(mode);
        mCameraWS.sendRequest("setShootMode", params, null);
    }

    public void takePicture(final TakePictureListener listener) {
        mCameraWS.sendRequest("actTakePicture", new JSONArray(), getTakePictureListener(listener));
    }

    public void awaitTakePicture(final TakePictureListener listener) {
        mCameraWS.sendRequest("awaitTakePicture", new JSONArray(), getTakePictureListener(listener));
    }

    private CameraWS.Listener getTakePictureListener(final TakePictureListener listener) {
        return new CameraWS.Listener() {

            @Override
            public void cameraResponse(CameraWS.ResponseCode responseCode, Object response) {

                if (listener == null) return;

                PictureResponse output = new PictureResponse();
                output.time = System.currentTimeMillis();
                output.status = responseCode;

                if (responseCode == CameraWS.ResponseCode.OK) {

                    try {
                        output.status = CameraWS.ResponseCode.OK;
                        output.url = ((JSONArray) response).getJSONArray(0).getString(0);

                    } catch (Exception e) {
                        output.status = CameraWS.ResponseCode.RESPONSE_NOT_WELL_FORMATED;
                    }
                }

                listener.onResult(output);
            }
        };
    }

    private void initWebService(final InitWebServiceListener listener) {

        mCameraWS.sendRequest("startRecMode", new JSONArray(), new CameraWS.Listener() {
            @Override
            public void cameraResponse(CameraWS.ResponseCode responseCode, Object response) {

                if (listener == null) return;

                listener.onResult(responseCode);

            }
        });

    }

    public void getVersion(final VersionListener listener) {

        mCameraWS.sendRequest("getVersions", new JSONArray(), new CameraWS.Listener() {

            @Override
            public void cameraResponse(CameraWS.ResponseCode responseCode, Object response) {

                if (listener == null) return;

                int version = 0;
                CameraWS.ResponseCode status = responseCode;

                if (responseCode == CameraWS.ResponseCode.OK) {

                    try {
                        version = ((JSONArray) response).getJSONArray(0).getInt(0);
                    } catch (Exception e) {
                        status = CameraWS.ResponseCode.RESPONSE_NOT_WELL_FORMATED;
                    }
                }

                listener.onResult(status, version);
            }

        });

    }

    public void testConnection(final TestConnectionListener listener) {

        this.getVersion(new VersionListener() {
            @Override
            public void onResult(CameraWS.ResponseCode responseCode, int version) {

                if (listener == null) return;

                listener.isConnected(responseCode == CameraWS.ResponseCode.OK);
            }
        });

    }

    public void closeConnection() {
        mCameraWS.sendRequest("stopRecMode", new JSONArray(), null, 0);
    }


    public void startLiveView(final StartLiveviewListener listener) {

        mCameraWS.sendRequest("startLiveview", new JSONArray(), new CameraWS.Listener() {

            @Override
            public void cameraResponse(CameraWS.ResponseCode responseCode, Object response) {

                if (listener == null) return;

                String url = null;
                CameraWS.ResponseCode status = responseCode;

                if (responseCode == CameraWS.ResponseCode.OK) {

                    try {
                        url = ((JSONArray) response).getString(0);
                    } catch (Exception e) {
                        status = CameraWS.ResponseCode.RESPONSE_NOT_WELL_FORMATED;
                    }
                }

                listener.onResult(status, url);
            }
        });
    }

    public void stopLiveView() {
        mCameraWS.sendRequest("stopLiveview", new JSONArray(), null);
    }


    public void actZoom(final ZoomDirection zoomDir) {

        JSONArray params = new JSONArray().
                put(zoomDir == ZoomDirection.IN ? "in" : "out").put("1shot");
        mCameraWS.sendRequest("actZoom", params, null);
    }

    public void actZoom(final ZoomDirection zoomDir, final ZoomAction zoomAct) {

        JSONArray params = new JSONArray().put(zoomDir == ZoomDirection.IN ? "in" : "out").
                put(zoomAct == ZoomAction.START ? "start" : "stop");
        mCameraWS.sendRequest("actZoom", params, null);
    }


    public void setFlash(final boolean enableFlash) {

        // TODO can be "auto" also
        // But this functionnality is not well implemented in all API
        JSONArray params = new JSONArray().put(enableFlash ? "on" : "off");
        mCameraWS.sendRequest("setFlashMode", params, null);
    }




    public interface InitWebServiceListener {
        void onResult(CameraWS.ResponseCode responseCode);
    }

    public interface TakePictureListener {
        void onResult(PictureResponse response);
    }

    public interface VersionListener {
        void onResult(CameraWS.ResponseCode responseCode, int version);
    }

    public interface TestConnectionListener {
        void isConnected(boolean isConnected);
    }

    public interface StartLiveviewListener {
        void onResult(CameraWS.ResponseCode responseCode, String liveviewUrl);
    }

}
