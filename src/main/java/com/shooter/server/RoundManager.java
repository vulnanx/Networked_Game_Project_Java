package com.shooter.server;

import com.shooter.shared.util.Constants;

/**
 * ============================================================
 * FILE: RoundManager.java
 * PACKAGE: server
 * OWNER: Member C (Systems)
 * ============================================================
 *
 * RESPONSIBILITY:
 * Tracks round progression:
 * - current round number
 * - starting each round
 * - detecting if a round is cleared
 *
 * WHAT TO ADD HERE:
 * - Round transition delay
 * - Game clear logic after Round 5
 * - Round start announcements
 *
 * WHAT NOT TO PUT HERE:
 * - Drawing HUD text
 * - Enemy movement
 * - Keyboard input
 *
 * CONNECTS TO:
 * EnemySpawner (creates enemies)
 * EntityManager (stores spawned enemies)
 * GameManager/GameServer (will control this in networking)
 * ============================================================
 */
public class RoundManager {

    private int currentRound = 1;
    private EnemySpawner enemySpawner = new EnemySpawner();

    public void startCurrentRound(EntityManager entityManager) {
        entityManager.addEnemies(enemySpawner.spawnEnemiesForRound(currentRound));

        System.out.println("Round " + currentRound + " started.");
        System.out.println("Enemy count: " + entityManager.getEnemies().size());
    }

    public void checkAndAdvanceRound(EntityManager entityManager) {
        if (!entityManager.hasNoEnemies()) {
            return;
        }

        if (currentRound < Constants.TOTAL_ROUNDS) {
            currentRound++;
            startCurrentRound(entityManager);
        } else {
            System.out.println("All rounds cleared!");
        }
    }

    public int getCurrentRound() {
        return currentRound;
    }
}