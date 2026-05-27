package com.shooter.ui;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.shooter.shared.model.GameState;
import com.shooter.shared.model.Player;
import com.shooter.shared.util.Constants;

/**
 * ============================================================
 * FILE: HUD.java
 * PACKAGE: ui
 * OWNER: Member A / UI REDESIGN: Christel
 * ============================================================
 * Dark horror pixel art HUD overlay.
 * All public API and logic preserved exactly.
 * Only rendering methods overhauled.
 * ============================================================
 */
public class HUD {

    // ── Notification model ────────────────────────────────────────────────────
    private static class Notification {
        String message; int ticksLeft;
        Notification(String m, int d) { message=m; ticksLeft=d; }
    }

    private static final int NOTIFICATION_DURATION = 180;
    private static final int MAX_NOTIFICATIONS      = 4;
    private static final int ROUND_BANNER_DURATION  = 120;

    private final List<Notification> notifications = new ArrayList<>();
    private int   roundBannerTicks = 0;
    private int   roundBannerRound = 1;

    // ── Horror palette ────────────────────────────────────────────────────────
    private static final Color VOID_PURPLE   = new Color(0x1A, 0x0A, 0x2E);
    private static final Color TOXIC_GREEN   = new Color(0x39, 0xFF, 0x14);
    private static final Color BLOOD_RED     = new Color(0x8B, 0x00, 0x00);
    private static final Color BRIGHT_RED    = new Color(0xCC, 0x00, 0x00);
    private static final Color GHOSTLY_WHITE = new Color(0xE8, 0xE8, 0xF0);
    private static final Color DARK_BG       = new Color(0x0D, 0x0D, 0x1A);
    private static final Color PURPLE_GLOW   = new Color(0x7B, 0x2F, 0xBE);
    private static final Color GREEN_GLOW    = new Color(0x00, 0xFF, 0x88);

    private static final Color[] PLAYER_COLORS = {
        new Color(0x4A, 0x90, 0xD9),
        new Color(0xCC, 0x00, 0x00),
        new Color(0x39, 0xFF, 0x14),
        new Color(0xF5, 0xA6, 0x23)
    };
    private static final Color[] PU_COLORS = {
        BRIGHT_RED,              // DAMAGE
        new Color(0x39,0xFF,0x14), // HP
        new Color(0x4A,0x90,0xD9), // MOVEMENT
        new Color(0xF5,0xA6,0x23)  // ATTACK_SPEED
    };

    // ── Layout ────────────────────────────────────────────────────────────────
    private static final int MARGIN      = 10;
    private static final int PAD         = 6;
    private static final int LINE_H      = 16;
    private static final int HP_BAR_W    = 130;
    private static final int HP_BAR_H    = 10;

    // ── Fonts ─────────────────────────────────────────────────────────────────
    private final Font fontPixelMd;   // Press Start 2P medium
    private final Font fontPixelSm;   // Press Start 2P small
    private final Font fontVTZ;       // VT323 body

    public HUD() {
        fontPixelMd = loadTtf("/assets/fonts/PressStart2P-Regular.ttf", 10f);
        fontPixelSm = loadTtf("/assets/fonts/PressStart2P-Regular.ttf",  8f);
        fontVTZ     = loadTtf("/assets/fonts/VT323-Regular.ttf",        18f);
    }

    private Font loadTtf(String path, float size) {
        try {
            InputStream is = getClass().getResourceAsStream(path);
            if (is == null) return new Font("Monospaced", Font.BOLD, (int)size);
            return Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(size);
        } catch (Exception e) { return new Font("Monospaced", Font.BOLD, (int)size); }
    }

    // ── Public API (unchanged signatures) ────────────────────────────────────
    public void notifyPowerUp(String playerName, String powerUpType) {
        String msg = iconFor(powerUpType) + " " + playerName + " picked up " + friendlyName(powerUpType) + "!";
        if (notifications.size() >= MAX_NOTIFICATIONS) notifications.remove(0);
        notifications.add(new Notification(msg, NOTIFICATION_DURATION));
    }

