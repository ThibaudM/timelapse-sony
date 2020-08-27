package com.thibaudperso.sonycamera.timelapse.ui.adjustments;

import android.content.Intent;

import androidx.fragment.app.Fragment;

import com.thibaudperso.sonycamera.R;
import com.thibaudperso.sonycamera.timelapse.ui.SingleFragmentActivity;

import static com.thibaudperso.sonycamera.timelapse.ui.connection.ConnectionFragment.EXTRA_EXIT;

public class AdjustmentsActivity extends SingleFragmentActivity {

    @Override
    protected Fragment createFragment() {
        return new AdjustmentsFragment();
    }

    @Override
    protected String getGuideTitle() {
        return getString(R.string.title_adjustments);
    }

    @Override
    public void onBackPressed() {
        mApplication.getCameraAPI().closeConnection();
        mApplication.getWifiHandler().disconnect();
        mStateMachineConnection.reset();
        setResult(RESULT_CANCELED, new Intent().putExtra(EXTRA_EXIT, true));
        finish();
    }
}
