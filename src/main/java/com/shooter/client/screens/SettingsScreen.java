package com.shooter.client.screens;

import com.shooter.client.GameClient;
import com.shooter.client.ScreenManager;
import com.shooter.shared.util.Constants;
import com.shooter.shared.util.GameSettings;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicReference;

/**
 * ============================================================
 * FILE: SettingsScreen.java
 * PACKAGE: client.screens
 * OWNER: Member C — UI REDESIGN: Christel
 * ============================================================
 * Dark horror pixel art game settings panel.
 * All logic, constructor signatures, and networking preserved.
 * ============================================================
 */
public class SettingsScreen implements Screen {

    // ── Horror palette ────────────────────────────────────────────────────────
    private static final Color VOID_PURPLE   = new Color(0x1A, 0x0A, 0x2E);
    private static final Color TOXIC_GREEN   = new Color(0x39, 0xFF, 0x14);
    private static final Color BLOOD_RED     = new Color(0x8B, 0x00, 0x00);
    private static final Color BRIGHT_RED    = new Color(0xCC, 0x00, 0x00);
    private static final Color GHOSTLY_WHITE = new Color(0xE8, 0xE8, 0xF0);
    private static final Color DARK_BG       = new Color(0x0D, 0x05, 0x1E);
    private static final Color PURPLE_GLOW   = new Color(0x7B, 0x2F, 0xBE);
    private static final Color AMBER         = new Color(0xF5, 0xA6, 0x23);

    // ── Core refs ─────────────────────────────────────────────────────────────
    private final ScreenManager               screenManager;
    private final GameClient                  gameClient;
    private final boolean                     isHost;
    private final Runnable                    onBack;
    private final AtomicReference<GameSettings> settingsRef;

    // ── Layout ────────────────────────────────────────────────────────────────
    private static final int PANEL_W = 680;
    private static final int PANEL_H = 620;
    private static final int PANEL_X = (Constants.SCREEN_WIDTH  - PANEL_W) / 2;
    private static final int PANEL_Y = (Constants.SCREEN_HEIGHT - PANEL_H) / 2;
    private static final int ROW_H   = 44;
    private static final int FIRST_Y = PANEL_Y + 100;
    private static final int LABEL_X = PANEL_X + 30;
    private static final int VALUE_X  = PANEL_X + PANEL_W - 220;
    private static final int BTN_SIZE = 28;
    private static final int ROW_COUNT = 10;

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

    // Row accent colors — each stat gets its own personality
    private static final Color[] ROW_ACCENTS = {
        new Color(0xCC,0x00,0x00),  // HP — blood red
        new Color(0x39,0xFF,0x14),  // Speed — toxic green
        new Color(0xF5,0xA6,0x23),  // Damage — amber
        new Color(0x4A,0x90,0xD9),  // Shoot CD — blue
        new Color(0x7B,0x2F,0xBE),  // Hit CD — purple
        new Color(0x39,0xFF,0x14),  // Max speed — green
        new Color(0xF5,0xA6,0x23),  // Drop chance — amber
        new Color(0xCC,0x00,0x00),  // Rounds — blood red
        new Color(0x7B,0x2F,0xBE),  // Spawn CD — purple
        new Color(0x4A,0x90,0xD9),  // Melee spd — blue
    };

    // ── Buttons ───────────────────────────────────────────────────────────────
    private final Rectangle   backBtn;
    private final Rectangle   resetBtn;
    private boolean           backHovered  = false;
    private boolean           resetHovered = false;
    private final Rectangle[] minusBtns = new Rectangle[ROW_COUNT];
    private final Rectangle[] plusBtns  = new Rectangle[ROW_COUNT];
    private final boolean[]   minusHov  = new boolean[ROW_COUNT];
    private final boolean[]   plusHov   = new boolean[ROW_COUNT];

    // ── Animation ─────────────────────────────────────────────────────────────
    private int   tick      = 0;
    private float panelAlpha = 0f;

