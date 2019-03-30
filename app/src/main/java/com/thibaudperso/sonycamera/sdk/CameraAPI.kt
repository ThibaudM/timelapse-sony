package com.thibaudperso.sonycamera.sdk

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.annotations.CheckReturnValue
import okhttp3.HttpUrl

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CameraAPI @Inject constructor(private val sonyCameraApi: SonyCameraApi) {

    var initialized: Boolean = false
        private set

    var device: SonyDevice? = null
        set(value) {
            if (field != value) {
                initialized = false
                if (value != null) {
                    val url = checkNotNull(value.serviceUrls["camera"])
                    sonyCameraApi.baseUrl = HttpUrl.get(url)
                }
            }
        }

    @CheckReturnValue
    fun initializeWS(): Completable = Completable.defer {
        if (initialized) {
            Completable.complete()
        } else {
            sonyCameraApi.startRecMode()
                    .andThen(sonyCameraApi.getAvailableApiList().ignoreElement())
                    .andThen(sonyCameraApi.setShootMode("still"))
                    .doOnComplete { initialized = true }
        }
    }

    @CheckReturnValue
    fun getVersions(): Single<List<String>> = sonyCameraApi.getVersions()

    @CheckReturnValue
    fun halfPressShutter(): Completable = sonyCameraApi.halfPressShutter()

    @CheckReturnValue
    fun takePicture(): Single<String> = sonyCameraApi.takePicture()

    @CheckReturnValue
    fun testConnection(): Single<List<String>> = getVersions()

    @CheckReturnValue
    fun closeConnection(): Completable = sonyCameraApi.stopRecMode()

    @CheckReturnValue
    fun startLiveView(): Single<String> = sonyCameraApi.startLiveView()

    @CheckReturnValue
    fun stopLiveView(): Completable = sonyCameraApi.stopLiveView()

    @CheckReturnValue
    fun actZoom(zoomDir: SonyCameraApi.ZoomDirection): Completable = sonyCameraApi.zoom(zoomDir)

    @CheckReturnValue
    fun actZoom(zoomDir: SonyCameraApi.ZoomDirection, zoomAct: SonyCameraApi.ZoomAction): Completable =
            sonyCameraApi.startZoom(zoomDir, zoomAct)

    @CheckReturnValue
    fun setFlash(enableFlash: Boolean): Completable =
            sonyCameraApi.setFlashMode(enableFlash.toFlashMode())

    private fun Boolean.toFlashMode() = if (this) {
        SonyCameraApi.FlashMode.ON
    } else {
        SonyCameraApi.FlashMode.OFF
    }

}
