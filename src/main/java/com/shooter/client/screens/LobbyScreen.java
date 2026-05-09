package com.shooter.client.screens;

import com.shooter.client.ScreenManager;
import com.shooter.shared.util.Constants;

import java.awt.*;

/**
 * ============================================================
 * FILE: LobbyScreen.java
 * PACKAGE: client.screens
 * OWNER: Christel (Systems Lead)
 * ============================================================
 *
 * DAY 1: Stub — shows player slots as "Waiting..." placeholders.
 * Switched to from MainMenuScreen.
 *
 * DAY 2: Wire in LobbyState received from server to fill slots
 * with real player names and ready status. Add ready
 * toggle button and host-only Start button.
 *
 * LAYER RULE: No game logic. No direct server calls.
 * Networking is handled by GameClient (Day 2).
 * ============================================================
 */
public class LobbyScreen implements Screen {

    private final ScreenManager screenManager;

    /** IP address of the server this lobby is connecting to. */
    private final String serverIp;

    /** True if this client is the host (connected to localhost). */
    private final boolean isHost;

    // ── Lobby data (populated from LobbyState in Day 2) ─────────────────────
    private String[] playerNames = new String[4]; // null = empty slot
    private boolean[] readyFlags = new boolean[4];

    // ── Back button ──────────────────────────────────────────────────────────
    private final Rectangle backBtn;
    private boolean backHovered = false;

    // ── Player color palette (matches player sprite colors) ──────────────────
    private static final Color[] PLAYER_COLORS = {
            new Color(0x4A90D9), // P1 blue
            new Color(0xE05C5C), // P2 red
            new Color(0x50E878), // P3 green
            new Color(0xF5A623) // P4 yellow/orange
    };

