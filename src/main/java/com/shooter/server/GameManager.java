package com.shooter.server;

import java.util.ArrayList;
import java.util.List;

import com.shooter.network.GameOverStats;
import com.shooter.network.InputSnapshot;
import com.shooter.network.MessageType;
import com.shooter.network.NetworkMessage;
import com.shooter.shared.model.Bullet;
import com.shooter.shared.model.GameState;
import com.shooter.shared.model.Player;
import com.shooter.shared.util.Constants;
import com.shooter.shared.util.Direction;
import com.shooter.shared.util.GameSettings;
import com.shooter.shared.model.Enemy;
import com.shooter.shared.model.PowerUp;
import com.shooter.shared.logic.CollisionDetector;
import com.shooter.server.EntityManager;
import com.shooter.server.RoundManager;

/**
 * Controls game logic (SERVER SIDE).
 *
 * NOT REQUIRED for Milestone 1 yet.
 */
public class GameManager {

    private final GameState gameState;
    private final List<ClientHandler> clients;
    private final GameSettings settings;
    private boolean running;
    private boolean isPaused = false;
    private boolean gameOverSent = false;
    private EntityManager entityManager;
    private RoundManager roundManager;
    private int[] playerContactCooldowns = new int[Constants.MAX_PLAYERS];
    private int[] playerSpawnCooldowns = new int[Constants.MAX_PLAYERS];

    /** Per-player kill count accumulated across all rounds (index = playerId). */
    private final int[] killsPerPlayer = new int[Constants.MAX_PLAYERS];
    /** Running total of all enemy kills across all rounds. */
    private int totalKillsAccumulated = 0;

    public GameManager(List<ClientHandler> clients, GameSettings settings) {
        this.settings = (settings != null) ? settings : new GameSettings();
        gameState = new GameState();
        entityManager = new EntityManager();
        roundManager = new RoundManager(this.settings);
        this.clients = clients;
        createPlayersForConnectedClients();
        running = false;
        roundManager.startCurrentRound(entityManager);
    }

    /**
     * Creates one authoritative Player object per connected client.
     * Clients render these server-owned players once GAME_STATE snapshots arrive.
     */
    private void createPlayersForConnectedClients() {
        List<ClientHandler> clientsSnapshot;
        synchronized (clients) {
            clientsSnapshot = new ArrayList<>(clients);
        }

        for (ClientHandler client : clientsSnapshot) {
            int playerId = client.getPlayerId();
            gameState.addPlayer(new Player(playerId, "Player " + (playerId + 1), settings));
        }
    }

    /**
     * Starts the server-side game loop.
     * This loop is separate from the client render loop: the server updates
     * game rules at 20 ticks per second, while clients can still render at 60 FPS.
     */
    public void startGameLoop() {
        running = true;

        long tickLengthNanos = 1_000_000_000L / Constants.SERVER_TICK_RATE;
        long nextTickTime = System.nanoTime();
        int ticksThisSecond = 0;
        long lastLogTime = System.currentTimeMillis();

        System.out.println("Server game loop started at " + Constants.SERVER_TICK_RATE + " ticks/sec.");

        while (running) {
            update();
            broadcastGameState();
            ticksThisSecond++;

            nextTickTime += tickLengthNanos;
            long sleepNanos = nextTickTime - System.nanoTime();

            if (sleepNanos > 0) {
                try {
                    Thread.sleep(sleepNanos / 1_000_000L, (int) (sleepNanos % 1_000_000L));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    stopGameLoop();
                }
            } else {
                // If the server falls behind, reset the schedule so it can recover.
                nextTickTime = System.nanoTime();
            }

            if (System.currentTimeMillis() - lastLogTime >= 1000) {
                System.out.println("Server ticks this second: " + ticksThisSecond);
                ticksThisSecond = 0;
                lastLogTime = System.currentTimeMillis();
            }
        }
    }

    /** Stops the server-side game loop cleanly. */
    public void stopGameLoop() {
        running = false;
    }

