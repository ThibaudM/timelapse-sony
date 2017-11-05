package com.thibaudperso.sonycamera.timelapse.control;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileRequest<T> extends Request<T> {

    private Response.Listener<T> mListener;
    private File mOutputFile;

    public FileRequest(String url, File outputFile, Response.Listener<T> listener,
                       Response.ErrorListener errorListener) {
        super(Method.GET, url, errorListener);
        mListener = listener;
        mOutputFile = outputFile;
    }

    @Override
    protected Response<T> parseNetworkResponse(NetworkResponse response) {

        try {
            BufferedOutputStream bos = new BufferedOutputStream(
                    new FileOutputStream(mOutputFile));
            bos.write(response.data);
            bos.flush();
            bos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return Response.success(null, HttpHeaderParser.parseCacheHeaders(response));

    }

    @Override
    protected void deliverResponse(T response) {
        mListener.onResponse(null);
    }
}