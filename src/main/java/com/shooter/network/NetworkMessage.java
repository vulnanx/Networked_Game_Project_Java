package com.shooter.network;

import java.io.Serializable;

/**
 * ============================================================
 * FILE: NetworkMessage.java
 * PACKAGE: network
 * OWNER: Member A (Networking Core)
 * ============================================================
 *
 * RESPONSIBILITY:
 * The standard packet format for ALL messages sent between
 * the client and the server over TCP.
 *
 * HOW IT WORKS:
 * - Every message has a 'type' (what kind of message is this?)
 * - 'playerId' tells the server/client who sent or is being addressed
 * - 'payload' is flexible — it can be an InputSnapshot, a GameState,
 *   a String, or null depending on the message type
 * - 'timestamp' records when the message was created (milliseconds
 *   since epoch) — useful for latency measurement later
 *
 * WHY Serializable?
 * Java's ObjectOutputStream/ObjectInputStream require the objects
 * they send/receive to implement Serializable. This is what allows
 * us to send entire Java objects over the network socket.
 *
 * WHAT NOT TO PUT HERE:
 * - Game logic
 * - Rendering code
 * ============================================================
 */
public class NetworkMessage implements Serializable {

    // serialVersionUID is required for Serializable classes.
    // It ensures that the class on both sides of the network is compatible.
    private static final long serialVersionUID = 1L;

    private final MessageType type;      // What kind of message is this?
    private final int playerId;          // Which player sent / is targeted by this message
    private final Object payload;        // The data attached to this message (can be null)
    private final long timestamp;        // When this message was created (ms since epoch)

    /**
     * Creates a new NetworkMessage with the current timestamp.
     *
     * @param type     the message type (e.g. CONNECT, PING, GAME_STATE)
     * @param playerId the player who sent this message (0–3), or -1 for server-only messages
     * @param payload  the data attached (can be null, a String, InputSnapshot, GameState, etc.)
     */
    public NetworkMessage(MessageType type, int playerId, Object payload) {
        this.type = type;
        this.playerId = playerId;
        this.payload = payload;
        this.timestamp = System.currentTimeMillis(); // Record the exact creation time
    }

    // --- Getters ---
    // We only provide getters (no setters) because messages are immutable —
    // once created, a message should not change.

    /**
     * @return the type of this message (what it means / what to do with it)
     */
    public MessageType getType() {
        return type;
    }

    /**
     * @return the player ID associated with this message (0–3, or -1 for server)
     */
    public int getPlayerId() {
        return playerId;
    }

    /**
     * @return the payload object — cast to the expected type based on message type.
     *         For example, for INPUT messages, cast to InputSnapshot.
     *         For GAME_STATE messages, cast to GameState.
     */
    public Object getPayload() {
        return payload;
    }

    /**
     * @return the time this message was created, in milliseconds since epoch.
     *         Useful for measuring network latency.
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Useful for debugging — prints the message type, player, and timestamp.
     */
    @Override
    public String toString() {
        return "NetworkMessage{type=" + type
             + ", playerId=" + playerId
             + ", timestamp=" + timestamp
             + ", payload=" + (payload != null ? payload.getClass().getSimpleName() : "null")
             + "}";
    }
}
