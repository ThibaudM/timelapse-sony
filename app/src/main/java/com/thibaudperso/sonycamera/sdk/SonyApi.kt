package com.thibaudperso.sonycamera.sdk

import io.reactivex.Single
import io.reactivex.annotations.CheckReturnValue

interface SonyApi {

    /**
     * This API provides supported versions on the "API service".
     * The client can get the list of API names for specific version using [getMethodTypes] API.
     * The client can get list of versions, which the server supports, using this API.
     */
    @CheckReturnValue
    fun getVersions(): Single<List<String>>

    /**
     * This API provides a function to get the supported APIs for the version.
     * The client can get the list of API names for specific version using this API.
     * The client can get list of versions, which the server supports, using [getVersions] API.
     */
    @CheckReturnValue
    fun getMethodTypes() = getMethodTypes("")

    /**
     * This API provides a function to get the supported APIs for the version.
     * The client can get the list of API names for specific version using this API.
     * The client can get list of versions, which the server supports, using [getVersions] API.
     */
    @CheckReturnValue
    fun getMethodTypes(apiVersion: String = ""): Single<Map<String, MethodInfo>>

    data class MethodInfo(val parameterTypes: List<String>, val responseTypes: List<String>, val version: String)
}
