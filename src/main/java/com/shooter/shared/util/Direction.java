package com.shooter.shared.util;

/**
 * ============================================================
 * FILE: Direction.java
 * PACKAGE: shared.util
 * OWNER: Member B (entities) — unlikely to need changes
 * ============================================================
 *
 * RESPONSIBILITY:
 * A simple enum representing the 4 movement/facing directions.
 * Used by Player (facing direction, movement), Bullet (travel direction),
 * and RangedEnemy (shooting direction).
 *
 * WHAT TO ADD HERE:
 * - Helper methods useful for all directions (e.g. opposite(), toVector())
 * - If you add diagonal movement later, add UPLEFT, UPRIGHT, etc.
 *
 * WHAT NOT TO PUT HERE:
 * - Game logic that belongs in a specific entity class
 * - Rendering code
 *
 * CONNECTS TO:
 * Player.java, Bullet.java, Enemy.java, InputHandler.java
 * ============================================================
 */
public enum Direction {

    UP, DOWN, LEFT, RIGHT;

    /**
     * Converts this direction into a normalized movement vector.
     * Index 0 = delta X, Index 1 = delta Y.
     *
     * Example: Direction.UP.toVector() → {0, -1}
     * (In Swing, Y increases downward, so UP is negative Y)
     *
     * TODO (Member B): Use this in Bullet.update() and Player.move()
     * to calculate new positions each tick.
     */
    public int[] toVector() {
        switch (this) {
            case UP:
                return new int[] { 0, -1 };
            case DOWN:
                return new int[] { 0, 1 };
            case LEFT:
                return new int[] { -1, 0 };
            case RIGHT:
                return new int[] { 1, 0 };
            default:
                return new int[] { 0, 0 };
        }
    }

    /**
     * Returns the opposite direction.
     * Useful for knockback or reversing movement.
     */
    public Direction opposite() {
        switch (this) {
            case UP:
                return DOWN;
            case DOWN:
                return UP;
            case LEFT:
                return RIGHT;
            case RIGHT:
                return LEFT;
            default:
                return this;
        }
    }
}