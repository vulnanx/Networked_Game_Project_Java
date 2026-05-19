package com.shooter.server;

import com.shooter.shared.model.GameState;
import com.shooter.shared.model.Player;
import com.shooter.shared.model.Bullet;
import com.shooter.shared.model.DeathEffect;
import com.shooter.shared.model.Enemy;
import com.shooter.shared.model.PowerUp;
import com.shooter.shared.logic.CollisionDetector;
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

        updateEnemiesFromServer();

        // Update bullets
        for (Bullet b : gameState.getBullets()) {
            b.update();
        }
        
        processServerCollisions();

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
     * Resolve every collision on the server.
     *
     * Clients may draw these results, but they should not decide whether a bullet
     * hit, an enemy died, a player lost HP, or a power-up was collected. Keeping
     * those decisions here prevents each client from simulating a slightly
     * different version of the game.
     */
    private void processServerCollisions() {
        // Bullet collisions: player bullets damage enemies; enemy bullets damage players.
        for (Bullet bullet : gameState.getBullets()) {
            if (bullet.isFromEnemy()) {
                for (Player player : gameState.getPlayers()) {
                    if (player.isAlive() && CollisionDetector.bulletHitsPlayer(bullet, player)) {
                        player.takeDamage(bullet.getDamage());
                        bullet.expire();
                        // Break to only hit one player
                        break;
                    }
                }
            } else {
                for (Enemy enemy : entityManager.getEnemies()) {
                    if (CollisionDetector.bulletHitsEnemy(bullet, enemy)) {
                        enemy.takeDamage(bullet.getDamage());
                        if (enemy.isDead()) {
                            roundManager.addKill();
                            PowerUp dropped = enemy.dropPowerUp();
                            if (dropped != null) {
                                entityManager.addPowerUp(dropped);
                            }
                            // Add DeathEffect to pendingEffects queue
                            gameState.addPendingEffect(new DeathEffect(enemy.getX(), enemy.getY(), enemy.getType()));
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
            for (Enemy enemy : entityManager.getEnemies()) {
                if (CollisionDetector.enemyHitsPlayer(enemy, player)) {
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
                if (player.isAlive() && CollisionDetector.playerCollectsPowerUp(player, powerUp)) {
                    player.applyPowerUp(powerUp);
                    return true;
                }
            }
            return false;
        });
    }

    /**
     * Move enemies and create ranged enemy bullets on the server.
     *
     * Every client receives these enemy positions and bullets through the next
     * GameState broadcast, so clients stay synchronized without running their
     * own enemy AI.
     */
    private void updateEnemiesFromServer() {
        for (Enemy enemy : entityManager.getEnemies()) {
            Player target = findNearestAlivePlayer(enemy);
            if (target == null) {
                continue;
            }

            float targetCenterX = target.getX() + target.getWidth() / 2f;
            float targetCenterY = target.getY() + target.getHeight() / 2f;

            enemy.moveToward(targetCenterX, targetCenterY);

            if (enemy.tickAndCanShoot()) {
                Direction direction = directionToward(enemy.getX(), enemy.getY(), targetCenterX, targetCenterY);
                gameState.addBullet(new Bullet(
                        enemy.getX(),
                        enemy.getY(),
                        direction,
                        enemy.getDamage(),
                        enemy.getId(),
                        true));
            }
        }
    }

    /**
     * Pick the closest living player so all clients see the same enemy target.
     */
    private Player findNearestAlivePlayer(Enemy enemy) {
        Player nearest = null;
        float nearestDistanceSquared = Float.MAX_VALUE;

        float enemyCenterX = enemy.getX() + enemy.getWidth() / 2f;
        float enemyCenterY = enemy.getY() + enemy.getHeight() / 2f;

        for (Player player : gameState.getPlayers()) {
            if (player == null || !player.isAlive()) {
                continue;
            }

            float playerCenterX = player.getX() + player.getWidth() / 2f;
            float playerCenterY = player.getY() + player.getHeight() / 2f;
            float dx = playerCenterX - enemyCenterX;
            float dy = playerCenterY - enemyCenterY;
            float distanceSquared = dx * dx + dy * dy;

            if (distanceSquared < nearestDistanceSquared) {
                nearestDistanceSquared = distanceSquared;
                nearest = player;
            }
        }

        return nearest;
    }

    /**
     * Convert a target point into one of the four bullet directions.
     */
    private Direction directionToward(float fromX, float fromY, float targetX, float targetY) {
        float dx = targetX - fromX;
        float dy = targetY - fromY;

        if (Math.abs(dx) > Math.abs(dy)) {
            return dx > 0 ? Direction.RIGHT : Direction.LEFT;
        }

        return dy > 0 ? Direction.DOWN : Direction.UP;
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
