package com.shooter.server;

import com.shooter.shared.model.GameState;
import com.shooter.shared.model.Player;
import com.shooter.shared.model.Bullet;
import com.shooter.shared.util.Direction;
import com.shooter.network.InputSnapshot;

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
        // --- SHARED TICK LOGIC ---
        // Every tick, we process things that happen automatically (bullets flying, enemies walking)
        
        // Update players (cooldowns only, movement is handled by handleInput)
        for (Player p : gameState.getPlayers()) {
            p.tickCooldown();
        }

        // Update bullets
        for (Bullet b : gameState.getBullets()) {
            b.update();
        }
        
        // Remove expired bullets
        gameState.removeExpiredBullets();

        // TODO:
        // - Update enemies (Member C)
        // - Handle collisions (Sophia/Geastin)
        // - Handle rounds (Member C)
    }

    /**
     * Authoritative logic: apply a client's input snapshot to their player.
     * Called by ClientHandler on the server side when a network packet arrives.
     */
    public void handleInput(int playerId, InputSnapshot snapshot) {
        if (snapshot == null) return;

        Player p = gameState.getPlayerById(playerId);
        if (p == null || !p.isAlive()) return;

        // --- MOVEMENT ---
        // Apply WASD independently to allow for diagonal movement
        if (snapshot.up)    p.move(Direction.UP);
        if (snapshot.down)  p.move(Direction.DOWN);
        if (snapshot.left)  p.move(Direction.LEFT);
        if (snapshot.right) p.move(Direction.RIGHT);

        // --- SHOOTING ---
        if (snapshot.shooting) {
            Bullet b = p.shoot();
            if (b != null) {
                gameState.addBullet(b);
            }
        }
    }

    public GameState getGameState() {
        return gameState;
    }
}