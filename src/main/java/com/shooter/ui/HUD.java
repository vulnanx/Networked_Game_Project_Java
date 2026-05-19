package com.shooter.ui;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import com.shooter.shared.model.GameState;
import com.shooter.shared.model.Player;
import com.shooter.shared.util.Constants;

/**
 * ============================================================
 * FILE: HUD.java
 * PACKAGE: ui
 * OWNER: Member A (UI)
 * ============================================================
 *
 * RESPONSIBILITY:
 * Draws overlay UI:
 * - HP
 * - Round & enemy count
 * - Power-up pickup notifications (timed banners)
 * - Per-player stats panel showing stacked power-up effects
 *
 * WHAT NOT TO PUT HERE:
 * - Game logic
 * - Input handling
 *
 * CONNECTS TO:
 * GamePanel (called during render)
 * ============================================================
 */
public class HUD {

    // ─── NOTIFICATION SYSTEM ────────────────────────────────────────────────
    /** A single timed power-up notification banner. */
    private static class Notification {
        String message;
        int ticksLeft;

        Notification(String message, int durationTicks) {
            this.message = message;
            this.ticksLeft = durationTicks;
        }
    }

    /**
     * Duration a notification stays on screen (ticks — 180 ≈ 3 seconds at 60 FPS).
     */
    private static final int NOTIFICATION_DURATION = 180;

    /** Max notifications shown at once. Oldest are pushed off the top. */
    private static final int MAX_NOTIFICATIONS = 4;

    private final List<Notification> notifications = new ArrayList<>();

    // ─── FONTS ──────────────────────────────────────────────────────────────
    private static final Font FONT_NORMAL = new Font("Monospaced", Font.PLAIN, 13);
    private static final Font FONT_BOLD = new Font("Monospaced", Font.BOLD, 13);
    private static final Font FONT_NOTIFY = new Font("Monospaced", Font.BOLD, 14);
    private static final Font FONT_HEADER = new Font("Monospaced", Font.BOLD, 12);

    // ─── COLORS ─────────────────────────────────────────────────────────────
    private static final Color COLOR_BG = new Color(0, 0, 0, 140);
    private static final Color COLOR_WHITE = Color.WHITE;
    private static final Color COLOR_GOLD = new Color(255, 215, 0);
    private static final Color COLOR_HP_BAR = new Color(70, 210, 100);
    private static final Color COLOR_HP_BG = new Color(60, 60, 60);
    private static final Color COLOR_NOTIFY_BG = new Color(20, 20, 40, 200);
    private static final Color COLOR_NOTIFY_TXT = new Color(255, 220, 80);
    private static final Color COLOR_STAT_LABEL = new Color(160, 200, 255);

    // ─── LAYOUT CONSTANTS ───────────────────────────────────────────────────
    private static final int MARGIN = 10;
    private static final int PANEL_PADDING = 6;
    private static final int LINE_H = 16;
    private static final int HP_BAR_W = 120;
    private static final int HP_BAR_H = 8;

    // =========================================================================
    // PUBLIC API
    // =========================================================================

    /**
     * Call this from GamePanel when a player collects a power-up.
     * The notification will be shown on everyone's HUD.
     *
     * @param playerName  name of the player who collected it
     * @param powerUpType the type name, e.g. "DAMAGE"
     */
    public void notifyPowerUp(String playerName, String powerUpType) {
        String icon = iconFor(powerUpType);
        String msg = icon + " " + playerName + " picked up " + friendlyName(powerUpType) + "!";

        // Remove oldest if at capacity
        if (notifications.size() >= MAX_NOTIFICATIONS) {
            notifications.remove(0);
        }
        notifications.add(new Notification(msg, NOTIFICATION_DURATION));
    }

    /**
     * Call this from GamePanel when a player collects a power-up but the stat is
     * already at maximum cap.
     *
     * @param playerName  name of the player who collected it
     * @param powerUpType the type name, e.g. "DAMAGE"
     */
    public void notifyPowerUpCapped(String playerName, String powerUpType) {
        String icon = iconFor(powerUpType);
        String msg = icon + " " + playerName + " cannot apply " + friendlyName(powerUpType) + " (At Cap)!";

        // Remove oldest if at capacity
        if (notifications.size() >= MAX_NOTIFICATIONS) {
            notifications.remove(0);
        }
        notifications.add(new Notification(msg, NOTIFICATION_DURATION));
    }

    /**
     * Tick down all active notifications. Call once per game tick.
     */
    public void tick() {
        notifications.removeIf(n -> --n.ticksLeft <= 0);
    }

