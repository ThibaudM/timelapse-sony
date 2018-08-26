package com.thibaudperso.sonycamera.sdk

import com.google.common.io.Resources
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import org.junit.Test

class SonyDeviceDescriptionParserTest: BaseTest() {

    private val sonyDeviceDescriptionParser = SonyDeviceDescriptionParser()

    @Test
    fun testParseDeviceDescription() {
        // Given
        val resource = Resources.getResource("dd.xml")

        // When
        val sonyCamera = resource.openStream().use {
            sonyDeviceDescriptionParser.parse(it)
        }

        // Then
        assertThat(sonyCamera).isNotNull
        assertThat(sonyCamera.friendlyName).isEqualTo("ILCE-7M3")
        assertThat(sonyCamera.serviceUrls).containsOnly(
            entry("guide", "http://192.168.122.1:10000/sony"),
            entry("system", "http://192.168.122.1:10000/sony"),
            entry("accessControl", "http://192.168.122.1:10000/sony"),
            entry("camera", "http://192.168.122.1:10000/sony")
        )
    }
}