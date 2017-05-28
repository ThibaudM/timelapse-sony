package com.thibaudperso.sonycamera.timelapse.model;

import com.thibaudperso.sonycamera.sdk.model.PictureResponse;

import java.util.LinkedHashMap;

public class ApiRequestsList extends LinkedHashMap<Long, PictureResponse> {

    /*
     * Pre-process attributes
     */
    private int mRequestsSent;
    private int mResponsesReceived;

    private int mResponsesWsUnreachable;
    private int mResponsesLongShot;
    private int mResponsesUnknown;

    private long mLastRequestSent;

    private String mLastPictureUrl;

    ApiRequestsList() {
        initPreProcessAttributes();
    }

    private void initPreProcessAttributes() {

        mRequestsSent = 0;
        mResponsesReceived = 0;
        mResponsesWsUnreachable = 0;
        mResponsesLongShot = 0;
        mResponsesUnknown = 0;
        mLastRequestSent = 0;
        mLastPictureUrl = null;
    }

    @Override
    public void clear() {
        super.clear();
        initPreProcessAttributes();
    }

    @Override
    public PictureResponse put(Long key, PictureResponse value) {

        if (value == null) {
            mRequestsSent++;
            mLastRequestSent = key;
        } else {
            mResponsesReceived++;

            switch (value.status) {
                case OK:
                    mLastPictureUrl = value.url;
                    break;

                case WS_UNREACHABLE:
                    mResponsesWsUnreachable++;
                    break;

                case LONG_SHOOTING:
                    mResponsesLongShot++;
                    break;

                default:
                    mResponsesUnknown++;
            }

        }
        return super.put(key, value);
    }


    public int getRequestsSent() {
        return mRequestsSent;
    }

    public int getResponsesReceived() {
        return mResponsesReceived;
    }

    public int getResponsesWsUnreachable() {
        return mResponsesWsUnreachable;
    }

    public int getResponsesLongShot() {
        return mResponsesLongShot;
    }

    public int getResponsesUnknown() {
        return mResponsesUnknown;
    }

    public int getNumberOfSkippedFrames() {
        return mResponsesLongShot + mResponsesWsUnreachable + mResponsesUnknown;
    }

    public long getLastRequestSent() {
        return mLastRequestSent;
    }

    public String getLastPictureUrl() {
        return mLastPictureUrl;
    }

    public boolean isTakingPicture() {
        return mRequestsSent != mResponsesReceived;
    }

    @Override
    public String toString() {
        return "ApiRequestsList{" +
                "mRequestsSent=" + mRequestsSent +
                ", mResponsesReceived=" + mResponsesReceived +
                ", mResponsesWsUnreachable=" + mResponsesWsUnreachable +
                ", mResponsesLongShot=" + mResponsesLongShot +
                ", mResponsesUnknown=" + mResponsesUnknown +
                '}';
    }

}
