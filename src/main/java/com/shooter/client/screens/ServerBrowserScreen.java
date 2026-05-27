package com.shooter.client.screens;

import com.shooter.client.GameClient;
import com.shooter.client.ScreenManager;
import com.shooter.client.ServerDiscovery;
import com.shooter.client.ServerEntry;
import com.shooter.shared.model.GameState;
import com.shooter.shared.util.Constants;

import java.awt.*;
import java.io.InputStream;
import java.util.List;

/**
 * ============================================================
 * FILE: ServerBrowserScreen.java
 * PACKAGE: client.screens
 * ============================================================
 *
 * Displays available servers discovered via UDP broadcast.
 * Players click an entry to join. A REFRESH button clears
 * the discovered list and begins a fresh scan.
 *
 * LIFECYCLE:
 *   onEnter()  → starts ServerDiscovery background thread
 *   update()   → polls discovery list every second
 *   render()   → draws server rows + buttons
 *   onExit()   → stops ServerDiscovery background thread
 * ============================================================
 */
public class ServerBrowserScreen implements Screen {

    // ── Palette (matches MainMenuScreen horror theme) ─────────────────────────
    private static final Color VOID_PURPLE   = new Color(0x1A, 0x0A, 0x2E);
    private static final Color TOXIC_GREEN   = new Color(0x39, 0xFF, 0x14);
    private static final Color BLOOD_RED     = new Color(0x8B, 0x00, 0x00);
    private static final Color BRIGHT_RED    = new Color(0xCC, 0x00, 0x00);
    private static final Color GHOSTLY_WHITE = new Color(0xE8, 0xE8, 0xF0);
    private static final Color DARK_BG       = new Color(0x0D, 0x0D, 0x1A);
    private static final Color PURPLE_GLOW   = new Color(0x7B, 0x2F, 0xBE);

    // ── Layout ────────────────────────────────────────────────────────────────
    private static final int PANEL_X = 80;
    private static final int PANEL_Y = 120;
    private static final int PANEL_W = Constants.SCREEN_WIDTH - 160;
    private static final int PANEL_H = Constants.SCREEN_HEIGHT - 220;
    private static final int ROW_H   = 64;
    private static final int ROW_PAD = 8;

    // ── Buttons ───────────────────────────────────────────────────────────────
    private static final int BTN_H = 44;
    private Rectangle backBtn;
    private Rectangle refreshBtn;
    private boolean   backHovered    = false;
    private boolean   refreshHovered = false;

    // ── Core references ───────────────────────────────────────────────────────
    private final ScreenManager screenManager;
    private final GameClient    gameClient;
    private final GameState     gameState;

    // ── Discovery ─────────────────────────────────────────────────────────────
    private ServerDiscovery   discovery;
    private Thread            discoveryThread;
    private List<ServerEntry> currentServers = java.util.Collections.emptyList();
    private int               hoveredRow     = -1;

    // ── Refresh animation ─────────────────────────────────────────────────────
    /** Counts down after a manual refresh — shows spinning indicator while > 0. */
    private int  refreshCooldown = 0;
    private static final int REFRESH_ANIM_TICKS = 40; // ~2 s at 20 ticks/s

    // ── General animation ─────────────────────────────────────────────────────
    private int  tick     = 0;
    private int  dotCount = 0;

    // ── Status ────────────────────────────────────────────────────────────────
    private String statusMessage = null;
    private Color  statusColor   = BRIGHT_RED;

    // ── Fonts ─────────────────────────────────────────────────────────────────
    private Font fontTitle, fontBtn, fontBody, fontSmall;

    // ── Constructor ───────────────────────────────────────────────────────────
    public ServerBrowserScreen(ScreenManager screenManager,
                               GameClient gameClient,
                               GameState gameState) {
        this.screenManager = screenManager;
        this.gameClient    = gameClient;
        this.gameState     = gameState;
        buildLayout();
        loadFonts();
    }

    private void buildLayout() {
        int btnY  = Constants.SCREEN_HEIGHT - 70;
        int backW = 140, refreshW = 180;
        backBtn    = new Rectangle(PANEL_X, btnY, backW, BTN_H);
        refreshBtn = new Rectangle(PANEL_X + backW + 16, btnY, refreshW, BTN_H);
    }

