package ru.suppelemen.vibevisuals.util.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.util.Identifier;
import ru.suppelemen.vibevisuals.VibeVisualsClient;
import ru.suppelemen.vibevisuals.theme.HudCardRenderType;
import ru.suppelemen.vibevisuals.theme.HudVisualSettings;

public final class HudCardRenderer {
    private static final Identifier WHITE_TEXTURE = Identifier.of(VibeVisualsClient.MOD_ID, "textures/gui/white.png");
    private static final Identifier ROUNDED_CARD_TEXTURE = Identifier.of(VibeVisualsClient.MOD_ID, "textures/gui/rounded_card.png");
    private static final int ROUNDED_CARD_TEXTURE_SIZE = 64;
    private static final int ROUNDED_CARD_CORNER_SIZE = 16;
    private static final RenderPipeline HUD_CARD_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.POSITION_TEX_COLOR_SNIPPET)
                    .withLocation(Identifier.of(VibeVisualsClient.MOD_ID, "pipeline/hud_card"))
                    .withFragmentShader(Identifier.of(VibeVisualsClient.MOD_ID, "core/hud_card"))
                    .build()
    );
    private static final RenderPipeline HUD_OUTLINE_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.POSITION_TEX_COLOR_SNIPPET)
                    .withLocation(Identifier.of(VibeVisualsClient.MOD_ID, "pipeline/hud_outline"))
                    .withFragmentShader(Identifier.of(VibeVisualsClient.MOD_ID, "core/hud_outline"))
                    .build()
    );

    private HudCardRenderer() {
    }

    public static void drawCard(DrawContext context, int x, int y, int width, int height, HudVisualSettings settings) {
        float radius = settings != null ? settings.radius : 6.0f;
        float opacity = settings != null ? settings.opacity : 0.90f;
        HudCardRenderType renderType = settings != null ? settings.renderType : HudCardRenderType.SIMPLE_JAVA;

        if (renderType == HudCardRenderType.LIQUID_GLASS || renderType == HudCardRenderType.SHADER_ANIMATED) {
            drawShaderCard(context, x, y, width, height, radius, opacity);
            return;
        }

        drawRoundedRectNoOverlap(context, x, y, width, height, Math.round(radius), colorWithOpacity(0x05060D, opacity));
    }

    public static void drawOverlayCard(
            DrawContext context,
            int x,
            int y,
            int width,
            int height,
            float radius,
            int color,
            float opacity
    ) {
        drawNineSliceCard(context, x, y, width, height, Math.round(radius), color, opacity);
    }

    public static void drawShaderOutline(
            DrawContext context,
            int x,
            int y,
            int width,
            int height,
            float radius,
            float thickness,
            float opacity
    ) {
        context.drawTexture(
                HUD_OUTLINE_PIPELINE,
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
                colorWithRadiusThicknessOpacity(radius, width, height, thickness, opacity)
        );
    }

    private static void drawShaderCard(
            DrawContext context,
            int x,
            int y,
            int width,
            int height,
            float radius,
            float opacity
    ) {
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
                colorWithRadiusOpacity(radius, width, height, opacity)
        );
    }

    private static void drawNineSliceCard(
            DrawContext context,
            int x,
            int y,
            int width,
            int height,
            int radius,
            float opacity
    ) {
        if (width <= 0 || height <= 0) {
            return;
        }

        int corner = Math.max(1, Math.min(radius, Math.min(width, height) / 2));
        drawNineSliceCard(context, x, y, width, height, radius, 0x11151F, opacity);
    }

    private static void drawNineSliceCard(
            DrawContext context,
            int x,
            int y,
            int width,
            int height,
            int radius,
            int rgb,
            float opacity
    ) {
        if (width <= 0 || height <= 0) {
            return;
        }

        int corner = Math.max(1, Math.min(radius, Math.min(width, height) / 2));
        int color = colorWithOpacity(rgb, opacity);
        int srcCorner = ROUNDED_CARD_CORNER_SIZE;
        int srcCenter = ROUNDED_CARD_TEXTURE_SIZE - srcCorner * 2;
        int innerWidth = Math.max(0, width - corner * 2);
        int innerHeight = Math.max(0, height - corner * 2);

        drawCardPatch(context, x, y, corner, corner, 0, 0, srcCorner, srcCorner, color);
        drawCardPatch(context, x + corner + innerWidth, y, corner, corner, srcCorner + srcCenter, 0, srcCorner, srcCorner, color);
        drawCardPatch(context, x, y + corner + innerHeight, corner, corner, 0, srcCorner + srcCenter, srcCorner, srcCorner, color);
        drawCardPatch(context, x + corner + innerWidth, y + corner + innerHeight, corner, corner, srcCorner + srcCenter, srcCorner + srcCenter, srcCorner, srcCorner, color);

        if (innerWidth > 0) {
            drawCardPatch(context, x + corner, y, innerWidth, corner, srcCorner, 0, srcCenter, srcCorner, color);
            drawCardPatch(context, x + corner, y + corner + innerHeight, innerWidth, corner, srcCorner, srcCorner + srcCenter, srcCenter, srcCorner, color);
        }

        if (innerHeight > 0) {
            drawCardPatch(context, x, y + corner, corner, innerHeight, 0, srcCorner, srcCorner, srcCenter, color);
            drawCardPatch(context, x + corner + innerWidth, y + corner, corner, innerHeight, srcCorner + srcCenter, srcCorner, srcCorner, srcCenter, color);
        }

        if (innerWidth > 0 && innerHeight > 0) {
            drawCardPatch(context, x + corner, y + corner, innerWidth, innerHeight, srcCorner, srcCorner, srcCenter, srcCenter, color);
        }
    }

    private static void drawCardPatch(
            DrawContext context,
            int x,
            int y,
            int width,
            int height,
            int u,
            int v,
            int sourceWidth,
            int sourceHeight,
            int color
    ) {
        context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                ROUNDED_CARD_TEXTURE,
                x,
                y,
                u,
                v,
                width,
                height,
                sourceWidth,
                sourceHeight,
                ROUNDED_CARD_TEXTURE_SIZE,
                ROUNDED_CARD_TEXTURE_SIZE,
                color
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

    private static int colorWithRadiusOpacity(float radius, int width, int height, float opacity) {
        float normalizedRadius = height <= 0 ? 0.0f : radius / (height * 0.5f);
        float aspect = height <= 0 ? 1.0f : width / (float) height;
        int encodedRadius = Math.max(0, Math.min(255, Math.round(normalizedRadius * 255.0f)));
        int encodedAspect = Math.max(0, Math.min(255, Math.round((aspect / 8.0f) * 255.0f)));
        int alpha = Math.max(0, Math.min(255, Math.round(opacity * 255.0f)));
        return (alpha << 24) | (encodedRadius << 16) | (encodedAspect << 8) | 0xFF;
    }

    private static int colorWithRadiusThicknessOpacity(float radius, int width, int height, float thickness, float opacity) {
        float normalizedRadius = height <= 0 ? 0.0f : radius / (height * 0.5f);
        float aspect = height <= 0 ? 1.0f : width / (float) height;
        float normalizedThickness = height <= 0 ? 0.02f : thickness / height;
        int encodedRadius = Math.max(0, Math.min(255, Math.round(normalizedRadius * 255.0f)));
        int encodedAspect = Math.max(0, Math.min(255, Math.round((aspect / 8.0f) * 255.0f)));
        int encodedThickness = Math.max(0, Math.min(255, Math.round(normalizedThickness * 255.0f)));
        int alpha = Math.max(0, Math.min(255, Math.round(opacity * 255.0f)));
        return (alpha << 24) | (encodedRadius << 16) | (encodedAspect << 8) | encodedThickness;
    }

}
