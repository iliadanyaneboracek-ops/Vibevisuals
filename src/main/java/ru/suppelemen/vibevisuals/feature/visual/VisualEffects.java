package ru.suppelemen.vibevisuals.feature.visual;

import net.minecraft.client.MinecraftClient;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.util.math.BlockPos;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;

import java.util.concurrent.ThreadLocalRandom;

public final class VisualEffects {
    private static int particleTickCounter;
    private static int cachedParticleColor;
    private static float cachedParticleSize;
    private static DustParticleEffect cachedParticleEffect;

    private VisualEffects() {
    }

    public static void tick(MinecraftClient client) {
        VibeVisualsConfig.VisualEffectsConfig config = VibeVisualsConfigManager.get().visualEffects;
        if (!config.customParticlesEnabled || config.particlesPerTick <= 0 || client.player == null || client.world == null) {
            return;
        }

        particleTickCounter++;
        if (particleTickCounter < config.particleSpawnIntervalTicks) {
            return;
        }
        particleTickCounter = 0;

        ThreadLocalRandom random = ThreadLocalRandom.current();
        DustParticleEffect particle = getParticleEffect(config);
        for (int index = 0; index < config.particlesPerTick; index++) {
            double angle = random.nextDouble(Math.PI * 2.0);
            double radius = Math.sqrt(random.nextDouble()) * config.particleRadius;
            double x = client.player.getX() + Math.cos(angle) * radius;
            double y = client.player.getY() + config.particleYOffset + random.nextDouble(-6.0, 10.0);
            double z = client.player.getZ() + Math.sin(angle) * radius;
            if (!client.world.isChunkLoaded((int) Math.floor(x) >> 4, (int) Math.floor(z) >> 4)) {
                continue;
            }

            BlockPos pos = BlockPos.ofFloored(x, y, z);
            if (!client.world.getWorldBorder().contains(pos)) {
                continue;
            }

            double velocityX = random.nextDouble(-config.particleVelocity, config.particleVelocity);
            double velocityY = random.nextDouble(0.0, config.particleVelocity);
            double velocityZ = random.nextDouble(-config.particleVelocity, config.particleVelocity);
            client.world.addParticleClient(particle, x, y, z, velocityX, velocityY, velocityZ);
        }
    }

    public static float red(int color) {
        return ((color >> 16) & 0xFF) / 255.0f;
    }

    public static float green(int color) {
        return ((color >> 8) & 0xFF) / 255.0f;
    }

    public static float blue(int color) {
        return (color & 0xFF) / 255.0f;
    }

    public static int withOpaqueAlpha(int color) {
        return 0xFF000000 | (color & 0x00FFFFFF);
    }

    private static int toRgb(int color) {
        return color & 0x00FFFFFF;
    }

    private static DustParticleEffect getParticleEffect(VibeVisualsConfig.VisualEffectsConfig config) {
        int color = toRgb(config.particleColor);
        if (cachedParticleEffect == null || cachedParticleColor != color || cachedParticleSize != config.particleSize) {
            cachedParticleColor = color;
            cachedParticleSize = config.particleSize;
            cachedParticleEffect = new DustParticleEffect(color, config.particleSize);
        }

        return cachedParticleEffect;
    }
}
