package ru.suppelemen.vibevisuals.feature.sound;

import net.fabricmc.loader.api.FabricLoader;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class CustomHitSoundPlayer {
    private static long lastPlayTime;

    private CustomHitSoundPlayer() {
    }

    public static void init() {
        try {
            Files.createDirectories(soundsDir());
        } catch (IOException ignored) {
        }
    }

    public static Path soundsDir() {
        return FabricLoader.getInstance().getConfigDir().resolve("vibevisuals").resolve("sounds");
    }

    /** Play any sound file (relative to the vibevisuals sounds dir) once. */
    public static void playSoundFile(String fileName, float volume) {
        if (fileName == null || fileName.isBlank()) {
            return;
        }
        Path sound = soundsDir().resolve(fileName).normalize();
        if (!sound.startsWith(soundsDir()) || !Files.isRegularFile(sound)) {
            return;
        }
        Thread thread = new Thread(() -> play(sound, volume), "VibeVisuals Sound");
        thread.setDaemon(true);
        thread.start();
    }

    public static void playCrit() {
        VibeVisualsConfig.CustomHitSoundConfig config = VibeVisualsConfigManager.get().customHitSound;
        if (!config.enabled) {
            return;
        }

        long now = System.nanoTime();
        long cooldownNanos = (long) (config.cooldownTicks * 50_000_000L);
        if (now - lastPlayTime < cooldownNanos) {
            return;
        }
        lastPlayTime = now;

        Path sound = soundsDir().resolve(config.soundFile).normalize();
        if (!sound.startsWith(soundsDir()) || !Files.isRegularFile(sound)) {
            return;
        }

        Thread thread = new Thread(() -> play(sound, config.volume), "VibeVisuals Crit Sound");
        thread.setDaemon(true);
        thread.start();
    }

    private static void play(Path sound, float volume) {
        try (AudioInputStream stream = AudioSystem.getAudioInputStream(sound.toFile())) {
            Clip clip = AudioSystem.getClip();
            clip.open(stream);
            applyVolume(clip, volume);
            clip.start();
        } catch (Exception ignored) {
        }
    }

    private static void applyVolume(Clip clip, float volume) {
        if (!clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            return;
        }

        FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        float clamped = Math.max(0.0f, Math.min(2.0f, volume));
        if (clamped <= 0.0f) {
            gain.setValue(gain.getMinimum());
            return;
        }

        float decibels = (float) (20.0 * Math.log10(clamped));
        gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), decibels)));
    }
}
