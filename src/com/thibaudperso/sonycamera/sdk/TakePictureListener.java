package com.thibaudperso.sonycamera.sdk;

/**
 * 
 * @author Thibaud Michel
 *
 */
public interface TakePictureListener {

	void onResult(String url);
	void onError(CameraIO.ResponseCode responseCode, String responseMsg);
	
}
