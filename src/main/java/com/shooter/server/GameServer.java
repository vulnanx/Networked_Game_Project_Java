package com.shooter.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.shooter.network.LobbyState;
import com.shooter.network.MessageType;
import com.shooter.network.NetworkMessage;
import com.shooter.network.ChatMessage;
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

    // Lobby data broadcast to clients whenever players connect, leave, or ready up.
    private final String[] lobbyPlayerNames = new String[Constants.MAX_PLAYERS];
    private final boolean[] lobbyReadyFlags = new boolean[Constants.MAX_PLAYERS];
    private int hostPlayerId = -1;
    private volatile boolean gameStarted = false;

    /**

    /**
     * Starts the server: opens the port, accepts connections,
     * and spawns one ClientHandler thread per player.
     */
    public void start() {
        System.out.println("Server starting on port " + Constants.SERVER_PORT + "...");

        // ServerSocket listens for incoming TCP connections
        try (ServerSocket serverSocket = new ServerSocket(Constants.SERVER_PORT)) {

            System.out.println("Waiting for players... (max " + Constants.MAX_PLAYERS + ")");

            // Keep accepting clients forever
            while (true) {

                // This line BLOCKS until a client connects
                Socket clientSocket = serverSocket.accept();
                
                if (gameStarted || gameManager != null) {
                    System.out.println("Connection rejected: Game is currently running.");
                    clientSocket.close();
                    continue;
                }

                // Find and reserve the first available lobby slot
                int playerId = -1;
                synchronized (this) {
                    for (int i = 0; i < Constants.MAX_PLAYERS; i++) {
                        if (lobbyPlayerNames[i] == null) {
                            playerId = i;
                            // Reserve the slot temporarily until the ClientHandler completes initialization
                            lobbyPlayerNames[i] = "Connecting...";
                            break;
                        }
                    }
                }

                if (playerId == -1) {
                    System.out.println("Lobby is full (4 players already connected/connecting). Rejecting connection.");
                    clientSocket.close();
                    continue;
                }

                // Create a handler for this specific client
                ClientHandler handler = new ClientHandler(clientSocket, playerId, this);
                clients.add(handler);

                // Run the handler on its own thread so we can keep accepting others
                Thread thread = new Thread(handler);
                thread.setName("ClientHandler-" + playerId);
                thread.start();

                System.out.println("Player slot " + playerId + " successfully assigned.");
            }

        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private GameManager gameManager;

    /**
     * Called by a ClientHandler when the host clicks "Start Game".
     * Broadcasts the start event and begins the authoritative game loop.
     */
    public synchronized void startGame() {
        if (gameStarted) return;
        gameStarted = true;
        System.out.println("Host started the game. Broadcasting START_GAME...");

        NetworkMessage startMsg = new NetworkMessage(MessageType.START_GAME, -1, null);
        synchronized (clients) {
            for (ClientHandler client : clients) {
                client.sendMessage(startMsg);
            }
        }

        gameManager = new GameManager(clients);
        Thread gameThread = new Thread(() -> gameManager.startGameLoop());
        gameThread.setName("GameManagerLoop");
        gameThread.start();
    }

    /**
     * Removes a disconnected client from the active server list.
     * ClientHandler calls this when its socket closes or the player leaves.
     *
     * @param client the client handler that disconnected
     */
    public synchronized void removeClient(ClientHandler client) {
        boolean removed = clients.remove(client);

        if (removed) {
            clearLobbySlot(client.getPlayerId());
            System.out.println("Cleaned up Player " + client.getPlayerId()
                    + ". Active clients: " + clients.size());
            if (gameManager != null) {
                gameManager.handlePlayerDisconnect(client.getPlayerId());
            }
            broadcastSystemMessage("Player " + (client.getPlayerId() + 1) + " has disconnected.");
            
            // If everybody leaves the server, automatically stop the active game so a new one can start later!
            if (clients.isEmpty()) {
                System.out.println("All players left. Shutting down active game and resetting lobby.");
                if (gameManager != null) {
                    gameManager.stopGameLoop();
                    gameManager = null;
                }
                gameStarted = false;
            }
        }
    }

    /**
     * Marks a player as visible in the lobby and broadcasts the new lobby state.
     *
     * @param playerId the connected player's slot ID
     */
    public synchronized void markPlayerConnected(int playerId) {
        if (!isValidPlayerId(playerId)) {
            return;
        }

        if (hostPlayerId == -1) {
            hostPlayerId = playerId;
        }

        lobbyPlayerNames[playerId] = "Player " + (playerId + 1);
        lobbyReadyFlags[playerId] = false;
        broadcastLobbyState();
        broadcastSystemMessage("Player " + (playerId + 1) + " has connected.");
    }

    /**
     * Updates one player's ready flag and broadcasts the new lobby state.
     *
     * @param playerId the player slot that changed readiness
     * @param ready true if the player is ready
     */
    public synchronized void updateReadyStatus(int playerId, boolean ready) {
        if (!isValidPlayerId(playerId) || lobbyPlayerNames[playerId] == null) {
            return;
        }

        lobbyReadyFlags[playerId] = ready;
        System.out.println("Player " + playerId + " ready=" + ready);
        broadcastLobbyState();
    }

    /** Sends the current lobby state to every connected client. */
    public synchronized void broadcastLobbyState() {
        LobbyState state = new LobbyState(lobbyPlayerNames, lobbyReadyFlags, hostPlayerId);
        NetworkMessage message = new NetworkMessage(MessageType.LOBBY_STATE, -1, state);
        List<ClientHandler> clientSnapshot;

        synchronized (clients) {
            clientSnapshot = new ArrayList<>(clients);
        }

        for (ClientHandler client : clientSnapshot) {
            client.sendMessage(message);
        }
    }

    private void clearLobbySlot(int playerId) {
        if (!isValidPlayerId(playerId)) {
            return;
        }

        lobbyPlayerNames[playerId] = null;
        lobbyReadyFlags[playerId] = false;

        if (hostPlayerId == playerId) {
            hostPlayerId = findNextConnectedPlayerId();
        }

        broadcastLobbyState();
    }

    private int findNextConnectedPlayerId() {
        for (int i = 0; i < lobbyPlayerNames.length; i++) {
            if (lobbyPlayerNames[i] != null) {
                return i;
            }
        }

        return -1;
    }

    private boolean isValidPlayerId(int playerId) {
        return playerId >= 0 && playerId < Constants.MAX_PLAYERS;
    }

    public synchronized void broadcastChatMessage(NetworkMessage message) {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                client.sendMessage(message);
            }
        }
    }

    public synchronized void broadcastSystemMessage(String text) {
        ChatMessage systemMsg = new ChatMessage("System", text, System.currentTimeMillis());
        NetworkMessage netMsg = new NetworkMessage(MessageType.CHAT, -1, systemMsg);
        broadcastChatMessage(netMsg);
    }

    /** Entry point — just creates and starts the server. */
    public static void main(String[] args) {
        new GameServer().start();
    }
}
