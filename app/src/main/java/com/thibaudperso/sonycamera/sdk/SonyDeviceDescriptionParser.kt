package com.thibaudperso.sonycamera.sdk

import android.util.Xml
import mu.KLogging
import okhttp3.HttpUrl
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream

class SonyDeviceDescriptionParser {

    companion object: KLogging() {
        const val XMLNS_UPNP_DEVICE = "urn:schemas-upnp-org:device-1-0"
        const val XMLNS_AV = "urn:schemas-sony-com:av"
        const val TAG_DEVICE = "device"
        const val TAG_FRIENDLY_NAME = "friendlyName"
        const val TAG_DEVICE_INFO = "X_ScalarWebAPI_DeviceInfo"
        const val TAG_SERVICE_LIST = "X_ScalarWebAPI_ServiceList"
        const val TAG_SERVICE = "X_ScalarWebAPI_Service"
        const val TAG_SERVICE_TYPE = "X_ScalarWebAPI_ServiceType"
        const val TAG_SERVICE_URL = "X_ScalarWebAPI_ActionList_URL"
    }

    fun parse(inputStream: InputStream): SonyDevice {
        val parser = Xml.newPullParser()
        parser.setInput(inputStream, null)
        parser.nextTag()
        return parser.readRoot()
    }

    private fun XmlPullParser.readRoot(): SonyDevice {
        require(XmlPullParser.START_TAG, XMLNS_UPNP_DEVICE, "root")
        while (next() != XmlPullParser.END_TAG) {
            if (eventType != XmlPullParser.START_TAG) {
                continue
            }
            when (name) {
                TAG_DEVICE -> return readDevice()
                else -> skipTag()
            }
        }
        throw IllegalStateException("No device found")
    }

    private fun XmlPullParser.readDevice(): SonyDevice {
        require(XmlPullParser.START_TAG, XMLNS_UPNP_DEVICE, TAG_DEVICE)
        var friendlyName = ""
        val serviceMap = mutableMapOf<String, String>()
        while (next() != XmlPullParser.END_TAG) {
            if (eventType != XmlPullParser.START_TAG) {
                continue
            }
            when (name) {
                TAG_FRIENDLY_NAME -> friendlyName = readFriendlyName()
                TAG_DEVICE_INFO -> readDeviceInfo(serviceMap)
                else -> skipTag()
            }
        }
        require(XmlPullParser.END_TAG, XMLNS_UPNP_DEVICE, TAG_DEVICE)
        return SonyDevice(friendlyName, serviceMap)
    }

    private fun XmlPullParser.readFriendlyName(): String {
        require(XmlPullParser.START_TAG, XMLNS_UPNP_DEVICE, TAG_FRIENDLY_NAME)
        val title = readText()
        require(XmlPullParser.END_TAG, XMLNS_UPNP_DEVICE, TAG_FRIENDLY_NAME)
        return title
    }

    private fun XmlPullParser.readDeviceInfo(serviceMap: MutableMap<String, String>) {
        require(XmlPullParser.START_TAG, XMLNS_AV, TAG_DEVICE_INFO)
        while (next() != XmlPullParser.END_TAG) {
            if (eventType != XmlPullParser.START_TAG) {
                continue
            }
            when (name) {
                TAG_SERVICE_LIST -> readServiceList(serviceMap)
                else -> skipTag()
            }
        }
        require(XmlPullParser.END_TAG, XMLNS_AV, TAG_DEVICE_INFO)
    }

    private fun XmlPullParser.readServiceList(serviceMap: MutableMap<String, String>) {
        require(XmlPullParser.START_TAG, XMLNS_AV, TAG_SERVICE_LIST)
        while (next() != XmlPullParser.END_TAG) {
            if (eventType != XmlPullParser.START_TAG) {
                continue
            }
            when (name) {
                TAG_SERVICE -> readService(serviceMap)
                else -> skipTag()
            }
        }
        require(XmlPullParser.END_TAG, XMLNS_AV, TAG_SERVICE_LIST)
    }

    private fun XmlPullParser.readService(serviceMap: MutableMap<String, String>) {
        require(XmlPullParser.START_TAG, XMLNS_AV, TAG_SERVICE)
        var serviceType = ""
        var serviceUrl = ""
        while (next() != XmlPullParser.END_TAG) {
            if (eventType != XmlPullParser.START_TAG) {
                continue
            }
            when (name) {
                TAG_SERVICE_TYPE -> serviceType = readServiceType()
                TAG_SERVICE_URL -> serviceUrl = readServiceUrl()
                else -> skipTag()
            }
        }
        require(XmlPullParser.END_TAG, XMLNS_AV, TAG_SERVICE)
        if (serviceType.isNotBlank() && serviceUrl.isNotBlank()) {
            serviceMap[serviceType] = HttpUrl.get(serviceUrl).newBuilder().addPathSegment(serviceType).build().toString()
        }
    }

    private fun XmlPullParser.readServiceType(): String {
        require(XmlPullParser.START_TAG, XMLNS_AV, TAG_SERVICE_TYPE)
        val text = readText()
        require(XmlPullParser.END_TAG, XMLNS_AV, TAG_SERVICE_TYPE)
        return text
    }

    private fun XmlPullParser.readServiceUrl(): String {
        require(XmlPullParser.START_TAG, XMLNS_AV, TAG_SERVICE_URL)
        val text = readText()
        require(XmlPullParser.END_TAG, XMLNS_AV, TAG_SERVICE_URL)
        return text
    }

    private fun XmlPullParser.readText(): String {
        var result = ""
        if (next() == XmlPullParser.TEXT) {
            result = text
            nextTag()
        }
        return result
    }

    private fun XmlPullParser.skipTag() {
        if (eventType != XmlPullParser.START_TAG) {
            throw IllegalStateException()
        }
        var depth = 1
        while (depth != 0) {
            when (next()) {
                XmlPullParser.START_TAG -> depth++
                XmlPullParser.END_TAG -> depth--
            }
        }
    }

}