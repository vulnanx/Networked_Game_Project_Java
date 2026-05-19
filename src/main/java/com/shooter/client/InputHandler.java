package com.shooter.client;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashSet;
import java.util.Set;

/**
 * Handles keyboard input.
 * Tracks currently pressed keys.
 */
public class InputHandler implements KeyListener {

    private Set<Integer> pressed = new HashSet<>();
    private boolean active = true;

    public boolean isPressed(int keyCode) {
        return active && pressed.contains(keyCode);
    }

    public void setActive(boolean active) {
        this.active = active;
        if (!active) {
            pressed.clear();
        }
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (active) {
            pressed.add(e.getKeyCode());
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        pressed.remove(e.getKeyCode());
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }
}