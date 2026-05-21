package com.shooter.shared.util;

import java.io.Serializable;

/**
 * ============================================================
 * FILE: GameSettings.java
 * PACKAGE: shared.util
 * ============================================================
 *
 * RESPONSIBILITY:
 * Serializable, mutable snapshot of every tunable game value.
 * Defaults mirror Constants.java so the game is identical with
 * stock settings.
 *
 * The host edits a GameSettings instance in the SettingsScreen
 * before the game starts. The server broadcasts it to every
 * client via a SETTINGS NetworkMessage. GameManager, RoundManager,
 * Player, and EnemySpawner all read from this object instead of
 * Constants so changes take effect for everyone simultaneously.
 *
 * WHAT BELONGS HERE:
 * - Default values (copied from Constants at construction)
 * - Getters / setters for each tunable field
 * - A reset() method to restore defaults
 *
 * WHAT DOES NOT BELONG HERE:
 * - Game logic
 * - UI / rendering code
 * - Networking code
 * ============================================================
 */
public class GameSettings implements Serializable {

    private static final long serialVersionUID = 1L;

    // ── Player ───────────────────────────────────────────────────────────────
    private int   playerBaseHp;
    private float playerBaseSpeed;
    private int   playerBaseDamage;
    private int   playerShootCooldown;   // ticks between shots (lower = faster)
    private int   playerHitCooldown;     // invincibility frames after contact hit
    private float playerMaxSpeed;
    private int   playerMaxDamage;
    private int   playerMinShootCooldown;

    // ── Power-ups ────────────────────────────────────────────────────────────
    private float powerUpDropChance;     // 0.0 – 1.0
    private float powerUpSpeedBonus;
    private int   powerUpDamageBonus;
    private int   powerUpCooldownBonus;
    private int   powerUpHpBonus;

    // ── Enemies ───────────────────────────────────────────────────────────────
    private float meleeEnemySpeed;
    private float rangedEnemySpeed;
    private float semiBossEnemySpeed;
    private int   rangedEnemyShootCooldown;

    // ── Rounds ───────────────────────────────────────────────────────────────
    private int totalRounds;
    private int enemySpawnCooldown;       // ticks between successive spawns

    // =========================================================================
    // CONSTRUCTOR — defaults from Constants
    // =========================================================================

    public GameSettings() {
        reset();
    }

    /**
     * Restores every field to the value defined in Constants.java.
     */
    public void reset() {
        playerBaseHp           = Constants.PLAYER_BASE_HP;
        playerBaseSpeed        = Constants.PLAYER_BASE_SPEED;
        playerBaseDamage       = Constants.PLAYER_BASE_DAMAGE;
        playerShootCooldown    = Constants.PLAYER_SHOOT_COOLDOWN;
        playerHitCooldown      = Constants.PLAYER_HIT_COOLDOWN;
        playerMaxSpeed         = Constants.PLAYER_MAX_SPEED;
        playerMaxDamage        = Constants.PLAYER_MAX_DAMAGE;
        playerMinShootCooldown = Constants.PLAYER_MIN_SHOOT_COOLDOWN;

        powerUpDropChance      = Constants.POWERUP_DROP_CHANCE;
        powerUpSpeedBonus      = Constants.POWERUP_SPEED_BONUS;
        powerUpDamageBonus     = Constants.POWERUP_DAMAGE_BONUS;
        powerUpCooldownBonus   = Constants.POWERUP_COOLDOWN_BONUS;
        powerUpHpBonus         = Constants.POWERUP_HP_BONUS;

        meleeEnemySpeed        = Constants.MELEE_SPEED;
        rangedEnemySpeed       = Constants.RANGED_SPEED;
        semiBossEnemySpeed     = Constants.SEMIBOSS_SPEED;
        rangedEnemyShootCooldown = Constants.RANGED_COOLDOWN;

        totalRounds            = Constants.TOTAL_ROUNDS;
        enemySpawnCooldown     = Constants.ENEMY_SPAWN_COOLDOWN;
    }

    // =========================================================================
    // GETTERS & SETTERS
    // =========================================================================

    // ── Player ───────────────────────────────────────────────────────────────

