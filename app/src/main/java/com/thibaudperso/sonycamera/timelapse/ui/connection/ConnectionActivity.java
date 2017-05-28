package com.thibaudperso.sonycamera.timelapse.ui.connection;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.thibaudperso.sonycamera.R;
import com.thibaudperso.sonycamera.timelapse.ui.SingleFragmentActivity;

public class ConnectionActivity extends SingleFragmentActivity {

    private ConnectionFragment mFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mNotConnectedMessage = false;
    }

    @Override
    protected Fragment createFragment() {
        return mFragment = new ConnectionFragment();
    }

    @Override
    protected String getGuideTitle() {
        return getString(R.string.title_connection);
    }

    @Override
    public void onBackPressed() {
        mApplication.getWifiHandler().disconnect();
        mStateMachineConnection.reset();
        finish();
    }
}
