package com.thibaudperso.sonycamera.sdk;

import android.content.Context;
import android.util.Log;

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

import static com.thibaudperso.sonycamera.timelapse.Constants.LOG_TAG;

/**
 * @author Thibaud Michel
 */
public class CameraWS {


    /**
     * ResponseCode from the camera WS
     * Negative responses have been added for our purpose
     */
    public enum ResponseCode {
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
        sendRequest(method, params, listener, 0);
    }

    void sendRequest(final String method, final JSONArray params, final Listener listener,
                     final int timeout) {

        if (mWSUrl == null) {
            throw new NullPointerException();
        }

        final JSONObject inputJsonObject = new JSONObject();
        try {
            inputJsonObject.put("version", "1.0");
            inputJsonObject.put("id", mRequestId++);
            inputJsonObject.put("method", method);
            inputJsonObject.put("params", params);
        } catch (JSONException e) {
            throw new RequestNotWellFormatedException(e);
        }

        JsonObjectRequest jsObjRequest = new JsonObjectRequest(Request.Method.POST,
                mWSUrl, inputJsonObject, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(final JSONObject response) {

                if (listener == null) return;

                try {

                    if (response.has("result")) {
                        listener.cameraResponse(ResponseCode.OK, response.getJSONArray("result"));

                    } else if (response.has("error")) {
                        //if no "results" element is present, there has probably an error occured
                        //and a "error" element is there instead
                        JSONArray arr = response.getJSONArray("error");
                        ResponseCode errorCode = ResponseCode.find(arr.getInt(0));
                        String errorMessage = arr.getString(1);
                        listener.cameraResponse(errorCode, errorMessage);
                        Log.d(LOG_TAG, "Request: " + inputJsonObject);
                        Log.d(LOG_TAG, "Error code: " + arr.getInt(0) + ", error message: " +
                                arr);
                    } else {
                        listener.cameraResponse(ResponseCode.RESPONSE_NOT_WELL_FORMATED,
                                response);
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                    listener.cameraResponse(ResponseCode.RESPONSE_NOT_WELL_FORMATED,
                            response);

                }
            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                if (listener == null) return;
                listener.cameraResponse(ResponseCode.WS_UNREACHABLE, null);
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
