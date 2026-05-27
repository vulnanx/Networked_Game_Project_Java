package com.shooter.client.screens;

import com.shooter.client.GameClient;
import com.shooter.client.ScreenManager;
import com.shooter.shared.model.GameState;
import com.shooter.shared.util.AssetManager;
import com.shooter.shared.util.Constants;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.RadialGradientPaint;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * ============================================================
 * FILE: MainMenuScreen.java
 * PACKAGE: client.screens
 * OWNER: Geastin (Member C) — UI REDESIGN: Christel
 * ============================================================
 * Dark horror pixel art main menu.
 * Palette: Void Purple · Toxic Green · Blood Red · Ghostly White
 * Fonts: Press Start 2P (header) + VT323 (body)
 * ============================================================
 */
public class MainMenuScreen implements Screen {

    // ── Colors ────────────────────────────────────────────────────────────────
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

    // ── Core references ───────────────────────────────────────────────────────
    private final ScreenManager screenManager;
    private final GameClient    gameClient;
    private final GameState     gameState;

    // ── Assets ────────────────────────────────────────────────────────────────
    private ImageIcon  animatedBg;       // GIF background — null if missing
    private BufferedImage staticBg;      // fallback static bg
    private Font fontTitle;              // Press Start 2P — large
    private Font fontBtn;                // Press Start 2P — buttons
    private Font fontBody;               // VT323 — body / footer

    // ── Button geometry ───────────────────────────────────────────────────────
    private static final int BTN_W = 260;
    private static final int BTN_H = 52;
    private static final int BTN_GAP = 18;
    private Rectangle hostBtn, joinBtn, exitBtn;

    // ── State ─────────────────────────────────────────────────────────────────
    private int     hoveredIndex  = -1;  // -1=none 0=host 1=join 2=exit
    private int     tick          = 0;
    private float   flickerAlpha  = 1f;
    private int     flickerTimer  = 0;
    private boolean enteringIp    = false;
    private String  joinIp        = "";
    private boolean ipCursorOn    = true;
    private int     ipCursorTimer = 0;
    private String  statusMessage = null;
    private Color   statusColor   = BRIGHT_RED;

    // ── Blood particle system ─────────────────────────────────────────────────
    private static final int PARTICLE_COUNT = 38;
    private final float[] px, py, pvx, pvy, palpha, psize;
    private final Random rng = new Random();

    // ── Hover pulse ───────────────────────────────────────────────────────────
    private float btnPulse = 0f;

    // ── Constructor ───────────────────────────────────────────────────────────
    public MainMenuScreen(ScreenManager screenManager) {
        this(screenManager, null, null);
    }

    public MainMenuScreen(ScreenManager screenManager, GameClient gameClient, GameState gameState) {
        this.screenManager = screenManager;
        this.gameClient    = gameClient;
        this.gameState     = gameState;

        buildButtonRects();
        loadFonts();
        loadBackground();

        // Init particle arrays
        px    = new float[PARTICLE_COUNT];
        py    = new float[PARTICLE_COUNT];
        pvx   = new float[PARTICLE_COUNT];
        pvy   = new float[PARTICLE_COUNT];
        palpha= new float[PARTICLE_COUNT];
        psize = new float[PARTICLE_COUNT];
        for (int i = 0; i < PARTICLE_COUNT; i++) spawnParticle(i, true);
    }

    // ── Button layout ─────────────────────────────────────────────────────────
    private void buildButtonRects() {
        int totalH = BTN_H * 3 + BTN_GAP * 2;
        int startY = Constants.SCREEN_HEIGHT / 2 + 60;
        int bx     = Constants.SCREEN_WIDTH / 2 - BTN_W / 2;
        hostBtn = new Rectangle(bx, startY,             BTN_W, BTN_H);
        joinBtn = new Rectangle(bx, startY + BTN_H + BTN_GAP, BTN_W, BTN_H);
        exitBtn = new Rectangle(bx, startY + (BTN_H + BTN_GAP) * 2, BTN_W, BTN_H);
    }

    // ── Font loading ──────────────────────────────────────────────────────────
    private void loadFonts() {
        fontTitle = loadTtf("/assets/fonts/PressStart2P-Regular.ttf", 28f);
        fontBtn   = loadTtf("/assets/fonts/PressStart2P-Regular.ttf", 11f);
        fontBody  = loadTtf("/assets/fonts/VT323-Regular.ttf",        22f);
        if (fontTitle == null) fontTitle = new Font("Monospaced", Font.BOLD, 28);
        if (fontBtn   == null) fontBtn   = new Font("Monospaced", Font.BOLD, 13);
        if (fontBody  == null) fontBody  = new Font("Monospaced", Font.PLAIN, 18);
    }

