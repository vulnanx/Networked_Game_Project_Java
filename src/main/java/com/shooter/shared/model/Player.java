package com.shooter.shared.model;

import com.shooter.shared.util.Constants;
import com.shooter.shared.util.Direction;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================
 * FILE: Player.java
 * PACKAGE: shared.model
 * OWNER: Member B (entities)
 * ============================================================
 *
 * RESPONSIBILITY:
 * Holds ALL state that defines one player at any moment:
 * position, HP, direction, power-up effects, cooldown timer.
 * This is a plain data + simple-logic class shared between
 * client and server. Both sides keep a copy of this object.
 *
 * WHAT TO ADD HERE:
 * - move(Direction d): update x/y based on direction + speed
 * - shoot(): create and return a new Bullet object (respect cooldown)
 * - takeDamage(int dmg): reduce HP, check for death
 * - revive(): reset HP to base, clear power-ups, reset position
 * - applyPowerUp(PowerUp p): modify stats based on power-up type
 * - Any getter/setter pairs needed by Renderer or GameManager
 *
 * WHAT NOT TO PUT HERE:
 * - Rendering code (no Graphics2D, no drawImage) — that's GamePanel's job
 * - Network send/receive code — that's ClientHandler / GameClient's job
 * - Enemy spawning — that's EnemySpawner's job
 * - Collision detection — that's CollisionDetector's job
 *
 * CONNECTS TO:
 * GameState.java (included in the snapshot),
 * CollisionDetector.java (provides bounding box),
 * GamePanel.java (rendered based on x, y, direction),
 * GameManager.java (calls move, shoot, takeDamage),
 * InputHandler.java (input converted to direction/action then passed here)
 * ============================================================
 */
public class Player implements Serializable {

    // Required for Java serialization (used when sending over network in M2)
    private static final long serialVersionUID = 1L;

    // ─── IDENTITY ────────────────────────────────────────────────────────────
    private int id; // Unique player ID (0–3). Assigned by server.
    private String name; // Display name

    // ─── POSITION & SIZE ─────────────────────────────────────────────────────
    private float x; // Top-left X in pixels
    private float y; // Top-left Y in pixels
    private int width = Constants.PLAYER_SIZE;
    private int height = Constants.PLAYER_SIZE;

    // ─── FACING DIRECTION ────────────────────────────────────────────────────
    // Determines which way bullets travel and which sprite frame to draw
    private Direction facing = Direction.DOWN;

    // ─── STATS (modified by power-ups) ───────────────────────────────────────
    private int hp;
    private int maxHp = Constants.PLAYER_BASE_HP;
    private float speed = Constants.PLAYER_BASE_SPEED;
    private int damage = Constants.PLAYER_BASE_DAMAGE;
    private int shootCooldown = Constants.PLAYER_SHOOT_COOLDOWN; // ticks between shots

    // ─── COOLDOWN TRACKING ────────────────────────────────────────────────────
    // Counts down each tick. Player can shoot when this reaches 0.
    private int shootCooldownTimer = 0;

    // ─── STATE FLAGS ─────────────────────────────────────────────────────────
    private boolean alive = true;

    // ─── POWER-UP LOG ────────────────────────────────────────────────────────
    // Track which power-ups are active so revive() can clear them
    private List<String> activePowerUps = new ArrayList<>();

    // =========================================================================
    // CONSTRUCTOR
    // =========================================================================

    public Player(int id, String name) {
        this.id = id;
        this.name = name;
        this.hp = maxHp;
        // Spawn at center of arena
        this.x = Constants.PLAYER_SPAWN_X - (width / 2f);
        this.y = Constants.PLAYER_SPAWN_Y - (height / 2f);
    }

    // =========================================================================
    // GAME LOGIC — TO BE IMPLEMENTED BY MEMBER B
    // =========================================================================

    /**
     * Move the player in the given direction by their current speed.
     * Called every tick by GameManager when input is received.
     */
    public void move(Direction direction) {
        this.facing = direction;

        int[] vec = direction.toVector();

        float newX = x + vec[0] * speed;
        float newY = y + vec[1] * speed;

        // Clamp inside arena
        newX = Math.max(Constants.ARENA_X,
                Math.min(newX, Constants.ARENA_X + Constants.ARENA_WIDTH - width));

        newY = Math.max(Constants.ARENA_Y,
                Math.min(newY, Constants.ARENA_Y + Constants.ARENA_HEIGHT - height));

        x = newX;
        y = newY;
    }

    /**
     * Attempt to fire a bullet in the current facing direction.
     * Returns a new Bullet if cooldown has elapsed, or null if still cooling down.
     */
    public Bullet shoot() {

        if (shootCooldownTimer > 0) {
            return null;
        }

        shootCooldownTimer = shootCooldown;

        float bulletX = x + width / 2f;
        float bulletY = y + height / 2f;

        return new Bullet(
                bulletX,
                bulletY,
                facing,
                damage,
                id,
                false);
    }

