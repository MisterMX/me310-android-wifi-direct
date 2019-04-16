package com.example.android.wifidirect.connection.socket;

import android.os.AsyncTask;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

public class ClientSocketConnection extends SocketConnection {
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
        clientSocketTask.cancel(true);
    }

    private static class ClientSocketTask extends AsyncTask<Void, Object, Void> {
        private final ClientSocketConnection clientSocketConnection;

        ClientSocketTask(ClientSocketConnection clientSocketConnection) {
            this.clientSocketConnection = clientSocketConnection;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                Socket socket = new Socket(clientSocketConnection.serverAddress, PORT_LISTENER);

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
    }
}