    // ── Fonts ─────────────────────────────────────────────────────────────────
    private final Font fontTitle;
    private final Font fontBtn;
    private final Font fontLabel;
    private final Font fontValue;

    // ─────────────────────────────────────────────────────────────────────────

    public SettingsScreen(ScreenManager screenManager, GameClient gameClient,
                          boolean isHost, Runnable onBack) {
        this.screenManager = screenManager;
        this.gameClient    = gameClient;
        this.isHost        = isHost;
        this.onBack        = onBack;
        this.settingsRef   = new AtomicReference<>(
            (gameClient != null) ? gameClient.getLastKnownSettings() : new GameSettings());

        for (int i = 0; i < ROW_COUNT; i++) {
            int rowY = FIRST_Y + i * ROW_H + (ROW_H - BTN_SIZE) / 2;
            minusBtns[i] = new Rectangle(VALUE_X,               rowY, BTN_SIZE, BTN_SIZE);
            plusBtns[i]  = new Rectangle(VALUE_X + BTN_SIZE + 90, rowY, BTN_SIZE, BTN_SIZE);
        }
        backBtn  = new Rectangle(PANEL_X + 20,              PANEL_Y + PANEL_H - 58, 130, 40);
        resetBtn = new Rectangle(PANEL_X + PANEL_W - 160,   PANEL_Y + PANEL_H - 58, 140, 40);

        fontTitle = ttf("/assets/fonts/PressStart2P-Regular.ttf", 13f);
        fontBtn   = ttf("/assets/fonts/PressStart2P-Regular.ttf",  8f);
        fontLabel = ttf("/assets/fonts/VT323-Regular.ttf",         19f);
        fontValue = ttf("/assets/fonts/VT323-Regular.ttf",         20f);
    }

