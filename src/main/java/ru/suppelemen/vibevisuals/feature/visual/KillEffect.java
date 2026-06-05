package ru.suppelemen.vibevisuals.feature.visual;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Kill Effect — a burst played at the spot where a recently-attacked entity
 * died. Several styles (RING / BURST / FLASH / LIGHTNING). Purely cosmetic and
 * packet-independent; driven by {@link CombatVisualsTracker}.
 */
public final class KillEffect {

    private enum Style { RING, BURST, FLASH, LIGHTNING, SOUL, BURN;
        static Style from(String s) {
            try {
                return valueOf(s.trim().toUpperCase(Locale.ROOT));
            } catch (Exception ignored) {
                return BURST;
            }
        }
    }

    private static final class Effect {
        final Vec3d center;
        final long startMs;
        final long lifeMs;
        final Style style;
        final int color;
        final int accent;
        final float maxRadius;
        final int rings;
        final float lineWidth;
        final int segments;
        final double heightOffset;
        final float riseHeight;
        final long seed;

        Effect(Vec3d center, VibeVisualsConfig.KillEffectConfig c) {
            this.center = center;
            this.startMs = System.currentTimeMillis();
            this.style = Style.from(c.style);
            // The soul lingers and ascends, so it gets a longer lifetime.
            long base = Math.max(1L, (long) c.lifeTicks * 50L);
            this.lifeMs = switch (this.style) {
                case SOUL -> base * 4L;
                case BURN -> base * 3L;
                default -> base;
            };
            this.color = c.color;
            this.accent = c.accentColor;
            this.maxRadius = c.maxRadius * c.size;
            this.rings = c.rings;
            this.lineWidth = c.lineWidth;
            this.segments = c.segments;
            this.heightOffset = c.heightOffset;
            this.riseHeight = c.soulRise * c.size;
            this.seed = ThreadLocalRandom.current().nextLong();
        }
    }

    private static final List<Effect> EFFECTS = new ArrayList<>();
    private static final int MAX_EFFECTS = 16;

    private KillEffect() {
    }

    public static void spawn(Vec3d center) {
        VibeVisualsConfig.KillEffectConfig c = VibeVisualsConfigManager.get().killEffect;
        if (!c.enabled || center == null) {
            return;
        }
        synchronized (EFFECTS) {
            if (EFFECTS.size() >= MAX_EFFECTS) {
                EFFECTS.remove(0);
            }
            EFFECTS.add(new Effect(center, c));
        }
        spawnParticles(center, c);
    }

    /** Emits the rising soul trail (called each client tick). */
    public static void tick(MinecraftClient client) {
        if (client.world == null) {
            return;
        }
        long now = System.currentTimeMillis();
        ThreadLocalRandom r = ThreadLocalRandom.current();
        synchronized (EFFECTS) {
            for (Effect fx : EFFECTS) {
                float t = (now - fx.startMs) / (float) fx.lifeMs;
                if (t < 0f || t >= 1f) {
                    continue;
                }
                if (fx.style == Style.SOUL) {
                    double y = fx.center.y + fx.heightOffset + fx.riseHeight * easeOutSoul(t);
                    for (int i = 0; i < 2; i++) {
                        double ox = (r.nextDouble() - 0.5) * 0.3;
                        double oz = (r.nextDouble() - 0.5) * 0.3;
                        client.world.addParticleClient(ParticleTypes.SOUL_FIRE_FLAME,
                                fx.center.x + ox, y, fx.center.z + oz, 0.0, 0.02, 0.0);
                    }
                } else if (fx.style == Style.BURN) {
                    // Flames fill a column that shrinks as the body burns away;
                    // smoke keeps rising above it.
                    float columnH = (1.9f * fx.maxRadius / 2.6f) * (1.0f - t);
                    for (int i = 0; i < 4; i++) {
                        double ox = (r.nextDouble() - 0.5) * 0.5;
                        double oz = (r.nextDouble() - 0.5) * 0.5;
                        double fy = fx.center.y + 0.1 + r.nextDouble() * Math.max(0.1, columnH);
                        client.world.addParticleClient(
                                r.nextBoolean() ? ParticleTypes.FLAME : ParticleTypes.SMALL_FLAME,
                                fx.center.x + ox, fy, fx.center.z + oz,
                                0.0, 0.02 + r.nextDouble() * 0.03, 0.0);
                    }
                    if (now % 2 == 0) {
                        double ox = (r.nextDouble() - 0.5) * 0.4;
                        double oz = (r.nextDouble() - 0.5) * 0.4;
                        client.world.addParticleClient(ParticleTypes.LARGE_SMOKE,
                                fx.center.x + ox, fx.center.y + columnH + 0.3, fx.center.z + oz,
                                0.0, 0.06, 0.0);
                    }
                }
            }
        }
    }

