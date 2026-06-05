package ru.suppelemen.vibevisuals.feature.sound;

import net.minecraft.client.sound.SoundInstance;
import net.minecraft.util.Identifier;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;

/**
 * Per-sound volume control. The actual scaling is applied in
 * {@code SoundSystemMixin} via {@link #multiplierFor(SoundInstance)}.
 */
public final class SoundController {
    private SoundController() {
    }

    public static float multiplierFor(SoundInstance instance) {
        if (instance == null) {
            return 1.0f;
        }

        VibeVisualsConfig.SoundControllerConfig config = VibeVisualsConfigManager.get().soundController;
        if (!config.enabled) {
            return 1.0f;
        }

        Identifier id = instance.getId();
        if (id == null) {
            return 1.0f;
        }

        String path = id.getPath();
        if (path.equals("entity.player.attack.crit")) {
            return config.critVolume;
        }
        if (path.startsWith("entity.player.attack.")) {
            return config.attackVolume;
        }
        if (path.equals("entity.experience_orb.pickup") || path.equals("entity.player.levelup")) {
            return config.experienceVolume;
        }

        return 1.0f;
    }
}