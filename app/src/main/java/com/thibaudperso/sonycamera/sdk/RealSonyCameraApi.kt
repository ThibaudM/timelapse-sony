package com.thibaudperso.sonycamera.sdk

import com.squareup.moshi.Moshi
import com.thibaudperso.sonycamera.sdk.SonyJsonRpcClient.RpcRequest
import io.reactivex.Completable
import io.reactivex.Single
import okhttp3.HttpUrl
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealSonyCameraApi @Inject constructor(private val sonyJsonRpcClient: SonyJsonRpcClient,
                                            private val moshi: Moshi) : SonyCameraApi {
    private val atomicInteger = AtomicInteger(1)
    private val stringListAdapter by lazy { moshi.adapter<List<String>>(List::class.java) }

    private var initialized = false
    override var baseUrl: HttpUrl = HttpUrl.get("http://localhost")
        get() {
            if (!initialized) {
                throw IllegalStateException("API baseUrl not initialized")
            }
            return field
        }
        set(value) {
            field = value
            initialized = true
        }

    override fun getApplicationInfo(): Single<SonyCameraApi.SonyApplicationInfo> =
            sonyJsonRpcClient.sendRequest(baseUrl, newRequest("getApplicationInfo"))
                    .flatMap { response ->
                        val result = response.result
                        if (response.error.isEmpty()) {
                            Single.just(SonyCameraApi.SonyApplicationInfo(name = result[0].toString(), version = result[1].toString()))
                        } else {
                            Single.error(IOException(response.error[0].toString()))
                        }
                    }

    override fun getVersions(): Single<List<String>> =
            sonyJsonRpcClient.sendRequest(baseUrl, newRequest("getVersions"))
                    .map { response -> (response.result[0] as List<*>).map { it.toString() } }

    override fun getMethodTypes(apiVersion: String): Single<Map<String, SonyApi.MethodInfo>> =
            sonyJsonRpcClient.sendRequest(baseUrl, newRequest("getMethodTypes", listOf(apiVersion)))
                    .map { response ->
                        return@map response.result.chunked(4).map { method ->
                            val name = method[0].toString()

                            val parameterTypes = stringListAdapter.fromJson(method[1].toString()) ?: emptyList()
                            val responseTypes = stringListAdapter.fromJson(method[2].toString()) ?: emptyList()
                            val version = method[3].toString()
                            name to SonyApi.MethodInfo(parameterTypes, responseTypes, version)
                        }.toMap()
                    }

    override fun setSelfTimer(seconds: Int): Completable {
        val request = newRequest("setSelfTimer", listOf(seconds))
        return sonyJsonRpcClient.sendRequest(baseUrl, request).ignoreElement()
    }

    override fun setFocusMode(focusMode: SonyCameraApi.FocusMode): Completable =
            sonyJsonRpcClient.sendRequest(baseUrl, newRequest("setFocusMode", listOf(focusMode.value)))
                    .ignoreElement()

    override fun takePicture(): Single<String> =
            sonyJsonRpcClient.sendRequest(baseUrl, newRequest("actTakePicture"))
                    .map { (it.result[0] as List<*>)[0].toString() }

    override fun stopLiveView(): Completable =
            sonyJsonRpcClient.sendRequest(baseUrl, newRequest("stopLiveview"))
                    .ignoreElement()

    override fun halfPressShutter(): Completable =
            sonyJsonRpcClient.sendRequest(baseUrl, newRequest("actHalfPressShutter"))
                    .ignoreElement()

    override fun getAvailableApiList(): Single<List<String>> =
            sonyJsonRpcClient.sendRequest(baseUrl, newRequest("getAvailableApiList"))
                    .map { response -> (response.result[0] as List<*>).map { it.toString() } }

    override fun setShootMode(shootMode: String): Completable =
            sonyJsonRpcClient.sendRequest(baseUrl, newRequest("setShootMode", listOf(shootMode)))
                    .ignoreElement()

    override fun startRecMode(): Completable =
            sonyJsonRpcClient.sendRequest(baseUrl, newRequest("startRecMode"))
                    .ignoreElement()

    override fun stopRecMode(): Completable =
            sonyJsonRpcClient.sendRequest(baseUrl, newRequest("stopRecMode"))
                    .ignoreElement()

    override fun startLiveView(): Single<String> {
        return sonyJsonRpcClient.sendRequest(baseUrl, newRequest("startLiveview"))
                .map { it.result[0].toString() }
    }

    override fun zoom(zoomDir: SonyCameraApi.ZoomDirection): Completable =
            sonyJsonRpcClient.sendRequest(baseUrl, newRequest("actZoom", listOf(zoomDir.name.toLowerCase(), "1shot")))
                    .ignoreElement()

    override fun startZoom(zoomDir: SonyCameraApi.ZoomDirection, zoomAction: SonyCameraApi.ZoomAction): Completable =
            sonyJsonRpcClient.sendRequest(baseUrl, newRequest("actZoom", listOf(zoomDir.name.toLowerCase(), zoomAction.name.toLowerCase())))
                    .ignoreElement()

    override fun setFlashMode(flashMode: SonyCameraApi.FlashMode): Completable =
            sonyJsonRpcClient.sendRequest(baseUrl, newRequest("setFlashMode", listOf(flashMode.name.toLowerCase())))
                    .ignoreElement()

    // ==========================================================================================================================
    // Private API
    // ==========================================================================================================================

    private fun newRequest(method: String,
                           params: List<Any> = emptyList()) = RpcRequest(id = atomicInteger.getAndIncrement(), method = method, params = params)
}