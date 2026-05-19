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

    private static final long serialVersionUID = 1L;

    private List<Player> players = new ArrayList<>();
    private List<Enemy> enemies = new ArrayList<>();
    private List<Bullet> bullets = new ArrayList<>();
    private List<PowerUp> powerUps = new ArrayList<>();

    private int currentRound = 1;
    private int killedEnemies = 0;
    private int totalEnemiesThisRound = 0;

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

    public void setPlayers(List<Player> players) {
        this.players = new ArrayList<>(players);
    }

    public List<Enemy> getEnemies() {
        return enemies;
    }

    public void setEnemies(List<Enemy> enemies) {
        this.enemies = new ArrayList<>(enemies);
    }

    public List<Bullet> getBullets() {
        return bullets;
    }

    public void setBullets(List<Bullet> bullets) {
        this.bullets = new ArrayList<>(bullets);
    }

    public List<PowerUp> getPowerUps() {
        return powerUps;
    }

    public void setPowerUps(List<PowerUp> powerUps) {
        this.powerUps = new ArrayList<>(powerUps);
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public void setCurrentRound(int r) {
        currentRound = r;
    }

    public int getKilledEnemies() {
        return killedEnemies;
    }

    public void setKilledEnemies(int killedEnemies) {
        this.killedEnemies = killedEnemies;
    }

    public int getTotalEnemiesThisRound() {
        return totalEnemiesThisRound;
    }

    public void setTotalEnemiesThisRound(int totalEnemiesThisRound) {
        this.totalEnemiesThisRound = totalEnemiesThisRound;
    }
}
