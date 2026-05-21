package com.shooter.server;

import java.io.IOException;
import java.io.ObjectOutputStream;
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
import com.shooter.shared.util.GameSettings;

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

    /**
     * Tracks the order in which each player slot was filled.
     * joinSequence[i] == -1  → slot i is empty.
     * joinSequence[i] == n   → this player was the (n+1)-th to join this lobby session.
     * Used by findNextConnectedPlayerId() so that host migration always promotes
     * the player who joined earliest, even after slot IDs are reused.
     */
    private final int[] joinSequence = new int[Constants.MAX_PLAYERS];
    private int joinCounter = 0;

    private int hostPlayerId = -1;
    private volatile boolean gameStarted = false;

    /**
     * Authoritative game settings for this lobby session.
     * The host can update this via a SETTINGS message before the game starts.
     * GameManager reads this once at startup to configure the game.
     */
    private final GameSettings gameSettings = new GameSettings();

    /**

    /**
     * Starts the server: opens the port, accepts connections,
     * and spawns one ClientHandler thread per player.
     */
    public void start() {
        System.out.println("Server starting on port " + Constants.SERVER_PORT + "...");

        // Initialise join-sequence slots to -1 (empty).
        java.util.Arrays.fill(joinSequence, -1);

        // ServerSocket listens for incoming TCP connections
        try (ServerSocket serverSocket = new ServerSocket(Constants.SERVER_PORT)) {

            System.out.println("Waiting for players... (max " + Constants.MAX_PLAYERS + ")");

            // Keep accepting clients forever
            while (true) {

                // This line BLOCKS until a client connects
                Socket clientSocket = serverSocket.accept();
                
                if (gameStarted || gameManager != null) {
                    System.out.println("Connection rejected: Game is currently running.");
                    rejectConnection(clientSocket, "Game already in progress. Wait for the current game to end.");
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

    /**
     * Sends a REJECTED message to a connecting client and closes the socket.
     * This gives the client a structured, human-readable reason instead of an
     * abrupt EOF that would appear as a generic connection error.
     *
     * @param clientSocket the freshly-accepted socket to reject
     * @param reason       human-readable reason shown on the client's screen
     */
    private void rejectConnection(Socket clientSocket, String reason) {
        try {
            ObjectOutputStream rejectOut = new ObjectOutputStream(clientSocket.getOutputStream());
            rejectOut.flush();
            rejectOut.writeObject(new NetworkMessage(MessageType.REJECTED, -1, reason));
            rejectOut.flush();
        } catch (IOException e) {
            System.err.println("[Server] Could not send rejection message: " + e.getMessage());
        } finally {
            try { clientSocket.close(); } catch (IOException ignored) {}
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

        gameManager = new GameManager(clients, gameSettings);
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

        // Stamp this slot with the current join counter so we can later elect
        // the earliest remaining joiner as the new host.
        joinSequence[playerId] = joinCounter++;

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
        NetworkMessage lobbyMsg  = new NetworkMessage(MessageType.LOBBY_STATE, -1, state);
        NetworkMessage settingsMsg = new NetworkMessage(MessageType.SETTINGS, -1, gameSettings);
        List<ClientHandler> clientSnapshot;

        synchronized (clients) {
            clientSnapshot = new ArrayList<>(clients);
        }

        for (ClientHandler client : clientSnapshot) {
            client.sendMessage(lobbyMsg);
            client.sendMessage(settingsMsg);
        }
    }

    /**
     * Called by ClientHandler when the host sends a SETTINGS message.
     * Only the current host is allowed to change settings; the check is done
     * in ClientHandler before calling this.
     *
     * @param hostPlayerId  the player ID that sent the update (must equal current host)
     * @param newSettings   the new settings payload from the host client
     */
    public synchronized void applySettings(int senderId, GameSettings newSettings) {
        if (newSettings == null) return;
        if (senderId != hostPlayerId) {
            System.out.println("[Server] Player " + senderId + " tried to change settings but is not host. Ignored.");
            return;
        }
        // Copy each field into the authoritative object so we don't replace the reference
        copySettings(newSettings, gameSettings);
        System.out.println("[Server] Settings updated by host: " + gameSettings);
        broadcastLobbyState(); // sends both LOBBY_STATE and the new SETTINGS to all clients
    }

    /** @return the current authoritative game settings (read by GameManager at start). */
    public GameSettings getSettings() {
        return gameSettings;
    }

    /**
     * Copies every field from {@code src} into {@code dst} so the server's
     * authoritative reference stays stable (no pointer swap needed).
     */
    private void copySettings(GameSettings src, GameSettings dst) {
        dst.setPlayerBaseHp(src.getPlayerBaseHp());
        dst.setPlayerBaseSpeed(src.getPlayerBaseSpeed());
        dst.setPlayerBaseDamage(src.getPlayerBaseDamage());
        dst.setPlayerShootCooldown(src.getPlayerShootCooldown());
        dst.setPlayerHitCooldown(src.getPlayerHitCooldown());
        dst.setPlayerMaxSpeed(src.getPlayerMaxSpeed());
        dst.setPlayerMaxDamage(src.getPlayerMaxDamage());
        dst.setPlayerMinShootCooldown(src.getPlayerMinShootCooldown());
        dst.setPowerUpDropChance(src.getPowerUpDropChance());
        dst.setPowerUpSpeedBonus(src.getPowerUpSpeedBonus());
        dst.setPowerUpDamageBonus(src.getPowerUpDamageBonus());
        dst.setPowerUpCooldownBonus(src.getPowerUpCooldownBonus());
        dst.setPowerUpHpBonus(src.getPowerUpHpBonus());
        dst.setMeleeEnemySpeed(src.getMeleeEnemySpeed());
        dst.setRangedEnemySpeed(src.getRangedEnemySpeed());
        dst.setSemiBossEnemySpeed(src.getSemiBossEnemySpeed());
        dst.setRangedEnemyShootCooldown(src.getRangedEnemyShootCooldown());
        dst.setTotalRounds(src.getTotalRounds());
        dst.setEnemySpawnCooldown(src.getEnemySpawnCooldown());
    }

    private void clearLobbySlot(int playerId) {
        if (!isValidPlayerId(playerId)) {
            return;
        }

        lobbyPlayerNames[playerId] = null;
        lobbyReadyFlags[playerId] = false;
        joinSequence[playerId] = -1; // free the join-order stamp

        if (hostPlayerId == playerId) {
            int newHost = findNextConnectedPlayerId();
            hostPlayerId = newHost;
            if (newHost != -1) {
                // Announce host migration so every client's LobbyScreen can update
                System.out.println("[Server] Host migrated to Player " + (newHost + 1));
                broadcastSystemMessage("Player " + (newHost + 1) + " is now the host.");
            }
        }

        broadcastLobbyState();
    }

    /**
     * Finds the remaining player who joined this lobby session the earliest.
     * We compare {@code joinSequence[]} values (stamped in {@link #markPlayerConnected})
     * rather than raw slot indices, so host migration is correct even after
     * slot IDs are reused following earlier disconnections.
     *
     * @return the playerId of the earliest remaining joiner, or -1 if the lobby is empty.
     */
    private int findNextConnectedPlayerId() {
        int bestId  = -1;
        int bestSeq = Integer.MAX_VALUE;

        for (int i = 0; i < Constants.MAX_PLAYERS; i++) {
            if (lobbyPlayerNames[i] != null && joinSequence[i] >= 0 && joinSequence[i] < bestSeq) {
                bestSeq = joinSequence[i];
                bestId  = i;
            }
        }

        return bestId;
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
