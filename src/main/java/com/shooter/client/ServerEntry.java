package com.shooter.client;

import java.net.InetAddress;

/**
 * Represents one discovered game server found via UDP beacon.
 */
public class ServerEntry {

    public final InetAddress address;
    public final int         playerCount;
    public final int         maxPlayers;
    public final String      name;
    public volatile long     lastSeen;   // epoch ms — refreshed on every beacon

    public ServerEntry(InetAddress address, int playerCount, int maxPlayers, String name) {
        this.address     = address;
        this.playerCount = playerCount;
        this.maxPlayers  = maxPlayers;
        this.name        = name;
        this.lastSeen    = System.currentTimeMillis();
    }

    public boolean isFull() {
        return playerCount >= maxPlayers;
    }

    /** Human-readable host string (avoid slow DNS lookups — use raw IP). */
    public String getHostAddress() {
        return address.getHostAddress();
    }
}
