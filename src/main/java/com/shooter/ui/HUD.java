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
 * Fantasy RPG pixel art HUD overlay.
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

    // ── Fantasy RPG palette ───────────────────────────────────────────────────
    private static final Color WOOD_BASE     = new Color(0x4A, 0x2E, 0x1B);
    private static final Color WOOD_DARK     = new Color(0x2A, 0x16, 0x0C);
    private static final Color WOOD_LIGHT    = new Color(0x6D, 0x44, 0x2A);
    private static final Color GOLD_ACCENT   = new Color(0xD4, 0xA8, 0x53);
    private static final Color GOLD_DARK     = new Color(0x8C, 0x62, 0x39);
    private static final Color PARCHMENT     = new Color(0xE1, 0xC6, 0x99);
    private static final Color BLOOD_RED     = new Color(0xB3, 0x20, 0x26);
    private static final Color MANA_BLUE     = new Color(0x2D, 0x6A, 0xB4);
    private static final Color STAMINA_GREEN = new Color(0x4C, 0xAF, 0x50);
    private static final Color GHOSTLY_WHITE = new Color(0xE8, 0xE8, 0xF0);

    private static final Color[] PLAYER_COLORS = {
        new Color(0x4A, 0x90, 0xD9),
        new Color(0xCC, 0x00, 0x00),
        new Color(0x39, 0xFF, 0x14),
        new Color(0xF5, 0xA6, 0x23)
    };
    private static final Color[] PU_COLORS = {
        BLOOD_RED,               // DAMAGE
        STAMINA_GREEN,           // HP
        MANA_BLUE,               // MOVEMENT
        GOLD_ACCENT              // ATTACK_SPEED
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
        
        Player mainP = state.getLocalPlayer() != null ? state.getLocalPlayer() : state.getMainPlayer();
        drawBottomHUD(g, mainP, playerSpawnCooldown);
        
        drawNotifications(g);
        drawRoundBanner(g);
    }

    // ── Top-left panel (round + enemy bar) ──────────────────────────
    private void drawTopLeftPanel(Graphics2D g, GameState state,
                                  int killed, int total, int spawnCD) {
        int panelH = 54;
        int x = MARGIN, y = MARGIN;

        // Panel background
        drawFantasyPanel(g, x - PAD, y - PAD, 256, panelH, 255);

        // ── Round counter ──
        g.setFont(fontPixelSm);
        // Shadow
        g.setColor(WOOD_DARK);
        g.drawString("ROUND " + state.getCurrentRound() + " / " + Constants.TOTAL_ROUNDS, x+2, y+LINE_H+2);
        // Text
        g.setColor(GOLD_ACCENT);
        g.drawString("ROUND " + state.getCurrentRound() + " / " + Constants.TOTAL_ROUNDS, x, y+LINE_H);

        // ── Enemy kill bar ──
        int barY = y + LINE_H + 10;
        g.setFont(fontVTZ != null ? fontVTZ.deriveFont(16f) : new Font("Monospaced",Font.PLAIN,12));
        g.setColor(PARCHMENT);
        g.drawString("ENEMIES: " + killed + " / " + total, x, barY + 12);

        // Mini enemy progress bar
        if (total > 0) {
            float pct = Math.min(1f, (float)killed/total);
            int pbx = x, pby = barY + 15, pbw = 220, pbh = 6;
            g.setColor(WOOD_DARK);
            g.fillRoundRect(pbx, pby, pbw, pbh, 2, 2);
            // Fill: blue → green when full
            Color fillC = pct >= 1f ? STAMINA_GREEN : MANA_BLUE;
            g.setColor(fillC);
            g.fillRoundRect(pbx, pby, (int)(pbw*pct), pbh, 2, 2);
            // Border
            g.setColor(WOOD_LIGHT);
            g.setStroke(new BasicStroke(1f));
            g.drawRoundRect(pbx, pby, pbw, pbh, 2, 2);
        }
    }

    // ── Main Player Bottom HUD ────────────────────────────────────────────────
    private void drawBottomHUD(Graphics2D g, Player p, int spawnCD) {
        if (p == null) return;
        
        int w = 400;
        int h = 90;
        int x = Constants.SCREEN_WIDTH / 2 - w / 2;
        int y = Constants.SCREEN_HEIGHT - h - MARGIN;

        drawFantasyPanel(g, x, y, w, h, 255);

        int pid = p.getPlayerId();
        Color pc = (pid>=0 && pid<PLAYER_COLORS.length) ? PLAYER_COLORS[pid] : GHOSTLY_WHITE;

        // Player Name & Badge
        g.setFont(fontPixelMd);
        g.setColor(WOOD_DARK);
        g.drawString("P" + (pid+1) + " " + p.getName(), x + PAD + 2, y + PAD + 14 + 1);
        g.setColor(pc);
        g.drawString("P" + (pid+1) + " ", x + PAD, y + PAD + 14);
        g.setColor(GOLD_ACCENT);
        g.drawString(p.getName(), x + PAD + g.getFontMetrics().stringWidth("P" + (pid+1) + " "), y + PAD + 14);

        // HP Bar
        int hpY = y + PAD + 24;
        if (p.isAlive()) {
            drawHpBar(g, x + PAD, hpY, p.getHp(), p.getMaxHp(), pc, w - PAD*2 - 45, 16);
            
            // Stats Row
            int statY = hpY + 16 + 18;
            g.setFont(fontVTZ != null ? fontVTZ.deriveFont(18f) : new Font("Monospaced",Font.PLAIN,14));
            String statsStr = String.format("SPD:%.1f   DMG:%d   ASPD:%d", p.getSpeed(), p.getDamage(), p.getShootCooldown());
            g.setColor(WOOD_DARK);
            g.drawString(statsStr, x + PAD + 1, statY + 1);
            g.setColor(PARCHMENT);
            g.drawString(statsStr, x + PAD, statY);
            
            // Buffs
            List<String> pups = p.getActivePowerUps();
            String pu = buildPuSummary(countOf(pups,"DAMAGE"),countOf(pups,"HP"),
                                       countOf(pups,"MOVEMENT"),countOf(pups,"ATTACK_SPEED"));
            g.setColor(WOOD_DARK);
            g.drawString("BUFFS: " + pu, x + PAD + 1, statY + 16 + 1);
            g.setColor(GHOSTLY_WHITE);
            g.drawString("BUFFS: " + pu, x + PAD, statY + 16);

        } else {
            g.setFont(fontVTZ != null ? fontVTZ.deriveFont(20f) : new Font("Monospaced",Font.PLAIN,16));
            int sec = (int)Math.ceil((double)spawnCD / Constants.TARGET_FPS);
            g.setColor(BLOOD_RED);
            g.drawString("DEAD - RESPAWN IN "+sec+"s...", x + PAD, hpY + 12);
        }
    }

    // ── HP bar ────────────────────────────────────────────────────────────────
    private void drawHpBar(Graphics2D g, int x, int y, int hp, int maxHp, Color playerColor, int width, int height) {
        float pct = maxHp>0 ? Math.max(0f, Math.min(1f,(float)hp/maxHp)) : 0f;

        // Background track
        g.setColor(WOOD_DARK);
        g.fillRoundRect(x, y, width, height, 4, 4);

        // HP fill
        Color hpColor;
        if (pct > 0.5f)      hpColor = STAMINA_GREEN;
        else if (pct > 0.25f) hpColor = GOLD_ACCENT;
        else                  hpColor = BLOOD_RED;

        int fillW = (int)(width * pct);
        if (fillW > 0) {
            g.setColor(hpColor);
            g.fillRoundRect(x, y, fillW, height, 4, 4);
            // Shine highlight on top of fill
            g.setColor(new Color(255,255,255,40));
            g.fillRoundRect(x, y, fillW, height/2, 4, 4);
        }

        // Border — Ornate Gold border
        g.setColor(GOLD_DARK);
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(x, y, width, height, 4, 4);
        g.setColor(GOLD_ACCENT);
        g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(x-1, y-1, width+2, height+2, 4, 4);

        // HP text
        g.setFont(fontVTZ != null ? fontVTZ.deriveFont(15f) : new Font("Monospaced",Font.PLAIN,10));
        g.setColor(WOOD_DARK);
        g.drawString(hp+"/"+maxHp, x+width+7, y+height-1); // Shadow
        g.setColor(pct > 0.25f ? PARCHMENT : BLOOD_RED);
        g.drawString(hp+"/"+maxHp, x+width+6, y+height-2);
    }

    // ── Right stats panel ─────────────────────────────────────────────────────
    private void drawStatsPanel(Graphics2D g, GameState state) {
        Player mainP = state.getLocalPlayer() != null ? state.getLocalPlayer() : state.getMainPlayer();
        List<Player> otherPlayers = new ArrayList<>();
        for (Player p : state.getPlayers()) {
            if (p != mainP) {
                otherPlayers.add(p);
            }
        }
        if (otherPlayers.isEmpty()) return;

        int panelW  = 200;
        int rowsPerP = 6;
        int panelH  = (rowsPerP * LINE_H + PAD*2) * otherPlayers.size() + PAD;
        int x = Constants.SCREEN_WIDTH - panelW - MARGIN;
        int y = MARGIN;

        drawFantasyPanel(g, x-PAD, y-PAD, panelW+PAD*2, panelH, 255);

        int cursor = y + PAD;
        for (Player p : otherPlayers) {
            int pid = p.getPlayerId();
            Color pc = (pid>=0&&pid<PLAYER_COLORS.length) ? PLAYER_COLORS[pid] : GHOSTLY_WHITE;

            // Player header
            g.setFont(fontPixelSm);
            // Left accent bar
            g.setColor(pc);
            g.fillRect(x-PAD+4, cursor, 3, LINE_H*rowsPerP+PAD);
            
            // Name shadow + text
            g.setColor(WOOD_DARK);
            g.drawString(p.getName(), x+2+2, cursor+LINE_H+1+1);
            g.setColor(pc);
            g.drawString(p.getName(), x+2, cursor+LINE_H+1);
            cursor += LINE_H + 2;

            // Stat rows
            g.setFont(fontVTZ != null ? fontVTZ.deriveFont(16f) : new Font("Monospaced",Font.PLAIN,11));
            drawStatRow(g, x+2, cursor, "HP",    p.getHp()+" / "+p.getMaxHp()); cursor+=LINE_H;
            drawStatRow(g, x+2, cursor, "SPD",   String.format("%.1f",p.getSpeed())); cursor+=LINE_H;
            drawStatRow(g, x+2, cursor, "DMG",   String.valueOf(p.getDamage())); cursor+=LINE_H;
            drawStatRow(g, x+2, cursor, "ASPD",  p.getShootCooldown()+"t"); cursor+=LINE_H;

            List<String> pups = p.getActivePowerUps();
            String pu = buildPuSummary(countOf(pups,"DAMAGE"),countOf(pups,"HP"),
                                       countOf(pups,"MOVEMENT"),countOf(pups,"ATTACK_SPEED"));
            drawStatRow(g, x+2, cursor, "BUFF",  pu);
            cursor += LINE_H + PAD;
        }
    }

    private void drawStatRow(Graphics2D g, int x, int y, String label, String value) {
        g.setColor(GOLD_DARK);
        g.drawString(label+":", x+4, y+1);
        g.setColor(PARCHMENT);
        g.drawString(label+":", x+4, y);
        
        g.setColor(WOOD_DARK);
        g.drawString(value, x+60, y+1);
        g.setColor(GHOSTLY_WHITE);
        g.drawString(value, x+60, y);
    }

    // ── Round banner ──────────────────────────────────────────────────────────
    private void drawRoundBanner(Graphics2D g) {
        if (roundBannerTicks <= 0) return;
        float alpha = Math.min(1f, (float)roundBannerTicks/40f);
        int W=Constants.SCREEN_WIDTH, H=Constants.SCREEN_HEIGHT;
        int cx=W/2, cy=H/2-50;

        // Wooden backdrop with gold border
        g.setColor(new Color(WOOD_BASE.getRed(), WOOD_BASE.getGreen(), WOOD_BASE.getBlue(), (int)(220*alpha)));
        g.fillRoundRect(cx-200, cy-50, 400, 110, 12, 12);
        
        g.setColor(new Color(GOLD_ACCENT.getRed(), GOLD_ACCENT.getGreen(), GOLD_ACCENT.getBlue(), (int)(255*alpha)));
        g.setStroke(new BasicStroke(4f));
        g.drawRoundRect(cx-200,cy-50,400,110,12,12);
        g.setColor(new Color(GOLD_DARK.getRed(), GOLD_DARK.getGreen(), GOLD_DARK.getBlue(), (int)(255*alpha)));
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(cx-198,cy-48,396,106,10,10);
        g.setStroke(new BasicStroke(1f));

        // Text
        Font bannerFont = fontPixelMd != null ? fontPixelMd.deriveFont(26f) : new Font("Monospaced",Font.BOLD,26);
        g.setFont(bannerFont);
        String text = "ROUND " + roundBannerRound;
        FontMetrics fm = g.getFontMetrics();
        int tx = cx - fm.stringWidth(text)/2;

        g.setColor(new Color(WOOD_DARK.getRed(), WOOD_DARK.getGreen(), WOOD_DARK.getBlue(), (int)(255*alpha)));
        g.drawString(text, tx+3, cy+12+3);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g.setColor(GOLD_ACCENT);
        g.drawString(text, tx, cy+12);

        // Subtitle
        Font subFont = fontVTZ != null ? fontVTZ.deriveFont(20f) : new Font("Monospaced",Font.PLAIN,14);
        g.setFont(subFont);
        g.setColor(new Color(PARCHMENT.getRed(), PARCHMENT.getGreen(), PARCHMENT.getBlue(), (int)(255*alpha)));
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

            // Background — Wooden panel
            g.setColor(new Color(WOOD_BASE.getRed(), WOOD_BASE.getGreen(), WOOD_BASE.getBlue(), (int)(240*alpha)));
            g.fillRoundRect(cx-nW/2, y, nW, nH, 6, 6);

            // Border — Gold accent
            g.setColor(new Color(GOLD_ACCENT.getRed(), GOLD_ACCENT.getGreen(), GOLD_ACCENT.getBlue(), (int)(200*alpha)));
            g.setStroke(new BasicStroke(2f));
            g.drawRoundRect(cx-nW/2, y, nW, nH, 6, 6);

            // Left accent bar
            g.setColor(new Color(GOLD_DARK.getRed(), GOLD_DARK.getGreen(), GOLD_DARK.getBlue(), (int)(255*alpha)));
            g.fillRect(cx-nW/2+2, y+3, 3, nH-6);

            // Text
            Font nFont = fontVTZ != null ? fontVTZ.deriveFont(17f) : new Font("Monospaced",Font.BOLD,12);
            g.setFont(nFont);
            g.setColor(new Color(WOOD_DARK.getRed(), WOOD_DARK.getGreen(), WOOD_DARK.getBlue(), (int)(255*alpha)));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(n.message, cx-fm.stringWidth(n.message)/2 + 1, y+nH-5 + 1); // shadow
            
            g.setColor(new Color(PARCHMENT.getRed(), PARCHMENT.getGreen(), PARCHMENT.getBlue(), (int)(255*alpha)));
            g.drawString(n.message, cx-fm.stringWidth(n.message)/2, y+nH-5);
        }
        g.setStroke(new BasicStroke(1f));
    }

    // ── Shared helper ─────────────────────────────────────────────────────────
    private void drawFantasyPanel(Graphics2D g, int x, int y, int w, int h, int alpha) {
        // Base wood
        g.setColor(new Color(WOOD_BASE.getRed(), WOOD_BASE.getGreen(), WOOD_BASE.getBlue(), alpha));
        g.fillRoundRect(x, y, w, h, 10, 10);
        
        // Dark inner border
        g.setColor(new Color(WOOD_DARK.getRed(), WOOD_DARK.getGreen(), WOOD_DARK.getBlue(), alpha));
        g.setStroke(new BasicStroke(4f));
        g.drawRoundRect(x+2, y+2, w-4, h-4, 8, 8);
        
        // Gold outer border
        g.setColor(new Color(GOLD_ACCENT.getRed(), GOLD_ACCENT.getGreen(), GOLD_ACCENT.getBlue(), alpha));
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(x, y, w, h, 10, 10);
        
        g.setStroke(new BasicStroke(1f));
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