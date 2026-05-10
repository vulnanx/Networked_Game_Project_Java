package com.shooter.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.shooter.shared.util.Constants;

/**
 * ============================================================
 * FILE: GameServer.java
 * PACKAGE: server
 * OWNER: Member A (Networking Core)
 * ============================================================
 *
 * RESPONSIBILITY:
 * Entry point for the game server. Opens a TCP socket on
 * port 5000 and waits for up to 4 players to connect.
 * Each connected player gets its own ClientHandler thread.
 *
 * WHAT TO ADD LATER:
 * - Start GameManager loop once all players are connected
 * - Broadcast GameState to all clients (Day 2)
 *
 * WHAT NOT TO PUT HERE:
 * - Rendering code
 * - Game logic (belongs in GameManager)
 * ============================================================
 */
public class GameServer {

    // Keeps track of all active client handlers (one per player).
    // synchronizedList lets ClientHandler threads remove themselves safely.
    private final List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());

    /**
     * Starts the server: opens the port, accepts connections,
     * and spawns one ClientHandler thread per player.
     */
    public void start() {
        System.out.println("Server starting on port " + Constants.SERVER_PORT + "...");

        // ServerSocket listens for incoming TCP connections
        try (ServerSocket serverSocket = new ServerSocket(Constants.SERVER_PORT)) {

            System.out.println("Waiting for players... (max " + Constants.MAX_PLAYERS + ")");

            // Keep accepting clients until we have MAX_PLAYERS (4)
            while (clients.size() < Constants.MAX_PLAYERS) {

                // This line BLOCKS until a client connects
                Socket clientSocket = serverSocket.accept();

                // Assign the next available player ID (0, 1, 2, 3)
                int playerId = clients.size();

                // Create a handler for this specific client
                ClientHandler handler = new ClientHandler(clientSocket, playerId, this);
                clients.add(handler);

                // Run the handler on its own thread so we can keep accepting others
                Thread thread = new Thread(handler);
                thread.setName("ClientHandler-" + playerId);
                thread.start();

                System.out.println("Player connected: " + playerId);
            }

            System.out.println("All " + Constants.MAX_PLAYERS + " players connected. Starting game...");

            GameManager gameManager = new GameManager(clients);
            gameManager.startGameLoop();

        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Removes a disconnected client from the active server list.
     * ClientHandler calls this when its socket closes or the player leaves.
     *
     * @param client the client handler that disconnected
     */
    public void removeClient(ClientHandler client) {
        boolean removed = clients.remove(client);

        if (removed) {
            System.out.println("Cleaned up Player " + client.getPlayerId()
                    + ". Active clients: " + clients.size());
        }
    }

    /** Entry point — just creates and starts the server. */
    public static void main(String[] args) {
        new GameServer().start();
    }
}
