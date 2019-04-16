package com.example.android.wifidirect;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.android.wifidirect.connection.p2p.ConnectionManager;

public class DisconnectedFragment extends Fragment {
    private static final int ACTIVITY_REQUEST_CONNECTION = 100;

    private ConnectionManager connectionManager;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_disconnected, container);

        Button connectButton = view.findViewById(R.id.main_btn_connect);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onConnectButtonClicked();
            }
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        connectionManager = new ConnectionManager(getContext(), null);
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

    public void onConnectButtonClicked() {
        Intent intent = new Intent(getActivity(), ConnectionActivity.class);
        getActivity().startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }
}
