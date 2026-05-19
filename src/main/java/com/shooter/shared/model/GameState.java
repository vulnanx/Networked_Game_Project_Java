package com.shooter.shared.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================
 * FILE: GameState.java
 * PACKAGE: shared.model
 * OWNER: All members (shared contract)
 * ============================================================
 *
 * RESPONSIBILITY:
 * Represents the ENTIRE game at one moment in time.
 * This is the "single source of truth" for:
 * - players
 * - enemies
 * - bullets
 * - power-ups
 * - current round
 *
 * In Milestone 2:
 * This is what the SERVER sends to all clients.
 *
 * WHAT TO ADD HERE:
 * - Score tracking
 * - Player list for multiplayer
 * - Round progression data
 *
 * WHAT NOT TO PUT HERE:
 * - Rendering logic
 * - Movement logic
 * - Input handling
 *
 * CONNECTS TO:
 * GamePanel (renders this)
 * GameManager (updates this)
 * Network (sent across clients)
 * ============================================================
 */
public class GameState implements Serializable {

    // ─── SERIALIZATION ────────────────────────────────────────────────────────
    // Must match Player.serialVersionUID version. Bump both together if fields change.
    // Without this, Java generates a random ID at runtime — two different machines
    // will get different IDs and reject each other's packets.
    private static final long serialVersionUID = 2L;

    // ─── GAME DATA ────────────────────────────────────────────────────────────
    private List<Player> players = new ArrayList<>();
    private List<Enemy> enemies = new ArrayList<>();
    private List<Bullet> bullets = new ArrayList<>();
    private List<PowerUp> powerUps = new ArrayList<>();
    private List<DeathEffect> pendingEffects = new ArrayList<>();

    private int currentRound = 1;

    // ─── CLIENT IDENTITY ─────────────────────────────────────────────────────
    // Which player slot does THIS client own? (0–3, assigned by server)
    // Only used on the client side. Server ignores this field.
    private int localPlayerId = -1; // -1 = not yet assigned

    public void addPlayer(Player player) {
        players.add(player);
    }

    public void addEnemy(Enemy enemy) {
        enemies.add(enemy);
    }

    public void addBullet(Bullet bullet) {
        bullets.add(bullet);
    }

    public void addPowerUp(PowerUp powerUp) {
        powerUps.add(powerUp);
    }

    /**
     * M1 compatibility: returns the first player in the list.
     * In M2 single-player mode, this is still the local player.
     * In M2 multiplayer, prefer getLocalPlayer() instead.
     */
    public Player getMainPlayer() {
        return players.isEmpty() ? null : players.get(0);
    }

    /**
     * M2: Returns the player this client controls, matched by localPlayerId.
     * Returns null if localPlayerId hasn't been set yet (before server assigns it).
     */
    public Player getLocalPlayer() {
        return getPlayerById(localPlayerId);
    }

    /**
     * Find any player in the list by their network ID (0–3).
     * Used by the client to locate its own player after receiving a GameState broadcast.
     * @param id the playerId to search for
     * @return the matching Player, or null if not found
     */
    public Player getPlayerById(int id) {
        for (Player p : players) {
            if (p.getPlayerId() == id) {
                return p;
            }
        }
        return null;
    }

    public void removeExpiredBullets() {
        bullets.removeIf(Bullet::isExpired);
    }

    public void removeDeadEnemies() {
        enemies.removeIf(Enemy::isDead);
    }

    public List<Player> getPlayers() {
        return players;
    }

    public List<Enemy> getEnemies() {
        return enemies;
    }

    public List<Bullet> getBullets() {
        return bullets;
    }

    public List<PowerUp> getPowerUps() {
        return powerUps;
    }

    public List<DeathEffect> getPendingEffects() {
        return pendingEffects;
    }

    public void addPendingEffect(DeathEffect effect) {
        pendingEffects.add(effect);
    }

    public void clearPendingEffects() {
        pendingEffects.clear();
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public void setCurrentRound(int r) {
        currentRound = r;
    }

    // ─── LIST SETTERS (used by client to apply server broadcast) ─────────────
    // When the server sends a full GameState snapshot, the client calls these
    // to replace its local data with the authoritative server data.
    // Never call these on the server side — the server always owns the lists.

    /**
     * Replace the entire player list with the server's authoritative snapshot.
     * Called by GamePanel when a GAME_STATE message arrives.
     */
    public void setPlayers(List<Player> players) {
        this.players = players;
    }

    /**
     * Replace the enemy list with the server's authoritative snapshot.
     */
    public void setEnemies(List<Enemy> enemies) {
        this.enemies = enemies;
    }

    /**
     * Replace the bullet list with the server's authoritative snapshot.
     */
    public void setBullets(List<Bullet> bullets) {
        this.bullets = bullets;
    }

    /**
     * Replace the power-up list with the server's authoritative snapshot.
     */
    public void setPowerUps(List<PowerUp> powerUps) {
        this.powerUps = powerUps;
    }

    /**
     * Replace the pending effects list with the server's authoritative snapshot.
     */
    public void setPendingEffects(List<DeathEffect> pendingEffects) {
        this.pendingEffects = pendingEffects;
    }

    // ─── LOCAL PLAYER ID ─────────────────────────────────────────────────────

    /**
     * Set which player slot this client owns.
     * Call this once after the server sends a CONNECTED message with the assigned ID.
     * @param id value 0–3
     */
    public void setLocalPlayerId(int id) {
        this.localPlayerId = id;
    }

    /** @return the player ID this client was assigned, or -1 if not yet connected */
    public int getLocalPlayerId() {
        return localPlayerId;
    }
}