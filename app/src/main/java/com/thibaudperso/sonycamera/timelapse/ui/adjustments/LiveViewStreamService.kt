package com.thibaudperso.sonycamera.timelapse.ui.adjustments

import io.reactivex.Observable
import mu.KLogging
import okhttp3.*
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiveViewStreamService @Inject constructor(private val okHttpClient: OkHttpClient) {

    companion object : KLogging() {
        /**
         * Bytes    Usage
         * 1        Start byte
         * 1        Payload type
         * 2        Sequence number
         * 4        Time stamp
         */
        const val COMMON_HEADER_LENGTH = 8L
        const val PAYLOAD_HEADER_LENGTH = 128L

        const val START_BYTE = 0xFF.toByte()
        val PAYLOAD_HEADER_START_CODE: ByteArray = byteArrayOf(0x24, 0x35, 0x68, 0x79)
    }

    fun stream(stream: HttpUrl): Observable<Payload> = Observable.create<Payload> { emitter ->
        val request = Request.Builder().get().url(stream).build()

        val call = okHttpClient.newCall(request)

        emitter.setCancellable { call.cancel() }

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                emitter.tryOnError(e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (it.isSuccessful) {
                        // Success
                        val responseBody = it.body()
                        if (responseBody != null) {
                            val bufferedSource = responseBody.source()
                            while (!bufferedSource.exhausted() && !emitter.isDisposed) {
                                val commonHeader = bufferedSource.readByteArray(COMMON_HEADER_LENGTH)
                                if (commonHeader.size.toLong() != COMMON_HEADER_LENGTH) {
                                    emitter.tryOnError(IOException("Cannot read stream for common header."))
                                    break
                                }
                                if (commonHeader[0] != START_BYTE) {
                                    emitter.tryOnError(IOException("Unexpected data format. (Start byte)"))
                                    break
                                }
                                val payloadHeader = bufferedSource.readByteArray(PAYLOAD_HEADER_LENGTH)
                                if (payloadHeader.size.toLong() != PAYLOAD_HEADER_LENGTH) {
                                    emitter.tryOnError(IOException("Cannot read stream for payload header."))
                                    break
                                }
                                if (!payloadHeader.equalsRange(PAYLOAD_HEADER_START_CODE)) {
                                    emitter.tryOnError(IOException("Unexpected data format. (Start code)"))
                                    break
                                }
                                val jpegSize = payloadHeader.bytesToInt(4, 3)
                                val paddingSize = payloadHeader.bytesToInt(7, 1)

                                val jpegData = bufferedSource.readByteArray(jpegSize.toLong())
                                bufferedSource.skip(paddingSize.toLong())

                                emitter.onNext(Payload(jpegData))
                            }
                        } else {
                            emitter.tryOnError(IOException("No response body"))
                        }
                    } else {
                        emitter.tryOnError(HttpException(retrofit2.Response.error<Payload>(response.code(), response.body()!!)))
                    }
                }
            }
        })
    }

}