    public int getPlayerBaseHp() { return playerBaseHp; }
    public void setPlayerBaseHp(int v) { playerBaseHp = Math.max(1, v); }

    public float getPlayerBaseSpeed() { return playerBaseSpeed; }
    public void setPlayerBaseSpeed(float v) { playerBaseSpeed = Math.max(0.5f, Math.min(v, 15f)); }

    public int getPlayerBaseDamage() { return playerBaseDamage; }
    public void setPlayerBaseDamage(int v) { playerBaseDamage = Math.max(1, v); }

    public int getPlayerShootCooldown() { return playerShootCooldown; }
    public void setPlayerShootCooldown(int v) { playerShootCooldown = Math.max(1, v); }

    public int getPlayerHitCooldown() { return playerHitCooldown; }
    public void setPlayerHitCooldown(int v) { playerHitCooldown = Math.max(0, v); }

    public float getPlayerMaxSpeed() { return playerMaxSpeed; }
    public void setPlayerMaxSpeed(float v) { playerMaxSpeed = Math.max(playerBaseSpeed, v); }

    public int getPlayerMaxDamage() { return playerMaxDamage; }
    public void setPlayerMaxDamage(int v) { playerMaxDamage = Math.max(playerBaseDamage, v); }

    public int getPlayerMinShootCooldown() { return playerMinShootCooldown; }
    public void setPlayerMinShootCooldown(int v) { playerMinShootCooldown = Math.max(1, v); }

    // ── Power-ups ────────────────────────────────────────────────────────────

    public float getPowerUpDropChance() { return powerUpDropChance; }
    public void setPowerUpDropChance(float v) { powerUpDropChance = Math.max(0f, Math.min(v, 1f)); }

    public float getPowerUpSpeedBonus() { return powerUpSpeedBonus; }
    public void setPowerUpSpeedBonus(float v) { powerUpSpeedBonus = Math.max(0f, v); }

    public int getPowerUpDamageBonus() { return powerUpDamageBonus; }
    public void setPowerUpDamageBonus(int v) { powerUpDamageBonus = Math.max(0, v); }

    public int getPowerUpCooldownBonus() { return powerUpCooldownBonus; }
    public void setPowerUpCooldownBonus(int v) { powerUpCooldownBonus = Math.max(0, v); }

    public int getPowerUpHpBonus() { return powerUpHpBonus; }
    public void setPowerUpHpBonus(int v) { powerUpHpBonus = Math.max(0, v); }

    // ── Enemies ───────────────────────────────────────────────────────────────

    public float getMeleeEnemySpeed() { return meleeEnemySpeed; }
    public void setMeleeEnemySpeed(float v) { meleeEnemySpeed = Math.max(0.1f, v); }

    public float getRangedEnemySpeed() { return rangedEnemySpeed; }
    public void setRangedEnemySpeed(float v) { rangedEnemySpeed = Math.max(0.1f, v); }

    public float getSemiBossEnemySpeed() { return semiBossEnemySpeed; }
    public void setSemiBossEnemySpeed(float v) { semiBossEnemySpeed = Math.max(0.1f, v); }

    public int getRangedEnemyShootCooldown() { return rangedEnemyShootCooldown; }
    public void setRangedEnemyShootCooldown(int v) { rangedEnemyShootCooldown = Math.max(10, v); }

    // ── Rounds ───────────────────────────────────────────────────────────────

    public int getTotalRounds() { return totalRounds; }
    public void setTotalRounds(int v) { totalRounds = Math.max(1, Math.min(v, 10)); }

    public int getEnemySpawnCooldown() { return enemySpawnCooldown; }
    public void setEnemySpawnCooldown(int v) { enemySpawnCooldown = Math.max(5, v); }

    // =========================================================================
    // UTILITY
    // =========================================================================

    @Override
    public String toString() {
        return String.format(
            "GameSettings{hp=%d, spd=%.1f, dmg=%d, rounds=%d, dropChance=%.0f%%}",
            playerBaseHp, playerBaseSpeed, playerBaseDamage,
            totalRounds, powerUpDropChance * 100f);
    }
}