    private void loadFonts() {
        fontTitle = loadTtf("/assets/fonts/PressStart2P-Regular.ttf", 16f);
        fontBtn   = loadTtf("/assets/fonts/PressStart2P-Regular.ttf",  9f);
        fontBody  = loadTtf("/assets/fonts/VT323-Regular.ttf",         24f);
        fontSmall = loadTtf("/assets/fonts/VT323-Regular.ttf",         18f);
        if (fontTitle == null) fontTitle = new Font("Monospaced", Font.BOLD,  16);
        if (fontBtn   == null) fontBtn   = new Font("Monospaced", Font.BOLD,   9);
        if (fontBody  == null) fontBody  = new Font("Monospaced", Font.PLAIN, 24);
        if (fontSmall == null) fontSmall = new Font("Monospaced", Font.PLAIN, 18);
    }

    private Font loadTtf(String path, float size) {
        try {
            InputStream is = getClass().getResourceAsStream(path);
            if (is == null) return null;
            return Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(size);
        } catch (Exception e) { return null; }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void onEnter() {
        tick          = 0;
        dotCount      = 0;
        statusMessage = null;

        discovery       = new ServerDiscovery();
        discoveryThread = new Thread(discovery, "ServerDiscovery");
        discoveryThread.setDaemon(true);
        discoveryThread.start();
    }

    @Override
    public void onExit() {
        if (discovery != null) {
            discovery.stop();
            discovery = null;
        }
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Override
    public void update() {
        tick++;

        // Poll discovery list every second
        if (tick % 20 == 0 && discovery != null) {
            currentServers = discovery.getServers();
        }

        // Scanning dots (0→3, cycles every 30 ticks)
        if (tick % 30 == 0) {
            dotCount = (dotCount + 1) % 4;
        }

        // Count down refresh animation
        if (refreshCooldown > 0) {
            refreshCooldown--;
        }
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void render(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        drawBackground(g);
        drawHeader(g);
        drawServerPanel(g);
        drawButtons(g);
        drawStatus(g);
    }

    // ── Draw ──────────────────────────────────────────────────────────────────

    private void drawBackground(Graphics2D g) {
        int W = Constants.SCREEN_WIDTH, H = Constants.SCREEN_HEIGHT;
        g.setPaint(new GradientPaint(0, 0, DARK_BG, 0, H, VOID_PURPLE));
        g.fillRect(0, 0, W, H);
        g.setColor(new Color(0x7B, 0x2F, 0xBE, 15));
        for (int x = 0; x < W; x += 42) g.drawLine(x, 0, x, H);
        for (int y = 0; y < H; y += 42) g.drawLine(0, y, W, y);
        g.setPaint(new GradientPaint(0, 0, new Color(0x7B, 0x2F, 0xBE, 180),
                W, 0, new Color(0x8B, 0x00, 0x00, 180)));
        g.fillRect(0, 0, W, 4);
    }

    private void drawHeader(Graphics2D g) {
        int W = Constants.SCREEN_WIDTH;
        g.setFont(fontTitle);
        String title = "AVAILABLE SERVERS";
        FontMetrics fm = g.getFontMetrics();
        int tx = W / 2 - fm.stringWidth(title) / 2;
        g.setColor(new Color(0x7B, 0x2F, 0xBE, 60)); g.drawString(title, tx + 3, 78);
        g.setColor(new Color(0, 0, 0, 160));          g.drawString(title, tx + 2, 77);
        g.setColor(GHOSTLY_WHITE);                    g.drawString(title, tx,     75);

        // Sub-label: scanning / found / refreshing
        String sub;
        if (refreshCooldown > 0) {
            sub = "Refreshing" + ".".repeat(dotCount);
        } else if (currentServers.isEmpty()) {
            sub = "Scanning for servers" + ".".repeat(dotCount);
        } else {
            sub = currentServers.size() + " server(s) found";
        }
        g.setFont(fontSmall);
        g.setColor(new Color(0x66, 0x55, 0x88));
        FontMetrics sm = g.getFontMetrics();
        g.drawString(sub, W / 2 - sm.stringWidth(sub) / 2, 100);
    }

    private void drawServerPanel(Graphics2D g) {
        g.setColor(new Color(0x07, 0x03, 0x15, 220));
        g.fillRoundRect(PANEL_X, PANEL_Y, PANEL_W, PANEL_H, 12, 12);
        g.setColor(new Color(0x7B, 0x2F, 0xBE, 80));
        g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect(PANEL_X, PANEL_Y, PANEL_W, PANEL_H, 12, 12);
        g.setStroke(new BasicStroke(1f));

        if (currentServers.isEmpty()) { drawNoServers(g); return; }

        int maxVisible = (PANEL_H - ROW_PAD * 2) / (ROW_H + ROW_PAD);
        int count      = Math.min(currentServers.size(), maxVisible);
        for (int i = 0; i < count; i++) {
            int rx = PANEL_X + ROW_PAD;
            int ry = PANEL_Y + ROW_PAD + i * (ROW_H + ROW_PAD);
            int rw = PANEL_W - ROW_PAD * 2;
            drawServerRow(g, currentServers.get(i), rx, ry, rw, ROW_H, i == hoveredRow);
        }
    }

    private void drawNoServers(Graphics2D g) {
        int cx = PANEL_X + PANEL_W / 2;
        int cy = PANEL_Y + PANEL_H / 2;
        g.setFont(fontBody.deriveFont(52f));
        g.setColor(new Color(0x44, 0x22, 0x66, 120));
        FontMetrics fm = g.getFontMetrics();
        String skull = "☠";
        g.drawString(skull, cx - fm.stringWidth(skull) / 2, cy - 10);
        String[] lines = {"No servers found.",
                          "Make sure a host is running",
                          "on the same network."};
        g.setFont(fontBody.deriveFont(20f));
        for (int i = 0; i < lines.length; i++) {
            g.setColor(i == 0 ? new Color(0x88, 0x77, 0x99) : new Color(0x55, 0x44, 0x66));
            FontMetrics lm = g.getFontMetrics();
            g.drawString(lines[i], cx - lm.stringWidth(lines[i]) / 2, cy + 28 + i * 26);
        }
    }

    private void drawServerRow(Graphics2D g, ServerEntry entry,
                               int x, int y, int w, int h, boolean hovered) {
        g.setColor(hovered ? new Color(0x3A, 0x14, 0x5E, 200) : new Color(0x12, 0x07, 0x25, 180));
        g.fillRoundRect(x, y, w, h, 8, 8);

        Color accent = entry.isFull() ? BLOOD_RED : TOXIC_GREEN;
        g.setColor(accent);
        g.fillRoundRect(x, y, 4, h, 4, 4);

        if (hovered && !entry.isFull()) {
            g.setColor(new Color(0x39, 0xFF, 0x14, 60));
            g.setStroke(new BasicStroke(2f));
            g.drawRoundRect(x, y, w, h, 8, 8);
            g.setStroke(new BasicStroke(1f));
        }

        g.setFont(fontBody.deriveFont(22f));
        g.setColor(hovered ? GHOSTLY_WHITE : new Color(0xCC, 0xBB, 0xDD));
        g.drawString(entry.name, x + 16, y + 26);

        g.setFont(fontSmall);
        g.setColor(new Color(0x77, 0x66, 0x88));
        g.drawString(entry.getHostAddress(), x + 16, y + 48);

        String badge = entry.playerCount + " / " + entry.maxPlayers;
        g.setFont(fontBtn.deriveFont(10f));
        FontMetrics fm = g.getFontMetrics();
        int bx = x + w - fm.stringWidth(badge) - 20;
        int by = y + h / 2 + fm.getAscent() / 2 - 4;
        g.setColor(entry.isFull() ? new Color(0xCC, 0x00, 0x00, 200) : new Color(0x20, 0xAA, 0x30, 200));
        g.fillRoundRect(bx - 8, by - fm.getAscent() - 4, fm.stringWidth(badge) + 16, fm.getHeight() + 8, 6, 6);
        g.setColor(GHOSTLY_WHITE);
        g.drawString(badge, bx, by);

        if (entry.isFull()) {
            g.setFont(fontSmall.deriveFont(Font.BOLD, 16f));
            g.setColor(BRIGHT_RED);
            g.drawString("FULL", x + w - 60, y + 26);
        }
    }

    private void drawButtons(Graphics2D g) {
        drawStyledButton(g, backBtn,    "< BACK",   BLOOD_RED,   backHovered);

        // Refresh button: show spinning dots while cooldown active
        boolean refreshing = refreshCooldown > 0;
        String refreshLabel = refreshing
                ? "REFRESHING" + ".".repeat(dotCount)
                : "↺ REFRESH";
        drawStyledButton(g, refreshBtn, refreshLabel, PURPLE_GLOW, refreshHovered || refreshing);
    }

    private void drawStyledButton(Graphics2D g, Rectangle r, String label,
                                  Color accent, boolean hovered) {
        int x = r.x, y = r.y, w = r.width, h = r.height;
        if (hovered) {
            g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 50));
            g.fillRoundRect(x - 4, y - 4, w + 8, h + 8, 14, 14);
        }
        g.setColor(hovered
                ? new Color(accent.getRed() / 4, accent.getGreen() / 4, accent.getBlue() / 4, 230)
                : new Color(0x0D, 0x05, 0x1E, 200));
        g.fillRoundRect(x, y, w, h, 10, 10);
        g.setColor(hovered ? accent : new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 120));
        g.setStroke(new BasicStroke(hovered ? 2f : 1f));
        g.drawRoundRect(x, y, w, h, 10, 10);
        g.setStroke(new BasicStroke(1f));

        g.setFont(fontBtn);
        FontMetrics fm = g.getFontMetrics();
        g.setColor(hovered ? GHOSTLY_WHITE : new Color(
                (accent.getRed()   + GHOSTLY_WHITE.getRed())   / 2,
                (accent.getGreen() + GHOSTLY_WHITE.getGreen()) / 2,
                (accent.getBlue()  + GHOSTLY_WHITE.getBlue())  / 2));
        g.drawString(label, x + w / 2 - fm.stringWidth(label) / 2,
                y + h / 2 + fm.getAscent() / 2 - 2);
    }

