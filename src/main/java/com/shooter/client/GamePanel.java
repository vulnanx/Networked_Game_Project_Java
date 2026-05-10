package com.shooter.client;

import com.shooter.shared.model.*;
import com.shooter.shared.util.Constants;
import com.shooter.shared.util.Direction;
import com.shooter.ui.HUD;
import com.shooter.network.InputSnapshot;

import javax.swing.JPanel;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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

    private static final float PLAYER_RENDER_LERP = 0.35f;

    private GameState gameState;
    private GameClient client;
    private InputHandler input;
    private HUD hud;

    private Thread gameThread;
    private boolean running = false;

    private EntityManager entityManager;
    private RoundManager roundManager;
    private int playerHitCooldown = Constants.PLAYER_HIT_COOLDOWN;
    private int playerSpawnCooldown = 0; // counts down after death; player revives when it hits 0
    private Map<Integer, Point2D.Float> playerRenderPositions = new HashMap<>();
    private Direction lastFacingDirection = Direction.DOWN;

    public GamePanel(GameState gameState) {
        this(gameState, null);
    }

    public GamePanel(GameState gameState, GameClient client) {
        this.gameState = gameState;
        this.client = client;
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

        Player player = gameState.getMainPlayer();
        if (player == null)
            return;

        player.tickCooldown();
        hud.tick();

        if (isMultiplayerClient()) {
            sendInputSnapshot();
            return;
        }

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
        if (player.isAlive() && input.isPressed(KeyEvent.VK_SPACE)) {
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

                    System.out.println("Player hit! HP: " + player.getHp());

                    break;
                }
            }
        }

        // Bullet-enemy collision
        for (Bullet bullet : gameState.getBullets()) {
            if (bullet.isFromEnemy()) {
                // Enemy bullet — check if it hits the player (only once per bullet, not per
                // enemy)
                if (CollisionDetector.bulletHitsPlayer(bullet, player)) {
                    player.takeDamage(bullet.getDamage());
                    bullet.expire();
                    playerHitCooldown = Constants.PLAYER_HIT_COOLDOWN;
                }
            } else {
                // Player bullet — check if it hits any enemy
                for (Enemy enemy : entityManager.getEnemies()) {
                    if (CollisionDetector.bulletHitsEnemy(bullet, enemy)) {
                        enemy.takeDamage(bullet.getDamage());
                        if (enemy.isDead()) {
                            roundManager.addKill();
                            PowerUp dropped = enemy.dropPowerUp();
                            if (dropped != null) {
                                entityManager.addPowerUp(dropped);
                                System.out.println("Power-up dropped: " + dropped.getType());
                            }
                        }
                        bullet.expire();
                        break;
                    }
                }
            }
        }

        handlePlayerDeath(player);
        entityManager.removeDeadEnemies();
        gameState.removeExpiredBullets();
        roundManager.checkAndAdvanceRound(entityManager);
        gameState.setCurrentRound(roundManager.getCurrentRound());

    }

    private boolean isMultiplayerClient() {
        return client != null && client.isConnectedToServer();
    }

    private void sendInputSnapshot() {
        if (client == null || !client.isConnectedToServer()) {
            return;
        }

        Direction inputFacing = getFacingDirectionFromInput();
        if (inputFacing != null) {
            lastFacingDirection = inputFacing;
        }

        InputSnapshot snapshot = new InputSnapshot(
                input.isPressed(KeyEvent.VK_W),
                input.isPressed(KeyEvent.VK_S),
                input.isPressed(KeyEvent.VK_A),
                input.isPressed(KeyEvent.VK_D),
                lastFacingDirection,
                input.isPressed(KeyEvent.VK_SPACE));

        client.sendInputSnapshot(snapshot);
    }

    private Direction getFacingDirectionFromInput() {
        if (input.isPressed(KeyEvent.VK_W)) {
            return Direction.UP;
        }
        if (input.isPressed(KeyEvent.VK_S)) {
            return Direction.DOWN;
        }
        if (input.isPressed(KeyEvent.VK_A)) {
            return Direction.LEFT;
        }
        if (input.isPressed(KeyEvent.VK_D)) {
            return Direction.RIGHT;
        }

        return null;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;

        drawPlayers(g2d);
        drawBullets(g2d);
        drawEnemies(g2d);
        drawPowerUps(g2d);

        hud.render(g2d, gameState, roundManager.getKilledEnemies(), roundManager.getTotalEnemiesThisRound(),
                playerSpawnCooldown);
    }

    private void drawPlayers(Graphics2D g2d) {
        removeMissingPlayerRenderPositions();

        for (Player p : gameState.getPlayers()) {
            if (p == null || !p.isAlive()) {
                continue;
            }

            Point2D.Float renderPosition = getInterpolatedPlayerPosition(p);

            g2d.setColor(getPlayerColor(p.getPlayerId()));
            g2d.fillRect((int) renderPosition.x, (int) renderPosition.y, p.getWidth(), p.getHeight());
        }
    }

    private Point2D.Float getInterpolatedPlayerPosition(Player player) {
        Point2D.Float renderPosition = playerRenderPositions.get(player.getPlayerId());

        if (renderPosition == null) {
            renderPosition = new Point2D.Float(player.getX(), player.getY());
            playerRenderPositions.put(player.getPlayerId(), renderPosition);
            return renderPosition;
        }

        // Rendering only: ease toward the latest position without changing gameplay state.
        renderPosition.x += (player.getX() - renderPosition.x) * PLAYER_RENDER_LERP;
        renderPosition.y += (player.getY() - renderPosition.y) * PLAYER_RENDER_LERP;

        return renderPosition;
    }

    private void removeMissingPlayerRenderPositions() {
        Iterator<Integer> ids = playerRenderPositions.keySet().iterator();

        while (ids.hasNext()) {
            int playerId = ids.next();

            if (!hasRenderablePlayer(playerId)) {
                ids.remove();
            }
        }
    }

    private boolean hasRenderablePlayer(int playerId) {
        for (Player player : gameState.getPlayers()) {
            if (player != null && player.isAlive() && player.getPlayerId() == playerId) {
                return true;
            }
        }

        return false;
    }

    private Color getPlayerColor(int playerId) {
        switch (playerId) {
            case 0:
                return new Color(Constants.COLOR_PLAYER);
            case 1:
                return new Color(0xE05C5C);
            case 2:
                return new Color(0x50E878);
            case 3:
                return new Color(0xF5D142);
            default:
                return Color.WHITE;
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
        for (Enemy enemy : entityManager.getEnemies()) {

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
        }
    }

    private void drawPowerUps(Graphics2D g2d) {
        g2d.setColor(new Color(Constants.COLOR_POWERUP));

        for (PowerUp powerUp : entityManager.getPowerUps()) {
            g2d.fillOval(
                    (int) powerUp.getX(),
                    (int) powerUp.getY(),
                    powerUp.getWidth(),
                    powerUp.getHeight());
        }
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
            // make sure that players cannot shoot while dead

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
}
