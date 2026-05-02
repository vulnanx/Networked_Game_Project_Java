package com.shooter.ui;

import java.awt.Graphics2D;
import java.awt.Color;
import com.shooter.shared.model.GameState;
import com.shooter.shared.model.Player;

/**
 * ============================================================
 * FILE: HUD.java
 * PACKAGE: ui
 * OWNER: Member A (UI)
 * ============================================================
 *
 * RESPONSIBILITY:
 * Draws overlay UI:
 * - HP
 * - Round
 * - Enemy count
 *
 * WHAT TO ADD HERE:
 * - HP bar graphics
 * - Power-up indicators
 * - Score display
 *
 * WHAT NOT TO PUT HERE:
 * - Game logic
 * - Input handling
 *
 * CONNECTS TO:
 * GamePanel (called during render)
 * ============================================================
 */
public class HUD {

    public void render(Graphics2D g, GameState state, int enemyCount) {

        Player player = state.getMainPlayer();

        g.setColor(Color.WHITE);
        g.drawString("Round: " + state.getCurrentRound(), 20, 20);
        g.drawString("Enemies: " + enemyCount, 20, 40);

        if (player != null) {
            g.drawString("HP: " + player.getHp(), 20, 60);
        }
    }
}