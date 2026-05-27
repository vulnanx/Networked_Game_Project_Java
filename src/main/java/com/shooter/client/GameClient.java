package com.shooter.client;

import javax.swing.JFrame;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.function.Consumer;

import com.shooter.client.screens.MainMenuScreen;
import com.shooter.client.screens.PauseScreen;
import com.shooter.client.screens.GameOverScreen;
import com.shooter.network.GameOverStats;
import com.shooter.network.InputSnapshot;
import com.shooter.network.LobbyState;
import com.shooter.network.NetworkMessage;
import com.shooter.network.MessageType;
import com.shooter.shared.model.GameState;
import com.shooter.shared.model.Player;
import com.shooter.shared.util.Constants;
import com.shooter.shared.util.GameSettings;
import com.shooter.server.EntityManager;
import com.shooter.network.ChatMessage;

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
    private volatile Consumer<LobbyState> lobbyStateListener;
    private volatile LobbyState lastLobbyState;
    private volatile Consumer<Boolean> pauseStateListener;
    private volatile Consumer<GameOverStats> gameOverListener;
    private volatile Runnable gameStartListener;
    private volatile Consumer<EntityManager.PowerUpCollection> powerUpCollectedListener;
    private volatile Consumer<Integer> roundStartListener;
    private volatile Runnable roundClearListener;
    private ChatPanel chatPanel;
    private volatile Consumer<ChatMessage> chatMessageListener;
    private volatile Consumer<GameSettings> settingsListener;
    private volatile GameSettings lastKnownSettings = new GameSettings(); // starts with defaults

    /** Human-readable reason for the last failed connection attempt (null = no error / not yet tried). */
    private String lastConnectionError;

    public GameClient() {
        chatPanel = new ChatPanel(this);
    }

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
        lastConnectionError = null; // reset on every attempt
        try {
            System.out.println("Connecting to server at " + host + ":" + Constants.SERVER_PORT + "...");

            socket = new Socket();
            // Implement a 2500ms timeout to prevent the game UI from hanging indefinitely
            socket.connect(new java.net.InetSocketAddress(host, Constants.SERVER_PORT), 2500);

            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();

            in = new ObjectInputStream(socket.getInputStream());

            NetworkMessage welcome = (NetworkMessage) in.readObject();

            if (welcome.getType() == MessageType.REJECTED) {
                // Server explicitly denied us — read the reason and bail out cleanly
                String reason = (welcome.getPayload() instanceof String)
                        ? (String) welcome.getPayload()
                        : "Connection rejected by server.";
                lastConnectionError = reason;
                System.err.println("[Client] Connection rejected: " + reason);
                socket.close();
                socket = null;
                return false;
            }

            if (welcome.getType() == MessageType.CONNECTED) {
                myPlayerId = welcome.getPlayerId();
                System.out.println("Connected! Assigned Player ID: " + myPlayerId);
            }

            sendMessage(new NetworkMessage(MessageType.PING, myPlayerId, null));
            System.out.println("Ping sent to server.");

            return true;

        } catch (IOException e) {
            lastConnectionError = "No server found at " + host + ". Make sure the host has started the game.";
            System.err.println("Could not connect to server: " + e.getMessage());
            return false;
        } catch (ClassNotFoundException e) {
            lastConnectionError = "Unexpected response from server.";
            System.err.println("Unexpected message from server: " + e.getMessage());
            return false;
        }
    }

    /**
     * Sends a NetworkMessage to the server.
     *
     * @param message the message to send
     */
    public synchronized void sendMessage(NetworkMessage message) {
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
                if (message.getPayload() instanceof LobbyState) {
                    lastLobbyState = (LobbyState) message.getPayload();
                    if (lobbyStateListener != null) {
                        lobbyStateListener.accept(lastLobbyState);
                    }
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

            case POWER_UP_COLLECTED:
                if (message.getPayload() instanceof EntityManager.PowerUpCollection && powerUpCollectedListener != null) {
                    powerUpCollectedListener.accept((EntityManager.PowerUpCollection) message.getPayload());
                }
                break;

            case ROUND_START:
                System.out.println("[Client] Round started: " + message.getPayload());
                if (message.getPayload() instanceof Integer && roundStartListener != null) {
                    roundStartListener.accept((Integer) message.getPayload());
                }
                break;

            case START_GAME:
                System.out.println("[Client] Game starting!");
                if (gameStartListener != null) {
                    gameStartListener.run();
                }
                break;

            case ROUND_CLEAR:
                System.out.println("[Client] Round cleared!");
                if (roundClearListener != null) {
                    roundClearListener.run();
                }
                break;

            case CHAT:
                if (message.getPayload() instanceof ChatMessage) {
                    ChatMessage chatMsg = (ChatMessage) message.getPayload();
                    if (chatMessageListener != null) {
                        chatMessageListener.accept(chatMsg);
                    }
                }
                break;

            case SETTINGS:
                if (message.getPayload() instanceof GameSettings) {
                    lastKnownSettings = (GameSettings) message.getPayload();
                    final GameSettings snap = lastKnownSettings;
                    final Consumer<GameSettings> sl = settingsListener;
                    if (sl != null) {
                        // Always dispatch on the EDT so screens don't need
                        // to wrap their listener in invokeLater themselves.
                        javax.swing.SwingUtilities.invokeLater(() -> sl.accept(snap));
                    }
                }
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

        // Fallback: if GAME_STATE arrives while the lobby is still showing
        // (START_GAME was missed or arrived before onEnter set the listener),
        // fire the listener now so the screen always clears.
        Runnable listener = gameStartListener;
        if (listener != null) {
            gameStartListener = null; // consume it — fire exactly once
            javax.swing.SwingUtilities.invokeLater(() -> {
                System.out.println("[GameClient] GAME_STATE fallback — clearing screen.");
                listener.run();
            });
        }
    }

    /** @return this client's assigned player ID (0-3), or -1 if not yet connected. */
    public int getMyPlayerId() {
        return myPlayerId;
    }

    /**
     * Returns the human-readable reason the last {@link #connectToServer} call failed,
     * or {@code null} if the connection succeeded or hasn't been attempted yet.
     */
    public String getLastConnectionError() {
        return lastConnectionError;
    }

    /** @return the active game state template */
    public GameState getGameState() {
        return renderState;
    }

    /**
     * Lets UI code receive lobby updates without reading from the socket itself.
     *
     * @param listener called whenever a LOBBY_STATE message arrives
     */
    public void setLobbyStateListener(Consumer<LobbyState> listener) {
        this.lobbyStateListener = listener;
        if (listener != null && lastLobbyState != null) {
            listener.accept(lastLobbyState);
        }
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

    /**
     * Sends a pause toggle request to the server.
     * The server decides whether to pause or unpause.
     */
    public void sendPauseRequest() {
        sendMessage(new NetworkMessage(MessageType.PAUSE, myPlayerId, null));
    }

    /**
     * Sends a request to start the game.
     * Only works if the client is the host and all players are ready.
     */
    public void sendStartGameRequest() {
        sendMessage(new NetworkMessage(MessageType.START_GAME, myPlayerId, null));
    }

    /**
     * Sets a listener for the game start event.
     * @param listener called when the game starts
     */
    public void setGameStartListener(Runnable listener) {
        this.gameStartListener = listener;
    }

    public ChatPanel getChatPanel() {
        return chatPanel;
    }

    public void setChatMessageListener(Consumer<ChatMessage> listener) {
        this.chatMessageListener = listener;
    }

    public void setPowerUpCollectedListener(Consumer<EntityManager.PowerUpCollection> listener) {
        this.powerUpCollectedListener = listener;
    }

    public void setRoundStartListener(Consumer<Integer> listener) {
        this.roundStartListener = listener;
    }

    public void setRoundClearListener(Runnable listener) {
        this.roundClearListener = listener;
    }

    /**
     * Registers a listener that is called whenever the server broadcasts new
     * {@link GameSettings}. Use this in the lobby UI to refresh the settings panel.
     *
     * @param listener called with the new settings; may be null to unregister
     */
    public void setSettingsListener(Consumer<GameSettings> listener) {
        this.settingsListener = listener;
        // Fire immediately so the UI is up-to-date even if no server message has arrived yet
        if (listener != null && lastKnownSettings != null) {
            listener.accept(lastKnownSettings);
        }
    }

    /**
     * Sends updated game settings to the server.
     * The server will validate that this client is the host before applying.
     *
     * @param settings the new settings to apply
     */
    public void sendSettings(GameSettings settings) {
        sendMessage(new NetworkMessage(MessageType.SETTINGS, myPlayerId, settings));
    }

    /**
     * @return the most recent {@link GameSettings} received from the server,
     *         or default settings if none have been received yet.
     */
    public GameSettings getLastKnownSettings() {
        return lastKnownSettings;
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
        } finally {
            myPlayerId = -1;
            lastLobbyState = null;
            serverListenerThread = null;
        }
    }

    /**
     * Launches the game window starting at the Main Menu.
     * The MainMenuScreen handles Host/Join/Exit and wires up
     * server connections before transitioning to LobbyScreen.
     */
    public static void main(String[] args) {
        // Create the networking client (not connected yet — MainMenuScreen handles that)
        GameClient client = new GameClient();

        // Game state starts empty — players are added once the game begins
        GameState gameState = new GameState();

        // Create the rendering panel and screen manager
        GamePanel panel = new GamePanel(gameState, client);
        ScreenManager screenManager = new ScreenManager(panel);
        panel.setScreenManager(screenManager);

        // Start at the Main Menu (Host / Join / Exit)
        screenManager.setScreen(
                new MainMenuScreen(screenManager, client, gameState));

        // ── Wire network listeners for screen transitions ──────────────────

        // When the server broadcasts pause state, show or hide PauseScreen
        // Uses setScreenImmediate() — pause should feel instant, not delayed by fade
        client.setPauseStateListener(isPaused -> {
            javax.swing.SwingUtilities.invokeLater(() -> {
                if (isPaused) {
                    screenManager.setScreenImmediate(new PauseScreen(
                            screenManager,
                            () -> client.sendPauseRequest(),
                            () -> {
                                client.disconnect();
                                screenManager.setScreen(new MainMenuScreen(screenManager, client, gameState));
                            }
                    ));
                } else {
                    screenManager.setScreenImmediate(null);
                }
            });
        });

        // When the server says the game is starting, activate gameplay view
        client.setGameStartListener(() ->
            javax.swing.SwingUtilities.invokeLater(panel::activateGameplay)
        );

        // When the server sends game over stats, show the GameOverScreen
        client.setGameOverListener(stats -> {
            javax.swing.SwingUtilities.invokeLater(() -> {
                int[] killsArray = new int[Constants.MAX_PLAYERS];
                if (stats.getPlayerKills() != null) {
                    for (var entry : stats.getPlayerKills().entrySet()) {
                        if (entry.getKey() >= 0 && entry.getKey() < killsArray.length) {
                            killsArray[entry.getKey()] = entry.getValue();
                        }
                    }
                }
                screenManager.setScreen(new GameOverScreen(
                        screenManager,
                        stats.isVictory(),
                        stats.getRoundsCleared(),
                        stats.getTotalKills(),
                        killsArray,
                        () -> {
                            client.disconnect();
                            screenManager.setScreen(
                                    new MainMenuScreen(screenManager, client, gameState));
                        }
                ));
            });
        });

        // Build and show the window
        JFrame window = new JFrame(Constants.WINDOW_TITLE);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(false);
        window.add(panel);
        window.pack();
        window.setLocationRelativeTo(null);
        window.setVisible(true);

        // Start the client-side game loop (renders screens + gameplay)
        panel.startGameLoop();
    }
}
