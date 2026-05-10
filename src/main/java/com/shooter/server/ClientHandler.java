package com.shooter.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import com.shooter.network.NetworkMessage;
import com.shooter.network.MessageType;

/**
 * ============================================================
 * FILE: ClientHandler.java
 * PACKAGE: server
 * OWNER: Member A (Networking Core)
 * ============================================================
 *
 * RESPONSIBILITY:
 * Manages the connection to ONE specific client.
 * Runs on its own thread. Reads incoming NetworkMessages
 * from the client and will send GameState updates back.
 *
 * WHY ObjectOutputStream FIRST:
 * Java's ObjectInputStream constructor blocks until it reads
 * a stream header. If both sides open their InputStream first,
 * they deadlock waiting for each other. Always open
 * ObjectOutputStream first on BOTH sides to avoid this.
 *
 * WHAT TO ADD LATER (Day 2):
 * - Process InputSnapshot messages
 * - Reference back to GameServer to broadcast state
 * ============================================================
 */
public class ClientHandler implements Runnable {

    private final Socket socket;     // The TCP connection to this specific client
    private final int playerId;      // Which player slot this client occupies (0–3)
    private final GameServer server; // Lets this handler remove itself from the active client list

    private ObjectOutputStream out;  // We write game state TO the client through this
    private ObjectInputStream in;    // We read input FROM the client through this
    private boolean disconnected = false;

    /**
     * Creates a handler for one connected client.
     *
     * @param socket   the accepted TCP socket for this player
     * @param playerId the assigned player slot (0, 1, 2, or 3)
     */
    public ClientHandler(Socket socket, int playerId, GameServer server) {
        this.socket = socket;
        this.playerId = playerId;
        this.server = server;
    }

    /**
     * Called when the thread starts.
     * Sets up the streams, sends a CONNECTED confirmation,
     * then enters a loop reading messages from the client.
     */
    @Override
    public void run() {
        try {
            // IMPORTANT: Open ObjectOutputStream FIRST to avoid deadlock
            // (see class comment above for explanation)
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush(); // Push the stream header to the client immediately

            in = new ObjectInputStream(socket.getInputStream());

            // Tell the client what player ID they were assigned
            NetworkMessage welcome = new NetworkMessage(
                MessageType.CONNECTED, playerId, "Welcome! You are Player " + playerId
            );
            sendMessage(welcome);
            server.markPlayerConnected(playerId);

            System.out.println("ClientHandler running for Player " + playerId);

            // --- Message read loop ---
            // Keep reading messages until the client disconnects
            while (true) {
                // readObject() blocks until a message arrives
                NetworkMessage message = (NetworkMessage) in.readObject();
                handleMessage(message);
            }

        } catch (IOException e) {
            // This fires when the client disconnects (socket closes)
            System.out.println("Player " + playerId + " disconnected.");
        } catch (ClassNotFoundException e) {
            // Fires if we receive an object we don't recognize
            System.err.println("Unknown message type from Player " + playerId + ": " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    /**
     * Processes one incoming message from the client.
     * More cases will be added in Day 2 (INPUT, PING, etc.)
     *
     * @param message the received NetworkMessage
     */
    private void handleMessage(NetworkMessage message) {
        switch (message.getType()) {
            case PING:
                System.out.println("Ping from Player " + playerId);
                // TODO (Day 2): Send PONG back
                break;
            case READY_STATUS:
                if (message.getPayload() instanceof Boolean) {
                    server.updateReadyStatus(playerId, (Boolean) message.getPayload());
                }
                break;
            case DISCONNECT:
                System.out.println("Player " + playerId + " sent DISCONNECT.");
                disconnect();
                break;
            default:
                System.out.println("Message from Player " + playerId + ": " + message.getType());
                break;
        }
    }

    /**
     * Sends a NetworkMessage to this client.
     * Used by GameServer/GameManager to broadcast state.
     *
     * @param message the message to send
     */
    public synchronized void sendMessage(NetworkMessage message) {
        if (disconnected || out == null) {
            return;
        }

        try {
            out.writeObject(message);
            out.flush();
            // Reset clears cached object references — important for mutable objects
            // like GameState that are sent repeatedly
            out.reset();
        } catch (IOException e) {
            System.err.println("Failed to send to Player " + playerId + ": " + e.getMessage());
            disconnect();
        }
    }

    /** Returns the player ID assigned to this handler. */
    public int getPlayerId() {
        return playerId;
    }

    /** Closes the socket connection cleanly. */
    private synchronized void disconnect() {
        if (disconnected) {
            return;
        }

        disconnected = true;

        closeInputStream();
        closeOutputStream();
        closeSocket();

        server.removeClient(this);
    }

    /** Closes the input stream, ignoring errors caused by an already-closed socket. */
    private void closeInputStream() {
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing input stream for Player " + playerId);
        }
    }

    /** Closes the output stream, ignoring errors caused by an already-closed socket. */
    private void closeOutputStream() {
        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing output stream for Player " + playerId);
        }
    }

    /** Closes the socket itself after the streams are closed. */
    private void closeSocket() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing socket for Player " + playerId);
        }
    }
}
