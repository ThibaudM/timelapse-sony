package com.thibaudperso.sonycamera.timelapse.ui.adjustments

import java.util.*

/**
 * Payload data class. See also Camera Remote API specification document to
 * know the data structure.
 */
data class Payload(val jpegData: ByteArray) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Payload

        if (!Arrays.equals(jpegData, other.jpegData)) return false

        return true
    }

    override fun hashCode(): Int {
        return Arrays.hashCode(jpegData)
    }
}
