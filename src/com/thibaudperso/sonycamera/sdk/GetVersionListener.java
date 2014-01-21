package com.thibaudperso.sonycamera.sdk;

/**
 * 
 * @author Thibaud Michel
 *
 */
public interface GetVersionListener {

	void onResult(int version);
	void onError(String error);
	
}
