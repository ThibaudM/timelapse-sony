package com.thibaudperso.sonycamera.timelapse.control

import com.thibaudperso.sonycamera.sdk.SonyDevice
import com.thibaudperso.sonycamera.sdk.SonyDeviceDescriptionParser
import io.reactivex.Observable
import io.resourcepool.ssdp.client.SsdpClient
import io.resourcepool.ssdp.model.DiscoveryListener
import io.resourcepool.ssdp.model.DiscoveryRequest
import io.resourcepool.ssdp.model.SsdpService
import io.resourcepool.ssdp.model.SsdpServiceAnnouncement
import mu.KLogging
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CameraDetectionService @Inject constructor(private val okHttpClient: OkHttpClient) {

    companion object: KLogging() {
        private const val SERVICE_TYPE = "urn:schemas-sony-com:service:ScalarWebAPI:1"
    }

    private val sonyDeviceDescriptionParser by lazy { SonyDeviceDescriptionParser() }

    val discoverCameras: Observable<SonyDevice>
        get() = Observable.create<SsdpService> { emitter ->
            val listener = object: DiscoveryListener {
                override fun onFailed(ex: Exception) {
                    logger.warn { "onFailed -> $ex" }
                    emitter.tryOnError(ex)
                }

                override fun onServiceDiscovered(service: SsdpService) {
                    logger.warn { "onServiceDiscovered -> $service" }
                    emitter.onNext(service)
                }

                override fun onServiceAnnouncement(announcement: SsdpServiceAnnouncement) {
                    logger.warn { "onServiceAnnouncement -> $announcement" }
                }
            }
            val ssdpClient = SsdpClient.create()
            val discoveryRequest = DiscoveryRequest.builder().apply {
                serviceType(SERVICE_TYPE)
            }.build()
            emitter.setCancellable { ssdpClient.stopDiscovery() }
            ssdpClient.discoverServices(discoveryRequest, listener)
        }.flatMap(this::readDeviceDescription)

    private fun readDeviceDescription(ssdpService: SsdpService): Observable<SonyDevice> = Observable.create { emitter ->
        val request = Request.Builder().apply {
            get()
            url(ssdpService.location)
        }.build()

        val call = okHttpClient.newCall(request)

        emitter.setCancellable { call.cancel() }

        call.enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                emitter.tryOnError(e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { res ->
                    if (res.isSuccessful) {
                        val responseBody = res.body()
                        if (responseBody != null) {
                            val sonyDevice = sonyDeviceDescriptionParser.parse(responseBody.byteStream())
                            emitter.onNext(sonyDevice)
                        }
                    }
                }
            }
        })
    }
}