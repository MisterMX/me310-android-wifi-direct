package com.example.android.wifidirect.connection.socket;

import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import lombok.Getter;
import lombok.Setter;

public abstract class SocketConnection {
    private static final String TAG = SocketConnection.class.getName();

    static final int PORT_LISTENER = 7070;
    static final byte FLAG_LOCATION_MESSAGE = 1;

    @Getter
    @Setter
    private EventListener eventListener;

    private DataOutputStream outputStream;

    public void sendMessage(LocationMessage message) {
        if (outputStream != null) {
            Log.d(TAG, "Sending location message");
            try {
                writeToStream(message, outputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Log.w(TAG, "Unable to send message due to closed socket connection.");
        }

    }

    void onMessageReceived(LocationMessage message) {
        Log.d(TAG, "Received location message");
        if (eventListener != null) {
            eventListener.onMessage(message);
        }
    }

    void onConnectionEstablished(DataOutputStream outputStream) {
        Log.d(TAG, "Connection established");
        this.outputStream = outputStream;
    }

    void onConnectionClosed() {
        if (outputStream != null) {
            Log.d(TAG, "Connection closed");
            try {
                outputStream.close();
                outputStream = null;
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
        outputStream.flush();
    }
}
