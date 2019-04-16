package com.example.android.wifidirect.connection.p2p;

import android.content.Context;
import android.net.NetworkInfo;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.util.Log;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import lombok.Getter;

public class ConnectionManager {
    private static final String TAG = ConnectionManager.class.getName();

    private final Context context;
    private final EventListener eventListener;
    private final WifiP2pManager manager;

    private Channel channel;
    private ConnectionBroadcastReceiver receiver;
    private ConnectingListener connectingListener;

    @Getter
    private boolean isInitialized = false;

    @Getter
    private boolean isActive = false;

    @Getter
    private boolean isWifiP2pEnabled = false;

    @Getter
    private WifiP2pInfo connectionInfo;

    @Getter
    private NetworkInfo networkInfo;

    @Getter
    private WifiP2pGroup groupInfo;


    public ConnectionManager(Context context, EventListener eventListener) {
        this.context = context;
        this.eventListener = eventListener;

        manager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
    }

    public void initialize() {
        if (isInitialized) {
            throw new WifiP2PConnectionException("ConnectionManager already initialized.");
        }

        channel = manager.initialize(context, context.getMainLooper(), null);

        receiver = new ConnectionBroadcastReceiver(manager, channel, new WifiEventListenerImpl());
        context.registerReceiver(receiver, receiver.getIntentFilter());

        isInitialized = true;
        isActive = true;

        // Initiate the first peer discovery. Changes will be notified automatically.
        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFailure(int errorCode) {
                Log.e(TAG, String.format("Unable to discover devices. Error code: %d", errorCode));
            }
        });
    }

    public void pause() {
        if (!isActive) {
            throw new WifiP2PConnectionException("ConnectionManager not active.");
        }

        context.unregisterReceiver(receiver);
        isActive = false;
    }

    public void resume() {
        if (isActive) {
            throw new WifiP2PConnectionException("ConnectionManager already active.");
        }

        context.registerReceiver(receiver, receiver.getIntentFilter());
        isActive = true;
    }

    public void shutdown() {
        if (!isInitialized) {
            throw new WifiP2PConnectionException("ConnectionManager not initialized.");
        }

        if (isActive) {
            context.unregisterReceiver(receiver);
        }

        receiver = null;
        isInitialized = false;
        isActive = false;
    }

    public void connectTo(WifiP2pDevice device, final ConnectingListener connectingListener) {
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;

        this.connectingListener = connectingListener;
        manager.connect(channel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFailure(int errorCode) {
                connectingListener.onFailure(errorCode);
                ConnectionManager.this.connectingListener = null;
            }
        });
    }

    public void disconnect(final DisconnectingListener disconnectingListener) {
        manager.stopPeerDiscovery(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                manager.removeGroup(channel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        disconnectingListener.onSuccess();
                    }

                    @Override
                    public void onFailure(int reasonCode) {
                        disconnectingListener.onFailure(reasonCode);
                    }
                });
            }

            @Override
            public void onFailure(int reasonCode) {
                disconnectingListener.onFailure(reasonCode);
            }
        });
    }

    public boolean isHost() {
        return connectionInfo != null
                ? connectionInfo.isGroupOwner
                : false;
    }

    public boolean isConnected() {
        return networkInfo != null && networkInfo.isConnected();
    }

    public List<InetAddress> getDeviceAddresses() {
        Collection<WifiP2pDevice> clients = groupInfo.getClientList();
        List<InetAddress> addresses = new ArrayList<>(clients.size());

        for (WifiP2pDevice client : clients) {
            try {
                addresses.add(InetAddress.getByName(client.deviceAddress));
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }

        return addresses;
    }

    private class WifiEventListenerImpl implements ConnectionBroadcastReceiver.WifiEventListener {

        @Override
        public void onDeviceWifiDirectEnabled() {
            isWifiP2pEnabled = true;
        }

        @Override
        public void onDeviceWifiDirectDisabled() {
            isWifiP2pEnabled = false;
        }

        @Override
        public void onDeviceWifiDirectChanged(WifiP2pDevice wifiP2pDevice) {
            if (eventListener != null) {
                eventListener.onDeviceWifiDirectChanged(wifiP2pDevice);
            }
        }

        @Override
        public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
            if (eventListener != null) {
                eventListener.onPeersAvailable(wifiP2pDeviceList);
            }
        }

        @Override
        public void onConnected(WifiP2pInfo wifiP2pInfo, WifiP2pGroup wifiP2pGroup, NetworkInfo networkInfo) {
            ConnectionManager.this.networkInfo = networkInfo;
            connectionInfo = wifiP2pInfo;
            groupInfo = wifiP2pGroup;
            if (eventListener != null) {
                eventListener.onConnected(wifiP2pInfo);
            }

            if (connectingListener != null) {
                connectingListener.onConnected(wifiP2pInfo);
                connectingListener = null;
            }
        }

        @Override
        public void onDisconnected(NetworkInfo networkInfo) {
            ConnectionManager.this.networkInfo = networkInfo;
            connectionInfo = null;
            groupInfo = null;
            if (eventListener != null) {
                eventListener.onDisconnected();
            }
        }

    }

    public interface EventListener {
        void onDeviceWifiDirectChanged(WifiP2pDevice wifiP2pDevice);

        void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList);

        void onConnected(WifiP2pInfo wifiP2pInfo);

        void onDisconnected();
    }

    public interface ConnectingListener {
        void onConnected(WifiP2pInfo wifiP2pInfo);

        void onFailure(int errorCode);
    }

    public interface DisconnectingListener {
        void onSuccess();

        void onFailure(int errorCode);
    }
}