    public void update() {
        checkPauseRequests();

        if (isPaused) {
            return;
        }

        applyClientInputs();
        tickPlayerCooldowns();
        tickPlayerContactCooldowns();
        tickPlayerSpawnCooldowns();

        for (Bullet bullet : gameState.getBullets()) {
            bullet.update();
        }

        roundManager.updateSpawning(entityManager);
        updateEnemies();
        handleCollisions();

        List<EntityManager.PowerUpCollection> collections = entityManager.collectPowerUpsForPlayers(gameState.getPlayers());
        for (EntityManager.PowerUpCollection coll : collections) {
            broadcastMessage(new NetworkMessage(MessageType.POWER_UP_COLLECTED, coll.getPlayerId(), coll));
        }

        RoundManager.RoundTransition transition = roundManager.checkAndAdvanceRound(entityManager);
        if (roundManager.hasRoundJustCleared()) {
            broadcastMessage(new NetworkMessage(MessageType.ROUND_CLEAR, -1, null));
        }
        if (transition == RoundManager.RoundTransition.ROUND_STARTED) {
            broadcastMessage(new NetworkMessage(MessageType.ROUND_START, -1, roundManager.getCurrentRound()));
        }
        if (transition == RoundManager.RoundTransition.GAME_COMPLETED) {
            // Build the per-player kill map from the accumulated array.
            java.util.Map<Integer, Integer> playerKillMap = new java.util.HashMap<>();
            for (int i = 0; i < killsPerPlayer.length; i++) {
                playerKillMap.put(i, killsPerPlayer[i]);
            }
            broadcastGameOver(true, roundManager.getCurrentRound(), totalKillsAccumulated, playerKillMap);
        }
        roundManager.clearTransitionFlags();

        checkGameOver();

        gameState.setCurrentRound(roundManager.getCurrentRound());
        gameState.setKilledEnemies(roundManager.getKilledEnemies());
        gameState.setTotalEnemiesThisRound(roundManager.getTotalEnemiesThisRound());
        entityManager.copyEntitiesToGameState(gameState);
    }

    /**
     * Server-side collision detection: bullet-enemy, bullet-player, enemy-player contact.
     */
    private void handleCollisions() {
        List<Bullet> bullets = new ArrayList<>(gameState.getBullets());
        List<Enemy> enemies = entityManager.getEnemies();
        List<Player> players = gameState.getPlayers();

        // Player bullets vs enemies
        for (Bullet bullet : bullets) {
            if (bullet.isFromEnemy() || bullet.isExpired()) continue;
            for (Enemy enemy : enemies) {
                if (enemy.isDead()) continue;
                if (CollisionDetector.bulletHitsEnemy(bullet, enemy)) {
                    enemy.takeDamage(bullet.getDamage());
                    bullet.expire();
                    if (enemy.isDead()) {
                        roundManager.addKill();
                        // Credit the kill to the player who fired the bullet.
                        int shooterId = bullet.getOwnerId();
                        if (shooterId >= 0 && shooterId < killsPerPlayer.length) {
                            killsPerPlayer[shooterId]++;
                        }
                        totalKillsAccumulated++;
                        PowerUp dropped = enemy.dropPowerUp();
                        if (dropped != null) {
                            entityManager.addPowerUp(dropped);
                        }
                    }
                    break;
                }
            }
        }

        // Enemy bullets vs players
        for (Bullet bullet : bullets) {
            if (!bullet.isFromEnemy() || bullet.isExpired()) continue;
            for (Player player : players) {
                if (player == null || !player.isAlive()) continue;
                if (CollisionDetector.bulletHitsPlayer(bullet, player)) {
                    player.takeDamage(bullet.getDamage());
                    bullet.expire();
                    break;
                }
            }
        }

        // Enemy-player contact damage (with per-player cooldown)
        for (Enemy enemy : enemies) {
            if (enemy.isDead()) continue;
            for (Player player : players) {
                if (player == null || !player.isAlive()) continue;
                int pid = player.getPlayerId();
                if (pid < 0 || pid >= playerContactCooldowns.length) continue;
                if (playerContactCooldowns[pid] > 0) continue;
                if (CollisionDetector.enemyHitsPlayer(enemy, player)) {
                    player.takeDamage(enemy.getDamage());
                    playerContactCooldowns[pid] = settings.getPlayerHitCooldown();
                    break;
                }
            }
        }

        entityManager.removeDeadEnemies();
        gameState.removeExpiredBullets();
    }

