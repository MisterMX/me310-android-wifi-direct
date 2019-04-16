package com.example.android.wifidirect;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.example.android.wifidirect.connection.p2p.ConnectionManager;
import com.example.android.wifidirect.connection.socket.LocationMessage;
import com.example.android.wifidirect.connection.socket.ServerSocketConnection;

import java.io.IOException;

public class ConnectedFragment extends Fragment {
    private static final String TAG = ConnectedFragment.class.getName();

    private ConnectionManager connectionManager;
    private ServerSocketConnection serverSocketConnection;

    private LocationManager locationManager;
    private LocationListener locationListener;

    private String deviceId;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        deviceId = Settings.Secure.getString(getContext().getContentResolver(), Settings.Secure.ANDROID_ID);

        locationManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);


        serverSocketConnection = new ServerSocketConnection();
        connectionManager = new ConnectionManager(getContext(), new ConnectionManager.EventListener() {
            @Override
            public void onDeviceWifiDirectChanged(WifiP2pDevice wifiP2pDevice) {

            }

            @Override
            public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {

            }

            @Override
            public void onConnected(WifiP2pInfo wifiP2pInfo) {
                onP2pConnected();
            }

            @Override
            public void onDisconnected() {
                onP2pDisconnected();
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_connected, container);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        connectionManager.initialize();
    }

    @Override
    public void onPause() {
        super.onPause();
        connectionManager.pause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!connectionManager.isActive()) {
            connectionManager.resume();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        connectionManager.shutdown();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.main_actions, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_disconnect) {
            disconnect();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void disconnect() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setView(R.layout.connecting_loading_dialog);

        final AlertDialog progressDialog = builder.create();
        progressDialog.show();

        connectionManager.disconnect(new ConnectionManager.DisconnectingListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Successfully disconnected from P2P device.");
                progressDialog.dismiss();
            }

            @Override
            public void onFailure(int errorCode) {
                Log.d(TAG, String.format("Failed to disconnect from P2P device: Error %d", errorCode));
                progressDialog.dismiss();
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setMessage(String.format("Unable to disconnect. Error %d", errorCode));
                builder.setPositiveButton("OK", null);
                builder.create().show();
            }
        });
    }

    private void onP2pConnected() {
        try {
            serverSocketConnection.open();
            serverSocketConnection.setReceiverAddresses(connectionManager.getDeviceAddresses());
        } catch (IOException e) {
            e.printStackTrace();
        }


        startLocationListener();
    }

    private void onP2pDisconnected() {
        if (serverSocketConnection != null) {
            serverSocketConnection.close();
        }

        stopLocationListener();
    }

    private void startLocationListener() {
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                ConnectedFragment.this.onLocationChanged(location);
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

            }
        };

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 1f, locationListener);
    }

    private void stopLocationListener() {
        if (locationListener != null) {
            locationManager.removeUpdates(locationListener);
        }
    }

    private void onLocationChanged(Location location) {
        try {
            serverSocketConnection.sendMessage(new LocationMessage(deviceId, location.getLongitude(), location.getLatitude()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
