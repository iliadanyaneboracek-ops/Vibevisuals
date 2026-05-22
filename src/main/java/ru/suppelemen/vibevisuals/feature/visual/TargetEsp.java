package ru.suppelemen.vibevisuals.feature.visual;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;

import java.util.Locale;

public final class TargetEsp {
    private static PlayerEntity target;
    private static PlayerEntity glowingTarget;
    private static boolean glowingTargetWasGlowing;
    private static int holdTicks;
    private static long ticks;

    private TargetEsp() {
    }

    public static void tick(MinecraftClient client) {
        VibeVisualsConfig.TargetEspConfig config = VibeVisualsConfigManager.get().targetEsp;
        ticks++;
        if (!config.enabled || client.player == null || client.world == null) {
            clearGlow();
            target = null;
            holdTicks = 0;
            return;
        }

        PlayerEntity aimed = aimedPlayer(client);
        Mode mode = Mode.from(config.mode);
        if (aimed != null) {
            target = aimed;
            holdTicks = config.targetHoldTicks;
            updateGlow(mode == Mode.GLOW ? aimed : null);
            return;
        }

        updateGlow(null);
        if (holdTicks > 0 && target != null && target.isAlive() && !target.isRemoved()) {
            holdTicks--;
            return;
        }

        target = null;
    }

