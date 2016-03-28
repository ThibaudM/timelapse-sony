package com.thibaudperso.sonycamera.sdk.core;

import android.content.Context;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Thibaud Michel
 */
public class CameraWS {

    private final RequestQueue mJsonQueue;

    private String mWSUrl;
    private int mRequestId;

    public CameraWS(Context context) {
        mJsonQueue = Volley.newRequestQueue(context);
        mRequestId = 1;
    }

    public void setWSUrl(String wsUrl) {
        mWSUrl = wsUrl;
    }

    public void sendRequest(String method, JSONArray params, CameraWSListener listener) {
        sendRequest(method, params, listener, 0);
    }

    public void sendRequest(final String method, final JSONArray params, final CameraWSListener listener,
                            final int timeout) {

        if (mWSUrl == null) {
            if (listener != null) {
                listener.cameraError(null);
            }
            return;
        }

        JSONObject inputJsonObject = new JSONObject();
        try {
            inputJsonObject.put("version", "1.0");
            inputJsonObject.put("id", mRequestId++);
            inputJsonObject.put("method", method);
            inputJsonObject.put("params", params);
        } catch (JSONException e) {
            if (listener != null) {
                listener.cameraError(null);
            }
            e.printStackTrace();
            return;
        }

        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.POST, mWSUrl, inputJsonObject, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {

                        if (listener != null) {
                            if (response.has("result")) {
                                try {
                                    listener.cameraResponse(response.getJSONArray("result"));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    listener.cameraError(response);
                                }
                            } else {
                                //if no "results" element is present, there has probably an error occured
                                //and a "error" element is there instead
                                listener.cameraError(response);
                            }
                        }

                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (listener != null) {
                            listener.cameraError(null);
                        }
                    }
                });

        if (timeout != 0) {
            jsObjRequest.setRetryPolicy(new DefaultRetryPolicy(
                    timeout,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        }

        mJsonQueue.add(jsObjRequest);
    }

}
