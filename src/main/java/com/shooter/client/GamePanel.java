package com.shooter.client;

import com.shooter.shared.model.*;
import com.shooter.shared.util.Constants;
import com.shooter.shared.util.Direction;
import com.shooter.ui.HUD;

import javax.swing.JPanel;
import java.awt.*;
import java.awt.event.KeyEvent;

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

        Player player = gameState.getMainPlayer();
        if (player == null)
            return;

        player.tickCooldown();

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

        // Enemies move toward player
        for (Enemy enemy : entityManager.getEnemies()) {
            enemy.moveToward(
                    player.getX() + player.getWidth() / 2f,
                    player.getY() + player.getHeight() / 2f);

            if (playerHitCooldown == 0) {
                if (CollisionDetector.enemyHitsPlayer(enemy, player)) {
                    player.takeDamage(enemy.getDamage());

                    playerHitCooldown = 60; // about 1 second at 60 FPS

                    System.out.println("Player hit! HP: " + player.getHp());

                    break;
                }
            }
        }

        // Bullet-enemy collision
        for (Bullet bullet : gameState.getBullets()) {
            for (Enemy enemy : entityManager.getEnemies()) {
                if (CollisionDetector.bulletHitsEnemy(bullet, enemy)) {
                    enemy.takeDamage(bullet.getDamage());
                    bullet.expire();
                    break;
                }
            }
        }

        entityManager.removeDeadEnemies();
        gameState.removeExpiredBullets();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;

        drawPlayer(g2d);
        drawBullets(g2d);
        drawEnemies(g2d);

        hud.render(g2d, gameState);
    }

    private void drawPlayer(Graphics2D g2d) {
        Player p = gameState.getMainPlayer();
        if (p == null)
            return;

        g2d.setColor(new Color(Constants.COLOR_PLAYER));
        g2d.fillRect((int) p.getX(), (int) p.getY(), p.getWidth(), p.getHeight());
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
}