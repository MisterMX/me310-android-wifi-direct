package com.example.android.wifidirect;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


public class UnconnectedFragment extends Fragment {
    private static final int ACTIVITY_REQUEST_CONNECTION = 100;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_connected, container);

        return view;
    }

    public void onConnectButtonClicked(View view) {
        Intent intent = new Intent(getContext(), ConnectionActivity.class);
        startActivityForResult(intent, ACTIVITY_REQUEST_CONNECTION);
    }
}
