package com.thibaudperso.sonycamera.sdk;

public interface StartLiveviewListener {

	void onResult(String liveviewUrl);
	void onError(String error);
	
}
