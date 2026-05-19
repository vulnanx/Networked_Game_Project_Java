package com.shooter.client;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.function.Consumer;

import com.shooter.network.GameOverStats;
import com.shooter.network.InputSnapshot;
import com.shooter.network.LobbyState;
import com.shooter.network.NetworkMessage;
import com.shooter.network.MessageType;
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
 * In Milestone 2, also connects to the GameServer.
 *
 * WHAT TO ADD HERE:
 * - Menu / Lobby UI (Milestone 2 - Day 2)
 * - Player login / name input
 *
 * WHAT NOT TO PUT HERE:
 * - Game logic (no movement, no collision)
 * - Rendering logic (belongs in GamePanel)
 * - Networking handling logic (belongs in ClientHandler/network/)
 *
 * CONNECTS TO:
 * GamePanel (renders everything)
 * GameState (holds game data)
 * GameServer (TCP connection for Milestone 2)
 * ============================================================
 */
public class GameClient {

    // ── Networking fields ────────────────────────────────────────
    private Socket socket;           // Our TCP connection to the server
    private ObjectOutputStream out;  // We send messages through this
    private ObjectInputStream in;    // We receive messages through this
    private int myPlayerId = -1;     // Assigned by server after connection (-1 = not yet assigned)

    private Thread serverListenerThread;
    private GameState renderState;
    private Consumer<LobbyState> lobbyStateListener;
    private Consumer<Boolean> pauseStateListener;
    private Consumer<GameOverStats> gameOverListener;

    /**
     * Connects to the game server at the given host address.
     * Opens the socket, negotiates streams, reads the CONNECTED
     * welcome message, and sends a PING to verify the link.
     *
     * IMPORTANT: ObjectOutputStream must be opened BEFORE ObjectInputStream
     * on both the client AND the server, or they deadlock.
     *
     * @param host the server IP address (e.g. "localhost" or "192.168.1.5")
     * @return true if connection succeeded, false if it failed
     */
    public boolean connectToServer(String host) {
        try {
            System.out.println("Connecting to server at " + host + ":" + Constants.SERVER_PORT + "...");

            socket = new Socket(host, Constants.SERVER_PORT);

            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();

            in = new ObjectInputStream(socket.getInputStream());

            NetworkMessage welcome = (NetworkMessage) in.readObject();
            if (welcome.getType() == MessageType.CONNECTED) {
                myPlayerId = welcome.getPlayerId();
                System.out.println("Connected! Assigned Player ID: " + myPlayerId);
            }

            sendMessage(new NetworkMessage(MessageType.PING, myPlayerId, null));
            System.out.println("Ping sent to server.");

            return true;

        } catch (IOException e) {
            System.err.println("Could not connect to server: " + e.getMessage());
            return false;
        } catch (ClassNotFoundException e) {
            System.err.println("Unexpected message from server: " + e.getMessage());
            return false;
        }
    }

    /**
     * Sends a NetworkMessage to the server.
     *
     * @param message the message to send
     */
    public void sendMessage(NetworkMessage message) {
        if (out == null) {
            return;
        }

        try {
            out.writeObject(message);
            out.flush();
        } catch (IOException e) {
            System.err.println("Failed to send message: " + e.getMessage());
        }
    }

    /**
     * Sends this client's current input to the server.
     * The server will decide how that input changes the real game state.
     *
     * @param snapshot the keys and actions currently pressed by this client
     */
    public void sendInputSnapshot(InputSnapshot snapshot) {
        if (!isConnectedToServer()) {
            return;
        }

        sendMessage(new NetworkMessage(MessageType.INPUT, myPlayerId, snapshot));
    }

    /** @return true when this client has an active server connection. */
    public boolean isConnectedToServer() {
        return socket != null && socket.isConnected() && !socket.isClosed() && myPlayerId >= 0;
    }

    /**
     * Starts a background thread that receives server messages.
     * The client uses GAME_STATE snapshots for rendering and LOBBY_STATE
     * snapshots for lobby UI updates.
     *
     * @param gameState the local render state to update from server snapshots
     */
    public void startListeningForServer(GameState gameState) {
        if (!isConnectedToServer() || serverListenerThread != null) {
            return;
        }

        renderState = gameState;
        serverListenerThread = new Thread(this::listenForServerMessages);
        serverListenerThread.setName("ServerListener-" + myPlayerId);
        serverListenerThread.setDaemon(true);
        serverListenerThread.start();
    }

