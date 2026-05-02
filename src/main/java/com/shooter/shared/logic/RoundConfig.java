package com.shooter.shared.logic;

/**
 * ============================================================
 * FILE: RoundConfig.java
 * PACKAGE: shared.logic
 * OWNER: Member C (Systems)
 * ============================================================
 *
 * RESPONSIBILITY:
 *   Stores the configuration for each round:
 *   - number of melee enemies
 *   - number of ranged enemies
 *   - number of semi-boss enemies
 *   - base HP of enemies
 *
 *   This class acts as the "difficulty table" of the game.
 *
 * WHAT TO ADD HERE:
 *   - Additional difficulty scaling (e.g., speed increase per round)
 *   - Boss-specific stats or modifiers
 *   - Future balancing changes
 *
 * WHAT NOT TO PUT HERE:
 *   - Enemy spawning logic (belongs in EnemySpawner.java)
 *   - Round progression logic (belongs in RoundManager.java)
 *   - Rendering or drawing code
 *
 * CONNECTS TO:
 *   EnemySpawner.java → uses this to spawn correct enemies
 *   RoundManager.java → uses this to determine current round difficulty
 *
 * ============================================================
 */
public class RoundConfig {

    private int meleeCount;
    private int rangedCount;
    private int semiBossCount;
    private int enemyHp;

    public RoundConfig(int meleeCount, int rangedCount, int semiBossCount, int enemyHp) {
        this.meleeCount = meleeCount;
        this.rangedCount = rangedCount;
        this.semiBossCount = semiBossCount;
        this.enemyHp = enemyHp;
    }

    /**
     * Returns the configuration for a specific round.
     *
     * @param round current round number (1–5)
     * @return RoundConfig object containing enemy counts and HP
     */
    public static RoundConfig getConfig(int round) {
        switch (round) {
            case 1: return new RoundConfig(20, 0, 0, 1);
            case 2: return new RoundConfig(30, 10, 0, 3);
            case 3: return new RoundConfig(40, 20, 3, 10);
            case 4: return new RoundConfig(50, 30, 10, 20);
            case 5: return new RoundConfig(60, 40, 20, 50);
            default: return new RoundConfig(0, 0, 0, 0);
        }
    }

    public int getMeleeCount() { return meleeCount; }
    public int getRangedCount() { return rangedCount; }
    public int getSemiBossCount() { return semiBossCount; }
    public int getEnemyHp() { return enemyHp; }
}