package com.thibaudperso.sonycamera.sdk.core;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 
 * @author Thibaud Michel
 *
 */
public interface CameraWSListener {

	void cameraResponse(JSONArray jsonResponse);

	void cameraError(JSONObject jsonResponse);
	
}
