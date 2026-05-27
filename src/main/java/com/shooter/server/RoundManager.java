package com.shooter.server;

import com.shooter.shared.logic.RoundConfig;
import com.shooter.shared.model.Enemy;
import com.shooter.shared.util.Constants;
import com.shooter.shared.util.GameSettings;

import java.util.List;

public class RoundManager {

    private final GameSettings settings;
    private int currentRound = 1;
    private EnemySpawner enemySpawner;

    private List<Enemy> enemiesToSpawn;
    private int nextSpawnIndex = 0;

    private int spawnTimer = 0;
    private int spawnInterval; 

    private int totalEnemiesThisRound = 0;
    private int killedEnemies = 0;
    private boolean roundJustStarted = false;
    private boolean roundJustCleared = false;
    private boolean allRoundsCleared = false;

    public RoundManager(GameSettings settings) {
        this.settings = (settings != null) ? settings : new GameSettings();
        this.enemySpawner = new EnemySpawner(this.settings);
    }

    /**
     * Describes what happened after checking round progress.
     * GameManager can use this later to send ROUND_START, ROUND_CLEAR,
     * or GAME_OVER messages to every client.
     */
    public enum RoundTransition {
        NONE,
        ROUND_STARTED,
        GAME_COMPLETED
    }

    /**
     * Small read-only bundle for synchronized HUD data.
     * This lets server/client integration code copy round progress into the UI
     * without asking the HUD to know how rounds are calculated.
     */
    public static class RoundHudSnapshot {
        private final int currentRound;
        private final int killedEnemies;
        private final int totalEnemiesThisRound;

        public RoundHudSnapshot(int currentRound, int killedEnemies, int totalEnemiesThisRound) {
            this.currentRound = currentRound;
            this.killedEnemies = killedEnemies;
            this.totalEnemiesThisRound = totalEnemiesThisRound;
        }

        public int getCurrentRound() {
            return currentRound;
        }

        public int getKilledEnemies() {
            return killedEnemies;
        }

        public int getTotalEnemiesThisRound() {
            return totalEnemiesThisRound;
        }
    }

    public void startCurrentRound(EntityManager entityManager) {
        enemiesToSpawn = enemySpawner.spawnEnemiesForRound(currentRound);

        nextSpawnIndex = 0;
        spawnTimer = 0;
        killedEnemies = 0;
        totalEnemiesThisRound = enemiesToSpawn.size();
        roundJustStarted = true;
        roundJustCleared = false;

        // Spawn faster each round — reduce interval by ENEMY_SPAWN_COOLDOWN_REDUCTION per round.
        spawnInterval = Math.max(
                Constants.ENEMY_SPAWN_COOLDOWN_MIN,
                settings.getEnemySpawnCooldown() - (currentRound - 1) * Constants.ENEMY_SPAWN_COOLDOWN_REDUCTION
        );

        System.out.println("Round " + currentRound + " started. Spawn interval: " + spawnInterval + " ticks.");
        System.out.println("Total enemies this round: " + totalEnemiesThisRound);
    }

    public void updateSpawning(EntityManager entityManager) {
        if (enemiesToSpawn == null) {
            return;
        }

        if (nextSpawnIndex >= totalEnemiesThisRound) {
            return;
        }

        // Performance: don't spawn if the arena is already at the alive-enemy cap.
        // The spawn timer keeps ticking so the enemy appears as soon as space clears.
        if (entityManager.getEnemies().size() >= Constants.MAX_ENEMIES_ALIVE) {
            return;
        }

        if (spawnTimer > 0) {
            spawnTimer--;
            return;
        }

        Enemy enemy = enemiesToSpawn.get(nextSpawnIndex);
        entityManager.addEnemy(enemy);

        nextSpawnIndex++;
        spawnTimer = spawnInterval;
    }

    public void addKill() {
        if (killedEnemies >= totalEnemiesThisRound) {
            return;
        }
        killedEnemies++;
    }

    public RoundTransition checkAndAdvanceRound(EntityManager entityManager) {
        boolean allSpawned = nextSpawnIndex >= totalEnemiesThisRound;
        boolean noAliveEnemies = entityManager.hasNoEnemies();
        boolean allKilled = killedEnemies >= totalEnemiesThisRound;

        if (!allSpawned || !noAliveEnemies || !allKilled) {
            return RoundTransition.NONE;
        }

        roundJustCleared = true;

        if (currentRound < settings.getTotalRounds()) {
            currentRound++;
            startCurrentRound(entityManager);
            return RoundTransition.ROUND_STARTED;
        } else {
            allRoundsCleared = true;
            System.out.println("All rounds cleared!");
            return RoundTransition.GAME_COMPLETED;
        }
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public int getKilledEnemies() {
        return killedEnemies;
    }

    public int getTotalEnemiesThisRound() {
        return totalEnemiesThisRound;
    }

    public boolean hasRoundJustStarted() {
        return roundJustStarted;
    }

    public boolean hasRoundJustCleared() {
        return roundJustCleared;
    }

    public boolean areAllRoundsCleared() {
        return allRoundsCleared;
    }

    /**
     * Clears one-tick transition flags after GameManager has broadcast them.
     */
    public void clearTransitionFlags() {
        roundJustStarted = false;
        roundJustCleared = false;
    }

    /**
     * Builds the round data the HUD needs: current round and enemy progress.
     */
    public RoundHudSnapshot getHudSnapshot() {
        return new RoundHudSnapshot(currentRound, killedEnemies, totalEnemiesThisRound);
    }
}
