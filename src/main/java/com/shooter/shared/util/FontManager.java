package com.shooter.shared.util;

import java.awt.*;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * ============================================================
 * FILE: FontManager.java
 * PACKAGE: shared.util
 * OWNER: Geastin (Member C - Screens / Systems / Assets)
 * ============================================================
 *
 * Loads custom pixel/bitmap fonts for retro UI styling.
 * If a font file is missing, falls back to monospaced system font.
 *
 * USAGE:
 *   Font pixelFont = FontManager.getInstance().getFont("pixel", 16);
 *   g.setFont(pixelFont);
 *   g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
 *                      RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
 * ============================================================
 */
public class FontManager {

    private static FontManager instance;

    public static FontManager getInstance() {
        if (instance == null) {
            instance = new FontManager();
        }
        return instance;
    }

    private final Map<String, Font> fonts = new HashMap<>();

    private FontManager() {
        loadAll();
    }

    /** Load every custom font the game uses. Add new fonts here as needed. */
    private void loadAll() {
        // Try to load custom pixel fonts from resources
        loadFont("pixel", "/fonts/pixel_font.ttf");

        // Fallback fonts (always available as system fonts)
        loadSystemFont("monospace", "Courier New", Font.BOLD);
        loadSystemFont("sans", "Arial", Font.BOLD);
    }

    /**
     * Loads a custom font from a TTF/OTF file in resources.
     * On failure, stores null (will use fallback when deriveFont is called).
     */
    private void loadFont(String key, String path) {
        try {
            InputStream is = getClass().getResourceAsStream(path);
            if (is == null) {
                System.out.println("[FontManager] MISSING: " + path);
                return;
            }
            Font customFont = Font.createFont(Font.TRUETYPE_FONT, is);
            fonts.put(key, customFont);
            System.out.println("[FontManager] Loaded: " + path);
        } catch (Exception e) {
            System.out.println("[FontManager] ERROR loading " + path + ": " + e.getMessage());
        }
    }

    /**
     * Register a system font (no file needed).
     */
    private void loadSystemFont(String key, String fontName, int style) {
        Font sysFont = new Font(fontName, style, 16);
        fonts.put(key, sysFont);
        System.out.println("[FontManager] Loaded system font: " + fontName);
    }

    /**
     * Returns a font at the requested size.
     * Falls back to Courier New (monospaced) if the custom font is missing.
     *
     * @param key  The font key (e.g. "pixel", "monospace", "sans").
     * @param size The font size in points.
     */
    public Font getFont(String key, float size) {
        Font base = fonts.get(key);
        if (base == null) {
            // Fallback: use system monospaced
            base = new Font("Courier New", Font.BOLD, (int) size);
        }
        return base.deriveFont(size);
    }

    /**
     * Returns a font at the requested size and style.
     */
    public Font getFont(String key, float size, int style) {
        Font base = fonts.get(key);
        if (base == null) {
            base = new Font("Courier New", style, (int) size);
        }
        return base.deriveFont(style, size);
    }
}
