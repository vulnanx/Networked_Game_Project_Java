package com.shooter.shared.logic;

/**
 * Stores round configurations.
 *
 * NOT REQUIRED for Day 1.
 */
public class RoundConfig {

    public static int getEnemyCount(int round) {
        switch (round) {
            case 1: return 20;
            case 2: return 40;
            case 3: return 60;
            default: return 0;
        }
    }
}