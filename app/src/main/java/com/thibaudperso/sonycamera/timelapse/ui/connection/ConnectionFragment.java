package com.thibaudperso.sonycamera.timelapse.ui.connection;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.thibaudperso.sonycamera.R;
import com.thibaudperso.sonycamera.sdk.CameraAPI;
import com.thibaudperso.sonycamera.sdk.model.Device;
import com.thibaudperso.sonycamera.sdk.model.DeviceManager;
import com.thibaudperso.sonycamera.timelapse.TimelapseApplication;
import com.thibaudperso.sonycamera.timelapse.control.io.IOHandler;
import com.thibaudperso.sonycamera.timelapse.control.io.TestConnectionListener;
import com.thibaudperso.sonycamera.timelapse.control.io.WifiHandler;
import com.thibaudperso.sonycamera.timelapse.ui.settings.TimelapseSettingsActivity;

import java.util.List;

public class ConnectionFragment extends Fragment {

    private static final String PREF_AUTOMATIC_CONTINUE = "pref_auto_continue";
    private static final int PERMISSIONS_REQUEST_COARSE_LOCATION = 1;
    private static final int REQUEST_CODE_NEXT_STEP = 1;

    private DeviceManager mDeviceManager;
    private IOHandler mIOHandler;
    private WifiHandler mWifiHandler;
    private CameraAPI mCameraAPI;

    private AlertDialog alertDialogChooseNetworkConnection;
    private AlertDialog alertDialogChooseNetworkCreation;
    private AlertDialog alertDialogAskForPassword;

    private ImageView connectionInfoNetworkState, connectionInfoAPIState;
    private ProgressBar connectionInfoNetworkStateProgress, connectionInfoAPIStateProgress;

    private boolean mAutomaticContinue = true;
    private boolean mDontCheckNextIO = false;

    private CheckBox connectionAutomaticCheckbox;
    private FloatingActionButton connectionContinueButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TimelapseApplication application = (TimelapseApplication) getActivity().getApplication();

        mDeviceManager = application.getDeviceManager();
        mIOHandler = application.getIOHandler();
        mWifiHandler = mIOHandler.getWifiHandler();
        mCameraAPI = application.getCameraAPI();


        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(application);
        mAutomaticContinue = preferences.getBoolean(PREF_AUTOMATIC_CONTINUE, mAutomaticContinue);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View viewResult = inflater.inflate(R.layout.fragment_connection, container, false);

        connectionInfoNetworkState = (ImageView) viewResult.findViewById(R.id.connectionInfoNetworkState);
        connectionInfoAPIState = (ImageView) viewResult.findViewById(R.id.connectionInfoAPIState);
        connectionInfoNetworkStateProgress = (ProgressBar) viewResult.findViewById(R.id.connectionInfoNetworkStateProgress);
        connectionInfoAPIStateProgress = (ProgressBar) viewResult.findViewById(R.id.connectionInfoAPIStateProgress);
        ((TextView) viewResult.findViewById(R.id.connectionInfoMessage)).setText(
                Html.fromHtml(getString(R.string.connection_information_message)));


        /**
         * Handle Camera spinner
         */
        final Spinner cameraSpinner = (Spinner) viewResult.findViewById(R.id.connectionCameraSpinner);

