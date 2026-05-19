package com.shooter.server;

import com.shooter.shared.model.Bullet;
import com.shooter.shared.model.Enemy;
import com.shooter.shared.model.GameState;
import com.shooter.shared.model.Player;
import com.shooter.shared.model.PowerUp;
import com.shooter.shared.logic.CollisionDetector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * ============================================================
 * FILE: EntityManager.java
 * PACKAGE: server
 * OWNER: Member C (Systems)
 * ============================================================
 *
 * RESPONSIBILITY:
 * Stores and manages active game entities:
 * - enemies
 * - bullets
 * - power-ups
 *
 * WHAT TO ADD HERE:
 * - Updating all enemies, bullets, and power-ups
 * - Removing expired bullets
 * - Removing dead enemies
 *
 * WHAT NOT TO PUT HERE:
 * - Rendering code
 * - Keyboard input code
 * - Round configuration values
 *
 * CONNECTS TO:
 * EnemySpawner (adds enemies here)
 * RoundManager (checks if enemies are gone)
 * GameManager/GameServer (will use this as server-side game state manager)
 * ============================================================
 */
public class EntityManager {

    private List<Enemy> enemies = new ArrayList<>();
    private List<Bullet> bullets = new ArrayList<>();
    private List<PowerUp> powerUps = new ArrayList<>();

    /**
     * Small result object used by the server when a player collects a power-up.
     * GameManager can use this later for HUD notifications or console logging.
     */
    public static class PowerUpCollection {
        private final int playerId;
        private final String playerName;
        private final PowerUp.Type powerUpType;
        private final boolean atCap;

        public PowerUpCollection(int playerId, String playerName, PowerUp.Type powerUpType, boolean atCap) {
            this.playerId = playerId;
            this.playerName = playerName;
            this.powerUpType = powerUpType;
            this.atCap = atCap;
        }

        public int getPlayerId() {
            return playerId;
        }

        public String getPlayerName() {
            return playerName;
        }

        public PowerUp.Type getPowerUpType() {
            return powerUpType;
        }

        public boolean isAtCap() {
            return atCap;
        }
    }

    // ==================
    // Manage Enemies
    // ==================
    public void addEnemy(Enemy enemy) {
        enemies.add(enemy);
    }

    public void addEnemies(List<Enemy> newEnemies) {
        enemies.addAll(newEnemies);
    }

    public void removeDeadEnemies() {
        enemies.removeIf(Enemy::isDead);
    }

    public boolean hasNoEnemies() {
        return enemies.isEmpty();
    }

    public List<Enemy> getEnemies() {
        return enemies;
    }

    // ==================
    // Manage Bullets
    // ==================
    public void addBullet(Bullet bullet) {
        bullets.add(bullet);
    }

    public void removeBullet(Bullet bullet) {
        bullets.remove(bullet);
    }

    public void updateBullets() {
        for (Bullet bullet : bullets) {
            bullet.update();
        }

        bullets.removeIf(Bullet::isExpired);
    }

    public List<Bullet> getBullets() {
        return bullets;
    }

    // ==================
    // Manage Power-Ups
    // ==================
    public List<PowerUp> getPowerUps() {
        return powerUps;
    }

    public void addPowerUp(PowerUp powerUp) {
        powerUps.add(powerUp);
    }

    /**
     * Checks every active power-up against every alive player.
     * When a player touches a power-up, the server applies the effect once,
     * removes the power-up, and records what happened for optional UI updates.
     *
     * @param players server-owned players from GameState
     * @return collection events that happened this tick
     */
    public List<PowerUpCollection> collectPowerUpsForPlayers(List<Player> players) {
        List<PowerUpCollection> collections = new ArrayList<>();
        Iterator<PowerUp> iterator = powerUps.iterator();

        while (iterator.hasNext()) {
            PowerUp powerUp = iterator.next();
            Player collector = findCollector(players, powerUp);

            if (collector == null) {
                continue;
            }

            boolean atCap = collector.applyPowerUp(powerUp);
            collections.add(new PowerUpCollection(
                    collector.getPlayerId(),
                    collector.getName(),
                    powerUp.getType(),
                    atCap));
            iterator.remove();
        }

        return collections;
    }

    /**
     * Copies the server-owned entities into the broadcast GameState.
     * This keeps clients rendering the same enemies, bullets, and power-ups
     * instead of relying on local-only lists.
     *
     * @param gameState the authoritative state that GameManager broadcasts
     */
    public void copyEntitiesToGameState(GameState gameState) {
        gameState.setEnemies(enemies);
        gameState.setBullets(bullets);
        gameState.setPowerUps(powerUps);
    }

    private Player findCollector(List<Player> players, PowerUp powerUp) {
        for (Player player : players) {
            if (player != null
                    && player.isAlive()
                    && CollisionDetector.playerCollectsPowerUp(player, powerUp)) {
                return player;
            }
        }

        return null;
    }
}
