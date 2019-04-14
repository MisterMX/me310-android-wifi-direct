package com.example.android.wifidirect.connection.socket;

import android.os.Bundle;
import android.os.Message;

public class MessageUtil {
    private static final String BUNDLE_TEXT = "BUNDLE_TEXT";

    public static Message createTextMessage(int what, String text) {
        Message message = new Message();
        message.what = what;
        Bundle bundle = new Bundle();
        bundle.putString(BUNDLE_TEXT, text);
        message.setData(bundle);

        return message;
    }

    public static String readTextMessage(Message message) {
        return message.getData().getString(BUNDLE_TEXT);
    }
}
