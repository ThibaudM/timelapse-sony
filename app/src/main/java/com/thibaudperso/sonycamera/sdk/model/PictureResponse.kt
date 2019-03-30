package com.thibaudperso.sonycamera.sdk.model

import com.thibaudperso.sonycamera.sdk.ResponseCode
import java.io.Serializable

data class PictureResponse(var status: ResponseCode? = null,
                           var receivedTimeMillis: Long = 0,
                           var url: String? = null) : Serializable
