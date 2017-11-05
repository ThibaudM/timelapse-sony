package com.thibaudperso.sonycamera.timelapse.model;

import java.io.Serializable;

public class TimelapseData implements Serializable {

    private long mStartTime;
    private IntervalometerSettings mSettings;
    private ApiRequestsList mApiRequestsList;

    private boolean mIsFinished;

    public TimelapseData() {
        mSettings = new IntervalometerSettings();
        mApiRequestsList = new ApiRequestsList();
        mIsFinished = false;
    }

    public void clear() {
        mSettings = new IntervalometerSettings();
        mApiRequestsList.clear();
        mStartTime = 0;
        mIsFinished = false;
    }

    public long getStartTime() {
        return mStartTime;
    }

    public void setStartTime(long startTime) {
        this.mStartTime = startTime;
    }

    public IntervalometerSettings getSettings() {
        return mSettings;
    }

    public void setSettings(IntervalometerSettings settings) {
        mSettings = settings;
    }

    public ApiRequestsList getApiRequestsList() {
        return mApiRequestsList;
    }

    /*
     * Should not be called if it's not unlimited mode
     */
    public void setFinished(boolean finished) {
        mIsFinished = finished;
    }

    public boolean isTimelapseIsFinished() {
        return mIsFinished || !mSettings.isUnlimitedMode()
                && mSettings.framesCount == mApiRequestsList.getRequestsSent();
    }
}
