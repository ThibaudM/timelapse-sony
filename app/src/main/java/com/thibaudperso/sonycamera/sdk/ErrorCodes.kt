package com.thibaudperso.sonycamera.sdk

enum class ErrorCodes(val code: Int) {

    ShootingFail(40400),
    Unknown(-1);

    companion object {
        @JvmStatic
        fun fromCode(code: Int) = when (code) {
            40400 -> ShootingFail
            else -> Unknown
        }
    }

}