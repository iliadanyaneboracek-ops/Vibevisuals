package ru.suppelemen.vibevisuals.feature.utility;

import net.minecraft.client.MinecraftClient;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;

/**
 * Smooth FOV-based zoom. The held state is driven by a keybinding in
 * {@code VibeVisualsClient}; the actual FOV scaling is applied in
 * {@code GameRendererMixin}.
 */
public final class ZoomController {
    private static boolean active;
    private static float currentFactor = 1.0f;
    private static long lastUpdateNanos;

    private ZoomController() {
    }

    public static void setActive(boolean value) {
        active = value;
    }

    public static boolean isZooming() {
        return active && VibeVisualsConfigManager.get().zoom.enabled && currentFactor < 0.999f;
    }

    /**
     * @return the multiplier applied to the computed FOV, in {@code (0, 1]}.
     */
    public static float currentFactor() {
        VibeVisualsConfig.ZoomConfig config = VibeVisualsConfigManager.get().zoom;
        float target = (active && config.enabled) ? config.zoomFactor : 1.0f;

        if (!config.smooth) {
            currentFactor = target;
            lastUpdateNanos = 0L;
            return currentFactor;
        }

        long now = System.nanoTime();
        if (lastUpdateNanos == 0L) {
            lastUpdateNanos = now;
        }
        float deltaSeconds = (now - lastUpdateNanos) / 1_000_000_000.0f;
        lastUpdateNanos = now;

        // Convert the 0..1 animation speed into a smoothing rate; higher = snappier.
        float rate = 1.0f - (float) Math.exp(-deltaSeconds * (4.0f + config.animationSpeed * 26.0f));
        currentFactor += (target - currentFactor) * Math.min(1.0f, Math.max(0.0f, rate));
        if (Math.abs(currentFactor - target) < 0.001f) {
            currentFactor = target;
        }
        return currentFactor;
    }

    public static void tick(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            active = false;
        }
    }
}