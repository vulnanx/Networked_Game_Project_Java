package com.shooter.network;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * ============================================================
 * FILE: GameOverStats.java
 * PACKAGE: network
 * OWNER: Member A (Networking Core)
 * ============================================================
 * 
 * RESPONSIBILITY:
 * Holds the final statistics of a completed game, whether it
 * was a victory or a defeat. Sent by the server to all clients
 * when the game ends.
 * ============================================================
 */
public class GameOverStats implements Serializable {

    private static final long serialVersionUID = 1L;

    private final boolean victory;
    private final int roundsCleared;
    private final int totalKills;
    private final Map<Integer, Integer> playerKills; // Player ID -> kills

    public GameOverStats(boolean victory, int roundsCleared, int totalKills, Map<Integer, Integer> playerKills) {
        this.victory = victory;
        this.roundsCleared = roundsCleared;
        this.totalKills = totalKills;
        this.playerKills = new HashMap<>(playerKills != null ? playerKills : new HashMap<>());
    }

    public boolean isVictory() {
        return victory;
    }

    public int getRoundsCleared() {
        return roundsCleared;
    }

    public int getTotalKills() {
        return totalKills;
    }

    public Map<Integer, Integer> getPlayerKills() {
        return playerKills;
    }
}