    /**
     * @param screenManager The ScreenManager to switch screens with.
     * @param serverIp      The server's IP address (shown in the lobby header).
     * @param isHost        True if this player is hosting (connected to localhost).
     */
    public LobbyScreen(ScreenManager screenManager, String serverIp, boolean isHost) {
        this.screenManager = screenManager;
        this.serverIp = serverIp;
        this.isHost = isHost;

        backBtn = new Rectangle(30, Constants.SCREEN_HEIGHT - 70, 140, 42);
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void onEnter() {
        System.out.println("[LobbyScreen] Entered. Server: " + serverIp
                + "  isHost=" + isHost);
        // TODO (Day 2): Open TCP connection to server here via GameClient.
    }

    @Override
    public void onExit() {
        System.out.println("[LobbyScreen] Exiting lobby.");
        // TODO (Day 2): Disconnect from server if leaving lobby.
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Override
    public void update() {
        // TODO (Day 2): Poll for incoming LobbyState messages and update
        // playerNames[] and readyFlags[] accordingly.
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void render(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        drawBackground(g);
        drawHeader(g);
        drawPlayerSlots(g);
        drawBackButton(g);
        drawStatusBar(g);
    }

    // ─────────────────────────────────────────────────────────────────────────

    private void drawBackground(Graphics2D g) {
        GradientPaint bg = new GradientPaint(
                0, 0, new Color(0x0D0D1A),
                0, Constants.SCREEN_HEIGHT, new Color(0x1A1A3E));
        g.setPaint(bg);
        g.fillRect(0, 0, Constants.SCREEN_WIDTH, Constants.SCREEN_HEIGHT);
    }

    private void drawHeader(Graphics2D g) {
        // Title
        g.setFont(new Font("Monospaced", Font.BOLD, 42));
        g.setColor(new Color(0xF5A623));
        String title = isHost ? "LOBBY  [HOST]" : "LOBBY";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(title, Constants.SCREEN_WIDTH / 2 - fm.stringWidth(title) / 2, 90);

        // Server address
        g.setFont(new Font("Monospaced", Font.PLAIN, 13));
        g.setColor(new Color(0x6677AA));
        String addr = "Server: " + serverIp + ":" + Constants.SERVER_PORT;
        g.drawString(addr,
                Constants.SCREEN_WIDTH / 2 - g.getFontMetrics().stringWidth(addr) / 2,
                118);

        // Separator
        g.setColor(new Color(0x334466));
        g.setStroke(new BasicStroke(1));
        g.drawLine(Constants.SCREEN_WIDTH / 2 - 200, 132,
                Constants.SCREEN_WIDTH / 2 + 200, 132);
    }

    /** Draw all 4 player slots. Uses stub data until Day 2 wires in LobbyState. */
    private void drawPlayerSlots(Graphics2D g) {
        int slotW = 460, slotH = 72;
        int slotX = Constants.SCREEN_WIDTH / 2 - slotW / 2;

        for (int i = 0; i < 4; i++) {
            int slotY = 160 + i * 96;
            String name = playerNames[i]; // null if empty
            boolean ready = readyFlags[i];

            drawOneSlot(g, slotX, slotY, slotW, slotH, i, name, ready);
        }
    }

    private void drawOneSlot(Graphics2D g,
            int x, int y, int w, int h,
            int index, String name, boolean ready) {
        boolean occupied = (name != null);

        // Slot background
        g.setColor(occupied ? new Color(0x1E2050) : new Color(0x111130));
        g.fillRoundRect(x, y, w, h, 10, 10);

        // Border — green if ready, dim otherwise
        g.setColor(ready ? new Color(0x50E878) : new Color(0x334466));
        g.setStroke(new BasicStroke(ready ? 2.5f : 1.5f));
        g.drawRoundRect(x, y, w, h, 10, 10);

        // Colored P# badge
        g.setColor(PLAYER_COLORS[index]);
        g.fillOval(x + 14, y + h / 2 - 14, 28, 28);
        g.setFont(new Font("Monospaced", Font.BOLD, 13));
        g.setColor(Color.WHITE);
        g.drawString("P" + (index + 1), x + 19, y + h / 2 + 5);

        // Player name or "Waiting..."
        g.setFont(new Font("Monospaced", Font.BOLD, 16));
        g.setColor(occupied ? Color.WHITE : new Color(0x445566));
        g.drawString(occupied ? name : "Waiting...", x + 58, y + h / 2 + 6);

        // READY badge (right side)
        if (ready) {
            g.setFont(new Font("Monospaced", Font.BOLD, 13));
            g.setColor(new Color(0x50E878));
            g.drawString("✓ READY", x + w - 100, y + h / 2 + 6);
        }
    }

    private void drawBackButton(Graphics2D g) {
        g.setColor(backHovered ? new Color(0xE05C5C) : new Color(0x2A1A1A));
        g.fillRoundRect(backBtn.x, backBtn.y, backBtn.width, backBtn.height, 8, 8);
        g.setColor(backHovered ? new Color(0xFF8888) : new Color(0x663333));
        g.setStroke(new BasicStroke(2));
        g.drawRoundRect(backBtn.x, backBtn.y, backBtn.width, backBtn.height, 8, 8);

        g.setFont(new Font("Monospaced", Font.BOLD, 14));
        g.setColor(Color.WHITE);
        g.drawString("← BACK", backBtn.x + 22, backBtn.y + 27);
    }

    private void drawStatusBar(Graphics2D g) {
        // Status message at the bottom
        g.setFont(new Font("Monospaced", Font.ITALIC, 12));
        g.setColor(new Color(0x556677));
        String status = isHost
                ? "Waiting for players to join and ready up..."
                : "Waiting for host to start the game...";
        g.drawString(status,
                Constants.SCREEN_WIDTH / 2 - g.getFontMetrics().stringWidth(status) / 2,
                Constants.SCREEN_HEIGHT - 18);
    }

    // ── Input handlers ────────────────────────────────────────────────────────

    @Override
    public void handleMouseClicked(int x, int y) {
        if (backBtn.contains(x, y)) {
            System.out.println("[LobbyScreen] Back to main menu.");
            screenManager.setScreen(new MainMenuScreen(screenManager));
        }
        // TODO (Day 2): Handle Ready toggle button click.
        // Handle Start button click (host only).
    }

    @Override
    public void handleMouseMoved(int x, int y) {
        backHovered = backBtn.contains(x, y);
    }

    // ── Day 2 wiring helpers (Christel will call these when LobbyState arrives) ─

    /**
     * Updates the displayed player list from a received LobbyState.
     * Call this in update() once networking is wired (Day 2).
     *
     * @param names      Array of 4 player names; null entry = empty slot.
     * @param readyFlags Array of 4 ready booleans.
     */
    public void updateFromLobbyState(String[] names, boolean[] readyFlags) {
        this.playerNames = names;
        this.readyFlags = readyFlags;
    }
}
