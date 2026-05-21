package com.shooter.shared.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * ============================================================
 * FILE: AssetManager.java
 * PACKAGE: shared.util
 * OWNER: Geastin (Member C - Screens / Systems / Assets)
 * ============================================================
 *
 * Loads ALL sprites and tiles exactly ONCE at startup.
 * Any class that needs a sprite calls AssetManager.getInstance().get("key").
 *
 * RULES:
 *  - Never put BufferedImage fields inside model classes (Player, Enemy, etc.)
 *    BufferedImage is NOT serializable — it will crash the network layer.
 *  - If an asset file is missing, a solid magenta 32×32 fallback is returned
 *    instead of throwing an exception. The game will never crash on a missing sprite.
 *
 * USAGE:
 *   BufferedImage sprite = AssetManager.getInstance().get("player_blue");
 *   BufferedImage tile   = AssetManager.getInstance().get("floor");
 *   BufferedImage p1     = AssetManager.getInstance().getPlayerSprite(0);
 * ============================================================
 */
public class AssetManager {

    // ── Singleton ─────────────────────────────────────────────────────────────
    private static AssetManager instance;

    /** Call this once to load assets, then call getInstance() anywhere to retrieve them. */
    public static AssetManager getInstance() {
        if (instance == null) {
            instance = new AssetManager();
        }
        return instance;
    }

    // ── Internal image store ──────────────────────────────────────────────────
    private final Map<String, BufferedImage> images = new HashMap<>();

    /**
     * Fallback image returned when a sprite file is missing.
     * Solid magenta (255, 0, 255) — immediately obvious during testing.
     */
    private final BufferedImage fallback;

    // ── Constructor (private — use getInstance()) ─────────────────────────────
    private AssetManager() {
        fallback = makeMagenta(32, 32);
        loadAll();
    }

    // ── Loading ───────────────────────────────────────────────────────────────

    /** Load every asset the game uses. Add new sprites here as you create them. */
    private void loadAll() {
        // Player sprites
        load("player_blue",      "/assets/sprites/player_blue.png");
        load("player_red",       "/assets/sprites/player_red.png");
        load("player_green",     "/assets/sprites/player_green.png");
        load("player_yellow",    "/assets/sprites/player_yellow.png");

        // Enemy sprites
        load("aswang",           "/assets/sprites/aswang.png");
        load("tiktik",           "/assets/sprites/tiktik.png");
        load("tikbalang",        "/assets/sprites/tikbalang.png");

        // Bullet sprites
        load("bullet_salt",      "/assets/sprites/bullet_salt.png");
        load("bullet_holywater", "/assets/sprites/bullet_holywater.png");

        // Power-up icons
        load("powerup_speed",    "/assets/sprites/powerup_speed.png");
        load("powerup_damage",   "/assets/sprites/powerup_damage.png");
        load("powerup_heal",     "/assets/sprites/powerup_heal.png");
        load("powerup_atk",      "/assets/sprites/powerup_atk.png");

        // Environment tiles
        load("floor",            "/assets/tiles/floor.png");
        load("border",           "/assets/tiles/border.png");
        load("main_bg",          "/assets/tiles/main_bg.png");

        // Main menu button assets
        load("host_button",      "/assets/tiles/host_button.png");
        load("join_button",      "/assets/tiles/join_button.png");
        load("exit_button",      "/assets/tiles/exit_button.png");

        // Lobby assets
        load("lobby_bg",         "/assets/tiles/lobby_bg.png");
        load("slot",             "/assets/tiles/slot.png");
        load("back_button",      "/assets/tiles/back_button.png");
        load("ready_button",     "/assets/tiles/ready_button.png");
        load("start_button",     "/assets/tiles/start_button.png");
        
        // Game over / victory screen assets
        load("game_over_bg",     "/assets/tiles/game_over_screen.png");
        load("run_stats_button", "/assets/tiles/run_stats_button.png");
        load("back_to_lobby_button", "/assets/tiles/back_to_lobby_button.png");
    }

    /**
     * Loads one image from the classpath.
     * On failure (missing file or IO error), stores the magenta fallback instead.
     *
     * @param key  The name you use to retrieve this image later.
     * @param path Classpath path (e.g. "/assets/sprites/player_blue.png").
     */
    private void load(String key, String path) {
        try {
            InputStream is = getClass().getResourceAsStream(path);
            if (is == null) {
                System.out.println("[AssetManager] MISSING: " + path + " → magenta fallback");
                images.put(key, fallback);
                return;
            }
            images.put(key, ImageIO.read(is));
            System.out.println("[AssetManager] Loaded:   " + path);
        } catch (Exception e) {
            System.out.println("[AssetManager] ERROR loading " + path
                + ": " + e.getMessage() + " → magenta fallback");
            images.put(key, fallback);
        }
    }

    // ── Retrieval ─────────────────────────────────────────────────────────────

    /**
     * Returns the image for the given key.
     * If the key is unknown, returns the magenta fallback (never returns null).
     *
     * @param key The sprite key (e.g. "player_blue", "aswang", "floor").
     */
    public BufferedImage get(String key) {
        return images.getOrDefault(key, fallback);
    }

    /**
     * Convenience: returns the player sprite matching a 0-based player ID.
     *   0 → player_blue (P1)
     *   1 → player_red  (P2)
     *   2 → player_green (P3)
     *   3 → player_yellow (P4)
     */
    public BufferedImage getPlayerSprite(int playerId) {
        switch (playerId) {
            case 0:  return get("player_blue");
            case 1:  return get("player_red");
            case 2:  return get("player_green");
            case 3:  return get("player_yellow");
            default: return fallback;
        }
    }

    /**
     * Convenience: returns the enemy sprite by type name.
     * Accepted keys: "MELEE" → aswang, "RANGED" → tiktik, "SEMI_BOSS" → tikbalang
     */
    public BufferedImage getEnemySprite(String typeName) {
        switch (typeName.toUpperCase()) {
            case "MELEE":     return get("aswang");
            case "RANGED":    return get("tiktik");
            case "SEMI_BOSS": return get("tikbalang");
            default:          return fallback;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Creates a solid magenta BufferedImage of the given size. */
    private BufferedImage makeMagenta(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.MAGENTA);
        g.fillRect(0, 0, w, h);
        g.dispose();
        return img;
    }
}
