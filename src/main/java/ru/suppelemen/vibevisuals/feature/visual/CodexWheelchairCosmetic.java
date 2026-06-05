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
import org.joml.Matrix4f;
import ru.suppelemen.vibevisuals.VibeVisualsClient;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;

public final class CodexWheelchairCosmetic {
    private static final Identifier TEXTURE =
            Identifier.of(VibeVisualsClient.MOD_ID, "textures/entity/codex_wheelchair.png");

    private CodexWheelchairCosmetic() {
    }

    public static void render(WorldRenderContext context) {
        VibeVisualsConfig.CodexWheelchairConfig config = VibeVisualsConfigManager.get().codexWheelchair;
        MinecraftClient client = MinecraftClient.getInstance();
        if (!config.enabled || client.player == null || client.world == null) {
            return;
        }
        if (config.onlyThirdPerson && client.options.getPerspective().isFirstPerson()) {
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

        PlayerEntity player = client.player;
        Vec3d camera = client.gameRenderer.getCamera().getCameraPos();
        Vec3d pos = player.getLerpedPos(tickProgress).add(0.0, 0.88 + config.yOffset, 0.0);

        MatrixStack matrices = context.matrices();
        matrices.push();
        matrices.translate(pos.x - camera.x, pos.y - camera.y, pos.z - camera.z);
        matrices.multiply(client.gameRenderer.getCamera().getRotation());

        float height = 1.85f * config.scale;
        float width = 1.85f * config.scale;
        MatrixStack.Entry entry = matrices.peek();
        Matrix4f matrix = entry.getPositionMatrix();
        VertexConsumer consumer = consumers.getBuffer(RenderLayers.entityTranslucent(TEXTURE));
        drawQuad(consumer, entry, matrix, width, height);
        matrices.pop();
    }

    private static void drawQuad(VertexConsumer consumer, MatrixStack.Entry entry, Matrix4f matrix, float width, float height) {
        float hw = width * 0.5f;
        float hh = height * 0.5f;
        int light = LightmapTextureManager.MAX_LIGHT_COORDINATE;
        int overlay = OverlayTexture.DEFAULT_UV;

        vertex(consumer, entry, matrix, -hw, -hh, 0.0f, 1.0f, light, overlay);
        vertex(consumer, entry, matrix, hw, -hh, 1.0f, 1.0f, light, overlay);
        vertex(consumer, entry, matrix, hw, hh, 1.0f, 0.0f, light, overlay);
        vertex(consumer, entry, matrix, -hw, hh, 0.0f, 0.0f, light, overlay);
    }

    private static void vertex(VertexConsumer consumer, MatrixStack.Entry entry, Matrix4f matrix,
                               float x, float y, float u, float v, int light, int overlay) {
        consumer.vertex(matrix, x, y, 0.0f)
                .color(255, 255, 255, 255)
                .texture(u, v)
                .overlay(overlay)
                .light(light)
                .normal(entry, 0.0f, 1.0f, 0.0f);
    }
}
