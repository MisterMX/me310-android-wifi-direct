package com.example.android.wifidirect.connection.socket;

import java.io.IOException;
import java.net.InetAddress;

import lombok.Getter;
import lombok.Setter;

public abstract class SocketConnection {
    @Getter
    @Setter
    private EventListener eventListener;

    public abstract void open() throws IOException;

    public abstract void sendMessage(LocationMessage message) throws IOException;

    public abstract void close();

    public interface EventListener {
        void onMessage(LocationMessage message);
    }
}
