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
 *   Holds ALL state that defines one player at any moment:
 *   position, HP, direction, power-up effects, cooldown timer.
 *   This is a plain data + simple-logic class shared between
 *   client and server. Both sides keep a copy of this object.
 *
 * WHAT TO ADD HERE:
 *   - move(Direction d): update x/y based on direction + speed
 *   - shoot(): create and return a new Bullet object (respect cooldown)
 *   - takeDamage(int dmg): reduce HP, check for death
 *   - revive(): reset HP to base, clear power-ups, reset position
 *   - applyPowerUp(PowerUp p): modify stats based on power-up type
 *   - Any getter/setter pairs needed by Renderer or GameManager
 *
 * WHAT NOT TO PUT HERE:
 *   - Rendering code (no Graphics2D, no drawImage) — that's GamePanel's job
 *   - Network send/receive code — that's ClientHandler / GameClient's job
 *   - Enemy spawning — that's EnemySpawner's job
 *   - Collision detection — that's CollisionDetector's job
 *
 * CONNECTS TO:
 *   GameState.java (included in the snapshot),
 *   CollisionDetector.java (provides bounding box),
 *   GamePanel.java (rendered based on x, y, direction),
 *   GameManager.java (calls move, shoot, takeDamage),
 *   InputHandler.java (input converted to direction/action then passed here)
 * ============================================================
 */
public class Player implements Serializable {

    // Required for Java serialization (used when sending over network in M2)
    private static final long serialVersionUID = 1L;

    // ─── IDENTITY ────────────────────────────────────────────────────────────
    private int id;         // Unique player ID (0–3). Assigned by server.
    private String name;    // Display name

    // ─── POSITION & SIZE ─────────────────────────────────────────────────────
    private float x;        // Top-left X in pixels
    private float y;        // Top-left Y in pixels
    private int width  = Constants.PLAYER_SIZE;
    private int height = Constants.PLAYER_SIZE;

    // ─── FACING DIRECTION ────────────────────────────────────────────────────
    // Determines which way bullets travel and which sprite frame to draw
    private Direction facing = Direction.DOWN;

    // ─── STATS (modified by power-ups) ───────────────────────────────────────
    private int   hp;
    private int   maxHp       = Constants.PLAYER_BASE_HP;
    private float speed       = Constants.PLAYER_BASE_SPEED;
    private int   damage      = Constants.PLAYER_BASE_DAMAGE;
    private int   shootCooldown = Constants.PLAYER_SHOOT_COOLDOWN; // ticks between shots

    // ─── COOLDOWN TRACKING ────────────────────────────────────────────────────
    // Counts down each tick. Player can shoot when this reaches 0.
    private int shootCooldownTimer = 0;

    // ─── STATE FLAGS ─────────────────────────────────────────────────────────
    private boolean alive = true;

    // ─── POWER-UP LOG ────────────────────────────────────────────────────────
    // Track which power-ups are active so revive() can clear them
    // TODO (Member B): Populate this when applyPowerUp() is implemented
    private List<String> activePowerUps = new ArrayList<>();

    // =========================================================================
    // CONSTRUCTOR
    // =========================================================================

    public Player(int id, String name) {
        this.id   = id;
        this.name = name;
        this.hp   = maxHp;
        // Spawn at center of arena
        this.x = Constants.PLAYER_SPAWN_X - (width  / 2f);
        this.y = Constants.PLAYER_SPAWN_Y - (height / 2f);
    }

    // =========================================================================
    // GAME LOGIC — TO BE IMPLEMENTED BY MEMBER B
    // =========================================================================

    /**
     * Move the player in the given direction by their current speed.
     * Called every tick by GameManager when input is received.
     *
     * TODO (Member B):
     *   1. Use direction.toVector() to get dx, dy
     *   2. Multiply by speed
     *   3. Clamp x and y to stay inside the arena bounds (use Constants.ARENA_*)
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
     *
     * TODO (Member B):
     *   1. Check if shootCooldownTimer <= 0
     *   2. If yes: create a new Bullet at the center of this player,
     *              facing the same direction, with this player's damage.
     *              Reset shootCooldownTimer = shootCooldown.
     *   3. If no: return null
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
                id
        );
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
     *
     * TODO (Member B):
     *   1. Subtract dmg from hp
     *   2. Clamp to minimum 0
     *   3. If hp == 0, set alive = false
     *      (GameManager will call revive() after showing death effect)
     */
    public void takeDamage(int dmg) {
        hp -= dmg;

        if (hp <= 0) {
            hp = 0;
            alive = false;
        }
    }

    /**
     * Revive the player: reset HP, position, and clear all power-up bonuses.
     * Called by GameManager when the player respawns.
     *
     * TODO (Member B):
     *   1. Reset hp = PLAYER_BASE_HP
     *   2. Reset speed, damage, shootCooldown to base constants
     *   3. Clear activePowerUps list
     *   4. Reset position to spawn point
     *   5. Set alive = true
     */
    public void revive() {
        hp = Constants.PLAYER_BASE_HP;

        x = Constants.PLAYER_SPAWN_X - width / 2f;
        y = Constants.PLAYER_SPAWN_Y - height / 2f;

        activePowerUps.clear();

        alive = true;
    }

    /**
     * Apply a power-up's effect to this player's stats.
     *
     * TODO (Member B):
     *   Use a switch on p.getType():
     *     ATTACK_SPEED  → decrease shootCooldown by Constants.POWERUP_COOLDOWN_BONUS (min 1)
     *     DAMAGE        → increase damage by Constants.POWERUP_DAMAGE_BONUS
     *     HP            → restore/increase hp by Constants.POWERUP_HP_BONUS (cap at maxHp)
     *     MOVEMENT      → increase speed by Constants.POWERUP_SPEED_BONUS
     *   Then add p.getType().name() to activePowerUps.
     */
    public void applyPowerUp(PowerUp p) {
        // TODO: implement power-up application
    }

    // =========================================================================
    // BOUNDING BOX — used by CollisionDetector
    // =========================================================================

    /** @return left edge of player sprite in pixels */
    public float getLeft()   { return x; }
    /** @return right edge of player sprite in pixels */
    public float getRight()  { return x + width; }
    /** @return top edge of player sprite in pixels */
    public float getTop()    { return y; }
    /** @return bottom edge of player sprite in pixels */
    public float getBottom() { return y + height; }

    // =========================================================================
    // GETTERS & SETTERS
    // =========================================================================

    public int     getId()          { return id; }
    public String  getName()        { return name; }
    public float   getX()           { return x; }
    public float   getY()           { return y; }
    public int     getWidth()       { return width; }
    public int     getHeight()      { return height; }
    public Direction getFacing()    { return facing; }
    public int     getHp()          { return hp; }
    public int     getMaxHp()       { return maxHp; }
    public float   getSpeed()       { return speed; }
    public int     getDamage()      { return damage; }
    public boolean isAlive()        { return alive; }
    public void    setX(float x)    { this.x = x; }
    public void    setY(float y)    { this.y = y; }
    public void    setFacing(Direction d) { this.facing = d; }
    public void    setAlive(boolean alive) { this.alive = alive; }

    @Override
    public String toString() {
        return String.format("Player[id=%d, name=%s, x=%.1f, y=%.1f, hp=%d, alive=%b]",
                id, name, x, y, hp, alive);
    }
}