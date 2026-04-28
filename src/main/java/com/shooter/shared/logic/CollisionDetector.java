package com.shooter.shared.logic;

/**
 * Handles collision detection.
 *
 * NOT REQUIRED for Day 1.
 */
public class CollisionDetector {

    public static boolean isColliding(
            float aLeft, float aRight, float aTop, float aBottom,
            float bLeft, float bRight, float bTop, float bBottom) {

        return aLeft < bRight &&
               aRight > bLeft &&
               aTop < bBottom &&
               aBottom > bTop;
    }
}