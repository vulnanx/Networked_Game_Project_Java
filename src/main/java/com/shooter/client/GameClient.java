package com.shooter.client;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
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
 * - Menu / Lobby UI (Milestone 2 — Day 2)
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

            // Open the TCP socket to the server
            socket = new Socket(host, Constants.SERVER_PORT);

            // Open ObjectOutputStream FIRST (matches server-side order to avoid deadlock)
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();

            // Now open the input stream
            in = new ObjectInputStream(socket.getInputStream());

            // Read the welcome message from the server (it sends CONNECTED immediately)
            NetworkMessage welcome = (NetworkMessage) in.readObject();
            if (welcome.getType() == MessageType.CONNECTED) {
                myPlayerId = welcome.getPlayerId();
                System.out.println("Connected! Assigned Player ID: " + myPlayerId);
            }

            // Send a PING to confirm our side of the link is working
            sendMessage(new NetworkMessage(MessageType.PING, myPlayerId, null));
            System.out.println("Ping sent to server.");

            return true; // Connection successful

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
        try {
            out.writeObject(message);
            out.flush();
        } catch (IOException e) {
            System.err.println("Failed to send message: " + e.getMessage());
        }
    }

    /** @return this client's assigned player ID (0–3), or -1 if not yet connected */
    public int getMyPlayerId() {
        return myPlayerId;
    }

    /**
     * Closes the server connection cleanly.
     */
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

    // ── Entry point ──────────────────────────────────────────────

    /**
     * Launches the game window and (for Milestone 2) connects to the server.
     * The Milestone 1 single-player code is kept intact below.
     */
    public static void main(String[] args) {

        // ── Milestone 2: prompt for server IP ────────────────
        // This is a simple dialog for now; will be replaced by the
        // MainMenuScreen UI in Day 2.
        String host = JOptionPane.showInputDialog(
            null,
            "Enter server IP address\n(leave blank or cancel for single-player / localhost):",
            "Holy Shot! — Connect",
            JOptionPane.QUESTION_MESSAGE
        );

        GameClient client = new GameClient();

        if (host != null && !host.isBlank()) {
            // Multiplayer mode — try to connect
            boolean connected = client.connectToServer(host.trim());
            if (!connected) {
                JOptionPane.showMessageDialog(null,
                    "Could not connect to " + host + ".\nStarting in single-player mode.",
                    "Connection Failed", JOptionPane.WARNING_MESSAGE);
            }
        } else {
            // Single-player mode — no server needed (Milestone 1 path)
            System.out.println("Starting in single-player mode (no server).");
        }

        // ── Milestone 1: window + game loop (unchanged) ──────
        JFrame window = new JFrame(Constants.WINDOW_TITLE);

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