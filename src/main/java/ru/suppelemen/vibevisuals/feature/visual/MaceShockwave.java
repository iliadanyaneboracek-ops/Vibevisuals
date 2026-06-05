package ru.suppelemen.vibevisuals.feature.visual;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Mace Shockwave — when the player lands a mace SMASH attack (hits an entity
 * while falling), a ring of energy ripples outward from the impact point and
 * fades. Purely client-side / cosmetic: concentric expanding line rings drawn
 * in world space, plus an optional dust burst on impact.
 */
public final class MaceShockwave {
    /** A single in-flight ripple. */
    private static final class Wave {
        final Vec3d center;
        final long startMs;
        final long lifeMs;
        final float maxRadius;
        final int rings;
        final int ringColor;
        final int coreColor;
        final float lineWidth;
        final float thickness;
        final float peakHeight;
        final int segments;
        final double heightOffset;

        Wave(Vec3d center, float damage, VibeVisualsConfig.MaceShockwaveConfig c) {
            this.center = center;
            this.startMs = System.currentTimeMillis();
            this.lifeMs = Math.max(1L, (long) c.lifeTicks * 50L);
            // Damage scaling: the harder the smash hit, the bigger / taller /
            // thicker the wave. Clamped so a soft hit still reads and a huge
            // crit stays sane. Disabled → factor 1.0 (base values).
            float scale = 1.0f;
            if (c.scaleWithDamage) {
                float ref = Math.max(1.0f, c.referenceDamage);
                scale = Math.max(0.3f, Math.min(2.5f, damage / ref));
            }
            float size = c.size * scale;
            this.maxRadius = c.maxRadius * size;
            this.thickness = c.thickness * size;
            this.peakHeight = c.waveHeight * size;
            this.rings = c.rings;
            this.ringColor = c.ringColor;
            this.coreColor = c.coreColor;
            this.lineWidth = c.lineWidth;
            this.segments = c.segments;
            this.heightOffset = c.heightOffset;
        }
    }

    private static final List<Wave> WAVES = new ArrayList<>();
    private static final int MAX_WAVES = 16;

    private MaceShockwave() {
    }

    /** Fire a shockwave at {@code center} (impact point, ground level). */
    public static void spawn(Vec3d center, float damage) {
        VibeVisualsConfig.MaceShockwaveConfig c = VibeVisualsConfigManager.get().maceShockwave;
        if (!c.enabled || center == null) {
            return;
        }
        synchronized (WAVES) {
            if (WAVES.size() >= MAX_WAVES) {
                WAVES.remove(0);
            }
            WAVES.add(new Wave(center, damage, c));
        }
        spawnParticles(center, c);
    }