        ArrayAdapter<Device> adapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_list_item_1, mDeviceManager.getDevices());
        adapter.sort(Device.COMPARE_BY_DEVICEMODEL);
        cameraSpinner.setAdapter(adapter);

        int defaultPosition = adapter.getPosition(mDeviceManager.getSelectedDevice());
        cameraSpinner.setSelection(defaultPosition, false);

        // http://stackoverflow.com/a/9375624/2239938
        cameraSpinner.post(new Runnable() {
            @Override
            public void run() {
                cameraSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(AdapterView<?> arg0, View view,
                                               int position, long id) {
                        mDeviceManager.setSelectedDevice((Device) cameraSpinner.getItemAtPosition(position));
                        checkAPIConnection();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> arg0) {
                    }
                });
            }
        });


        /**
         * Handle Camera Refresh
         */

        viewResult.findViewById(R.id.connectionInfoRefresh).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                checkIO();
            }
        });

        connectionAutomaticCheckbox = ((CheckBox) viewResult.findViewById(R.id.connectionAutomaticCheckbox));
        connectionAutomaticCheckbox.setChecked(mAutomaticContinue);
        connectionContinueButton = (FloatingActionButton) viewResult.findViewById(R.id.connectionSettingsButton);
        connectionContinueButton.setVisibility(View.GONE);
        connectionContinueButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                nextStep();
            }
        });

        return viewResult;
    }


    @Override
    public void onResume() {
        super.onResume();
        mWifiHandler.addListener(mWifiListener);

        checkIO();
    }

    @Override
    public void onPause() {
        super.onPause();
        mWifiHandler.removeListener(mWifiListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();


        if (alertDialogChooseNetworkConnection != null) {
            alertDialogChooseNetworkConnection.cancel();
        }
        if (alertDialogChooseNetworkCreation != null) {
            alertDialogChooseNetworkCreation.cancel();
        }
        if (alertDialogAskForPassword != null) {
            alertDialogAskForPassword.cancel();
        }

    }



    private void checkIO() {

        if(mDontCheckNextIO) {
            mDontCheckNextIO = false;
            return;
        }

        if (checkWifi()) {
            checkAPIConnection();
        }
    }



	/*
     * Wifi
	 */

    private boolean checkWifi() {

        if (!mWifiHandler.isConnected()) {
            wifiError();

            String permission = Manifest.permission.ACCESS_COARSE_LOCATION;

            if (ContextCompat.checkSelfPermission(getContext(), permission)
                    == PackageManager.PERMISSION_GRANTED) {
                wifiProgress();
                mWifiHandler.scanWifiConnections(mWifiScanListener);

            }
            else if (!ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), permission)) {

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
            return false;
        }

        wifiOk();
        return true;
    }



    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_COARSE_LOCATION: {

                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    mWifiHandler.scanWifiConnections(mWifiScanListener);

                } else {
                    Toast.makeText(getContext(), R.string.connection_permission_cancel_message,
                            Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private WifiHandler.ScanListener mWifiScanListener = new WifiHandler.ScanListener() {
        @Override
        public void onWifiScanFinished(List<ScanResult> sonyCameraScanResults,
                                       List<WifiConfiguration> knownSonyCameraConfigurations) {
            /*
             * No Sony Camera network found in scan
             */
            if (sonyCameraScanResults.size() == 0) {
                wifiError();
            }

            /*
             * No Sony Camera network registered on this phone but we found only one in scan
             */
            else if (knownSonyCameraConfigurations.size() == 0 && sonyCameraScanResults.size() == 1) {
                askForNetworkPasswordThenConnect(sonyCameraScanResults.get(0));
            }

            /*
             * No Sony Camera network registered on this phone but we found more than one in scan
             */
            else if (knownSonyCameraConfigurations.size() == 0) {
                selectNetworkForCreation(sonyCameraScanResults);
            }

            /*
             * There is only one Sony Camera known network connected
             */
            else if (knownSonyCameraConfigurations.size() == 1) {
                mWifiHandler.connectToNetworkId(knownSonyCameraConfigurations.get(0).networkId);
            }

            /*
             * There is more than one Sony Camera known network connected
             */
            else {
                selectNetworkForConnection(knownSonyCameraConfigurations);
            }
        }
    };


    private WifiHandler.Listener mWifiListener = new WifiHandler.Listener() {
        @Override
        public void onWifiConnecting(String ssid) {
        }

        @Override
        public void onWifiConnected(String ssid) {

            wifiOk();
            //before checking connection: give the camera some time to adjust to the new connection
            new Handler().postDelayed(new Runnable() {
                public void run() {
                    checkAPIConnection();
                }
            }, 300);
        }

        @Override
        public void onWifiDisconnected() {
            wifiError();
        }
    };


    /*
     * Handle network prompts
	 */

    private void askForNetworkPasswordThenConnect(final ScanResult scanResult) {

        final EditText input = new EditText(getActivity());
        input.setGravity(Gravity.CENTER_HORIZONTAL);
        alertDialogAskForPassword = new AlertDialog.Builder(getActivity())
                .setTitle(String.format(getString(R.string.connection_enter_password), scanResult.SSID))
                .setView(input)
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        wifiError();
                    }
                })
                .setPositiveButton(R.string.connection_enter_password_ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                                String value = input.getText().toString();
                                mWifiHandler.createIfNeededThenConnectToWifi(scanResult.SSID, value);

                            }
                        })
                .setNegativeButton(R.string.connection_enter_password_cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                wifiError();
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
                TextView textView = (TextView) view.findViewById(android.R.id.text1);
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

        alertDialogChooseNetworkCreation = new AlertDialog.Builder(getActivity())
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

    private void selectNetworkForConnection(final List<WifiConfiguration> wifiConfigurations) {

        final ListView listView = new ListView(getActivity());

        ListAdapter adapter = new ArrayAdapter<WifiConfiguration>(getActivity(),
                android.R.layout.simple_list_item_1, wifiConfigurations) {

            @NonNull
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {

                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view.findViewById(android.R.id.text1);
                textView.setText((getItem(position)).SSID);
                return textView;
            }
        };

        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, final View view,
                                    int position, long id) {

                WifiConfiguration wc = (WifiConfiguration) parent.getItemAtPosition(position);
                mWifiHandler.connectToNetworkId(wc.networkId);
            }
        });

        alertDialogChooseNetworkConnection = new AlertDialog.Builder(getActivity())
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


    private void wifiProgress() {
        apiError();
        connectionInfoNetworkState.setVisibility(View.GONE);
        connectionInfoNetworkStateProgress.setVisibility(View.VISIBLE);
    }

    private void wifiError() {
        apiError();
        connectionInfoNetworkStateProgress.setVisibility(View.GONE);
        connectionInfoNetworkState.setVisibility(View.VISIBLE);
        connectionInfoNetworkState.setImageResource(R.drawable.error);
    }

    private void wifiOk() {
        connectionInfoNetworkStateProgress.setVisibility(View.GONE);
        connectionInfoNetworkState.setVisibility(View.VISIBLE);
        connectionInfoNetworkState.setImageResource(R.drawable.ok);
    }




    private boolean mAPITesting = false;

    private void checkAPIConnection() {

        if (mAPITesting || !mWifiHandler.isConnected()) {
            return;
        }

        mAPITesting = true;
        connectionContinueButton.setVisibility(View.GONE);
        apiProgress();

        mCameraAPI.testConnection(new TestConnectionListener() {

            @Override
            public void isConnected(final boolean isConnected) {

                if (isConnected) {
                    mCameraAPI.initialize();
                    apiOk();
                    stepFinished();
                } else {
                    apiError();
                }

                mAPITesting = false;
            }
        });
    }

    private void apiProgress() {
        connectionInfoAPIState.setVisibility(View.GONE);
        connectionInfoAPIStateProgress.setVisibility(View.VISIBLE);
    }

    private void apiError() {
        connectionInfoAPIStateProgress.setVisibility(View.GONE);
        connectionInfoAPIState.setVisibility(View.VISIBLE);
        connectionInfoAPIState.setImageResource(R.drawable.error);
    }

    private void apiOk() {
        connectionInfoAPIStateProgress.setVisibility(View.GONE);
        connectionInfoAPIState.setVisibility(View.VISIBLE);
        connectionInfoAPIState.setImageResource(R.drawable.ok);
    }


    private void stepFinished() {
        if (connectionAutomaticCheckbox.isChecked()) {
            nextStep();
        } else {
            connectionContinueButton.setVisibility(View.VISIBLE);
        }
    }

    private void nextStep() {
        Intent intent = new Intent(getContext(), TimelapseSettingsActivity.class);
        startActivityForResult(intent, REQUEST_CODE_NEXT_STEP);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_CODE_NEXT_STEP) {
            mDontCheckNextIO = true;
            wifiError();
            connectionContinueButton.setVisibility(View.GONE);
        }
    }
}
