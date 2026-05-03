package com.shooter.server;

import com.shooter.shared.logic.RoundConfig;
import com.shooter.shared.model.Enemy;
import com.shooter.shared.util.Constants;

import java.util.List;

public class RoundManager {

    private int currentRound = 1;
    private EnemySpawner enemySpawner = new EnemySpawner();

    private List<Enemy> enemiesToSpawn;
    private int nextSpawnIndex = 0;

    private int spawnTimer = 0;
    private int spawnInterval = Constants.ENEMY_SPAWN_COOLDOWN;

    private int totalEnemiesThisRound = 0;
    private int killedEnemies = 0;

    public void startCurrentRound(EntityManager entityManager) {
        enemiesToSpawn = enemySpawner.spawnEnemiesForRound(currentRound);

        nextSpawnIndex = 0;
        spawnTimer = 0;
        killedEnemies = 0;
        totalEnemiesThisRound = enemiesToSpawn.size();

        System.out.println("Round " + currentRound + " started.");
        System.out.println("Total enemies this round: " + totalEnemiesThisRound);
    }

    public void updateSpawning(EntityManager entityManager) {
        if (enemiesToSpawn == null) {
            return;
        }

        if (nextSpawnIndex >= totalEnemiesThisRound) {
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
        killedEnemies++;
    }

    public void checkAndAdvanceRound(EntityManager entityManager) {
        boolean allSpawned = nextSpawnIndex >= totalEnemiesThisRound;
        boolean noAliveEnemies = entityManager.hasNoEnemies();
        boolean allKilled = killedEnemies >= totalEnemiesThisRound;

        if (!allSpawned || !noAliveEnemies || !allKilled) {
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

    public int getKilledEnemies() {
        return killedEnemies;
    }

    public int getTotalEnemiesThisRound() {
        return totalEnemiesThisRound;
    }
}