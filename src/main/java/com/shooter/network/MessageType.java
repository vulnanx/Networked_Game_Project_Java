package com.shooter.network;

/**
 * ============================================================
 * FILE: MessageType.java
 * PACKAGE: network
 * SHARED — all members use this file
 * ============================================================
 *
 * RESPONSIBILITY:
 * Defines every type of message that can be sent between
 * the client and the server. Think of each value as a
 * "subject line" for a NetworkMessage packet.
 *
 * HOW TO READ THIS:
 * - Connection phase: CONNECT, CONNECTED, DISCONNECT
 * - Lobby phase:      LOBBY_STATE
 * - In-game sending:  INPUT (client → server), PING
 * - In-game receiving: GAME_STATE (server → clients)
 * - Game events:      ROUND_START, ROUND_CLEAR, GAME_OVER, PAUSE
 * - Bonus:            CHAT
 * - Errors:           ERROR
 * ============================================================
 */
public enum MessageType {

    // ── Connection lifecycle ──────────────────────────────────
    /** Client → Server: "I want to join." */
    CONNECT,

    /** Server → Client: "You are connected, here is your player ID." */
    CONNECTED,

    /** Either side: "I am leaving the game." */
    DISCONNECT,

    // ── Lobby ────────────────────────────────────────────────
    /** Server → All Clients: current lobby state (player list + ready flags). */
    LOBBY_STATE,

    /** Client → Server: this player changed their ready status. Payload = Boolean. */
    READY_STATUS,

    /** Host Client → Server → All Clients: start the game now. */
    START_GAME,

    // ── In-game input / state ─────────────────────────────────
    /** Client → Server: input snapshot (WASD + facing direction + shoot flag). */
    INPUT,

    /** Server → All Clients: full authoritative game state. Sent 20 times/sec. */
    GAME_STATE,

    // ── Game events ───────────────────────────────────────────
    /** Server → All Clients: a new round has begun. Payload = round number. */
    ROUND_START,

    /** Server → All Clients: current round is cleared. */
    ROUND_CLEAR,

    /** Server → All Clients: the game has ended. Payload = GameOverStats. */
    GAME_OVER,

    /** Either side: game is paused or unpaused. */
    PAUSE,
    POWER_UP_COLLECTED,

    // ── Legacy / keep for compatibility ──────────────────────
    /** Client → Server: player movement (legacy — replaced by INPUT in Day 2). */
    MOVE,

    /** Client → Server: player shoot action (legacy — replaced by INPUT in Day 2). */
    SHOOT,

    // ── Diagnostic ────────────────────────────────────────────
    /** Either side: connectivity check. */
    PING,

    // ── Bonus feature ─────────────────────────────────────────
    /** Either side: a chat message. Payload = String. */
    CHAT,

    // ── Error handling ────────────────────────────────────────
    /** Server → Client: something went wrong. Payload = error String. */
    ERROR
}
