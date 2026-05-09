package com.shooter.shared.util;

/**
 * ============================================================
 * FILE: Constants.java
 * PACKAGE: shared.util
 * OWNER: Any member can edit — discuss changes with the team first
 * ============================================================
 *
 * RESPONSIBILITY:
 * Central home for every "magic number" in the project.
 * Instead of writing 800, 60, 100 scattered across files,
 * everyone references Constants.SCREEN_WIDTH, Constants.TARGET_FPS, etc.
 * This makes tuning the game much easier.
 *
 * WHAT TO ADD HERE:
 * - Screen/window dimensions
 * - Player base stats (speed, HP, fire rate)
 * - Enemy base stats
 * - Network settings (port, tick rate)
 * - Tile/sprite sizes
 * - Any number you find yourself repeating in multiple files
 *
 * WHAT NOT TO PUT HERE:
 * - Game logic (no if-statements, no methods)
 * - Mutable state (all fields must be static final)
 * - Round-specific configs — those go in RoundConfig.java
 *
 * CONNECTS TO:
 * Everything. Every package imports this file.
 * ============================================================
 */
public final class Constants {

    // Prevent instantiation — this is a pure constants class
    private Constants() {
    }

    // ─── WINDOW & DISPLAY ───────────────────────────────────────────────────
    public static final int SCREEN_WIDTH = 800; // pixels
    public static final int SCREEN_HEIGHT = 800; // pixels
    public static final int TILE_SIZE = 32; // pixels per tile
    public static final String WINDOW_TITLE = "CMSC 137 Shooter";

    // ─── GAME LOOP ───────────────────────────────────────────────────────────
    public static final int TARGET_FPS = 60;
    public static final long NS_PER_TICK = 1_000_000_000L / TARGET_FPS;
    // How many ticks the server broadcasts state to clients (Milestone 2)
    public static final int SERVER_TICK_RATE = 15;

    // ─── ENTITY SPAWNING ─────────────────────────────────────────────────
    public static final int SAFE_SPAWN_RADIUS = 150;
    public static final int ENEMY_SPAWN_COOLDOWN = 120;
    public static final int ENEMY_SPAWN_COOLDOWN_REDUCTION = 20;
    public static final int ENEMY_SPAWN_COOLDOWN_MIN = 20;
    public static final int PLAYER_SPAWN_COOLDOWN = 180;

    // ─── ARENA ───────────────────────────────────────────────────────────────
    // The playable area is inset from the screen edges by BORDER_THICKNESS tiles
    public static final int BORDER_THICKNESS = 2; // tiles
    public static final int ARENA_X = BORDER_THICKNESS * TILE_SIZE;
    public static final int ARENA_Y = BORDER_THICKNESS * TILE_SIZE;
    public static final int ARENA_WIDTH = SCREEN_WIDTH - (2 * BORDER_THICKNESS * TILE_SIZE);
    public static final int ARENA_HEIGHT = SCREEN_HEIGHT - (2 * BORDER_THICKNESS * TILE_SIZE);

    // ─── PLAYER ──────────────────────────────────────────────────────────────
    public static final int PLAYER_SIZE = 32; // sprite width & height in pixels
    public static final int PLAYER_BASE_HP = 100;
    public static final float PLAYER_BASE_SPEED = 3.0f; // pixels per tick
    public static final int PLAYER_BASE_DAMAGE = 1;
    public static final int PLAYER_SHOOT_COOLDOWN = 15; // ticks between shots (~4 shots/sec at 60fps)
    public static final int PLAYER_HIT_COOLDOWN = 60; // 60 ticks ≈ 1 second

    /** Maximum movement speed a player can reach through power-ups. */
    public static final float PLAYER_MAX_SPEED = 7.0f;
    /** Maximum damage a player can reach through power-ups. */
    public static final int PLAYER_MAX_DAMAGE = 5;
    /**
     * Minimum shoot cooldown (ticks) — attack speed cannot go faster than this.
     * Lower = faster shooting. At 60 FPS, 5 ticks = 12 shots/sec.
     */
    public static final int PLAYER_MIN_SHOOT_COOLDOWN = 5;

    // Spawn position: center of arena
    public static final int PLAYER_SPAWN_X = SCREEN_WIDTH / 2;
    public static final int PLAYER_SPAWN_Y = SCREEN_HEIGHT / 2;

    // ─── BULLET ──────────────────────────────────────────────────────────────
    public static final int BULLET_SIZE = 8; // pixels
    public static final float BULLET_SPEED = 10.0f; // pixels per tick

    // ─── ENEMIES ─────────────────────────────────────────────────────────────
    public static final int ENEMY_SIZE = 32; // pixels
    public static final float MELEE_SPEED = 0.5f;
    public static final float RANGED_SPEED = 1.0f;
    public static final float SEMIBOSS_SPEED = 0.8f;
    public static final int RANGED_SHOOT_RANGE = 300; // pixels — ranged enemies fire within this distance
    public static final int RANGED_COOLDOWN = 150; // ticks between enemy shots

    // ─── POWER-UPS ───────────────────────────────────────────────────────────
    public static final int POWERUP_SIZE = 24; // pixels
    public static final float POWERUP_DROP_CHANCE = 0.25f; // 25% chance per enemy death
    public static final float POWERUP_SPEED_BONUS = 0.5f; // added to movement speed
    public static final int POWERUP_DAMAGE_BONUS = 1; // added to bullet damage
    public static final int POWERUP_COOLDOWN_BONUS = 5; // ticks removed from shoot cooldown
    public static final int POWERUP_HP_BONUS = 20; // HP restored / added to max

    // ─── ROUNDS ──────────────────────────────────────────────────────────────
    public static final int TOTAL_ROUNDS = 5;

    // ─── NETWORKING (Milestone 2) ─────────────────────────────────────────────
    public static final int SERVER_PORT = 5000;
    public static final String DEFAULT_HOST = "localhost";
    public static final int MAX_PLAYERS = 4;
    public static final int SOCKET_TIMEOUT = 5000; // milliseconds

    // ─── COLORS (fallback if sprites are missing) ─────────────────────────────
    // These are RGB int values used by Color(int rgb)
    public static final int COLOR_PLAYER = 0x4A90D9; // blue
    public static final int COLOR_MELEE = 0xE05C5C; // red
    public static final int COLOR_RANGED = 0xE09B3D; // orange
    public static final int COLOR_SEMIBOSS = 0x9B3DE0; // purple
    public static final int COLOR_BULLET = 0xF5F5A0; // yellow
    public static final int COLOR_POWERUP = 0x50E878; // green
    public static final int COLOR_ARENA_BG = 0x2B2B2B; // dark gray
}