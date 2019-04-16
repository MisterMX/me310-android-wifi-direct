package com.example.android.wifidirect.connection.p2p;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;

class ConnectionBroadcastReceiver extends BroadcastReceiver implements PeerListListener, ConnectionInfoListener, WifiP2pManager.GroupInfoListener {
    private final WifiEventListener wifiEventListener;

    private WifiP2pManager manager;
    private Channel channel;
    private WifiP2pInfo connectionInfo;
    private NetworkInfo networkInfo;
    private WifiP2pGroup groupInfo;


    public ConnectionBroadcastReceiver(WifiP2pManager manager, Channel channel, WifiEventListener wifiEventListener) {
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
                networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

                if (networkInfo.isConnected()) {
                    // we are connected with the other device, request connection
                    // info to find group owner IP
                    manager.requestConnectionInfo(channel, this);
                } else {
                    // It's a disconnect
                    wifiEventListener.onDisconnected(networkInfo);
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
        connectionInfo = wifiP2pInfo;
        if (wifiP2pInfo.groupFormed) {
            manager.requestGroupInfo(channel, this);
        }
    }

    public IntentFilter getIntentFilter() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        return intentFilter;
    }

    @Override
    public void onGroupInfoAvailable(WifiP2pGroup wifiP2pGroup) {
        groupInfo = wifiP2pGroup;
        wifiEventListener.onConnected(connectionInfo, wifiP2pGroup, networkInfo);
    }


    public interface WifiEventListener {
        void onDeviceWifiDirectEnabled();

        void onDeviceWifiDirectDisabled();

        void onDeviceWifiDirectChanged(WifiP2pDevice wifiP2pDevice);

        void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList);

        void onConnected(WifiP2pInfo wifiP2pInfo, WifiP2pGroup groupInfo, NetworkInfo networkInfo);

        void onDisconnected(NetworkInfo networkInfo);
    }
}
