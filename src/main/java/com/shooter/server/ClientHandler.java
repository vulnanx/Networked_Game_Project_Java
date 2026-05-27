package com.shooter.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;
import com.shooter.network.InputSnapshot;
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
 * from the client and sends GameState updates back.
 *
 * ARCHITECTURE:
 * Two threads per client:
 *  - Reader thread (this Runnable): blocks on in.readObject()
 *  - Sender thread (SenderThread inner): drains the sendQueue
 *
 * WHY NON-BLOCKING SEND QUEUE:
 * out.flush() can block indefinitely if the TCP send buffer is
 * full (slow client / congested network). Doing this on the server
 * game loop thread stalls ALL players. The queue decouples the
 * game loop from I/O latency — the loop just enqueues and moves on.
 *
 * WHY out.reset() EVERY SEND:
 * ObjectOutputStream caches object references. Sending the same
 * GameState reference repeatedly would write a back-reference
 * handle (no data!) after the first send. The client would then
 * deserialize stale game state. reset() clears the cache so every
 * send is a fresh full serialization.
 *
 * WHY ObjectOutputStream FIRST:
 * Java's ObjectInputStream constructor blocks until it reads a
 * stream header. Always open ObjectOutputStream first on BOTH
 * sides to avoid deadlock.
 * ============================================================
 */
public class ClientHandler implements Runnable {

    /** Max messages queued per client before oldest are dropped. */
    private static final int QUEUE_CAPACITY = 32;

    private final Socket socket;
    private final int playerId;
    private final GameServer server;

    private ObjectOutputStream out;
    private ObjectInputStream in;
    private volatile boolean disconnected = false;
    private volatile InputSnapshot latestInput;
    private volatile boolean pauseRequested = false;

    /**
     * Non-blocking send queue. The game loop enqueues messages here;
     * SenderThread drains it and does the actual I/O.
     * Bounded at QUEUE_CAPACITY: if a client is too slow, oldest
     * GAME_STATE packets are dropped (stale data is useless anyway).
     */
    private final LinkedBlockingQueue<NetworkMessage> sendQueue =
            new LinkedBlockingQueue<>(QUEUE_CAPACITY);

    public ClientHandler(Socket socket, int playerId, GameServer server) {
        this.socket = socket;
        this.playerId = playerId;
        this.server = server;
    }

    // ── Reader thread ─────────────────────────────────────────────────────────

    @Override
    public void run() {
        try {
            // IMPORTANT: ObjectOutputStream FIRST to avoid deadlock
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();

            in = new ObjectInputStream(socket.getInputStream());

            // Start the dedicated sender thread BEFORE announcing we're connected
            Thread senderThread = new Thread(this::senderLoop,
                    "SenderThread-" + playerId);
            senderThread.setDaemon(true);
            senderThread.start();

            // Announce assignment to the client
            sendMessage(new NetworkMessage(
                    MessageType.CONNECTED, playerId,
                    "Welcome! You are Player " + playerId));
            server.markPlayerConnected(playerId);

            System.out.println("ClientHandler running for Player " + playerId);

            // Block reading input from this client
            while (!disconnected) {
                NetworkMessage message = (NetworkMessage) in.readObject();
                handleMessage(message);
            }

        } catch (IOException e) {
            System.out.println("Player " + playerId + " disconnected.");
        } catch (ClassNotFoundException e) {
            System.err.println("Unknown message from Player " + playerId
                    + ": " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    // ── Sender thread ─────────────────────────────────────────────────────────

    /**
     * Drains the sendQueue and performs the actual socket writes.
     * Running on its own thread means I/O blocking never stalls the game loop.
     */
    private void senderLoop() {
        try {
            while (!disconnected) {
                // Block until a message is available (timeout to check disconnected)
                NetworkMessage msg = sendQueue.poll(100,
                        java.util.concurrent.TimeUnit.MILLISECONDS);
                if (msg == null) continue;

                // Always reset so ObjectOutputStream doesn't send stale
                // back-references for repeatedly-sent GameState objects.
                out.reset();
                out.writeObject(msg);
                out.flush();
            }
        } catch (IOException e) {
            if (!disconnected) {
                System.err.println("[Sender] I/O error for Player "
                        + playerId + ": " + e.getMessage());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            disconnect();
        }
    }

    // ── Message handling ──────────────────────────────────────────────────────

    private void handleMessage(NetworkMessage message) {
        switch (message.getType()) {
            case PING:
                System.out.println("Ping from Player " + playerId);
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
            case INPUT:
                if (message.getPayload() instanceof InputSnapshot) {
                    latestInput = (InputSnapshot) message.getPayload();
                }
                break;
            case PAUSE:
                pauseRequested = true;
                break;
            case START_GAME:
                server.startGame();
                break;
            case CHAT:
                server.broadcastChatMessage(message);
                break;
            case SETTINGS:
                if (message.getPayload() instanceof com.shooter.shared.util.GameSettings) {
                    server.applySettings(playerId,
                            (com.shooter.shared.util.GameSettings) message.getPayload());
                }
                break;
            default:
                System.out.println("Message from Player " + playerId
                        + ": " + message.getType());
                break;
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Enqueues a message for sending. Non-blocking — the game loop never waits.
     * If the queue is full (client too slow), the oldest entry is dropped to
     * make room. Stale GAME_STATE packets are worthless anyway.
     */
    public void sendMessage(NetworkMessage message) {
        if (disconnected) return;
        if (!sendQueue.offer(message)) {
            // Queue full: drop oldest, insert newest
            sendQueue.poll();
            sendQueue.offer(message);
        }
    }

    public int getPlayerId() {
        return playerId;
    }

    public InputSnapshot getLatestInput() {
        return latestInput;
    }

    /**
     * Returns true if this client requested a pause toggle.
     * Resets the flag automatically.
     */
    public boolean pollPauseRequest() {
        if (pauseRequested) {
            pauseRequested = false;
            return true;
        }
        return false;
    }

    // ── Disconnect ────────────────────────────────────────────────────────────

    private synchronized void disconnect() {
        if (disconnected) return;
        disconnected = true;
        closeInputStream();
        closeOutputStream();
        closeSocket();
        server.removeClient(this);
    }

    private void closeInputStream() {
        try { if (in  != null) in.close();  } catch (IOException ignored) {}
    }

    private void closeOutputStream() {
        try { if (out != null) out.close(); } catch (IOException ignored) {}
    }

    private void closeSocket() {
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }
}
