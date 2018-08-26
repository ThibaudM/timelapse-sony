package com.thibaudperso.sonycamera.sdk

/**
 * ResponseCode from the camera WS
 * Negative responses have been added for our purpose
 */
enum class ResponseCode(val value: Int) {
    DEVICE_IS_NOT_SET(-4), // means device is not set and url does not exist
    RESPONSE_NOT_WELL_FORMATTED(-3), // means web service is unreachable
    WS_UNREACHABLE(-2), // means web service is unreachable
    NONE(-1), // means no code available
    OK(0),
    ANY(1),
    LONG_SHOOTING(40403);

}
