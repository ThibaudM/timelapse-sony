package com.thibaudperso.camera.core;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 
 * @author Thibaud Michel
 *
 */
public class CameraWS {

	private String mWSUrl;
	private int requestId;

	public CameraWS() {
		requestId = 1;
	}
	
	public void setWSUrl(String wsUrl) {
		mWSUrl = wsUrl;
	}

	public void sendRequest(String method, JSONArray params, CameraWSListener listener)  {
		sendRequest(method, params, listener, 0);
	}

	public void sendRequest(final String method, final JSONArray params, final CameraWSListener listener, 
			final int timeout) {

		if(mWSUrl == null) {
			listener.cameraError(null);
			return;
		}
		
		Thread requestThread = new Thread(new Runnable() {

			@Override
			public void run() {

				HttpParams httpParameters = new BasicHttpParams();
				HttpConnectionParams.setConnectionTimeout(httpParameters, timeout);

				HttpClient httpclient = new DefaultHttpClient(httpParameters);
				HttpPost httppost = new HttpPost(mWSUrl);

				try {
					JSONObject jsonObject = new JSONObject();

					jsonObject.put("version", "1.0");
					jsonObject.put("id", requestId++);
					jsonObject.put("method", method);
					jsonObject.put("params", params);

					StringEntity stringEntity = new StringEntity(jsonObject.toString());
					stringEntity.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));

					httppost.setEntity(stringEntity);

					// Execute HTTP Post Request
					HttpResponse response = httpclient.execute(httppost);

					HttpEntity entity = response.getEntity();
					String responseBody = EntityUtils.toString(entity);					
					JSONObject jsonResponse = new JSONObject(responseBody);

					if(jsonResponse.has("result")) {
						listener.cameraResponse(jsonResponse.getJSONArray("result"));
						return;
					}

					listener.cameraError(jsonResponse);


				} catch (JSONException e) {
					listener.cameraError(null);
					e.printStackTrace();
				} catch (IOException e) {
					listener.cameraError(null);
					e.printStackTrace();
				}
			}

		});

		requestThread.start();

	}


	public void testConnection(final int timeout, final TestConnectionListener listener) {

		if(mWSUrl == null) {
			listener.cameraConnected(false);
			return;
		}
		
		Thread requestThread = new Thread(new Runnable() {

			@Override
			public void run() {

				HttpParams httpParameters = new BasicHttpParams();
				HttpConnectionParams.setConnectionTimeout(httpParameters, timeout);

				HttpClient httpclient = new DefaultHttpClient(httpParameters);
				HttpGet httpget = new HttpGet(mWSUrl);

				try {
					httpclient.execute(httpget);
					listener.cameraConnected(true);
				} catch (IOException e) {
					listener.cameraConnected(false);
				}
			}

		});

		requestThread.start();

	}	

}
