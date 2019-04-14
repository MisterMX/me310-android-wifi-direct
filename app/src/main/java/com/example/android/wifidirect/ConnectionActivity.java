package com.example.android.wifidirect;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.example.android.wifidirect.connection.p2p.ConnectionManager;

import java.util.ArrayList;
import java.util.List;

public class ConnectionActivity extends AppCompatActivity {
    private ConnectionManager manager;
    private ListView deviceListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        deviceListView = findViewById(R.id.connection_device_list);

        manager = new ConnectionManager(this, new ConnectionEventListener());
        manager.initialize();
    }

    @Override
    protected void onPause() {
        super.onPause();
        manager.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!manager.isActive()) {
            manager.resume();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        manager.shutdown();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    private void onDeviceClick(final WifiP2pDevice device) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(String.format("Connect to device '%s'?", device.deviceName));
        builder.setPositiveButton("Connect", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                onConnectToDevice(device);
            }
        });
        builder.setNegativeButton("Cancel", null);

        builder.create().show();
    }

    private void onConnectToDevice(WifiP2pDevice device) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(R.layout.connecting_loading_dialog);

        final AlertDialog dialog = builder.create();
        dialog.show();

        manager.connectTo(device, new ConnectionManager.ConnectingListener() {
            @Override
            public void onConnected(WifiP2pInfo wifiP2pInfo) {
                dialog.dismiss();
                ConnectionActivity.this.getParent().setResult(Activity.RESULT_OK);
                finish();
            }

            @Override
            public void onFailure(int errorCode) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ConnectionActivity.this);
                builder.setMessage(String.format("Unable to connect. Error %d", errorCode));
                builder.setPositiveButton("OK", null);
                builder.create().show();
            }
        });
    }

    private class ConnectionEventListener implements ConnectionManager.EventListener {

        @Override
        public void onDeviceWifiDirectChanged(WifiP2pDevice wifiP2pDevice) {

        }

        @Override
        public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {

            deviceListView.setAdapter(new DeviceListAdapter(
                    ConnectionActivity.this,
                    new ArrayList<>(wifiP2pDeviceList.getDeviceList())));
        }
    }

    private class DeviceListAdapter extends ArrayAdapter<WifiP2pDevice> {
        public DeviceListAdapter(Context context, List<WifiP2pDevice> list) {
            super(context, 0, list);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            final WifiP2pDevice device = getItem(position);
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.activity_connection_item, parent, false);
            }

            TextView textView = convertView.findViewById(R.id.connection_device_list_item_text);
            textView.setText(device.deviceName);

            ImageView imageView = convertView.findViewById(R.id.connection_device_list_item_icon);
            imageView.setImageResource(R.mipmap.ic_launcher_round);

            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onDeviceClick(device);
                }
            });

            return convertView;
        }
    }
}
