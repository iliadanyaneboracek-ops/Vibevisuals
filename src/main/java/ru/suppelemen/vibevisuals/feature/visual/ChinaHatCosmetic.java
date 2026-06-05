package ru.suppelemen.vibevisuals.feature.visual;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import ru.suppelemen.vibevisuals.VibeVisualsClient;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;

/**
 * China Hat — a conical "coolie / rice hat" cosmetic that sits on the player's
 * head, in any colour. Drawn as a world-oriented filled cone using the entity
 * translucent layer (a white texture tinted by the chosen colour) so it writes
 * depth correctly — i.e. clouds and other late passes can't paint over it, and
 * transparency is uniform from every angle. Client-side only.
 */
public final class ChinaHatCosmetic {

    private static final Identifier TEXTURE =
            Identifier.of(VibeVisualsClient.MOD_ID, "textures/entity/white.png");

    private ChinaHatCosmetic() {
    }

    public static void render(WorldRenderContext context) {
        VibeVisualsConfig.ChinaHatConfig config = VibeVisualsConfigManager.get().chinaHat;
        MinecraftClient client = MinecraftClient.getInstance();
        if (!config.enabled || client.player == null || client.world == null) {
            return;
        }
        VertexConsumerProvider consumers = context.consumers();
        if (consumers == null) {
            return;
        }

        float tickProgress = 1.0f;
        try {
            tickProgress = client.getRenderTickCounter().getTickProgress(true);
        } catch (Throwable ignored) {
        }

        Vec3d camera = client.gameRenderer.getCamera().getCameraPos();
        VertexConsumer buffer = consumers.getBuffer(RenderLayers.entityTranslucent(TEXTURE));

        boolean firstPerson = client.options.getPerspective().isFirstPerson();
        if (config.allPlayers) {
            for (PlayerEntity p : client.world.getPlayers()) {
                if (p == client.player && config.onlyThirdPerson && firstPerson) {
                    continue;
                }
                drawHat(buffer, context.matrices(), camera, p, tickProgress, config);
            }
        } else {
            if (config.onlyThirdPerson && firstPerson) {
                return;
            }
            drawHat(buffer, context.matrices(), camera, client.player, tickProgress, config);
        }
    }

    private static void drawHat(VertexConsumer buffer, MatrixStack matrices, Vec3d camera,
                                PlayerEntity player, float tickProgress,
                                VibeVisualsConfig.ChinaHatConfig config) {
        Vec3d pos = player.getLerpedPos(tickProgress);
        double headY = pos.y + player.getStandingEyeHeight() + 0.22 + config.yOffset;

        int color = config.color;
        int a = (color >>> 24) & 0xFF;
        if (a == 0) {
            a = 0xFF;
        }
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;

        matrices.push();
        matrices.translate(pos.x - camera.x, headY - camera.y, pos.z - camera.z);
        MatrixStack.Entry entry = matrices.peek();

        int seg = config.segments;
        float r = config.radius;
        float h = config.height;

        for (int i = 0; i < seg; i++) {
            double a0 = Math.PI * 2.0 * i / seg;
            double a1 = Math.PI * 2.0 * (i + 1) / seg;
            float x0 = (float) (Math.cos(a0) * r), z0 = (float) (Math.sin(a0) * r);
            float x1 = (float) (Math.cos(a1) * r), z1 = (float) (Math.sin(a1) * r);

            // Cone side as a collapsed quad (apex, base0, base1, base1). Drawn
            // both windings so it shows from above and below.
            quad(buffer, entry, 0f, h, 0f, x0, 0f, z0, x1, 0f, z1, x1, 0f, z1, red, green, blue, a);
            quad(buffer, entry, 0f, h, 0f, x1, 0f, z1, x0, 0f, z0, x0, 0f, z0, red, green, blue, a);
        }
        matrices.pop();
    }

    private static void quad(VertexConsumer b, MatrixStack.Entry entry,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             float x3, float y3, float z3, float x4, float y4, float z4,
                             int r, int g, int bl, int a) {
        vertex(b, entry, x1, y1, z1, 0f, 0f, r, g, bl, a);
        vertex(b, entry, x2, y2, z2, 1f, 0f, r, g, bl, a);
        vertex(b, entry, x3, y3, z3, 1f, 1f, r, g, bl, a);
        vertex(b, entry, x4, y4, z4, 0f, 1f, r, g, bl, a);
    }

    private static void vertex(VertexConsumer b, MatrixStack.Entry entry,
                               float x, float y, float z, float u, float v,
                               int r, int g, int bl, int a) {
        b.vertex(entry.getPositionMatrix(), x, y, z)
                .color(r, g, bl, a)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
                .normal(entry, 0.0f, 1.0f, 0.0f);
    }
}
