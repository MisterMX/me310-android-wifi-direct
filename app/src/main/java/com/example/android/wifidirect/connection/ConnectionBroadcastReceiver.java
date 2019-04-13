package com.example.android.wifidirect.connection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;

class ConnectionBroadcastReceiver extends BroadcastReceiver implements PeerListListener, ConnectionInfoListener {
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private final WifiEventListener wifiEventListener;

    public ConnectionBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel, WifiEventListener wifiEventListener) {
        super();
        this.manager = manager;
        this.channel = channel;
        this.wifiEventListener = wifiEventListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        switch (intent.getAction()) {
            case WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION:
                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                    wifiEventListener.onDeviceWifiDirectEnabled();
                } else {
                    wifiEventListener.onDeviceWifiDirectDisabled();
                }
                break;

            case WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION:
                if (manager != null) {
                    manager.requestPeers(channel, this);
                }
                break;

            case WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION:
                NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

                if (networkInfo.isConnected()) {
                    // we are connected with the other device, request connection
                    // info to find group owner IP
                    manager.requestConnectionInfo(channel, this);
                } else {
                    // It's a disconnect
                    wifiEventListener.onDisconnected();
                }
                break;

            case WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION:
                WifiP2pDevice wifiP2pDevice = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
                wifiEventListener.onDeviceWifiDirectChanged(wifiP2pDevice);
                break;
        }
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
        wifiEventListener.onPeersAvailable(wifiP2pDeviceList);
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
        wifiEventListener.onConnected(wifiP2pInfo);
    }

    public IntentFilter getIntentFilter() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        return intentFilter;
    }


    public interface WifiEventListener {
        void onDeviceWifiDirectEnabled();
        void onDeviceWifiDirectDisabled();
        void onDeviceWifiDirectChanged(WifiP2pDevice wifiP2pDevice);
        void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList);
        void onConnected(WifiP2pInfo wifiP2pInfo);
        void onDisconnected();
    }
}
