package com.shooter.client;

import com.shooter.client.screens.Screen;

import javax.swing.JPanel;
import java.awt.Graphics2D;
import java.awt.event.*;

/**
 * ============================================================
 * FILE: ScreenManager.java
 * PACKAGE: client
 * OWNER: Geastin (Member C - Screens / Systems / Assets)
 * ============================================================
 *
 * Manages which Screen is currently active.
 * GamePanel calls update() and render() on this every tick/frame.
 *
 * Usage (by Geastin in GamePanel):
 * screenManager = new ScreenManager(this);
 * screenManager.setScreen(new MainMenuScreen(screenManager));
 *
 * LAYER RULE: No game logic here. No server calls here.
 * ============================================================
 */
public class ScreenManager implements MouseListener, MouseMotionListener, KeyListener {

    /** The currently displayed screen (null = no overlay, game is running). */
    private Screen currentScreen;

    /** The panel this manager is attached to (for registering listeners). */
    private JPanel panel;

    /**
     * Creates a ScreenManager and registers input listeners on the given panel.
     *
     * @param panel The JPanel that receives mouse/keyboard events (usually
     *              GamePanel).
     */
    public ScreenManager(JPanel panel) {
        this.panel = panel;
        panel.addMouseListener(this);
        panel.addMouseMotionListener(this);
        panel.addKeyListener(this);
    }

    /**
     * Switches to a new screen.
     * Calls onExit() on the old screen and onEnter() on the new one.
     *
     * @param newScreen The screen to switch to. Pass null to clear the overlay.
     */
    public void setScreen(Screen newScreen) {
        if (currentScreen != null) {
            currentScreen.onExit();
        }
        currentScreen = newScreen;
        if (currentScreen != null) {
            currentScreen.onEnter();
        }
    }

    /** @return The currently active screen, or null if none. */
    public Screen getCurrentScreen() {
        return currentScreen;
    }

    /** @return true if a screen is active (menu/lobby/pause). */
    public boolean hasActiveScreen() {
        return currentScreen != null;
    }

    /** Called by GamePanel every game tick. */
    public void update() {
        if (currentScreen != null) {
            currentScreen.update();
        }
    }

    /** Called by GamePanel every render frame. */
    public void render(Graphics2D g) {
        if (currentScreen != null) {
            currentScreen.render(g);
        }
    }

    // ── MouseListener ────────────────────────────────────────────────────────

    @Override
    public void mouseClicked(MouseEvent e) {
        if (currentScreen != null) {
            currentScreen.handleMouseClicked(e.getX(), e.getY());
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    // ── MouseMotionListener ──────────────────────────────────────────────────

    @Override
    public void mouseMoved(MouseEvent e) {
        if (currentScreen != null) {
            currentScreen.handleMouseMoved(e.getX(), e.getY());
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
    }

    // ── KeyListener ──────────────────────────────────────────────────────────

    @Override
    public void keyPressed(KeyEvent e) {
        if (currentScreen != null) {
            currentScreen.handleKeyPressed(e.getKeyCode());
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }
}
