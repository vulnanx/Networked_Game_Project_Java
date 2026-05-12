package com.shooter.network;

import com.shooter.shared.util.Direction;
import java.io.Serializable;

/**
 * ============================================================
 * FILE: InputSnapshot.java
 * PACKAGE: network
 * OWNER: Member B (Sophia)
 * ============================================================
 * 
 * RESPONSIBILITY:
 * Represents a "snapshot" of a player's keyboard state at a specific moment.
 * This is sent from Client -> Server to request movement or shooting.
 * 
 * WHY USE THIS:
 * Instead of sending "Move Left" then "Move Up" as separate messages,
 * we send the entire state of the WASD keys. This prevents "input lag"
 * and ensures the server sees exactly what the player is holding.
 * ============================================================
 */
public class InputSnapshot implements Serializable {

    private static final long serialVersionUID = 1L;

    // Movement keys
    public final boolean up;
    public final boolean down;
    public final boolean left;
    public final boolean right;

    // Action keys
    public final boolean shooting;

    // The direction the player was facing when this snapshot was taken
    public final Direction facing;

    public InputSnapshot(boolean up, boolean down, boolean left, boolean right, boolean shooting, Direction facing) {
        this.up = up;
        this.down = down;
        this.left = left;
        this.right = right;
        this.shooting = shooting;
        this.facing = facing;
    }

    @Override
    public String toString() {
        return String.format("Input[U:%b, D:%b, L:%b, R:%b, Shoot:%b, Face:%s]",
                up, down, left, right, shooting, facing);
    }
}
