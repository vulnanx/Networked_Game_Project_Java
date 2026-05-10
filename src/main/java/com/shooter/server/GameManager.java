package com.shooter.server;

import java.util.ArrayList;
import java.util.List;

import com.shooter.network.InputSnapshot;
import com.shooter.network.MessageType;
import com.shooter.network.NetworkMessage;
import com.shooter.shared.model.GameState;
import com.shooter.shared.model.Player;
import com.shooter.shared.util.Constants;
import com.shooter.shared.util.Direction;

/**
 * Controls game logic (SERVER SIDE).
 *
 * NOT REQUIRED for Milestone 1 yet.
 */
public class GameManager {

    private final GameState gameState;
    private final List<ClientHandler> clients;
    private boolean running;

    public GameManager(List<ClientHandler> clients) {
        gameState = new GameState();
        this.clients = clients;
        createPlayersForConnectedClients();
        running = false;
    }

    /**
     * Creates one authoritative Player object per connected client.
     * Clients render these server-owned players once GAME_STATE snapshots arrive.
     */
    private void createPlayersForConnectedClients() {
        List<ClientHandler> clientsSnapshot;
        synchronized (clients) {
            clientsSnapshot = new ArrayList<>(clients);
        }

        for (ClientHandler client : clientsSnapshot) {
            int playerId = client.getPlayerId();
            gameState.addPlayer(new Player(playerId, "Player " + (playerId + 1)));
        }
    }

    /**
     * Starts the server-side game loop.
     * This loop is separate from the client render loop: the server updates
     * game rules at 20 ticks per second, while clients can still render at 60 FPS.
     */
    public void startGameLoop() {
        running = true;

        long tickLengthNanos = 1_000_000_000L / Constants.SERVER_TICK_RATE;
        long nextTickTime = System.nanoTime();
        int ticksThisSecond = 0;
        long lastLogTime = System.currentTimeMillis();

        System.out.println("Server game loop started at " + Constants.SERVER_TICK_RATE + " ticks/sec.");

        while (running) {
            update();
            broadcastGameState();
            ticksThisSecond++;

            nextTickTime += tickLengthNanos;
            long sleepNanos = nextTickTime - System.nanoTime();

            if (sleepNanos > 0) {
                try {
                    Thread.sleep(sleepNanos / 1_000_000L, (int) (sleepNanos % 1_000_000L));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    stopGameLoop();
                }
            } else {
                // If the server falls behind, reset the schedule so it can recover.
                nextTickTime = System.nanoTime();
            }

            if (System.currentTimeMillis() - lastLogTime >= 1000) {
                System.out.println("Server ticks this second: " + ticksThisSecond);
                ticksThisSecond = 0;
                lastLogTime = System.currentTimeMillis();
            }
        }
    }

    /** Stops the server-side game loop cleanly. */
    public void stopGameLoop() {
        running = false;
    }

    public void update() {
        applyClientInputs();
        tickPlayerCooldowns();
        // TODO:
        // - Update enemies
        // - Handle collisions
        // - Handle rounds
    }

    /**
     * Applies each client's latest InputSnapshot to the matching server-owned Player.
     * This keeps movement authority on the server instead of trusting client positions.
     */
    private void applyClientInputs() {
        List<ClientHandler> clientsSnapshot;
        synchronized (clients) {
            clientsSnapshot = new ArrayList<>(clients);
        }

        for (ClientHandler client : clientsSnapshot) {
            Player player = findPlayerById(client.getPlayerId());
            InputSnapshot input = client.getLatestInput();

            if (player == null || input == null || !player.isAlive()) {
                continue;
            }

            if (input.isUpPressed()) {
                player.move(Direction.UP);
            }
            if (input.isDownPressed()) {
                player.move(Direction.DOWN);
            }
            if (input.isLeftPressed()) {
                player.move(Direction.LEFT);
            }
            if (input.isRightPressed()) {
                player.move(Direction.RIGHT);
            }

            if (input.getFacingDirection() != null) {
                player.setFacing(input.getFacingDirection());
            }
        }
    }

    private void tickPlayerCooldowns() {
        for (Player player : gameState.getPlayers()) {
            player.tickCooldown();
        }
    }

    private Player findPlayerById(int playerId) {
        for (Player player : gameState.getPlayers()) {
            if (player.getPlayerId() == playerId) {
                return player;
            }
        }

        return null;
    }

    public GameState getGameState() {
        return gameState;
    }

    /**
     * Sends the current authoritative GameState to every connected client.
     * Clients should render this state instead of making their own game-state changes.
     */
    private void broadcastGameState() {
        NetworkMessage stateMessage = new NetworkMessage(
            MessageType.GAME_STATE,
            -1,
            gameState
        );

        List<ClientHandler> clientsSnapshot;
        synchronized (clients) {
            clientsSnapshot = new ArrayList<>(clients);
        }

        for (ClientHandler client : clientsSnapshot) {
            client.sendMessage(stateMessage);
        }
    }
}
