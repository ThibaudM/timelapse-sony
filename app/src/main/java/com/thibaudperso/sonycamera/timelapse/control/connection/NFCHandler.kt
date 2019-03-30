package com.thibaudperso.sonycamera.timelapse.control.connection

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentFilter.MalformedMimeTypeException
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NfcF
import android.os.Parcelable
import android.util.Pair

object NFCHandler {

    private const val SONY_MIME_TYPE = "application/x-sony-pmm"

    val techListArray: Array<Array<String>>
        get() = arrayOf(arrayOf(NfcF::class.java.name))

    val intentFilterArray: Array<IntentFilter>
        get() {
            val ndef = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)
            try {
                ndef.addDataType(SONY_MIME_TYPE)
            } catch (e: MalformedMimeTypeException) {
                throw RuntimeException("fail", e)
            }

            return arrayOf(ndef)
        }

    fun getPendingIntent(activity: Activity): PendingIntent {
        return PendingIntent.getActivity(activity, 0,
                Intent(activity, activity.javaClass)
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0)
    }

    @Throws(Exception::class)
    fun parseIntent(intent: Intent): Pair<String, String>? {

        val tagFromIntent = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
        val messages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)

        return if (tagFromIntent != null && messages != null) {
            getCameraWifiSettingsFromTag(tagFromIntent, messages)
        } else null

    }

    @Throws(Exception::class)
    fun getCameraWifiSettingsFromTag(tag: Tag, messages: Array<Parcelable>): Pair<String, String>? {

        val ndef = Ndef.get(tag)
        ndef.connect()

        val record = (messages[0] as NdefMessage).records[0]
        val cameraWifiSettings = decodeSonyPPMMessage(record)

        ndef.close()

        return cameraWifiSettings
    }

    private fun decodeSonyPPMMessage(ndefRecord: NdefRecord): Pair<String, String>? {
        if (SONY_MIME_TYPE != String(ndefRecord.type)) {
            return null
        }

        try {
            val payload = ndefRecord.payload

            val ssidBytesStart = 8
            val ssidLength = payload[ssidBytesStart].toInt()

            val ssidBytes = ByteArray(ssidLength)
            var ssidPointer = 0
            for (i in ssidBytesStart + 1..ssidBytesStart + ssidLength) {
                ssidBytes[ssidPointer++] = payload[i]
            }
            val ssid = String(ssidBytes)

            val passwordBytesStart = ssidBytesStart + ssidLength + 4
            val passwordLength = payload[passwordBytesStart].toInt()

            val passwordBytes = ByteArray(passwordLength)
            var passwordPointer = 0
            for (i in passwordBytesStart + 1..passwordBytesStart + passwordLength) {
                passwordBytes[passwordPointer++] = payload[i]
            }
            val password = String(passwordBytes)

            return Pair(ssid, password)

        } catch (e: Exception) {
            return null
        }

    }

}
