package com.shooter.client.screens;

import com.shooter.client.ScreenManager;
import com.shooter.shared.util.AssetManager;
import com.shooter.shared.util.Constants;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

/**
 * ============================================================
 * FILE: GameOverScreen.java
 * PACKAGE: client.screens
 * OWNER: Geastin (Member C - Screens / Systems / Assets)
 * ============================================================
 *
 * Post-game result screen.
 *
 * LAYER RULE:
 * This screen displays result data passed to it. It does not calculate
 * gameplay results and does not talk to the server directly.
 * ============================================================
 */
public class GameOverScreen implements Screen {

    private final ScreenManager screenManager;
    private final Runnable onBackToLobby;

    private final boolean teamWon;
    private final int roundsCleared;
    private final int totalEnemiesKilled;
    private final int[] killsPerPlayer;

    // Images
    private final BufferedImage gameOverBgImg;
    private final BufferedImage victoryBgImg;
    private final BufferedImage runStatsBtnImg;
    private final BufferedImage backToLobbyBtnImg;

    // Buttons / interaction
    private final Rectangle runStatsBtn;
    private final Rectangle backToLobbyBtn;
    private boolean runStatsHovered = false;
    private boolean backHovered = false;
    private boolean statsModalOpen = false;
    private int tick = 0;

    /**
     * Creates a simple game-over screen with placeholder stats.
     *
     * @param screenManager manager used for screen changes
     */
    public GameOverScreen(ScreenManager screenManager) {
        this(screenManager, false, 0, 0, null, null);
    }

    /**
     * @param screenManager      manager used for screen changes
     * @param teamWon            true if all rounds were cleared
     * @param roundsCleared      number of rounds cleared by the team
     * @param totalEnemiesKilled total enemies killed during the run
     * @param killsPerPlayer     optional array of kills for P1-P4
     * @param onBackToLobby      called when Back to Lobby is selected
     */
    public GameOverScreen(ScreenManager screenManager, boolean teamWon, int roundsCleared,
            int totalEnemiesKilled, int[] killsPerPlayer, Runnable onBackToLobby) {
        this.screenManager = screenManager;
        this.teamWon = teamWon;
        this.roundsCleared = roundsCleared;
        this.totalEnemiesKilled = totalEnemiesKilled;
        this.killsPerPlayer = copyKills(killsPerPlayer);
        this.onBackToLobby = onBackToLobby;

        // Load images (fallbacks handled by AssetManager)
        this.gameOverBgImg = AssetManager.getInstance().get("game_over_bg");
        this.victoryBgImg = AssetManager.getInstance().get("victory_bg");
        this.runStatsBtnImg = AssetManager.getInstance().get("run_stats_button");
        this.backToLobbyBtnImg = AssetManager.getInstance().get("back_to_lobby_button");

        // Button hitboxes (sizes chosen to match previous layout)
        runStatsBtn = new Rectangle(Constants.SCREEN_WIDTH / 2 - 260, 675, 220, 46);
        backToLobbyBtn = new Rectangle(Constants.SCREEN_WIDTH / 2 + 40, 675, 220, 46);
    }

    @Override
    public void onEnter() {
        tick = 0;
        runStatsHovered = false;
        backHovered = false;
        statsModalOpen = false;
        System.out.println("[GameOverScreen] Showing game over results.");
    }

    @Override
    public void onExit() {
        System.out.println("[GameOverScreen] Leaving game over screen.");
    }

    @Override
    public void update() {
        tick++;
    }

    @Override
    public void render(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        drawBackground(g);
        drawButtons(g);
        if (statsModalOpen) {
            drawRunStatsModal(g);
        }
    }

    private void drawBackground(Graphics2D g) {
        BufferedImage bg = teamWon ? victoryBgImg : gameOverBgImg;
        if (bg != null) {
            g.drawImage(bg, 0, 0, Constants.SCREEN_WIDTH, Constants.SCREEN_HEIGHT, null);
            return;
        }

        GradientPaint gradient = new GradientPaint(
                0, 0, teamWon ? new Color(0x0E2A10) : new Color(0x0D0D1A),
                0, Constants.SCREEN_HEIGHT, teamWon ? new Color(0x2A6632) : new Color(0x1A1A3E));
        g.setPaint(gradient);
        g.fillRect(0, 0, Constants.SCREEN_WIDTH, Constants.SCREEN_HEIGHT);
    }

    private void drawTitle(Graphics2D g) {
        // Title and subtitle are baked into the background image.
    }

