package com.shooter.client.screens;

import com.shooter.client.GameClient;
import com.shooter.client.ScreenManager;
import com.shooter.shared.util.Constants;
import com.shooter.shared.util.GameSettings;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.concurrent.atomic.AtomicReference;

/**
 * ============================================================
 * FILE: SettingsScreen.java
 * PACKAGE: client.screens
 * OWNER: Member C (Screens / Systems / Assets)
 * ============================================================
 *
 * Pre-game settings panel.  Only the host can interact with it;
 * non-host clients see a read-only overlay.
 *
 * Changes are sent to the server in real time via
 * GameClient.sendSettings(), which broadcasts them to all
 * connected clients so every LobbyScreen shows the same values.
 *
 * LAYER RULE: No game logic. No direct socket calls beyond
 * what GameClient exposes.
 * ============================================================
 */
public class SettingsScreen implements Screen {

    private final ScreenManager screenManager;
    private final GameClient    gameClient;
    private final boolean       isHost;
    private final Runnable      onBack;

    // Working copy — edited locally, sent to server on each change.
    // Stored in an AtomicReference so the settingsListener (called on the
    // EDT by GameClient) can safely swap it while render() reads it.
    private final AtomicReference<GameSettings> settingsRef;

    // ── Layout constants ─────────────────────────────────────────────────────
    private static final int PANEL_W = 680;
    private static final int PANEL_H = 620;
    private static final int PANEL_X = (Constants.SCREEN_WIDTH  - PANEL_W) / 2;
    private static final int PANEL_Y = (Constants.SCREEN_HEIGHT - PANEL_H) / 2;

    // Row geometry
    private static final int ROW_H    = 44;
    private static final int FIRST_Y  = PANEL_Y + 100;
    private static final int LABEL_X  = PANEL_X + 30;
    private static final int VALUE_X  = PANEL_X + PANEL_W - 220;
    private static final int BTN_SIZE = 28;

    // ── Section headers: list of (title, firstRowIndex) pairs ───────────────
    //  Rows are ordered as they appear on screen.
    //  0 = Player HP       6 = Powerup drop %
    //  1 = Player Speed    7 = Total Rounds
    //  2 = Damage          8 = Spawn cooldown
    //  3 = Shoot CD        ...
    //  4 = Hit CD
    //  5 = Max Speed
    private static final int ROW_COUNT = 10;

    // Row labels shown to the user
    private static final String[] ROW_LABELS = {
        "Player Base HP",
        "Player Base Speed",
        "Player Base Damage",
        "Shoot Cooldown (ticks)",
        "Hit Invincibility (ticks)",
        "Max Speed",
        "Power-Up Drop Chance (%)",
        "Total Rounds",
        "Enemy Spawn Cooldown",
        "Melee Enemy Speed",
    };

    // ── Back button ──────────────────────────────────────────────────────────
    private final Rectangle backBtn;
    private boolean backHovered = false;

    // ── Reset button ─────────────────────────────────────────────────────────
    private final Rectangle resetBtn;
    private boolean resetHovered = false;

    // ── Per-row ± button rectangles ──────────────────────────────────────────
    private final Rectangle[] minusBtns = new Rectangle[ROW_COUNT];
    private final Rectangle[] plusBtns  = new Rectangle[ROW_COUNT];
    private final boolean[]   minusHov  = new boolean[ROW_COUNT];
    private final boolean[]   plusHov   = new boolean[ROW_COUNT];

    // ── Animation ────────────────────────────────────────────────────────────
    private int tick = 0;

    // =========================================================================

