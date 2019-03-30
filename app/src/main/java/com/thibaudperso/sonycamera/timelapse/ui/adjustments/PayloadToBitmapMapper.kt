package com.thibaudperso.sonycamera.timelapse.ui.adjustments

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import io.reactivex.functions.Function

internal class PayloadToBitmapMapper: Function<Payload, Bitmap> {

    private val factoryOptions = BitmapFactory.Options().apply {
        inSampleSize = 1
        inBitmap = null
        inMutable = true
    }

    @Throws(Exception::class)
    override fun apply(payload: Payload): Bitmap {
        val jpegData = payload.jpegData
        val bitmap = try {
            jpegData.convertToBitmap()
        } catch (iae: IllegalArgumentException) {
            // Clear inBitmap and try again
            factoryOptions.clearInBitmap()
            jpegData.convertToBitmap()
        }
        // Update inBitmap to reuse bitmap if possible
        factoryOptions.inBitmap = bitmap
        return bitmap
    }

    private fun ByteArray.convertToBitmap() = BitmapFactory.decodeByteArray(this, 0, size, factoryOptions)

    private fun BitmapFactory.Options.clearInBitmap() {
        if (inBitmap != null) {
            inBitmap.recycle()
            inBitmap = null
        }
    }
}
