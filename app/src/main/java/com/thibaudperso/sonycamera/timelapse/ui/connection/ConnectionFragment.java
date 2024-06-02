package com.thibaudperso.sonycamera.timelapse.ui.connection;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.thibaudperso.sonycamera.R;
import com.thibaudperso.sonycamera.sdk.model.Device;
import com.thibaudperso.sonycamera.timelapse.TimelapseApplication;
import com.thibaudperso.sonycamera.timelapse.control.DeviceManager;
import com.thibaudperso.sonycamera.timelapse.control.connection.NFCHandler;
import com.thibaudperso.sonycamera.timelapse.control.connection.StateMachineConnection;
import com.thibaudperso.sonycamera.timelapse.ui.adjustments.AdjustmentsActivity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.thibaudperso.sonycamera.timelapse.Constants.PREF_AUTOMATIC_CONTINUE;
import static com.thibaudperso.sonycamera.timelapse.control.connection.StateMachineConnection.State.BAD_API_ACCESS;
import static com.thibaudperso.sonycamera.timelapse.control.connection.StateMachineConnection.State.CHECK_API;
import static com.thibaudperso.sonycamera.timelapse.control.connection.StateMachineConnection.State.GOOD_API_ACCESS;
import static com.thibaudperso.sonycamera.timelapse.control.connection.StateMachineConnection.State.WIFI_DISABLED;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

public class ConnectionFragment extends Fragment {

    private final static int ADJUSTMENTS_ACTIVITY_RESULT = 0x1;
    private static final int PERMISSIONS_REQUEST_COARSE_LOCATION = 1;
    public static final String EXTRA_EXIT = "exit";

    private TimelapseApplication mApplication;

    private DeviceManager mDeviceManager;
    private StateMachineConnection mStateMachineConnection;

    private NfcAdapter mNfcAdapter;

    private AlertDialog mAlertDialogChooseNetworkConnection,
            mAlertDialogAskForPassword,
            mAlertDialogChooseNetworkCreation;

    private Spinner mCameraSpinner;
    private ArrayAdapter<Device> mAdapter;

    private ImageView mConnectionInfoWifiEnabled, mConnectionInfoNetworkState,
            mConnectionInfoAPIState;
    private ProgressBar mConnectionInfoWifiEnabledProgress, mConnectionInfoNetworkStateProgress,
            mConnectionInfoAPIStateProgress;
    private ImageView mConnectionDeviceListUpdateButton;
    private ProgressBar mConnectionDeviceListUpdateProgress;

    private boolean mAutomaticContinue = true;
    private boolean mSkipNextState = false;

    private CheckBox mConnectionAutomaticCheckbox;
    private FloatingActionButton mConnectionContinueButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mApplication = (TimelapseApplication) getActivity().getApplication();

        mDeviceManager = mApplication.getDeviceManager();
        mStateMachineConnection = mApplication.getStateMachineConnection();

        mNfcAdapter = NfcAdapter.getDefaultAdapter(getContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View viewResult = inflater.inflate(R.layout.fragment_connection, container, false);

        mConnectionDeviceListUpdateButton = viewResult.findViewById(R.id.connection_camera_list_update);
        mConnectionDeviceListUpdateProgress = viewResult.findViewById(R.id.connection_camera_list_update_progress);
        mConnectionInfoWifiEnabled = viewResult.findViewById(R.id.connection_info_wifi_enabled_icon);
        mConnectionInfoNetworkState = viewResult.findViewById(R.id.connection_info_network_state);
        mConnectionInfoAPIState = viewResult.findViewById(R.id.connection_info_api_state);
        mConnectionInfoWifiEnabledProgress = viewResult.findViewById(R.id.connection_info_wifi_enabled_progress);
        mConnectionInfoNetworkStateProgress = viewResult.findViewById(R.id.connection_info_network_state_progress);
        mConnectionInfoAPIStateProgress = viewResult.findViewById(R.id.connection_info_api_state_progress);
        ((TextView) viewResult.findViewById(R.id.connection_info_message)).setText(
                Html.fromHtml(getString(R.string.connection_information_message)));


        /*
         * Handle Camera spinner
         */
        mCameraSpinner = viewResult.findViewById(R.id.connection_camera_spinner);

        mAdapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_list_item_1, mDeviceManager.getDevices());
        mCameraSpinner.setAdapter(mAdapter);

