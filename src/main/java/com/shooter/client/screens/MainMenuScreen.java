package com.shooter.client.screens;

import com.shooter.client.GameClient;
import com.shooter.client.ScreenManager;
import com.shooter.shared.model.GameState;
import com.shooter.shared.model.Player;
import com.shooter.shared.util.Constants;

import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * ============================================================
 * FILE: MainMenuScreen.java
 * PACKAGE: client.screens
 * OWNER: Geastin (Member C - Screens / Systems / Assets)
 * ============================================================
 *
 * The first screen the player sees.
 * Buttons: HOST GAME | JOIN GAME | EXIT
 *
 * HOST GAME → switches to LobbyScreen (connecting to localhost)
 * JOIN GAME → prompts for IP, then switches to LobbyScreen with that IP
 * EXIT → closes the application
 *
 * LAYER RULE: No networking here. No game logic here.
 * Only UI drawing and screen switching.
 * ============================================================
 */
public class MainMenuScreen implements Screen {

    private final ScreenManager screenManager;
    private final GameClient gameClient;   // may be null for UI-only testing
    private final GameState gameState;     // may be null for UI-only testing

    // ── Connection status message ─────────────────────────────────────────────
    private String statusMessage = null;
    private Color statusColor = new Color(0xE05C5C);

    // ── Button hit areas (for click detection) ───────────────────────────────
    private final Rectangle hostBtn;
    private final Rectangle joinBtn;
    private final Rectangle exitBtn;

    // ── Hover state (-1 = none, 0 = host, 1 = join, 2 = exit) ───────────────
    private int hoveredIndex = -1;

    // ── Animation ────────────────────────────────────────────────────────────
    private int tick = 0;

    // ── IP entry mode (activated when JOIN is clicked) ───────────────────────
    private boolean enteringIp = false;
    private String joinIp = "";

    // ── Player color palette ─────────────────────────────────────────────────
    private static final Color[] PLAYER_COLORS = {
            new Color(0x4A90D9), // P1 blue
            new Color(0xE05C5C), // P2 red
            new Color(0x50E878), // P3 green
            new Color(0xF5A623) // P4 yellow
    };

    public MainMenuScreen(ScreenManager screenManager) {
        this(screenManager, null, null);
    }

