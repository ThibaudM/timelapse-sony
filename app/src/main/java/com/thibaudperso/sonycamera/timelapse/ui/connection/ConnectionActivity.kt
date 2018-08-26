package com.thibaudperso.sonycamera.timelapse.ui.connection

import android.os.Bundle
import com.thibaudperso.sonycamera.R
import com.thibaudperso.sonycamera.timelapse.ui.SingleFragmentActivity
import io.reactivex.schedulers.Schedulers

class ConnectionActivity : SingleFragmentActivity<ConnectionFragment>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notConnectedMessage = false
    }

    override fun onBackPressed() {
        if (cameraAPI.initialized) {
            cameraAPI.closeConnection().subscribeOn(Schedulers.io()).subscribe()
        }
        wifiHandler.disconnect()
        stateMachineConnection.reset()
        finish()
    }

    override fun createFragment() = ConnectionFragment()

    override val guideTitle: String
        get() = getString(R.string.title_connection)
}
