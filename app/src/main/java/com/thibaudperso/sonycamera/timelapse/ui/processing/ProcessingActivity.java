package com.thibaudperso.sonycamera.timelapse.ui.processing;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.thibaudperso.sonycamera.R;
import com.thibaudperso.sonycamera.timelapse.ui.SingleFragmentActivity;


public class ProcessingActivity extends SingleFragmentActivity {

    public static final String ACTION_STOP = "stop";

    private ProcessingFragment mFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mEnableStateMachineConnection = false;
    }

    @Override
    protected Fragment createFragment() {
        mFragment = new ProcessingFragment();
        return mFragment;
    }

    @Override
    protected String getGuideTitle() {
        return getString(R.string.title_processing);
    }

    @Override
    public void onBackPressed() {

        if(mFragment == null || mFragment.isFinished()) {
            super.onBackPressed();
            return;
        }

        mFragment.askToStopProcessing(new ProcessingFragment.TimelapseStopListener() {
            @Override
            public void onStop() {
                ProcessingActivity.super.onBackPressed();
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (ACTION_STOP.equals(intent.getAction())) {
            mFragment.askToStopProcessing();
        }
    }
}
