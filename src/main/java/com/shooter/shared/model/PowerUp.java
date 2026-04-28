package com.shooter.shared.model;

import java.io.Serializable;

/**
 * Represents a power-up item.
 *
 * NOT REQUIRED for Day 1.
 */
public class PowerUp implements Serializable {

    public enum Type {
        SPEED,
        DAMAGE,
        HP
    }

    private float x, y;
    private Type type;

    public PowerUp(float x, float y, Type type) {
        this.x = x;
        this.y = y;
        this.type = type;
    }
}