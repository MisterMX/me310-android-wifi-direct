package com.example.android.wifidirect.connection.socket;

import android.os.AsyncTask;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

public class ClientSocketConnection extends SocketConnection {
    private static final String TAG = ClientSocketConnection.class.getName();

    private final InetAddress serverAddress;

    private ClientSocketTask clientSocketTask;

    public ClientSocketConnection(InetAddress serverAddress) {
        this.serverAddress = serverAddress;
    }

    @Override
    public void open() {
        clientSocketTask = new ClientSocketTask(this);
        clientSocketTask.execute();
    }

    @Override
    public void close() {
        if (clientSocketTask != null) {
            clientSocketTask.forceCloseSocket();
            clientSocketTask.cancel(true);
            clientSocketTask = null;
        }
    }

    private static class ClientSocketTask extends AsyncTask<Void, Object, Void> {
        private final ClientSocketConnection clientSocketConnection;
        private Socket socket;

        ClientSocketTask(ClientSocketConnection clientSocketConnection) {
            this.clientSocketConnection = clientSocketConnection;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                while (!isCancelled()) {
                    try {
                        socket = new Socket(clientSocketConnection.serverAddress, PORT_LISTENER);

                        DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                        publishProgress(outputStream);

                        DataInputStream inputStream = new DataInputStream(socket.getInputStream());
                        while (!isCancelled()) {
                            byte flag = inputStream.readByte();
                            if (flag == FLAG_LOCATION_MESSAGE) {
                                LocationMessage locationMessage = readFromStream(inputStream);
                                publishProgress(locationMessage);
                            }
                        }

                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        Log.d(TAG, "Connection to socket failed. Retrying in 5 seconds.");
                        Thread.sleep(5000);
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Object... values) {
            if (values[0] instanceof DataOutputStream) {
                clientSocketConnection.onConnectionEstablished((DataOutputStream) values[0]);
            } else if (values[0] instanceof LocationMessage) {
                clientSocketConnection.onMessageReceived((LocationMessage) values[0]);
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            clientSocketConnection.onConnectionClosed();
        }

        public void forceCloseSocket() {
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