    private void drawStats(Graphics2D g) {
        int panelW = 440;
        int panelH = 390;
        int panelX = Constants.SCREEN_WIDTH / 2 - panelW / 2;
        int panelY = 220;

        g.setColor(new Color(0x111130));
        g.fillRoundRect(panelX, panelY, panelW, panelH, 10, 10);
        g.setColor(new Color(0x4455AA));
        g.setStroke(new BasicStroke(2));
        g.drawRoundRect(panelX, panelY, panelW, panelH, 10, 10);

        g.setFont(new Font("Monospaced", Font.BOLD, 18));
        g.setColor(new Color(0xF5A623));
        drawCentered(g, "RUN STATS", Constants.SCREEN_WIDTH / 2, panelY + 44);

        int labelX = panelX + 55;
        int valueX = panelX + panelW - 150;
        int y = panelY + 92;

        drawStatLine(g, labelX, valueX, y, "Rounds Cleared", roundsCleared + " / " + Constants.TOTAL_ROUNDS);
        y += 36;
        drawStatLine(g, labelX, valueX, y, "Enemies Killed", String.valueOf(totalEnemiesKilled));
        y += 52;

        g.setFont(new Font("Monospaced", Font.BOLD, 15));
        g.setColor(new Color(0xA8B4D8));
        g.drawString("Kills Per Player", labelX, y);
        y += 30;

        for (int i = 0; i < Constants.MAX_PLAYERS; i++) {
            drawPlayerKillLine(g, labelX, valueX, y, i, killsPerPlayer[i]);
            y += 34;
        }
    }

    private void drawStatLine(Graphics2D g, int labelX, int valueX, int y, String label, String value) {
        g.setFont(new Font("Monospaced", Font.PLAIN, 15));
        g.setColor(new Color(0xA8B4D8));
        g.drawString(label + ":", labelX, y);

        g.setFont(new Font("Monospaced", Font.BOLD, 15));
        g.setColor(Color.WHITE);
        g.drawString(value, valueX, y);
    }

    private void drawPlayerKillLine(Graphics2D g, int labelX, int valueX, int y, int playerIndex, int kills) {
        Color[] playerColors = {
                new Color(0x4A90D9),
                new Color(0xE05C5C),
                new Color(0x50E878),
                new Color(0xF5A623)
        };

        g.setColor(playerColors[playerIndex]);
        g.fillOval(labelX, y - 17, 18, 18);

        g.setFont(new Font("Monospaced", Font.BOLD, 13));
        g.setColor(Color.WHITE);
        g.drawString("P" + (playerIndex + 1), labelX + 4, y - 3);

        g.setFont(new Font("Monospaced", Font.PLAIN, 15));
        g.setColor(new Color(0xA8B4D8));
        g.drawString("Player " + (playerIndex + 1), labelX + 36, y);

        g.setFont(new Font("Monospaced", Font.BOLD, 15));
        g.setColor(Color.WHITE);
        g.drawString(String.valueOf(kills), valueX, y);
    }

    private void drawButtons(Graphics2D g) {
        // Draw run stats button (left)
        drawImageButton(g, runStatsBtnImg, runStatsBtn, runStatsHovered);

        // Draw back to lobby button (right)
        drawImageButton(g, backToLobbyBtnImg, backToLobbyBtn, backHovered);
    }

    private void drawImageButton(Graphics2D g, BufferedImage img, Rectangle r, boolean hovered) {
        if (img != null) {
            int drawW = r.width;
            int drawH = r.height;
            double scale = hovered ? 1.06 : 1.0;
            int w = (int) (drawW * scale);
            int h = (int) (drawH * scale);
            int x = r.x - (w - drawW) / 2;
            int y = r.y - (h - drawH) / 2;
            g.drawImage(img, x, y, w, h, null);
            return;
        }

        Color fill = hovered ? new Color(0xF5A623) : new Color(0x1E2050);
        Color border = hovered ? new Color(0xFFD27A) : new Color(0x4455AA);
        g.setColor(fill);
        g.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);
        g.setColor(border);
        g.setStroke(new BasicStroke(2));
        g.drawRoundRect(r.x, r.y, r.width, r.height, 8, 8);
    }

    private void drawRunStatsModal(Graphics2D g) {
        // Dim background
        g.setColor(new Color(0, 0, 0, 160));
        g.fillRect(0, 0, Constants.SCREEN_WIDTH, Constants.SCREEN_HEIGHT);

        // Reuse stats panel drawing in the center
        drawStats(g);
    }

    private void drawCentered(Graphics2D g, String text, int centerX, int baselineY) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, centerX - fm.stringWidth(text) / 2, baselineY);
    }

    @Override
    public void handleMouseClicked(int x, int y) {
        if (statsModalOpen) {
            // clicking anywhere while modal open closes it
            statsModalOpen = false;
            return;
        }

        if (runStatsBtn.contains(x, y)) {
            statsModalOpen = true;
            return;
        }

        if (backToLobbyBtn.contains(x, y)) {
            backToLobby();
        }
    }

    @Override
    public void handleMouseMoved(int x, int y) {
        runStatsHovered = runStatsBtn.contains(x, y);
        backHovered = backToLobbyBtn.contains(x, y);
    }

    @Override
    public void handleKeyPressed(int keyCode) {
        if (keyCode == KeyEvent.VK_ENTER) {
            backToLobby();
        }
    }

    private void backToLobby() {
        if (onBackToLobby != null) {
            onBackToLobby.run();
        }
    }

    private int[] copyKills(int[] source) {
        int[] copy = new int[Constants.MAX_PLAYERS];
        if (source == null) {
            return copy;
        }

        int length = Math.min(source.length, copy.length);
        for (int i = 0; i < length; i++) {
            copy[i] = source[i];
        }

        return copy;
    }
}
