package com.thibaudperso.sonycamera.timelapse.fragments;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.net.NetworkInfo.State;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.thibaudperso.sonycamera.R;
import com.thibaudperso.sonycamera.io.WifiHandler;
import com.thibaudperso.sonycamera.io.WifiListener;
import com.thibaudperso.sonycamera.sdk.CameraIO;
import com.thibaudperso.sonycamera.sdk.core.TestConnectionListener;
import com.thibaudperso.sonycamera.sdk.model.Device;
import com.thibaudperso.sonycamera.sdk.model.DeviceManager;
import com.thibaudperso.sonycamera.timelapse.StepFragment;
import com.thibaudperso.sonycamera.timelapse.TimelapseApplication;

import java.util.List;

public class ConnectionFragment extends StepFragment implements WifiListener {

    private final static int PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1;

    private DeviceManager mDeviceManager;
    private WifiHandler mWifiHandler;
    private CameraIO mCameraIO;

    private TextView connectionInfoMessage;

    private AlertDialog alertDialogChooseNetworkConnection;
    private AlertDialog alertDialogChooseNetworkCreation;
    private AlertDialog alertDialogAskForPassword;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDeviceManager = ((TimelapseApplication) getActivity().getApplication()).getDeviceManager();
        mWifiHandler = ((TimelapseApplication) getActivity().getApplication()).getWifiHandler();
        mCameraIO = ((TimelapseApplication) getActivity().getApplication()).getCameraIO();

    }

    @Override
    public void onStart() {
        super.onStart();

        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View viewResult = inflater.inflate(R.layout.fragment_connection, container, false);

        connectionInfoMessage = (TextView) viewResult.findViewById(R.id.connectionInfoMessage);

        /**
         * Handle Camera spinner
         */
        final Spinner cameraSpinner = (Spinner) viewResult.findViewById(R.id.connectionCameraSpinner);

        ArrayAdapter<Device> adapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_list_item_1, mDeviceManager.getDevices());
        adapter.sort(Device.COMPARE_BY_DEVICEMODEL);
        cameraSpinner.setAdapter(adapter);

        int defaultPosition = adapter.getPosition(mDeviceManager.getSelectedDevice());
        cameraSpinner.setSelection(defaultPosition);

        cameraSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View view,
                                       int position, long id) {
                mDeviceManager.setSelectedDevice((Device) cameraSpinner.getItemAtPosition(position));
                checkForConnection();
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });


        /**
         * Handle Camera Refresh
         */

        ImageView deviceConnectionRefresh = (ImageView) viewResult.findViewById(R.id.connectionInfoRefresh);
        deviceConnectionRefresh.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                checkForConnection();
            }
        });

        return viewResult;
    }


    private void checkForConnection() {
        setStepCompleted(false);
        if (mWifiHandler != null) {
            mWifiHandler.checkForConnection();
        }
    }

    @Override
    public void onEnterFragment() {
        super.onEnterFragment();

        if (mWifiHandler != null) {
            mWifiHandler.addListener(this);
        }
        if (mWifiHandler.getCameraWifiState() == State.DISCONNECTED) {
            checkForConnection();
        }
    }

    @Override
    public void onExitFragment() {
        super.onExitFragment();

        if (mWifiHandler != null) {
            mWifiHandler.removeListener(this);
        }

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


	/*
     * Handle network information
	 */

    private void setConnectionInfoMessage(int resId, Object... params) {
        if (this.mIsActive) {
            connectionInfoMessage.setText(String.format(getString(resId), params));
        }
    }


    @Override
    public void onWifiConnecting(String ssid) {
        setConnectionInfoMessage(R.string.connection_info_wifi_connecting, ssid);
    }

    @Override
    public void onWifiConnected(String ssid) {
        setConnectionInfoMessage(R.string.connection_info_wifi_connected, ssid);
        //before checking connection: give the camera some time to adjust to the new connection
        android.os.Handler handler = new android.os.Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                checkWSConnection();
            }
        }, 300);
    }

    @Override
    public void onWifiDisconnected() {
        setConnectionInfoMessage(R.string.connection_info_wifi_disconnected);
        setStepCompleted(false);
    }

    @Override
    public void onWifiStartScan() {
        setConnectionInfoMessage(R.string.connection_info_scan_networks);
    }

    @Override
    public void onWifiScanFinished(List<ScanResult> sonyCameraScanResults,
                                   List<WifiConfiguration> sonyCameraWifiConfiguration) {

		/*
         * No Sony Camera network found in scan
		 */
        if (sonyCameraScanResults.size() == 0) {
            setConnectionInfoMessage(R.string.connection_info_wifi_not_found);
        }

		/*
		 * No Sony Camera network registered on this phone but we found only one in scan 
		 */
        else if (sonyCameraWifiConfiguration.size() == 0 && sonyCameraScanResults.size() == 1) {
            askForNetworkPasswordThenConnect(sonyCameraScanResults.get(0));
        }

		/*
		 * No Sony Camera network registered on this phone but we found more than one in scan 
		 */
        else if (sonyCameraWifiConfiguration.size() == 0) {
            selectNetworkForCreation(sonyCameraScanResults);
        }

		/*
		 * There is only one Sony Camera known network connected
		 */
        else if (sonyCameraWifiConfiguration.size() == 1) {
            mWifiHandler.connectToNetworkId(sonyCameraWifiConfiguration.get(0).networkId);
        }

		/*
		 * There is more than one Sony Camera known network connected
		 */
        else {
            selectNetworkForConnection(sonyCameraWifiConfiguration);
        }

    }

    private void checkWSConnection() {

        mCameraIO.testConnection(new TestConnectionListener() {

            @Override
            public void cameraConnected(final boolean isConnected) {

                if (isConnected && mDeviceManager.getSelectedDevice() != null) {
                    if (mDeviceManager.getSelectedDevice().needInit()) {
                        mCameraIO.initWebService(null);
                    }
                    mCameraIO.setShootMode("still");
                }

                if (getActivity() == null) {
                    return;
                }

                getActivity().runOnUiThread(new Runnable() {

                    @Override
                    public void run() {

                        setStepCompleted(isConnected);

                        setConnectionInfoMessage(isConnected ? R.string.connection_info_ok : R.string.connection_info_ws_failed);
                    }
                });
            }
        });
    }




	/*
	 * Handle network prompts 
	 */

    private void askForNetworkPasswordThenConnect(final ScanResult scanResult) {

        final EditText input = new EditText(getActivity());

        alertDialogAskForPassword = new AlertDialog.Builder(getActivity())
                .setTitle(String.format(getString(R.string.connection_enter_password), scanResult.SSID))
                .setView(input)
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
                                // Do nothing.
                            }
                        }).show();

    }

    private void selectNetworkForCreation(final List<ScanResult> scanResults) {

        final ListView listView = new ListView(getActivity());

        ListAdapter adapter = new ArrayAdapter<ScanResult>(getActivity(),
                android.R.layout.simple_list_item_1, scanResults) {

            public View getView(int position, View convertView, ViewGroup parent) {

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
                .setNegativeButton(R.string.connection_choose_network_cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // Do nothing.
                            }
                        }).show();

    }

    private void selectNetworkForConnection(final List<WifiConfiguration> wifiConfigurations) {

        final ListView listView = new ListView(getActivity());

        ListAdapter adapter = new ArrayAdapter<WifiConfiguration>(getActivity(),
                android.R.layout.simple_list_item_1, wifiConfigurations) {

            public View getView(int position, View convertView, ViewGroup parent) {

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
                .setNegativeButton(R.string.connection_choose_network_cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // Do nothing.
                            }
                        }).show();

    }


    @Override
    public Spanned getInformation() {
        return Html.fromHtml(getString(R.string.connection_information_message));
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

        if (requestCode == PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            checkForConnection();
        }
    }
}