    public SettingsScreen(ScreenManager screenManager, GameClient gameClient,
                          boolean isHost, Runnable onBack) {
        this.screenManager = screenManager;
        this.gameClient    = gameClient;
        this.isHost        = isHost;
        this.onBack        = onBack;


        this.settingsRef = new AtomicReference<>(
                (gameClient != null) ? gameClient.getLastKnownSettings() : new GameSettings());

        // Build button hit areas
        for (int i = 0; i < ROW_COUNT; i++) {
            int rowY = FIRST_Y + i * ROW_H + (ROW_H - BTN_SIZE) / 2;
            minusBtns[i] = new Rectangle(VALUE_X,              rowY, BTN_SIZE, BTN_SIZE);
            plusBtns[i]  = new Rectangle(VALUE_X + BTN_SIZE + 90, rowY, BTN_SIZE, BTN_SIZE);
        }

        backBtn  = new Rectangle(PANEL_X + 20,  PANEL_Y + PANEL_H - 58, 130, 40);
        resetBtn = new Rectangle(PANEL_X + PANEL_W - 160, PANEL_Y + PANEL_H - 58, 140, 40);
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override public void onEnter() {
        tick = 0;
        System.out.println("[SettingsScreen] Opened. isHost=" + isHost);
        // Stay in sync with server re-broadcasts (e.g. lobby state changes
        // triggering a SETTINGS echo while we have this screen open).
        if (gameClient != null) {
            gameClient.setSettingsListener(s -> settingsRef.set(s));
        }
    }

    @Override public void onExit() {
        // Unregister so the LobbyScreen can re-register its own listener
        if (gameClient != null) {
            gameClient.setSettingsListener(null);
        }
        System.out.println("[SettingsScreen] Closed.");
    }

    @Override public void update() { tick++; }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void render(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);

        drawBackground(g);
        drawPanel(g);
        drawTitle(g);
        drawRows(g);
        drawFooterButtons(g);

        if (!isHost) {
            drawReadOnlyOverlay(g);
        }
    }

