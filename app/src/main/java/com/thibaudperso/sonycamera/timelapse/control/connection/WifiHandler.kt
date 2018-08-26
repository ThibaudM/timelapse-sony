package com.thibaudperso.sonycamera.timelapse.control.connection

import android.Manifest
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.NetworkInfo
import android.net.wifi.ScanResult
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import androidx.core.content.ContextCompat
import mu.KLogging
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WifiHandler @Inject constructor(private val application: Application) {

    companion object : KLogging() {
        fun isSonyCameraSSID(ssid: String?): Boolean = ssid != null && ssid.matches("^DIRECT-\\w{4}:.*$".toRegex())
        fun parseSSID(ssid: String): String? = if (ssid.length >= 2 && ssid.startsWith('"') && ssid.endsWith('"')) {
            ssid.substring(1, ssid.length - 1)
        } else ssid
    }

    private val wifiManager: WifiManager = application.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private val broadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            if (listener == null) {
                return
            }
            if (isInitialStickyBroadcast) {
                return
            }

            when (intent.action) {
                WifiManager.NETWORK_STATE_CHANGED_ACTION -> {
                    val networkInfo = intent.getParcelableExtra<NetworkInfo>(WifiManager.EXTRA_NETWORK_INFO)
                    if (networkInfo.isConnected
                            && currentBSSID != wifiManager.connectionInfo.ssid
                            && isFirstConnection) {

                        currentBSSID = wifiManager.connectionInfo.ssid

                        listener?.wifiConnected(networkInfo)
                        isFirstConnection = false
                        isFirstDisconnection = true
                    } else if (!networkInfo.isConnected && isFirstDisconnection) {
                        listener?.wifiDisconnected(networkInfo)
                        isFirstDisconnection = false
                        isFirstConnection = true
                    }
                }
                WifiManager.WIFI_STATE_CHANGED_ACTION -> {
                    val state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
                            WifiManager.WIFI_STATE_UNKNOWN)
                    if (state == WifiManager.WIFI_STATE_ENABLED) {
                        listener?.wifiEnabled()
                    } else if (state == WifiManager.WIFI_STATE_DISABLED) {
                        listener?.wifiDisabled()
                    }
                }
                WifiManager.SCAN_RESULTS_AVAILABLE_ACTION -> {
                    if (ContextCompat.checkSelfPermission(application,
                                    Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        val sonyCameraScanResults = ArrayList<ScanResult>()
                        val knownSonyCameraConfigurations = ArrayList<WifiConfiguration>()

                        for (sr in wifiManager.scanResults) {
                            if (!isSonyCameraSSID(sr.SSID)) {
                                continue
                            }
                            sonyCameraScanResults.add(sr)
                            val wc = getWifiConfigurationFromSSID(sr.SSID) ?: continue
                            knownSonyCameraConfigurations.add(wc)
                        }
                        listener?.onWifiScanFinished(sonyCameraScanResults, knownSonyCameraConfigurations)
                    }
                }
            }
        }
    }

    // Workaround for multiple broadcast of wifi events:
    // http://stackoverflow.com/questions/8412714/broadcastreceiver-receives-multiple-identical-messages-for-one-event
    // We added bssid to the condition, because sometimes two wifi are connected in a row without disconnection
    private var isFirstConnection = true
    private var isFirstDisconnection = true
    private var currentBSSID = ""

    val connectedWifi: WifiInfo
        get() = wifiManager.connectionInfo

    val isEnabled: Boolean
        get() = wifiManager.isWifiEnabled

    var listener: Listener? = null
        set(value) {
            if (value != null && field == null) {
                val intentFilter = IntentFilter().apply {
                    addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
                    addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
                    addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
                }
                application.registerReceiver(broadcastReceiver, intentFilter)
            } else if (value == null && field != null) {
                application.unregisterReceiver(broadcastReceiver)
            }
            field = value
        }

    fun connectToNetworkId(netId: Int): Boolean {
        logger.warn { "connectToNetworkId[netId=$netId]" }
        /*
         * Check if network id exists
         */
        var netIdFound = false
        val configuredNetworks = wifiManager.configuredNetworks
        for (wc in configuredNetworks) {
            if (wc.networkId == netId) {
                netIdFound = true
                break
            }
        }
        if (!netIdFound) {
            logger.warn("Network with id {} not found", netId)
            return false
        }

        return wifiManager.enableNetwork(netId, true)
    }

    @Suppress("DEPRECATION")
    fun startScan() {
        wifiManager.startScan()
    }

    fun disconnect() {
        wifiManager.disconnect()
    }

    fun createIfNeededThenConnectToWifi(networkSSID: String, networkPassword: String): Int {
        var netId = -1

        val list: List<WifiConfiguration> = wifiManager.configuredNetworks.orEmpty()
        for (wifiConfiguration: WifiConfiguration in list) {
            // In the case of network is already registered
            if (wifiConfiguration.SSID != null && wifiConfiguration.SSID == "\"" + networkSSID + "\"") {

                // In case password changed since last time
                wifiConfiguration.preSharedKey = "\"" + networkPassword + "\""
                wifiManager.saveConfiguration()

                netId = wifiConfiguration.networkId
                break
            }
        }

        // In the case of network is not registered create it and join it
        if (netId == -1) {
            netId = createWPAWifi(networkSSID, networkPassword)
        }

        connectToNetworkId(netId)
        return netId
    }

    private fun getWifiConfigurationFromSSID(ssid: String): WifiConfiguration? =
            wifiManager.configuredNetworks.firstOrNull { it.SSID != null && it.SSID == "\"$ssid\"" }

    private fun createWPAWifi(networkSsid: String, networkPassword: String): Int {
        val wc = WifiConfiguration().apply {
            SSID = "\"$networkSsid\""
            preSharedKey = "\"$networkPassword\""
            hiddenSSID = true
            status = WifiConfiguration.Status.ENABLED
            allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP)
            allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP)
            allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
            allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP)
            allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP)
            allowedProtocols.set(WifiConfiguration.Protocol.RSN)
        }
        val netId = wifiManager.addNetwork(wc)
        wifiManager.saveConfiguration()
        return netId
    }

    interface Listener {
        fun wifiEnabled()
        fun wifiDisabled()
        fun wifiConnected(networkInfo: NetworkInfo)
        fun wifiDisconnected(networkInfo: NetworkInfo)
        fun onWifiScanFinished(sonyCameraScanResults: List<ScanResult>, configurations: List<WifiConfiguration>)
    }

}
