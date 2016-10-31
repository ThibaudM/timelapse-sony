package com.thibaudperso.sonycamera.timelapse.ui.settings;

import android.support.v4.app.Fragment;

import com.thibaudperso.sonycamera.R;
import com.thibaudperso.sonycamera.timelapse.ui.SingleFragmentActivity;

/**
 * Created by thibaud on 18/02/16.
 */
public class CameraSettingsActivity extends SingleFragmentActivity {

    @Override
    protected Fragment createFragment() {
        return new CameraSettingsFragment();
    }

    @Override
    protected String getGuideTitle() {
        return getString(R.string.guidebar_title_camera_settings);
    }
}
