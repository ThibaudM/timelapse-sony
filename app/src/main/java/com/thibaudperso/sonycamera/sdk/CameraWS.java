package com.thibaudperso.sonycamera.sdk;

import android.content.Context;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.thibaudperso.sonycamera.timelapse.control.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Thibaud Michel
 */
public class CameraWS {

    private final static int DEFAULT_WS_TIMEOUT = 5000;

    /**
     * ResponseCode from the camera WS
     * Negative responses have been added for our purpose
     */
    public enum ResponseCode {
        DEVICE_IS_NOT_SET(-4), // means device is not set and url does not exist
        RESPONSE_NOT_WELL_FORMATED(-3), // means web service is unreachable
        WS_UNREACHABLE(-2), // means web service is unreachable
        NONE(-1), // means no code available
        OK(0),
        ANY(1),
        LONG_SHOOTING(40403);

        private int value;

        ResponseCode(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static ResponseCode find(int value) {
            for (ResponseCode el : ResponseCode.values())
                if (el.getValue() == value)
                    return el;
            return NONE; // if not an appropriate found
        }

    }

    private final RequestQueue mJsonQueue;

    private String mWSUrl;
    private int mRequestId;

    CameraWS(Context context) {
        mJsonQueue = Volley.newRequestQueue(context);
        mRequestId = 1;
    }

    void setWSUrl(String wsUrl) {
        mWSUrl = wsUrl;
    }

    void sendRequest(String method, JSONArray params, Listener listener) {
        sendRequest(method, params, listener, DEFAULT_WS_TIMEOUT);
    }

    void sendRequest(final String method, final JSONArray params, final Listener listener,
                     final int timeout) {

        if (mWSUrl == null) {
            if (listener != null) {
                listener.cameraResponse(ResponseCode.DEVICE_IS_NOT_SET, null);
            }
            return;
        }

        final int requestId = mRequestId++;

        final JSONObject inputJsonObject = new JSONObject();
        try {
            inputJsonObject.put("version", "1.0");
            inputJsonObject.put("id", requestId);
            inputJsonObject.put("method", method);
            inputJsonObject.put("params", params);
        } catch (JSONException e) {
            throw new RequestNotWellFormatedException(e);
        }

        Logger.d("[" + getClass().getSimpleName() + "] Request sent to WS: " + inputJsonObject.toString());

        JsonObjectRequest jsObjRequest = new JsonObjectRequest(Request.Method.POST,
                mWSUrl, inputJsonObject, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(final JSONObject response) {

                Logger.d("[" + CameraWS.this.getClass().getSimpleName() + "] Result message: " + response.toString());

                if (listener == null) return;

                try {

                    if (response.has("result")) {
                        listener.cameraResponse(ResponseCode.OK, response.getJSONArray("result"));
                        Logger.d("[" + CameraWS.this.getClass().getSimpleName() + "] Result correctly parsed");

                    } else if (response.has("error")) {
                        //if no "results" element is present, there has probably an error occured
                        //and a "error" element is there instead
                        JSONArray arr = response.getJSONArray("error");
                        ResponseCode errorCode = ResponseCode.find(arr.getInt(0));
                        String errorMessage = arr.getString(1);
                        listener.cameraResponse(errorCode, errorMessage);
                        Logger.d("[" + CameraWS.this.getClass().getSimpleName() + "] " +
                                "Result contains error message: " + errorMessage + " (" + errorCode + ")");
                    } else {
                        listener.cameraResponse(ResponseCode.RESPONSE_NOT_WELL_FORMATED,
                                response);
                        Logger.d("[" + CameraWS.this.getClass().getSimpleName() + "] " +
                                "Result cannot be parsed: " + response.toString());
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                    listener.cameraResponse(ResponseCode.RESPONSE_NOT_WELL_FORMATED, response);
                    Logger.d("[" + CameraWS.this.getClass().getSimpleName() + "] " +
                            "Result is not JSON formatted: " + response.toString() +
                            ", error: " + e.getMessage());

                }
            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                if (listener == null) return;
                error.printStackTrace();
                listener.cameraResponse(ResponseCode.WS_UNREACHABLE, null);
                Logger.d("[" + CameraWS.this.getClass().getSimpleName() + "] " +
                        "Web service unreachable (id=" + (requestId) + ")" +
                        ", error: " + error);
            }
        }

        );

        if (timeout != 0) {
            jsObjRequest.setRetryPolicy(new DefaultRetryPolicy(
                    timeout,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        }

        mJsonQueue.add(jsObjRequest);
    }

    public interface Listener {

        void cameraResponse(ResponseCode responseCode, Object response);

    }

}
