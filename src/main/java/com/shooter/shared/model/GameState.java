package com.shooter.shared.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================
 * FILE: GameState.java
 * PACKAGE: shared.model
 * OWNER: All members (shared contract)
 * ============================================================
 *
 * RESPONSIBILITY:
 * Represents the ENTIRE game at one moment in time.
 * This is the "single source of truth" for:
 * - players
 * - enemies
 * - bullets
 * - power-ups
 * - current round
 *
 * In Milestone 2:
 * This is what the SERVER sends to all clients.
 *
 * WHAT TO ADD HERE:
 * - Score tracking
 * - Player list for multiplayer
 * - Round progression data
 *
 * WHAT NOT TO PUT HERE:
 * - Rendering logic
 * - Movement logic
 * - Input handling
 *
 * CONNECTS TO:
 * GamePanel (renders this)
 * GameManager (updates this)
 * Network (sent across clients)
 * ============================================================
 */
public class GameState implements Serializable {

    private List<Player> players = new ArrayList<>();
    private List<Enemy> enemies = new ArrayList<>();
    private List<Bullet> bullets = new ArrayList<>();
    private List<PowerUp> powerUps = new ArrayList<>();

    private int currentRound = 1;

    public void addPlayer(Player player) {
        players.add(player);
    }

    public void addEnemy(Enemy enemy) {
        enemies.add(enemy);
    }

    public void addBullet(Bullet bullet) {
        bullets.add(bullet);
    }

    public void addPowerUp(PowerUp powerUp) {
        powerUps.add(powerUp);
    }

    public Player getMainPlayer() {
        return players.isEmpty() ? null : players.get(0);
    }

    public void removeExpiredBullets() {
        bullets.removeIf(Bullet::isExpired);
    }

    public void removeDeadEnemies() {
        enemies.removeIf(Enemy::isDead);
    }

    public List<Player> getPlayers() {
        return players;
    }

    public List<Enemy> getEnemies() {
        return enemies;
    }

    public List<Bullet> getBullets() {
        return bullets;
    }

    public List<PowerUp> getPowerUps() {
        return powerUps;
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public void setCurrentRound(int r) {
        currentRound = r;
    }
}