    private void drawStatus(Graphics2D g) {
        if (statusMessage == null) return;
        int W = Constants.SCREEN_WIDTH;
        g.setFont(fontBody.deriveFont(20f));
        g.setColor(new Color(0, 0, 0, 140));
        g.drawString(statusMessage, W / 2 - g.getFontMetrics().stringWidth(statusMessage) / 2 + 1,
                backBtn.y - 10 + 1);
        g.setColor(statusColor);
        g.drawString(statusMessage, W / 2 - g.getFontMetrics().stringWidth(statusMessage) / 2,
                backBtn.y - 10);
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    @Override
    public void handleMouseClicked(int x, int y) {
        // Server row click
        if (!currentServers.isEmpty()) {
            int maxVisible = (PANEL_H - ROW_PAD * 2) / (ROW_H + ROW_PAD);
            int count = Math.min(currentServers.size(), maxVisible);
            for (int i = 0; i < count; i++) {
                int ry = PANEL_Y + ROW_PAD + i * (ROW_H + ROW_PAD);
                int rx = PANEL_X + ROW_PAD;
                int rw = PANEL_W - ROW_PAD * 2;
                if (x >= rx && x <= rx + rw && y >= ry && y <= ry + ROW_H) {
                    ServerEntry entry = currentServers.get(i);
                    if (!entry.isFull()) {
                        connectAndGoToLobby(entry.getHostAddress(), false);
                    } else {
                        statusMessage = "That server is full!";
                        statusColor   = BRIGHT_RED;
                    }
                    return;
                }
            }
        }

        if (backBtn.contains(x, y)) {
            screenManager.setScreen(new MainMenuScreen(screenManager, gameClient, gameState));
        } else if (refreshBtn.contains(x, y) && refreshCooldown == 0) {
            doRefresh();
        }
    }

    @Override
    public void handleMouseMoved(int x, int y) {
        backHovered    = backBtn.contains(x, y);
        refreshHovered = refreshBtn.contains(x, y);

        hoveredRow = -1;
        if (!currentServers.isEmpty()) {
            int maxVisible = (PANEL_H - ROW_PAD * 2) / (ROW_H + ROW_PAD);
            int count = Math.min(currentServers.size(), maxVisible);
            for (int i = 0; i < count; i++) {
                int ry = PANEL_Y + ROW_PAD + i * (ROW_H + ROW_PAD);
                int rx = PANEL_X + ROW_PAD;
                int rw = PANEL_W - ROW_PAD * 2;
                if (x >= rx && x <= rx + rw && y >= ry && y <= ry + ROW_H) {
                    hoveredRow = i;
                    break;
                }
            }
        }
    }

    @Override public void handleKeyPressed(int keyCode) {}
    @Override public void handleKeyTyped(char c) {}

    // ── Refresh ───────────────────────────────────────────────────────────────

    private void doRefresh() {
        if (discovery != null) {
            discovery.clearServers();
        }
        currentServers  = java.util.Collections.emptyList();
        refreshCooldown = REFRESH_ANIM_TICKS;
        statusMessage   = null;
    }

    // ── Connection ────────────────────────────────────────────────────────────

    private void connectAndGoToLobby(String ip, boolean isHost) {
        if (gameClient == null) {
            statusMessage = "No network client available.";
            statusColor   = BRIGHT_RED;
            return;
        }
        statusMessage = "Connecting to " + ip + "...";
        statusColor   = new Color(0xF5, 0xA6, 0x23);

        new Thread(() -> {
            boolean connected = gameClient.connectToServer(ip);
            if (!connected) {
                String reason = gameClient.getLastConnectionError();
                statusMessage = (reason != null) ? reason : "Connection failed.";
                statusColor   = BRIGHT_RED;
                return;
            }
            if (gameState != null) gameClient.startListeningForServer(gameState);
            statusMessage = null;
            screenManager.setScreen(new LobbyScreen(screenManager, ip, isHost, gameClient));
        }).start();
    }
}