    private Font loadTtf(String path, float size) {
        try {
            InputStream is = getClass().getResourceAsStream(path);
            if (is == null) return null;
            return Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(size);
        } catch (Exception e) {
            return null;
        }
    }

    // ── Background loading ────────────────────────────────────────────────────
    private void loadBackground() {
        // 1) Animated GIF
        URL gifUrl = getClass().getResource("/assets/ui/bg_menu.gif");
        if (gifUrl != null) { animatedBg = new ImageIcon(gifUrl); return; }

        // 2) Static PNG at the expected ui/ path
        URL pngUrl = getClass().getResource("/assets/ui/bg_menu.png");
        if (pngUrl != null) { animatedBg = new ImageIcon(pngUrl); return; }

        // 3) Legacy AssetManager key (old tiles/ location)
        BufferedImage img = AssetManager.getInstance().get("main_bg");
        if (img != null && img.getRGB(0, 0) != Color.MAGENTA.getRGB()) {
            staticBg = img;
        }
        // If all three miss, drawBackground() falls through to the procedural gradient.
    }


    // ── Particle helpers ──────────────────────────────────────────────────────
    private void spawnParticle(int i, boolean randomY) {
        px[i]    = rng.nextFloat() * Constants.SCREEN_WIDTH;
        py[i]    = randomY ? rng.nextFloat() * Constants.SCREEN_HEIGHT
                           : Constants.SCREEN_HEIGHT + 5;
        pvx[i]   = (rng.nextFloat() - 0.5f) * 0.6f;
        pvy[i]   = -(0.4f + rng.nextFloat() * 1.2f);
        palpha[i]= 0.15f + rng.nextFloat() * 0.55f;
        psize[i] = 2f + rng.nextFloat() * 4f;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    @Override public void onEnter() {
        tick = 0; enteringIp = false; joinIp = "";
        statusMessage = null;
    }

    @Override public void onExit() {}

    // ── Update ────────────────────────────────────────────────────────────────
    @Override
    public void update() {
        tick++;

        // Title flicker
        flickerTimer++;
        if (flickerTimer % 9 == 0) flickerAlpha = 0.75f + rng.nextFloat() * 0.25f;

        // Button hover pulse
        if (hoveredIndex != -1) btnPulse = (float) Math.sin(tick * 0.12f) * 0.5f + 0.5f;

        // IP cursor blink
        ipCursorTimer++;
        if (ipCursorTimer % 28 == 0) ipCursorOn = !ipCursorOn;

        // Update particles (blood/dust rising)
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            px[i] += pvx[i];
            py[i] += pvy[i];
            palpha[i] -= 0.003f;
            if (palpha[i] <= 0 || py[i] < -10) spawnParticle(i, false);
        }
    }

    // ── Render ────────────────────────────────────────────────────────────────
    @Override
    public void render(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,         RenderingHints.VALUE_RENDER_QUALITY);

        drawBackground(g);
        drawParticles(g);
        drawVignette(g);
        drawTitle(g);
        drawPlayerBadges(g);
        drawButtons(g);
        drawFooter(g);
        drawStatus(g);

