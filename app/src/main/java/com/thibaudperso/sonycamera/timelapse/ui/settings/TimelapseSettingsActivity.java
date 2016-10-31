package com.thibaudperso.sonycamera.timelapse.ui.settings;

import android.content.DialogInterface;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;

import com.thibaudperso.sonycamera.R;
import com.thibaudperso.sonycamera.timelapse.ui.SingleFragmentActivity;

/**
 * Created by thibaud on 18/02/16.
 */
public class TimelapseSettingsActivity extends SingleFragmentActivity {

    @Override
    protected Fragment createFragment() {
        return new TimelapseSettingsFragment();
    }

    @Override
    protected String getGuideTitle() {
        return getString(R.string.guidebar_title_timelapse_settings);
    }

    @Override
    public void onBackPressed() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.alert_disconnect_camera_title)
                .setMessage(R.string.alert_disconnect_camera_message)
                .setPositiveButton(R.string.alert_disconnect_camera_yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mApplication.getWifiHandler().disconnect();
                        TimelapseSettingsActivity.super.onBackPressed();
                    }
                })
                .setNegativeButton(R.string.alert_disconnect_camera_no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Do nothing
                    }
                });

        builder.create().show();
    }

}
