package com.thibaudperso.sonycamera.timelapse.model

import com.thibaudperso.sonycamera.sdk.ResponseCode
import com.thibaudperso.sonycamera.sdk.model.PictureResponse
import java.util.*

class ApiRequestsList : LinkedHashMap<Long, PictureResponse?>() {

    var requestsSent: Int = 0
        private set
    var responsesReceived: Int = 0
        private set

    var responsesWsUnreachable: Int = 0
        private set
    var responsesLongShot: Int = 0
        private set
    var responsesUnknown: Int = 0
        private set

    var lastRequestSent: Long = 0
        private set

    var lastPictureUrl: String? = null
        private set

    val numberOfSkippedFrames: Int
        get() = responsesLongShot + responsesWsUnreachable + responsesUnknown

    val isTakingPicture: Boolean
        get() = requestsSent != responsesReceived

    init {
        initPreProcessAttributes()
    }

    override fun clear() {
        super.clear()
        initPreProcessAttributes()
    }

    override fun put(key: Long, value: PictureResponse?): PictureResponse? {
        if (value == null) {
            requestsSent++
            lastRequestSent = key
        } else {
            responsesReceived++
            when (value.status) {
                ResponseCode.OK -> lastPictureUrl = value.url
                ResponseCode.WS_UNREACHABLE -> responsesWsUnreachable++
                ResponseCode.LONG_SHOOTING -> responsesLongShot++
                else -> responsesUnknown++
            }
        }
        return super.put(key, value)
    }

    override fun toString(): String = "ApiRequestsList{requestsSent=$requestsSent, responsesReceived=$responsesReceived, responsesWsUnreachable=$responsesWsUnreachable, responsesLongShot=$responsesLongShot, responsesUnknown=$responsesUnknown}"

    private fun initPreProcessAttributes() {
        requestsSent = 0
        responsesReceived = 0
        responsesWsUnreachable = 0
        responsesLongShot = 0
        responsesUnknown = 0
        lastRequestSent = 0
        lastPictureUrl = null
    }

}
