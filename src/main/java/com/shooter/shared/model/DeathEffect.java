package com.shooter.shared.model;

import java.io.Serializable;

public class DeathEffect implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private float x;
    private float y;
    private Enemy.Type type;
    
    public DeathEffect(float x, float y, Enemy.Type type) {
        this.x = x;
        this.y = y;
        this.type = type;
    }
    
    public float getX() { return x; }
    public float getY() { return y; }
    public Enemy.Type getType() { return type; }
}
