package com.thibaudperso.sonycamera.timelapse.control.connection

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.wifi.ScanResult
import android.net.wifi.SupplicantState
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiInfo
import android.os.Build
import androidx.core.content.ContextCompat
import com.thibaudperso.sonycamera.sdk.SonyDevice
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import mu.KLogging
import java.util.concurrent.TimeUnit

sealed class ConnectionState {
    companion object : KLogging();

    protected val disposables = CompositeDisposable()

    abstract fun process(sm: ConnectionStateMachine)

    open fun stopAsyncTasks() {
        disposables.clear()
    }

    final override fun toString(): String = javaClass.simpleName

    object Init : ConnectionState() {
        override fun process(sm: ConnectionStateMachine) {
            val isWifiEnabled = sm.wifiHandler.isEnabled
            if (isWifiEnabled) {
                sm.currentState = WifiEnabled
            } else {
                sm.currentState = WifiDisabled
            }
        }
    }

    object WifiEnabled : ConnectionState() {
        override fun process(sm: ConnectionStateMachine) {
            val connectedWifi = sm.wifiHandler.connectedWifi
            if (connectedWifi.supplicantState != SupplicantState.DISCONNECTED) {
                sm.currentState = WifiConnected(connectedWifi)
            } else {
                sm.currentState = WifiDisconnected
            }
        }
    }

    object WifiDisabled : ConnectionState() {
        override fun process(sm: ConnectionStateMachine) {
            // TODO: notify wifi disabled
        }
    }

    data class WifiConnected(val connectedWifi: WifiInfo) : ConnectionState() {
        override fun process(sm: ConnectionStateMachine) {
            val ssid = WifiHandler.parseSSID(connectedWifi.ssid)
            if (WifiHandler.isSonyCameraSSID(ssid)) {
                sm.currentState = SonyWifi(connectedWifi)
            } else {
                sm.currentState = NotSonyWifi
            }
        }

        override fun stopAsyncTasks() = Unit
    }

    object WifiDisconnected : ConnectionState() {
        override fun process(sm: ConnectionStateMachine) {
            sm.currentState = WifiScan
        }
    }

    data class SonyWifi(val wifiInfo: WifiInfo) : ConnectionState() {
        override fun process(sm: ConnectionStateMachine) {
            // Workaround when there is a data connection more than the wifi one
            // http://stackoverflow.com/questions/33237074/request-over-wifi-on-android-m
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val connectivityManager = sm.application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                for (net in connectivityManager.allNetworks) {
                    val netInfo = connectivityManager.getNetworkInfo(net)
                    if (netInfo != null
                            && netInfo.type == ConnectivityManager.TYPE_WIFI
                            && netInfo.extraInfo != null
                            && netInfo.extraInfo == wifiInfo.ssid) {
                        connectivityManager.bindProcessToNetwork(net)
                        break
                    }
                }
            }

            disposables += sm.cameraDetectionService.discoverCameras
                    .take(1L)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnNext { sonyDevice ->
                        logger.warn("Discovered: {}", sonyDevice)
                        sm.currentState = CheckApi(sonyDevice, 1)
                    }
                    .subscribe()
        }
    }

    object NotSonyWifi : ConnectionState() {
        override fun process(sm: ConnectionStateMachine) {
            sm.currentState = WifiScan
        }
    }

    data class CheckApi(val sonyDevice: SonyDevice, val attempt: Int = 1) : ConnectionState() {
        override fun process(sm: ConnectionStateMachine) {
            sm.cameraAPI.device = sonyDevice
            disposables += sm.cameraAPI.getVersions()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ versions ->
                        logger.debug { "Api check success, API versions: $versions" }
                        sm.currentState = GoodApiAccess(sonyDevice)
                    }, { throwable ->
                        logger.warn(throwable) { "Error during api check: ${throwable.message}" }
                        sm.currentState = BadApiAccess(sonyDevice, attempt)
                    })
        }
    }

    data class GoodApiAccess(val sonyDevice: SonyDevice) : ConnectionState() {
        override fun process(sm: ConnectionStateMachine) = Unit
    }

    data class BadApiAccess(val sonyDevice: SonyDevice, val attempt: Int) : ConnectionState() {
        override fun process(sm: ConnectionStateMachine) {
            disposables += Completable.complete()
                    .delay(1000L, TimeUnit.MILLISECONDS)
                    .subscribe {
                        sm.currentState = CheckApi(sonyDevice, attempt + 1)
                    }
        }
    }

    object WifiScan : ConnectionState() {
        override fun process(sm: ConnectionStateMachine) {
            if (ContextCompat.checkSelfPermission(sm.application, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                sm.wifiHandler.startScan()
            } else {
                sm.currentState = NoWifiScanPermission
            }
        }
    }

    object NoWifiScanPermission : ConnectionState() {
        override fun process(sm: ConnectionStateMachine) = Unit
    }

    data class WifiScanFinished(val scanResults: List<ScanResult>,
                                val knownWifiConfigurations: List<WifiConfiguration>) : ConnectionState() {
        override fun process(sm: ConnectionStateMachine) {
            /*
             * No Sony Camera network found in scan
             */
            if (scanResults.isEmpty()) {
                disposables += Completable.complete()
                        .delay(2000L, TimeUnit.MILLISECONDS)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe { sm.currentState = WifiScan }
            } else if (knownWifiConfigurations.isEmpty() && scanResults.size == 1) {
                /*
                 * No Sony Camera network registered on this phone but we found only one in scan
                 */
                sm.currentState = AskPasswordForWifi(scanResults)
            } else if (knownWifiConfigurations.isEmpty()) {
                /*
                * No Sony Camera network registered on this phone but we found more than one in scan
                */
                sm.currentState = MultipleSonyScanDetected(scanResults)
            } else if (knownWifiConfigurations.size == 1) {
                /*
                 * There is only one Sony Camera known network connected
                 */
                val networkId = knownWifiConfigurations[0].networkId
                sm.currentState = TryToConnectToSsid(networkId)
            } else {
                /*
                 * There is more than one Sony Camera known network connected
                 */
                sm.currentState = MultipleSonyConfDetected(knownWifiConfigurations)
            }
        }
    }

    data class MultipleSonyConfDetected(val knownWifiConfigurations: List<WifiConfiguration>) : ConnectionState() {
        override fun process(sm: ConnectionStateMachine) = Unit
    }

    data class MultipleSonyScanDetected(val scanResults: List<ScanResult>) : ConnectionState() {
        override fun process(sm: ConnectionStateMachine) = Unit
    }

    data class AskPasswordForWifi(val scanResults: List<ScanResult>) : ConnectionState() {
        override fun process(sm: ConnectionStateMachine) = Unit
    }

    data class TryToConnectToSsid(val networkId: Int) : ConnectionState() {
        override fun process(sm: ConnectionStateMachine) {
            if (!sm.wifiHandler.connectToNetworkId(networkId)) {
                sm.currentState = WifiScan
            }
        }
    }

}