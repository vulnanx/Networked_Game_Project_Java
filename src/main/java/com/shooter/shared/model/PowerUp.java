package com.shooter.shared.model;

import com.shooter.shared.util.Constants;

import java.io.Serializable;

/**
 * ============================================================
 * FILE: PowerUp.java
 * PACKAGE: shared.model
 * OWNER: Member C (Systems)
 * ============================================================
 *
 * RESPONSIBILITY:
 * Represents a collectible power-up dropped by enemies.
 *
 * WHAT TO ADD HERE:
 * - More power-up types
 * - Visual effects (Milestone 2+)
 * - Duration-based buffs (optional later)
 *
 * WHAT NOT TO PUT HERE:
 * - Rendering logic (GamePanel)
 * - Collision logic (CollisionDetector)
 * - Drop logic (Enemy)
 *
 * CONNECTS TO:
 * Enemy (dropPowerUp)
 * Player (applyPowerUp)
 * GamePanel (draw + collect)
 * ============================================================
 */
public class PowerUp implements Serializable {

    public enum Type {
        SPEED,
        DAMAGE,
        HP
    }

    private float x, y;

    private int width = Constants.POWERUP_SIZE;
    private int height = Constants.POWERUP_SIZE;

    private Type type;

    public PowerUp(float x, float y, Type type) {
        this.x = x;
        this.y = y;
        this.type = type;
    }

    /**
     * Applies this power-up effect to the player.
     */
    public void apply(Player player) {
        player.applyPowerUp(this);
    }

    // ─── GETTERS ─────────────────────────────────────────────

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Type getType() {
        return type;
    }

    // ─── BOUNDING BOX (for collision) ─────────────────────────

    public float getLeft() {
        return x;
    }

    public float getRight() {
        return x + width;
    }

    public float getTop() {
        return y;
    }

    public float getBottom() {
        return y + height;
    }
}