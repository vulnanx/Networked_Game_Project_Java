package com.shooter.client.screens;

import com.shooter.client.ScreenManager;
import com.shooter.shared.util.Constants;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.InputStream;
import java.util.Random;

/**
 * ============================================================
 * FILE: PauseScreen.java
 * PACKAGE: client.screens
 * OWNER: Geastin (Member C) — UI REDESIGN: Christel
 * ============================================================
 * Dark horror pixel art pause overlay.
 * Draws ON TOP of the frozen game world.
 * All constructor signatures and logic preserved exactly.
 * ============================================================
 */
public class PauseScreen implements Screen {

    // ── Horror palette ────────────────────────────────────────────────────────
    private static final Color VOID_PURPLE   = new Color(0x1A, 0x0A, 0x2E);
    private static final Color TOXIC_GREEN   = new Color(0x39, 0xFF, 0x14);
    private static final Color BLOOD_RED     = new Color(0x8B, 0x00, 0x00);
    private static final Color BRIGHT_RED    = new Color(0xCC, 0x00, 0x00);
    private static final Color GHOSTLY_WHITE = new Color(0xE8, 0xE8, 0xF0);
    private static final Color DARK_BG       = new Color(0x0D, 0x05, 0x1E);
    private static final Color PURPLE_GLOW   = new Color(0x7B, 0x2F, 0xBE);

    // ── Core refs ─────────────────────────────────────────────────────────────
    private final ScreenManager screenManager;
    private final Runnable      onResume;
    private final Runnable      onExitGame;

    // ── Buttons ───────────────────────────────────────────────────────────────
    private final Rectangle resumeBtn;
    private final Rectangle exitBtn;
    private int   hoveredIndex = -1;
    private float btnPulse     = 0f;

    // ── Animation ─────────────────────────────────────────────────────────────
    private int   tick       = 0;
    private float panelAlpha = 0f;   // fade-in
    private float scanOffset = 0f;   // CRT scanline drift

    // ── Particles (sparse, eerie) ─────────────────────────────────────────────
    private static final int PC = 18;
    private final float[] px=new float[PC],py=new float[PC],
                          pvx=new float[PC],pvy=new float[PC],
                          pa=new float[PC],ps=new float[PC];
    private final Random rng = new Random();

    // ── Fonts ─────────────────────────────────────────────────────────────────
    private final Font fontTitle;
    private final Font fontBtn;
    private final Font fontBody;

    // ─────────────────────────────────────────────────────────────────────────

    public PauseScreen(ScreenManager screenManager) {
        this(screenManager, null, null);
    }

