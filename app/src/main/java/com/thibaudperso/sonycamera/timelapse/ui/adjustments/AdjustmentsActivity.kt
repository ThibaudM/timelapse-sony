package com.thibaudperso.sonycamera.timelapse.ui.adjustments

import android.app.Activity
import android.content.Intent
import com.thibaudperso.sonycamera.R
import com.thibaudperso.sonycamera.timelapse.ui.SingleFragmentActivity
import com.thibaudperso.sonycamera.timelapse.ui.connection.ConnectionFragment.Companion.EXTRA_EXIT
import io.reactivex.schedulers.Schedulers

class AdjustmentsActivity : SingleFragmentActivity<AdjustmentsFragment>() {

    override fun onBackPressed() {
        cameraAPI.closeConnection().subscribeOn(Schedulers.io()).subscribe()
        wifiHandler.disconnect()
        stateMachineConnection.reset()
        setResult(Activity.RESULT_CANCELED, Intent().putExtra(EXTRA_EXIT, true))
        finish()
    }

    override fun createFragment() = AdjustmentsFragment()

    override val guideTitle: String
        get() = getString(R.string.title_adjustments)
}