    private void tickPlayerContactCooldowns() {
        for (int i = 0; i < playerContactCooldowns.length; i++) {
            if (playerContactCooldowns[i] > 0) {
                playerContactCooldowns[i]--;
            }
        }
    }

    private void updateEnemies() {
        for (Enemy enemy : entityManager.getEnemies()) {
            Player target = findNearestAlivePlayer(enemy.getX(), enemy.getY());
            if (target == null) continue;

            float targetCenterX = target.getX() + target.getWidth() / 2f;
            float targetCenterY = target.getY() + target.getHeight() / 2f;

            enemy.moveToward(targetCenterX, targetCenterY);

            if (enemy.tickAndCanShoot()) {
                float dx = targetCenterX - enemy.getX();
                float dy = targetCenterY - enemy.getY();
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
            }
        }
    }

    private Player findNearestAlivePlayer(float ex, float ey) {
        Player nearest = null;
        float minDist = Float.MAX_VALUE;
        for (Player p : gameState.getPlayers()) {
            if (p != null && p.isAlive()) {
                float px = p.getX() + p.getWidth() / 2f;
                float py = p.getY() + p.getHeight() / 2f;
                float dist = (px - ex)*(px - ex) + (py - ey)*(py - ey);
                if (dist < minDist) {
                    minDist = dist;
                    nearest = p;
                }
            }
        }
        return nearest;
    }

    /**
     * Applies each client's latest InputSnapshot to the matching server-owned Player.
     * This keeps movement authority on the server instead of trusting client positions.
     */
    private void checkPauseRequests() {
        boolean togglePause = false;
        List<ClientHandler> clientsSnapshot;
        synchronized (clients) {
            clientsSnapshot = new ArrayList<>(clients);
        }
        for (ClientHandler client : clientsSnapshot) {
            if (client.pollPauseRequest()) {
                togglePause = true;
            }
        }
        if (togglePause) {
            isPaused = !isPaused;
            broadcastPauseState();
        }
    }

    private void checkGameOver() {
        if (gameOverSent || gameState.getPlayers().isEmpty()) {
            return;
        }

        boolean allDead = true;
        for (Player player : gameState.getPlayers()) {
            if (player.isAlive()) {
                allDead = false;
                break;
            }
        }

        if (allDead) {
            // Build the per-player kill map from the accumulated array.
            java.util.Map<Integer, Integer> playerKillMap = new java.util.HashMap<>();
            for (int i = 0; i < killsPerPlayer.length; i++) {
                playerKillMap.put(i, killsPerPlayer[i]);
            }
            // roundsCleared = rounds fully completed before dying on this round.
            // currentRound is 1-indexed and is the round that caused the game over,
            // so completed rounds = currentRound - 1.
            int roundsCleared = Math.max(0, gameState.getCurrentRound() - 1);
            broadcastGameOver(false, roundsCleared, totalKillsAccumulated, playerKillMap);
            stopGameLoop();
        }
    }

    public void broadcastGameOver(boolean victory, int roundsCleared, int totalKills, java.util.Map<Integer, Integer> playerKills) {
        if (gameOverSent) return;
        gameOverSent = true;
        
        GameOverStats stats = new GameOverStats(victory, roundsCleared, totalKills, playerKills);
        NetworkMessage gameOverMessage = new NetworkMessage(MessageType.GAME_OVER, -1, stats);
        
        List<ClientHandler> clientsSnapshot;
        synchronized (clients) {
            clientsSnapshot = new ArrayList<>(clients);
        }
        for (ClientHandler client : clientsSnapshot) {
            client.sendMessage(gameOverMessage);
        }
    }

    private void broadcastPauseState() {
        NetworkMessage pauseMessage = new NetworkMessage(
            MessageType.PAUSE,
            -1,
            isPaused
        );
        List<ClientHandler> clientsSnapshot;
        synchronized (clients) {
            clientsSnapshot = new ArrayList<>(clients);
        }
        for (ClientHandler client : clientsSnapshot) {
            client.sendMessage(pauseMessage);
        }
    }

