package com.thibaudperso.sonycamera.timelapse.ui.connection

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiConfiguration
import android.nfc.NfcAdapter
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.text.parseAsHtml
import com.thibaudperso.sonycamera.R
import com.thibaudperso.sonycamera.sdk.CameraAPI
import com.thibaudperso.sonycamera.sdk.SonyDevice
import com.thibaudperso.sonycamera.timelapse.BaseFragment
import com.thibaudperso.sonycamera.timelapse.Constants.PREF_AUTOMATIC_CONTINUE
import com.thibaudperso.sonycamera.timelapse.control.connection.ConnectionState
import com.thibaudperso.sonycamera.timelapse.control.connection.ConnectionStateMachine
import com.thibaudperso.sonycamera.timelapse.control.connection.NFCHandler
import com.thibaudperso.sonycamera.timelapse.ui.adjustments.AdjustmentsActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_connection.*
import mu.KLogging
import javax.inject.Inject

class ConnectionFragment : BaseFragment() {

    companion object : KLogging() {
        private const val ADJUSTMENTS_ACTIVITY_RESULT = 0x1
        private const val PERMISSIONS_REQUEST_COARSE_LOCATION = 1
        const val EXTRA_EXIT = "exit"
    }

    @Inject lateinit var connectionStateMachine: ConnectionStateMachine
    @Inject lateinit var cameraAPI: CameraAPI

    private val nfcAdapter: NfcAdapter? by lazy { NfcAdapter.getDefaultAdapter(requireContext()) }