    public static void render(WorldRenderContext context) {
        VibeVisualsConfig.TargetEspConfig config = VibeVisualsConfigManager.get().targetEsp;
        if (!config.enabled || target == null || !target.isAlive() || target.isRemoved()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        Vec3d camera = client.gameRenderer.getCamera().getCameraPos();
        MatrixStack.Entry entry = context.matrices().peek();
        Matrix4f matrix = entry.getPositionMatrix();
        VertexConsumer consumer = context.consumers().getBuffer(RenderLayers.linesTranslucent());
        Vec3d base = target.getLerpedPos(1.0f);
        float yaw = ticks * config.spinSpeed;
        Mode mode = Mode.from(config.mode);
        if (mode == Mode.GLOW) {
            return;
        }

        if (mode == Mode.RING || mode == Mode.COMBO) {
            drawRing(consumer, entry, matrix, camera, base.add(0.0, config.heightOffset, 0.0), config.radius, yaw, config.color, 220, config);
        }
        if (mode == Mode.STAR || mode == Mode.COMBO) {
            drawStar(consumer, entry, matrix, camera, base.add(0.0, config.heightOffset + 0.02, 0.0), config.radius * 0.95f, yaw, config.secondaryColor, 230, config);
        }
        if (mode == Mode.ORBIT_PARTICLES || mode == Mode.COMBO) {
            drawOrbitParticles(consumer, entry, matrix, camera, base, yaw, config);
        }
        if (mode == Mode.SPIRAL || mode == Mode.COMBO) {
            drawSpiral(consumer, entry, matrix, camera, base, yaw, config);
        }
    }

    private static PlayerEntity aimedPlayer(MinecraftClient client) {
        HitResult hit = client.crosshairTarget;
        if (!(hit instanceof EntityHitResult entityHit)) {
            return null;
        }

        Entity entity = entityHit.getEntity();
        if (entity instanceof PlayerEntity player && player != client.player && player.isAlive() && !player.isSpectator()) {
            return player;
        }
        return null;
    }

    private static void updateGlow(PlayerEntity nextTarget) {
        if (glowingTarget == nextTarget) {
            return;
        }

        clearGlow();
        if (nextTarget != null) {
            glowingTarget = nextTarget;
            glowingTargetWasGlowing = nextTarget.isGlowing();
            nextTarget.setGlowing(true);
        }
    }

    private static void clearGlow() {
        if (glowingTarget != null && !glowingTargetWasGlowing) {
            glowingTarget.setGlowing(false);
        }
        glowingTarget = null;
        glowingTargetWasGlowing = false;
    }

    private static void drawRing(VertexConsumer consumer, MatrixStack.Entry entry, Matrix4f matrix, Vec3d camera, Vec3d center, float radius, float spin, int color, int alpha, VibeVisualsConfig.TargetEspConfig config) {
        Vec3d previous = null;
        for (int index = 0; index <= config.segments; index++) {
            double angle = spin + Math.PI * 2.0 * index / config.segments;
            Vec3d next = center.add(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius);
            if (previous != null) {
                drawLine(consumer, entry, matrix, camera, previous, next, color, alpha, config.lineWidth);
            }
            previous = next;
        }
    }

    private static void drawStar(VertexConsumer consumer, MatrixStack.Entry entry, Matrix4f matrix, Vec3d camera, Vec3d center, float radius, float spin, int color, int alpha, VibeVisualsConfig.TargetEspConfig config) {
        int points = 5;
        Vec3d[] vertices = new Vec3d[points * 2];
        for (int i = 0; i < vertices.length; i++) {
            double angle = spin + Math.PI / 2.0 + Math.PI * i / points;
            double r = i % 2 == 0 ? radius : radius * 0.42;
            vertices[i] = center.add(Math.cos(angle) * r, 0.0, Math.sin(angle) * r);
        }
        for (int i = 0; i < vertices.length; i++) {
            drawLine(consumer, entry, matrix, camera, vertices[i], vertices[(i + 1) % vertices.length], color, alpha, config.lineWidth);
        }
    }

    private static void drawOrbitParticles(VertexConsumer consumer, MatrixStack.Entry entry, Matrix4f matrix, Vec3d camera, Vec3d base, float spin, VibeVisualsConfig.TargetEspConfig config) {
        for (int i = 0; i < config.particles; i++) {
            double angle = spin * 1.6 + Math.PI * 2.0 * i / config.particles;
            double bob = 0.55 + 0.35 * Math.sin(spin * 2.0 + i);
            Vec3d center = base.add(Math.cos(angle) * config.radius, bob, Math.sin(angle) * config.radius);
            drawCross(consumer, entry, matrix, camera, center, config.particleSize, i % 2 == 0 ? config.color : config.secondaryColor, 210, config.lineWidth);
        }
    }

    private static void drawSpiral(VertexConsumer consumer, MatrixStack.Entry entry, Matrix4f matrix, Vec3d camera, Vec3d base, float spin, VibeVisualsConfig.TargetEspConfig config) {
        Vec3d previous = null;
        int spiralSegments = Math.max(16, config.segments);
        for (int i = 0; i <= spiralSegments; i++) {
            float t = i / (float) spiralSegments;
            double angle = spin * 1.8 + t * Math.PI * 4.0;
            Vec3d next = base.add(Math.cos(angle) * config.radius, config.heightOffset + t * Math.max(1.2, target.getHeight()), Math.sin(angle) * config.radius);
            if (previous != null) {
                int alpha = Math.max(60, Math.min(220, Math.round(220.0f * (1.0f - t * 0.35f))));
                drawLine(consumer, entry, matrix, camera, previous, next, config.color, alpha, config.lineWidth);
            }
            previous = next;
        }
    }

    private static void drawCross(VertexConsumer consumer, MatrixStack.Entry entry, Matrix4f matrix, Vec3d camera, Vec3d center, float size, int color, int alpha, float width) {
        drawLine(consumer, entry, matrix, camera, center.add(-size, 0.0, 0.0), center.add(size, 0.0, 0.0), color, alpha, width);
        drawLine(consumer, entry, matrix, camera, center.add(0.0, -size, 0.0), center.add(0.0, size, 0.0), color, alpha, width);
        drawLine(consumer, entry, matrix, camera, center.add(0.0, 0.0, -size), center.add(0.0, 0.0, size), color, alpha, width);
    }

    private static void drawLine(VertexConsumer consumer, MatrixStack.Entry entry, Matrix4f matrix, Vec3d camera, Vec3d start, Vec3d end, int color, int alpha, float width) {
        float x1 = (float) (start.x - camera.x);
        float y1 = (float) (start.y - camera.y);
        float z1 = (float) (start.z - camera.z);
        float x2 = (float) (end.x - camera.x);
        float y2 = (float) (end.y - camera.y);
        float z2 = (float) (end.z - camera.z);
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;
        consumer.vertex(matrix, x1, y1, z1).color(red, green, blue, alpha).normal(entry, 0.0f, 1.0f, 0.0f).lineWidth(width);
        consumer.vertex(matrix, x2, y2, z2).color(red, green, blue, alpha).normal(entry, 0.0f, 1.0f, 0.0f).lineWidth(width);
    }

    private enum Mode {
        RING,
        STAR,
        ORBIT_PARTICLES,
        SPIRAL,
        GLOW,
        COMBO;

        static Mode from(String value) {
            try {
                return Mode.valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException | NullPointerException ignored) {
                return COMBO;
            }
        }
    }
}