    /**
     * Full constructor used by GameClient.main().
     * GameClient is needed to connect to the server when Host/Join is clicked.
     */
    public MainMenuScreen(ScreenManager screenManager, GameClient gameClient, GameState gameState) {
        this.screenManager = screenManager;
        this.gameClient = gameClient;
        this.gameState = gameState;

        // Centre buttons horizontally
        int bW = 240, bH = 48;
        int bX = Constants.SCREEN_WIDTH / 2 - bW / 2;
        hostBtn = new Rectangle(bX, 370, bW, bH);
        joinBtn = new Rectangle(bX, 435, bW, bH);
        exitBtn = new Rectangle(bX, 500, bW, bH);
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void onEnter() {
        tick = 0;
        enteringIp = false;
        joinIp = "";
        System.out.println("[MainMenuScreen] Showing main menu.");
    }

    @Override
    public void onExit() {
        System.out.println("[MainMenuScreen] Leaving main menu.");
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Override
    public void update() {
        tick++; // drives title glow animation
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void render(Graphics2D g) {
        // Enable smooth rendering for text
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        drawBackground(g);
        drawTitle(g);
        drawButtons(g);
        drawColorBadges(g);
        drawFooter(g);

        // Overlay if player is typing an IP address
        if (enteringIp) {
            drawIpPrompt(g);
        }

        // Connection status feedback (shown below buttons)
        if (statusMessage != null) {
            g.setFont(new Font("Monospaced", Font.BOLD, 13));
            g.setColor(statusColor);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(statusMessage,
                    Constants.SCREEN_WIDTH / 2 - fm.stringWidth(statusMessage) / 2,
                    exitBtn.y + exitBtn.height + 30);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    /** Dark gradient background with subtle vignette feel. */
    private void drawBackground(Graphics2D g) {
        GradientPaint bg = new GradientPaint(
                0, 0, new Color(0x0D0D1A),
                0, Constants.SCREEN_HEIGHT, new Color(0x1A1A3E));
        g.setPaint(bg);
        g.fillRect(0, 0, Constants.SCREEN_WIDTH, Constants.SCREEN_HEIGHT);
    }

    /** Animated glowing title "HOLY SHOT!" */
    private void drawTitle(Graphics2D g) {
        // Glow intensity pulses between 0.55 and 1.0
        float glow = 0.55f + 0.45f * (float) Math.abs(Math.sin(tick * 0.04));

        // Drop shadow
        g.setFont(new Font("Monospaced", Font.BOLD, 72));
        g.setColor(new Color(0, 0, 0, 100));
        FontMetrics fm = g.getFontMetrics();
        int titleX = Constants.SCREEN_WIDTH / 2 - fm.stringWidth("HOLY SHOT!") / 2;
        g.drawString("HOLY SHOT!", titleX + 4, 220 + 4);

        // Main title — gold/orange glow
        g.setColor(new Color(
                (int) (255 * glow),
                (int) (185 * glow),
                (int) (30 * glow)));
        g.drawString("HOLY SHOT!", titleX, 220);

        // Subtitle
        g.setFont(new Font("Monospaced", Font.PLAIN, 15));
        g.setColor(new Color(0x8899BB));
        String sub = "Spirit-Cleansing 4-Player Co-op Shooter";
        g.drawString(sub,
                Constants.SCREEN_WIDTH / 2 - g.getFontMetrics().stringWidth(sub) / 2,
                248);

        // Thin separator line under subtitle
        g.setColor(new Color(0x334466));
        g.setStroke(new BasicStroke(1));
        g.drawLine(Constants.SCREEN_WIDTH / 2 - 160, 264,
                Constants.SCREEN_WIDTH / 2 + 160, 264);
    }

    /** Draw all three menu buttons. */
    private void drawButtons(Graphics2D g) {
        // Join button label changes when entering IP
        String joinLabel = enteringIp
                ? "IP: " + joinIp + "|"
                : "JOIN GAME";

        drawOneButton(g, hostBtn, "HOST GAME", 0);
        drawOneButton(g, joinBtn, joinLabel, 1);
        drawOneButton(g, exitBtn, "EXIT", 2);
    }

    private void drawOneButton(Graphics2D g, Rectangle btn, String label, int index) {
        boolean hovered = (hoveredIndex == index);

        // Background fill
        g.setColor(hovered ? new Color(0xF5A623) : new Color(0x1E2050));
        g.fillRoundRect(btn.x, btn.y, btn.width, btn.height, 12, 12);

        // Border
        g.setColor(hovered ? new Color(0xFFD700) : new Color(0x4455AA));
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(btn.x, btn.y, btn.width, btn.height, 12, 12);

        // Label text
        g.setFont(new Font("Monospaced", Font.BOLD, 17));
        g.setColor(hovered ? Color.BLACK : Color.WHITE);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(label,
                btn.x + btn.width / 2 - fm.stringWidth(label) / 2,
                btn.y + btn.height / 2 + fm.getAscent() / 2 - 3);
    }

    /** Small colored P1–P4 circles to show the 4-player theme. */
    private void drawColorBadges(Graphics2D g) {
        int y = Constants.SCREEN_HEIGHT - 80;
        int startX = Constants.SCREEN_WIDTH / 2 - 70;
        g.setFont(new Font("Monospaced", Font.BOLD, 11));
        for (int i = 0; i < 4; i++) {
            int cx = startX + i * 46;
            g.setColor(PLAYER_COLORS[i]);
            g.fillOval(cx, y, 18, 18);
            g.setColor(Color.WHITE);
            g.drawString("P" + (i + 1), cx + 3, y + 30);
        }
    }

    private void drawFooter(Graphics2D g) {
        g.setFont(new Font("Monospaced", Font.PLAIN, 11));
        g.setColor(new Color(0x334455));
        String f = "CMSC 137 | Holy Shot! | Use WASD + SPACE";
        g.drawString(f,
                Constants.SCREEN_WIDTH / 2 - g.getFontMetrics().stringWidth(f) / 2,
                Constants.SCREEN_HEIGHT - 12);
    }

    /** Dark overlay + IP input prompt when joining. */
    private void drawIpPrompt(Graphics2D g) {
        // Dim the background
        g.setColor(new Color(0, 0, 0, 160));
        g.fillRect(0, 0, Constants.SCREEN_WIDTH, Constants.SCREEN_HEIGHT);

        // Prompt box
        int bx = Constants.SCREEN_WIDTH / 2 - 200;
        int by = Constants.SCREEN_HEIGHT / 2 - 70;
        g.setColor(new Color(0x1E2050));
        g.fillRoundRect(bx, by, 400, 140, 14, 14);
        g.setColor(new Color(0x4455AA));
        g.setStroke(new BasicStroke(2));
        g.drawRoundRect(bx, by, 400, 140, 14, 14);

        g.setFont(new Font("Monospaced", Font.BOLD, 16));
        g.setColor(Color.WHITE);
        g.drawString("Enter Server IP Address:", bx + 20, by + 36);

        g.setFont(new Font("Monospaced", Font.PLAIN, 20));
        g.setColor(new Color(0xF5A623));
        g.drawString(joinIp.isEmpty() ? "_" : joinIp + "|", bx + 20, by + 76);

        g.setFont(new Font("Monospaced", Font.PLAIN, 12));
        g.setColor(new Color(0x8899BB));
        g.drawString("ENTER to connect  |  ESC to cancel", bx + 20, by + 116);
    }

    // ── Input handlers ────────────────────────────────────────────────────────

    @Override
    public void handleMouseClicked(int x, int y) {
        if (enteringIp)
            return; // keyboard mode; ignore mouse clicks

        if (hostBtn.contains(x, y)) {
            System.out.println("[MainMenuScreen] Hosting game on localhost.");
            connectAndGoToLobby(Constants.DEFAULT_HOST, true);

        } else if (joinBtn.contains(x, y)) {
            System.out.println("[MainMenuScreen] Join clicked — enter IP.");
            enteringIp = true;
            joinIp = "";

        } else if (exitBtn.contains(x, y)) {
            System.out.println("[MainMenuScreen] Exiting.");
            System.exit(0);
        }
    }

    @Override
    public void handleMouseMoved(int x, int y) {
        if (hostBtn.contains(x, y))
            hoveredIndex = 0;
        else if (joinBtn.contains(x, y))
            hoveredIndex = 1;
        else if (exitBtn.contains(x, y))
            hoveredIndex = 2;
        else
            hoveredIndex = -1;
    }

    @Override
    public void handleKeyPressed(int keyCode) {
        if (!enteringIp)
            return;

        if (keyCode == KeyEvent.VK_ENTER) {
            String ip = joinIp.isEmpty() ? Constants.DEFAULT_HOST : joinIp;
            System.out.println("[MainMenuScreen] Joining at: " + ip);
            connectAndGoToLobby(ip, false);
            enteringIp = false;

        } else if (keyCode == KeyEvent.VK_ESCAPE) {
            enteringIp = false;
            joinIp = "";

        } else if (keyCode == KeyEvent.VK_BACK_SPACE) {
            if (!joinIp.isEmpty()) {
                joinIp = joinIp.substring(0, joinIp.length() - 1);
            }

        } else {
            // Accept digits and dots (valid IP characters)
            char c = (char) keyCode;
            if ((Character.isDigit(c) || c == '.') && joinIp.length() < 15) {
                joinIp += c;
            }
        }
    }

    // ── Connection logic ─────────────────────────────────────────────────────

    /**
     * Connects to the server at the given IP address using GameClient,
     * then transitions to LobbyScreen on success.
     *
     * @param ip      server IP address
     * @param isHost  true if this player is hosting (started the server)
     */
    private void connectAndGoToLobby(String ip, boolean isHost) {
        // If no GameClient was provided, fall back to the old screen-only transition
        if (gameClient == null) {
            screenManager.setScreen(new LobbyScreen(screenManager, ip, isHost));
            return;
        }

        statusMessage = "Connecting to " + ip + "...";
        statusColor = new Color(0xF5A623); // orange while connecting

        // Try to connect (this blocks briefly)
        boolean connected = gameClient.connectToServer(ip);

        if (!connected) {
            statusMessage = "Connection failed. Is the server running?";
            statusColor = new Color(0xE05C5C); // red on failure
            System.out.println("[MainMenuScreen] Connection to " + ip + " failed.");
            return;
        }

        // Start listening for server messages (LOBBY_STATE, GAME_STATE, etc.)
        if (gameState != null) {
            gameClient.startListeningForServer(gameState);
        }

        statusMessage = null; // clear status
        System.out.println("[MainMenuScreen] Connected! Going to lobby.");

        // Transition to the Lobby screen
        screenManager.setScreen(new LobbyScreen(screenManager, ip, isHost, gameClient));
    }
}
