package com.shooter.network;

import com.shooter.shared.util.Constants;

import java.io.Serializable;
import java.util.Arrays;

/**
 * ============================================================
 * FILE: LobbyState.java
 * PACKAGE: network
 * OWNER: Geastin (Member C - Screens / Systems / Assets)
 * ============================================================
 *
 * Serializable snapshot of the lobby.
 * The server sends this as the payload of a LOBBY_STATE message so every
 * client can show the same connected players and ready flags.
 *
 * WHAT BELONGS HERE:
 * - Player names per slot
 * - Ready status per slot
 * - Host player ID
 *
 * WHAT DOES NOT BELONG HERE:
 * - Rendering code
 * - Socket code
 * - Game rules
 * ============================================================
 */
public class LobbyState implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String[] playerNames;
    private final boolean[] readyFlags;
    private final int hostPlayerId;

    /**
     * Creates a lobby snapshot.
     *
     * @param playerNames 4 player names; null means the slot is empty.
     * @param readyFlags  4 ready flags matching the player slots.
     * @param hostPlayerId player ID of the host, or -1 if no host is assigned yet.
     */
    public LobbyState(String[] playerNames, boolean[] readyFlags, int hostPlayerId) {
        this.playerNames = copyNames(playerNames);
        this.readyFlags = copyReadyFlags(readyFlags);
        this.hostPlayerId = hostPlayerId;
    }

    /**
     * @return a copy of the player name slots. Null means an empty slot.
     */
    public String[] getPlayerNames() {
        return Arrays.copyOf(playerNames, playerNames.length);
    }

    /**
     * @return a copy of the ready flags, one per player slot.
     */
    public boolean[] getReadyFlags() {
        return Arrays.copyOf(readyFlags, readyFlags.length);
    }

    /**
     * @return the host player ID, or -1 if no host is assigned.
     */
    public int getHostPlayerId() {
        return hostPlayerId;
    }

    /**
     * @return how many player slots are currently occupied.
     */
    public int getConnectedPlayerCount() {
        int count = 0;
        for (String name : playerNames) {
            if (name != null) {
                count++;
            }
        }
        return count;
    }

    /**
     * @return true only when at least one connected player exists and every
     *         connected player is ready.
     */
    public boolean areConnectedPlayersReady() {
        boolean hasConnectedPlayer = false;

        for (int i = 0; i < playerNames.length; i++) {
            if (playerNames[i] == null) {
                continue;
            }

            hasConnectedPlayer = true;
            if (!readyFlags[i]) {
                return false;
            }
        }

        return hasConnectedPlayer;
    }

    private String[] copyNames(String[] source) {
        String[] copy = new String[Constants.MAX_PLAYERS];
        if (source == null) {
            return copy;
        }

        int limit = Math.min(source.length, copy.length);
        System.arraycopy(source, 0, copy, 0, limit);
        return copy;
    }

    private boolean[] copyReadyFlags(boolean[] source) {
        boolean[] copy = new boolean[Constants.MAX_PLAYERS];
        if (source == null) {
            return copy;
        }

        int limit = Math.min(source.length, copy.length);
        System.arraycopy(source, 0, copy, 0, limit);
        return copy;
    }
}
