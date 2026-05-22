package ru.suppelemen.vibevisuals.feature.hud;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;
import ru.suppelemen.vibevisuals.mixin.HungerManagerAccessor;

public final class SaturationDisplayRenderer {
    private SaturationDisplayRenderer() {
    }

    public static void render(DrawContext context, PlayerEntity player, int top, int right) {
        VibeVisualsConfig.SaturationDisplayConfig config = VibeVisualsConfigManager.get().saturationDisplay;
        if (!config.enabled || player.isCreative() || player.isSpectator()) {
            return;
        }

        float saturation = Math.max(0.0f, Math.min(20.0f, player.getHungerManager().getSaturationLevel()));
        float exhaustion = Math.max(0.0f, Math.min(4.0f, ((HungerManagerAccessor) player.getHungerManager()).vibevisuals$getExhaustion()));
        int alpha = Math.round(255.0f * config.opacity) << 24;
        int color = alpha | (config.color & 0x00FFFFFF);
        int backgroundColor = ((int) (0x66 * config.opacity) << 24) | (config.backgroundColor & 0x00FFFFFF);
        int exhaustionColor = alpha | (config.exhaustionColor & 0x00FFFFFF);

        for (int index = 0; index < 10; index++) {
            int iconX = right - index * 8 - 9;
            int x = iconX + config.xOffset + Math.max(0, (8 - config.segmentWidth) / 2);
            int y = top + config.yOffset;
            context.fill(x, y, x + config.segmentWidth, y + config.segmentHeight, backgroundColor);

            float segmentValue = Math.max(0.0f, Math.min(2.0f, saturation - index * 2.0f));
            int filled = Math.round(config.segmentWidth * (segmentValue / 2.0f));
            if (filled > 0) {
                context.fill(x, y, x + filled, y + config.segmentHeight, color);
            }
        }

        if (config.showExhaustion && exhaustion > 0.0f) {
            int totalWidth = 10 * config.segmentWidth + 9 * config.segmentGap;
            int startX = right - 9 * 8 - 9 + config.xOffset + Math.max(0, (8 - config.segmentWidth) / 2);
            int y = top + config.yOffset + config.segmentHeight + 1;
            int filled = Math.round(totalWidth * (exhaustion / 4.0f));
            context.fill(startX, y, startX + totalWidth, y + 1, backgroundColor);
            context.fill(startX, y, startX + filled, y + 1, exhaustionColor);
        }
    }
}
