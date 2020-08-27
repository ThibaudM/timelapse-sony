package com.thibaudperso.sonycamera.timelapse.ui.connection;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import com.thibaudperso.sonycamera.R;
import com.thibaudperso.sonycamera.timelapse.ui.SingleFragmentActivity;

public class ConnectionActivity extends SingleFragmentActivity {


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mNotConnectedMessage = false;
    }

    @Override
    protected Fragment createFragment() {
        return new ConnectionFragment();
    }

    @Override
    protected String getGuideTitle() {
        return getString(R.string.title_connection);
    }

    @Override
    public void onBackPressed() {
        mApplication.getCameraAPI().closeConnection();
        mApplication.getWifiHandler().disconnect();
        mStateMachineConnection.reset();
        finish();
    }
}