    private Font ttf(String path, float sz) {
        try {
            InputStream is = getClass().getResourceAsStream(path);
            if (is==null) return new Font("Monospaced", Font.BOLD, (int)sz);
            return Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(sz);
        } catch (Exception e) { return new Font("Monospaced", Font.BOLD, (int)sz); }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    @Override public void onEnter() {
        tick=0; panelAlpha=0f;
        if (gameClient!=null) gameClient.setSettingsListener(s -> settingsRef.set(s));
        System.out.println("[SettingsScreen] Opened. isHost="+isHost);
    }

    @Override public void onExit() {
        if (gameClient!=null) gameClient.setSettingsListener(null);
        System.out.println("[SettingsScreen] Closed.");
    }

    @Override public void update() {
        tick++;
        panelAlpha = Math.min(1f, panelAlpha + 0.08f);
    }

    // ── Render ────────────────────────────────────────────────────────────────
    @Override public void render(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        drawDim(g);
        drawPanel(g);
        drawTitle(g);
        drawRows(g);
        drawFooterButtons(g);
        if (!isHost) drawReadOnlyOverlay(g);
    }

    // ── Dim ───────────────────────────────────────────────────────────────────
    private void drawDim(Graphics2D g) {
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, panelAlpha));
        g.setColor(new Color(0,0,0,185));
        g.fillRect(0,0,Constants.SCREEN_WIDTH,Constants.SCREEN_HEIGHT);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
    }

    // ── Panel body ────────────────────────────────────────────────────────────
    private void drawPanel(Graphics2D g) {
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, panelAlpha));

        // Outer glow (pulsing purple)
        float glow = 0.5f + 0.5f*(float)Math.abs(Math.sin(tick*0.04));
        g.setColor(new Color(0x7B,0x2F,0xBE,(int)(40*glow)));
        g.fillRoundRect(PANEL_X-8,PANEL_Y-8,PANEL_W+16,PANEL_H+16,18,18);

        // Body
        g.setColor(DARK_BG);
        g.fillRoundRect(PANEL_X,PANEL_Y,PANEL_W,PANEL_H,14,14);

        // Border — pulsing purple glow
        g.setColor(new Color(0x7B,0x2F,0xBE,(int)(160+95*glow)));
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(PANEL_X,PANEL_Y,PANEL_W,PANEL_H,14,14);
        g.setStroke(new BasicStroke(1f));

        // Blood red top accent bar
        g.setColor(new Color(0x8B,0x00,0x00,180));
        g.fillRoundRect(PANEL_X,PANEL_Y,PANEL_W,5,14,14);

        // Separator under title
        g.setColor(new Color(0x8B,0x00,0x00,100));
        g.setStroke(new BasicStroke(1.5f));
        g.drawLine(PANEL_X+20, PANEL_Y+82, PANEL_X+PANEL_W-20, PANEL_Y+82);
        g.setStroke(new BasicStroke(1f));

        // Corner detail squares
        g.setColor(new Color(0x7B,0x2F,0xBE,100));
        int cs=6;
        g.fillRect(PANEL_X+10,       PANEL_Y+10,       cs,cs);
        g.fillRect(PANEL_X+PANEL_W-10-cs,PANEL_Y+10,   cs,cs);
        g.fillRect(PANEL_X+10,       PANEL_Y+PANEL_H-10-cs,cs,cs);
        g.fillRect(PANEL_X+PANEL_W-10-cs,PANEL_Y+PANEL_H-10-cs,cs,cs);

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
    }

    // ── Title ─────────────────────────────────────────────────────────────────
    private void drawTitle(Graphics2D g) {
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, panelAlpha));
        String t = isHost ? "GAME SETTINGS  [HOST]" : "GAME SETTINGS  [VIEW ONLY]";
        g.setFont(fontTitle);
        FontMetrics fm=g.getFontMetrics();
        int tx=PANEL_X+PANEL_W/2-fm.stringWidth(t)/2;

        // Shadow
        g.setColor(BLOOD_RED);
        g.drawString(t, tx+2, PANEL_Y+55);
        // Main — amber for host, muted purple for view-only
        g.setColor(isHost ? AMBER : PURPLE_GLOW);
        g.drawString(t, tx, PANEL_Y+53);

        // ⚙ icon left of title
        g.setFont(fontLabel.deriveFont(22f));
        g.setColor(new Color(0x7B,0x2F,0xBE,160));
        g.drawString("⚙", PANEL_X+20, PANEL_Y+56);

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
    }

    // ── Rows ──────────────────────────────────────────────────────────────────
    private void drawRows(Graphics2D g) {
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, panelAlpha));
        for (int i=0;i<ROW_COUNT;i++) drawOneRow(g, i, FIRST_Y + i*ROW_H);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
    }

    private void drawOneRow(Graphics2D g, int idx, int rowY) {
        Color accent = ROW_ACCENTS[idx];

        // Alternating subtle row background
        if (idx%2==0) {
            g.setColor(new Color(0xFF,0xFF,0xFF,6));
            g.fillRect(PANEL_X+10, rowY, PANEL_W-20, ROW_H-2);
        }

        // Left accent pip
        g.setColor(new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),120));
        g.fillRect(PANEL_X+10, rowY+10, 3, ROW_H-22);

        // Label
        g.setFont(fontLabel.deriveFont(18f));
        g.setColor(new Color(0xAA,0xAA,0xCC));
        g.drawString(ROW_LABELS[idx], LABEL_X+8, rowY+28);

        // Value — accent colored
        String val = formatValue(idx);
        g.setFont(fontValue.deriveFont(20f));
        g.setColor(accent);
        FontMetrics fm=g.getFontMetrics();
        int valCenterX = VALUE_X + BTN_SIZE + 45;
        g.drawString(val, valCenterX - fm.stringWidth(val)/2, rowY+28);

        // ± buttons
        if (isHost) {
            drawAdjustBtn(g, minusBtns[idx], "−", minusHov[idx], accent);
            drawAdjustBtn(g, plusBtns[idx],  "+", plusHov[idx],  accent);
        }
    }

    private void drawAdjustBtn(Graphics2D g, Rectangle btn, String label,
                                boolean hovered, Color accent) {
        // Body
        g.setColor(hovered
            ? new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),180)
            : DARK_BG);
        g.fillRoundRect(btn.x,btn.y,btn.width,btn.height,6,6);

        // Border
        g.setColor(new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),hovered?255:120));
        g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect(btn.x,btn.y,btn.width,btn.height,6,6);
        g.setStroke(new BasicStroke(1f));

        // Label
        g.setFont(fontValue.deriveFont(20f));
        g.setColor(hovered ? DARK_BG : GHOSTLY_WHITE);
        FontMetrics fm=g.getFontMetrics();
        g.drawString(label,
            btn.x+btn.width/2-fm.stringWidth(label)/2,
            btn.y+btn.height/2+fm.getAscent()/2-3);
    }

    // ── Footer buttons ────────────────────────────────────────────────────────
    private void drawFooterButtons(Graphics2D g) {
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, panelAlpha));

        // BACK button — blood red
        drawHorrorBtn(g, backBtn, "<- BACK", backHovered, BRIGHT_RED);

        // RESET button — purple (host only)
        if (isHost) drawHorrorBtn(g, resetBtn, "RESET DEFAULTS", resetHovered, PURPLE_GLOW);

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
    }

    private void drawHorrorBtn(Graphics2D g, Rectangle r, String label,
                                boolean hovered, Color accent) {
        int x=r.x,y=r.y,w=r.width,h=r.height;
        if (hovered) {
            g.setColor(new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),70));
            g.fillRoundRect(x-4,y-4,w+8,h+8,10,10);
        }
        g.setColor(DARK_BG);
        g.fillRoundRect(x,y,w,h,8,8);
        g.setColor(new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),hovered?255:130));
        g.setStroke(new BasicStroke(hovered?2f:1.5f));
        g.drawRoundRect(x,y,w,h,8,8);
        g.setStroke(new BasicStroke(1f));
        // Left bar
        g.setColor(accent);
        g.fillRect(x+1,y+6,3,h-12);

        g.setFont(fontBtn.deriveFont(8f));
        FontMetrics fm=g.getFontMetrics();
        int tx=x+w/2-fm.stringWidth(label)/2;
        int ty=y+h/2+fm.getAscent()/2-2;
        g.setColor(new Color(0,0,0,130)); g.drawString(label,tx+2,ty+2);
        g.setColor(hovered ? GHOSTLY_WHITE : new Color(
            (accent.getRed()+GHOSTLY_WHITE.getRed())/2,
            (accent.getGreen()+GHOSTLY_WHITE.getGreen())/2,
            (accent.getBlue()+GHOSTLY_WHITE.getBlue())/2));
        g.drawString(label,tx,ty);
    }

    // ── Read-only overlay ─────────────────────────────────────────────────────
    private void drawReadOnlyOverlay(Graphics2D g) {
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, panelAlpha));
        g.setFont(fontLabel.deriveFont(17f));
        g.setColor(BRIGHT_RED);
        String msg = "Only the host can change settings.";
        FontMetrics fm=g.getFontMetrics();
        g.drawString(msg,
            PANEL_X+PANEL_W/2-fm.stringWidth(msg)/2,
            PANEL_Y+PANEL_H-14);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
    }

    // ── Value formatting (unchanged) ──────────────────────────────────────────
    private String formatValue(int row) {
        GameSettings s = settingsRef.get();
        switch (row) {
            case 0: return String.valueOf(s.getPlayerBaseHp());
            case 1: return String.format("%.1f", s.getPlayerBaseSpeed());
            case 2: return String.valueOf(s.getPlayerBaseDamage());
            case 3: return s.getPlayerShootCooldown()+" tks";
            case 4: return s.getPlayerHitCooldown()+" tks";
            case 5: return String.format("%.1f", s.getPlayerMaxSpeed());
            case 6: return String.format("%.0f%%", s.getPowerUpDropChance()*100f);
            case 7: return s.getTotalRounds()+" rounds";
            case 8: return s.getEnemySpawnCooldown()+" tks";
            case 9: return String.format("%.1f", s.getMeleeEnemySpeed());
            default: return "?";
        }
    }

    // ── Adjustment logic (unchanged) ──────────────────────────────────────────
    private void increment(int row) {
        GameSettings s = settingsRef.get();
        switch (row) {
            case 0: s.setPlayerBaseHp(s.getPlayerBaseHp()+10); break;
            case 1: s.setPlayerBaseSpeed(Math.round((s.getPlayerBaseSpeed()+0.5f)*10f)/10f); break;
            case 2: s.setPlayerBaseDamage(s.getPlayerBaseDamage()+1); break;
            case 3: s.setPlayerShootCooldown(s.getPlayerShootCooldown()+1); break;
            case 4: s.setPlayerHitCooldown(s.getPlayerHitCooldown()+5); break;
            case 5: s.setPlayerMaxSpeed(Math.round((s.getPlayerMaxSpeed()+0.5f)*10f)/10f); break;
            case 6: s.setPowerUpDropChance(Math.min(1f,s.getPowerUpDropChance()+0.05f)); break;
            case 7: s.setTotalRounds(s.getTotalRounds()+1); break;
            case 8: s.setEnemySpawnCooldown(s.getEnemySpawnCooldown()+5); break;
            case 9: s.setMeleeEnemySpeed(Math.round((s.getMeleeEnemySpeed()+0.1f)*10f)/10f); break;
        }
        pushSettings(s);
    }

    private void decrement(int row) {
        GameSettings s = settingsRef.get();
        switch (row) {
            case 0: s.setPlayerBaseHp(Math.max(10,s.getPlayerBaseHp()-10)); break;
            case 1: s.setPlayerBaseSpeed(Math.max(0.5f,Math.round((s.getPlayerBaseSpeed()-0.5f)*10f)/10f)); break;
            case 2: s.setPlayerBaseDamage(Math.max(1,s.getPlayerBaseDamage()-1)); break;
            case 3: s.setPlayerShootCooldown(Math.max(1,s.getPlayerShootCooldown()-1)); break;
            case 4: s.setPlayerHitCooldown(Math.max(0,s.getPlayerHitCooldown()-5)); break;
            case 5: s.setPlayerMaxSpeed(Math.max(s.getPlayerBaseSpeed(),Math.round((s.getPlayerMaxSpeed()-0.5f)*10f)/10f)); break;
            case 6: s.setPowerUpDropChance(Math.max(0f,s.getPowerUpDropChance()-0.05f)); break;
            case 7: s.setTotalRounds(Math.max(1,s.getTotalRounds()-1)); break;
            case 8: s.setEnemySpawnCooldown(Math.max(5,s.getEnemySpawnCooldown()-5)); break;
            case 9: s.setMeleeEnemySpeed(Math.max(0.1f,Math.round((s.getMeleeEnemySpeed()-0.1f)*10f)/10f)); break;
        }
        pushSettings(s);
    }

    private void pushSettings(GameSettings s) {
        if (gameClient!=null && isHost) gameClient.sendSettings(s);
    }

    // ── Input (unchanged) ─────────────────────────────────────────────────────
    @Override public void handleMouseClicked(int x, int y) {
        if (backBtn.contains(x,y)) { if (onBack!=null) onBack.run(); return; }
        if (!isHost) return;
        if (resetBtn.contains(x,y)) { GameSettings s=settingsRef.get(); s.reset(); pushSettings(s); return; }
        for (int i=0;i<ROW_COUNT;i++) {
            if (minusBtns[i].contains(x,y)) { decrement(i); return; }
            if (plusBtns[i].contains(x,y))  { increment(i); return; }
        }
    }

    @Override public void handleMouseMoved(int x, int y) {
        backHovered  = backBtn.contains(x,y);
        resetHovered = resetBtn.contains(x,y);
        for (int i=0;i<ROW_COUNT;i++) {
            minusHov[i] = minusBtns[i].contains(x,y);
            plusHov[i]  = plusBtns[i].contains(x,y);
        }
    }

    @Override public void handleKeyPressed(int keyCode) {
        if (keyCode==KeyEvent.VK_ESCAPE && onBack!=null) onBack.run();
    }
}
