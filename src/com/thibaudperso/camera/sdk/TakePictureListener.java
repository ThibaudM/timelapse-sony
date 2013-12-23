package com.thibaudperso.camera.sdk;

/**
 * 
 * @author Thibaud Michel
 *
 */
public interface TakePictureListener {

	void onResult(String url);
	void onError(String error);
	
}
