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
    private EntityManager entityManager;
    private RoundManager roundManager;

    public GameManager() {
        gameState = new GameState();
        entityManager = new EntityManager();
        roundManager = new RoundManager();
        
        // Start Round 1
        roundManager.startCurrentRound(entityManager);
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
        
        // Handle Collisions
        for (Bullet bullet : gameState.getBullets()) {
            if (bullet.isFromEnemy()) {
                for (Player player : gameState.getPlayers()) {
                    if (player.isAlive() && com.shooter.shared.logic.CollisionDetector.bulletHitsPlayer(bullet, player)) {
                        player.takeDamage(bullet.getDamage());
                        bullet.expire();
                        // Break to only hit one player
                        break;
                    }
                }
            } else {
                for (com.shooter.shared.model.Enemy enemy : entityManager.getEnemies()) {
                    if (com.shooter.shared.logic.CollisionDetector.bulletHitsEnemy(bullet, enemy)) {
                        enemy.takeDamage(bullet.getDamage());
                        if (enemy.isDead()) {
                            roundManager.addKill();
                            com.shooter.shared.model.PowerUp dropped = enemy.dropPowerUp();
                            if (dropped != null) {
                                entityManager.addPowerUp(dropped);
                            }
                            // Add DeathEffect to pendingEffects queue
                            gameState.addPendingEffect(new com.shooter.shared.model.DeathEffect(enemy.getX(), enemy.getY(), enemy.getType()));
                        }
                        bullet.expire();
                        break;
                    }
                }
            }
        }
        
        // Enemy vs Player collision
        for (Player player : gameState.getPlayers()) {
            if (!player.isAlive()) continue;
            for (com.shooter.shared.model.Enemy enemy : entityManager.getEnemies()) {
                if (com.shooter.shared.logic.CollisionDetector.enemyHitsPlayer(enemy, player)) {
                    // Simple cooldown check might be needed per player, but applying damage directly for now
                    player.takeDamage(enemy.getDamage());
                    // Break so only one enemy hits per tick, or don't break. In M1 we broke.
                    break;
                }
            }
        }
        
        // PowerUp vs Player collision
        entityManager.getPowerUps().removeIf(powerUp -> {
            for (Player player : gameState.getPlayers()) {
                if (player.isAlive() && com.shooter.shared.logic.CollisionDetector.playerCollectsPowerUp(player, powerUp)) {
                    player.applyPowerUp(powerUp);
                    return true;
                }
            }
            return false;
        });

        // Remove expired bullets and dead enemies
        gameState.removeExpiredBullets();
        entityManager.removeDeadEnemies();

        // Round logic
        roundManager.checkAndAdvanceRound(entityManager);
        gameState.setCurrentRound(roundManager.getCurrentRound());
        
        // Sync entityManager entities to gameState for broadcast
        gameState.setEnemies(entityManager.getEnemies());
        gameState.setPowerUps(entityManager.getPowerUps());
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