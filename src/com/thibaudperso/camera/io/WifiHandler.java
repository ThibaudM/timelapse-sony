package com.thibaudperso.camera.io;

import java.util.ArrayList;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

public class WifiHandler {

	private Context context;
	private WifiManager mWifiManager;
	private WifiInfo lastWifiConnected;
	private NetworkInfo.State cameraWifiState;

	private List<WifiListener> listeners;

	public WifiHandler(Context context) {

		this.context = context;
		mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE); 

		listeners = new ArrayList<WifiListener>();

	}

	public void checkForConnection() {
		cameraWifiState = State.DISCONNECTED;
		WifiInfo wifiInfo = mWifiManager.getConnectionInfo();

		String ssid = wifiInfo.getSSID().substring(1, wifiInfo.getSSID().length()-1);
		if(wifiInfo != null && isSonyCameraSSID(ssid)) {

			// Don't need to check more, camera is already connected
			connected(ssid);
			return;
		}

		mWifiManager.startScan();
		context.registerReceiver(scanResultsBroadcastReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));	

	}


	public void createIfNeededThenConnectToWifi(String networkSSID, String networkPassword) {
		int netId = -1;

		List<WifiConfiguration> list = mWifiManager.getConfiguredNetworks();
		for( WifiConfiguration i : list ) {

			// In the case of network is already registered
			if(i.SSID != null && i.SSID.equals("\"" + networkSSID + "\"")) {

				// In case password changed since last time
				i.preSharedKey = "\""+ networkPassword +"\"";
				mWifiManager.saveConfiguration();

				netId = i.networkId;
				break;
			}           
		}

		// In the case of network is not registered create it and join it
		if(netId == -1) {
			netId = createWPAWifi(networkSSID, networkPassword);
		}

		connectToNetworkId(netId);
	}


	public void connectToNetworkId(int netId) {

		List<WifiConfiguration> configuredNetworks = mWifiManager.getConfiguredNetworks();
		for(WifiConfiguration wc : configuredNetworks) {

			if(wc.networkId != netId) {
				continue;
			}

			if(mWifiManager.getConnectionInfo() != null) {
				String ssid = mWifiManager.getConnectionInfo().getSSID().substring(1, mWifiManager.getConnectionInfo().getSSID().length()-1);

				if(!isSonyCameraSSID(ssid)) {
					lastWifiConnected = mWifiManager.getConnectionInfo();
				}
			}


			// Connect only if network id exist
			mWifiManager.enableNetwork(netId, true);

			for(WifiListener listener : listeners) {
				listener.onWifiConnecting(wc.SSID);
			}

		}
	}


	private int createWPAWifi(String networkSSID, String networkPassword) {

		WifiConfiguration wc = new WifiConfiguration();
		wc.SSID = "\"" + networkSSID + "\"";
		wc.preSharedKey = "\""+ networkPassword +"\"";

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


	public void reconnectToLastWifi() {

		if(lastWifiConnected != null) {
			mWifiManager.enableNetwork(lastWifiConnected.getNetworkId(), true);
			
			// Maybe remove comment on this line for more logic
			//lastWifiConnected = null;
		}
	}

	private static boolean isSonyCameraSSID(String SSID) {
		return SSID.matches("^DIRECT-\\w{4}:.*$");
	}

	private WifiConfiguration getWifiConfigurationFromSSID(String SSID) {

		List<WifiConfiguration> knownNetworks = mWifiManager.getConfiguredNetworks();

		for(WifiConfiguration net : knownNetworks) {
			if(net.SSID.equals("\""+SSID+"\"")) {
				return net;
			}
		}

		return null;
	}


	public void addListener(WifiListener listener) {
		if(listeners.contains(listener)) {
			return;
		}
		listeners.add(listener);
	}

	public void removeListener(WifiListener listener) {
		if(!listeners.contains(listener)) {
			return;
		}
		listeners.remove(listener);
	}

	private void connected(String ssid) {
		cameraWifiState = State.CONNECTED;
		for(WifiListener listener : listeners) {
			listener.onWifiConnected(ssid);
		}
	}

	private void disconnected() {
		cameraWifiState = State.DISCONNECTED;
		for(WifiListener listener : listeners) {
			listener.onWifiDisconnected();
		}
	}

	private BroadcastReceiver wifiBroadcastReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {

			NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);

			if(networkInfo == null || 
					networkInfo.getType() != ConnectivityManager.TYPE_WIFI) {
				return;
			}

			NetworkInfo.State state = networkInfo.getState();

			if(cameraWifiState == State.CONNECTED && state == State.DISCONNECTED) {

				disconnected();

			}

			if(state == State.CONNECTED && networkInfo.getExtraInfo() != null) {

				String ssid = networkInfo.getExtraInfo().substring(1, networkInfo.getExtraInfo().length()-1);

				if(!isSonyCameraSSID(ssid)) {
					return;
				}

				connected(ssid);

			}
		}
	};

	private BroadcastReceiver scanResultsBroadcastReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent) 
		{
			List<WifiConfiguration> sonyCameraWifiConfiguration = new ArrayList<WifiConfiguration>();
			List<ScanResult> sonyCameraScanResults = new ArrayList<ScanResult>();

			List<ScanResult> results = mWifiManager.getScanResults();
			for(ScanResult sr : results) {

				if(!isSonyCameraSSID(sr.SSID)) {
					continue;
				}
				sonyCameraScanResults.add(sr);

				WifiConfiguration wc = getWifiConfigurationFromSSID(sr.SSID);

				if(wc == null) {
					continue;
				}
				sonyCameraWifiConfiguration.add(wc);
			}

			for(WifiListener listener : listeners) {
				listener.onWifiScanFinished(sonyCameraScanResults, sonyCameraWifiConfiguration);
			}

			context.unregisterReceiver(scanResultsBroadcastReceiver);
		}
	};

	public void register() {
		final IntentFilter filters = new IntentFilter();
		filters.addAction("android.net.wifi.WIFI_STATE_CHANGED");
		filters.addAction("android.net.wifi.STATE_CHANGE");
		context.registerReceiver(wifiBroadcastReceiver, filters);
	}

	public void unregister() {
		try {
			context.unregisterReceiver(wifiBroadcastReceiver);
			context.unregisterReceiver(scanResultsBroadcastReceiver);
		} catch (IllegalArgumentException e) { }
	}
}
