package com.thibaudperso.sonycamera.sdk.model;

import com.thibaudperso.sonycamera.sdk.CameraWS;

import java.io.Serializable;


public class PictureResponse implements Serializable {

    public CameraWS.ResponseCode status;
    public long time; // picture received time in millis
    public String url;

    @Override
    public String toString() {
        return "PictureResponse{" +
                "status=" + status +
                ", time=" + time +
                ", url='" + url + '\'' +
                '}';
    }
}
