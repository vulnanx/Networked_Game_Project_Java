package com.shooter.client;

import javax.swing.JFrame;
import com.shooter.shared.model.GameState;
import com.shooter.shared.model.Player;
import com.shooter.shared.util.Constants;

/**
 * ============================================================
 * FILE: GameClient.java
 * PACKAGE: client
 * OWNER: Member A (Engine/UI)
 * ============================================================
 *
 * RESPONSIBILITY:
 * Entry point for the CLIENT (player).
 * Creates the game window and starts the game loop.
 *
 * WHAT TO ADD HERE:
 * - Menu / Lobby UI (Milestone 2)
 * - Connection to server (GameClient networking)
 * - Player login / name input
 *
 * WHAT NOT TO PUT HERE:
 * - Game logic (no movement, no collision)
 * - Rendering logic (belongs in GamePanel)
 * - Networking handling logic (belongs in network/)
 *
 * CONNECTS TO:
 * GamePanel (renders everything)
 * GameState (holds game data)
 * ============================================================
 */
public class GameClient {

    public static void main(String[] args) {

        JFrame window = new JFrame(Constants.WINDOW_TITLE);

        // Create game state
        GameState gameState = new GameState();
        Player player = new Player(0, "Player 1");
        gameState.addPlayer(player);

        GamePanel panel = new GamePanel(gameState);

        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(false);
        window.add(panel);
        window.pack();
        window.setLocationRelativeTo(null);
        window.setVisible(true);

        panel.startGameLoop();
    }
}