package com.shooter.shared.model;

import com.shooter.shared.util.Constants;
import java.io.Serializable;

/**
 * ============================================================
 * FILE: Enemy.java
 * PACKAGE: shared.model
 * OWNER: Member B (entities)
 * ============================================================
 *
 * RESPONSIBILITY:
 * Base class for all enemy types. Holds shared state:
 * position, HP, type, and movement behavior toward the player.
 * Subclasses (MeleeEnemy, RangedEnemy, SemiBossEnemy) override
 * attack behavior and starting HP.
 *
 * For Milestone 1, you can use Enemy directly with a type field
 * and skip full subclassing — get it working first, refactor later.
 *
 * WHAT TO ADD HERE:
 * - moveToward(float targetX, float targetY): move toward player each tick
 * - takeDamage(int dmg): reduce HP, check for death
 * - isDead(): return hp <= 0
 * - dropPowerUp(): random chance to return a PowerUp, or null
 * - attack(Player p): apply damage to player if in range (melee contact)
 *
 * WHAT NOT TO PUT HERE:
 * - Rendering code — belongs in GamePanel.java
 * - Spawning logic — belongs in EnemySpawner.java
 * - Round configuration — belongs in RoundConfig.java
 * - Collision detection — belongs in CollisionDetector.java
 *
 * CONNECTS TO:
 * GameState.java (list of enemies),
 * CollisionDetector.java (provides bounding box),
 * EnemySpawner.java (creates instances of this),
 * GamePanel.java (renders based on x, y, type),
 * RoundConfig.java (determines how many and what HP)
 * ============================================================
 */
public class Enemy implements Serializable {

    private static final long serialVersionUID = 1L;

    // ─── ENEMY TYPE ──────────────────────────────────────────────────────────
    public enum Type {
        MELEE, // Walks into the player, deals contact damage
        RANGED, // Stops at range, shoots projectiles
        SEMI_BOSS // Larger melee enemy with much more HP
    }

    // ─── IDENTITY ────────────────────────────────────────────────────────────
    private int id; // Unique ID assigned by EnemySpawner
    private Type type;

    // ─── POSITION & SIZE ─────────────────────────────────────────────────────
    private float x;
    private float y;
    private int width = Constants.ENEMY_SIZE;
    private int height = Constants.ENEMY_SIZE;

    // ─── STATS ───────────────────────────────────────────────────────────────
    private int hp;
    private int maxHp;
    private float speed;
    private int damage; // Damage dealt to player per hit
    private int shootCooldownTimer = 0; // Only used by RANGED type

    // ─── STATE ───────────────────────────────────────────────────────────────
    private boolean dead = false;

    // =========================================================================
    // CONSTRUCTOR
    // =========================================================================

    /**
     * Create an enemy at a given position.
     * Speed, damage, and HP are set based on type.
     * RoundConfig controls starting HP and adjusts it per round.
     */
    public Enemy(int id, Type type, float x, float y, int hp) {
        this.id = id;
        this.type = type;
        this.x = x;
        this.y = y;
        this.hp = hp;
        this.maxHp = hp;

        // Set speed and base damage based on type
        switch (type) {
            case MELEE:
                this.speed = Constants.MELEE_SPEED;
                this.damage = 10;
                break;
            case RANGED:
                this.speed = Constants.RANGED_SPEED;
                this.damage = 5; // bullet damage
                break;
            case SEMI_BOSS:
                this.speed = Constants.SEMIBOSS_SPEED;
                this.damage = 20;
                break;
        }
    }

    // =========================================================================
    // GAME LOGIC — TO BE IMPLEMENTED BY MEMBER B
    // =========================================================================

    /**
     * Move this enemy toward a target position (the player's center).
     * Called every tick by GameManager.
     */
    public void moveToward(float targetX, float targetY) {
        float enemyCenterX = x + width / 2f;
        float enemyCenterY = y + height / 2f;

        float dx = targetX - enemyCenterX;
        float dy = targetY - enemyCenterY;

        float distance = (float) Math.sqrt(dx * dx + dy * dy);

        if (distance > 0) {
            x += (dx / distance) * speed;
            y += (dy / distance) * speed;
        }
    }

    /**
     * Reduce this enemy's HP by dmg.
     * If HP reaches 0, mark as dead.
     *
     * TODO (Member B):
     * 1. hp -= dmg, clamp to 0
     * 2. if hp == 0: dead = true
     */
    public void takeDamage(int dmg) {
        // TODO: implement damage and death
    }

    /**
     * Randomly decide whether this enemy drops a power-up on death.
     * Returns a new PowerUp at this enemy's position, or null.
     *
     * TODO (Member C):
     * 1. Roll Math.random() < Constants.POWERUP_DROP_CHANCE
     * 2. If yes: pick a random PowerUp.Type and return new PowerUp(x, y, type)
     * 3. If no: return null
     */
    public PowerUp dropPowerUp() {
        // TODO: implement drop logic
        return null;
    }

    /**
     * Tick the ranged enemy's shoot cooldown.
     * Returns true if the enemy is ready to fire this tick.
     *
     * TODO (Member B):
     * Only relevant for RANGED type. Each tick decrement
     * shootCooldownTimer. Return true when it hits 0, then
     * reset to Constants.RANGED_COOLDOWN.
     */
    public boolean tickAndCanShoot() {
        // TODO: implement for RANGED type
        return false;
    }

    // ─── BOUNDING BOX ────────────────────────────────────────────────────────

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

    // ─── GETTERS ─────────────────────────────────────────────────────────────

    public int getId() {
        return id;
    }

    public Type getType() {
        return type;
    }

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

    public int getHp() {
        return hp;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public float getSpeed() {
        return speed;
    }

    public int getDamage() {
        return damage;
    }

    public boolean isDead() {
        return dead;
    }

    @Override
    public String toString() {
        return String.format("Enemy[id=%d, type=%s, x=%.1f, y=%.1f, hp=%d]",
                id, type, x, y, hp);
    }
}