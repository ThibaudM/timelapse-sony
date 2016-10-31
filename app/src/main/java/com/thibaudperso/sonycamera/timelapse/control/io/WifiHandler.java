package com.thibaudperso.sonycamera.timelapse.control.io;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;

import java.util.ArrayList;
import java.util.List;

public class WifiHandler {

    public enum State {DISCONNECTED, SCANNING, CONNECTING, CONNECTED}

    private Context mContext;
    private WifiManager mWifiManager;
    private boolean wasWifiDisabled;
    private WifiInfo lastWifiConnected;


    private List<Listener> stateListeners;

    private State mState;

    public WifiHandler(Context context) {

        this.mContext = context;
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        wasWifiDisabled = mWifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLED;
        mWifiManager.setWifiEnabled(true);
        stateListeners = new ArrayList<>();

        mState = State.DISCONNECTED;

        isConnected();
    }


    public boolean isConnected() {

        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        if (wifiInfo == null) {
            mState = State.DISCONNECTED;
            return false;
        }

        String ssid = parseSSID(wifiInfo.getSSID());
        if(isSonyCameraSSID(ssid)) {
            bindProcessToWifiNetwork();
            mState = State.CONNECTED;
            return true;
        }

        mState = State.DISCONNECTED;
        return false;
    }


