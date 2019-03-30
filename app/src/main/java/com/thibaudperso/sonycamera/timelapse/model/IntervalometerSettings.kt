package com.thibaudperso.sonycamera.timelapse.model

import java.io.Serializable

data class IntervalometerSettings(var initialDelay: Long = 0L,
                                  var intervalTime: Long = 0L,
                                  /**
                                   * 0 for infinity
                                   */
                                  var framesCount: Int = 0) : Serializable {

    val isUnlimitedMode: Boolean
        get() = framesCount == 0

}
