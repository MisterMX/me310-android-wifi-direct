package com.example.android.wifidirect.connection.socket;

import java.io.IOException;

import lombok.Getter;
import lombok.Setter;

public abstract class SocketConnection {
    @Getter
    @Setter
    private EventListener eventListener;

    public abstract void open() throws IOException;

    public abstract void sendMessage(String message);

    public abstract void close();

    public interface EventListener {
        void onMessage(String message);
    }
}