        if (enteringIp) drawIpPrompt(g);
    }

    // ── Draw: Background ──────────────────────────────────────────────────────
    private void drawBackground(Graphics2D g) {
        int W = Constants.SCREEN_WIDTH, H = Constants.SCREEN_HEIGHT;

        if (animatedBg != null) {
            g.drawImage(animatedBg.getImage(), 0, 0, W, H, null);
            // Dark tint over bg so UI stays readable
            g.setColor(new Color(0, 0, 0, 90));
            g.fillRect(0, 0, W, H);
        } else if (staticBg != null) {
            g.drawImage(staticBg, 0, 0, W, H, null);
            g.setColor(new Color(0, 0, 0, 90));
            g.fillRect(0, 0, W, H);
        } else {
            // Procedural gradient fallback
            GradientPaint grad = new GradientPaint(0, 0, DARK_BG, 0, H, VOID_PURPLE);
            g.setPaint(grad);
            g.fillRect(0, 0, W, H);

            // Subtle grid lines — haunted feel
            g.setColor(new Color(0x7B, 0x2F, 0xBE, 18));
            for (int x = 0; x < W; x += 42) g.drawLine(x, 0, x, H);
            for (int y = 0; y < H; y += 42) g.drawLine(0, y, W, y);
        }
    }

    // ── Draw: Particles ───────────────────────────────────────────────────────
    private void drawParticles(Graphics2D g) {
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            float a = Math.max(0, palpha[i]);
            // Alternate blood red and purple glow
            Color c = (i % 2 == 0)
                ? new Color(0.55f, 0f, 0f, a)
                : new Color(0.48f, 0.18f, 0.74f, a);
            g.setColor(c);
            int s = (int) psize[i];
            g.fillRect((int) px[i], (int) py[i], s, s);
        }
    }

    // ── Draw: Vignette ────────────────────────────────────────────────────────
    private void drawVignette(Graphics2D g) {
        int W = Constants.SCREEN_WIDTH, H = Constants.SCREEN_HEIGHT;
        RadialGradientPaint vignette = new RadialGradientPaint(
            W / 2f, H / 2f,
            Math.max(W, H) / 1.4f,
            new float[]{ 0f, 1f },
            new Color[]{ new Color(0, 0, 0, 0), new Color(0, 0, 0, 210) }
        );
        g.setPaint(vignette);
        g.fillRect(0, 0, W, H);
    }

    // ── Draw: Title ───────────────────────────────────────────────────────────
    private void drawTitle(Graphics2D g) {
        int W = Constants.SCREEN_WIDTH;
        String line1 = "HOLY";
        String line2 = "SHOT!";
        int y1 = 210, y2 = 260;

        // Glow layer (purple, blurred via multiple passes)
        g.setFont(fontTitle.deriveFont(46f));
        for (int r = 12; r > 0; r -= 2) {
            g.setColor(new Color(0x7B, 0x2F, 0xBE, 14));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(line1, W/2 - fm.stringWidth(line1)/2 + r, y1 + r);
            g.drawString(line1, W/2 - fm.stringWidth(line1)/2 - r, y1 - r);
            g.drawString(line2, W/2 - fm.stringWidth(line2)/2 + r, y2 + r);
            g.drawString(line2, W/2 - fm.stringWidth(line2)/2 - r, y2 - r);
        }

        // Shadow pass (blood red, offset)
        g.setColor(BLOOD_RED);
        drawCenteredString(g, line1, W, y1 + 4, fontTitle.deriveFont(46f));
        drawCenteredString(g, line2, W, y2 + 4, fontTitle.deriveFont(46f));

        // Main text (ghostly white, flicker)
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, flickerAlpha));
        g.setColor(GHOSTLY_WHITE);
        drawCenteredString(g, line1, W, y1, fontTitle.deriveFont(46f));
        drawCenteredString(g, line2, W, y2, fontTitle.deriveFont(46f));
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

        // Subtitle
        g.setFont(fontBody);
        g.setColor(new Color(0xAA, 0xAA, 0xCC));
        drawCenteredString(g, "A CO-OP HORROR SHOOTER", W, 298, fontBody);

        // Decorative divider line with blood drops
        int lx = W/2 - 160, ly = 316;
        g.setColor(new Color(BLOOD_RED.getRed(), BLOOD_RED.getGreen(), BLOOD_RED.getBlue(), 180));
        g.setStroke(new BasicStroke(1.5f));
        g.drawLine(lx, ly, lx + 320, ly);
        // Three blood drop dots
        for (int d = 0; d < 3; d++) {
            int dx = lx + 80 + d * 80;
            g.fillOval(dx - 3, ly - 3, 6, 6);
            g.fillRect(dx - 1, ly + 3, 2, 5 + d * 2);
        }
        g.setStroke(new BasicStroke(1f));
    }

    // ── Draw: Player badges ───────────────────────────────────────────────────
    private void drawPlayerBadges(Graphics2D g) {
        int W = Constants.SCREEN_WIDTH;
        int totalW = 4 * 22 + 3 * 14;
        int startX = W / 2 - totalW / 2;
        int y = hostBtn.y - 44;

        g.setFont(fontBody.deriveFont(16f));
        for (int i = 0; i < 4; i++) {
            int cx = startX + i * 36;
            // Glow ring
            g.setColor(new Color(PLAYER_COLORS[i].getRed(),
                                 PLAYER_COLORS[i].getGreen(),
                                 PLAYER_COLORS[i].getBlue(), 60));
            g.fillOval(cx - 4, y - 4, 30, 30);
            // Fill
            g.setColor(PLAYER_COLORS[i]);
            g.fillOval(cx, y, 22, 22);
            // Label
            g.setColor(DARK_BG);
            FontMetrics fm = g.getFontMetrics();
            String label = "P" + (i + 1);
            g.drawString(label, cx + 11 - fm.stringWidth(label) / 2, y + 15);
        }
    }

    // ── Draw: Buttons ─────────────────────────────────────────────────────────
    private void drawButtons(Graphics2D g) {
        drawHorrorButton(g, hostBtn, "HOST GAME", hoveredIndex == 0, TOXIC_GREEN);
        drawHorrorButton(g, joinBtn, "JOIN GAME", hoveredIndex == 1, BRIGHT_RED);
        drawHorrorButton(g, exitBtn, "EXIT",      hoveredIndex == 2, PURPLE_GLOW);
    }

    private void drawHorrorButton(Graphics2D g, Rectangle r, String label, boolean hovered, Color accent) {
        int x = r.x, y = r.y, w = r.width, h = r.height;

        // Outer glow when hovered
        if (hovered) {
            float glow = 0.3f + btnPulse * 0.4f;
            g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(),
                                 (int)(glow * 110)));
            g.fillRoundRect(x - 6, y - 6, w + 12, h + 12, 14, 14);
        }

        // Button body — dark panel
        g.setColor(new Color(0x0D, 0x0D, 0x1A, 210));
        g.fillRoundRect(x, y, w, h, 8, 8);

        // Border: accent colored, brighter on hover
        int borderAlpha = hovered ? 255 : 130;
        g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), borderAlpha));
        g.setStroke(new BasicStroke(hovered ? 2.5f : 1.5f));
        g.drawRoundRect(x, y, w, h, 8, 8);
        g.setStroke(new BasicStroke(1f));

        // Left accent bar
        g.setColor(accent);
        g.fillRect(x + 1, y + 8, 3, h - 16);

        // Label shadow
        g.setFont(fontBtn);
        FontMetrics fm = g.getFontMetrics();
        int tx = x + w / 2 - fm.stringWidth(label) / 2;
        int ty = y + h / 2 + fm.getAscent() / 2 - 2;
        g.setColor(new Color(0, 0, 0, 160));
        g.drawString(label, tx + 2, ty + 2);

        // Label main
        g.setColor(hovered ? GHOSTLY_WHITE : new Color(
            (accent.getRed()   + GHOSTLY_WHITE.getRed())   / 2,
            (accent.getGreen() + GHOSTLY_WHITE.getGreen()) / 2,
            (accent.getBlue()  + GHOSTLY_WHITE.getBlue())  / 2
        ));
        g.drawString(label, tx, ty);
    }

    // ── Draw: Footer ──────────────────────────────────────────────────────────
    private void drawFooter(Graphics2D g) {
        int W = Constants.SCREEN_WIDTH, H = Constants.SCREEN_HEIGHT;
        g.setFont(fontBody.deriveFont(17f));
        g.setColor(new Color(0x44, 0x33, 0x66));
        drawCenteredString(g, "CMSC 137  |  Holy Shot!  |  Use WASD + SPACE", W, H - 14, fontBody.deriveFont(17f));
    }

    // ── Draw: Status message ──────────────────────────────────────────────────
    private void drawStatus(Graphics2D g) {
        if (statusMessage == null) return;
        g.setFont(fontBody.deriveFont(20f));
        g.setColor(new Color(0, 0, 0, 140));
        drawCenteredString(g, statusMessage, Constants.SCREEN_WIDTH,
                           exitBtn.y + exitBtn.height + 34, fontBody.deriveFont(20f));
        g.setColor(statusColor);
        drawCenteredString(g, statusMessage, Constants.SCREEN_WIDTH,
                           exitBtn.y + exitBtn.height + 32, fontBody.deriveFont(20f));
    }

    // ── Draw: IP Prompt overlay ───────────────────────────────────────────────
    private void drawIpPrompt(Graphics2D g) {
        int W = Constants.SCREEN_WIDTH, H = Constants.SCREEN_HEIGHT;

        // Full-screen dim
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, 0, W, H);

        // Dialog box
        int bx = W/2 - 220, by = H/2 - 90, bw = 440, bh = 180;

        // Glow outline
        g.setColor(new Color(0x7B, 0x2F, 0xBE, 80));
        g.fillRoundRect(bx - 6, by - 6, bw + 12, bh + 12, 18, 18);

        // Body
        g.setColor(new Color(0x0D, 0x05, 0x1E, 240));
        g.fillRoundRect(bx, by, bw, bh, 12, 12);

        // Border
        g.setColor(PURPLE_GLOW);
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(bx, by, bw, bh, 12, 12);
        g.setStroke(new BasicStroke(1f));

        // Title bar accent
        g.setColor(new Color(0x7B, 0x2F, 0xBE, 90));
        g.fillRoundRect(bx, by, bw, 36, 12, 12);

        // Header text
        g.setFont(fontBtn.deriveFont(10f));
        g.setColor(GHOSTLY_WHITE);
        drawCenteredString(g, "ENTER SERVER IP ADDRESS", W, by + 23, fontBtn.deriveFont(10f));

        // IP input field background
        int fx = bx + 20, fy = by + 50, fw = bw - 40, fh = 44;
        g.setColor(new Color(0x05, 0x02, 0x10, 230));
        g.fillRoundRect(fx, fy, fw, fh, 6, 6);
        g.setColor(new Color(0xCC, 0x00, 0x00, 160));
        g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect(fx, fy, fw, fh, 6, 6);
        g.setStroke(new BasicStroke(1f));

        // IP text
        g.setFont(fontBody.deriveFont(26f));
        String display = joinIp.isEmpty() ? "" : joinIp;
        String cursor  = ipCursorOn ? "|" : "";
        g.setColor(TOXIC_GREEN);
        g.drawString(display + cursor, fx + 12, fy + 30);

        // Hint text
        g.setFont(fontBody.deriveFont(18f));
        g.setColor(new Color(0x66, 0x55, 0x88));
        drawCenteredString(g, "ENTER = connect     ESC = cancel", W, by + bh - 14, fontBody.deriveFont(18f));
    }

    // ── Input ─────────────────────────────────────────────────────────────────
    @Override
    public void handleMouseClicked(int x, int y) {
        if (enteringIp) return;
        if      (hostBtn.contains(x, y)) connectAndGoToLobby(Constants.DEFAULT_HOST, true);
        else if (joinBtn.contains(x, y)) { enteringIp = true; joinIp = ""; }
        else if (exitBtn.contains(x, y)) System.exit(0);
    }

    @Override
    public void handleMouseMoved(int x, int y) {
        if      (hostBtn.contains(x, y)) hoveredIndex = 0;
        else if (joinBtn.contains(x, y)) hoveredIndex = 1;
        else if (exitBtn.contains(x, y)) hoveredIndex = 2;
        else                             hoveredIndex = -1;
    }

    @Override
    public void handleKeyPressed(int keyCode) {
        if (!enteringIp) return;
        if (keyCode == KeyEvent.VK_ENTER) {
            String ip = joinIp.isEmpty() ? Constants.DEFAULT_HOST : joinIp;
            connectAndGoToLobby(ip, false);
            enteringIp = false;
        } else if (keyCode == KeyEvent.VK_ESCAPE) {
            enteringIp = false; joinIp = "";
        } else if (keyCode == KeyEvent.VK_BACK_SPACE && !joinIp.isEmpty()) {
            joinIp = joinIp.substring(0, joinIp.length() - 1);
        }
    }

    @Override
    public void handleKeyTyped(char c) {
        if (!enteringIp) return;
        if ((Character.isDigit(c) || c == '.') && joinIp.length() < 15) {
            joinIp += c;
        }
    }

    // ── Connection logic ──────────────────────────────────────────────────────
    private void connectAndGoToLobby(String ip, boolean isHost) {
        if (ip == null || ip.trim().isEmpty()) {
            statusMessage = "Please enter a valid IP address.";
            statusColor = BRIGHT_RED; return;
        }
        if (gameClient == null) {
            statusMessage = "No network client available.";
            statusColor = BRIGHT_RED; return;
        }
        statusMessage = "Connecting to " + ip + "...";
        statusColor = new Color(0xF5, 0xA6, 0x23);

        new Thread(() -> {
            boolean connected = gameClient.connectToServer(ip);
            if (!connected) {
                String reason = gameClient.getLastConnectionError();
                statusMessage = (reason != null) ? reason : "Connection failed.";
                statusColor = BRIGHT_RED; return;
            }
            if (gameState != null) gameClient.startListeningForServer(gameState);
            statusMessage = null;
            screenManager.setScreen(new LobbyScreen(screenManager, ip, isHost, gameClient));
        }).start();
    }

    // ── Helper: centered string ───────────────────────────────────────────────
    private void drawCenteredString(Graphics2D g, String text, int containerW, int y, Font font) {
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, containerW / 2 - fm.stringWidth(text) / 2, y);
    }
}
