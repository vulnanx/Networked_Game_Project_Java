package com.shooter.server;

import com.shooter.shared.logic.RoundConfig;
import com.shooter.shared.model.Enemy;
import com.shooter.shared.util.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * ============================================================
 * FILE: EnemySpawner.java
 * PACKAGE: server
 * OWNER: Member C (Systems)
 * ============================================================
 *
 * RESPONSIBILITY:
 * Creates enemies for each round and places them at arena edges.
 *
 * WHAT TO ADD HERE:
 * - Spawn patterns
 * - Different spawn behavior per enemy type
 * - Safer spawn spacing later
 *
 * WHAT NOT TO PUT HERE:
 * - Enemy movement logic
 * - Collision logic
 * - Drawing/rendering code
 *
 * CONNECTS TO:
 * RoundConfig (knows enemy count and HP per round)
 * EntityManager (receives spawned enemies)
 * RoundManager (requests spawning when a round starts)
 * ============================================================
 */
public class EnemySpawner {

    private Random random = new Random();
    private int nextEnemyId = 0;

    public List<Enemy> spawnEnemiesForRound(int round) {
        List<Enemy> enemies = new ArrayList<>();
        RoundConfig config = RoundConfig.getConfig(round);

        spawnEnemies(enemies, Enemy.Type.MELEE, config.getMeleeCount(), config.getEnemyHp());
        spawnEnemies(enemies, Enemy.Type.RANGED, config.getRangedCount(), config.getEnemyHp());
        spawnEnemies(enemies, Enemy.Type.SEMI_BOSS, config.getSemiBossCount(), config.getEnemyHp());

        return enemies;
    }

    private void spawnEnemies(List<Enemy> enemies, Enemy.Type type, int count, int hp) {
        for (int i = 0; i < count; i++) {
            float[] position = getRandomEdgePosition();

            Enemy enemy = new Enemy(
                    nextEnemyId++,
                    type,
                    position[0],
                    position[1],
                    hp
            );

            enemies.add(enemy);
        }
    }

    private float[] getRandomEdgePosition() {
        int edge = random.nextInt(4);

        float x = Constants.ARENA_X;
        float y = Constants.ARENA_Y;

        switch (edge) {
            case 0: // top
                x = Constants.ARENA_X + random.nextInt(Constants.ARENA_WIDTH);
                y = Constants.ARENA_Y;
                break;

            case 1: // bottom
                x = Constants.ARENA_X + random.nextInt(Constants.ARENA_WIDTH);
                y = Constants.ARENA_Y + Constants.ARENA_HEIGHT - Constants.ENEMY_SIZE;
                break;

            case 2: // left
                x = Constants.ARENA_X;
                y = Constants.ARENA_Y + random.nextInt(Constants.ARENA_HEIGHT);
                break;

            case 3: // right
                x = Constants.ARENA_X + Constants.ARENA_WIDTH - Constants.ENEMY_SIZE;
                y = Constants.ARENA_Y + random.nextInt(Constants.ARENA_HEIGHT);
                break;
        }

        return new float[]{x, y};
    }
}