package com.shooter.client.screens;

import com.shooter.client.GameClient;
import com.shooter.client.ScreenManager;
import com.shooter.network.LobbyState;
import com.shooter.shared.util.Constants;

import javax.swing.SwingUtilities;
import java.awt.*;

/**
 * ============================================================
 * FILE: LobbyScreen.java
 * PACKAGE: client.screens
 * OWNER: Geastin (Member C - Screens / Systems / Assets)
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
    private final GameClient gameClient;

    /** IP address of the server this lobby is connecting to. */
    private final String serverIp;

    /** True if this client is the host (connected to localhost). */
    private final boolean isHost;

    // ── Lobby data (populated from LobbyState in Day 2) ─────────────────────
    private String[] playerNames = new String[4]; // null = empty slot
    private boolean[] readyFlags = new boolean[4];
    private int localPlayerId = 0;
    private boolean localReady = false;

    // ── Lobby buttons ────────────────────────────────────────────────────────
    private final Rectangle backBtn;
    private final Rectangle readyBtn;
    private final Rectangle startBtn;
    private boolean backHovered = false;
    private boolean readyHovered = false;
    private boolean startHovered = false;

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
        this(screenManager, serverIp, isHost, null);
    }

    /**
     * @param screenManager The ScreenManager to switch screens with.
     * @param serverIp      The server's IP address (shown in the lobby header).
     * @param isHost        True if this player is hosting (connected to localhost).
     * @param gameClient    Client networking helper; may be null for UI-only testing.
     */
    public LobbyScreen(ScreenManager screenManager, String serverIp, boolean isHost, GameClient gameClient) {
        this.screenManager = screenManager;
        this.gameClient = gameClient;
        this.serverIp = serverIp;
        this.isHost = isHost;

        backBtn = new Rectangle(30, Constants.SCREEN_HEIGHT - 70, 140, 42);
        readyBtn = new Rectangle(Constants.SCREEN_WIDTH / 2 - 100, Constants.SCREEN_HEIGHT - 78, 200, 46);
        startBtn = new Rectangle(Constants.SCREEN_WIDTH - 190, Constants.SCREEN_HEIGHT - 70, 160, 42);
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void onEnter() {
        System.out.println("[LobbyScreen] Entered. Server: " + serverIp
                + "  isHost=" + isHost);

        // Temporary local display data until real LobbyState packets arrive.
        // This lets the lobby screen show a connected player immediately.
        if (playerNames[localPlayerId] == null) {
            playerNames[localPlayerId] = isHost ? "Host Player" : "Player";
        }

        if (gameClient != null) {
            setLocalPlayerId(gameClient.getMyPlayerId());
            gameClient.setLobbyStateListener(this::applyLobbyStateOnUiThread);
        }
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
        drawLobbyButtons(g);
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
        } else if (occupied) {
            g.setFont(new Font("Monospaced", Font.BOLD, 13));
            g.setColor(new Color(0x8899BB));
            g.drawString("NOT READY", x + w - 115, y + h / 2 + 6);
        }
    }

    private void drawLobbyButtons(Graphics2D g) {
        drawBackButton(g);
        drawReadyButton(g);

        if (isHost) {
            drawStartButton(g);
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

    private void drawReadyButton(Graphics2D g) {
        Color fill = localReady ? new Color(0x245C3A) : new Color(0x1E2050);
        Color border = localReady ? new Color(0x50E878) : new Color(0x4455AA);

        if (readyHovered) {
            fill = localReady ? new Color(0x2F744A) : new Color(0x2A2D70);
        }

        g.setColor(fill);
        g.fillRoundRect(readyBtn.x, readyBtn.y, readyBtn.width, readyBtn.height, 8, 8);
        g.setColor(border);
        g.setStroke(new BasicStroke(2));
        g.drawRoundRect(readyBtn.x, readyBtn.y, readyBtn.width, readyBtn.height, 8, 8);

        String label = localReady ? "READY" : "READY?";
        g.setFont(new Font("Monospaced", Font.BOLD, 15));
        g.setColor(Color.WHITE);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(label,
                readyBtn.x + readyBtn.width / 2 - fm.stringWidth(label) / 2,
                readyBtn.y + readyBtn.height / 2 + fm.getAscent() / 2 - 3);
    }

    private void drawStartButton(Graphics2D g) {
        boolean canStart = canHostStart();

        Color fill = canStart ? new Color(0x5C4318) : new Color(0x222238);
        Color border = canStart ? new Color(0xF5A623) : new Color(0x444455);

        if (canStart && startHovered) {
            fill = new Color(0x7A5920);
        }

        g.setColor(fill);
        g.fillRoundRect(startBtn.x, startBtn.y, startBtn.width, startBtn.height, 8, 8);
        g.setColor(border);
        g.setStroke(new BasicStroke(2));
        g.drawRoundRect(startBtn.x, startBtn.y, startBtn.width, startBtn.height, 8, 8);

        g.setFont(new Font("Monospaced", Font.BOLD, 14));
        g.setColor(canStart ? Color.WHITE : new Color(0x777788));
        String label = "START";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(label,
                startBtn.x + startBtn.width / 2 - fm.stringWidth(label) / 2,
                startBtn.y + startBtn.height / 2 + fm.getAscent() / 2 - 3);
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
            System.out.println("[LobbyScreen] Back to main menu. Disconnecting...");
            if (gameClient != null) {
                gameClient.disconnect();
            }
            screenManager.setScreen(new MainMenuScreen(screenManager, gameClient, gameClient != null ? gameClient.getGameState() : null));
            return;
        }

        if (readyBtn.contains(x, y)) {
            toggleLocalReady();
            return;
        }

        if (isHost && startBtn.contains(x, y)) {
            if (canHostStart()) {
                System.out.println("[LobbyScreen] Host clicked Start.");
                if (gameClient != null) {
                    gameClient.sendStartGameRequest();
                }
            } else {
                System.out.println("[LobbyScreen] Cannot start yet. Connected players must be ready.");
            }
        }
    }

    @Override
    public void handleMouseMoved(int x, int y) {
        backHovered = backBtn.contains(x, y);
        readyHovered = readyBtn.contains(x, y);
        startHovered = startBtn.contains(x, y);
    }

    // ── Day 2 wiring helpers (used when LobbyState arrives) ─

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

        if (localPlayerId >= 0 && localPlayerId < this.readyFlags.length) {
            localReady = this.readyFlags[localPlayerId];
        }
    }

    /**
     * Sets which lobby slot belongs to this client.
     * Used later when the server assigns the player's ID.
     *
     * @param localPlayerId player slot index from 0 to 3.
     */
    public void setLocalPlayerId(int localPlayerId) {
        if (localPlayerId < 0 || localPlayerId >= playerNames.length) {
            return;
        }

        this.localPlayerId = localPlayerId;
        if (playerNames[localPlayerId] == null) {
            playerNames[localPlayerId] = "Player " + (localPlayerId + 1);
        }
    }

    /** @return true when this client's ready button is toggled on. */
    public boolean isLocalReady() {
        return localReady;
    }

    /** Toggles this client's local ready state until network messages take over. */
    private void toggleLocalReady() {
        localReady = !localReady;
        readyFlags[localPlayerId] = localReady;

        if (playerNames[localPlayerId] == null) {
            playerNames[localPlayerId] = "Player " + (localPlayerId + 1);
        }

        System.out.println("[LobbyScreen] Ready toggled: " + localReady);
        if (gameClient != null) {
            gameClient.sendReadyStatus(localReady);
        }
    }

    /** @return true when the host can start with the currently known lobby data. */
    private boolean canHostStart() {
        boolean hasConnectedPlayer = false;

        for (int i = 0; i < playerNames.length; i++) {
            if (playerNames[i] == null) {
                continue;
            }

            hasConnectedPlayer = true;
            if (!readyFlags[i]) {
                return false;
            }
        }

        return hasConnectedPlayer;
    }

    private void applyLobbyStateOnUiThread(LobbyState lobbyState) {
        SwingUtilities.invokeLater(() -> {
            updateFromLobbyState(lobbyState.getPlayerNames(), lobbyState.getReadyFlags());
            setLocalPlayerId(gameClient.getMyPlayerId());
        });
    }
}
