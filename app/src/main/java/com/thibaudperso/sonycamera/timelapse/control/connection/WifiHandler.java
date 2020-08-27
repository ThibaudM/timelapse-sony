package com.thibaudperso.sonycamera.timelapse.control.connection;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class WifiHandler {

    private Context mContext;

    private WifiManager mWifiManager;


    public WifiHandler(Context context) {
        mContext = context;
        mWifiManager = (WifiManager) context.getApplicationContext().
                getSystemService(Context.WIFI_SERVICE);
    }


    WifiInfo getConnectedWifi() {
        return mWifiManager.getConnectionInfo();
    }

    void connectToNetworkId(int netId) {

        if (mWifiManager == null) {
            return;
        }

        /*
         * Check if network id exists
         */
        boolean netIdFound = false;
        List<WifiConfiguration> configuredNetworks = mWifiManager.getConfiguredNetworks();
        for (WifiConfiguration wc : configuredNetworks) {
            if (wc.networkId == netId) {
                netIdFound = true;
                break;
            }
        }
        if (!netIdFound) return;

        mWifiManager.enableNetwork(netId, true);
    }


    static boolean isSonyCameraSSID(String ssid) {
        return ssid != null && ssid.matches("^DIRECT-\\w{4}:.*$");
    }


    static String parseSSID(String ssid) {

        if (ssid != null
                && ssid.length() >= 2 && ssid.charAt(0) == '"'
                && ssid.charAt(ssid.length() - 1) == '"') {
            return ssid.substring(1, ssid.length() - 1);
        }

        return ssid;
    }


    void startScan() {
        mWifiManager.startScan();
    }

    public void disconnect() {
        mWifiManager.disconnect();
    }

    boolean isEnabled() {
        return mWifiManager.isWifiEnabled();
    }

    private WifiConfiguration getWifiConfigurationFromSSID(String SSID) {

        List<WifiConfiguration> knownNetworks = mWifiManager.getConfiguredNetworks();

        if (knownNetworks == null) return null;

        for (WifiConfiguration net : knownNetworks) {
            if (net.SSID != null && net.SSID.equals("\"" + SSID + "\"")) {
                return net;
            }
        }
        return null;
    }

    private Listener mListener;

    public void setListener(Listener listener) {

        if (listener != null && mListener == null) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
            intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
            intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
            mContext.registerReceiver(mBroadcastReceiver, intentFilter);
        } else if (listener == null && mListener != null) {
            mContext.unregisterReceiver(mBroadcastReceiver);
        }

        mListener = listener;
    }

    public interface Listener {
        void wifiEnabled();

        void wifiDisabled();

        void wifiConnected(NetworkInfo networkInfo);

        void wifiDisconnected(NetworkInfo networkInfo);

        void onWifiScanFinished(List<ScanResult> sonyCameraScanResults,
                                List<WifiConfiguration> configurations);
    }


    // Workaround for multiple broadcast of wifi events:
    // http://stackoverflow.com/questions/8412714/broadcastreceiver-receives-multiple-identical-messages-for-one-event
    // We added bssid to the condition, because sometimes two wifi are connected in a row
    //  without disconnection
    private boolean mIsFirstConnection = true;
    private boolean mIsFirstDisconnection = true;
    private String mCurrentBSSID = "";
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (mListener == null) return;
            if (isInitialStickyBroadcast()) return;

            if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())) {

                NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);

                if (networkInfo.isConnected()
                        && !mCurrentBSSID.equals(mWifiManager.getConnectionInfo().getSSID())
                        && mIsFirstConnection) {

                    mCurrentBSSID = mWifiManager.getConnectionInfo().getSSID();

                    mListener.wifiConnected(networkInfo);
                    mIsFirstConnection = false;
                    mIsFirstDisconnection = true;
                } else if (!networkInfo.isConnected() && mIsFirstDisconnection) {
                    mListener.wifiDisconnected(networkInfo);
                    mIsFirstDisconnection = false;
                    mIsFirstConnection = true;
                }

            } else if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())) {

                int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
                        WifiManager.WIFI_STATE_UNKNOWN);
                if (state == WifiManager.WIFI_STATE_ENABLED) {
                    mListener.wifiEnabled();
                } else if (state == WifiManager.WIFI_STATE_DISABLED) {
                    mListener.wifiDisabled();
                }

            } else if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(intent.getAction())
                    && ContextCompat.checkSelfPermission(mContext,
                    Manifest.permission.ACCESS_COARSE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {

                final List<ScanResult> sonyCameraScanResults = new ArrayList<>();
                final List<WifiConfiguration> knownSonyCameraConfigurations = new ArrayList<>();

                for (ScanResult sr : mWifiManager.getScanResults()) {
                    if (!isSonyCameraSSID(sr.SSID)) continue;
                    sonyCameraScanResults.add(sr);
                    WifiConfiguration wc = getWifiConfigurationFromSSID(sr.SSID);
                    if (wc == null) continue;
                    knownSonyCameraConfigurations.add(wc);
                }
                mListener.onWifiScanFinished(sonyCameraScanResults, knownSonyCameraConfigurations);
            }
        }
    };

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
}
