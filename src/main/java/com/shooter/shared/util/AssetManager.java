package com.shooter.shared.util;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URL;
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

    // ── Animated GIF store ────────────────────────────────────────────────────
    // ImageIcon preserves GIF animation; BufferedImage only captures frame 0.
    // Keys: "player_blue_down", "aswang_left", "tikbalang_up", etc.
    private final Map<String, ImageIcon> gifs = new HashMap<>();

    /**
     * Fallback image returned when a sprite file is missing.
     * Solid magenta (255, 0, 255) — immediately obvious during testing.
     */
    private final BufferedImage fallback;

    // ── Constructor (private — use getInstance()) ─────────────────────────────
    private AssetManager() {
        fallback = makeMagenta(32, 32);
        loadAll();
        loadAllGifs();
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
        load("victory_bg",       "/assets/tiles/victory_screen.png");
        load("run_stats_button", "/assets/tiles/run_stats_button.png");
        load("back_to_lobby_button", "/assets/tiles/back_to_lobby_button.png");
        load("paused_bg",        "/assets/tiles/paused_screen.png");
        load("resume_button",    "/assets/tiles/resume_button.png");
        load("exit_game_button", "/assets/tiles/exit_game_button.png");
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
            BufferedImage image = ImageIO.read(is);
            if (image == null) {
                System.out.println("[AssetManager] ERROR loading " + path + " → image decode returned null");
                images.put(key, fallback);
                return;
            }
            images.put(key, image);
            System.out.println("[AssetManager] Loaded:   " + path);
        } catch (Exception e) {
            System.out.println("[AssetManager] ERROR loading " + path
                + ": " + e.getMessage() + " → magenta fallback");
            images.put(key, fallback);
        }
    }

    // ── GIF Loading ──────────────────────────────────────────────────

    /**
     * Load all directional GIFs for players and enemies.
     * Missing files are silently skipped — callers fall back to static PNG.
     */
    private void loadAllGifs() {
        String[] dirs = {"down", "up", "left", "right"};

        // Player directional GIFs: player_blue_down.gif etc.
        String[] playerColors = {"blue", "red", "green", "yellow"};
        for (String color : playerColors) {
            for (String dir : dirs) {
                loadGif("player_" + color + "_" + dir,
                        "/assets/sprites/player_" + color + "_" + dir + ".gif");
            }
        }

        // Enemy directional GIFs: aswang_down.gif etc.
        String[] enemyTypes = {"aswang", "tiktik", "tikbalang"};
        for (String type : enemyTypes) {
            for (String dir : dirs) {
                loadGif(type + "_" + dir,
                        "/assets/sprites/" + type + "_" + dir + ".gif");
            }
        }
    }

    /**
     * Loads a GIF from the classpath as an ImageIcon (preserves animation).
     * Silently skips if the file is missing — no fallback stored, returns null.
     *
     * @param key  Retrieval key, e.g. "player_blue_down"
     * @param path Classpath path,  e.g. "/assets/sprites/player_blue_down.gif"
     */
    private void loadGif(String key, String path) {
        URL url = getClass().getResource(path);
        if (url == null) {
            // Not found — silently skip; getPlayerGif/getEnemyGif returns null
            System.out.println("[AssetManager] MISSING GIF: " + path);
            return;
        }
        gifs.put(key, new ImageIcon(url));
        System.out.println("[AssetManager] Loaded GIF: " + path);
    }

    // ── Retrieval ─────────────────────────────────────────────────────────────

    /**
     * Returns the image for the given key.
     * If the key is unknown, returns the magenta fallback (never returns null).
     */
    public BufferedImage get(String key) {
        BufferedImage image = images.get(key);
        return image != null ? image : fallback;
    }

    /**
     * Returns the directional animated GIF for a player.
     * Returns null if the GIF file was not found (caller should fall back to static PNG).
     *
     * @param playerId 0=blue, 1=red, 2=green, 3=yellow
     * @param dir      Direction enum value
     */
    public ImageIcon getPlayerGif(int playerId, Direction dir) {
        if (dir == null) return null; // transient field is null after deserialization
        String color;
        switch (playerId) {
            case 0:  color = "blue";   break;
            case 1:  color = "red";    break;
            case 2:  color = "green";  break;
            case 3:  color = "yellow"; break;
            default: return null;
        }
        return gifs.get("player_" + color + "_" + dir.name().toLowerCase());
    }

    /**
     * Returns the directional animated GIF for an enemy type.
     * Returns null if the GIF file was not found (caller should fall back to static PNG).
     *
     * @param typeName Enemy.Type name: "MELEE", "RANGED", "SEMI_BOSS"
     * @param dir      Direction enum value
     */
    public ImageIcon getEnemyGif(String typeName, Direction dir) {
        if (dir == null) return null; // transient field is null after deserialization
        String sprite;
        switch (typeName.toUpperCase()) {
            case "MELEE":     sprite = "aswang";    break;
            case "RANGED":    sprite = "tiktik";    break;
            case "SEMI_BOSS": sprite = "tikbalang"; break;
            default:          return null;
        }
        return gifs.get(sprite + "_" + dir.name().toLowerCase());
    }

    /**
     * Convenience: returns the player static-PNG sprite matching a 0-based player ID.
     * Used as fallback when directional GIF is missing.
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
     * Convenience: returns the enemy static-PNG sprite by type name.
     * Used as fallback when directional GIF is missing.
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