    private void listenForServerMessages() {
        try {
            while (isConnectedToServer()) {
                NetworkMessage message = (NetworkMessage) in.readObject();
                handleServerMessage(message);
            }
        } catch (IOException e) {
            System.err.println("Lost connection to server: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.err.println("Unexpected server message: " + e.getMessage());
        }
    }

    private void handleServerMessage(NetworkMessage message) {
        switch (message.getType()) {
            case GAME_STATE:
                applyGameStatePayload(message.getPayload());
                break;

            case LOBBY_STATE:
                if (message.getPayload() instanceof LobbyState && lobbyStateListener != null) {
                    lobbyStateListener.accept((LobbyState) message.getPayload());
                }
                break;

            case PAUSE:
                if (message.getPayload() instanceof Boolean && pauseStateListener != null) {
                    pauseStateListener.accept((Boolean) message.getPayload());
                }
                break;

            case GAME_OVER:
                if (message.getPayload() instanceof GameOverStats && gameOverListener != null) {
                    gameOverListener.accept((GameOverStats) message.getPayload());
                }
                break;

            case ROUND_START:
                System.out.println("[Client] Round started: " + message.getPayload());
                break;

            case ROUND_CLEAR:
                System.out.println("[Client] Round cleared!");
                break;

            default:
                System.out.println("[Client] Unhandled message: " + message.getType());
                break;
        }
    }

    private void applyGameStatePayload(Object payload) {
        if (!(payload instanceof GameState) || renderState == null) {
            return;
        }

        GameState authoritativeState = (GameState) payload;

        renderState.setPlayers(authoritativeState.getPlayers());
        renderState.setBullets(authoritativeState.getBullets());
        renderState.setEnemies(authoritativeState.getEnemies());
        renderState.setPowerUps(authoritativeState.getPowerUps());
        renderState.setCurrentRound(authoritativeState.getCurrentRound());
        renderState.setKilledEnemies(authoritativeState.getKilledEnemies());
        renderState.setTotalEnemiesThisRound(authoritativeState.getTotalEnemiesThisRound());
    }

    /** @return this client's assigned player ID (0-3), or -1 if not yet connected. */
    public int getMyPlayerId() {
        return myPlayerId;
    }

    /**
     * Lets UI code receive lobby updates without reading from the socket itself.
     *
     * @param listener called whenever a LOBBY_STATE message arrives
     */
    public void setLobbyStateListener(Consumer<LobbyState> listener) {
        this.lobbyStateListener = listener;
    }

    /**
     * Sets a listener for server pause state changes.
     * @param listener called with true when paused, false when resumed
     */
    public void setPauseStateListener(Consumer<Boolean> listener) {
        this.pauseStateListener = listener;
    }

    /**
     * Sets a listener for game over events from the server.
     * @param listener called with game over stats
     */
    public void setGameOverListener(Consumer<GameOverStats> listener) {
        this.gameOverListener = listener;
    }

    /**
     * Sends this client's ready status to the server.
     *
     * @param ready true when this player is ready in the lobby
     */
    public void sendReadyStatus(boolean ready) {
        sendMessage(new NetworkMessage(MessageType.READY_STATUS, myPlayerId, ready));
    }

    /** Closes the server connection cleanly. */
    public void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                sendMessage(new NetworkMessage(MessageType.DISCONNECT, myPlayerId, null));
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }

    /**
     * Launches the game window and (for Milestone 2) connects to the server.
     * The Milestone 1 single-player code is kept intact below.
     */
    public static void main(String[] args) {

        String host = JOptionPane.showInputDialog(
                null,
                "Enter server IP address\n(leave blank or cancel for single-player / localhost):",
                "Holy Shot! - Connect",
                JOptionPane.QUESTION_MESSAGE);

        GameClient client = new GameClient();

        if (host != null && !host.isBlank()) {
            boolean connected = client.connectToServer(host.trim());
            if (!connected) {
                JOptionPane.showMessageDialog(null,
                        "Could not connect to " + host + ".\nStarting in single-player mode.",
                        "Connection Failed", JOptionPane.WARNING_MESSAGE);
            }
        } else {
            System.out.println("Starting in single-player mode (no server).");
        }

        JFrame window = new JFrame(Constants.WINDOW_TITLE);

        GameState gameState = new GameState();
        Player player = new Player(0, "Player 1");
        gameState.addPlayer(player);

        GamePanel panel = new GamePanel(gameState, client);
        client.startListeningForServer(gameState);

        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(false);
        window.add(panel);
        window.pack();
        window.setLocationRelativeTo(null);
        window.setVisible(true);

        panel.startGameLoop();
    }
}
