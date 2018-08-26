package com.thibaudperso.sonycamera.sdk

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.annotations.CheckReturnValue
import okhttp3.HttpUrl

interface SonyCameraApi : SonyApi {

    var baseUrl: HttpUrl

    @CheckReturnValue
    fun getApplicationInfo(): Single<SonyApplicationInfo>

    /**
     * This API provides a function to set a value of self-timer.
     * @param seconds Can be either 0 (off), 2 or 10 seconds.
     */
    @CheckReturnValue
    fun setSelfTimer(seconds: Int): Completable

    /**
     * This API provides a function to set a value of focus mode.
     */
    @CheckReturnValue
    fun setFocusMode(focusMode: FocusMode): Completable

    /**
     * This API provides a function to take picture.
     *
     * @return Stream which upon success emits an URL to an image preview.
     */
    @CheckReturnValue
    fun takePicture(): Single<String>

    @CheckReturnValue
    fun halfPressShutter(): Completable

    /**
     * This API provides a function to get the available API names that the server supports at the moment.
     */
    @CheckReturnValue
    fun getAvailableApiList(): Single<List<String>>

    /**
     * Sets the shoot mode, "still" or "movie". This needs to be set to "still"
     * on some camcorders, because they default to video.
     *
     * @param shootMode either "still" or "movie".
     */
    @CheckReturnValue
    fun setShootMode(shootMode: String): Completable

    /**
     * This API provides a function to set up camera for shooting function.
     * Some camera models need this API call before starting liveview, capturing still image, recording movie, or
     * accessing all other camera shooting functions.
     */
    @CheckReturnValue
    fun startRecMode(): Completable

    @CheckReturnValue
    fun stopRecMode(): Completable

    @CheckReturnValue
    fun startLiveView(): Single<String>

    @CheckReturnValue
    fun stopLiveView(): Completable

    @CheckReturnValue
    fun zoom(zoomDir: ZoomDirection): Completable

    @CheckReturnValue
    fun startZoom(zoomDir: ZoomDirection, zoomAction: ZoomAction): Completable

    @CheckReturnValue
    fun setFlashMode(flashMode: FlashMode): Completable

    // ==========================================================================================================================
    // Models
    // ==========================================================================================================================

    data class SonyApplicationInfo(val name: String, val version: String)

    enum class FocusMode(val value: String) {
        AF_S("AF-S"), AF_C("AF-C"), DMF("DMF"), MF("MF")
    }

    enum class ZoomDirection {
        IN, OUT
    }

    enum class ZoomAction {
        START, STOP
    }

    enum class FlashMode {
        ON, OFF, AUTO
    }

}

