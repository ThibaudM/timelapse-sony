package com.thibaudperso.sonycamera.timelapse.ui.processing

import android.content.Intent
import android.os.Bundle
import com.thibaudperso.sonycamera.R
import com.thibaudperso.sonycamera.timelapse.ui.SingleFragmentActivity
import mu.KLogging

class ProcessingActivity : SingleFragmentActivity<ProcessingFragment>() {

    companion object : KLogging() {
        const val ACTION_STOP = "stop"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableStateMachineConnection = false
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        if (ACTION_STOP == intent.action) {
            fragment.askToStopProcessing()
        }
    }

    override fun onBackPressed() {
        if (fragment.isFinished) {
            super.onBackPressed()
            return
        }

        fragment.askToStopProcessing { super@ProcessingActivity.onBackPressed() }
    }

    override fun createFragment() = ProcessingFragment()

    override val guideTitle: String
        get() = getString(R.string.title_processing)

}
