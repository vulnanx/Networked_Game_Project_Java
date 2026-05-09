package com.shooter.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import com.shooter.shared.util.Constants;

/**
 * ============================================================
 * FILE: GameServer.java
 * PACKAGE: server
 * OWNER: Member C (Systems) - Currently being worked on by Member A for
 * networking
 * ============================================================
 *
 * RESPONSIBILITY:
 * Entry point for the SERVER (host player).
 *
 * In Milestone 2:
 * - Accept client connections
 * - Maintain authoritative GameState
 * - Broadcast updates to all clients
 *
 * WHAT TO ADD HERE:
 * - ServerSocket setup
 * - Accept connections
 * - Start GameManager loop
 *
 * WHAT NOT TO PUT HERE:
 * - Rendering code
 * - Client input logic
 *
 * CONNECTS TO:
 * ClientHandler (per player)
 * GameManager (game loop)
 * ============================================================
 */
public class GameServer {

    public static void main(String[] args) {
        System.out.println("Starting Game Server on port " + Constants.SERVER_PORT + "...");

        // Open a ServerSocket on port 5000 (defined in Constants)
        try (ServerSocket serverSocket = new ServerSocket(Constants.SERVER_PORT)) {
            System.out.println("Server started. Waiting for players...");

            int connectedPlayers = 0;

            // Accept up to 4 clients (Constants.MAX_PLAYERS)
            while (connectedPlayers < Constants.MAX_PLAYERS) {
                // This blocks until a client connects
                Socket clientSocket = serverSocket.accept();

                // Print the required connection log
                System.out.println("Player connected: " + connectedPlayers);

                // (Task 3 will go here: creating and starting the ClientHandler thread)

                connectedPlayers++;
            }

            System.out.println("Lobby is full! All " + Constants.MAX_PLAYERS + " players connected.");

        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}