package com.example.android.wifidirect.connection.socket;

import android.os.AsyncTask;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ServerSocketConnection extends SocketConnection {
    public static final int PORT = 7070;

    private static final String TAG = ServerSocketConnection.class.getName();

    private ServerSocketTask serverSocketTask;
    private List<ClientSocketTask> clientSocketTasks = new ArrayList<>();

    public ServerSocketConnection() {

    }

    @Override
    public void open() {
        ServerSocketTask serverSocketTask = new ServerSocketTask(this);
        serverSocketTask.execute();
    }

    @Override
    public void sendMessage(String message) {
        for (ClientSocketTask clientSocketTask : clientSocketTasks) {
            clientSocketTask.sendMessage(message);
        }
    }

    @Override
    public void close() {
        serverSocketTask.cancel(true);
    }

    private static class ServerSocketTask extends AsyncTask<Void, Socket, Void> {
        private final ServerSocketConnection serverSocketConnection;

        ServerSocketTask(ServerSocketConnection serverSocketConnection) {
            this.serverSocketConnection = serverSocketConnection;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            ServerSocket serverSocket = null;
            try {
                serverSocket = new ServerSocket(PORT);
                while (!isCancelled()) {
                    Socket socket = serverSocket.accept();
                    publishProgress(socket);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Socket... values) {
            Socket socket = values[0];
            Log.d(TAG, String.format("Opened new socket connection with %s", socket.getInetAddress()));

            new ClientSocketTask(serverSocketConnection).execute(socket);
        }
    }

    private static class ClientSocketTask extends AsyncTask<Socket, String, Void> {
        private final ServerSocketConnection serverSocketConnection;

        ClientSocketTask(ServerSocketConnection serverSocketConnection) {
            this.serverSocketConnection = serverSocketConnection;
        }

        private Socket socket;
        private DataInputStream inputStream;
        private DataOutputStream outputStream;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            serverSocketConnection.clientSocketTasks.add(this);
        }

        @Override
        protected Void doInBackground(Socket... sockets) {
            socket = sockets[0];
            try {
                inputStream = new DataInputStream(socket.getInputStream());
                outputStream = new DataOutputStream(socket.getOutputStream());

                while (!socket.isClosed()) {
                    String text = inputStream.readUTF();
                    publishProgress(text);
                }

                inputStream.close();
                outputStream.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            if (serverSocketConnection.getEventListener() != null) {
                serverSocketConnection.getEventListener().onMessage(values[0]);
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            serverSocketConnection.clientSocketTasks.remove(this);
        }

        public void sendMessage(String message) {

            // TODO: Send message in background thread.
        }
    }


}
