package com.thibaudperso.sonycamera.timelapse.ui.adjustments

import java.math.BigInteger

fun ByteArray.equalsRange(other: ByteArray, offset: Int = 0, length: Int = other.size): Boolean {
    if (this === other) {
        // Same instance
        return true
    }
    if (size < (offset + length) || other.size < length) {
        // Not enough bytes
        return false
    }
    for (i in 0 until length) {
        if (this[offset + i] != other[i]) {
            return false
        }
    }
    return true
}

/**
 * Converts parts of byte array to int.
 */
fun ByteArray.bytesToInt(startIndex: Int, count: Int): Int = BigInteger(copyOfRange(startIndex, startIndex + count)).toInt()