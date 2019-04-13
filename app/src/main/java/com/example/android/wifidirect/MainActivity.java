package com.example.android.wifidirect;

import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.View;

import com.example.android.wifidirect.connection.ConnectionManager;

public class MainActivity extends AppCompatActivity {
    private ConnectionManager connectionManager;

    private ConnectedFragment connectedFragment;
    private UnconnectedFragment unconnectedFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        connectedFragment = (ConnectedFragment)getSupportFragmentManager().findFragmentById(R.id.main_connected);
        unconnectedFragment = (UnconnectedFragment)getSupportFragmentManager().findFragmentById(R.id.main_unconnected);

        connectionManager = new ConnectionManager(this, new ConnectionEventListener());
        connectionManager.initialize();

        updateUi();
    }

    @Override
    protected void onPause() {
        super.onPause();
        connectionManager.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!connectionManager.isActive()) {
            connectionManager.resume();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        connectionManager.shutdown();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_actions, menu);
        return true;
    }

    private void updateUi() {
        if (connectionManager.isConnected()) {
            connectedFragment.getView().setVisibility(View.VISIBLE);
            unconnectedFragment.getView().setVisibility(View.GONE);
        } else {
            connectedFragment.getView().setVisibility(View.GONE);
            unconnectedFragment.getView().setVisibility(View.VISIBLE);
        }

        invalidateOptionsMenu();
    }

    private class ConnectionEventListener implements ConnectionManager.EventListener {

        @Override
        public void onDeviceWifiDirectChanged(WifiP2pDevice wifiP2pDevice) {
            updateUi();
        }

        @Override
        public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {

        }
    }
}