    public void scanWifiConnections(final ScanListener listener) {

        if (isConnected() || mState == State.SCANNING || listener == null) {
            return;
        }

        mState = State.SCANNING;

        final Handler handler = new Handler();
        mContext.registerReceiver(
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        context.unregisterReceiver(this);

                        final List<WifiConfiguration> sonyCameraWifiConfiguration = new ArrayList<>();
                        final List<ScanResult> sonyCameraScanResults = new ArrayList<>();

                        for (ScanResult sr : mWifiManager.getScanResults()) {

                            if (!isSonyCameraSSID(sr.SSID)) {
                                continue;
                            }
                            sonyCameraScanResults.add(sr);

                            WifiConfiguration wc = getWifiConfigurationFromSSID(sr.SSID);

                            if (wc == null) {
                                continue;
                            }
                            sonyCameraWifiConfiguration.add(wc);
                        }

                        mState = State.DISCONNECTED;

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                listener.onWifiScanFinished(sonyCameraScanResults,
                                        sonyCameraWifiConfiguration);
                            }
                        });
                    }
                },
                new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        mWifiManager.startScan();

    }


    public void createIfNeededThenConnectToWifi(String networkSSID, String networkPassword) {
        int netId = -1;

        List<WifiConfiguration> list = mWifiManager.getConfiguredNetworks();

        if (list != null) {

            for (WifiConfiguration i : list) {

                // In the case of network is already registered
                if (i.SSID != null && i.SSID.equals("\"" + networkSSID + "\"")) {

                    // In case password changed since last time
                    i.preSharedKey = "\"" + networkPassword + "\"";
                    mWifiManager.saveConfiguration();

                    netId = i.networkId;
                    break;
                }
            }
        }

        // In the case of network is not registered create it and join it
        if (netId == -1) {
            netId = createWPAWifi(networkSSID, networkPassword);
        }

        connectToNetworkId(netId);
    }


    public void connectToNetworkId(int netId) {
        List<WifiConfiguration> configuredNetworks = mWifiManager.getConfiguredNetworks();
        for (WifiConfiguration wc : configuredNetworks) {

            if (wc.networkId != netId) {
                continue;
            }

            WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
            if (wifiInfo != null) {
                String ssid = parseSSID(mWifiManager.getConnectionInfo().getSSID());

                if (!isSonyCameraSSID(ssid)) {
                    lastWifiConnected = mWifiManager.getConnectionInfo();
                }
            }


            // Connect only if network id exist
            mWifiManager.enableNetwork(netId, true);

            for (Listener listener : stateListeners) {
                listener.onWifiConnecting(wc.SSID);
            }

        }
    }


    private int createWPAWifi(String networkSSID, String networkPassword) {

        WifiConfiguration wc = new WifiConfiguration();
        wc.SSID = "\"" + networkSSID + "\"";
        wc.preSharedKey = "\"" + networkPassword + "\"";

        wc.hiddenSSID = true;
        wc.status = WifiConfiguration.Status.ENABLED;
        wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        wc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);


        int netId = mWifiManager.addNetwork(wc);
        mWifiManager.saveConfiguration();

        return netId;
    }

    private static boolean isSonyCameraSSID(String ssid) {
        return ssid != null && ssid.matches("^DIRECT-\\w{4}:.*$");
    }

    private WifiConfiguration getWifiConfigurationFromSSID(String SSID) {

        List<WifiConfiguration> knownNetworks = mWifiManager.getConfiguredNetworks();

        if (knownNetworks == null) {
            return null;
        }

        for (WifiConfiguration net : knownNetworks) {
            if (net.SSID != null && net.SSID.equals("\"" + SSID + "\"")) {
                return net;
            }
        }

        return null;
    }




    private boolean bindProcessToWifiNetwork() {

        // Workaround when there is a data connection more than the wifi one
        // http://stackoverflow.com/questions/33237074/request-over-wifi-on-android-m
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            ConnectivityManager connectivityManager = (ConnectivityManager)
                    mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            Network activeNetwork = connectivityManager.getActiveNetwork();
            for (Network net : connectivityManager.getAllNetworks()) {
                if (!net.equals(activeNetwork)) {
                    connectivityManager.bindProcessToNetwork(net);
                    return true;
                }
            }
            return false;
        }
        return true;
    }




    public void addListener(Listener listener) {
        if (stateListeners.contains(listener)) {
            return;
        }
        stateListeners.add(listener);

        if (stateListeners.size() > 0) {
            final IntentFilter filters = new IntentFilter();
            filters.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
            mContext.registerReceiver(connectivityBroadcastReceiver, filters);
        }
    }

    public void removeListener(Listener listener) {
        stateListeners.remove(listener);

        if (stateListeners.size() == 0) {
            try {
                mContext.unregisterReceiver(connectivityBroadcastReceiver);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }


    private BroadcastReceiver connectivityBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);

            if (networkInfo == null || networkInfo.getType() != ConnectivityManager.TYPE_WIFI) {
                return;
            }

            NetworkInfo.State state = networkInfo.getState();

            if (mState == State.CONNECTED && state == NetworkInfo.State.DISCONNECTED) {

                mState = State.DISCONNECTED;
                for (Listener listener : stateListeners) {
                    listener.onWifiDisconnected();
                }

            } else if (state == NetworkInfo.State.CONNECTED && mState != State.CONNECTED) {

                WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
                String ssid;

                if (wifiInfo == null
                        || !isSonyCameraSSID(ssid = parseSSID(wifiInfo.getSSID()))
                        || !bindProcessToWifiNetwork()) {
                    return;
                }

                mState = State.CONNECTED;
                for (Listener listener : stateListeners) {
                    listener.onWifiConnected(ssid);
                }
            }
        }
    };


    private String parseSSID(String ssid) {
        if (ssid != null && ssid.length() >= 2 && ssid.charAt(0) == '"' && ssid.charAt(ssid.length() - 1) == '"') {
            return ssid.substring(1, ssid.length() - 1);
        }
        return ssid;
    }

    public void disconnect() {

        mState = State.DISCONNECTED;

        //reconnect to last wifi
        if (lastWifiConnected != null && lastWifiConnected.getNetworkId() != -1) {
            mWifiManager.enableNetwork(lastWifiConnected.getNetworkId(), true);
        } else {
            //no previous wifi: just disconnect from the camera's
            mWifiManager.disconnect();
            //disable wifi as well if it was disabled on app start
            if (wasWifiDisabled) {
                mWifiManager.setWifiEnabled(false);
            }
        }
    }


    public interface ScanListener {

        void onWifiScanFinished(List<ScanResult> sonyCameraScanResults,
                                List<WifiConfiguration> knownSonyCameraConfigurations);
    }

    public interface Listener {

        void onWifiConnecting(String ssid);

        void onWifiConnected(String ssid);

        void onWifiDisconnected();


    }

}
