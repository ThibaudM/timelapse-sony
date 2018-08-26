package com.thibaudperso.sonycamera.timelapse.model

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimelapseData @Inject constructor() {

    var startTime: Long = 0L
    var settings: IntervalometerSettings = IntervalometerSettings()
    val apiRequestsList: ApiRequestsList = ApiRequestsList()

    /*
     * Should not be called if it's not unlimited mode
     */
    var finished: Boolean = false

    val isTimelapseIsFinished: Boolean
        get() = finished || !settings.isUnlimitedMode && settings.framesCount == apiRequestsList.requestsSent

    fun clear() {
        settings = IntervalometerSettings()
        apiRequestsList.clear()
        startTime = 0L
        finished = false
    }


}
