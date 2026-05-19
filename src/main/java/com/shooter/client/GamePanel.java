package com.shooter.client;

import com.shooter.shared.model.*;
import com.shooter.shared.util.Constants;
import com.shooter.shared.util.Direction;
import com.shooter.network.InputSnapshot;
import com.shooter.ui.HUD;

import javax.swing.JPanel;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.List;

import com.shooter.server.EntityManager;
import com.shooter.server.RoundManager;
import com.shooter.shared.model.Enemy;
import com.shooter.shared.logic.CollisionDetector;

/**
 * ============================================================
 * FILE: GamePanel.java
 * PACKAGE: client
 * OWNER: Member A (Engine/UI)
 * ============================================================
 *
 * RESPONSIBILITY:
 * - Main rendering surface (JPanel)
 * - Runs the game loop (update + render)
 * - Reads input and applies it to Player
 *
 * WHAT TO ADD LATER:
 * - Enemy rendering (Day 2)
 * - Collision integration
 * - Animations / sprites
 *
 * WHAT NOT TO PUT HERE:
 * - Game rules (belongs in GameManager)
 * - Networking logic
 *
 * ============================================================
 */
public class GamePanel extends JPanel implements Runnable {

    private GameState gameState;
    private InputHandler input;
    private HUD hud;

    private Thread gameThread;
    private boolean running = false;

    private EntityManager entityManager;
    private RoundManager roundManager;
    private int playerHitCooldown = Constants.PLAYER_HIT_COOLDOWN;
    private int playerSpawnCooldown = 0; // counts down after death; player revives when it hits 0

    public GamePanel(GameState gameState) {
        this.gameState = gameState;
        this.input = new InputHandler();
        this.hud = new HUD();
        this.entityManager = new EntityManager();
        this.roundManager = new RoundManager();

        // Start Round 1 once when the game panel is created.
        roundManager.startCurrentRound(entityManager);

        setPreferredSize(new Dimension(Constants.SCREEN_WIDTH, Constants.SCREEN_HEIGHT));
        setBackground(new Color(Constants.COLOR_ARENA_BG));
        setFocusable(true);
        addKeyListener(input);
    }

    public void startGameLoop() {
        running = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        long delta = 0;

        while (running) {
            long now = System.nanoTime();
            delta += now - lastTime;
            lastTime = now;

            while (delta >= Constants.NS_PER_TICK) {
                updateGame();
                delta -= Constants.NS_PER_TICK;
            }

            repaint();
        }
    }