    private var alertDialogChooseNetworkConnection: AlertDialog? = null
    private var alertDialogAskForPassword: AlertDialog? = null
    private var alertDialogChooseNetworkCreation: AlertDialog? = null
    private var automaticContinue = true
    private var stateSubscription: Disposable? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_connection, container, false)

    @SuppressLint("RestrictedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        connection_info_message.text = getString(R.string.connection_information_message).parseAsHtml()

        connection_automatic_checkbox.setOnCheckedChangeListener { _, isChecked ->
            PreferenceManager.getDefaultSharedPreferences(context).edit().apply {
                putBoolean(PREF_AUTOMATIC_CONTINUE, isChecked)
            }.apply()
        }
        connection_settings_button.visibility = View.GONE
        connection_settings_button.setOnClickListener { nextStep() }
        initConnectionInfo()
    }

    override fun onResume() {
        super.onResume()

        val preferences = PreferenceManager.getDefaultSharedPreferences(requireActivity().application)
        automaticContinue = preferences.getBoolean(PREF_AUTOMATIC_CONTINUE, automaticContinue)
        connection_automatic_checkbox.isChecked = automaticContinue

        stateSubscription = connectionStateMachine.states
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(this@ConnectionFragment::updateUI)
                .subscribe()

        nfcAdapter?.enableForegroundDispatch(activity, NFCHandler.getPendingIntent(requireActivity()),
                NFCHandler.intentFilterArray, NFCHandler.techListArray)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == PERMISSIONS_REQUEST_COARSE_LOCATION && grantResults.isNotEmpty()
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            connectionStateMachine.notifyWifiScanPermissionAccepted()
        } else {
            Toast.makeText(context, R.string.connection_permission_cancel_message,
                    Toast.LENGTH_LONG).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ADJUSTMENTS_ACTIVITY_RESULT && data?.extras?.getBoolean(EXTRA_EXIT) == true) {
            requireActivity().finish()
        }
    }

    override fun onPause() {
        super.onPause()
        stateSubscription?.dispose()
        stateSubscription = null
        nfcAdapter?.disableForegroundDispatch(activity)
    }

    override fun onDestroy() {
        super.onDestroy()
        alertDialogChooseNetworkConnection?.cancel()
        alertDialogChooseNetworkConnection = null
        alertDialogAskForPassword?.cancel()
        alertDialogAskForPassword = null
        alertDialogChooseNetworkCreation?.cancel()
        alertDialogChooseNetworkCreation = null
    }

    private fun updateUI(newState: ConnectionState) = when (newState) {
        ConnectionState.Init -> {
            wifiEnabledError()
            wifiConnectionError()
            apiError()
        }
        ConnectionState.WifiEnabled -> {
            wifiEnabledOk()
            wifiConnectionError()
            apiError()
        }
        ConnectionState.WifiDisabled -> {
            wifiEnabledError()
            wifiConnectionError()
            apiError()
        }
        is ConnectionState.WifiConnected -> {
            wifiEnabledOk()
            wifiConnectionProgress()
            apiError()
        }
        ConnectionState.WifiDisconnected -> {
            wifiEnabledOk()
            wifiConnectionProgress()
            apiError()
        }
        is ConnectionState.SonyWifi -> {
            wifiEnabledOk()
            wifiConnectionOk()
            apiProgress()
        }
        ConnectionState.NotSonyWifi -> {
            wifiEnabledOk()
            wifiConnectionError()
            apiError()
        }
        is ConnectionState.CheckApi -> {
            wifiEnabledOk()
            wifiConnectionOk()
            apiProgress()
        }
        is ConnectionState.GoodApiAccess -> {
            wifiEnabledOk()
            wifiConnectionOk()
            apiOk()
            showDeviceInfo(newState.sonyDevice)
            stepFinished()
        }
        is ConnectionState.BadApiAccess -> {
            wifiEnabledOk()
            wifiConnectionOk()
            apiError()
        }
        ConnectionState.WifiScan -> {
            wifiEnabledOk()
            wifiConnectionProgress()
            apiError()
        }
        ConnectionState.NoWifiScanPermission -> {
            wifiEnabledOk()
            wifiConnectionError()
            apiError()
            askForScanPermission()
        }
        is ConnectionState.WifiScanFinished -> {
            wifiEnabledOk()
            wifiConnectionProgress()
            apiError()
        }
        is ConnectionState.MultipleSonyConfDetected -> selectNetworkForConnection(newState.knownWifiConfigurations)
        is ConnectionState.MultipleSonyScanDetected -> selectNetworkForCreation(newState.scanResults)
        is ConnectionState.AskPasswordForWifi -> askForNetworkPasswordThenConnect(newState.scanResults[0])
        is ConnectionState.TryToConnectToSsid -> Unit
    }

    /*
     * Wifi
     */
    private fun askForScanPermission() {

        val permission = Manifest.permission.ACCESS_COARSE_LOCATION

        if (!ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), permission)) {

            val builder = AlertDialog.Builder(activity)
            builder.setMessage(R.string.connection_permission_message)
            builder.setPositiveButton(R.string.connection_permission_ok) { _, _ ->
                requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                        PERMISSIONS_REQUEST_COARSE_LOCATION)
            }
            builder.setNegativeButton(R.string.connection_permission_cancel) { _, _ ->
                Toast.makeText(context, R.string.connection_permission_cancel_message, Toast.LENGTH_LONG).show()
            }

            builder.create().show()

        } else {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                    PERMISSIONS_REQUEST_COARSE_LOCATION)
        }
    }

    /*
     * Handle network prompts
     */

    private fun selectNetworkForConnection(wifiConfigurations: List<WifiConfiguration>) {

        val listView = ListView(activity)

        val adapter = object : ArrayAdapter<WifiConfiguration>(requireContext(),
                android.R.layout.simple_list_item_1, wifiConfigurations) {

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

                val view = super.getView(position, convertView, parent)
                val textView = view.findViewById<TextView>(android.R.id.text1)
                val network = getItem(position)
                if (network != null) {
                    textView.text = network.SSID
                }
                return textView
            }
        }

        listView.adapter = adapter
        listView.setOnItemClickListener { parent, _, position, _ ->
            val wc = parent.getItemAtPosition(position) as WifiConfiguration
            connectionStateMachine.tryToConnectToNetworkId(wc.networkId)
        }

        alertDialogChooseNetworkConnection = AlertDialog.Builder(activity)
                .setTitle(R.string.connection_choose_network)
                .setView(listView)
                .setOnCancelListener { wifiConnectionError() }
                .setNegativeButton(R.string.connection_choose_network_cancel) { _, _ -> wifiConnectionError() }.show()

    }

    private fun askForNetworkPasswordThenConnect(scanResult: ScanResult) {
        val input = EditText(activity)
        alertDialogAskForPassword?.dismiss()
        alertDialogAskForPassword = AlertDialog.Builder(activity)
                .setTitle(String.format(getString(R.string.connection_enter_password), scanResult.SSID))
                .setView(input)
                .setPositiveButton(R.string.connection_enter_password_ok) { _, _ ->
                    val value = input.text.toString()
                    connectionStateMachine.createNetwork(scanResult.SSID, value)
                }
                .setNegativeButton(R.string.connection_enter_password_cancel) { _, _ ->
                    // Do nothing.
                }.show()

    }

    private fun selectNetworkForCreation(scanResults: List<ScanResult>) {
        val listView = ListView(requireActivity())

        val adapter = object : ArrayAdapter<ScanResult>(requireContext(), android.R.layout.simple_list_item_1, scanResults) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

                val view = super.getView(position, convertView, parent)
                val textView = view.findViewById<TextView>(android.R.id.text1)
                textView.text = getItem(position).SSID
                return textView
            }
        }

        listView.adapter = adapter
        listView.setOnItemClickListener { parent, _, position, _ ->
            val scanResult = parent.getItemAtPosition(position) as ScanResult
            askForNetworkPasswordThenConnect(scanResult)
        }

        alertDialogChooseNetworkCreation = AlertDialog.Builder(activity)
                .setTitle(R.string.connection_choose_network)
                .setView(listView)
                .setNegativeButton(R.string.connection_choose_network_cancel) { _, _ ->
                    // Do nothing.
                }.show()

    }

    @SuppressLint("RestrictedApi")
    private fun initConnectionInfo() {
        wifiEnabledError()
        wifiConnectionError()
        apiError()
        txt_device_info_name.text = ""
        connection_settings_button.visibility = View.GONE
    }

    private fun wifiEnabledError() {
        connection_info_wifi_enabled_progress.visibility = View.GONE
        connection_info_wifi_enabled_icon.visibility = View.VISIBLE
        connection_info_wifi_enabled_icon.setImageResource(R.drawable.error)
    }

    private fun wifiEnabledOk() {
        connection_info_wifi_enabled_progress.visibility = View.GONE
        connection_info_wifi_enabled_icon.visibility = View.VISIBLE
        connection_info_wifi_enabled_icon.setImageResource(R.drawable.ok)
    }

    private fun wifiConnectionProgress() {
        connection_info_network_state.visibility = View.GONE
        connection_info_network_state_progress.visibility = View.VISIBLE
    }

    private fun wifiConnectionError() {
        connection_info_network_state_progress.visibility = View.GONE
        connection_info_network_state.visibility = View.VISIBLE
        connection_info_network_state.setImageResource(R.drawable.error)
    }

    private fun wifiConnectionOk() {
        connection_info_network_state_progress.visibility = View.GONE
        connection_info_network_state.visibility = View.VISIBLE
        connection_info_network_state.setImageResource(R.drawable.ok)
    }

    private fun apiProgress() {
        connection_info_api_state.visibility = View.GONE
        connection_info_api_state_progress.visibility = View.VISIBLE
    }

    private fun apiError() {
        connection_info_api_state_progress.visibility = View.GONE
        connection_info_api_state.visibility = View.VISIBLE
        connection_info_api_state.setImageResource(R.drawable.error)
    }

    private fun apiOk() {
        connection_info_api_state_progress.visibility = View.GONE
        connection_info_api_state.visibility = View.VISIBLE
        connection_info_api_state.setImageResource(R.drawable.ok)
    }

    private fun showDeviceInfo(sonyDevice: SonyDevice) {
        txt_device_info_name.text = sonyDevice.friendlyName
    }

    @SuppressLint("RestrictedApi")
    private fun stepFinished() {
        if (connection_automatic_checkbox.isChecked) {
            nextStep()
        } else {
            connection_settings_button.visibility = View.VISIBLE
        }
    }

    private fun nextStep() {
        cameraAPI.initializeWS().subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe()
        startActivityForResult(Intent(context, AdjustmentsActivity::class.java), ADJUSTMENTS_ACTIVITY_RESULT)
    }

}
