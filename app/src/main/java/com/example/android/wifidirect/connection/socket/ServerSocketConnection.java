package com.example.android.wifidirect.connection.socket;

import android.os.AsyncTask;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

public class ServerSocketConnection extends SocketConnection {
    private static final String TAG = ServerSocketConnection.class.getName();
    private static final int PORT_LISTENER = 7070;
    private static final int PORT_SENDER = 7071;
    private static final int MESSAGE_BUFFER_SIZE = 512;
    private static final String MULTICAST_ADDRESS = "228.0.0.0";

    private ReceiverSocketTask serverSocketTask;
    private MulticastSocket senderSocket;

    @Getter
    @Setter
    @NonNull
    private List<InetAddress> receiverAddresses = new ArrayList<>();

    public ServerSocketConnection() {
    }

    @Override
    public void open() throws IOException {
        //this.targetAddress = targetAddress;

        serverSocketTask = new ReceiverSocketTask(this);
        serverSocketTask.execute();

        //senderSocket = new DatagramSocket(PORT_SENDER, targetAddress);

        senderSocket = new MulticastSocket(PORT_SENDER);
        senderSocket.joinGroup(InetAddress.getByName(MULTICAST_ADDRESS));
    }

    @Override
    public void sendMessage(LocationMessage message) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(MESSAGE_BUFFER_SIZE);
        DataOutputStream outputStream = new DataOutputStream(byteArrayOutputStream);
        outputStream.writeUTF(message.getDeviceId());
        outputStream.writeDouble(message.getLongitude());
        outputStream.writeDouble(message.getLatitude());

        byte[] senderData = byteArrayOutputStream.toByteArray();

        DatagramPacket senderPacket = new DatagramPacket(senderData, senderData.length);

//        for (InetAddress receiverAddress : receiverAddresses) {
//            DatagramSocket socket = new DatagramSocket(PORT_LISTENER, receiverAddress);
//            socket.send(senderPacket);
//            socket.close();
//        }

        senderSocket.send(senderPacket);
    }

    @Override
    public void close() {
        if (serverSocketTask != null) {
            serverSocketTask.cancel(true);
        }
    }

    private static class ReceiverSocketTask extends AsyncTask<Void, LocationMessage, Void> {
        private final ServerSocketConnection serverSocketConnection;

        ReceiverSocketTask(ServerSocketConnection serverSocketConnection) {
            this.serverSocketConnection = serverSocketConnection;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                MulticastSocket listenerSocket = new MulticastSocket(PORT_LISTENER);
                listenerSocket.joinGroup(InetAddress.getByName(MULTICAST_ADDRESS));

                //DatagramSocket listenerSocket = new DatagramSocket(PORT_LISTENER);

                byte[] receiverBuffer = new byte[MESSAGE_BUFFER_SIZE];
                DatagramPacket receivedPacket = new DatagramPacket(receiverBuffer, receiverBuffer.length);
                while (!isCancelled()) {
                    listenerSocket.receive(receivedPacket);

                    DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(receivedPacket.getData()));
                    String senderId = inputStream.readUTF();
                    double longitude = inputStream.readDouble();
                    double latitude = inputStream.readDouble();
                    inputStream.close();

                    LocationMessage message = new LocationMessage(senderId, longitude, latitude);
                    publishProgress(message);
                }

                listenerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(LocationMessage... values) {
            if (serverSocketConnection.getEventListener() != null) {
                serverSocketConnection.getEventListener().onMessage(values[0]);
            }
        }
    }
}
