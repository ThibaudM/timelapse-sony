package com.thibaudperso.sonycamera.timelapse.ui.settings;

import android.support.v4.app.Fragment;

import com.thibaudperso.sonycamera.R;
import com.thibaudperso.sonycamera.timelapse.ui.SingleFragmentActivity;

public class SettingsActivity extends SingleFragmentActivity {

    @Override
    protected Fragment createFragment() {
        return new SettingsFragment();
    }

    @Override
    protected String getGuideTitle() {
        return getString(R.string.title_settings);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.pop_enter, R.anim.pop_exit);
    }
}
