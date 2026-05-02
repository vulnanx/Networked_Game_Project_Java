package com.shooter.shared.model;

import com.shooter.shared.util.Constants;
import com.shooter.shared.util.Direction;

import java.io.Serializable;

/**
 * Represents a bullet fired by the player.
 */
public class Bullet implements Serializable {

    private float x, y;
    private int width = Constants.BULLET_SIZE;
    private int height = Constants.BULLET_SIZE;

    private Direction direction;
    private int damage;
    private int ownerId;
    private boolean expired = false; // set to true when bullet hits an enemy

    public Bullet(float x, float y, Direction dir, int dmg, int ownerId) {
        this.x = x;
        this.y = y;
        this.direction = dir;
        this.damage = dmg;
        this.ownerId = ownerId;
    }

    public void update() {
        int[] vec = direction.toVector();

        x += vec[0] * Constants.BULLET_SPEED;
        y += vec[1] * Constants.BULLET_SPEED;
    }

    /** Force this bullet to be cleaned up (e.g. after hitting an enemy). */
    public void expire() {
        expired = true;
    }

    public boolean isExpired() {
        return expired
            || x < 0 || x > Constants.SCREEN_WIDTH
            || y < 0 || y > Constants.SCREEN_HEIGHT;
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getDamage() { return damage; }
    public int getOwnerId() { return ownerId; }
}