    public static void render(WorldRenderContext context) {
        VibeVisualsConfig.KillEffectConfig c = VibeVisualsConfigManager.get().killEffect;
        if (!c.enabled) {
            synchronized (EFFECTS) {
                EFFECTS.clear();
            }
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        Vec3d camera = client.gameRenderer.getCamera().getCameraPos();
        MatrixStack.Entry entry = context.matrices().peek();
        Matrix4f matrix = entry.getPositionMatrix();
        VertexConsumer lines = context.consumers().getBuffer(RenderLayers.linesTranslucent());

        long now = System.currentTimeMillis();
        synchronized (EFFECTS) {
            Iterator<Effect> it = EFFECTS.iterator();
            while (it.hasNext()) {
                Effect fx = it.next();
                float t = (now - fx.startMs) / (float) fx.lifeMs;
                if (t >= 1.0f) {
                    it.remove();
                    continue;
                }
                draw(lines, entry, matrix, camera, fx, t);
            }
        }
    }

    private static void draw(VertexConsumer lines, MatrixStack.Entry entry, Matrix4f matrix,
                             Vec3d camera, Effect fx, float t) {
        Vec3d center = fx.center.add(0.0, fx.heightOffset, 0.0);
        float ease = 1.0f - (1.0f - t) * (1.0f - t);
        int baseAlpha = Math.round(235f * (1.0f - t));
        if (baseAlpha <= 0) {
            return;
        }

        switch (fx.style) {
            case SOUL -> drawSoul(lines, entry, matrix, camera, center, fx, t);
            case BURN -> drawBurn(lines, entry, matrix, camera, center, fx, t);
            case LIGHTNING -> {
                drawRing(lines, entry, matrix, camera, center, fx.maxRadius * ease, fx.color, baseAlpha, fx);
                drawBolts(lines, entry, matrix, camera, center, fx, t, baseAlpha);
            }
            case FLASH -> {
                // Several quick rings expanding at slightly different speeds.
                for (int r = 0; r < Math.max(2, fx.rings); r++) {
                    float speed = 1.0f + r * 0.35f;
                    float radius = fx.maxRadius * Math.min(1.0f, ease * speed);
                    int alpha = Math.round(baseAlpha * (1.0f - r * 0.2f));
                    if (alpha <= 0) continue;
                    drawRing(lines, entry, matrix, camera, center, radius,
                            r % 2 == 0 ? fx.color : fx.accent, alpha, fx);
                }
            }
            default -> { // RING and BURST share the staggered-ring look.
                for (int r = 0; r < fx.rings; r++) {
                    float stagger = fx.rings <= 1 ? 0f : (r / (float) fx.rings) * 0.4f;
                    float rt = ease - stagger;
                    if (rt <= 0f) continue;
                    float radius = fx.maxRadius * Math.min(1.0f, rt);
                    int alpha = Math.round(baseAlpha * (1.0f - stagger));
                    if (alpha <= 0) continue;
                    drawRing(lines, entry, matrix, camera, center, radius,
                            r % 2 == 0 ? fx.color : fx.accent, alpha, fx);
                }
            }
        }
    }

    /**
     * SOUL: a glowing wisp that lifts off the body and floats up to the sky,
     * shrinking and fading, leaving a faint vertical trail. The wisp itself is
     * a small pair of cross-rings (cheap orb stand-in) drawn at the rising
     * height; the soul particles carry most of the look.
     */
    private static void drawSoul(VertexConsumer lines, MatrixStack.Entry entry, Matrix4f matrix,
                                 Vec3d camera, Vec3d ground, Effect fx, float t) {
        float climb = easeOutSoul(t);
        double soulY = ground.y + fx.riseHeight * climb;
        Vec3d soul = new Vec3d(ground.x, soulY, ground.z);

        // Alpha: fade in quickly, fade out over the last third.
        float fadeIn = Math.min(1.0f, t / 0.12f);
        float fadeOut = Math.min(1.0f, (1.0f - t) / 0.33f);
        int alpha = Math.round(235f * fadeIn * fadeOut);
        if (alpha <= 0) {
            return;
        }

        // Faint vertical trail from the body up to the soul.
        int trailAlpha = Math.round(alpha * 0.35f);
        drawLine(lines, entry, matrix, camera, ground.add(0.0, 0.2, 0.0), soul, fx.accent, trailAlpha, fx.lineWidth);

        // The wisp: two small rings (horizontal + vertical) that shrink as it rises.
        float orb = 0.45f * (1.0f - 0.5f * climb);
        drawHorizontalRing(lines, entry, matrix, camera, soul, orb, fx.color, alpha, 20, fx.lineWidth);
        drawVerticalRing(lines, entry, matrix, camera, soul, orb, fx.accent, alpha, 20, fx.lineWidth);
    }

    private static void drawHorizontalRing(VertexConsumer lines, MatrixStack.Entry entry, Matrix4f matrix,
                                           Vec3d camera, Vec3d c, float radius, int color, int alpha,
                                           int seg, float width) {
        Vec3d prev = null;
        for (int i = 0; i <= seg; i++) {
            double a = Math.PI * 2.0 * i / seg;
            Vec3d p = c.add(Math.cos(a) * radius, 0.0, Math.sin(a) * radius);
            if (prev != null) drawLine(lines, entry, matrix, camera, prev, p, color, alpha, width);
            prev = p;
        }
    }

    private static void drawVerticalRing(VertexConsumer lines, MatrixStack.Entry entry, Matrix4f matrix,
                                         Vec3d camera, Vec3d c, float radius, int color, int alpha,
                                         int seg, float width) {
        Vec3d prev = null;
        for (int i = 0; i <= seg; i++) {
            double a = Math.PI * 2.0 * i / seg;
            Vec3d p = c.add(Math.cos(a) * radius, Math.sin(a) * radius, 0.0);
            if (prev != null) drawLine(lines, entry, matrix, camera, prev, p, color, alpha, width);
            prev = p;
        }
    }

    private static float easeOutSoul(float t) {
        // Quick lift then gentle drift — ease-out cubic.
        float p = 1.0f - t;
        return 1.0f - p * p * p;
    }

    /**
     * BURN: smouldering fire rings at the feet that flicker and fade as the
     * "body" is consumed. The flames + smoke come from the particle stream.
     */
    private static void drawBurn(VertexConsumer lines, MatrixStack.Entry entry, Matrix4f matrix,
                                 Vec3d camera, Vec3d ground, Effect fx, float t) {
        float flick = 0.55f + 0.45f * (float) Math.sin(System.currentTimeMillis() * 0.02 + fx.seed);
        int baseAlpha = Math.round(200f * (1.0f - t) * flick);
        if (baseAlpha <= 0) {
            return;
        }
        float baseR = fx.maxRadius * 0.22f; // small ring around the feet
        // A couple of stacked flickering rings: base ring + a smaller, higher one.
        drawHorizontalRing(lines, entry, matrix, camera, ground.add(0.0, 0.05, 0.0),
                baseR * (0.9f + 0.2f * flick), fx.color, baseAlpha, 24, fx.lineWidth);
        drawHorizontalRing(lines, entry, matrix, camera, ground.add(0.0, 0.35 + 0.1 * flick, 0.0),
                baseR * 0.6f, fx.accent, Math.round(baseAlpha * 0.8f), 20, fx.lineWidth);
    }

    private static void drawBolts(VertexConsumer lines, MatrixStack.Entry entry, Matrix4f matrix,
                                  Vec3d camera, Vec3d center, Effect fx, float t, int alpha) {
        int bolts = Math.max(3, fx.rings * 3);
        float height = fx.maxRadius * 1.6f;
        int steps = 6;
        java.util.Random rnd = new java.util.Random(fx.seed);
        for (int b = 0; b < bolts; b++) {
            double baseAngle = Math.PI * 2.0 * b / bolts;
            Vec3d prev = center;
            for (int s = 1; s <= steps; s++) {
                float frac = s / (float) steps;
                double jitter = (rnd.nextDouble() - 0.5) * 0.5;
                double ang = baseAngle + jitter;
                double rad = 0.25 + rnd.nextDouble() * 0.35;
                Vec3d next = center.add(Math.cos(ang) * rad, height * frac, Math.sin(ang) * rad);
                drawLine(lines, entry, matrix, camera, prev, next, fx.accent, alpha, fx.lineWidth);
                prev = next;
            }
        }
    }

    private static void drawRing(VertexConsumer lines, MatrixStack.Entry entry, Matrix4f matrix,
                                 Vec3d camera, Vec3d center, float radius, int color, int alpha, Effect fx) {
        Vec3d previous = null;
        for (int index = 0; index <= fx.segments; index++) {
            double angle = Math.PI * 2.0 * index / fx.segments;
            Vec3d next = center.add(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius);
            if (previous != null) {
                drawLine(lines, entry, matrix, camera, previous, next, color, alpha, fx.lineWidth);
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

    private static void spawnParticles(Vec3d center, VibeVisualsConfig.KillEffectConfig c) {
        if (!c.spawnParticles || c.particleCount <= 0) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return;
        }
        ThreadLocalRandom r = ThreadLocalRandom.current();

        // SOUL: a rising column of soul flames + soul particles streaming skyward.
        if (Style.from(c.style) == Style.SOUL) {
            int count = Math.max(12, c.particleCount);
            for (int i = 0; i < count; i++) {
                double angle = r.nextDouble(Math.PI * 2.0);
                double rad = r.nextDouble() * 0.35;
                double px = center.x + Math.cos(angle) * rad;
                double pz = center.z + Math.sin(angle) * rad;
                double vy = 0.10 + r.nextDouble() * 0.18; // steady upward drift
                client.world.addParticleClient(ParticleTypes.SOUL_FIRE_FLAME, px, center.y + 0.2, pz,
                        (r.nextDouble() - 0.5) * 0.02, vy, (r.nextDouble() - 0.5) * 0.02);
                if (i % 2 == 0) {
                    client.world.addParticleClient(ParticleTypes.SOUL, px, center.y + 0.3, pz,
                            (r.nextDouble() - 0.5) * 0.03, vy * 0.8, (r.nextDouble() - 0.5) * 0.03);
                }
            }
            return;
        }

        // BURN: an initial flare of lava + flame outward and a puff of smoke.
        if (Style.from(c.style) == Style.BURN) {
            int count = Math.max(16, c.particleCount);
            for (int i = 0; i < count; i++) {
                double angle = r.nextDouble(Math.PI * 2.0);
                double speed = 0.08 + r.nextDouble() * 0.18;
                double vx = Math.cos(angle) * speed;
                double vz = Math.sin(angle) * speed;
                double vy = 0.05 + r.nextDouble() * 0.25;
                client.world.addParticleClient(ParticleTypes.FLAME, center.x, center.y + 0.2, center.z, vx, vy, vz);
                if (i % 3 == 0) {
                    client.world.addParticleClient(ParticleTypes.LAVA, center.x, center.y + 0.2, center.z,
                            vx * 0.5, vy, vz * 0.5);
                }
                if (i % 2 == 0) {
                    client.world.addParticleClient(ParticleTypes.LARGE_SMOKE, center.x, center.y + 0.4, center.z,
                            vx * 0.3, 0.1 + r.nextDouble() * 0.15, vz * 0.3);
                }
            }
            return;
        }

        DustParticleEffect dust = new DustParticleEffect(c.accentColor & 0x00FFFFFF, 1.5f);
        for (int i = 0; i < c.particleCount; i++) {
            double angle = Math.PI * 2.0 * i / c.particleCount;
            double speed = 0.15 + r.nextDouble() * 0.25;
            double vx = Math.cos(angle) * speed;
            double vz = Math.sin(angle) * speed;
            double vy = 0.15 + r.nextDouble() * 0.45; // fountain upward
            client.world.addParticleClient(dust, center.x, center.y + 0.2, center.z, vx, vy, vz);
        }
        // A few sparkles for punch.
        for (int i = 0; i < Math.min(12, c.particleCount / 3); i++) {
            double vx = (r.nextDouble() - 0.5) * 0.3;
            double vz = (r.nextDouble() - 0.5) * 0.3;
            double vy = 0.3 + r.nextDouble() * 0.5;
            client.world.addParticleClient(ParticleTypes.END_ROD, center.x, center.y + 0.3, center.z, vx, vy, vz);
        }
    }
}
