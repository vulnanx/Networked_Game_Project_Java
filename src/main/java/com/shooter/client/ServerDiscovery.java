package com.shooter.client;

import com.shooter.shared.util.Constants;

import java.net.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * ============================================================
 * FILE: ServerDiscovery.java
 * PACKAGE: client
 * ============================================================
 *
 * Listens on UDP_BEACON_PORT for HOLY_SHOT beacon packets and
 * maintains a live list of available servers.
 *
 * THREAD SAFETY:
 *   servers is a CopyOnWriteArrayList — safe to read from the EDT
 *   (ServerBrowserScreen.update()) while this thread writes.
 *
 * LIFECYCLE:
 *   - Starts when ServerBrowserScreen.onEnter() is called.
 *   - Stops when ServerBrowserScreen.onExit() is called.
 * ============================================================
 */
public class ServerDiscovery implements Runnable {

    /** How long a server stays listed without a fresh beacon. */
    private static final long EXPIRE_MS = 6000; // 3 missed beacons

    private volatile boolean running = true;
    private final List<ServerEntry> servers = new CopyOnWriteArrayList<>();

    // ── Control ───────────────────────────────────────────────────────────────

    public void stop() {
        running = false;
    }

    // ── Query (called from EDT) ───────────────────────────────────────────────

    /**
     * Returns a snapshot of currently-visible servers, expiring stale ones.
     * Safe to call from any thread.
     */
    public List<ServerEntry> getServers() {
        long now = System.currentTimeMillis();
        // Remove stale entries (no beacon in 6s) and empty lobbies
        servers.removeIf(e -> now - e.lastSeen > EXPIRE_MS || e.playerCount == 0);
        return Collections.unmodifiableList(new ArrayList<>(servers));
    }

    /** Clears all discovered servers so the browser shows a fresh scan. */
    public void clearServers() {
        servers.clear();
    }

    // ── Listener loop ─────────────────────────────────────────────────────────

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket(Constants.UDP_BEACON_PORT)) {
            // 1-second receive timeout so we can check the running flag
            socket.setSoTimeout(1000);
            byte[] buf = new byte[256];

            System.out.println("[ServerDiscovery] Listening on port "
                    + Constants.UDP_BEACON_PORT + "...");

            while (running) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                try {
                    socket.receive(packet);
                    parsePacket(packet);
                } catch (SocketTimeoutException ignored) {
                    // Normal — loop and check running flag
                }
            }
        } catch (Exception e) {
            if (running) {
                System.err.println("[ServerDiscovery] Error: " + e.getMessage());
            }
        }
        System.out.println("[ServerDiscovery] Stopped.");
    }

    // ── Packet parsing ────────────────────────────────────────────────────────

    private void parsePacket(DatagramPacket packet) {
        try {
            String payload = new String(packet.getData(), 0,
                    packet.getLength(), "UTF-8").trim();

            if (!payload.startsWith("HOLY_SHOT|")) return;

            String[] parts = payload.split("\\|", 3);
            if (parts.length < 3) return;

            String[] counts = parts[1].split("/", 2);
            if (counts.length != 2) return;

            int playerCount = Integer.parseInt(counts[0].trim());
            int maxPlayers  = Integer.parseInt(counts[1].trim());
            String name     = parts[2];
            InetAddress addr = packet.getAddress();

            // Refresh existing entry or add new one
            for (ServerEntry entry : servers) {
                if (entry.address.equals(addr)) {
                    entry.lastSeen = System.currentTimeMillis();
                    return;
                }
            }
            servers.add(new ServerEntry(addr, playerCount, maxPlayers, name));
            System.out.println("[ServerDiscovery] Found server at "
                    + addr.getHostAddress()
                    + " [" + playerCount + "/" + maxPlayers + "]");

        } catch (Exception e) {
            // Malformed packet — ignore silently
        }
    }
}
