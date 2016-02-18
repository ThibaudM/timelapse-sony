package com.thibaudperso.sonycamera.timelapse.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.view.Gravity;
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
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.thibaudperso.sonycamera.R;
import com.thibaudperso.sonycamera.io.WifiHandler;
import com.thibaudperso.sonycamera.io.WifiListener;
import com.thibaudperso.sonycamera.sdk.CameraAPI;
import com.thibaudperso.sonycamera.sdk.core.TestConnectionListener;
import com.thibaudperso.sonycamera.sdk.model.Device;
import com.thibaudperso.sonycamera.sdk.model.DeviceManager;
import com.thibaudperso.sonycamera.timelapse.StepFragment;
import com.thibaudperso.sonycamera.timelapse.TimelapseApplication;

import java.util.List;

public class ConnectionFragment extends StepFragment {

    private DeviceManager mDeviceManager;
    private WifiHandler mWifiHandler;
    private CameraAPI mCameraAPI;

    private AlertDialog alertDialogChooseNetworkConnection;
    private AlertDialog alertDialogChooseNetworkCreation;
    private AlertDialog alertDialogAskForPassword;

    private ImageView connectionInfoNetworkState, connectionInfoAPIState;
    private ProgressBar connectionInfoNetworkStateProgress, connectionInfoAPIStateProgress;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDeviceManager = ((TimelapseApplication) getActivity().getApplication()).getDeviceManager();
        mWifiHandler = ((TimelapseApplication) getActivity().getApplication()).getWifiHandler();
        mCameraAPI = ((TimelapseApplication) getActivity().getApplication()).getCameraAPI();

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
        cameraSpinner.setSelection(defaultPosition);

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


        /**
         * Handle Camera Refresh
         */

        ImageView deviceConnectionRefresh = (ImageView) viewResult.findViewById(R.id.connectionInfoRefresh);
        deviceConnectionRefresh.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                refreshConnection();
            }
        });

        return viewResult;
    }



    @Override
    public void onEnterFragment() {
        super.onEnterFragment();

        if (mWifiHandler != null) {
            mWifiHandler.addListener(mWifiListener);
        }

        refreshConnection();
    }

    @Override
    public void onExitFragment() {
        super.onExitFragment();

        if (mWifiHandler != null) {
            mWifiHandler.removeListener(mWifiListener);
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



    private void refreshConnection() {

        setStepCompleted(false);

        if(mWifiHandler == null) {
            wifiError();
            apiError();
            return;
        }

        if(mWifiHandler.getWifiState() != NetworkInfo.State.CONNECTED) {
            wifiError();
            mWifiHandler.checkForConnection();
        }
        else {
            wifiOk();
            apiError();
            checkAPIConnection();
        }
    }



	/*
     * Wifi
	 */



    private WifiListener mWifiListener = new WifiListener() {
        @Override
        public void onWifiConnecting(String ssid) {}

        @Override
        public void onWifiConnected(String ssid) {
            wifiOk();
            apiProgress();
            //before checking connection: give the camera some time to adjust to the new connection
            android.os.Handler handler = new android.os.Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    checkAPIConnection();
                }
            }, 300);
        }

        @Override
        public void onWifiDisconnected() {
            wifiError();
        }

        @Override
        public void onWifiStartScan() {
            wifiProgress();
        }

        @Override
        public void onWifiScanFinished(List<ScanResult> sonyCameraScanResults,
                                       List<WifiConfiguration> sonyCameraWifiConfiguration) {

            /*
             * No Sony Camera network found in scan
             */
            if (sonyCameraScanResults.size() == 0) {
                wifiError();
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
        setStepCompleted(false);
    }

    private void wifiOk() {
        connectionInfoNetworkStateProgress.setVisibility(View.GONE);
        connectionInfoNetworkState.setVisibility(View.VISIBLE);
        connectionInfoNetworkState.setImageResource(R.drawable.ok);
    }














    private void checkAPIConnection() {

        apiProgress();

        mCameraAPI.testConnection(new TestConnectionListener() {

            @Override
            public void cameraConnected(final boolean isConnected) {

                if (isConnected) {
                    mCameraAPI.initWebService(null);
                    mCameraAPI.setShootMode("still");
                }

                if (getActivity() == null) {
                    return;
                }

                getActivity().runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        setStepCompleted(isConnected);
                        if(isConnected)
                            apiOk();
                        else
                            apiError();
                    }
                });
            }
        });
    }

    private void apiProgress() {
        wifiOk();
        connectionInfoAPIState.setVisibility(View.GONE);
        connectionInfoAPIStateProgress.setVisibility(View.VISIBLE);
    }

    private void apiError() {
        connectionInfoAPIStateProgress.setVisibility(View.GONE);
        connectionInfoAPIState.setVisibility(View.VISIBLE);
        connectionInfoAPIState.setImageResource(R.drawable.error);
    }

    private void apiOk() {
        wifiOk();
        connectionInfoAPIStateProgress.setVisibility(View.GONE);
        connectionInfoAPIState.setVisibility(View.VISIBLE);
        connectionInfoAPIState.setImageResource(R.drawable.ok);
    }






    @Override
    public Spanned getInformation() {
        return Html.fromHtml(getString(R.string.connection_information_message));
    }

}
