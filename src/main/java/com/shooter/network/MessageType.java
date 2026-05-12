package com.shooter.network;

/**
 * Types of messages exchanged between client and server.
 * (Used in Milestone 2)
 */
public enum MessageType {

    CONNECT,
    CONNECTED,
    DISCONNECT,
    INPUT,
    MOVE,
    SHOOT,
    GAME_STATE,
    CHAT
}