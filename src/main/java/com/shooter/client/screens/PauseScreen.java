package com.shooter.client.screens;

import com.shooter.client.ScreenManager;
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

/**
 * ============================================================
 * FILE: PauseScreen.java
 * PACKAGE: client.screens
 * OWNER: Geastin (Member C - Screens / Systems / Assets)
 * ============================================================
 *
 * Pause overlay shown when the synchronized pause state is active.
 *
 * LAYER RULE:
 * This screen only draws UI and reports button actions through callbacks.
 * Game/server pause logic belongs in GameManager/GameClient.
 * ============================================================
 */
public class PauseScreen implements Screen {

    private final ScreenManager screenManager;
    private final Runnable onResume;
    private final Runnable onExitGame;

    private final Rectangle resumeBtn;
    private final Rectangle exitBtn;

    private int hoveredIndex = -1;
    private int tick = 0;

    /**
     * Creates a pause screen with no-op callbacks for UI-only testing.
     *
     * @param screenManager manager used to clear this overlay
     */
    public PauseScreen(ScreenManager screenManager) {
        this(screenManager, null, null);
    }

    /**
     * @param screenManager manager used to clear this overlay
     * @param onResume      called when the player chooses Resume
     * @param onExitGame    called when the player chooses Exit Game
     */
    public PauseScreen(ScreenManager screenManager, Runnable onResume, Runnable onExitGame) {
        this.screenManager = screenManager;
        this.onResume = onResume;
        this.onExitGame = onExitGame;

        int buttonWidth = 230;
        int buttonHeight = 48;
        int buttonX = Constants.SCREEN_WIDTH / 2 - buttonWidth / 2;

        resumeBtn = new Rectangle(buttonX, 365, buttonWidth, buttonHeight);
        exitBtn = new Rectangle(buttonX, 430, buttonWidth, buttonHeight);
    }

    @Override
    public void onEnter() {
        tick = 0;
        hoveredIndex = -1;
        System.out.println("[PauseScreen] Game paused.");
    }

    @Override
    public void onExit() {
        System.out.println("[PauseScreen] Leaving pause screen.");
    }

    @Override
    public void update() {
        tick++;
    }

    @Override
    public void render(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        drawDimmedGame(g);
        drawPausePanel(g);
        drawButtons(g);
    }

    private void drawDimmedGame(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 170));
        g.fillRect(0, 0, Constants.SCREEN_WIDTH, Constants.SCREEN_HEIGHT);
    }

    private void drawPausePanel(Graphics2D g) {
        int panelW = 390;
        int panelH = 260;
        int panelX = Constants.SCREEN_WIDTH / 2 - panelW / 2;
        int panelY = 245;

        GradientPaint panelPaint = new GradientPaint(
                panelX, panelY, new Color(0x171739),
                panelX, panelY + panelH, new Color(0x101025));
        g.setPaint(panelPaint);
        g.fillRoundRect(panelX, panelY, panelW, panelH, 12, 12);

        g.setColor(new Color(0x4455AA));
        g.setStroke(new BasicStroke(2));
        g.drawRoundRect(panelX, panelY, panelW, panelH, 12, 12);

        float glow = 0.65f + 0.35f * (float) Math.abs(Math.sin(tick * 0.05));
        g.setFont(new Font("Monospaced", Font.BOLD, 42));
        g.setColor(new Color((int) (245 * glow), (int) (166 * glow), 35));
        drawCentered(g, "PAUSED", Constants.SCREEN_WIDTH / 2, panelY + 70);

        g.setFont(new Font("Monospaced", Font.PLAIN, 13));
        g.setColor(new Color(0xA8B4D8));
        drawCentered(g, "Waiting for the team pause state to change.",
                Constants.SCREEN_WIDTH / 2, panelY + 102);
    }

    private void drawButtons(Graphics2D g) {
        drawButton(g, resumeBtn, "RESUME", 0);
        drawButton(g, exitBtn, "EXIT GAME", 1);
    }

    private void drawButton(Graphics2D g, Rectangle button, String label, int index) {
        boolean hovered = hoveredIndex == index;
        Color fill = hovered ? new Color(0xF5A623) : new Color(0x1E2050);
        Color border = hovered ? new Color(0xFFD27A) : new Color(0x4455AA);

        if (index == 1 && hovered) {
            fill = new Color(0xE05C5C);
            border = new Color(0xFF9999);
        }

        g.setColor(fill);
        g.fillRoundRect(button.x, button.y, button.width, button.height, 8, 8);
        g.setColor(border);
        g.setStroke(new BasicStroke(2));
        g.drawRoundRect(button.x, button.y, button.width, button.height, 8, 8);

        g.setFont(new Font("Monospaced", Font.BOLD, 15));
        g.setColor(hovered && index == 0 ? Color.BLACK : Color.WHITE);
        drawCentered(g, label, button.x + button.width / 2, button.y + 30);
    }

    private void drawCentered(Graphics2D g, String text, int centerX, int baselineY) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, centerX - fm.stringWidth(text) / 2, baselineY);
    }

    @Override
    public void handleMouseClicked(int x, int y) {
        if (resumeBtn.contains(x, y)) {
            resume();
            return;
        }

        if (exitBtn.contains(x, y)) {
            exitGame();
        }
    }

    @Override
    public void handleMouseMoved(int x, int y) {
        if (resumeBtn.contains(x, y)) {
            hoveredIndex = 0;
        } else if (exitBtn.contains(x, y)) {
            hoveredIndex = 1;
        } else {
            hoveredIndex = -1;
        }
    }

    @Override
    public void handleKeyPressed(int keyCode) {
        if (keyCode == KeyEvent.VK_ESCAPE || keyCode == KeyEvent.VK_P) {
            resume();
        }
    }

    private void resume() {
        if (onResume != null) {
            onResume.run();
        }
        screenManager.setScreen(null);
    }

    private void exitGame() {
        if (onExitGame != null) {
            onExitGame.run();
        }
    }
}
