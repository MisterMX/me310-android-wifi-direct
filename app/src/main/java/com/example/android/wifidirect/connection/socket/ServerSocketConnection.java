package com.example.android.wifidirect.connection.socket;

import android.os.AsyncTask;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerSocketConnection extends SocketConnection {
    private static final String TAG = ServerSocketConnection.class.getName();

    private ServerSocketTask serverSocketTask;

    public ServerSocketConnection() {
    }

    @Override
    public void open() {
        serverSocketTask = new ServerSocketTask(this);
        serverSocketTask.execute();
    }

    @Override
    public void close() {
        if (serverSocketTask != null) {
            serverSocketTask.cancel(true);
        }
    }

    private static class ServerSocketTask extends AsyncTask<Void, Object, Void> {
        private final ServerSocketConnection serverSocketConnection;

        ServerSocketTask(ServerSocketConnection serverSocketConnection) {
            this.serverSocketConnection = serverSocketConnection;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                ServerSocket serverSocket = new ServerSocket(PORT_LISTENER);

                while (!isCancelled()) {
                    Socket socket = serverSocket.accept();

                    DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                    publishProgress(outputStream);

                    DataInputStream inputStream = new DataInputStream(socket.getInputStream());

                    while (!isCancelled()) {
                        byte flag = inputStream.readByte();
                        if (flag == 1) {
                            LocationMessage message = readFromStream(inputStream);
                            publishProgress(message);
                        }
                    }

                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Object... values) {
            if (values[0] instanceof DataOutputStream) {
                serverSocketConnection.onConnectionEstablished((DataOutputStream)values[0]);
            } else if (values[0] instanceof LocationMessage) {
                serverSocketConnection.onMessageReceived((LocationMessage)values[0]);
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            serverSocketConnection.onConnectionClosed();
        }
    }
}
