package ru.suppelemen.vibevisuals.feature.visual;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;

import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Floating damage numbers that pop above a hit entity, drift upward and fade.
 * Rendered in world space, billboarded toward the camera using the same matrix
 * recipe as {@link MoggedOverlay}.
 */
public final class DamageIndicators {

    private static final class Indicator {
        final Vec3d origin;     // world position at spawn (entity head)
        final long startMs;
        final long lifeMs;
        final String text;
        final int rgb;          // colour without alpha
        final float scale;      // base scale multiplier (crits are bigger)
        final float jitterX;
        final float jitterZ;

        Indicator(Vec3d origin, long lifeMs, String text, int rgb, float scale) {
            this.origin = origin;
            this.startMs = System.currentTimeMillis();
            this.lifeMs = lifeMs;
            this.text = text;
            this.rgb = rgb & 0x00FFFFFF;
            this.scale = scale;
            ThreadLocalRandom r = ThreadLocalRandom.current();
            this.jitterX = (float) r.nextDouble(-0.25, 0.25);
            this.jitterZ = (float) r.nextDouble(-0.25, 0.25);
        }
    }

    private static final CopyOnWriteArrayList<Indicator> ACTIVE = new CopyOnWriteArrayList<>();
    private static final int MAX_ACTIVE = 64;

    private DamageIndicators() {
    }

    public static void spawn(Entity target, float amount, boolean crit) {
        VibeVisualsConfig.DamageIndicatorsConfig c = VibeVisualsConfigManager.get().damageIndicators;
        if (!c.enabled || target == null) {
            return;
        }
        if (c.onlyPlayers && !(target instanceof net.minecraft.entity.player.PlayerEntity)) {
            return;
        }
        int rgb = crit ? c.critColor : c.color;
        float scale = c.size * (crit ? 1.0f + c.critSizeBonus : 1.0f);
        add(target, format(amount, c.showDecimals), rgb, scale, c.lifeSeconds);
    }

    public static void spawnHeal(Entity target, float amount) {
        VibeVisualsConfig.DamageIndicatorsConfig c = VibeVisualsConfigManager.get().damageIndicators;
        if (!c.enabled || !c.showHealing || target == null) {
            return;
        }
        if (c.onlyPlayers && !(target instanceof net.minecraft.entity.player.PlayerEntity)) {
            return;
        }
        add(target, "+" + format(amount, c.showDecimals), c.healColor, c.size, c.lifeSeconds);
    }

    private static void add(Entity target, String text, int rgb, float scale, float lifeSeconds) {
        Vec3d origin = target.getLerpedPos(1.0f)
                .add(0.0, target.getStandingEyeHeight() + 0.55, 0.0);
        if (ACTIVE.size() >= MAX_ACTIVE) {
            ACTIVE.remove(0);
        }
        ACTIVE.add(new Indicator(origin, Math.round(lifeSeconds * 1000.0f), text, rgb, scale));
    }

    public static void clear() {
        ACTIVE.clear();
    }

    public static void render(WorldRenderContext context) {
        VibeVisualsConfig.DamageIndicatorsConfig c = VibeVisualsConfigManager.get().damageIndicators;
        if (!c.enabled || ACTIVE.isEmpty()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        VertexConsumerProvider consumers = context.consumers();
        if (consumers == null) {
            return;
        }
        Camera camera = client.gameRenderer.getCamera();
        Vec3d camPos = camera.getCameraPos();
        TextRenderer tr = client.textRenderer;
        MatrixStack matrices = context.matrices();

        long now = System.currentTimeMillis();
        Iterator<Indicator> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Indicator ind = it.next();
            float t = (now - ind.startMs) / (float) ind.lifeMs;
            if (t >= 1.0f) {
                ACTIVE.remove(ind);
                continue;
            }
            renderOne(tr, matrices, consumers, camera, camPos, ind, t, c);
        }
    }

    private static void renderOne(TextRenderer tr, MatrixStack matrices, VertexConsumerProvider consumers,
                                  Camera camera, Vec3d camPos, Indicator ind, float t,
                                  VibeVisualsConfig.DamageIndicatorsConfig c) {
        // Rise upward over the lifetime; fade in fast, fade out over the back half.
        double rise = c.riseDistance * easeOut(t);
        double x = ind.origin.x + ind.jitterX;
        double y = ind.origin.y + rise;
        double z = ind.origin.z + ind.jitterZ;

        float popIn = clamp(t / 0.10f, 0.0f, 1.0f);
        float fadeOut = clamp((1.0f - t) / 0.40f, 0.0f, 1.0f);
        float alpha = popIn * fadeOut;
        if (alpha <= 0.01f) {
            return;
        }
        float pop = 0.7f + 0.3f * easeOutBack(popIn);

        matrices.push();
        matrices.translate(x - camPos.x, y - camPos.y, z - camPos.z);
        matrices.multiply(camera.getRotation());
        float s = 0.025f * ind.scale * pop;
        matrices.scale(s, -s, s); // only Y negated (winding!)
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        Text text = Text.literal(ind.text);
        float tx = -tr.getWidth(text) / 2.0f;
        float ty = -tr.fontHeight / 2.0f;
        int light = 0xF000F0;
        int alphaByte = Math.round(alpha * 255.0f) & 0xFF;
        int color = (alphaByte << 24) | ind.rgb;

        if (c.seeThrough) {
            int seeThrough = (Math.round(alpha * 0x55) << 24) | ind.rgb;
            tr.draw(text, tx, ty, seeThrough, false, matrix, consumers,
                    TextRenderer.TextLayerType.SEE_THROUGH, 0, light);
        }
        tr.draw(text, tx, ty, color, false, matrix, consumers,
                TextRenderer.TextLayerType.NORMAL, 0, light);
        matrices.pop();
    }

    private static String format(float amount, boolean decimals) {
        if (!decimals || Math.abs(amount - Math.round(amount)) < 0.05f) {
            return Integer.toString(Math.round(amount));
        }
        return String.format(Locale.US, "%.1f", amount);
    }

    private static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    private static float easeOut(float t) {
        return 1.0f - (1.0f - t) * (1.0f - t);
    }

    private static float easeOutBack(float t) {
        float c1 = 1.70158f;
        float c3 = c1 + 1.0f;
        float p = t - 1.0f;
        return 1.0f + c3 * p * p * p + c1 * p * p;
    }
}
