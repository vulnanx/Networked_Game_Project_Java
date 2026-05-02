package com.shooter.shared.logic;

import com.shooter.shared.model.Bullet;
import com.shooter.shared.model.Enemy;
import com.shooter.shared.model.Player;
import com.shooter.shared.model.PowerUp;

/**
 * ============================================================
 * FILE: CollisionDetector.java
 * PACKAGE: shared.logic
 * OWNER: Member B (Collision) / Member C integration
 * ============================================================
 *
 * RESPONSIBILITY:
 * Handles collision checks between game objects.
 *
 * WHAT TO ADD HERE:
 * - Bullet vs enemy collision
 * - Enemy vs player collision
 * - Power-up vs player collision
 *
 * WHAT NOT TO PUT HERE:
 * - Drawing code
 * - Enemy spawning
 * - Round progression
 *
 * CONNECTS TO:
 * Bullet.java
 * Enemy.java
 * Player.java
 * GamePanel.java / GameManager.java
 * ============================================================
 */
public class CollisionDetector {

    // **************
    // Helper
    // **************
    public static boolean isColliding(
            float aLeft, float aRight, float aTop, float aBottom,
            float bLeft, float bRight, float bTop, float bBottom) {
        return aLeft < bRight &&
                aRight > bLeft &&
                aTop < bBottom &&
                aBottom > bTop;
    }

    // **************
    // ACTOR: BULLET
    // **************
    public static boolean bulletHitsEnemy(Bullet bullet, Enemy enemy) {
        return isColliding(
                bullet.getX(),
                bullet.getX() + bullet.getWidth(),
                bullet.getY(),
                bullet.getY() + bullet.getHeight(),

                enemy.getX(),
                enemy.getX() + enemy.getWidth(),
                enemy.getY(),
                enemy.getY() + enemy.getHeight());
    }

    // **************
    // ACTOR: ENEMY
    // **************
    public static boolean enemyHitsPlayer(Enemy enemy, Player player) {
        return isColliding(
                enemy.getLeft(),
                enemy.getRight(),
                enemy.getTop(),
                enemy.getBottom(),

                player.getLeft(),
                player.getRight(),
                player.getTop(),
                player.getBottom());
    }

    // **************
    // ACTOR: PLAYER
    // **************
    public static boolean playerCollectsPowerUp(Player player, PowerUp powerUp) {
        return isColliding(
                player.getLeft(),
                player.getRight(),
                player.getTop(),
                player.getBottom(),
                powerUp.getLeft(),
                powerUp.getRight(),
                powerUp.getTop(),
                powerUp.getBottom()
        );
    }
}