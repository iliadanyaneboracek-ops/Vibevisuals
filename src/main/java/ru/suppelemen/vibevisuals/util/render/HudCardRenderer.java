package ru.suppelemen.vibevisuals.util.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.util.Identifier;
import ru.suppelemen.vibevisuals.SupsVisualsClient;
import ru.suppelemen.vibevisuals.theme.HudCardRenderType;
import ru.suppelemen.vibevisuals.theme.HudVisualSettings;

public final class HudCardRenderer {
    private static final Identifier WHITE_TEXTURE = Identifier.of(SupsVisualsClient.MOD_ID, "textures/gui/white.png");
    private static final RenderPipeline HUD_CARD_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.POSITION_TEX_COLOR_SNIPPET)
                    .withLocation(Identifier.of(SupsVisualsClient.MOD_ID, "pipeline/hud_card"))
                    .withFragmentShader(Identifier.of(SupsVisualsClient.MOD_ID, "core/hud_card"))
                    .build()
    );

    private HudCardRenderer() {
    }

    public static void drawCard(DrawContext context, int x, int y, int width, int height, HudVisualSettings settings) {
        float radius = settings != null ? settings.radius : 6.0f;
        float opacity = settings != null ? settings.opacity : 0.90f;
        HudCardRenderType renderType = settings != null ? settings.renderType : HudCardRenderType.SIMPLE_JAVA;

        if (renderType == HudCardRenderType.LIQUID_GLASS || renderType == HudCardRenderType.SHADER_ANIMATED) {
            drawShaderCard(context, x, y, width, height, opacity);
            return;
        }

        drawRoundedRectNoOverlap(context, x, y, width, height, Math.round(radius), colorWithOpacity(0x05060D, opacity));
    }

    private static void drawShaderCard(DrawContext context, int x, int y, int width, int height, float opacity) {
        context.drawTexture(
                HUD_CARD_PIPELINE,
                WHITE_TEXTURE,
                x,
                y,
                0.0f,
                0.0f,
                width,
                height,
                1,
                1,
                1,
                1,
                colorWithOpacity(0xFFFFFF, opacity)
        );
    }

    private static void drawRoundedRectNoOverlap(
            DrawContext context,
            int x,
            int y,
            int width,
            int height,
            int radius,
            int color
    ) {
        if (width <= 0 || height <= 0) {
            return;
        }

        radius = Math.max(0, Math.min(radius, Math.min(width, height) / 2));

        if (radius <= 1) {
            context.fill(x, y, x + width, y + height, color);
            return;
        }

        // Draw each row once so alpha does not stack in the center.
        for (int row = 0; row < height; row++) {
            int inset = 0;

            if (row < radius) {
                inset = cornerInset(row, radius);
            } else if (row >= height - radius) {
                inset = cornerInset(height - row - 1, radius);
            }

            context.fill(
                    x + inset,
                    y + row,
                    x + width - inset,
                    y + row + 1,
                    color
            );
        }
    }

    private static int cornerInset(int row, int radius) {
        double dy = radius - row - 0.5;
        double r = radius;
        double dx = r - Math.sqrt(Math.max(0.0, r * r - dy * dy));
        return Math.max(0, (int) Math.ceil(dx));
    }

    private static int colorWithOpacity(int rgb, float opacity) {
        int alpha = Math.max(0, Math.min(255, Math.round(opacity * 255.0f)));
        return (alpha << 24) | (rgb & 0x00FFFFFF);
    }
}