    private void drawBackground(Graphics2D g) {
        // Dim the lobby behind this overlay
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, 0, Constants.SCREEN_WIDTH, Constants.SCREEN_HEIGHT);
    }

    private void drawPanel(Graphics2D g) {
        // Card background
        g.setColor(new Color(0x0F1128));
        g.fillRoundRect(PANEL_X, PANEL_Y, PANEL_W, PANEL_H, 14, 14);

        // Glowing border
        float glow = 0.55f + 0.45f * (float) Math.abs(Math.sin(tick * 0.04));
        g.setColor(new Color(
                (int)(80  * glow),
                (int)(100 * glow),
                (int)(220 * glow)));
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(PANEL_X, PANEL_Y, PANEL_W, PANEL_H, 14, 14);

        // Separator under title
        g.setColor(new Color(0x334466));
        g.setStroke(new BasicStroke(1f));
        g.drawLine(PANEL_X + 20, PANEL_Y + 82, PANEL_X + PANEL_W - 20, PANEL_Y + 82);
    }

    private void drawTitle(Graphics2D g) {
        g.setFont(new Font("Monospaced", Font.BOLD, 26));
        g.setColor(new Color(0xF5A623));
        String t = isHost ? "GAME SETTINGS  [HOST]" : "GAME SETTINGS  [VIEW ONLY]";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(t, PANEL_X + PANEL_W / 2 - fm.stringWidth(t) / 2, PANEL_Y + 54);
    }

    private void drawRows(Graphics2D g) {
        for (int i = 0; i < ROW_COUNT; i++) {
            int rowY = FIRST_Y + i * ROW_H;
            drawOneRow(g, i, rowY);
        }
    }

    private void drawOneRow(Graphics2D g, int index, int rowY) {
        // Subtle alternating row background
        if (index % 2 == 0) {
            g.setColor(new Color(255, 255, 255, 10));
            g.fillRect(PANEL_X + 10, rowY, PANEL_W - 20, ROW_H - 2);
        }

        // Label
        g.setFont(new Font("Monospaced", Font.PLAIN, 14));
        g.setColor(new Color(0xA8B4D8));
        g.drawString(ROW_LABELS[index], LABEL_X, rowY + 28);

        // Value
        String val = formatValue(index);
        g.setFont(new Font("Monospaced", Font.BOLD, 15));
        g.setColor(Color.WHITE);
        FontMetrics fm = g.getFontMetrics();
        int valCenterX = VALUE_X + BTN_SIZE + 45; // centre of the value area
        g.drawString(val, valCenterX - fm.stringWidth(val) / 2, rowY + 28);

        // − button
        if (isHost) {
            drawAdjustBtn(g, minusBtns[index], "−", minusHov[index]);
            drawAdjustBtn(g, plusBtns[index],  "+", plusHov[index]);
        }
    }

    private void drawAdjustBtn(Graphics2D g, Rectangle btn, String label, boolean hovered) {
        g.setColor(hovered ? new Color(0xF5A623) : new Color(0x1E2050));
        g.fillRoundRect(btn.x, btn.y, btn.width, btn.height, 6, 6);
        g.setColor(hovered ? new Color(0xFFD700) : new Color(0x4455AA));
        g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect(btn.x, btn.y, btn.width, btn.height, 6, 6);

        g.setFont(new Font("Monospaced", Font.BOLD, 16));
        g.setColor(hovered ? Color.BLACK : Color.WHITE);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(label,
                btn.x + btn.width  / 2 - fm.stringWidth(label) / 2,
                btn.y + btn.height / 2 + fm.getAscent() / 2 - 3);
    }

    private void drawFooterButtons(Graphics2D g) {
        // Back
        boolean bh = backHovered && isHost; // dim back only when host (non-host always has it)
        g.setColor((backHovered) ? new Color(0xE05C5C) : new Color(0x2A1A1A));
        g.fillRoundRect(backBtn.x, backBtn.y, backBtn.width, backBtn.height, 8, 8);
        g.setColor(backHovered ? new Color(0xFF8888) : new Color(0x663333));
        g.setStroke(new BasicStroke(2));
        g.drawRoundRect(backBtn.x, backBtn.y, backBtn.width, backBtn.height, 8, 8);
        g.setFont(new Font("Monospaced", Font.BOLD, 13));
        g.setColor(Color.WHITE);
        g.drawString("<- BACK", backBtn.x + 14, backBtn.y + 26);

        // Reset (host only)
        if (isHost) {
            g.setColor(resetHovered ? new Color(0x3A5C8A) : new Color(0x1A2A3A));
            g.fillRoundRect(resetBtn.x, resetBtn.y, resetBtn.width, resetBtn.height, 8, 8);
            g.setColor(resetHovered ? new Color(0x6699CC) : new Color(0x334466));
            g.setStroke(new BasicStroke(2));
            g.drawRoundRect(resetBtn.x, resetBtn.y, resetBtn.width, resetBtn.height, 8, 8);
            g.setFont(new Font("Monospaced", Font.BOLD, 13));
            g.setColor(Color.WHITE);
            g.drawString("RESET DEFAULTS", resetBtn.x + 8, resetBtn.y + 26);
        }
    }

    /** Grayed-out "VIEW ONLY" banner for non-host clients. */
    private void drawReadOnlyOverlay(Graphics2D g) {
        g.setFont(new Font("Monospaced", Font.ITALIC, 12));
        g.setColor(new Color(0xFF8888));
        String msg = "Only the host can change settings.";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(msg,
                PANEL_X + PANEL_W / 2 - fm.stringWidth(msg) / 2,
                PANEL_Y + PANEL_H - 14);
    }

    // ── Value formatting ──────────────────────────────────────────────────────

    private String formatValue(int row) {
        GameSettings s = settingsRef.get();
        switch (row) {
            case 0: return String.valueOf(s.getPlayerBaseHp());
            case 1: return String.format("%.1f", s.getPlayerBaseSpeed());
            case 2: return String.valueOf(s.getPlayerBaseDamage());
            case 3: return s.getPlayerShootCooldown() + " tks";
            case 4: return s.getPlayerHitCooldown() + " tks";
            case 5: return String.format("%.1f", s.getPlayerMaxSpeed());
            case 6: return String.format("%.0f%%", s.getPowerUpDropChance() * 100f);
            case 7: return s.getTotalRounds() + " rounds";
            case 8: return s.getEnemySpawnCooldown() + " tks";
            case 9: return String.format("%.1f", s.getMeleeEnemySpeed());
            default: return "?";
        }
    }

    // ── Adjustment logic ──────────────────────────────────────────────────────

    /** Large step sizes feel more game-tuning-friendly than ±1 for everything. */
    private void increment(int row) {
        GameSettings s = settingsRef.get();
        switch (row) {
            case 0: s.setPlayerBaseHp(s.getPlayerBaseHp() + 10); break;
            case 1: s.setPlayerBaseSpeed(Math.round((s.getPlayerBaseSpeed() + 0.5f) * 10f) / 10f); break;
            case 2: s.setPlayerBaseDamage(s.getPlayerBaseDamage() + 1); break;
            case 3: s.setPlayerShootCooldown(s.getPlayerShootCooldown() + 1); break;
            case 4: s.setPlayerHitCooldown(s.getPlayerHitCooldown() + 5); break;
            case 5: s.setPlayerMaxSpeed(Math.round((s.getPlayerMaxSpeed() + 0.5f) * 10f) / 10f); break;
            case 6: s.setPowerUpDropChance(Math.min(1f, s.getPowerUpDropChance() + 0.05f)); break;
            case 7: s.setTotalRounds(s.getTotalRounds() + 1); break;
            case 8: s.setEnemySpawnCooldown(s.getEnemySpawnCooldown() + 5); break;
            case 9: s.setMeleeEnemySpeed(Math.round((s.getMeleeEnemySpeed() + 0.1f) * 10f) / 10f); break;
        }
        pushSettings(s);
    }

    private void decrement(int row) {
        GameSettings s = settingsRef.get();
        switch (row) {
            case 0: s.setPlayerBaseHp(Math.max(10, s.getPlayerBaseHp() - 10)); break;
            case 1: s.setPlayerBaseSpeed(Math.max(0.5f, Math.round((s.getPlayerBaseSpeed() - 0.5f) * 10f) / 10f)); break;
            case 2: s.setPlayerBaseDamage(Math.max(1, s.getPlayerBaseDamage() - 1)); break;
            case 3: s.setPlayerShootCooldown(Math.max(1, s.getPlayerShootCooldown() - 1)); break;
            case 4: s.setPlayerHitCooldown(Math.max(0, s.getPlayerHitCooldown() - 5)); break;
            case 5: s.setPlayerMaxSpeed(Math.max(s.getPlayerBaseSpeed(), Math.round((s.getPlayerMaxSpeed() - 0.5f) * 10f) / 10f)); break;
            case 6: s.setPowerUpDropChance(Math.max(0f, s.getPowerUpDropChance() - 0.05f)); break;
            case 7: s.setTotalRounds(Math.max(1, s.getTotalRounds() - 1)); break;
            case 8: s.setEnemySpawnCooldown(Math.max(5, s.getEnemySpawnCooldown() - 5)); break;
            case 9: s.setMeleeEnemySpeed(Math.max(0.1f, Math.round((s.getMeleeEnemySpeed() - 0.1f) * 10f) / 10f)); break;
        }
        pushSettings(s);
    }

    /** Sends the working copy to the server so it re-broadcasts to all clients. */
    private void pushSettings(GameSettings s) {
        if (gameClient != null && isHost) {
            gameClient.sendSettings(s);
        }
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    @Override
    public void handleMouseClicked(int x, int y) {
        if (backBtn.contains(x, y)) {
            if (onBack != null) onBack.run();
            return;
        }

        if (!isHost) return;

        if (resetBtn.contains(x, y)) {
            GameSettings s = settingsRef.get();
            s.reset();
            pushSettings(s);
            return;
        }

        for (int i = 0; i < ROW_COUNT; i++) {
            if (minusBtns[i].contains(x, y)) { decrement(i); return; }
            if (plusBtns[i].contains(x, y))  { increment(i); return; }
        }
    }

    @Override
    public void handleMouseMoved(int x, int y) {
        backHovered  = backBtn.contains(x, y);
        resetHovered = resetBtn.contains(x, y);
        for (int i = 0; i < ROW_COUNT; i++) {
            minusHov[i] = minusBtns[i].contains(x, y);
            plusHov[i]  = plusBtns[i].contains(x, y);
        }
    }

    @Override
    public void handleKeyPressed(int keyCode) {
        if (keyCode == KeyEvent.VK_ESCAPE && onBack != null) {
            onBack.run();
        }
    }
}