    /**
     * Main render call — draws everything on-screen.
     *
     * @param g                     graphics context
     * @param state                 current game state (all players)
     * @param killedEnemies         enemies killed this round
     * @param totalEnemiesThisRound total enemies this round
     * @param playerSpawnCooldown   ticks remaining before the player respawns
     */
    public void render(Graphics2D g, GameState state, int killedEnemies, int totalEnemiesThisRound,
            int playerSpawnCooldown) {
        setupRenderingHints(g);

        drawTopBar(g, state, killedEnemies, totalEnemiesThisRound, playerSpawnCooldown);
        drawPlayerStatsPanel(g, state);
        drawNotifications(g);
    }

    // =========================================================================
    // PRIVATE DRAWING HELPERS
    // =========================================================================

    /** Top-left: round info and own HP bar. */
    private void drawTopBar(Graphics2D g, GameState state, int killed, int total, int playerSpawnCooldown) {
        int x = MARGIN;
        int y = MARGIN;

        List<Player> players = state.getPlayers();
        int panelHeight = 50 + (players.size() * 16);

        // Semi-transparent background panel
        g.setColor(COLOR_BG);
        g.fillRoundRect(x - PANEL_PADDING, y - PANEL_PADDING,
                220, panelHeight, 8, 8);

        g.setFont(FONT_BOLD);
        g.setColor(COLOR_GOLD);
        g.drawString("Round: " + state.getCurrentRound() + " / " + Constants.TOTAL_ROUNDS,
                x, y + LINE_H);

        g.setFont(FONT_NORMAL);
        g.setColor(COLOR_WHITE);
        g.drawString("Enemies: " + killed + " / " + total, x, y + LINE_H * 2);

        int barY = y + LINE_H * 2 + 6;
        for (Player p : players) {
            g.setFont(FONT_BOLD);
            g.setColor(Color.WHITE);
            g.drawString("P" + (p.getPlayerId() + 1), x, barY + 8);

            if (p.isAlive()) {
                drawHpBar(g, x + 25, barY, p.getHp(), p.getMaxHp());
            } else {
                g.setFont(FONT_NORMAL);
                g.setColor(new Color(255, 80, 80));
                if (p.getPlayerId() == state.getLocalPlayerId()) {
                    int seconds = (int) Math.ceil((double) playerSpawnCooldown / Constants.TARGET_FPS);
                    g.drawString("Respawning in " + seconds + "s...", x + 25, barY + 8);
                } else {
                    g.drawString("DEAD", x + 25, barY + 8);
                }
            }
            barY += 16;
        }
    }

    /** HP bar with fill and label. */
    private void drawHpBar(Graphics2D g, int x, int y, int hp, int maxHp) {
        // Background
        g.setColor(COLOR_HP_BG);
        g.fillRoundRect(x, y, HP_BAR_W, HP_BAR_H, 4, 4);

        // Fill (clamp to 0–1)
        float pct = maxHp > 0 ? Math.max(0f, Math.min(1f, (float) hp / maxHp)) : 0f;
        g.setColor(COLOR_HP_BAR);
        g.fillRoundRect(x, y, (int) (HP_BAR_W * pct), HP_BAR_H, 4, 4);

        // Label
        g.setFont(FONT_NORMAL);
        g.setColor(COLOR_WHITE);
        g.drawString("HP: " + hp + " / " + maxHp, x + HP_BAR_W + 6, y + HP_BAR_H - 1);
    }

    /**
     * Right side: stats panel for every player showing their stacked power-up
     * stats.
     * All players in the game are listed so teammates can see everyone's buffs.
     */
    private void drawPlayerStatsPanel(Graphics2D g, GameState state) {
        List<Player> players = state.getPlayers();
        if (players.isEmpty())
            return;

        int panelW = 210;
        int rowsPerP = 6; // name + hp + spd + dmg + atkspd + powerups collected
        int panelH = (rowsPerP * LINE_H + PANEL_PADDING * 2) * players.size() + PANEL_PADDING;
        int x = Constants.SCREEN_WIDTH - panelW - MARGIN;
        int y = MARGIN;

        // Panel background
        g.setColor(COLOR_BG);
        g.fillRoundRect(x - PANEL_PADDING, y - PANEL_PADDING, panelW + PANEL_PADDING * 2, panelH, 8, 8);

        int cursor = y + PANEL_PADDING;

        for (Player p : players) {
            // Player name header
            g.setFont(FONT_HEADER);
            g.setColor(COLOR_GOLD);
            String header = (p == state.getMainPlayer() ? "► " : "  ") + p.getName();
            g.drawString(header, x, cursor + LINE_H);
            cursor += LINE_H + 2;

            // Stats rows
            g.setFont(FONT_NORMAL);

            drawStatRow(g, x, cursor, "HP", p.getHp() + " / " + p.getMaxHp());
            cursor += LINE_H;

            drawStatRow(g, x, cursor, "Speed", String.format("%.1f", p.getSpeed()));
            cursor += LINE_H;

            drawStatRow(g, x, cursor, "Damage", String.valueOf(p.getDamage()));
            cursor += LINE_H;

            drawStatRow(g, x, cursor, "Atk Spd", p.getShootCooldown() + " ticks");
            cursor += LINE_H;

            // Power-up count summary
            List<String> pups = p.getActivePowerUps();
            int dmgCount = countOf(pups, "DAMAGE");
            int hpCount = countOf(pups, "HP");
            int spdCount = countOf(pups, "MOVEMENT");
            int atkCount = countOf(pups, "ATTACK_SPEED");
            String puSummary = buildPuSummary(dmgCount, hpCount, spdCount, atkCount);

            drawStatRow(g, x, cursor, "Buffs", puSummary);
            cursor += LINE_H + PANEL_PADDING;
        }
    }

