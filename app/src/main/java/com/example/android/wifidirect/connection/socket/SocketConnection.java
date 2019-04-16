package com.example.android.wifidirect.connection.socket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import lombok.Getter;
import lombok.Setter;

public abstract class SocketConnection {
    static final int PORT_LISTENER = 7070;
    static final byte FLAG_LOCATION_MESSAGE = 1;

    @Getter
    @Setter
    private EventListener eventListener;

    private DataOutputStream outputStream;

    public void sendMessage(LocationMessage message) {
        if (outputStream != null) {
            try {
                writeToStream(message, outputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    void onMessageReceived(LocationMessage message) {
        if (eventListener != null) {
            eventListener.onMessage(message);
        }
    }

    void onConnectionEstablished(DataOutputStream outputStream) {
        this.outputStream = outputStream;
    }

    void onConnectionClosed() {
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public abstract void open() throws IOException;

    public abstract void close();

    public interface EventListener {
        void onMessage(LocationMessage message);
    }

    static LocationMessage readFromStream(DataInputStream inputStream) throws IOException {
        String senderId = inputStream.readUTF();
        double longitude = inputStream.readDouble();
        double latitude = inputStream.readDouble();

        LocationMessage message = new LocationMessage(senderId, longitude, latitude);
        return message;
    }

    static void writeToStream(LocationMessage message, DataOutputStream outputStream) throws IOException {
        outputStream.writeUTF(message.getDeviceId());
        outputStream.writeDouble(message.getLongitude());
        outputStream.writeDouble(message.getLatitude());
    }
}
