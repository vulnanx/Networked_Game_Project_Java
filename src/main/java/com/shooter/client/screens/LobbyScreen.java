package com.shooter.client.screens;

import com.shooter.client.GameClient;
import com.shooter.client.ScreenManager;
import com.shooter.network.LobbyState;
import com.shooter.shared.util.AssetManager;
import com.shooter.shared.util.Constants;
import com.shooter.shared.util.FontManager;
import com.shooter.shared.util.GameSettings;

import javax.swing.*;
import java.awt.*;
import java.awt.RadialGradientPaint;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URL;
import java.util.Random;

/**
 * ============================================================
 * FILE: LobbyScreen.java
 * PACKAGE: client.screens
 * OWNER: Geastin (Member C) — UI REDESIGN: Christel
 * ============================================================
 * Dark horror pixel art lobby screen.
 * All networking/logic preserved. Only rendering overhauled.
 * ============================================================
 */
public class LobbyScreen implements Screen {

    // ── Horror palette ────────────────────────────────────────────────────────
    private static final Color VOID_PURPLE   = new Color(0x1A, 0x0A, 0x2E);
    private static final Color TOXIC_GREEN   = new Color(0x39, 0xFF, 0x14);
    private static final Color BLOOD_RED     = new Color(0x8B, 0x00, 0x00);
    private static final Color BRIGHT_RED    = new Color(0xCC, 0x00, 0x00);
    private static final Color GHOSTLY_WHITE = new Color(0xE8, 0xE8, 0xF0);
    private static final Color DARK_BG       = new Color(0x0D, 0x0D, 0x1A);
    private static final Color PURPLE_GLOW   = new Color(0x7B, 0x2F, 0xBE);

    private static final Color[] PLAYER_COLORS = {
        new Color(0x4A, 0x90, 0xD9),
        new Color(0xCC, 0x00, 0x00),
        new Color(0x39, 0xFF, 0x14),
        new Color(0xF5, 0xA6, 0x23)
    };

    // ── Core refs ─────────────────────────────────────────────────────────────
    private final ScreenManager screenManager;
    private final GameClient    gameClient;
    private final String        serverIp;
    private boolean             isHost;

    // ── Lobby data ────────────────────────────────────────────────────────────
    private String[]  playerNames  = new String[4];
    private boolean[] readyFlags   = new boolean[4];
    private int       localPlayerId = 0;
    private boolean   localReady   = false;
    private GameSettings currentSettings;

    // ── Buttons ───────────────────────────────────────────────────────────────
    private final Rectangle backBtn;
    private final Rectangle readyBtn;
    private final Rectangle startBtn;
    private final Rectangle settingsBtn;
    private boolean backHovered, readyHovered, startHovered, settingsHovered;

    // ── Fonts ─────────────────────────────────────────────────────────────────
    private Font fontTitle;
    private Font fontSlot;
    private Font fontBody;
    private Font fontBtn;

    // ── Background ────────────────────────────────────────────────────────────
    private ImageIcon    animatedBg;
    private BufferedImage staticBg;

    // ── Animation ─────────────────────────────────────────────────────────────
    private int   tick       = 0;
    private float btnPulse   = 0f;

    // ── Particles ─────────────────────────────────────────────────────────────
    private static final int PC = 24;
    private final float[] px = new float[PC], py = new float[PC],
                          pvx= new float[PC], pvy= new float[PC],
                          pa = new float[PC], ps = new float[PC];
    private final Random rng = new Random();

    // ─────────────────────────────────────────────────────────────────────────

    public LobbyScreen(ScreenManager sm, String ip, boolean host) {
        this(sm, ip, host, null);
    }

