package com.example.android.wifidirect.connection;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ConnectionService extends Service {
    public static String ACTION_REQUEST_P2P_DEVICE_LIST = "ACTION_REQUEST_P2P_DEVICE_LIST";
    public static String ACTION_P2P_DEVICE_LIST_CHANGED = "ACTION_P2P_DEVICE_LIST_CHANGED";
    public static String EXTRA_DEVICE_LIST = "EXTRA_DEVICE_LIST";
    public static int MESSAGE_P2P_DEVICE_LIST_CHANGED = 12000;

    private ConnectionManager manager;
    private List<Handler> handlers = new ArrayList<>();

    @Override
    public void onCreate() {
        super.onCreate();

        manager = new ConnectionManager(this, new ConnectionManager.EventListener() {
            @Override
            public void onDeviceWifiDirectChanged(WifiP2pDevice wifiP2pDevice) {

            }

            @Override
            public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
                Message message = new Message();
                message.what = MESSAGE_P2P_DEVICE_LIST_CHANGED;

                for (Handler handler : handlers) {
                    //Message message = new Message();
                    //message.what = MESSAGE_P2P_DEVICE_LIST_CHANGED;


                }
            }
        });
        manager.initialize();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        manager.shutdown();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new LocalBinder();
    }

    public void registerMessageHandler(Handler handler) {
        handlers.add(handler);
    }

    public void unregisterMessageHandler(Handler handler) {
        handlers.remove(handler);
    }

    public class LocalBinder extends Binder {
        public ConnectionService getService() {
            return ConnectionService.this;
        }
    }
}
