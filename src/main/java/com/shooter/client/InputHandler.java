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

    public boolean isPressed(int keyCode) {
        return pressed.contains(keyCode);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        pressed.add(e.getKeyCode());
    }

    @Override
    public void keyReleased(KeyEvent e) {
        pressed.remove(e.getKeyCode());
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }
}