    public PauseScreen(ScreenManager screenManager, Runnable onResume, Runnable onExitGame) {
        this.screenManager = screenManager;
        this.onResume      = onResume;
        this.onExitGame    = onExitGame;

        int W = Constants.SCREEN_WIDTH;
        int cx = W / 2;
        int btnW = 260, btnH = 50;
        resumeBtn = new Rectangle(cx - btnW/2, 390, btnW, btnH);
        exitBtn   = new Rectangle(cx - btnW/2, 460, btnW, btnH);

        fontTitle = ttf("/assets/fonts/PressStart2P-Regular.ttf", 24f);
        fontBtn   = ttf("/assets/fonts/PressStart2P-Regular.ttf", 10f);
        fontBody  = ttf("/assets/fonts/VT323-Regular.ttf",        20f);
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
        int W=Constants.SCREEN_WIDTH, H=Constants.SCREEN_HEIGHT;
        px[i]  = rng.nextFloat()*W;
        py[i]  = anyY ? rng.nextFloat()*H : H+5;
        pvx[i] = (rng.nextFloat()-0.5f)*0.4f;
        pvy[i] = -(0.2f+rng.nextFloat()*0.7f);
        pa[i]  = 0.08f+rng.nextFloat()*0.3f;
        ps[i]  = 1.5f+rng.nextFloat()*3f;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    @Override public void onEnter() {
        tick=0; panelAlpha=0f; hoveredIndex=-1;
        for (int i=0;i<PC;i++) spawnParticle(i,true);
        System.out.println("[PauseScreen] Game paused.");
    }

    @Override public void onExit() {
        System.out.println("[PauseScreen] Leaving pause screen.");
    }

    // ── Update ────────────────────────────────────────────────────────────────
    @Override public void update() {
        tick++;
        panelAlpha = Math.min(1f, panelAlpha+0.06f);  // fast fade-in
        if (hoveredIndex!=-1)
            btnPulse = (float)Math.sin(tick*0.14f)*0.5f+0.5f;
        scanOffset = (scanOffset+0.8f)%Constants.SCREEN_HEIGHT;

        for (int i=0;i<PC;i++) {
            px[i]+=pvx[i]; py[i]+=pvy[i]; pa[i]-=0.002f;
            if (pa[i]<=0||py[i]<-10) spawnParticle(i,false);
        }
    }

    // ── Render ────────────────────────────────────────────────────────────────
    @Override public void render(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        drawDim(g);
        drawScanlines(g);
        drawParticles(g);
        drawPanel(g);
        drawTitle(g);
        drawHint(g);
        drawButtons(g);
    }

    // ── Full-screen dim (lets the game world show through darkened) ───────────
    private void drawDim(Graphics2D g) {
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, panelAlpha));
        // Two-layer dim: base dark + subtle purple tint
        g.setColor(new Color(0,0,0,160));
        g.fillRect(0,0,Constants.SCREEN_WIDTH,Constants.SCREEN_HEIGHT);
        g.setColor(new Color(0x1A,0x0A,0x2E,60));
        g.fillRect(0,0,Constants.SCREEN_WIDTH,Constants.SCREEN_HEIGHT);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
    }

    // ── CRT scanline effect (eerie, "frozen signal" feel) ────────────────────
    private void drawScanlines(Graphics2D g) {
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                        panelAlpha * 0.18f));
        g.setColor(new Color(0,0,0,255));
        int H=Constants.SCREEN_HEIGHT, W=Constants.SCREEN_WIDTH;
        for (int y=0;y<H;y+=3) g.fillRect(0,y,W,1);
        // Moving bright scan line
        g.setColor(new Color(0x7B,0x2F,0xBE,40));
        int scanY=(int)scanOffset;
        g.fillRect(0,scanY,W,2);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
    }

    // ── Sparse particles (purple/blood drifting up) ───────────────────────────
    private void drawParticles(Graphics2D g) {
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, panelAlpha));
        for (int i=0;i<PC;i++) {
            float a=Math.max(0,pa[i]);
            g.setColor(i%2==0 ? new Color(0.48f,0.18f,0.74f,a) : new Color(0.55f,0f,0f,a));
            int s=(int)ps[i];
            g.fillRect((int)px[i],(int)py[i],s,s);
        }
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
    }

    // ── Central pause panel ───────────────────────────────────────────────────
    private void drawPanel(Graphics2D g) {
        int W=Constants.SCREEN_WIDTH;
        int panelW=380, panelH=300;
        int panelX=W/2-panelW/2, panelY=270;

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, panelAlpha));

        // Outer glow
        g.setColor(new Color(0x7B,0x2F,0xBE,30));
        g.fillRoundRect(panelX-8,panelY-8,panelW+16,panelH+16,18,18);

        // Body
        g.setColor(new Color(0x0D,0x05,0x1E,230));
        g.fillRoundRect(panelX,panelY,panelW,panelH,12,12);

        // Purple border
        g.setColor(new Color(0x7B,0x2F,0xBE,200));
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(panelX,panelY,panelW,panelH,12,12);
        g.setStroke(new BasicStroke(1f));

        // Blood red top accent bar
        g.setColor(new Color(0x8B,0x00,0x00,180));
        g.fillRoundRect(panelX,panelY,panelW,5,12,12);

        // Corner skull ornaments (pixel squares as corner detail)
        g.setColor(new Color(0x8B,0x00,0x00,120));
        int cs=6;
        g.fillRect(panelX+8,     panelY+8,      cs,cs);
        g.fillRect(panelX+panelW-8-cs, panelY+8,cs,cs);
        g.fillRect(panelX+8,     panelY+panelH-8-cs,cs,cs);
        g.fillRect(panelX+panelW-8-cs,panelY+panelH-8-cs,cs,cs);

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
    }

    // ── "PAUSED" title ────────────────────────────────────────────────────────
    private void drawTitle(Graphics2D g) {
        int W=Constants.SCREEN_WIDTH;
        String text = "PAUSED";

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, panelAlpha));

        // Glow passes
        g.setFont(fontTitle.deriveFont(26f));
        FontMetrics fm=g.getFontMetrics();
        int tx=W/2-fm.stringWidth(text)/2;
        for (int r=10;r>0;r-=2) {
            g.setColor(new Color(0x7B,0x2F,0xBE,16));
            g.drawString(text,tx+r,310+r);
            g.drawString(text,tx-r,310-r);
        }

        // Shadow
        g.setColor(BLOOD_RED);
        g.drawString(text, tx+3, 313);

        // Main — ghostly white
        g.setColor(GHOSTLY_WHITE);
        g.drawString(text, tx, 310);

        // Decorative blood divider under title
        int lx=W/2-140, ly=328;
        g.setColor(new Color(0x8B,0x00,0x00,140));
        g.setStroke(new BasicStroke(1.5f));
        g.drawLine(lx,ly,lx+280,ly);
        for (int d=0;d<4;d++) {
            int dx=lx+35+d*70;
            g.fillOval(dx-2,ly-2,5,5);
            g.fillRect(dx-1,ly+3,2,3+d%3*2);
        }
        g.setStroke(new BasicStroke(1f));

        // Subtext
        g.setFont(fontBody.deriveFont(17f));
        g.setColor(new Color(0x66,0x55,0x88));
        String hint="ALL PLAYERS FROZEN";
        fm=g.getFontMetrics();
        g.drawString(hint, W/2-fm.stringWidth(hint)/2, 355);

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
    }

    // ── ESC hint at bottom ────────────────────────────────────────────────────
    private void drawHint(Graphics2D g) {
        int W=Constants.SCREEN_WIDTH;
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, panelAlpha*0.7f));
        g.setFont(fontBody.deriveFont(16f));
        g.setColor(new Color(0x44,0x33,0x55));
        String hint="Press ESC or P to resume";
        FontMetrics fm=g.getFontMetrics();
        g.drawString(hint, W/2-fm.stringWidth(hint)/2, 530);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
    }

    // ── Buttons ───────────────────────────────────────────────────────────────
    private void drawButtons(Graphics2D g) {
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, panelAlpha));
        drawHorrorBtn(g, resumeBtn, "▶ RESUME",   hoveredIndex==0, TOXIC_GREEN);
        drawHorrorBtn(g, exitBtn,   "✕ EXIT GAME", hoveredIndex==1, BRIGHT_RED);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
    }

    private void drawHorrorBtn(Graphics2D g, Rectangle r, String label,
                                boolean hovered, Color accent) {
        int x=r.x,y=r.y,w=r.width,h=r.height;

        // Outer glow on hover
        if (hovered) {
            float glow=0.3f+btnPulse*0.4f;
            g.setColor(new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),(int)(glow*90)));
            g.fillRoundRect(x-6,y-6,w+12,h+12,14,14);
        }

        // Body
        g.setColor(new Color(0x0D,0x05,0x1E,220));
        g.fillRoundRect(x,y,w,h,8,8);

        // Border
        int ba=hovered?255:130;
        g.setColor(new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),ba));
        g.setStroke(new BasicStroke(hovered?2.5f:1.5f));
        g.drawRoundRect(x,y,w,h,8,8);
        g.setStroke(new BasicStroke(1f));

        // Left accent bar
        g.setColor(accent);
        g.fillRect(x+1,y+8,3,h-16);

        // Label
        g.setFont(fontBtn.deriveFont(9f));
        FontMetrics fm=g.getFontMetrics();
        int tx=x+w/2-fm.stringWidth(label)/2;
        int ty=y+h/2+fm.getAscent()/2-2;
        g.setColor(new Color(0,0,0,150)); g.drawString(label,tx+2,ty+2);
        g.setColor(hovered ? GHOSTLY_WHITE : new Color(
            (accent.getRed()+GHOSTLY_WHITE.getRed())/2,
            (accent.getGreen()+GHOSTLY_WHITE.getGreen())/2,
            (accent.getBlue()+GHOSTLY_WHITE.getBlue())/2));
        g.drawString(label,tx,ty);
    }

    // ── Input (all logic unchanged) ───────────────────────────────────────────
    @Override public void handleMouseClicked(int x, int y) {
        if (resumeBtn.contains(x,y)) { resume();   return; }
        if (exitBtn.contains(x,y))   { exitGame();        }
    }

    @Override public void handleMouseMoved(int x, int y) {
        if      (resumeBtn.contains(x,y)) hoveredIndex=0;
        else if (exitBtn.contains(x,y))   hoveredIndex=1;
        else                              hoveredIndex=-1;
    }

    @Override public void handleKeyPressed(int keyCode) {
        if (keyCode==KeyEvent.VK_ESCAPE || keyCode==KeyEvent.VK_P) resume();
    }

    private void resume() {
        if (onResume!=null) onResume.run();
        screenManager.setScreen(null);
    }

    private void exitGame() {
        if (onExitGame!=null) onExitGame.run();
    }
}
