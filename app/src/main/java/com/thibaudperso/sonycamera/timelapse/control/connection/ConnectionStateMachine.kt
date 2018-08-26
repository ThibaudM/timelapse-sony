package com.thibaudperso.sonycamera.timelapse.control.connection

import android.app.Application
import android.content.Context
import android.net.NetworkInfo
import android.net.wifi.ScanResult
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import com.thibaudperso.sonycamera.sdk.CameraAPI
import com.thibaudperso.sonycamera.timelapse.control.CameraDetectionService
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import mu.KLogging
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectionStateMachine @Inject constructor(internal val application: Application,
                                                 internal val wifiHandler: WifiHandler,
                                                 internal val cameraAPI: CameraAPI,
                                                 internal val cameraDetectionService: CameraDetectionService) {

    companion object : KLogging();

    private val wifiManager by lazy { application.getSystemService(Context.WIFI_SERVICE) as WifiManager }
    private val wifiLock by lazy {
        wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "WifiLock")
    }
    private val wifiListener = object : WifiHandler.Listener {
        override fun wifiEnabled() {
            currentState = ConnectionState.WifiEnabled
        }

        override fun wifiDisabled() {
            currentState = ConnectionState.WifiDisabled
        }

        override fun wifiConnected(networkInfo: NetworkInfo) {
            currentState = ConnectionState.WifiConnected(wifiHandler.connectedWifi)
        }

        override fun wifiDisconnected(networkInfo: NetworkInfo) {
            if (currentState is ConnectionState.TryToConnectToSsid || currentState is ConnectionState.WifiDisabled) {
                return
            }
            currentState = ConnectionState.WifiDisconnected
        }

        override fun onWifiScanFinished(sonyCameraScanResults: List<ScanResult>,
                                        configurations: List<WifiConfiguration>) {
            if (currentState !is ConnectionState.WifiScan) {
                return
            }
            currentState = ConnectionState.WifiScanFinished(sonyCameraScanResults, configurations)
        }
    }

    private val currentStateSubject = BehaviorSubject.createDefault<ConnectionState>(ConnectionState.Init).toSerialized()

    internal var currentState: ConnectionState = ConnectionState.Init
        set(newState) {
            logger.debug("State: {} --> {}", field, newState)
            field.stopAsyncTasks()
            field = newState
            field.process(this)
            currentStateSubject.onNext(field)
        }

    val states: Observable<ConnectionState> = currentStateSubject.hide()

    fun start() {
        logger.debug("----------- StateMachineConnection START -----------")
        currentState.process(this)
        wifiHandler.listener = wifiListener
        wifiLock.acquire()
    }

    fun stop() {
        logger.debug("----------- StateMachineConnection STOP -----------")
        currentState.stopAsyncTasks()
        wifiHandler.listener = null
        wifiLock.releaseSafely()
    }

    fun reset() {
        currentState = ConnectionState.Init
    }

    fun notifyWifiScanPermissionAccepted() {
        currentState = ConnectionState.Init
    }

    fun tryToConnectToNetworkId(networkId: Int) {
        wifiHandler.connectToNetworkId(networkId)
        currentState = ConnectionState.TryToConnectToSsid(networkId)
    }

    fun createNetwork(networkSSID: String, networkPassword: String) {
        val netId = wifiHandler.createIfNeededThenConnectToWifi(networkSSID, networkPassword)
        currentState = ConnectionState.TryToConnectToSsid(netId)
    }

    private fun WifiManager.WifiLock.releaseSafely() {
        if (isHeld) {
            try {
                release()
            } catch (e: Exception) {
                // http://errorprone.info/bugpattern/WakelockReleasedDangerously
                logger.warn("Error releasing wifi lock: {}", e.message)
            }
        }
    }

}