    private void applyClientInputs() {
        List<ClientHandler> clientsSnapshot;
        synchronized (clients) {
            clientsSnapshot = new ArrayList<>(clients);
        }

        for (ClientHandler client : clientsSnapshot) {
            Player player = findPlayerById(client.getPlayerId());
            InputSnapshot input = client.getLatestInput();

            if (player == null || input == null || !player.isAlive()) {
                continue;
            }

            if (input.isUpPressed()) {
                player.move(Direction.UP);
            }
            if (input.isDownPressed()) {
                player.move(Direction.DOWN);
            }
            if (input.isLeftPressed()) {
                player.move(Direction.LEFT);
            }
            if (input.isRightPressed()) {
                player.move(Direction.RIGHT);
            }

            if (input.getFacingDirection() != null) {
                player.setFacing(input.getFacingDirection());
            }

            if (input.isShooting()) {
                Bullet newBullet = player.shoot();
                if (newBullet != null) {
                    gameState.addBullet(newBullet);
                }
            }
        }
    }

    private void tickPlayerCooldowns() {
        for (Player player : gameState.getPlayers()) {
            player.tickCooldown();
        }
    }

    private Player findPlayerById(int playerId) {
        for (Player player : gameState.getPlayers()) {
            if (player.getPlayerId() == playerId) {
                return player;
            }
        }

        return null;
    }

    public GameState getGameState() {
        return gameState;
    }

    /**
     * Sends the current authoritative GameState to every connected client.
     * Clients should render this state instead of making their own game-state changes.
     */
    private void broadcastGameState() {
        NetworkMessage stateMessage = new NetworkMessage(
            MessageType.GAME_STATE,
            -1,
            gameState
        );

        List<ClientHandler> clientsSnapshot;
        synchronized (clients) {
            clientsSnapshot = new ArrayList<>(clients);
        }

        for (ClientHandler client : clientsSnapshot) {
            client.sendMessage(stateMessage);
        }
    }

    private void broadcastMessage(NetworkMessage msg) {
        List<ClientHandler> clientsSnapshot;
        synchronized (clients) {
            clientsSnapshot = new ArrayList<>(clients);
        }
        for (ClientHandler client : clientsSnapshot) {
            client.sendMessage(msg);
        }
    }

    public synchronized void handlePlayerDisconnect(int playerId) {
        if (gameState != null) {
            boolean removed = gameState.getPlayers().removeIf(p -> p.getPlayerId() == playerId);
            if (removed) {
                System.out.println("Removed disconnected Player " + playerId + " from game state.");
            }
        }
    }

    private void tickPlayerSpawnCooldowns() {
        for (Player player : new ArrayList<>(gameState.getPlayers())) {
            if (player == null) continue;
            if (!player.isAlive()) {
                int pid = player.getPlayerId();
                if (pid < 0 || pid >= playerSpawnCooldowns.length) continue;

                if (playerSpawnCooldowns[pid] == 0) {
                    playerSpawnCooldowns[pid] = Constants.PLAYER_SPAWN_COOLDOWN;
                    System.out.println("Player " + pid + " died. Respawning in " + Constants.PLAYER_SPAWN_COOLDOWN + " ticks.");
                    continue;
                }

                playerSpawnCooldowns[pid]--;

                if (playerSpawnCooldowns[pid] <= 0) {
                    float[] spawn = findSafestSpawnPoint();
                    player.reviveAt(spawn[0], spawn[1], settings);
                    System.out.println("Player " + pid + " respawned on server.");
                }
            } else {
                int pid = player.getPlayerId();
                if (pid >= 0 && pid < playerSpawnCooldowns.length) {
                    playerSpawnCooldowns[pid] = 0;
                }
            }
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

        return bestSpawn;
    }

    private int countNearbyEnemies(float sx, float sy) {
        int count = 0;
        float radius = 150f;
        for (Enemy enemy : entityManager.getEnemies()) {
            float dx = enemy.getX() - sx;
            float dy = enemy.getY() - sy;
            if (dx*dx + dy*dy <= radius*radius) {
                count++;
            }
        }
        return count;
    }
}
