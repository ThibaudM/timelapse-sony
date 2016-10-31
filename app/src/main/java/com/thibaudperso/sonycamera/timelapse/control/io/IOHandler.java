package com.thibaudperso.sonycamera.timelapse.control.io;

import android.content.Context;

import com.thibaudperso.sonycamera.sdk.CameraAPI;

/**
 * Created by thibaud on 24/04/16.
 */
public class IOHandler {


    private final CameraAPI mCameraAPI;
    private final WifiHandler mWifiHandler;

    public IOHandler(Context context) {
        mWifiHandler = new WifiHandler(context);
        mCameraAPI = new CameraAPI(context);
    }

    /*
     * This method is asynchronous
     */
    public void isConnected(TestConnectionListener listener) {

        if(listener == null) {
            return;
        }

        if(!mWifiHandler.isConnected()) {
            listener.isConnected(false);
            return;
        }

        mCameraAPI.testConnection(listener);
    }



    public WifiHandler getWifiHandler() {
        return mWifiHandler;
    }

    public CameraAPI getCameraAPI() {
        return mCameraAPI;
    }




}
