package com.shooter.client.screens;

import java.awt.Graphics2D;

/**
 * ============================================================
 * FILE: Screen.java
 * PACKAGE: client.screens
 * OWNER: Geastin (Member C - Screens / Systems / Assets)
 * ============================================================
 *
 * Interface that every game screen must implement.
 * ScreenManager calls these methods on the active screen.
 *
 * Screens: MainMenuScreen, LobbyScreen, PauseScreen, GameOverScreen
 * ============================================================
 */
public interface Screen {

    /** Called once per game tick to update screen logic (animations, timers). */
    void update();

    /** Called once per frame to draw the screen. */
    void render(Graphics2D g);

    /**
     * Called when this screen becomes the active screen. Use for initialization.
     */
    void onEnter();

    /** Called when this screen is being replaced. Use for cleanup. */
    void onExit();

    // ── Optional input hooks (screens override only what they need) ──────────

    /** Called when the mouse is clicked on the game panel. */
    default void handleMouseClicked(int x, int y) {
    }

    /** Called when the mouse is moved over the game panel (for hover effects). */
    default void handleMouseMoved(int x, int y) {
    }

    /** Called when a key is pressed while this screen is active. */
    default void handleKeyPressed(int keyCode) {
    }
}
