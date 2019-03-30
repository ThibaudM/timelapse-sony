package com.thibaudperso.sonycamera.sdk

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import io.reactivex.Single
import mu.KLogging
import okhttp3.*
import java.io.IOException
import javax.inject.Inject

class SonyJsonRpcClient @Inject constructor(private val okHttpClient: OkHttpClient,
                                            private val moshi: Moshi) {
    companion object : KLogging()

    private val requestAdapter by lazy { moshi.adapter(RpcRequest::class.java) }
    private val responseAdapter by lazy { moshi.adapter(RpcResponse::class.java) }
    private val mediaTypeJson by lazy { MediaType.get("application/json") }

    fun sendRequest(baseUrl: HttpUrl, rpcRequest: RpcRequest): Single<RpcResponse> = Single.create { emitter ->
        val json = requestAdapter.toJson(rpcRequest)
        logger.warn { "sendRequest with JSON: '$json'" }
        val request = Request.Builder()
                .url(baseUrl)
                .post(RequestBody.create(mediaTypeJson, json))
                .build()

        val call = okHttpClient.newCall(request)

        emitter.setCancellable { call.cancel() }

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                emitter.tryOnError(e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (it.isSuccessful) {
                        val responseBody = it.body()
                        if (responseBody != null) {
                            val responseString = responseBody.string()
                            logger.warn { "Response: $responseString" }
                            responseAdapter.fromJson(responseString)?.let { rpcResponse ->
                                emitter.onSuccess(rpcResponse)
                            }
                        }
                    }
                }
            }
        })
    }

    @JsonClass(generateAdapter = true)
    data class RpcRequest(val id: Int, val method: String, val params: List<Any> = emptyList(), val version: String = "1.0")

    @JsonClass(generateAdapter = true)
    data class RpcResponse(val id: Int, val result: List<Any> = emptyList(), val error: List<Any> = emptyList())
}