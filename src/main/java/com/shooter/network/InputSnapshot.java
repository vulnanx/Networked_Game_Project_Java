package com.shooter.network;

import com.shooter.shared.util.Direction;
import java.io.Serializable;

/**
 * Holds one client's input for a single network tick.
 *
 * The client creates this from the currently pressed keys, then sends it to the
 * server inside a NetworkMessage with type INPUT. The server uses it to update
 * the authoritative player position and facing direction.
 */
public class InputSnapshot implements Serializable {

    private static final long serialVersionUID = 1L;

    private final boolean upPressed;
    private final boolean downPressed;
    private final boolean leftPressed;
    private final boolean rightPressed;
    private final Direction facingDirection;
    private final boolean shooting;

    public InputSnapshot(
            boolean upPressed,
            boolean downPressed,
            boolean leftPressed,
            boolean rightPressed,
            Direction facingDirection,
            boolean shooting) {
        this.upPressed = upPressed;
        this.downPressed = downPressed;
        this.leftPressed = leftPressed;
        this.rightPressed = rightPressed;
        this.facingDirection = facingDirection;
        this.shooting = shooting;
    }

    public boolean isUpPressed() {
        return upPressed;
    }

    public boolean isDownPressed() {
        return downPressed;
    }

    public boolean isLeftPressed() {
        return leftPressed;
    }

    public boolean isRightPressed() {
        return rightPressed;
    }

    public Direction getFacingDirection() {
        return facingDirection;
    }

    public boolean isShooting() {
        return shooting;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof InputSnapshot)) return false;
        InputSnapshot o = (InputSnapshot) obj;
        return upPressed    == o.upPressed
            && downPressed  == o.downPressed
            && leftPressed  == o.leftPressed
            && rightPressed == o.rightPressed
            && shooting     == o.shooting
            && facingDirection == o.facingDirection;
    }

    @Override
    public int hashCode() {
        int h = Boolean.hashCode(upPressed);
        h = 31 * h + Boolean.hashCode(downPressed);
        h = 31 * h + Boolean.hashCode(leftPressed);
        h = 31 * h + Boolean.hashCode(rightPressed);
        h = 31 * h + Boolean.hashCode(shooting);
        h = 31 * h + (facingDirection == null ? 0 : facingDirection.hashCode());
        return h;
    }
}
