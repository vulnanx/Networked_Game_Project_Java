package com.shooter.server;

/**
 * ============================================================
 * FILE: GameServer.java
 * PACKAGE: server
 * OWNER: Member C (Systems)
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
        System.out.println("Server started (stub).");
    }
}