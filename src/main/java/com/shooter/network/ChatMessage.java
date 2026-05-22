package com.shooter.network;

import java.io.Serializable;

/**
 * Represents a chat message sent between players or by the server system.
 */
public class ChatMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String sender;
    private final String text;
    private final long timestamp;

    public ChatMessage(String sender, String text, long timestamp) {
        this.sender = sender;
        this.text = text;
        this.timestamp = timestamp;
    }

    public String getSender() {
        return sender;
    }

    public String getText() {
        return text;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
