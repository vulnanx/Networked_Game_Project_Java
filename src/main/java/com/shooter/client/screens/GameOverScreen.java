package com.shooter.client.screens;

import com.shooter.client.ScreenManager;
import com.shooter.shared.util.AssetManager;
import com.shooter.shared.util.Constants;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Random;

/**
 * ============================================================
 * FILE: GameOverScreen.java
 * PACKAGE: client.screens
 * OWNER: Geastin (Member C) — UI REDESIGN: Christel
 * ============================================================
 * Dark horror pixel art game over / victory screen.
 * All constructor signatures and logic preserved exactly.
 * ============================================================
 */
public class GameOverScreen implements Screen {

    // ── Horror palette ────────────────────────────────────────────────────────
    private static final Color VOID_PURPLE   = new Color(0x1A, 0x0A, 0x2E);
    private static final Color TOXIC_GREEN   = new Color(0x39, 0xFF, 0x14);
    private static final Color BLOOD_RED     = new Color(0x8B, 0x00, 0x00);
    private static final Color BRIGHT_RED    = new Color(0xCC, 0x00, 0x00);
    private static final Color GHOSTLY_WHITE = new Color(0xE8, 0xE8, 0xF0);
    private static final Color DARK_BG       = new Color(0x0D, 0x0D, 0x1A);
    private static final Color PURPLE_GLOW   = new Color(0x7B, 0x2F, 0xBE);
    private static final Color AMBER         = new Color(0xF5, 0xA6, 0x23);

    private static final Color[] PLAYER_COLORS = {
        new Color(0x4A, 0x90, 0xD9),
        new Color(0xCC, 0x00, 0x00),
        new Color(0x39, 0xFF, 0x14),
        new Color(0xF5, 0xA6, 0x23)
    };

    // ── Core data ─────────────────────────────────────────────────────────────
    private final ScreenManager screenManager;
    private final Runnable      onBackToLobby;
    private final boolean       teamWon;
    private final int           roundsCleared;
    private final int           totalEnemiesKilled;
    private final int[]         killsPerPlayer;

    // ── Assets ────────────────────────────────────────────────────────────────
    private final BufferedImage gameOverBgImg;
    private final BufferedImage victoryBgImg;

    // ── Buttons ───────────────────────────────────────────────────────────────
    private final Rectangle runStatsBtn;
    private final Rectangle backToLobbyBtn;
    private boolean runStatsHovered = false;
    private boolean backHovered     = false;
    private boolean statsModalOpen  = false;

    // ── Animation ─────────────────────────────────────────────────────────────
    private int   tick        = 0;
    private float titleAlpha  = 0f;  // fade-in
    private float statsAlpha  = 0f;
    private float flickerA    = 1f;
    private int   flickerT    = 0;

    // ── Particles ─────────────────────────────────────────────────────────────
    private static final int PC = 40;
    private final float[] px=new float[PC],py=new float[PC],
                          pvx=new float[PC],pvy=new float[PC],
                          pa=new float[PC],ps=new float[PC];
    private final Random rng = new Random();

    // ── Fonts ─────────────────────────────────────────────────────────────────
    private final Font fontTitle;
    private final Font fontBtn;
    private final Font fontBody;
    private final Font fontStat;

    // ─────────────────────────────────────────────────────────────────────────

    public GameOverScreen(ScreenManager screenManager) {
        this(screenManager, false, 0, 0, null, null);
    }

    public GameOverScreen(ScreenManager sm, boolean won, int rounds,
                          int totalKills, int[] kills, Runnable onBack) {
        this.screenManager     = sm;
        this.teamWon           = won;
        this.roundsCleared     = rounds;
        this.totalEnemiesKilled= totalKills;
        this.killsPerPlayer    = copyKills(kills);
        this.onBackToLobby     = onBack;

        this.gameOverBgImg = AssetManager.getInstance().get("game_over_bg");
        this.victoryBgImg  = AssetManager.getInstance().get("victory_bg");

        int W = Constants.SCREEN_WIDTH;
        int btnY = Constants.SCREEN_HEIGHT - 110;
        runStatsBtn    = new Rectangle(W/2 - 260, btnY, 220, 48);
        backToLobbyBtn = new Rectangle(W/2 +  40, btnY, 220, 48);

        fontTitle = ttf("/assets/fonts/PressStart2P-Regular.ttf", 32f);
        fontBtn   = ttf("/assets/fonts/PressStart2P-Regular.ttf", 10f);
        fontBody  = ttf("/assets/fonts/VT323-Regular.ttf",        22f);
        fontStat  = ttf("/assets/fonts/VT323-Regular.ttf",        20f);
    }

