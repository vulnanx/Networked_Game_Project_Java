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
 * OWNER: Member A (UI) / Polished by Geastin (Day 4)
 * ============================================================
 *
 * RESPONSIBILITY:
 * Draws overlay UI:
 * - HP bars (color-coded: green → yellow → red)
 * - Round counter & enemy kill progress
 * - Power-up pickup notifications (timed banners)
 * - Per-player stats panel showing stacked power-up effects
 * - Power-up indicator icons under each player's HP bar
 * - Round start banner ("ROUND X" flash)
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

    // ─── ROUND BANNER ────────────────────────────────────────────────────────
    /** Ticks remaining for the "ROUND X" centre banner. */
    private int roundBannerTicks = 0;
    private int roundBannerRound = 1;
    private static final int ROUND_BANNER_DURATION = 120; // 2 seconds at 60 FPS

    // ─── FONTS ──────────────────────────────────────────────────────────────
    private static final Font FONT_NORMAL = new Font("Monospaced", Font.PLAIN, 13);
    private static final Font FONT_BOLD = new Font("Monospaced", Font.BOLD, 13);
    private static final Font FONT_NOTIFY = new Font("Monospaced", Font.BOLD, 14);
    private static final Font FONT_HEADER = new Font("Monospaced", Font.BOLD, 12);
    private static final Font FONT_ROUND_BANNER = new Font("Monospaced", Font.BOLD, 48);
    private static final Font FONT_ROUND_SUB = new Font("Monospaced", Font.PLAIN, 16);

    // ─── COLORS ─────────────────────────────────────────────────────────────
    private static final Color COLOR_BG = new Color(0, 0, 0, 140);
    private static final Color COLOR_WHITE = Color.WHITE;
    private static final Color COLOR_GOLD = new Color(255, 215, 0);
    private static final Color COLOR_HP_BG = new Color(60, 60, 60);
    private static final Color COLOR_NOTIFY_BG = new Color(20, 20, 40, 200);
    private static final Color COLOR_NOTIFY_TXT = new Color(255, 220, 80);
    private static final Color COLOR_STAT_LABEL = new Color(160, 200, 255);

    // Player colors (matches MainMenuScreen badges)
    private static final Color[] PLAYER_COLORS = {
            new Color(0x4A90D9), // P1 blue
            new Color(0xE05C5C), // P2 red
            new Color(0x50E878), // P3 green
            new Color(0xF5A623)  // P4 yellow
    };

    // Power-up indicator colors
    private static final Color COLOR_PU_DAMAGE = new Color(0xE05C5C);    // red
    private static final Color COLOR_PU_HP = new Color(0x50E878);         // green
    private static final Color COLOR_PU_SPEED = new Color(0x4A90D9);      // blue
    private static final Color COLOR_PU_ATKSPD = new Color(0xF5A623);     // orange

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
     * Show a "ROUND X" banner in the centre of the screen.
     * Called when a new round begins.
     */
    public void showRoundBanner(int round) {
        roundBannerRound = round;
        roundBannerTicks = ROUND_BANNER_DURATION;
    }

    /**
     * Tick down all active notifications and banners. Call once per game tick.
     */
    public void tick() {
        notifications.removeIf(n -> --n.ticksLeft <= 0);
        if (roundBannerTicks > 0) {
            roundBannerTicks--;
        }
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
        drawRoundBanner(g);
    }

    // =========================================================================
    // PRIVATE DRAWING HELPERS
    // =========================================================================

    /** Top-left: round info and per-player HP bars with power-up indicators. */
    private void drawTopBar(Graphics2D g, GameState state, int killed, int total, int playerSpawnCooldown) {
        int x = MARGIN;
        int y = MARGIN;

        List<Player> players = state.getPlayers();
        // Calculate panel height: round line + enemy line + per-player rows (HP bar + indicators)
        int panelHeight = 44 + (players.size() * 28);

        // Semi-transparent background panel
        g.setColor(COLOR_BG);
        g.fillRoundRect(x - PANEL_PADDING, y - PANEL_PADDING,
                260, panelHeight, 8, 8);

        // Round counter
        g.setFont(FONT_BOLD);
        g.setColor(COLOR_GOLD);
        g.drawString("Round " + state.getCurrentRound() + " / " + Constants.TOTAL_ROUNDS,
                x, y + LINE_H);

        // Enemy kill progress with progress indicator
        g.setFont(FONT_NORMAL);
        g.setColor(COLOR_WHITE);
        String enemyText = "Enemies: " + killed + " / " + total;
        g.drawString(enemyText, x, y + LINE_H * 2);

        // Mini progress bar for enemy kills
        if (total > 0) {
            int progX = x + 130;
            int progY = y + LINE_H * 2 - 7;
            int progW = 80;
            int progH = 5;
            float pct = Math.min(1f, (float) killed / total);

            g.setColor(COLOR_HP_BG);
            g.fillRoundRect(progX, progY, progW, progH, 3, 3);
            g.setColor(pct >= 1f ? COLOR_GOLD : new Color(100, 180, 255));
            g.fillRoundRect(progX, progY, (int) (progW * pct), progH, 3, 3);
        }

        // Per-player HP bars
        int barY = y + LINE_H * 2 + 10;
        for (Player p : players) {
            // Player label with team color
            int pid = p.getPlayerId();
            Color pColor = pid >= 0 && pid < PLAYER_COLORS.length ? PLAYER_COLORS[pid] : COLOR_WHITE;

            g.setFont(FONT_BOLD);
            g.setColor(pColor);
            g.drawString("P" + (pid + 1), x, barY + 8);

            if (p.isAlive()) {
                drawHpBar(g, x + 25, barY, p.getHp(), p.getMaxHp());

                // Power-up indicator dots below HP bar
                drawPowerUpIndicators(g, x + 25, barY + HP_BAR_H + 2, p);
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
            barY += 26; // extra space for power-up indicators
        }
    }

    /**
     * HP bar with dynamic color: green (>50%), yellow (25-50%), red (<25%).
     */
    private void drawHpBar(Graphics2D g, int x, int y, int hp, int maxHp) {
        // Background
        g.setColor(COLOR_HP_BG);
        g.fillRoundRect(x, y, HP_BAR_W, HP_BAR_H, 4, 4);

        // Fill with color based on health percentage
        float pct = maxHp > 0 ? Math.max(0f, Math.min(1f, (float) hp / maxHp)) : 0f;

        Color hpColor;
        if (pct > 0.5f) {
            hpColor = new Color(70, 210, 100);      // green — healthy
        } else if (pct > 0.25f) {
            hpColor = new Color(240, 200, 50);       // yellow — caution
        } else {
            hpColor = new Color(220, 60, 60);        // red — critical
        }

        g.setColor(hpColor);
        g.fillRoundRect(x, y, (int) (HP_BAR_W * pct), HP_BAR_H, 4, 4);

        // Label
        g.setFont(FONT_NORMAL);
        g.setColor(COLOR_WHITE);
        g.drawString(hp + "/" + maxHp, x + HP_BAR_W + 6, y + HP_BAR_H - 1);
    }

    /**
     * Small colored dots below each player's HP bar showing active power-up buffs.
     * Each dot = one power-up collected (color-coded by type).
     */
    private void drawPowerUpIndicators(Graphics2D g, int x, int y, Player p) {
        List<String> pups = p.getActivePowerUps();
        if (pups == null || pups.isEmpty()) return;

        int dotSize = 6;
        int spacing = 2;
        int cx = x;

        for (String type : pups) {
            g.setColor(colorForPowerUp(type));
            g.fillOval(cx, y, dotSize, dotSize);
            cx += dotSize + spacing;

            // Don't overflow the HP bar width
            if (cx > x + HP_BAR_W) break;
        }
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
            // Player name header with team color
            int pid = p.getPlayerId();
            Color pColor = pid >= 0 && pid < PLAYER_COLORS.length ? PLAYER_COLORS[pid] : COLOR_GOLD;

            g.setFont(FONT_HEADER);
            g.setColor(pColor);
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

            // Power-up count summary with colored icons
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
     * Centre screen: "ROUND X" banner that fades out.
     * Shown when a new round begins.
     */
    private void drawRoundBanner(Graphics2D g) {
        if (roundBannerTicks <= 0) return;

        // Fade out in the last 40 ticks
        float alpha = Math.min(1f, (float) roundBannerTicks / 40f);

        int cx = Constants.SCREEN_WIDTH / 2;
        int cy = Constants.SCREEN_HEIGHT / 2 - 40;

        // Dark backdrop
        g.setColor(new Color(0, 0, 0, (int) (120 * alpha)));
        g.fillRoundRect(cx - 180, cy - 40, 360, 90, 16, 16);

        // "ROUND X" title
        g.setFont(FONT_ROUND_BANNER);
        g.setColor(new Color(255, 215, 0, (int) (255 * alpha)));
        String text = "ROUND " + roundBannerRound;
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, cx - fm.stringWidth(text) / 2, cy + 10);

        // Subtitle
        g.setFont(FONT_ROUND_SUB);
        g.setColor(new Color(200, 200, 255, (int) (200 * alpha)));
        String sub = "Survive the wave!";
        fm = g.getFontMetrics();
        g.drawString(sub, cx - fm.stringWidth(sub) / 2, cy + 35);
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

    /** Returns the indicator color for a power-up type. */
    private Color colorForPowerUp(String type) {
        switch (type) {
            case "DAMAGE":       return COLOR_PU_DAMAGE;
            case "HP":           return COLOR_PU_HP;
            case "MOVEMENT":     return COLOR_PU_SPEED;
            case "ATTACK_SPEED": return COLOR_PU_ATKSPD;
            default:             return COLOR_WHITE;
        }
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