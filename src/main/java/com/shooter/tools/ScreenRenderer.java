package com.shooter.tools;

import com.shooter.client.ScreenManager;
import com.shooter.client.screens.GameOverScreen;
import com.shooter.client.screens.PauseScreen;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.io.File;

public class ScreenRenderer {
    public static void main(String[] args) throws Exception {
        int w = 800, h = 800;
        JPanel panel = new JPanel();
        panel.setSize(w, h);
        ScreenManager manager = new ScreenManager(panel);

        // Create loss screen preview
        GameOverScreen loss = new GameOverScreen(manager, false, 3, 127, new int[]{30,40,27,30}, () -> {});
        BufferedImage imgLoss = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = imgLoss.createGraphics();
        loss.onEnter();
        loss.render(g);
        g.dispose();

        File outDir = new File("out/screens_preview");
        outDir.mkdirs();
        ImageIO.write(imgLoss, "png", new File(outDir, "gameover_preview.png"));
        System.out.println("Wrote: " + new File(outDir, "gameover_preview.png").getAbsolutePath());

        // Create victory screen preview
        GameOverScreen win = new GameOverScreen(manager, true, 5, 321, new int[]{90,80,70,81}, () -> {});
        BufferedImage imgWin = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = imgWin.createGraphics();
        win.onEnter();
        win.render(g2);
        g2.dispose();
        ImageIO.write(imgWin, "png", new File(outDir, "victory_preview.png"));
        System.out.println("Wrote: " + new File(outDir, "victory_preview.png").getAbsolutePath());

        // Create pause screen preview
        PauseScreen pause = new PauseScreen(manager);
        BufferedImage imgPause = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g3 = imgPause.createGraphics();
        pause.onEnter();
        pause.render(g3);
        g3.dispose();
        ImageIO.write(imgPause, "png", new File(outDir, "pause_preview.png"));
        System.out.println("Wrote: " + new File(outDir, "pause_preview.png").getAbsolutePath());
    }
}
