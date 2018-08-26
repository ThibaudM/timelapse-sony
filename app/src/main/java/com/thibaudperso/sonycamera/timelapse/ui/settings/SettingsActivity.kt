package com.thibaudperso.sonycamera.timelapse.ui.settings

import com.thibaudperso.sonycamera.R
import com.thibaudperso.sonycamera.timelapse.ui.SingleFragmentActivity

class SettingsActivity : SingleFragmentActivity<SettingsFragment>() {

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.pop_enter, R.anim.pop_exit)
    }

    override fun createFragment() = SettingsFragment()

    override val guideTitle: String
        get() = getString(R.string.title_settings)
}