    private void updateGame() {

        Player player = gameState.getLocalPlayer();
        
        // If we haven't connected yet (localPlayerId == -1), use the M1 fallback
        if (player == null) {
            player = gameState.getMainPlayer();
        }

        if (player == null)
            return;

        // HUD notifications are local presentation only, so this can tick on both
        // M1 and M2. Gameplay cooldowns are handled inside the correct branch.
        hud.tick();

        // --- BRANCH: MULTIPLAYER vs SINGLE-PLAYER ---
        // If localPlayerId is -1, we are in M1 single-player mode. Run local physics.
        // If localPlayerId is 0-3, we are in M2 multiplayer. Server has authority.
        if (gameState.getLocalPlayerId() == -1) {
            
            // M1 Local Physics Logic
            player.tickCooldown();
            roundManager.updateSpawning(entityManager);
            handlePowerUpCollection(player);

            // Update player hit cooldown
            if (playerHitCooldown > 0) {
                playerHitCooldown--;
            }

            // Movement
            if (input.isPressed(KeyEvent.VK_W))
                player.move(Direction.UP);
            if (input.isPressed(KeyEvent.VK_S))
                player.move(Direction.DOWN);
            if (input.isPressed(KeyEvent.VK_A))
                player.move(Direction.LEFT);
            if (input.isPressed(KeyEvent.VK_D))
                player.move(Direction.RIGHT);

            // Shooting
            if (input.isPressed(KeyEvent.VK_SPACE)) {
                Bullet b = player.shoot();
                if (b != null) {
                    gameState.addBullet(b);
                }
            }

            // Update bullets
            for (Bullet b : gameState.getBullets()) {
                b.update();
            }

            updateEnemies(player);

            if (playerHitCooldown == 0) {
                for (Enemy enemy : entityManager.getEnemies()) {
                    if (CollisionDetector.enemyHitsPlayer(enemy, player)) {
                        player.takeDamage(enemy.getDamage());
                        playerHitCooldown = Constants.PLAYER_HIT_COOLDOWN;
                        break;
                    }
                }
            }

            // Bullet-enemy collision
            for (Bullet bullet : gameState.getBullets()) {
                if (bullet.isFromEnemy()) {
                    if (CollisionDetector.bulletHitsPlayer(bullet, player)) {
                        player.takeDamage(bullet.getDamage());
                        bullet.expire();
                        playerHitCooldown = Constants.PLAYER_HIT_COOLDOWN;
                    }
                } else {
                    for (Enemy enemy : entityManager.getEnemies()) {
                        if (CollisionDetector.bulletHitsEnemy(bullet, enemy)) {
                            enemy.takeDamage(bullet.getDamage());
                            if (enemy.isDead()) {
                                roundManager.addKill();
                                PowerUp dropped = enemy.dropPowerUp();
                                if (dropped != null) {
                                    entityManager.addPowerUp(dropped);
                                }
                            }
                            bullet.expire();
                            break;
                        }
                    }
                }
            }

            entityManager.removeDeadEnemies();
            gameState.removeExpiredBullets();
            roundManager.checkAndAdvanceRound(entityManager);
            gameState.setCurrentRound(roundManager.getCurrentRound());

            handlePlayerDeath(player);
        } 
        else {
            // M2 Multiplayer: AUTHORITATIVE RENDERING ONLY
            // We do NOT call player.move() or collision logic here.
            // We also do NOT revive the player here; HP/death state must come
            // from the server's GameState broadcast.
            // We only handle time-based animations or HUD updates if necessary.
            // The positions will be updated by applyServerState() when the server broadcasts.
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;

        drawPlayer(g2d);
        drawBullets(g2d);
        drawEnemies(g2d);
        drawPowerUps(g2d);
        drawEffects(g2d);

        hud.render(g2d, gameState, roundManager.getKilledEnemies(), roundManager.getTotalEnemiesThisRound(),
                playerSpawnCooldown);
    }

    private void drawEffects(Graphics2D g2d) {
        List<DeathEffect> effects = gameState.getPendingEffects();
        if (effects != null) {
            for (DeathEffect effect : effects) {
                // Flash white at the location
                g2d.setColor(new Color(255, 255, 255, 180));
                g2d.fillRect((int) effect.getX(), (int) effect.getY(), Constants.ENEMY_SIZE, Constants.ENEMY_SIZE);
            }
            // Clear the list after rendering so it only flashes once
            gameState.clearPendingEffects();
        }
    }

    /**
     * Draw EVERY player currently in the game state.
     *
     * In M1 single-player mode: gameState has exactly one player — this still works.
     * In M2 multiplayer mode: gameState has up to 4 players sent by the server.
     *
     * Color is chosen by playerId (0=Blue, 1=Red, 2=Green, 3=Yellow).
     * The local player gets a white outline so you can easily spot yourself.
     * A small "P1" / "P2" label is drawn above each rectangle for debugging.
     */
    private void drawPlayer(Graphics2D g2d) {
        for (Player p : gameState.getPlayers()) {
            if (p == null || !p.isAlive()) continue;

            // Pick the color for this player slot (safe against out-of-range ids)
            int colorId = p.getPlayerId();
            int colorRgb = (colorId >= 0 && colorId < Constants.COLOR_PLAYERS.length)
                    ? Constants.COLOR_PLAYERS[colorId]
                    : Constants.COLOR_PLAYER; // fallback if id is unexpected

            g2d.setColor(new Color(colorRgb));
            g2d.fillRect((int) p.getX(), (int) p.getY(), p.getWidth(), p.getHeight());

            // Hit flash overlay
            if (System.currentTimeMillis() < p.getHitFlashUntil()) {
                g2d.setColor(new Color(255, 255, 255, 180));
                g2d.fillRect((int) p.getX(), (int) p.getY(), p.getWidth(), p.getHeight());
            }

            // White outline for the local player (helps you see yourself in a crowd)
            if (p.getPlayerId() == gameState.getLocalPlayerId()) {
                g2d.setColor(Color.WHITE);
                g2d.drawRect((int) p.getX(), (int) p.getY(), p.getWidth(), p.getHeight());
            }

            // Small debug label: "P1", "P2", etc.
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 10));
            g2d.drawString("P" + (p.getPlayerId() + 1), (int) p.getX() + 8, (int) p.getY() - 4);
        }
    }

    private void drawBullets(Graphics2D g2d) {
        g2d.setColor(new Color(Constants.COLOR_BULLET));

        for (Bullet b : gameState.getBullets()) {
            g2d.fillRect((int) b.getX(), (int) b.getY(), b.getWidth(), b.getHeight());
        }
    }

    /**
     * Draws all enemies currently stored in EntityManager.
     * For now, enemies are simple colored rectangles.
     * Later, this can be replaced with sprite drawing.
     */
    private void drawEnemies(Graphics2D g2d) {
        for (Enemy enemy : getVisibleEnemies()) {

            switch (enemy.getType()) {
                case MELEE:
                    g2d.setColor(new Color(Constants.COLOR_MELEE));
                    break;

                case RANGED:
                    g2d.setColor(new Color(Constants.COLOR_RANGED));
                    break;

                case SEMI_BOSS:
                    g2d.setColor(new Color(Constants.COLOR_SEMIBOSS));
                    break;
            }

            g2d.fillRect(
                    (int) enemy.getX(),
                    (int) enemy.getY(),
                    enemy.getWidth(),
                    enemy.getHeight());

            // Hit flash overlay
            if (System.currentTimeMillis() < enemy.getHitFlashUntil()) {
                g2d.setColor(new Color(255, 255, 255, 180));
                g2d.fillRect((int) enemy.getX(), (int) enemy.getY(), enemy.getWidth(), enemy.getHeight());
            }
        }
    }

    private void drawPowerUps(Graphics2D g2d) {
        g2d.setColor(new Color(Constants.COLOR_POWERUP));

        for (PowerUp powerUp : getVisiblePowerUps()) {
            g2d.fillOval(
                    (int) powerUp.getX(),
                    (int) powerUp.getY(),
                    powerUp.getWidth(),
                    powerUp.getHeight());
        }
    }

    /**
     * M1 draws locally simulated enemies. M2 draws only the server snapshot.
     */
    private List<Enemy> getVisibleEnemies() {
        if (gameState.getLocalPlayerId() == -1) {
            return entityManager.getEnemies();
        }
        return gameState.getEnemies();
    }

    /**
     * M1 draws local power-ups. M2 draws only the server snapshot.
     */
    private List<PowerUp> getVisiblePowerUps() {
        if (gameState.getLocalPlayerId() == -1) {
            return entityManager.getPowerUps();
        }
        return gameState.getPowerUps();
    }

    private void handlePowerUpCollection(Player player) {
        entityManager.getPowerUps().removeIf(powerUp -> {
            if (CollisionDetector.playerCollectsPowerUp(player, powerUp)) {
                boolean atCap = player.applyPowerUp(powerUp);

                if (atCap) {
                    hud.notifyPowerUpCapped(player.getName(), powerUp.getType().name());
                } else {
                    hud.notifyPowerUp(player.getName(), powerUp.getType().name());
                }

                System.out.println("Collected power-up: " + powerUp.getType());

                return true; // removes power-up from screen
            }

            return false;
        });
    }

    private void updateEnemies(Player player) {

        float playerCenterX = player.getX() + player.getWidth() / 2f;
        float playerCenterY = player.getY() + player.getHeight() / 2f;

        for (Enemy enemy : entityManager.getEnemies()) {

            // Movement
            enemy.moveToward(playerCenterX, playerCenterY);

            // RANGED enemy shooting
            if (enemy.tickAndCanShoot()) {

                float dx = playerCenterX - enemy.getX();
                float dy = playerCenterY - enemy.getY();

                Direction direction;

                if (Math.abs(dx) > Math.abs(dy)) {
                    direction = dx > 0 ? Direction.RIGHT : Direction.LEFT;
                } else {
                    direction = dy > 0 ? Direction.DOWN : Direction.UP;
                }

                Bullet bullet = new Bullet(
                        enemy.getX(),
                        enemy.getY(),
                        direction,
                        enemy.getDamage(),
                        enemy.getId(),
                        true);

                gameState.addBullet(bullet);

                System.out.println("Ranged enemy fired!");
            }
        }
    }

    private void handlePlayerDeath(Player player) {
        if (player.isAlive()) {
            playerSpawnCooldown = 0; // reset if somehow alive
            return;
        }

        // Start the spawn cooldown the moment the player dies
        if (playerSpawnCooldown == 0) {
            playerSpawnCooldown = Constants.PLAYER_SPAWN_COOLDOWN;
            System.out.println("Player died. Respawning in " + Constants.PLAYER_SPAWN_COOLDOWN + " ticks.");
            return;
        }

        playerSpawnCooldown--;

        if (playerSpawnCooldown <= 0) {
            float[] spawn = findSafestSpawnPoint();
            player.reviveAt(spawn[0], spawn[1]);
            System.out.println("Player respawned.");
        }
    }

    private float[] findSafestSpawnPoint() {
        float[][] spawnPoints = {
                { Constants.ARENA_X + 50, Constants.ARENA_Y + 50 }, // top-left
                { Constants.ARENA_X + Constants.ARENA_WIDTH - 50, Constants.ARENA_Y + 50 }, // top-right
                { Constants.ARENA_X + 50, Constants.ARENA_Y + Constants.ARENA_HEIGHT - 50 }, // bottom-left
                { Constants.ARENA_X + Constants.ARENA_WIDTH - 50, Constants.ARENA_Y + Constants.ARENA_HEIGHT - 50 }, // bottom-right
                { Constants.PLAYER_SPAWN_X, Constants.PLAYER_SPAWN_Y } // fallback center
        };

        float[] bestSpawn = spawnPoints[4];
        int lowestEnemyCount = Integer.MAX_VALUE;

        for (float[] spawn : spawnPoints) {
            int nearbyEnemies = countNearbyEnemies(spawn[0], spawn[1]);

            if (nearbyEnemies < lowestEnemyCount) {
                lowestEnemyCount = nearbyEnemies;
                bestSpawn = spawn;
            }
        }

        System.out.println("Safe spawn selected. Nearby enemies: " + lowestEnemyCount);

        return bestSpawn;
    }

    private int countNearbyEnemies(float spawnX, float spawnY) {
        int count = 0;

        for (Enemy enemy : entityManager.getEnemies()) {
            float enemyCenterX = enemy.getX() + enemy.getWidth() / 2f;
            float enemyCenterY = enemy.getY() + enemy.getHeight() / 2f;

            float dx = spawnX - enemyCenterX;
            float dy = spawnY - enemyCenterY;

            float distance = (float) Math.sqrt(dx * dx + dy * dy);

            if (distance <= Constants.SAFE_SPAWN_RADIUS) {
                count++;
            }
        }

        return count;
    }

    // =========================================================================
    // MILESTONE 2 — INTERPOLATION-SAFE STATE UPDATE
    // =========================================================================

    /**
     * Apply an authoritative GameState snapshot received from the server.
     *
     * This is the ONLY place on the client where entity positions are updated
     * from network data. We do NOT run physics, collision, or any game logic here.
     * The 60fps render loop will pick up the new positions automatically on the
     * next repaint.
     *
     * Why "interpolation-safe"?
     * Because we replace the entire list at once rather than updating individual
     * fields, there is no partial-update window where, e.g., bullet X has moved
     * but bullet Y hasn't. The swap is atomic from the render thread's perspective
     * (Java list reference assignment is a single pointer write).
     *
     * IMPORTANT: Do NOT call updateGame() from here. The client has no authority
     * to move entities — only the server does that.
     *
     * @param serverState the GameState broadcast by the server at 20 ticks/sec
     */
    public void applyServerState(GameState serverState) {
        if (serverState == null) return;

        long now = System.currentTimeMillis();

        // Track HP drops for hit flash
        if (serverState.getPlayers() != null && gameState.getPlayers() != null) {
            for (Player newP : serverState.getPlayers()) {
                Player oldP = gameState.getPlayerById(newP.getPlayerId());
                if (oldP != null) {
                    if (newP.getHp() < oldP.getHp()) {
                        newP.setHitFlashUntil(now + 200);
                    } else {
                        newP.setHitFlashUntil(oldP.getHitFlashUntil());
                    }
                }
            }
        }

        if (serverState.getEnemies() != null && gameState.getEnemies() != null) {
            for (Enemy newE : serverState.getEnemies()) {
                Enemy oldE = null;
                for (Enemy e : gameState.getEnemies()) {
                    if (e.getId() == newE.getId()) {
                        oldE = e;
                        break;
                    }
                }
                if (oldE != null) {
                    if (newE.getHp() < oldE.getHp()) {
                        newE.setHitFlashUntil(now + 100);
                    } else {
                        newE.setHitFlashUntil(oldE.getHitFlashUntil());
                    }
                }
            }
        }

        // Replace our local entity lists with the server's authoritative data.
        // All lists in GameState are Serializable, so they arrived intact.
        gameState.setPlayers(serverState.getPlayers());
        gameState.setEnemies(serverState.getEnemies());
        gameState.setBullets(serverState.getBullets());
        gameState.setPowerUps(serverState.getPowerUps());
        gameState.setPendingEffects(serverState.getPendingEffects());
        gameState.setCurrentRound(serverState.getCurrentRound());

        // Note: localPlayerId is NOT overwritten here — it was set once when
        // the server sent the CONNECTED message and must not change mid-game.
    }

    /**
     * Packages the current keyboard state into a serializable snapshot.
     * Member A will call this and send it to the server.
     */
    public InputSnapshot getCurrentInputSnapshot() {
        Player p = gameState.getLocalPlayer();
        if (p == null) return null;

        return new InputSnapshot(
            input.isPressed(KeyEvent.VK_W),
            input.isPressed(KeyEvent.VK_S),
            input.isPressed(KeyEvent.VK_A),
            input.isPressed(KeyEvent.VK_D),
            input.isPressed(KeyEvent.VK_SPACE),
            p.getFacing()
        );
    }
}
