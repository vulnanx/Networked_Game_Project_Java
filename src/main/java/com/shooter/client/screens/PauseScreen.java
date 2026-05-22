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

    private final BufferedImage pausedBgImg;
    private final BufferedImage resumeBtnImg;
    private final BufferedImage exitBtnImg;

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

        pausedBgImg = AssetManager.getInstance().get("paused_bg");
        resumeBtnImg = AssetManager.getInstance().get("resume_button");
        exitBtnImg = AssetManager.getInstance().get("exit_game_button");

        int buttonWidth = 240;
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

        if (pausedBgImg != null) {
            g.drawImage(pausedBgImg, panelX, panelY, panelW, panelH, null);
            return;
        }

        GradientPaint panelPaint = new GradientPaint(
                panelX, panelY, new Color(0x171739),
                panelX, panelY + panelH, new Color(0x101025));
        g.setPaint(panelPaint);
        g.fillRoundRect(panelX, panelY, panelW, panelH, 12, 12);

        g.setColor(new Color(0x4455AA));
        g.setStroke(new BasicStroke(2));
        g.drawRoundRect(panelX, panelY, panelW, panelH, 12, 12);
    }

    private void drawButtons(Graphics2D g) {
        drawImageButton(g, resumeBtnImg, resumeBtn, hoveredIndex == 0);
        drawImageButton(g, exitBtnImg, exitBtn, hoveredIndex == 1);
    }

    private void drawImageButton(Graphics2D g, BufferedImage img, Rectangle button, boolean hovered) {
        if (img != null) {
            int drawW = button.width;
            int drawH = button.height;
            double scale = hovered ? 1.06 : 1.0;
            int w = (int) (drawW * scale);
            int h = (int) (drawH * scale);
            int x = button.x - (w - drawW) / 2;
            int y = button.y - (h - drawH) / 2;
            g.drawImage(img, x, y, w, h, null);
            return;
        }

        boolean isExit = button == exitBtn;
        Color fill = hovered ? (isExit ? new Color(0xE05C5C) : new Color(0xF5A623)) : new Color(0x1E2050);
        Color border = hovered ? (isExit ? new Color(0xFF9999) : new Color(0xFFD27A)) : new Color(0x4455AA);
        g.setColor(fill);
        g.fillRoundRect(button.x, button.y, button.width, button.height, 8, 8);
        g.setColor(border);
        g.setStroke(new BasicStroke(2));
        g.drawRoundRect(button.x, button.y, button.width, button.height, 8, 8);
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
