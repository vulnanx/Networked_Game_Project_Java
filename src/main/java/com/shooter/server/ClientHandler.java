package com.shooter.server;

/**
 * Handles communication with ONE client.
 *
 * NOT USED in Milestone 1.
 */
public class ClientHandler implements Runnable {

    private int playerId;

    public ClientHandler(int playerId) {
        this.playerId = playerId;
    }

    @Override
    public void run() {
        // TODO (Milestone 2):
        // Listen to client messages
        // Send game updates
    }
}