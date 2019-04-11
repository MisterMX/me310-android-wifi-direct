/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.wifidirect;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.android.wifidirect.DeviceListFragment.DeviceActionListener;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import static android.content.Context.LOCATION_SERVICE;

/**
 * A fragment that manages a particular peer and allows interaction with device
 * i.e. setting up network connection and transferring data.
 */
public class DeviceDetailFragment extends Fragment implements ConnectionInfoListener {

    protected static final int CHOOSE_FILE_RESULT_CODE = 20;
    private View mContentView = null;
    private WifiP2pDevice device;
    private WifiP2pInfo info;
    private Handler serverHandler;
    private Handler clientHandler;
    ProgressDialog progressDialog = null;

    private static final long LOCATION_REFRESH_TIME = 1000;
    private static final float LOCATION_REFRESH_DISTANCE = 0.5f;

    private int counterReceivedMessages = 0;


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mContentView = inflater.inflate(R.layout.device_detail, null);
        mContentView.findViewById(R.id.btn_connect).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;
                config.wps.setup = WpsInfo.PBC;
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                progressDialog = ProgressDialog.show(getActivity(), "Press back to cancel",
                        "Connecting to :" + device.deviceAddress, true, true
//                        new DialogInterface.OnCancelListener() {
//
//                            @Override
//                            public void onCancel(DialogInterface dialog) {
//                                ((DeviceActionListener) getActivity()).cancelDisconnect();
//                            }
//                        }
                );
                ((DeviceActionListener) getActivity()).connect(config);

            }
        });

        mContentView.findViewById(R.id.btn_disconnect).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        ((DeviceActionListener) getActivity()).disconnect();
                    }
                });

        mContentView.findViewById(R.id.btn_start_client).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        // Allow user to pick an image from Gallery or other
                        // registered apps
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("image/*");
                        startActivityForResult(intent, CHOOSE_FILE_RESULT_CODE);
                    }
                });

        mContentView.findViewById(R.id.btn_client_send_message).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Message message = createTextMessage(STATE_SEND_MESSAGE, "Hello world!");
                clientHandler.sendMessage(message);
            }
        });

        return mContentView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        // User has picked an image. Transfer it to group owner i.e peer using
        // FileTransferService.
        Uri uri = data.getData();
        TextView statusText = (TextView) mContentView.findViewById(R.id.status_text);
        statusText.setText("Sending: " + uri);
        Log.d(WiFiDirectActivity.TAG, "Intent----------- " + uri);
        Intent serviceIntent = new Intent(getActivity(), FileTransferService.class);
        serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
        serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH, uri.toString());
        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
                info.groupOwnerAddress.getHostAddress());
        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, 8988);
        getActivity().startService(serviceIntent);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        this.info = info;
        this.getView().setVisibility(View.VISIBLE);

        // The owner IP is now known.
        TextView view = (TextView) mContentView.findViewById(R.id.group_owner);
        view.setText(getResources().getString(R.string.group_owner_text)
                + ((info.isGroupOwner == true) ? getResources().getString(R.string.yes)
                : getResources().getString(R.string.no)));

        // InetAddress from WifiP2pInfo struct.
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText("Group Owner IP - " + info.groupOwnerAddress.getHostAddress());

        // After the group negotiation, we assign the group owner as the file
        // server. The file server is single threaded, single connection server
        // socket.
        if (info.groupFormed && info.isGroupOwner) {
//            new FileServerAsyncTask(getActivity(), mContentView.findViewById(R.id.status_text))
//                    .execute();
            serverHandler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case STATE_RECEIVED_MESSAGE:
                            Log.d("test", "Received message on main thread: " + readStringFromMessage(msg));
                            ((TextView) mContentView.findViewById(R.id.txt_server_received_messages)).setText("Received messages: " + counterReceivedMessages++);
                            break;

                        case STATE_CLIENT_CONNECTED:
                            Log.d("test", "Client connected");
                            ((TextView) mContentView.findViewById(R.id.txt_server_client_connected)).setVisibility(View.VISIBLE);
                            break;

                        case STATE_CLIENT_DISCONNECTED:
                            Log.d("test", "Client disconnected");
                            ((TextView) mContentView.findViewById(R.id.txt_server_client_connected)).setVisibility(View.GONE);
                            break;
                    }
                }
            };

            new Thread(new ServerTask()).start();

            mContentView.findViewById(R.id.txt_server_received_messages).setVisibility(View.VISIBLE);
        } else if (info.groupFormed) {

            HandlerThread handlerThread = new HandlerThread("serverHandler-thread");
            handlerThread.start();

            ClientWorker clientWorker = new ClientWorker(info.groupOwnerAddress);
            clientHandler = new Handler(handlerThread.getLooper(), clientWorker);
            clientHandler.sendEmptyMessage(STATE_OPEN_SOCKET);

            LocationManager locationManager = (LocationManager) getContext().getSystemService(LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_REFRESH_TIME, LOCATION_REFRESH_DISTANCE, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    String str = String.format("%d;%d", location.getLongitude(), location.getLatitude());
                    Message message = createTextMessage(STATE_SEND_MESSAGE, str);

                    clientHandler.sendMessage(message);
                    Log.d("Test", "Location update " + str);
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
            });

            mContentView.findViewById(R.id.btn_client_send_message).setVisibility(View.VISIBLE);
        }

        // hide the connect button
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.GONE);
    }

    /**
     * Updates the UI with device data
     *
     * @param device the device to be displayed
     */
    public void showDetails(WifiP2pDevice device) {
        this.device = device;
        this.getView().setVisibility(View.VISIBLE);
        TextView view = (TextView) mContentView.findViewById(R.id.device_address);
        view.setText(device.deviceAddress);
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText(device.toString());

    }

    /**
     * Clears the UI fields after a disconnect or direct mode disable operation.
     */
    public void resetViews() {
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.VISIBLE);
        TextView view = (TextView) mContentView.findViewById(R.id.device_address);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.group_owner);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.status_text);
        view.setText(R.string.empty);
        mContentView.findViewById(R.id.btn_start_client).setVisibility(View.GONE);
        this.getView().setVisibility(View.GONE);
    }

    /**
     * A simple server socket that accepts connection and writes some data on
     * the stream.
     */
    public static class FileServerAsyncTask extends AsyncTask<Void, Void, String> {

        private Context context;
        private TextView statusText;

        /**
         * @param context
         * @param statusText
         */
        public FileServerAsyncTask(Context context, View statusText) {
            this.context = context;
            this.statusText = (TextView) statusText;
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                ServerSocket serverSocket = new ServerSocket(8988);
                Log.d(WiFiDirectActivity.TAG, "Server: Socket opened");
                Socket client = serverSocket.accept();
                Log.d(WiFiDirectActivity.TAG, "Server: connection done");
                final File f = new File(context.getExternalFilesDir("received"),
                        "wifip2pshared-" + System.currentTimeMillis()
                                + ".jpg");

                File dirs = new File(f.getParent());
                if (!dirs.exists())
                    dirs.mkdirs();
                f.createNewFile();

                Log.d(WiFiDirectActivity.TAG, "server: copying files " + f.toString());
                InputStream inputstream = client.getInputStream();
                copyFile(inputstream, new FileOutputStream(f));
                serverSocket.close();
                return f.getAbsolutePath();
            } catch (IOException e) {
                Log.e(WiFiDirectActivity.TAG, e.getMessage());
                return null;
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                statusText.setText("File copied - " + result);

                File recvFile = new File(result);
                Uri fileUri = FileProvider.getUriForFile(
                        context,
                        "com.example.android.wifidirect.fileprovider",
                        recvFile);
                Intent intent = new Intent();
                intent.setAction(android.content.Intent.ACTION_VIEW);
                intent.setDataAndType(fileUri, "image/*");
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                context.startActivity(intent);
            }

        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {
            statusText.setText("Opening a server socket");
        }

    }

    public static boolean copyFile(InputStream inputStream, OutputStream out) {
        byte buf[] = new byte[1024];
        int len;
        try {
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);

            }
            out.close();
            inputStream.close();
        } catch (IOException e) {
            Log.d(WiFiDirectActivity.TAG, e.toString());
            return false;
        }
        return true;
    }


    private static final int PORT_SERVER = 7077;
    private static final int STATE_RECEIVED_MESSAGE = 1010;
    private static final int STATE_CLIENT_CONNECTED = 2000;
    private static final int STATE_CLIENT_DISCONNECTED = 2001;

    private static final int STATE_OPEN_SOCKET = 1112;
    private static final int STATE_SEND_MESSAGE = 1111;


    public class ServerTask implements Runnable {
        @Override
        public void run() {
            ServerSocket serverSocket = null;
            try {
                serverSocket = new ServerSocket(PORT_SERVER);
                serverSocket.setSoTimeout(10000);

                while (true) {
                    final Socket client = serverSocket.accept();
                    client.setKeepAlive(true);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            BufferedReader reader = null;
                            try {
                                serverHandler.sendEmptyMessage(STATE_CLIENT_CONNECTED);
                                DataInputStream dataInputStream = new DataInputStream(client.getInputStream());
                                //reader = new BufferedReader(new InputStreamReader(client.getInputStream()));

                                while (true) {


//                                    if (val != -1) {
//                                        Message message = createTextMessage(STATE_RECEIVED_MESSAGE, String.valueOf(val));
//                                        serverHandler.sendMessage(message);
//                                    }

                                    byte b = dataInputStream.readByte();
                                    if (b == 10) {
                                        String line = dataInputStream.readUTF();

                                        Message message = createTextMessage(STATE_RECEIVED_MESSAGE, line);
                                        serverHandler.sendMessage(message);

//                                    int val = reader.read();
//                                    Message message = createTextMessage(STATE_RECEIVED_MESSAGE, String.valueOf(val));
//                                    serverHandler.sendMessage(message);
                                    }
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            } finally {
                                serverHandler.sendEmptyMessage(STATE_CLIENT_DISCONNECTED);
                                if (reader != null) {
                                    try {
                                        reader.close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    }).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (serverSocket != null) {
                        serverSocket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    public class ClientWorker implements Handler.Callback {


        //private PrintWriter writer;
        private final InetAddress serverAddress;

        private Socket socket;
        private DataOutputStream dataOutputStream;


        public ClientWorker(InetAddress serverAddress) {
            this.serverAddress = serverAddress;
        }

        @Override
        public boolean handleMessage(Message message) {
            try {
                switch (message.what) {
                    case STATE_OPEN_SOCKET:
                        socket = new Socket();
                        socket.bind(null);
                        socket.connect(new InetSocketAddress(serverAddress, PORT_SERVER));
                        socket.setSoTimeout(10000);
                        socket.setKeepAlive(true);
                        dataOutputStream = new DataOutputStream(socket.getOutputStream());
                        dataOutputStream.writeByte(1);
                        break;

                    case STATE_SEND_MESSAGE:
                        String str = readStringFromMessage(message);

                        dataOutputStream.writeByte(10);
                        dataOutputStream.writeUTF(str);
                        dataOutputStream.flush();
                        break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
//            } finally {
//                try {
//                    if (socket != null) {
//                        socket.close();
//                    }
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }

            return true;
        }


    }

    private static Message createTextMessage(int what, String text) {
        Message message = new Message();
        message.what = what;

        Bundle bundle = new Bundle();
        bundle.putString("msg", text);

        message.setData(bundle);

        return message;
    }

    private static String readStringFromMessage(Message message) {
        return message.getData().getString("msg");
    }
}