    /** Draws a label + value pair with colour-coded label. */
    private void drawStatRow(Graphics2D g, int x, int y, String label, String value) {
        g.setColor(COLOR_STAT_LABEL);
        g.setFont(FONT_NORMAL);
        g.drawString(label + ":", x + 4, y);

        g.setColor(COLOR_WHITE);
        g.drawString(value, x + 70, y);
    }

    /**
     * Bottom-centre: stacked notification banners when a power-up is collected.
     * Newest appears at the bottom; fades in opacity as it ages.
     */
    private void drawNotifications(Graphics2D g) {
        if (notifications.isEmpty())
            return;

        int centreX = Constants.SCREEN_WIDTH / 2;
        int baseY = Constants.SCREEN_HEIGHT - 60;
        int notifW = 360;
        int notifH = 22;

        for (int i = 0; i < notifications.size(); i++) {
            Notification n = notifications.get(i);

            // Fade out in the last 60 ticks
            float alpha = Math.min(1f, (float) n.ticksLeft / 60f);
            int y = baseY - i * (notifH + 4);

            // Background
            g.setColor(new Color(
                    COLOR_NOTIFY_BG.getRed(),
                    COLOR_NOTIFY_BG.getGreen(),
                    COLOR_NOTIFY_BG.getBlue(),
                    (int) (200 * alpha)));
            g.fillRoundRect(centreX - notifW / 2, y, notifW, notifH, 6, 6);

            // Border
            g.setColor(new Color(255, 220, 80, (int) (180 * alpha)));
            g.drawRoundRect(centreX - notifW / 2, y, notifW, notifH, 6, 6);

            // Text
            g.setFont(FONT_NOTIFY);
            g.setColor(new Color(
                    COLOR_NOTIFY_TXT.getRed(),
                    COLOR_NOTIFY_TXT.getGreen(),
                    COLOR_NOTIFY_TXT.getBlue(),
                    (int) (255 * alpha)));

            FontMetrics fm = g.getFontMetrics();
            int textX = centreX - fm.stringWidth(n.message) / 2;
            g.drawString(n.message, textX, y + notifH - 5);
        }
    }

    // =========================================================================
    // UTILITIES
    // =========================================================================

    private void setupRenderingHints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    private int countOf(List<String> list, String type) {
        int count = 0;
        for (String s : list) {
            if (s.equals(type))
                count++;
        }
        return count;
    }

    private String buildPuSummary(int dmg, int hp, int spd, int atk) {
        StringBuilder sb = new StringBuilder();
        if (dmg > 0)
            sb.append("DMG×").append(dmg).append(" ");
        if (hp > 0)
            sb.append("HP×").append(hp).append(" ");
        if (spd > 0)
            sb.append("SPD×").append(spd).append(" ");
        if (atk > 0)
            sb.append("ATK×").append(atk).append(" ");
        return sb.length() > 0 ? sb.toString().trim() : "none";
    }

    private String friendlyName(String type) {
        switch (type) {
            case "DAMAGE":
                return "Damage Boost";
            case "HP":
                return "Health Pack";
            case "ATTACK_SPEED":
                return "Attack Speed";
            case "MOVEMENT":
                return "Speed Boost";
            default:
                return type;
        }
    }

    private String iconFor(String type) {
        switch (type) {
            case "DAMAGE":
                return "[ATK]";
            case "HP":
                return "[HP+]";
            case "ATTACK_SPEED":
                return "[SPD]";
            case "MOVEMENT":
                return "[MOV]";
            default:
                return "[???]";
        }
    }
}