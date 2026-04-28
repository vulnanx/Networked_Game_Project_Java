package com.shooter.network;

import java.io.Serializable;

/**
 * ============================================================
 * FILE: Message.java
 * PACKAGE: network
 * OWNER: Member C (Systems / Networking)
 * ============================================================
 *
 * RESPONSIBILITY:
 *   Represents ANY message sent between client and server.
 *
 *   Examples:
 *     - Player movement
 *     - Shooting action
 *     - Full game state update
 *
 * WHAT TO ADD HERE:
 *   - Specific message payload objects (MoveData, ShootData)
 *
 * WHAT NOT TO PUT HERE:
 *   - Game logic
 *   - Rendering logic
 *
 * CONNECTS TO:
 *   GameClient (sends messages)
 *   GameServer (receives messages)
 * ============================================================
 */
public class Message implements Serializable {

    private MessageType type;
    private int playerId;
    private Object payload;

    public Message(MessageType type, int playerId, Object payload) {
        this.type = type;
        this.playerId = playerId;
        this.payload = payload;
    }

    public MessageType getType() { return type; }
    public int getPlayerId() { return playerId; }
    public Object getPayload() { return payload; }
}