    private Font ttf(String path, float sz) {
        try {
            InputStream is = getClass().getResourceAsStream(path);
            if (is==null) return new Font("Monospaced", Font.BOLD, (int)sz);
            return Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(sz);
        } catch (Exception e) { return new Font("Monospaced", Font.BOLD, (int)sz); }
    }

    // ── Particles ─────────────────────────────────────────────────────────────
    private void spawnParticle(int i, boolean anyY) {
        px[i]  = rng.nextFloat() * Constants.SCREEN_WIDTH;
        py[i]  = anyY ? rng.nextFloat() * Constants.SCREEN_HEIGHT : Constants.SCREEN_HEIGHT + 5;
        pvx[i] = (rng.nextFloat()-0.5f)*0.8f;
        pvy[i] = -(0.5f + rng.nextFloat()*1.4f);
        pa[i]  = 0.15f + rng.nextFloat()*0.5f;
        ps[i]  = 2f + rng.nextFloat()*5f;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    @Override public void onEnter() {
        tick=0; titleAlpha=0f; statsAlpha=0f; flickerA=1f;
        statsModalOpen=false; runStatsHovered=false; backHovered=false;
        for (int i=0;i<PC;i++) spawnParticle(i,true);
    }

    @Override public void onExit() {}

    // ── Update ────────────────────────────────────────────────────────────────
    @Override public void update() {
        tick++;
        // Fade-in title (60 ticks = 1s)
        titleAlpha = Math.min(1f, titleAlpha + 0.018f);
        // Stats panel fades in after title
        if (tick > 50) statsAlpha = Math.min(1f, statsAlpha + 0.02f);

        // Flicker on GAME OVER (not victory)
        if (!teamWon) {
            flickerT++;
            if (flickerT%10==0) flickerA = 0.7f + rng.nextFloat()*0.3f;
        }

        // Particles
        for (int i=0;i<PC;i++) {
            px[i]+=pvx[i]; py[i]+=pvy[i]; pa[i]-=0.003f;
            if (pa[i]<=0||py[i]<-10) spawnParticle(i,false);
        }
    }

    // ── Render ────────────────────────────────────────────────────────────────
    @Override public void render(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        drawBackground(g);
        drawParticles(g);
        drawVignette(g);
        drawTitle(g);
        drawStatsPanel(g);
        drawButtons(g);

        if (statsModalOpen) drawRunStatsModal(g);
    }

    // ── Background ────────────────────────────────────────────────────────────
    private void drawBackground(Graphics2D g) {
        int W=Constants.SCREEN_WIDTH, H=Constants.SCREEN_HEIGHT;
        BufferedImage bg = teamWon ? victoryBgImg : gameOverBgImg;
        // Only use if not magenta fallback
        if (bg!=null && bg.getRGB(0,0)!=Color.MAGENTA.getRGB()) {
            g.drawImage(bg,0,0,W,H,null);
            g.setColor(new Color(0,0,0,teamWon?60:100));
            g.fillRect(0,0,W,H);
            return;
        }
        // Procedural fallback
        if (teamWon) {
            GradientPaint grad = new GradientPaint(0,0,new Color(0x05,0x14,0x05),
                                                   0,H,new Color(0x0A,0x20,0x0A));
            g.setPaint(grad); g.fillRect(0,0,W,H);
            // Green grid
            g.setColor(new Color(0x39,0xFF,0x14,12));
        } else {
            GradientPaint grad = new GradientPaint(0,0,DARK_BG,0,H,VOID_PURPLE);
            g.setPaint(grad); g.fillRect(0,0,W,H);
            // Purple grid
            g.setColor(new Color(0x7B,0x2F,0xBE,12));
        }
        for (int x=0;x<W;x+=42) g.drawLine(x,0,x,H);
        for (int y=0;y<H;y+=42) g.drawLine(0,y,W,y);
    }

    private void drawParticles(Graphics2D g) {
        for (int i=0;i<PC;i++) {
            float a=Math.max(0,pa[i]);
            if (teamWon)
                g.setColor(new Color(0.1f,0.8f,0.1f,a));
            else
                // Alternate blood and purple
                g.setColor(i%2==0 ? new Color(0.6f,0f,0f,a) : new Color(0.48f,0.18f,0.74f,a));
            int s=(int)ps[i];
            g.fillRect((int)px[i],(int)py[i],s,s);
        }
    }

    private void drawVignette(Graphics2D g) {
        int W=Constants.SCREEN_WIDTH, H=Constants.SCREEN_HEIGHT;
        RadialGradientPaint v = new RadialGradientPaint(W/2f,H/2f,Math.max(W,H)/1.3f,
            new float[]{0f,1f}, new Color[]{new Color(0,0,0,0),new Color(0,0,0,220)});
        g.setPaint(v); g.fillRect(0,0,W,H);
    }

    // ── Title ─────────────────────────────────────────────────────────────────
    private void drawTitle(Graphics2D g) {
        int W=Constants.SCREEN_WIDTH;
        String line1 = teamWon ? "VICTORY!" : "GAME";
        String line2 = teamWon ? null        : "OVER";
        Color  mainC = teamWon ? TOXIC_GREEN  : GHOSTLY_WHITE;
        Color  shadC = teamWon ? new Color(0x00,0x88,0x00) : BLOOD_RED;

        Font tf = fontTitle.deriveFont(teamWon ? 36f : 42f);
        g.setFont(tf);
        FontMetrics fm=g.getFontMetrics();

        // Glow passes
        Color glowC = teamWon ? new Color(0x39,0xFF,0x14,20) : new Color(0x8B,0x00,0x00,18);
        for (int r=14;r>0;r-=2) {
            g.setColor(glowC);
            g.drawString(line1, W/2-fm.stringWidth(line1)/2+r, 180+r);
            g.drawString(line1, W/2-fm.stringWidth(line1)/2-r, 180-r);
        }

        // Shadow
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, titleAlpha));
        g.setColor(shadC);
        g.drawString(line1, W/2-fm.stringWidth(line1)/2+3, 183);

