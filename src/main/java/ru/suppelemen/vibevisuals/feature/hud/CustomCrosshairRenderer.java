package ru.suppelemen.vibevisuals.feature.hud;

import net.minecraft.client.gui.DrawContext;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;

public final class CustomCrosshairRenderer {
    private CustomCrosshairRenderer() {
    }

    public static void render(DrawContext context) {
        VibeVisualsConfig.CustomCrosshairConfig config = VibeVisualsConfigManager.get().customCrosshair;
        if (!config.enabled) {
            return;
        }

        int centerX = context.getScaledWindowWidth() / 2 + config.xOffset;
        int centerY = context.getScaledWindowHeight() / 2 + config.yOffset;
        double angle = Math.toRadians(config.angle);
        drawArm(context, centerX, centerY, angle, config.gap, config.gap + config.length, config.width, config.thickness, config.color);
        drawArm(context, centerX, centerY, angle + Math.PI, config.gap, config.gap + config.length, config.width, config.thickness, config.color);
        drawArm(context, centerX, centerY, angle + Math.PI / 2.0, config.gap, config.gap + config.length, config.width, config.thickness, config.color);
        drawArm(context, centerX, centerY, angle - Math.PI / 2.0, config.gap, config.gap + config.length, config.width, config.thickness, config.color);
        if (config.dot) {
            int r = Math.max(1, config.dotSize / 2);
            context.fill(centerX - r, centerY - r, centerX + r + 1, centerY + r + 1, config.color);
        }
    }

    private static void drawArm(DrawContext context, int cx, int cy, double angle, int from, int to, int width, int thickness, int color) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double sideX = -sin;
        double sideY = cos;
        int steps = Math.max(1, to - from);
        int halfThickness = Math.max(0, thickness / 2);
        int halfWidth = Math.max(0, width / 2);
        for (int i = 0; i <= steps; i++) {
            int distance = from + i;
            for (int side = -halfWidth; side <= halfWidth; side++) {
                int x = (int) Math.round(cx + cos * distance + sideX * side);
                int y = (int) Math.round(cy + sin * distance + sideY * side);
                context.fill(x - halfThickness, y - halfThickness, x + halfThickness + 1, y + halfThickness + 1, color);
            }
        }
    }
}