    public LobbyScreen(ScreenManager sm, String ip, boolean host, GameClient gc) {
        this.screenManager = sm;
        this.serverIp      = ip;
        this.isHost        = host;
        this.gameClient    = gc;

        int W = Constants.SCREEN_WIDTH, H = Constants.SCREEN_HEIGHT;
        backBtn     = new Rectangle(30,          H - 70,  140, 42);
        readyBtn    = new Rectangle(W/2 - 100,   H - 78,  200, 46);
        startBtn    = new Rectangle(W - 190,     H - 70,  160, 42);
        settingsBtn = new Rectangle(W - 190,     H - 128, 160, 42);

        loadFonts();
        loadBackground();
        for (int i = 0; i < PC; i++) spawnParticle(i, true);

        currentSettings = (gc != null) ? gc.getLastKnownSettings() : new GameSettings();
    }

    // ── Font / bg loading ─────────────────────────────────────────────────────
    private void loadFonts() {
        fontTitle = ttf("/assets/fonts/PressStart2P-Regular.ttf", 16f);
        fontBtn   = ttf("/assets/fonts/PressStart2P-Regular.ttf", 10f);
        fontSlot  = ttf("/assets/fonts/PressStart2P-Regular.ttf",  9f);
        fontBody  = ttf("/assets/fonts/VT323-Regular.ttf",        20f);
        if (fontTitle == null) fontTitle = new Font("Monospaced", Font.BOLD, 16);
        if (fontBtn   == null) fontBtn   = new Font("Monospaced", Font.BOLD, 12);
        if (fontSlot  == null) fontSlot  = new Font("Monospaced", Font.BOLD, 11);
        if (fontBody  == null) fontBody  = new Font("Monospaced", Font.PLAIN, 16);
    }

    private Font ttf(String path, float sz) {
        try {
            InputStream is = getClass().getResourceAsStream(path);
            return is == null ? null : Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(sz);
        } catch (Exception e) { return null; }
    }

    private void loadBackground() {
        URL gif = getClass().getResource("/assets/ui/bg_lobby.gif");
        if (gif != null) { animatedBg = new ImageIcon(gif); return; }
        URL png = getClass().getResource("/assets/ui/bg_lobby.png");
        if (png != null) { animatedBg = new ImageIcon(png); return; }
        BufferedImage img = AssetManager.getInstance().get("lobby_bg");
        if (img != null && img.getRGB(0,0) != Color.MAGENTA.getRGB()) staticBg = img;
    }

