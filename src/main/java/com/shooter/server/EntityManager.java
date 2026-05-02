package com.shooter.server;

import com.shooter.shared.model.Bullet;
import com.shooter.shared.model.Enemy;
import com.shooter.shared.model.PowerUp;

import java.util.ArrayList;
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
}