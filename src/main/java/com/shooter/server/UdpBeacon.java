package com.shooter.server;

import com.shooter.shared.util.Constants;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * ============================================================
 * FILE: UdpBeacon.java
 * PACKAGE: server
 * ============================================================
 *
 * Broadcasts the server's presence over UDP every 2 seconds so
 * clients on the same LAN can discover it without manually entering
 * an IP address.
 *
 * PAYLOAD FORMAT (UTF-8):
 *   "HOLY_SHOT|{playerCount}/{maxPlayers}|{serverName}"
 *   Example: "HOLY_SHOT|2/4|Holy Shot!"
 *
 * The client reads the sender address from the datagram packet
 * (DatagramPacket.getAddress()) so no IP needs to be encoded.
 *
 * LIFECYCLE:
 *   - Started by GameServer.start() before accepting TCP connections.
 *   - Stopped by GameServer.startGame() once the host starts the match
 *     (full/in-progress servers should not appear in the browser).
 * ============================================================
 */
public class UdpBeacon implements Runnable {

    private volatile boolean running = true;
    private final GameServer server;

    public UdpBeacon(GameServer server) {
        this.server = server;
    }

    /** Signals the beacon loop to exit cleanly. */
    public void stop() {
        running = false;
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true);
            InetAddress broadcast = InetAddress.getByName("255.255.255.255");

            System.out.println("[UdpBeacon] Broadcasting on port "
                    + Constants.UDP_BEACON_PORT + " every 2s.");

            while (running) {
                int count = server.getConnectedPlayerCount();

                // Don't advertise an empty lobby — skip broadcast
                if (count > 0) {
                    String payload = "HOLY_SHOT|" + count + "/"
                            + Constants.MAX_PLAYERS + "|Holy Shot!";
                    byte[] data = payload.getBytes("UTF-8");
                    DatagramPacket packet = new DatagramPacket(
                            data, data.length, broadcast, Constants.UDP_BEACON_PORT);
                    socket.send(packet);
                }

                Thread.sleep(2000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            if (running) {
                System.err.println("[UdpBeacon] Error: " + e.getMessage());
            }
        }
        System.out.println("[UdpBeacon] Stopped.");
    }
}