    // ── Particles ─────────────────────────────────────────────────────────────
    private void spawnParticle(int i, boolean anyY) {
        px[i]  = rng.nextFloat() * Constants.SCREEN_WIDTH;
        py[i]  = anyY ? rng.nextFloat() * Constants.SCREEN_HEIGHT : Constants.SCREEN_HEIGHT + 5;
        pvx[i] = (rng.nextFloat() - 0.5f) * 0.5f;
        pvy[i] = -(0.3f + rng.nextFloat() * 0.9f);
        pa[i]  = 0.1f + rng.nextFloat() * 0.45f;
        ps[i]  = 2f + rng.nextFloat() * 3.5f;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    @Override public void onEnter() {
        tick = 0;
        if (playerNames[localPlayerId] == null)
            playerNames[localPlayerId] = isHost ? "Host Player" : "Player";
        if (gameClient != null) {
            setLocalPlayerId(gameClient.getMyPlayerId());
            gameClient.setLobbyStateListener(this::applyLobbyStateOnUiThread);
            gameClient.setSettingsListener(s -> currentSettings = s);
        }
    }

    @Override public void onExit() {
        System.out.println("[LobbyScreen] Exiting lobby.");
    }

    // ── Update ────────────────────────────────────────────────────────────────
    @Override public void update() {
        tick++;
        if (readyHovered || startHovered || backHovered || settingsHovered)
            btnPulse = (float) Math.sin(tick * 0.14f) * 0.5f + 0.5f;
        for (int i = 0; i < PC; i++) {
            px[i] += pvx[i]; py[i] += pvy[i]; pa[i] -= 0.0025f;
            if (pa[i] <= 0 || py[i] < -10) spawnParticle(i, false);
        }
    }

    // ── Render ────────────────────────────────────────────────────────────────
    @Override public void render(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        drawBackground(g);
        drawParticles(g);
        drawVignette(g);
        drawHeader(g);
        drawPlayerSlots(g);
        drawSettingsSummary(g);
        drawLobbyButtons(g);
        drawStatusBar(g);

        if (gameClient != null && gameClient.getChatPanel() != null)
            gameClient.getChatPanel().render(g);
    }

    // ── Background ────────────────────────────────────────────────────────────
    private void drawBackground(Graphics2D g) {
        int W = Constants.SCREEN_WIDTH, H = Constants.SCREEN_HEIGHT;
        if (animatedBg != null) {
            g.drawImage(animatedBg.getImage(), 0, 0, W, H, null);
            g.setColor(new Color(0,0,0,100)); g.fillRect(0,0,W,H);
        } else if (staticBg != null) {
            g.drawImage(staticBg, 0, 0, W, H, null);
            g.setColor(new Color(0,0,0,100)); g.fillRect(0,0,W,H);
        } else {
            // Procedural fallback — darker than menu
            GradientPaint grad = new GradientPaint(0,0,DARK_BG, 0,H,new Color(0x10,0x04,0x20));
            g.setPaint(grad); g.fillRect(0,0,W,H);
            g.setColor(new Color(0x7B,0x2F,0xBE,14));
            for (int x=0;x<W;x+=42) g.drawLine(x,0,x,H);
            for (int y=0;y<H;y+=42) g.drawLine(0,y,W,y);
        }
    }

    private void drawParticles(Graphics2D g) {
        for (int i = 0; i < PC; i++) {
            float a = Math.max(0, pa[i]);
            g.setColor(i%2==0 ? new Color(0.55f,0f,0f,a) : new Color(0.48f,0.18f,0.74f,a));
            int s = (int) ps[i];
            g.fillRect((int)px[i],(int)py[i],s,s);
        }
    }

    private void drawVignette(Graphics2D g) {
        int W=Constants.SCREEN_WIDTH, H=Constants.SCREEN_HEIGHT;
        RadialGradientPaint v = new RadialGradientPaint(W/2f,H/2f,Math.max(W,H)/1.4f,
            new float[]{0f,1f}, new Color[]{new Color(0,0,0,0),new Color(0,0,0,200)});
        g.setPaint(v); g.fillRect(0,0,W,H);
    }

    // ── Header ────────────────────────────────────────────────────────────────
    private void drawHeader(Graphics2D g) {
        int W = Constants.SCREEN_WIDTH;

        // Blood-red top accent bar
        g.setColor(new Color(0x8B,0x00,0x00,180));
        g.fillRect(0, 0, W, 6);

        // Title
        String title = "LOBBY";
        g.setFont(fontTitle.deriveFont(22f));
        FontMetrics fm = g.getFontMetrics();

        // Shadow
        g.setColor(BLOOD_RED);
        g.drawString(title, W/2 - fm.stringWidth(title)/2 + 3, 68);
        // Main
        g.setColor(GHOSTLY_WHITE);
        g.drawString(title, W/2 - fm.stringWidth(title)/2, 65);

        // Server IP subtitle
        g.setFont(fontBody.deriveFont(18f));
        g.setColor(new Color(0x66,0x55,0x88));
        String ipLine = "SERVER: " + (serverIp != null ? serverIp : "localhost");
        fm = g.getFontMetrics();
        g.drawString(ipLine, W/2 - fm.stringWidth(ipLine)/2, 92);

        // Divider
        int lx = W/2-180, ly=108;
        g.setColor(new Color(0x8B,0x00,0x00,140));
        g.setStroke(new BasicStroke(1.5f));
        g.drawLine(lx, ly, lx+360, ly);
        // Drop dots
        for (int d=0;d<5;d++){
            int dx=lx+36+d*72;
            g.fillOval(dx-2,ly-2,5,5);
            g.fillRect(dx-1,ly+3,2,3+d%3*2);
        }
        g.setStroke(new BasicStroke(1f));
    }

    // ── Player slots ──────────────────────────────────────────────────────────
    private void drawPlayerSlots(Graphics2D g) {
        int W=Constants.SCREEN_WIDTH;
        int slotW=500, slotH=78;
        int slotX=W/2 - slotW/2;
        for (int i=0;i<4;i++) {
            int slotY = 130 + i * 96;
            drawOneSlot(g, slotX, slotY, slotW, slotH, i, playerNames[i], readyFlags[i]);
        }
    }

    private void drawOneSlot(Graphics2D g, int x, int y, int w, int h,
                              int idx, String name, boolean ready) {
        boolean occupied = (name != null);
        boolean isLocal  = (idx == localPlayerId);
        Color accent = PLAYER_COLORS[idx];

        // Outer glow for local player's slot
        if (isLocal) {
            g.setColor(new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),40));
            g.fillRoundRect(x-5,y-5,w+10,h+10,14,14);
        }

