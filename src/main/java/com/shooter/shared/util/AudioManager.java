package com.shooter.shared.util;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.InputStream;

/**
 * ============================================================
 * FILE: AudioManager.java
 * PACKAGE: shared.util
 * OWNER: Geastin (Member C - Screens / Systems / Assets)
 * ============================================================
 *
 * Loads and plays WAV audio clips.
 *
 * RULES:
 *  - Stub-safe: if a WAV file is missing or fails to load,
 *    the play method does nothing — the game never crashes on audio.
 *  - Ambient music loops continuously until stopAmbient() is called.
 *  - SFX clips (shoot, hit, powerup) are one-shot plays.
 *
 * USAGE:
 *   AudioManager.getInstance().playAmbient();   // start background music
 *   AudioManager.getInstance().playShoot();     // fire SFX
 *   AudioManager.getInstance().playHit();       // hit SFX
 *   AudioManager.getInstance().playPowerUp();   // pickup SFX
 *   AudioManager.getInstance().stopAmbient();   // stop music
 * ============================================================
 */
public class AudioManager {

    // ── Singleton ─────────────────────────────────────────────────────────────
    private static AudioManager instance;

    public static AudioManager getInstance() {
        if (instance == null) {
            instance = new AudioManager();
        }
        return instance;
    }

    // ── Ambient music clip (kept so we can stop it) ───────────────────────────
    private Clip ambientClip;

    // Private constructor
    private AudioManager() {}

    // ── Public playback methods ───────────────────────────────────────────────

    /**
     * Starts the background ambient music on a continuous loop.
     * Safe to call every tick — does nothing if ambient is already playing.
     */
    public void playAmbient() {
        // Already playing? Do nothing.
        if (ambientClip != null && ambientClip.isRunning()) {
            return;
        }
        stopAmbient(); // clean up any stopped clip
        ambientClip = loadClip("/assets/audio/ambient.wav");
        if (ambientClip != null) {
            ambientClip.loop(Clip.LOOP_CONTINUOUSLY);
            System.out.println("[AudioManager] Ambient music started.");
        }
    }

    /** Stops the ambient music if it is currently playing. */
    public void stopAmbient() {
        if (ambientClip != null && ambientClip.isRunning()) {
            ambientClip.stop();
            ambientClip.close();
            System.out.println("[AudioManager] Ambient music stopped.");
        }
        ambientClip = null;
    }

    /** Plays the bullet-fire sound effect once. */
    public void playShoot() {
        playOnce("/assets/audio/shoot.wav");
    }

    /** Plays the enemy-hit / death sound effect once. */
    public void playHit() {
        playOnce("/assets/audio/hit.wav");
    }

    /** Plays the power-up pickup sound effect once. */
    public void playPowerUp() {
        playOnce("/assets/audio/powerup.wav");
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    /**
     * Loads a WAV clip from the classpath and starts it once.
     * If the file is missing or cannot be decoded, prints a warning and returns.
     */
    private void playOnce(String path) {
        Clip clip = loadClip(path);
        if (clip == null) return;

        // Release the clip's resources automatically when it finishes
        clip.addLineListener(event -> {
            if (event.getType() == LineEvent.Type.STOP) {
                clip.close();
            }
        });

        clip.start();
    }

    /**
     * Loads a WAV file from the classpath into a Clip object.
     *
     * @param path Classpath path to the WAV file (e.g. "/assets/audio/shoot.wav").
     * @return A ready-to-play Clip, or null if the file is missing or unreadable.
     */
    private Clip loadClip(String path) {
        try {
            InputStream is = getClass().getResourceAsStream(path);
            if (is == null) {
                System.out.println("[AudioManager] MISSING: " + path + " → skipping");
                return null;
            }
            // BufferedInputStream is required by AudioSystem — prevents stream reset errors
            AudioInputStream ais = AudioSystem.getAudioInputStream(
                new BufferedInputStream(is));
            Clip clip = AudioSystem.getClip();
            clip.open(ais);
            return clip;
        } catch (Exception e) {
            System.out.println("[AudioManager] Could not load " + path
                + ": " + e.getMessage() + " → skipping");
            return null;
        }
    }
}
