package com.thibaudperso.sonycamera.timelapse.model;

import java.io.Serializable;


public class IntervalometerSettings implements Serializable {

    public int initialDelay = 0;
    public int intervalTime = 0;

    /**
     * 0 for infinity
     */
    public int framesCount = 0;


    public boolean isUnlimitedMode() {
        return framesCount == 0;
    }

    @Override
    public String toString() {
        return "IntervalometerSettings{" +
                "initialDelay=" + initialDelay +
                ", intervalTime=" + intervalTime +
                ", framesCount=" + framesCount +
                '}';
    }

}