    public static void render(WorldRenderContext context) {
        VibeVisualsConfig.MaceShockwaveConfig c = VibeVisualsConfigManager.get().maceShockwave;
        if (!c.enabled) {
            synchronized (WAVES) {
                WAVES.clear();
            }
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        Vec3d camera = client.gameRenderer.getCamera().getCameraPos();
        MatrixStack.Entry entry = context.matrices().peek();
        Matrix4f matrix = entry.getPositionMatrix();
        long now = System.currentTimeMillis();

        // The world VertexConsumerProvider is an Immediate that shares one
        // fallback buffer between non-fixed layers (lines + debug quads). Only
        // ONE such buffer can be "building" at a time, so we must finish all
        // line work before switching to the quad buffer — never interleave.
        synchronized (WAVES) {
            // Pass 1: ground ripple rings (lines). Expire finished waves here.
            VertexConsumer lines = context.consumers().getBuffer(RenderLayers.linesTranslucent());
            Iterator<Wave> it = WAVES.iterator();
            while (it.hasNext()) {
                Wave wave = it.next();
                float t = (now - wave.startMs) / (float) wave.lifeMs;
                if (t >= 1.0f) {
                    it.remove();
                    continue;
                }
                drawGround(lines, entry, matrix, camera, wave, t);
            }

            // Pass 2: continuous vertical walls (filled quads).
            VertexConsumer quads = context.consumers().getBuffer(RenderLayers.debugQuads());
            for (Wave wave : WAVES) {
                float t = (now - wave.startMs) / (float) wave.lifeMs;
                drawWalls(quads, entry, matrix, camera, wave, t);
            }
        }
    }

    /** Shared per-ring iteration. Returns radius/alpha/color via the callback. */
    private interface RingSink {
        void ring(float radius, int alpha, int color);
    }

    private static void forEachRing(Wave wave, float t, RingSink sink) {
        float ease = 1.0f - (1.0f - t) * (1.0f - t);
        int baseAlpha = Math.round(230f * (1.0f - t));
        if (baseAlpha <= 0) {
            return;
        }
        for (int r = 0; r < wave.rings; r++) {
            float stagger = wave.rings <= 1 ? 0f : (r / (float) wave.rings) * 0.45f;
            float rt = ease - stagger;
            if (rt <= 0f) {
                continue;
            }
            float radius = wave.maxRadius * Math.min(1.0f, rt);
            int alpha = Math.round(baseAlpha * (1.0f - stagger));
            if (alpha <= 0) {
                continue;
            }
            int color = (r % 2 == 0) ? wave.ringColor : wave.coreColor;
            sink.ring(radius, alpha, color);
        }
    }

    private static void drawGround(VertexConsumer lines, MatrixStack.Entry entry, Matrix4f matrix,
                                   Vec3d camera, Wave wave, float t) {
        Vec3d center = wave.center.add(0.0, wave.heightOffset, 0.0);
        forEachRing(wave, t, (radius, alpha, color) ->
                drawRingBand(lines, entry, matrix, camera, center, radius, color, alpha, wave));
    }

    private static void drawWalls(VertexConsumer quads, MatrixStack.Entry entry, Matrix4f matrix,
                                  Vec3d camera, Wave wave, float t) {
        Vec3d center = wave.center.add(0.0, wave.heightOffset, 0.0);
        // Wall height: tall on impact, collapses to the ground as the wave ages.
        float currentHeight = wave.peakHeight * (1.0f - t);
        forEachRing(wave, t, (radius, alpha, color) ->
                drawWall(quads, entry, matrix, camera, center, radius, currentHeight, color, alpha, wave));
    }

    /**
     * The vertical wall: a continuous filled cylinder surface — a tall ring,
     * not separate posts. Built from quads, one per segment, faded toward the
     * top for a soft crest. Double-sided so it shows from inside and out.
     */
    private static void drawWall(VertexConsumer quads, MatrixStack.Entry entry, Matrix4f matrix,
                                 Vec3d camera, Vec3d center, float radius, float height,
                                 int color, int alpha, Wave wave) {
        if (height <= 0.01f || alpha <= 0) {
            return;
        }
        int topAlpha = Math.round(alpha * 0.10f);
        int seg = wave.segments;
        for (int i = 0; i < seg; i++) {
            double a0 = Math.PI * 2.0 * i / seg;
            double a1 = Math.PI * 2.0 * (i + 1) / seg;
            Vec3d b0 = center.add(Math.cos(a0) * radius, 0.0, Math.sin(a0) * radius);
            Vec3d b1 = center.add(Math.cos(a1) * radius, 0.0, Math.sin(a1) * radius);
            Vec3d t0 = b0.add(0.0, height, 0.0);
            Vec3d t1 = b1.add(0.0, height, 0.0);
            // Outer face.
            quad(quads, entry, matrix, camera, b0, b1, t1, t0, color, alpha, alpha, topAlpha, topAlpha);
            // Inner face (reversed winding) so it's visible from both sides.
            quad(quads, entry, matrix, camera, t0, t1, b1, b0, color, topAlpha, topAlpha, alpha, alpha);
        }
    }

    private static void quad(VertexConsumer quads, MatrixStack.Entry entry, Matrix4f matrix, Vec3d camera,
                             Vec3d v1, Vec3d v2, Vec3d v3, Vec3d v4,
                             int color, int a1, int a2, int a3, int a4) {
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;
        vertex(quads, matrix, camera, v1, red, green, blue, a1);
        vertex(quads, matrix, camera, v2, red, green, blue, a2);
        vertex(quads, matrix, camera, v3, red, green, blue, a3);
        vertex(quads, matrix, camera, v4, red, green, blue, a4);
    }

    private static void vertex(VertexConsumer quads, Matrix4f matrix, Vec3d camera, Vec3d p,
                               int red, int green, int blue, int alpha) {
        quads.vertex(matrix, (float) (p.x - camera.x), (float) (p.y - camera.y), (float) (p.z - camera.z))
                .color(red, green, blue, alpha);
    }

    private static void drawRingBand(VertexConsumer consumer, MatrixStack.Entry entry, Matrix4f matrix,
                                     Vec3d camera, Vec3d center, float radius, int color, int alpha,
                                     Wave wave) {
        // Draw the ring as a BAND of stacked concentric sub-rings spanning the
        // configured thickness. GL line width is capped at ~1px on most GPUs,
        // so stacking lines across [r-t/2, r+t/2] is what actually makes the
        // ring visibly thick. Sub-ring count scales with thickness (~0.04b each).
        float t = wave.thickness;
        int subRings = Math.max(1, Math.round(t / 0.04f));
        for (int s = 0; s < subRings; s++) {
            float frac = subRings == 1 ? 0.5f : s / (float) (subRings - 1);
            float subRadius = radius + (frac - 0.5f) * t;
            if (subRadius <= 0f) {
                continue;
            }
            drawRingLoop(consumer, entry, matrix, camera, center, subRadius, color, alpha, wave);
        }
    }

    private static void drawRingLoop(VertexConsumer consumer, MatrixStack.Entry entry, Matrix4f matrix,
                                     Vec3d camera, Vec3d center, float radius, int color, int alpha,
                                     Wave wave) {
        Vec3d previous = null;
        for (int index = 0; index <= wave.segments; index++) {
            double angle = Math.PI * 2.0 * index / wave.segments;
            Vec3d next = center.add(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius);
            if (previous != null) {
                drawLine(consumer, entry, matrix, camera, previous, next, color, alpha, wave.lineWidth);
            }
            previous = next;
        }
    }

    private static void drawLine(VertexConsumer consumer, MatrixStack.Entry entry, Matrix4f matrix,
                                 Vec3d camera, Vec3d start, Vec3d end, int color, int alpha, float width) {
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

    private static void spawnParticles(Vec3d center, VibeVisualsConfig.MaceShockwaveConfig c) {
        if (!c.spawnParticles || c.particleCount <= 0) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return;
        }
        DustParticleEffect dust = new DustParticleEffect(c.coreColor & 0x00FFFFFF, 1.4f);
        for (int i = 0; i < c.particleCount; i++) {
            double angle = Math.PI * 2.0 * i / c.particleCount;
            double speed = 0.18;
            double vx = Math.cos(angle) * speed;
            double vz = Math.sin(angle) * speed;
            client.world.addParticleClient(dust,
                    center.x, center.y + 0.1, center.z,
                    vx, 0.02, vz);
        }
    }
}
