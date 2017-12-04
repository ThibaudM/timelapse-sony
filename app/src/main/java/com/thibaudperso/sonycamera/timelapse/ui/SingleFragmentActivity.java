package com.thibaudperso.sonycamera.timelapse.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.FrameLayout;

import com.thibaudperso.sonycamera.R;
import com.thibaudperso.sonycamera.timelapse.TimelapseApplication;
import com.thibaudperso.sonycamera.timelapse.control.Logger;
import com.thibaudperso.sonycamera.timelapse.control.connection.NFCHandler;
import com.thibaudperso.sonycamera.timelapse.control.connection.StateMachineConnection;


/**
 * An activity with a single fragment
 * https://github.com/tkunstek/android-big-nerd-ranch/blob/master/20_CameraImage_CriminalIntent/src/com/bignerdranch/android/criminalintent/SingleFragmentActivity.java
 */
public abstract class SingleFragmentActivity extends AppCompatActivity {

    protected TimelapseApplication mApplication;
    protected StateMachineConnection mStateMachineConnection;

    private Snackbar mSnackBarConnectionLost;
    protected boolean mEnableStateMachineConnection = true;
    protected boolean mNotConnectedMessage = true;

    protected abstract Fragment createFragment();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mApplication = ((TimelapseApplication) getApplication());
        mStateMachineConnection = mApplication.getStateMachineConnection();

        setContentView(R.layout.activity_fragment);
        FragmentManager manager = getSupportFragmentManager();
        Fragment fragment = manager.findFragmentById(R.id.fragment_container);

        if (fragment == null) {
            fragment = createFragment();
            manager.beginTransaction()
                    .add(R.id.fragment_container, fragment)
                    .commit();
        }

        setTitle(getString(R.string.app_name) + " - " + getGuideTitle());
    }

    @Override
    protected void onStart() {
        super.onStart();

        FrameLayout frameLayout = findViewById(R.id.fragment_container);
        if (frameLayout != null && frameLayout.getChildCount() > 0) {
            mSnackBarConnectionLost = Snackbar.make(frameLayout.getChildAt(0),
                    getString(R.string.connection_with_camera_lost), Snackbar.LENGTH_INDEFINITE);
        } else {
            mNotConnectedMessage = false;
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mNotConnectedMessage)
            mStateMachineConnection.addListener(mStateMachineConnectionListener);

        if (mEnableStateMachineConnection)
            mStateMachineConnection.start();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mEnableStateMachineConnection)
            mStateMachineConnection.stop();

        if (mNotConnectedMessage)
            mStateMachineConnection.removeListener(mStateMachineConnectionListener);

    }


    protected abstract String getGuideTitle();

    private StateMachineConnection.Listener
            mStateMachineConnectionListener = new StateMachineConnection.Listener() {
        @Override
        public void onNewState(StateMachineConnection.State previousState,
                               StateMachineConnection.State newState) {

            if (newState != StateMachineConnection.State.GOOD_API_ACCESS) {
                mSnackBarConnectionLost.show();
            } else {
                mSnackBarConnectionLost.dismiss();
            }
        }
    };

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        try {
            Pair<String, String> loginPwd = NFCHandler.parseIntent(intent);
            if (loginPwd == null) {
                return;
            }
            mApplication.getWifiHandler()
                    .createIfNeededThenConnectToWifi(loginPwd.first, loginPwd.second);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.debug_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.troubleshooting) {
            Logger.sendEmail(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}