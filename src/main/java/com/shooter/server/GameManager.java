package com.shooter.server;

import com.shooter.shared.model.GameState;

/**
 * Controls game logic (SERVER SIDE).
 *
 * NOT REQUIRED for Milestone 1 yet.
 */
public class GameManager {

    private GameState gameState;

    public GameManager() {
        gameState = new GameState();
    }

    public void update() {
        // TODO:
        // - Update players
        // - Update enemies
        // - Handle collisions
        // - Handle rounds
    }

    public GameState getGameState() {
        return gameState;
    }
}