    public void notifyPowerUpCapped(String playerName, String powerUpType) {
        String msg = iconFor(powerUpType) + " " + playerName + " cannot apply " + friendlyName(powerUpType) + " (At Cap)!";
        if (notifications.size() >= MAX_NOTIFICATIONS) notifications.remove(0);
        notifications.add(new Notification(msg, NOTIFICATION_DURATION));
    }

    public void showRoundBanner(int round) {
        roundBannerRound = round;
        roundBannerTicks = ROUND_BANNER_DURATION;
    }

    public void tick() {
        notifications.removeIf(n -> --n.ticksLeft <= 0);
        if (roundBannerTicks > 0) roundBannerTicks--;
    }

    public void render(Graphics2D g, GameState state, int killedEnemies,
                       int totalEnemiesThisRound, int playerSpawnCooldown) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        drawTopLeftPanel(g, state, killedEnemies, totalEnemiesThisRound, playerSpawnCooldown);
        drawStatsPanel(g, state);
        drawNotifications(g);
        drawRoundBanner(g);
    }

    // ── Top-left panel (round + enemy bar + HP bars) ──────────────────────────
    private void drawTopLeftPanel(Graphics2D g, GameState state,
                                  int killed, int total, int spawnCD) {
        List<Player> players = state.getPlayers();
        int panelH = 52 + players.size() * 32;
        int x = MARGIN, y = MARGIN;

        // Panel background
        drawDarkPanel(g, x - PAD, y - PAD, 256, panelH, 200);

        // ── Round counter ──
        g.setFont(fontPixelSm);
        // Shadow
        g.setColor(BLOOD_RED);
        g.drawString("ROUND " + state.getCurrentRound() + " / " + Constants.TOTAL_ROUNDS, x+2, y+LINE_H+2);
        // Text
        g.setColor(new Color(0xF5,0xA6,0x23));
        g.drawString("ROUND " + state.getCurrentRound() + " / " + Constants.TOTAL_ROUNDS, x, y+LINE_H);

        // ── Enemy kill bar ──
        int barY = y + LINE_H + 10;
        g.setFont(fontVTZ != null ? fontVTZ.deriveFont(16f) : new Font("Monospaced",Font.PLAIN,12));
        g.setColor(new Color(0xAA,0xAA,0xCC));
        g.drawString("ENEMIES: " + killed + " / " + total, x, barY + 12);

        // Mini enemy progress bar
        if (total > 0) {
            float pct = Math.min(1f, (float)killed/total);
            int pbx = x, pby = barY + 15, pbw = 220, pbh = 5;
            g.setColor(new Color(0x1A,0x0A,0x2E));
            g.fillRoundRect(pbx, pby, pbw, pbh, 3, 3);
            // Fill: purple → green when full
            Color fillC = pct >= 1f ? TOXIC_GREEN : PURPLE_GLOW;
            g.setColor(fillC);
            g.fillRoundRect(pbx, pby, (int)(pbw*pct), pbh, 3, 3);
            // Glow outline
            if (pct >= 1f) {
                g.setColor(new Color(0x39,0xFF,0x14,80));
                g.setStroke(new BasicStroke(2f));
                g.drawRoundRect(pbx,pby,pbw,pbh,3,3);
                g.setStroke(new BasicStroke(1f));
            }
        }

        // ── Per-player HP bars ──
        int hpY = barY + 30;
        for (Player p : players) {
            int pid = p.getPlayerId();
            Color pc = (pid>=0 && pid<PLAYER_COLORS.length) ? PLAYER_COLORS[pid] : GHOSTLY_WHITE;

            // P# badge
            g.setFont(fontPixelSm);
            g.setColor(pc);
            g.drawString("P"+(pid+1), x, hpY+9);

            if (p.isAlive()) {
                drawHpBar(g, x+28, hpY, p.getHp(), p.getMaxHp(), pc);
                drawPowerUpDots(g, x+28, hpY+HP_BAR_H+2, p);
            } else {
                g.setFont(fontVTZ != null ? fontVTZ.deriveFont(16f) : new Font("Monospaced",Font.PLAIN,12));
                if (p.getPlayerId() == state.getLocalPlayerId()) {
                    int sec = (int)Math.ceil((double)spawnCD / Constants.TARGET_FPS);
                    g.setColor(BRIGHT_RED);
                    g.drawString("RESPAWN IN "+sec+"s...", x+28, hpY+10);
                } else {
                    g.setColor(new Color(0x66,0x22,0x22));
                    g.drawString("DEAD", x+28, hpY+10);
                }
            }
            hpY += 30;
        }
    }

    // ── HP bar ────────────────────────────────────────────────────────────────
    private void drawHpBar(Graphics2D g, int x, int y, int hp, int maxHp, Color playerColor) {
        float pct = maxHp>0 ? Math.max(0f, Math.min(1f,(float)hp/maxHp)) : 0f;

        // Background track
        g.setColor(new Color(0x0D,0x05,0x1E));
        g.fillRoundRect(x, y, HP_BAR_W, HP_BAR_H, 4, 4);
        // Inner shadow
        g.setColor(new Color(0,0,0,100));
        g.fillRoundRect(x+1, y+1, HP_BAR_W-2, HP_BAR_H-2, 3, 3);

        // HP fill — color transitions: green→yellow→blood red
        Color hpColor;
        if (pct > 0.5f)      hpColor = new Color(0x39,0xFF,0x14); // toxic green
        else if (pct > 0.25f) hpColor = new Color(0xF5,0xA6,0x23); // amber
        else                  hpColor = BRIGHT_RED;                  // blood red

        int fillW = (int)(HP_BAR_W * pct);
        if (fillW > 0) {
            g.setColor(hpColor);
            g.fillRoundRect(x, y, fillW, HP_BAR_H, 4, 4);
            // Shine highlight on top of fill
            g.setColor(new Color(255,255,255,30));
            g.fillRoundRect(x, y, fillW, HP_BAR_H/2, 4, 4);
        }

        // Border — player color accent
        g.setColor(new Color(playerColor.getRed(),playerColor.getGreen(),playerColor.getBlue(),120));
        g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(x, y, HP_BAR_W, HP_BAR_H, 4, 4);
        g.setStroke(new BasicStroke(1f));

        // HP text
        g.setFont(fontVTZ != null ? fontVTZ.deriveFont(15f) : new Font("Monospaced",Font.PLAIN,10));
        g.setColor(pct > 0.25f ? new Color(0xDD,0xDD,0xEE) : BRIGHT_RED);
        g.drawString(hp+"/"+maxHp, x+HP_BAR_W+6, y+HP_BAR_H+1);
    }

    // ── Power-up dots ─────────────────────────────────────────────────────────
    private void drawPowerUpDots(Graphics2D g, int x, int y, Player p) {
        List<String> pups = p.getActivePowerUps();
        if (pups==null||pups.isEmpty()) return;
        int dot=6, gap=3, cx=x;
        for (String type : pups) {
            Color c = colorForPowerUp(type);
            // Glow
            g.setColor(new Color(c.getRed(),c.getGreen(),c.getBlue(),60));
            g.fillOval(cx-1,y-1,dot+2,dot+2);
            // Fill
            g.setColor(c);
            g.fillOval(cx,y,dot,dot);
            cx += dot+gap;
            if (cx > x+HP_BAR_W) break;
        }
    }

    // ── Right stats panel ─────────────────────────────────────────────────────
    private void drawStatsPanel(Graphics2D g, GameState state) {
        List<Player> players = state.getPlayers();
        if (players.isEmpty()) return;

        int panelW  = 200;
        int rowsPerP = 6;
        int panelH  = (rowsPerP * LINE_H + PAD*2) * players.size() + PAD;
        int x = Constants.SCREEN_WIDTH - panelW - MARGIN;
        int y = MARGIN;

        drawDarkPanel(g, x-PAD, y-PAD, panelW+PAD*2, panelH, 200);

        int cursor = y + PAD;
        for (Player p : players) {
            int pid = p.getPlayerId();
            Color pc = (pid>=0&&pid<PLAYER_COLORS.length) ? PLAYER_COLORS[pid] : GHOSTLY_WHITE;

            // Player header
            g.setFont(fontPixelSm);
            // Left accent bar
            g.setColor(pc);
            g.fillRect(x-PAD, cursor, 3, LINE_H*rowsPerP+PAD);
            // Name shadow + text
            boolean isMain = (p == state.getMainPlayer());
            g.setColor(isMain ? BRIGHT_RED : new Color(pc.getRed(),pc.getGreen(),pc.getBlue(),160));
            g.drawString((isMain?"▶ ":"  ") + p.getName(), x+2, cursor+LINE_H+1);
            g.setColor(isMain ? GHOSTLY_WHITE : pc);
            g.drawString((isMain?"▶ ":"  ") + p.getName(), x, cursor+LINE_H);
            cursor += LINE_H + 2;

            // Stat rows
            g.setFont(fontVTZ != null ? fontVTZ.deriveFont(16f) : new Font("Monospaced",Font.PLAIN,11));
            drawStatRow(g, x, cursor, "HP",    p.getHp()+" / "+p.getMaxHp()); cursor+=LINE_H;
            drawStatRow(g, x, cursor, "SPD",   String.format("%.1f",p.getSpeed())); cursor+=LINE_H;
            drawStatRow(g, x, cursor, "DMG",   String.valueOf(p.getDamage())); cursor+=LINE_H;
            drawStatRow(g, x, cursor, "ASPD",  p.getShootCooldown()+"t"); cursor+=LINE_H;

            List<String> pups = p.getActivePowerUps();
            String pu = buildPuSummary(countOf(pups,"DAMAGE"),countOf(pups,"HP"),
                                       countOf(pups,"MOVEMENT"),countOf(pups,"ATTACK_SPEED"));
            drawStatRow(g, x, cursor, "BUFF",  pu);
            cursor += LINE_H + PAD;
        }
    }

    private void drawStatRow(Graphics2D g, int x, int y, String label, String value) {
        g.setColor(PURPLE_GLOW);
        g.drawString(label+":", x+4, y);
        g.setColor(GHOSTLY_WHITE);
        g.drawString(value, x+60, y);
    }

    // ── Round banner ──────────────────────────────────────────────────────────
    private void drawRoundBanner(Graphics2D g) {
        if (roundBannerTicks <= 0) return;
        float alpha = Math.min(1f, (float)roundBannerTicks/40f);
        int W=Constants.SCREEN_WIDTH, H=Constants.SCREEN_HEIGHT;
        int cx=W/2, cy=H/2-50;

        // Dark backdrop with purple border
        g.setColor(new Color(0x0D,0x05,0x1E,(int)(220*alpha)));
        g.fillRoundRect(cx-200, cy-50, 400, 110, 16, 16);
        g.setColor(new Color(0x8B,0x00,0x00,(int)(200*alpha)));
        g.setStroke(new BasicStroke(2.5f));
        g.drawRoundRect(cx-200,cy-50,400,110,16,16);
        g.setStroke(new BasicStroke(1f));

        // Blood red top accent line
        g.setColor(new Color(0xCC,0x00,0x00,(int)(180*alpha)));
        g.fillRect(cx-200, cy-50, 400, 5);

        // "ROUND X" — shadow then main
        Font bannerFont = fontPixelMd != null ? fontPixelMd.deriveFont(26f) : new Font("Monospaced",Font.BOLD,26);
        g.setFont(bannerFont);
        String text = "ROUND " + roundBannerRound;
        FontMetrics fm = g.getFontMetrics();
        int tx = cx - fm.stringWidth(text)/2;

        g.setColor(new Color(0x8B,0x00,0x00,(int)(255*alpha)));
        g.drawString(text, tx+3, cy+12+3);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g.setColor(GHOSTLY_WHITE);
        g.drawString(text, tx, cy+12);

        // Subtitle
        Font subFont = fontVTZ != null ? fontVTZ.deriveFont(20f) : new Font("Monospaced",Font.PLAIN,14);
        g.setFont(subFont);
        g.setColor(new Color(0xAA,0xAA,0xCC,(int)(200*alpha)));
        String sub = "Survive the wave!";
        fm = g.getFontMetrics();
        g.drawString(sub, cx-fm.stringWidth(sub)/2, cy+40);

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
    }

    // ── Notifications ─────────────────────────────────────────────────────────
    private void drawNotifications(Graphics2D g) {
        if (notifications.isEmpty()) return;
        int cx = Constants.SCREEN_WIDTH/2;
        int baseY = Constants.SCREEN_HEIGHT - 60;
        int nW=380, nH=24;

        for (int i=0; i<notifications.size(); i++) {
            Notification n = notifications.get(i);
            float alpha = Math.min(1f, (float)n.ticksLeft/60f);
            int y = baseY - i*(nH+5);

            // Background — dark void panel
            g.setColor(new Color(0x0D,0x05,0x1E,(int)(210*alpha)));
            g.fillRoundRect(cx-nW/2, y, nW, nH, 6, 6);

            // Border — toxic green for power-up pickups
            g.setColor(new Color(0x39,0xFF,0x14,(int)(160*alpha)));
            g.setStroke(new BasicStroke(1f));
            g.drawRoundRect(cx-nW/2, y, nW, nH, 6, 6);

            // Left accent bar
            g.setColor(new Color(0x39,0xFF,0x14,(int)(200*alpha)));
            g.fillRect(cx-nW/2, y+3, 3, nH-6);

            // Text
            Font nFont = fontVTZ != null ? fontVTZ.deriveFont(17f) : new Font("Monospaced",Font.BOLD,12);
            g.setFont(nFont);
            g.setColor(new Color(0xF5,0xDC,0x50,(int)(255*alpha)));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(n.message, cx-fm.stringWidth(n.message)/2, y+nH-5);
        }
        g.setStroke(new BasicStroke(1f));
    }

    // ── Shared helper ─────────────────────────────────────────────────────────
    private void drawDarkPanel(Graphics2D g, int x, int y, int w, int h, int alpha) {
        g.setColor(new Color(0x0D,0x05,0x1E,alpha));
        g.fillRoundRect(x, y, w, h, 8, 8);
        g.setColor(new Color(0x7B,0x2F,0xBE,80));
        g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(x, y, w, h, 8, 8);
    }

    // ── Utilities (unchanged) ─────────────────────────────────────────────────
    private Color colorForPowerUp(String type) {
        switch (type) {
            case "DAMAGE":       return PU_COLORS[0];
            case "HP":           return PU_COLORS[1];
            case "MOVEMENT":     return PU_COLORS[2];
            case "ATTACK_SPEED": return PU_COLORS[3];
            default:             return GHOSTLY_WHITE;
        }
    }

    private int countOf(List<String> list, String type) {
        int c=0; for (String s:list) if(s.equals(type)) c++; return c;
    }

    private String buildPuSummary(int dmg,int hp,int spd,int atk) {
        StringBuilder sb = new StringBuilder();
        if (dmg>0) sb.append("DMG×").append(dmg).append(" ");
        if (hp>0)  sb.append("HP×").append(hp).append(" ");
        if (spd>0) sb.append("SPD×").append(spd).append(" ");
        if (atk>0) sb.append("ATK×").append(atk).append(" ");
        return sb.length()>0 ? sb.toString().trim() : "none";
    }

    private String friendlyName(String type) {
        switch (type) {
            case "DAMAGE":       return "Damage Boost";
            case "HP":           return "Health Pack";
            case "ATTACK_SPEED": return "Attack Speed";
            case "MOVEMENT":     return "Speed Boost";
            default:             return type;
        }
    }

    private String iconFor(String type) {
        switch (type) {
            case "DAMAGE":       return "[ATK]";
            case "HP":           return "[HP+]";
            case "ATTACK_SPEED": return "[SPD]";
            case "MOVEMENT":     return "[MOV]";
            default:             return "[???]";
        }
    }
}