        updateList();

        // http://stackoverflow.com/a/9375624/2239938
        mCameraSpinner.post(new Runnable() {
            @Override
            public void run() {
                mCameraSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(AdapterView<?> arg0, View view,
                                               int position, long id) {
                        mDeviceManager.setSelectedDevice((Device) mCameraSpinner.getItemAtPosition(position));
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> arg0) {
                    }
                });
            }
        });

        viewResult.findViewById(R.id.connection_camera_list_update).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mConnectionDeviceListUpdateButton.setVisibility(View.GONE);
                mConnectionDeviceListUpdateProgress.setVisibility(View.VISIBLE);
                mDeviceManager.getLastListOfDevicesFromInternet(new DeviceManager.Listener() {
                    @Override
                    public void onDevicesListChanged(List<Device> devices) {
                        mAdapter.notifyDataSetChanged();
                        updateList();

                        if(getContext() != null) {
                            Toast.makeText(getContext(), R.string.connection_camera_list_updated,
                                    Toast.LENGTH_SHORT).show();
                        }

                        if (mConnectionDeviceListUpdateButton != null &&
                                mConnectionDeviceListUpdateProgress != null) {
                            mConnectionDeviceListUpdateButton.setVisibility(View.VISIBLE);
                            mConnectionDeviceListUpdateProgress.setVisibility(View.GONE);
                        }
                    }
                });
            }
        });


        mConnectionAutomaticCheckbox = viewResult.findViewById(R.id.connection_automatic_checkbox);
        mConnectionAutomaticCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                final SharedPreferences.Editor editor = PreferenceManager
                        .getDefaultSharedPreferences(getContext()).edit();
                editor.putBoolean(PREF_AUTOMATIC_CONTINUE, isChecked);
                editor.apply();
            }
        });
        mConnectionContinueButton = viewResult.findViewById(R.id.connection_settings_button);
        mConnectionContinueButton.hide();
        mConnectionContinueButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                nextStep();
            }
        });

        initConnectionInfo();

        return viewResult;
    }

    private void updateList() {
        mAdapter.sort(Device.COMPARE_BY_DEVICEMODEL);

        int defaultPosition = mAdapter.getPosition(mDeviceManager.getSelectedDevice());
        mCameraSpinner.setSelection(defaultPosition, false);
    }

    @Override
    public void onResume() {
        super.onResume();

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mApplication);
        mAutomaticContinue = preferences.getBoolean(PREF_AUTOMATIC_CONTINUE, mAutomaticContinue);
        mConnectionAutomaticCheckbox.setChecked(mAutomaticContinue);


        initConnectionInfo();

        updateUI(mStateMachineConnection.getCurrentState());

        mStateMachineConnection.addListener(mStateMachineListener);

        if (mNfcAdapter != null) {
            mNfcAdapter.enableForegroundDispatch(getActivity(),
                    NFCHandler.getPendingIntent(getActivity()),
                    NFCHandler.getIntentFilterArray(), NFCHandler.getTechListArray());
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mStateMachineConnection.removeListener(mStateMachineListener);

        if (mNfcAdapter != null) {
            mNfcAdapter.disableForegroundDispatch(getActivity());
        }
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mAlertDialogChooseNetworkConnection != null) {
            mAlertDialogChooseNetworkConnection.cancel();
        }

        if (mAlertDialogAskForPassword != null) {
            mAlertDialogAskForPassword.cancel();
        }

        if (mAlertDialogChooseNetworkCreation != null) {
            mAlertDialogChooseNetworkCreation.cancel();
        }
    }

    private StateMachineConnection.Listener
            mStateMachineListener = new StateMachineConnection.Listener() {
        @Override
        public void onNewState(StateMachineConnection.State previousState,
                               StateMachineConnection.State newState) {
            updateUI(newState);
        }
    };

    private void updateUI(StateMachineConnection.State newState) {

        if (mSkipNextState) {
            mSkipNextState = false;
            return;
        }

        if (Collections.singletonList(GOOD_API_ACCESS).contains(newState)) {
            apiOk();
        }

        if (Arrays.asList(GOOD_API_ACCESS, BAD_API_ACCESS, CHECK_API).contains(newState)) {
            wifiOk();
        }

        if (!Collections.singletonList(WIFI_DISABLED).contains(newState)) {
            wifiEnabledOk();
        }

        switch (newState) {

            case WIFI_ENABLED:
                wifiEnabledOk();
                break;

            case WIFI_DISABLED:
                wifiEnabledError();
                break;

            case WIFI_CONNECTED:
                wifiError();
                apiError();
                break;

            case WIFI_DISCONNECTED:
                wifiError();
                apiError();
                break;

            case SONY_WIFI:
                wifiOk();
                break;

            case CHECK_API:
                apiProgress();
                break;

            case NOT_SONY_WIFI:
                wifiError();
                break;

            case BAD_API_ACCESS:
                apiError();
                break;

            case GOOD_API_ACCESS:
                apiOk();
                stepFinished();
                break;

            case WIFI_SCAN:
                wifiProgress();
                break;

            case NO_WIFI_SCAN_PERMISSION:
                askForScanPermission();
                break;

            case MULTIPLE_SONY_CONF_DETECTED:
                selectNetworkForConnection(mStateMachineConnection.getWifiConfigurations());
                break;

            case ASK_PASSWORD_FOR_WIFI:
                askForNetworkPasswordThenConnect(mStateMachineConnection.getScanResults().get(0));
                break;

            case MULTIPLE_SONY_SCAN_DETECTED:
                selectNetworkForCreation(mStateMachineConnection.getScanResults());
                break;


        }
    }



	/*
     * Wifi
	 */

    private void askForScanPermission() {

        String permission = Manifest.permission.ACCESS_COARSE_LOCATION;

        if (!ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), permission)) {

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.connection_permission_message);
            builder.setPositiveButton(R.string.connection_permission_ok,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                    PERMISSIONS_REQUEST_COARSE_LOCATION);
                        }
                    });
            builder.setNegativeButton(R.string.connection_permission_cancel,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Toast.makeText(getContext(), R.string.connection_permission_cancel_message,
                                    Toast.LENGTH_LONG).show();
                        }
                    });

            builder.create().show();

        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSIONS_REQUEST_COARSE_LOCATION);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {

        if (requestCode == PERMISSIONS_REQUEST_COARSE_LOCATION
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            mStateMachineConnection.notifyWifiScanPermissionAccepted();

        } else {
            Toast.makeText(getContext(), R.string.connection_permission_cancel_message,
                    Toast.LENGTH_LONG).show();
        }

    }



    /*
     * Handle network prompts
	 */

    private void selectNetworkForConnection(final List<WifiConfiguration> wifiConfigurations) {

        final ListView listView = new ListView(getActivity());

        ListAdapter adapter = new ArrayAdapter<WifiConfiguration>(getActivity(),
                android.R.layout.simple_list_item_1, wifiConfigurations) {

            @NonNull
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {

                View view = super.getView(position, convertView, parent);
                TextView textView = view.findViewById(android.R.id.text1);
                WifiConfiguration network = getItem(position);
                if (network != null) {
                    textView.setText(network.SSID);
                }
                return textView;
            }
        };

        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, final View view,
                                    int position, long id) {

                WifiConfiguration wc = (WifiConfiguration) parent.getItemAtPosition(position);
                mStateMachineConnection.tryToConnectToNetworkId(wc.networkId);
            }
        });

        mAlertDialogChooseNetworkConnection = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.connection_choose_network)
                .setView(listView)
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        wifiError();
                    }
                })
                .setNegativeButton(R.string.connection_choose_network_cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                wifiError();
                            }
                        }).show();

    }

    private void askForNetworkPasswordThenConnect(final ScanResult scanResult) {

        final EditText input = new EditText(getActivity());

        mAlertDialogAskForPassword = new AlertDialog.Builder(getActivity())
                .setTitle(String.format(getString(R.string.connection_enter_password), scanResult.SSID))
                .setView(input)
                .setPositiveButton(R.string.connection_enter_password_ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                                String value = input.getText().toString();
                                mStateMachineConnection.createNetwork(scanResult.SSID, value);
                            }
                        })
                .setNegativeButton(R.string.connection_enter_password_cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // Do nothing.
                            }
                        }).show();

    }

    private void selectNetworkForCreation(final List<ScanResult> scanResults) {

        final ListView listView = new ListView(getActivity());

        ListAdapter adapter = new ArrayAdapter<ScanResult>(getActivity(),
                android.R.layout.simple_list_item_1, scanResults) {

            @NonNull
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {

                View view = super.getView(position, convertView, parent);
                TextView textView = view.findViewById(android.R.id.text1);
                textView.setText((getItem(position)).SSID);
                return textView;
            }
        };

        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, final View view,
                                    int position, long id) {

                ScanResult scanResult = (ScanResult) parent.getItemAtPosition(position);
                askForNetworkPasswordThenConnect(scanResult);
            }
        });

        mAlertDialogChooseNetworkCreation = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.connection_choose_network)
                .setView(listView)
                .setNegativeButton(R.string.connection_choose_network_cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // Do nothing.
                            }
                        }).show();

    }


    private void initConnectionInfo() {
        wifiEnabledError();
        wifiError();
        apiError();
        mConnectionContinueButton.hide();
    }


    private void wifiEnabledError() {
        mConnectionInfoWifiEnabledProgress.setVisibility(View.GONE);
        mConnectionInfoWifiEnabled.setVisibility(View.VISIBLE);
        mConnectionInfoWifiEnabled.setImageResource(R.drawable.error);
    }

    private void wifiEnabledOk() {
        mConnectionInfoWifiEnabledProgress.setVisibility(View.GONE);
        mConnectionInfoWifiEnabled.setVisibility(View.VISIBLE);
        mConnectionInfoWifiEnabled.setImageResource(R.drawable.ok);
    }

    private void wifiProgress() {
        mConnectionInfoNetworkState.setVisibility(View.GONE);
        mConnectionInfoNetworkStateProgress.setVisibility(View.VISIBLE);
    }

    private void wifiError() {
        mConnectionInfoNetworkStateProgress.setVisibility(View.GONE);
        mConnectionInfoNetworkState.setVisibility(View.VISIBLE);
        mConnectionInfoNetworkState.setImageResource(R.drawable.error);
    }

    private void wifiOk() {
        mConnectionInfoNetworkStateProgress.setVisibility(View.GONE);
        mConnectionInfoNetworkState.setVisibility(View.VISIBLE);
        mConnectionInfoNetworkState.setImageResource(R.drawable.ok);
    }


    private void apiProgress() {
        mConnectionInfoAPIState.setVisibility(View.GONE);
        mConnectionInfoAPIStateProgress.setVisibility(View.VISIBLE);
    }

    private void apiError() {
        mConnectionInfoAPIStateProgress.setVisibility(View.GONE);
        mConnectionInfoAPIState.setVisibility(View.VISIBLE);
        mConnectionInfoAPIState.setImageResource(R.drawable.error);
    }

    private void apiOk() {
        mConnectionInfoAPIStateProgress.setVisibility(View.GONE);
        mConnectionInfoAPIState.setVisibility(View.VISIBLE);
        mConnectionInfoAPIState.setImageResource(R.drawable.ok);
    }


    private void stepFinished() {
        if (mConnectionAutomaticCheckbox.isChecked()) {
            nextStep();
        } else {
            mConnectionContinueButton.show();
        }
    }

    private void nextStep() {
        ((TimelapseApplication) getActivity().getApplication()).getCameraAPI().initializeWS();
        startActivityForResult(new Intent(getContext(), AdjustmentsActivity.class),
                ADJUSTMENTS_ACTIVITY_RESULT);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ADJUSTMENTS_ACTIVITY_RESULT) {
            if (data != null && data.hasExtra(EXTRA_EXIT)
                    && data.getExtras().getBoolean(EXTRA_EXIT, false)) {
                getActivity().finish();
            }
        }

        // This variable is used as a workaround for the long asynchronous time of disconnection
        mSkipNextState = true;
    }

}