    /**
     * Decrement the shoot cooldown timer each tick.
     * Call this in GameManager.update() on every tick.
     */
    public void tickCooldown() {
        if (shootCooldownTimer > 0) {
            shootCooldownTimer--;
        }
    }

    /**
     * Reduce HP by dmg. If HP drops to 0 or below, trigger death.
     */
    public void takeDamage(int dmg) {
        hp -= dmg;

        if (hp <= 0) {
            hp = 0;
            alive = false;
            System.out.println("Player " + id + " died.");
        }

        // Print the Player took damage and HP after taking damage
        System.out.println("Player " + id + " took " + dmg + " damage. Current HP: " + hp);
    }

    /**
     * Revive the player: reset HP, position, and clear all power-up bonuses.
     * Called by GameManager when the player respawns
     */
    public void revive() {
        reviveAt(Constants.PLAYER_SPAWN_X, Constants.PLAYER_SPAWN_Y);
    }

    public void reviveAt(float spawnCenterX, float spawnCenterY) {
        hp = Constants.PLAYER_BASE_HP;
        maxHp = Constants.PLAYER_BASE_HP;
        speed = Constants.PLAYER_BASE_SPEED;
        damage = Constants.PLAYER_BASE_DAMAGE;
        shootCooldown = Constants.PLAYER_SHOOT_COOLDOWN;
        shootCooldownTimer = 0;

        x = spawnCenterX - width / 2f;
        y = spawnCenterY - height / 2f;

        activePowerUps.clear();

        alive = true;

        System.out.println("Player revived at safe spawn. Power-ups reset.");
    }

    /**
     * Apply a power-up's effect to this player's stats, respecting maximum caps.
     */
    public void applyPowerUp(PowerUp p) {
        boolean atCap = false;

        switch (p.getType()) {
            case ATTACK_SPEED:
                if (shootCooldown <= Constants.PLAYER_MIN_SHOOT_COOLDOWN) {
                    atCap = true;
                } else {
                    shootCooldown -= Constants.POWERUP_COOLDOWN_BONUS;
                    if (shootCooldown < Constants.PLAYER_MIN_SHOOT_COOLDOWN) {
                        shootCooldown = Constants.PLAYER_MIN_SHOOT_COOLDOWN;
                    }
                }
                break;

            case DAMAGE:
                if (damage >= Constants.PLAYER_MAX_DAMAGE) {
                    atCap = true;
                } else {
                    damage += Constants.POWERUP_DAMAGE_BONUS;
                    if (damage > Constants.PLAYER_MAX_DAMAGE) {
                        damage = Constants.PLAYER_MAX_DAMAGE;
                    }
                }
                break;

            case HP:
                hp += Constants.POWERUP_HP_BONUS;
                if (hp > maxHp) {
                    hp = maxHp;
                }
                break;

            case MOVEMENT:
                if (speed >= Constants.PLAYER_MAX_SPEED) {
                    atCap = true;
                } else {
                    speed += Constants.POWERUP_SPEED_BONUS;
                    if (speed > Constants.PLAYER_MAX_SPEED) {
                        speed = Constants.PLAYER_MAX_SPEED;
                    }
                }
                break;
        }

        activePowerUps.add(p.getType().name());

        if (atCap) {
            System.out.println("Applied power-up: " + p.getType() + " (MAX CAP REACHED)");
        } else {
            System.out.println("Applied power-up: " + p.getType());
        }
    }

    // =========================================================================
    // BOUNDING BOX — used by CollisionDetector
    // =========================================================================

    /** @return left edge of player sprite in pixels */
    public float getLeft() {
        return x;
    }

    /** @return right edge of player sprite in pixels */
    public float getRight() {
        return x + width;
    }

    /** @return top edge of player sprite in pixels */
    public float getTop() {
        return y;
    }

    /** @return bottom edge of player sprite in pixels */
    public float getBottom() {
        return y + height;
    }

    // =========================================================================
    // GETTERS & SETTERS
    // =========================================================================

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
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

    public Direction getFacing() {
        return facing;
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

    public int getShootCooldown() {
        return shootCooldown;
    }

    public List<String> getActivePowerUps() {
        return activePowerUps;
    }

    public boolean isAlive() {
        return alive;
    }

    public void setX(float x) {
        this.x = x;
    }

    public void setY(float y) {
        this.y = y;
    }

    public void setFacing(Direction d) {
        this.facing = d;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    @Override
    public String toString() {
        return String.format("Player[id=%d, name=%s, x=%.1f, y=%.1f, hp=%d, alive=%b]",
                id, name, x, y, hp, alive);
    }
}