        // Main — flicker for GAME OVER
        float drawAlpha = teamWon ? titleAlpha : titleAlpha*flickerA;
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, drawAlpha));
        g.setColor(mainC);
        g.drawString(line1, W/2-fm.stringWidth(line1)/2, 180);

        if (line2 != null) {
            g.setColor(shadC);
            g.drawString(line2, W/2-fm.stringWidth(line2)/2+3, 240);
            g.setColor(mainC);
            g.drawString(line2, W/2-fm.stringWidth(line2)/2, 237);
        }
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

        // Subtitle
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1f,titleAlpha*0.8f)));
        g.setFont(fontBody.deriveFont(20f));
        g.setColor(teamWon ? new Color(0xAA,0xFF,0xAA) : new Color(0xAA,0xAA,0xCC));
        String sub = teamWon
            ? "The spirits have been cleansed."
            : "The darkness has consumed you.";
        fm=g.getFontMetrics();
        g.drawString(sub, W/2-fm.stringWidth(sub)/2, teamWon?228:276);

        // Decorative divider
        int ly = teamWon ? 248 : 296, lx=W/2-180;
        g.setColor(teamWon ? new Color(0x39,0xFF,0x14,120) : new Color(0x8B,0x00,0x00,120));
        g.setStroke(new BasicStroke(1.5f));
        g.drawLine(lx,ly,lx+360,ly);
        for (int d=0;d<5;d++) {
            int dx=lx+36+d*72;
            g.fillOval(dx-2,ly-2,5,5);
            g.fillRect(dx-1,ly+3,2,3+d%3*2);
        }
        g.setStroke(new BasicStroke(1f));
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
    }

    // ── Stats panel (always visible, fades in) ────────────────────────────────
    private void drawStatsPanel(Graphics2D g) {
        if (statsAlpha <= 0) return;
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, statsAlpha));

        int W=Constants.SCREEN_WIDTH;
        int panelW=460, panelH=260;
        int panelX=W/2-panelW/2, panelY=310;

        // Panel body
        g.setColor(new Color(0x0D,0x05,0x1E,210));
        g.fillRoundRect(panelX,panelY,panelW,panelH,12,12);
        Color border = teamWon ? new Color(0x39,0xFF,0x14,160) : new Color(0x8B,0x00,0x00,160);
        g.setColor(border);
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(panelX,panelY,panelW,panelH,12,12);
        g.setStroke(new BasicStroke(1f));

        // Top accent bar
        g.setColor(new Color(border.getRed(),border.getGreen(),border.getBlue(),100));
        g.fillRoundRect(panelX,panelY,panelW,5,12,12);

        // Header
        g.setFont(fontBtn.deriveFont(9f));
        g.setColor(teamWon ? TOXIC_GREEN : AMBER);
        drawCentered(g, "RUN RESULTS", W/2, panelY+24);

        // Summary rows
        int lx=panelX+50, vx=panelX+panelW-130, y=panelY+52;
        drawStatLine(g, lx, vx, y, "Rounds Cleared", roundsCleared+" / "+Constants.TOTAL_ROUNDS);
        y+=30;
        drawStatLine(g, lx, vx, y, "Total Kills",    String.valueOf(totalEnemiesKilled));
        y+=38;

        // Player kills header
        g.setFont(fontBody.deriveFont(16f));
        g.setColor(new Color(0x66,0x55,0x88));
        g.drawString("Kills per player:", lx, y); y+=26;

        // Per-player kill bars
        for (int i=0;i<Constants.MAX_PLAYERS;i++) {
            drawPlayerKillRow(g, lx, vx, panelX+panelW-50, y, i); y+=36;
        }

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
    }

    private void drawStatLine(Graphics2D g, int lx, int vx, int y, String label, String value) {
        g.setFont(fontBody.deriveFont(18f));
        g.setColor(PURPLE_GLOW);
        g.drawString(label+":", lx, y);
        g.setColor(GHOSTLY_WHITE);
        g.drawString(value, vx, y);
    }

    private void drawPlayerKillRow(Graphics2D g, int lx, int vx, int maxX, int y, int idx) {
        Color pc = PLAYER_COLORS[idx];
        int kills = killsPerPlayer[idx];

        // Badge
        g.setColor(new Color(pc.getRed(),pc.getGreen(),pc.getBlue(),50));
        g.fillOval(lx-4,y-18,26,26);
        g.setColor(pc);
        g.fillOval(lx,y-16,20,20);
        g.setFont(fontStat.deriveFont(13f));
        g.setColor(DARK_BG);
        FontMetrics fm=g.getFontMetrics();
        String badge="P"+(idx+1);
        g.drawString(badge,lx+10-fm.stringWidth(badge)/2,y-2);

        // Name
        g.setFont(fontBody.deriveFont(17f));
        g.setColor(new Color(0xAA,0xAA,0xCC));
        g.drawString("Player "+(idx+1), lx+28, y);

        // Kill count
        g.setFont(fontBtn.deriveFont(9f));
        g.setColor(kills>0 ? pc : new Color(0x44,0x33,0x55));
        g.drawString(String.valueOf(kills), vx, y);

        // Mini kill bar
        int barX=lx+28, barW=vx-barX-20, barH=4, barY=y+4;
        int maxKills = getMaxKills();
        float pct = maxKills>0 ? Math.min(1f,(float)kills/maxKills) : 0f;
        g.setColor(new Color(0x1A,0x0A,0x2E));
        g.fillRoundRect(barX,barY,barW,barH,2,2);
        if (pct>0) {
            g.setColor(new Color(pc.getRed(),pc.getGreen(),pc.getBlue(),180));
            g.fillRoundRect(barX,barY,(int)(barW*pct),barH,2,2);
        }
    }

    // ── Buttons ───────────────────────────────────────────────────────────────
    private void drawButtons(Graphics2D g) {
        drawHorrorBtn(g, runStatsBtn,    "VIEW STATS",   runStatsHovered, PURPLE_GLOW);
        drawHorrorBtn(g, backToLobbyBtn, "BACK TO LOBBY",backHovered,
                      teamWon ? TOXIC_GREEN : BRIGHT_RED);
    }

    private void drawHorrorBtn(Graphics2D g, Rectangle r, String label, boolean hovered, Color accent) {
        int x=r.x,y=r.y,w=r.width,h=r.height;
        if (hovered) {
            g.setColor(new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),80));
            g.fillRoundRect(x-5,y-5,w+10,h+10,12,12);
        }
        g.setColor(new Color(0x0D,0x05,0x1E,210));
        g.fillRoundRect(x,y,w,h,8,8);
        g.setColor(new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),hovered?255:130));
        g.setStroke(new BasicStroke(hovered?2f:1.5f));
        g.drawRoundRect(x,y,w,h,8,8);
        g.setStroke(new BasicStroke(1f));
        g.setColor(accent);
        g.fillRect(x+1,y+8,3,h-16);

        g.setFont(fontBtn.deriveFont(8f));
        FontMetrics fm=g.getFontMetrics();
        int tx=x+w/2-fm.stringWidth(label)/2;
        int ty=y+h/2+fm.getAscent()/2-2;
        g.setColor(new Color(0,0,0,140)); g.drawString(label,tx+2,ty+2);
        g.setColor(hovered?GHOSTLY_WHITE:new Color(
            (accent.getRed()+GHOSTLY_WHITE.getRed())/2,
            (accent.getGreen()+GHOSTLY_WHITE.getGreen())/2,
            (accent.getBlue()+GHOSTLY_WHITE.getBlue())/2));
        g.drawString(label,tx,ty);
    }

    // ── Stats modal (click VIEW STATS) ────────────────────────────────────────
    private void drawRunStatsModal(Graphics2D g) {
        // Dim
        g.setColor(new Color(0,0,0,180));
        g.fillRect(0,0,Constants.SCREEN_WIDTH,Constants.SCREEN_HEIGHT);

        int W=Constants.SCREEN_WIDTH, H=Constants.SCREEN_HEIGHT;
        int mW=500,mH=420,mX=W/2-mW/2,mY=H/2-mH/2;

        // Panel
        g.setColor(new Color(0x0D,0x05,0x1E,240));
        g.fillRoundRect(mX,mY,mW,mH,14,14);
        g.setColor(PURPLE_GLOW);
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(mX,mY,mW,mH,14,14);
        g.setStroke(new BasicStroke(1f));
        // Top accent
        g.setColor(new Color(0x8B,0x00,0x00,160));
        g.fillRoundRect(mX,mY,mW,5,14,14);

        // Title
        g.setFont(fontBtn.deriveFont(10f));
        g.setColor(AMBER);
        drawCentered(g,"FULL RUN STATS",W/2,mY+30);

        // Stat rows
        int lx=mX+60,vx=mX+mW-100,y=mY+65;
        g.setFont(fontBody.deriveFont(18f));
        drawStatLine(g,lx,vx,y,"Rounds Cleared",roundsCleared+" / "+Constants.TOTAL_ROUNDS); y+=32;
        drawStatLine(g,lx,vx,y,"Total Kills",String.valueOf(totalEnemiesKilled)); y+=32;
        drawStatLine(g,lx,vx,y,"Result",teamWon?"VICTORY":"DEFEATED"); y+=48;

        g.setFont(fontBody.deriveFont(16f));
        g.setColor(new Color(0x66,0x55,0x88));
        g.drawString("Kills per player:", lx, y); y+=26;

        for (int i=0;i<Constants.MAX_PLAYERS;i++) {
            drawPlayerKillRow(g,lx,vx,mX+mW-40,y,i); y+=36;
        }

        g.setFont(fontBody.deriveFont(16f));
        g.setColor(new Color(0x44,0x33,0x55));
        drawCentered(g,"Click anywhere to close",W/2,mY+mH-16);
    }

    // ── Input ─────────────────────────────────────────────────────────────────
    @Override public void handleMouseClicked(int x, int y) {
        if (statsModalOpen) { statsModalOpen=false; return; }
        if (runStatsBtn.contains(x,y))    { statsModalOpen=true; return; }
        if (backToLobbyBtn.contains(x,y)) backToLobby();
    }

    @Override public void handleMouseMoved(int x, int y) {
        runStatsHovered = runStatsBtn.contains(x,y);
        backHovered     = backToLobbyBtn.contains(x,y);
    }

    @Override public void handleKeyPressed(int keyCode) {
        if (keyCode==KeyEvent.VK_ENTER) backToLobby();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private void backToLobby() { if (onBackToLobby!=null) onBackToLobby.run(); }

    private void drawCentered(Graphics2D g, String text, int cx, int y) {
        FontMetrics fm=g.getFontMetrics();
        g.drawString(text, cx-fm.stringWidth(text)/2, y);
    }

    private int getMaxKills() {
        int m=1;
        for (int k:killsPerPlayer) if(k>m) m=k;
        return m;
    }

    private int[] copyKills(int[] src) {
        int[] copy=new int[Constants.MAX_PLAYERS];
        if (src==null) return copy;
        int len=Math.min(src.length,copy.length);
        for (int i=0;i<len;i++) copy[i]=src[i];
        return copy;
    }
}
