package com.shooter.client;

import com.shooter.client.screens.Screen;

import javax.swing.JPanel;
import java.awt.Color;
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
 * TRANSITION SYSTEM (added Day 4 by Christel):
 * When setScreen() is called, a smooth fade-to-black transition plays:
 *   1. FADE_OUT: black overlay fades in over ~10 ticks
 *   2. Screen is swapped at full black
 *   3. FADE_IN:  black overlay fades out over ~10 ticks
 *
 * For instant switches (e.g. server-driven pause toggle),
 * use setScreenImmediate().
 *
 * LAYER RULE: No game logic here. No server calls here.
 * ============================================================
 */
public class ScreenManager implements MouseListener, MouseMotionListener, KeyListener {

    // ── Transition constants ─────────────────────────────────────────────────
    /** How many ticks to fade out (0→255 alpha). */
    private static final int FADE_OUT_TICKS = 10;
    /** How many ticks to fade in (255→0 alpha). */
    private static final int FADE_IN_TICKS = 10;

    // ── Transition state ─────────────────────────────────────────────────────
    private enum TransitionState { NONE, FADING_OUT, FADING_IN }

    private TransitionState transitionState = TransitionState.NONE;
    private int transitionTick = 0;
    private Screen pendingScreen = null; // screen to switch to after fade-out

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

    // ── Screen switching ─────────────────────────────────────────────────────

    /**
     * Switches to a new screen with a smooth fade transition.
     * Calls onExit() on the old screen and onEnter() on the new one
     * once the fade-out completes.
     *
     * @param newScreen The screen to switch to. Pass null to clear the overlay.
     */
    public void setScreen(Screen newScreen) {
        // If we're already in the middle of a transition, skip the animation
        if (transitionState != TransitionState.NONE) {
            applyScreenSwitch(newScreen);
            return;
        }

        // If there's no current screen, skip fade-out (nothing to fade from)
        if (currentScreen == null) {
            applyScreenSwitch(newScreen);
            if (newScreen != null) {
                // Start a fade-in for the new screen
                transitionState = TransitionState.FADING_IN;
                transitionTick = 0;
            }
            return;
        }

        // Start the fade-out → swap → fade-in sequence
        pendingScreen = newScreen;
        transitionState = TransitionState.FADING_OUT;
        transitionTick = 0;
    }

    /**
     * Switches to a new screen INSTANTLY with no transition.
     * Use this for server-driven state changes (pause toggle, etc.)
     * where a fade would feel sluggish.
     */
    public void setScreenImmediate(Screen newScreen) {
        applyScreenSwitch(newScreen);
        transitionState = TransitionState.NONE;
        transitionTick = 0;
        pendingScreen = null;
    }

    /**
     * Internal: actually swaps the screen, calling lifecycle hooks.
     */
    private void applyScreenSwitch(Screen newScreen) {
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

    /** @return true if a screen is active (menu/lobby/pause) or a transition is playing. */
    public boolean hasActiveScreen() {
        return currentScreen != null || transitionState != TransitionState.NONE;
    }

    // ── Update & Render ──────────────────────────────────────────────────────

    /** Called by GamePanel every game tick. */
    public void update() {
        // Advance transition animation
        if (transitionState == TransitionState.FADING_OUT) {
            transitionTick++;
            if (transitionTick >= FADE_OUT_TICKS) {
                // Fade-out complete → swap screen → start fade-in
                applyScreenSwitch(pendingScreen);
                pendingScreen = null;
                transitionState = TransitionState.FADING_IN;
                transitionTick = 0;
            }
        } else if (transitionState == TransitionState.FADING_IN) {
            transitionTick++;
            if (transitionTick >= FADE_IN_TICKS) {
                transitionState = TransitionState.NONE;
                transitionTick = 0;
            }
        }

        // Update the current screen's logic
        if (currentScreen != null) {
            currentScreen.update();
        }
    }

    /** Called by GamePanel every render frame. */
    public void render(Graphics2D g) {
        // Draw the current screen
        if (currentScreen != null) {
            currentScreen.render(g);
        }

        // Draw the fade overlay on top
        if (transitionState == TransitionState.FADING_OUT) {
            // Alpha goes from 0 → 255 over FADE_OUT_TICKS
            int alpha = Math.min(255, (transitionTick * 255) / FADE_OUT_TICKS);
            g.setColor(new Color(0, 0, 0, alpha));
            g.fillRect(0, 0, panel.getWidth(), panel.getHeight());

        } else if (transitionState == TransitionState.FADING_IN) {
            // Alpha goes from 255 → 0 over FADE_IN_TICKS
            int alpha = Math.max(0, 255 - (transitionTick * 255) / FADE_IN_TICKS);
            g.setColor(new Color(0, 0, 0, alpha));
            g.fillRect(0, 0, panel.getWidth(), panel.getHeight());
        }
    }

    // ── MouseListener ────────────────────────────────────────────────────────

    @Override
    public void mouseClicked(MouseEvent e) {
        // Block input during transitions
        if (transitionState != TransitionState.NONE) return;
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
        if (transitionState != TransitionState.NONE) return;
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
        if (transitionState != TransitionState.NONE) return;
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