        // Slot body
        g.setColor(new Color(0x0D,0x05,0x1E,210));
        g.fillRoundRect(x,y,w,h,10,10);

        // Border: green=ready, accent=occupied, dim=empty
        Color border = ready ? TOXIC_GREEN : (occupied ? accent : new Color(0x33,0x22,0x44));
        int bAlpha   = ready ? 255 : (occupied ? 160 : 80);
        g.setColor(new Color(border.getRed(),border.getGreen(),border.getBlue(),bAlpha));
        g.setStroke(new BasicStroke(ready ? 2.5f : 1.5f));
        g.drawRoundRect(x,y,w,h,10,10);
        g.setStroke(new BasicStroke(1f));

        // Left color bar
        g.setColor(new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),occupied?200:60));
        g.fillRoundRect(x,y,5,h,4,4);

        // Player badge circle
        int cx=x+22, cy=y+h/2-16;
        g.setColor(new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),50));
        g.fillOval(cx-4,cy-4,40,40);
        g.setColor(accent);
        g.fillOval(cx,cy,32,32);
        g.setFont(fontSlot.deriveFont(9f));
        g.setColor(DARK_BG);
        FontMetrics fm=g.getFontMetrics();
        String badge="P"+(idx+1);
        g.drawString(badge, cx+16-fm.stringWidth(badge)/2, cy+21);

        // Name or waiting
        g.setFont(fontSlot.deriveFont(9f));
        g.setColor(occupied ? GHOSTLY_WHITE : new Color(0x44,0x33,0x55));
        g.drawString(occupied ? name : "WAITING...", x+68, y+h/2+4);

        // YOU tag for local player
        if (isLocal && occupied) {
            g.setFont(fontBody.deriveFont(15f));
            g.setColor(new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),180));
            g.drawString("← YOU", x+68+fm.stringWidth(name!=null?name:"")+10, y+h/2+4);
        }

        // Ready badge
        if (ready) {
            g.setFont(fontSlot.deriveFont(8f));
            g.setColor(new Color(0x39,0xFF,0x14,220));
            String rTxt="✓ READY";
            FontMetrics rfm=g.getFontMetrics();
            g.drawString(rTxt, x+w-rfm.stringWidth(rTxt)-14, y+h/2+4);
        } else if (occupied) {
            g.setFont(fontBody.deriveFont(16f));
            g.setColor(new Color(0x55,0x44,0x66));
            String rTxt="NOT READY";
            FontMetrics rfm=g.getFontMetrics();
            g.drawString(rTxt, x+w-rfm.stringWidth(rTxt)-14, y+h/2+5);
        }
    }

    // ── Settings summary ──────────────────────────────────────────────────────
    private void drawSettingsSummary(Graphics2D g) {
        if (currentSettings==null) return;
        int W=Constants.SCREEN_WIDTH;
        int sx=W/2-240, sy=518, sw=480, sh=42;

        // Panel
        g.setColor(new Color(0x0A,0x03,0x18,200));
        g.fillRoundRect(sx,sy,sw,sh,8,8);
        g.setColor(new Color(0x7B,0x2F,0xBE,100));
        g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(sx,sy,sw,sh,8,8);

        // Label
        g.setFont(fontBody.deriveFont(15f));
        g.setColor(PURPLE_GLOW);
        g.drawString("SETTINGS:", sx+10, sy+16);

        // Values
        g.setColor(new Color(0xAA,0xAA,0xCC));
        String s=String.format("HP:%d  SPD:%.1f  DMG:%d  ROUNDS:%d  DROP:%.0f%%",
            currentSettings.getPlayerBaseHp(),
            currentSettings.getPlayerBaseSpeed(),
            currentSettings.getPlayerBaseDamage(),
            currentSettings.getTotalRounds(),
            currentSettings.getPowerUpDropChance()*100f);
        g.drawString(s, sx+10, sy+33);

        if (isHost) {
            g.setColor(new Color(0x44,0x33,0x55));
            g.setFont(fontBody.deriveFont(14f));
            g.drawString("click ⚙ to configure →", sx+sw-162, sy+33);
        }
    }

    // ── Buttons ───────────────────────────────────────────────────────────────
    private void drawLobbyButtons(Graphics2D g) {
        drawHorrorBtn(g, backBtn,  "← BACK",   backHovered,     BLOOD_RED);
        // Ready button label changes with state
        String readyLabel = localReady ? "✓ READY!" : "READY UP";
        Color  readyColor = localReady ? TOXIC_GREEN : new Color(0x39,0xFF,0x14,180);
        drawHorrorBtn(g, readyBtn, readyLabel, readyHovered, readyColor);
        if (isHost) {
            drawHorrorBtn(g, settingsBtn, "⚙ SETTINGS", settingsHovered, PURPLE_GLOW);
            boolean canStart = canHostStart();
            Color startColor = canStart ? TOXIC_GREEN : new Color(0x33,0x44,0x33);
            drawHorrorBtn(g, startBtn, "▶ START", startHovered && canStart, startColor);
        }
    }

    private void drawHorrorBtn(Graphics2D g, Rectangle r, String label, boolean hovered, Color accent) {
        int x=r.x, y=r.y, w=r.width, h=r.height;

        // Outer glow
        if (hovered) {
            float glow = 0.3f + btnPulse * 0.4f;
            g.setColor(new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),(int)(glow*90)));
            g.fillRoundRect(x-5,y-5,w+10,h+10,12,12);
        }

        // Body
        g.setColor(new Color(0x0D,0x0D,0x1A,210));
        g.fillRoundRect(x,y,w,h,8,8);

        // Border
        int ba=hovered?255:120;
        g.setColor(new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),ba));
        g.setStroke(new BasicStroke(hovered?2f:1.5f));
        g.drawRoundRect(x,y,w,h,8,8);
        g.setStroke(new BasicStroke(1f));

        // Left bar
        g.setColor(accent);
        g.fillRect(x+1,y+7,3,h-14);

        // Text
        g.setFont(fontBtn);
        FontMetrics fm=g.getFontMetrics();
        int tx=x+w/2-fm.stringWidth(label)/2;
        int ty=y+h/2+fm.getAscent()/2-2;
        g.setColor(new Color(0,0,0,150)); g.drawString(label,tx+2,ty+2);
        g.setColor(hovered?GHOSTLY_WHITE:new Color(
            (accent.getRed()+GHOSTLY_WHITE.getRed())/2,
            (accent.getGreen()+GHOSTLY_WHITE.getGreen())/2,
            (accent.getBlue()+GHOSTLY_WHITE.getBlue())/2));
        g.drawString(label,tx,ty);
    }

    // ── Status bar ────────────────────────────────────────────────────────────
    private void drawStatusBar(Graphics2D g) {
        int W=Constants.SCREEN_WIDTH, H=Constants.SCREEN_HEIGHT;
        // Bottom accent bar
        g.setColor(new Color(0x8B,0x00,0x00,120));
        g.fillRect(0,H-6,W,6);

        g.setFont(fontBody.deriveFont(17f));
        g.setColor(new Color(0x44,0x33,0x55));
        String status = isHost
            ? "Waiting for players to ready up..."
            : "Waiting for host to start the game...";
        FontMetrics fm=g.getFontMetrics();
        g.drawString(status, W/2-fm.stringWidth(status)/2, H-14);
    }

    // ── Input ─────────────────────────────────────────────────────────────────
    @Override
    public void handleMouseClicked(int x, int y) {
        if (backBtn.contains(x,y)) {
            if (gameClient!=null) gameClient.disconnect();
            screenManager.setScreen(new MainMenuScreen(screenManager, gameClient,
                gameClient!=null ? gameClient.getGameState() : null));
            return;
        }
        if (readyBtn.contains(x,y)) { toggleLocalReady(); return; }
        if (isHost && settingsBtn.contains(x,y)) {
            screenManager.setScreen(new SettingsScreen(screenManager, gameClient, true,
                () -> screenManager.setScreen(this)));
            return;
        }
        if (isHost && startBtn.contains(x,y) && canHostStart()) {
            if (gameClient!=null) gameClient.sendStartGameRequest();
        }
    }

    @Override
    public void handleMouseMoved(int x, int y) {
        backHovered     = backBtn.contains(x,y);
        readyHovered    = readyBtn.contains(x,y);
        startHovered    = startBtn.contains(x,y);
        settingsHovered = settingsBtn.contains(x,y);
    }

    @Override
    public void handleKeyPressed(int keyCode) {
        if (gameClient!=null && gameClient.getChatPanel()!=null)
            gameClient.getChatPanel().handleKeyPressed(keyCode);
    }

    @Override
    public void handleKeyTyped(char keyChar) {
        if (gameClient!=null && gameClient.getChatPanel()!=null)
            gameClient.getChatPanel().handleKeyTyped(keyChar);
    }

    // ── Lobby logic (unchanged) ───────────────────────────────────────────────
    public void updateFromLobbyState(String[] names, boolean[] flags) {
        this.playerNames = names;
        this.readyFlags  = flags;
        if (localPlayerId>=0 && localPlayerId<flags.length)
            localReady = flags[localPlayerId];
    }

    public void setLocalPlayerId(int id) {
        if (id<0||id>=playerNames.length) return;
        this.localPlayerId = id;
        if (playerNames[id]==null) playerNames[id]="Player "+(id+1);
    }

    public boolean isLocalReady() { return localReady; }

    private void toggleLocalReady() {
        localReady = !localReady;
        readyFlags[localPlayerId] = localReady;
        if (playerNames[localPlayerId]==null)
            playerNames[localPlayerId]="Player "+(localPlayerId+1);
        if (gameClient!=null) gameClient.sendReadyStatus(localReady);
    }

    private boolean canHostStart() {
        boolean any=false;
        for (int i=0;i<playerNames.length;i++) {
            if (playerNames[i]==null) continue;
            any=true;
            if (!readyFlags[i]) return false;
        }
        return any;
    }

    private void applyLobbyStateOnUiThread(LobbyState ls) {
        SwingUtilities.invokeLater(() -> {
            updateFromLobbyState(ls.getPlayerNames(), ls.getReadyFlags());
            setLocalPlayerId(gameClient.getMyPlayerId());
            boolean wasHost = isHost;
            isHost = (ls.getHostPlayerId()==gameClient.getMyPlayerId());
            if (!wasHost && isHost)
                System.out.println("[LobbyScreen] Promoted to host!");
        });
    }
}
