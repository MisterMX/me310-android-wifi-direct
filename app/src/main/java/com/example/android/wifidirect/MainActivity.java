package com.example.android.wifidirect;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

import com.example.android.wifidirect.connection.p2p.ConnectionManager;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_REQUIRE_PERMISSIONS = 1020;

    private ConnectionManager connectionManager;

    private ConnectedFragment connectedFragment;
    private DisconnectedFragment disconnectedFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        disconnectedFragment = (DisconnectedFragment) getSupportFragmentManager().findFragmentById(R.id.main_disconnected);
        connectedFragment = (ConnectedFragment) getSupportFragmentManager().findFragmentById(R.id.main_connected);

        requestPermissions(Manifest.permission.ACCESS_FINE_LOCATION);
    }

    @Override
    protected void onStart() {
        super.onStart();

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

    private void updateUi() {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);

        if (connectionManager.isConnected()) {
            transaction.show(connectedFragment);
            transaction.hide(disconnectedFragment);
        } else {
            transaction.hide(connectedFragment);
            transaction.show(disconnectedFragment);
        }

        transaction.commitAllowingStateLoss();

        supportInvalidateOptionsMenu();
    }

    private void requestPermissions(String... permissions) {
        List<String> requiredPermissions = new ArrayList<>();

        for (String permission : permissions) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                requiredPermissions.add(permission);
            }
        }

        if (!requiredPermissions.isEmpty()) {
            String[] permissionsRequest = new String[requiredPermissions.size()];
            permissionsRequest = requiredPermissions.toArray(permissionsRequest);

            requestPermissions(permissionsRequest, REQUEST_CODE_REQUIRE_PERMISSIONS);
        }
    }

    private class ConnectionEventListener implements ConnectionManager.EventListener {

        @Override
        public void onDeviceWifiDirectChanged(WifiP2pDevice wifiP2pDevice) {
            updateUi();
        }

        @Override
        public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
        }

        @Override
        public void onConnected(WifiP2pInfo wifiP2pInfo) {
            updateUi();
        }

        @Override
        public void onDisconnected() {
            updateUi();
        }
    }
}
