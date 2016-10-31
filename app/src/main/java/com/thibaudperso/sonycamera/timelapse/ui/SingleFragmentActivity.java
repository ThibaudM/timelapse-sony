package com.thibaudperso.sonycamera.timelapse.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.thibaudperso.sonycamera.R;
import com.thibaudperso.sonycamera.timelapse.control.io.WifiHandler;
import com.thibaudperso.sonycamera.timelapse.TimelapseApplication;


/**
 * An activity with a single fragment
 * https://github.com/tkunstek/android-big-nerd-ranch/blob/master/20_CameraImage_CriminalIntent/src/com/bignerdranch/android/criminalintent/SingleFragmentActivity.java
 */
public abstract class SingleFragmentActivity extends AppCompatActivity {

    protected TimelapseApplication mApplication;
    protected WifiHandler mWifiHandler;

    protected abstract Fragment createFragment();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mApplication = ((TimelapseApplication) getApplication());
//        mWifiHandler = mApplication.getWifiHandler();

        setContentView(R.layout.activity_fragment);
        FragmentManager manager = getSupportFragmentManager();
        Fragment fragment = manager.findFragmentById(R.id.fragmentContainer);

        if (fragment == null) {
            fragment = createFragment();
            manager.beginTransaction()
                    .add(R.id.fragmentContainer, fragment)
                    .commit();
        }

        ((TextView)findViewById(R.id.guideTitle)).setText(getGuideTitle());
    }

    protected abstract String getGuideTitle();

    public void setGuideTitle(String title) {
        ((TextView)findViewById(R.id.guideTitle)).setText(title);
    }


    @Override
    protected void onResume() {
        super.onResume();
//        mWifiHandler.addListener(mWifiListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
//        mWifiHandler.removeListener(mWifiListener);
    }

//    private WifiHandler.Listener mWifiListener = new WifiHandler.Listener() {
//        @Override
//        public void onWifiConnecting(String ssid) {
//
//        }
//
//        @Override
//        public void onWifiConnected(String ssid) {
//
//            if(!(SingleFragmentActivity.this instanceof ConnectionActivity)) {
//                // Hide message wifi disconnected
//            }
//        }
//
//        @Override
//        public void onWifiDisconnected() {
//
//            if(!(SingleFragmentActivity.this instanceof ConnectionActivity)) {
//                // Show message wifi disconnected
//            }
//
//        }
//
//        @Override
//        public void onWifiStartScan() {
//
//        }
//
//        @Override
//        public void onWifiScanFinished(List<ScanResult> sonyCameraScanResults, List<WifiConfiguration> sonyCameraWifiConfiguration) {
//
//        }
//    };
}