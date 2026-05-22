package ru.suppelemen.vibevisuals.feature.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;

public final class FireOverlayRenderer {
    private static float animationTime;

    private FireOverlayRenderer() {
    }

    public static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        VibeVisualsConfig.FireOverlayConfig config = VibeVisualsConfigManager.get().fireOverlay;
        if (!config.enabled || client.player == null || !client.options.getPerspective().isFirstPerson() || !client.player.isOnFire()) {
            return;
        }

        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();
        animationTime += 0.08f * config.animationSpeed;
        int flameHeight = config.height;
        int baseY = height - flameHeight + config.yOffset;
        int color = withAlpha(config.color, config.opacity);
        int hotColor = withAlpha(0xFFFFF0B5, config.opacity * 0.50f);
        int softColor = withAlpha(config.color, config.opacity * 0.42f);
        int darkColor = withAlpha(0xFF171019, config.opacity * 0.34f);

        int step = Math.max(6, width / config.detail);
        for (int index = -2; index <= config.detail + 2; index++) {
            int x = index * step;
            float wave = (float) Math.sin(animationTime + index * 0.83f);
            float wave2 = (float) Math.sin(animationTime * 1.7f + index * 1.41f);
            int tongueHeight = Math.round(flameHeight * (0.52f + Math.abs(wave) * 0.36f + Math.max(0.0f, wave2) * 0.18f));
            int top = height - tongueHeight + config.yOffset;
            int sway = Math.round(wave2 * step * 0.28f);
            int flameWidth = Math.round(step * (1.25f + Math.abs(wave2) * 0.35f));
            context.fillGradient(x + sway, top + tongueHeight / 3, x + sway + flameWidth, height + 4, color, darkColor);
            context.fillGradient(x + sway + flameWidth / 5, top, x + sway + flameWidth * 4 / 5, baseY + flameHeight / 2, hotColor, 0x00000000);
            context.fillGradient(x + sway + flameWidth / 3, top + tongueHeight / 5, x + sway + flameWidth, height + 4, softColor, 0x00000000);
        }
    }

    private static int withAlpha(int color, float opacity) {
        int alpha = Math.max(0, Math.min(255, Math.round(255.0f * opacity)));
        return (alpha << 24) | (color & 0x00FFFFFF);
    }
}
