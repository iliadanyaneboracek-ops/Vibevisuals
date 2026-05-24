package ru.suppelemen.vibevisuals.feature.visual;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;
import ru.suppelemen.vibevisuals.feature.sound.CustomHitSoundPlayer;

import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Drops a bold red <strong>MOGGED</strong> banner above any player you hit
 * and plays a user-supplied .wav.  Rendered in <em>world space</em> using
 * the exact same matrix recipe vanilla uses for nametags so it sticks to
 * the opponent's head and stays billboarded toward the camera.
 *
 * <p>Key detail: the scale must be {@code (+s, -s, +s)} — negating <em>only</em>
 * the Y axis (to flip text right-side up in world space).  Negating both X
 * and Y inverts the polygon winding and the text gets back-face culled,
 * which was the silent failure mode in the previous attempt.
 */
public final class MoggedOverlay {

    private static final CopyOnWriteArrayList<Mogged> ACTIVE = new CopyOnWriteArrayList<>();

    private MoggedOverlay() {
    }

    /** Called whenever the local player attacks another player. */
    public static void onHit(PlayerEntity target) {
        VibeVisualsConfig.MoggedConfig config = VibeVisualsConfigManager.get().mogged;
        System.out.println("[vibevisuals.mogged] onHit fired (enabled=" + config.enabled
                + ", target=" + (target == null ? "null" : target.getName().getString()) + ")");
        if (!config.enabled || target == null) {
            return;
        }

        long now = System.currentTimeMillis();
        long totalMs = Math.round(config.displayDurationSeconds * 1000.0f);
        long expiresAt = now + totalMs;
        ACTIVE.removeIf(m -> m.target == target);
        ACTIVE.add(new Mogged(target, now, expiresAt, totalMs));
        System.out.println("[vibevisuals.mogged] added to ACTIVE (size=" + ACTIVE.size() + ")");

        if (config.playSound) {
            CustomHitSoundPlayer.playSoundFile(config.soundFile, config.volume);
        }
    }

    public static void clear() {
        ACTIVE.clear();
    }

    /** World-render hook (WorldRenderEvents.AFTER_ENTITIES). */
    public static void render(WorldRenderContext context) {
        VibeVisualsConfig.MoggedConfig config = VibeVisualsConfigManager.get().mogged;
        if (!config.enabled || ACTIVE.isEmpty()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        Camera camera = client.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getCameraPos();
        // Per-frame tick interpolation — without this the banner snaps at 20 TPS
        // because getLerpedPos(1.0) returns the end-of-tick position.
        float tickProgress = 1.0f;
        try {
            tickProgress = client.getRenderTickCounter().getTickProgress(true);
        } catch (Throwable ignored) {
        }
        long now = System.currentTimeMillis();
        Iterator<Mogged> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Mogged m = it.next();
            if (now >= m.expiresAt || m.target.isRemoved() || !m.target.isAlive()) {
                ACTIVE.remove(m);
                continue;
            }
            renderBanner(context, client, camera, cameraPos, m, tickProgress, config);
        }
    }

    private static void renderBanner(WorldRenderContext context, MinecraftClient client,
                                      Camera camera, Vec3d cameraPos, Mogged m,
                                      float tickProgress, VibeVisualsConfig.MoggedConfig config) {
        Vec3d lerped = m.target.getLerpedPos(tickProgress);
        // Eye height + small lift = banner sits right by the opponent's face.
        Vec3d worldPos = lerped.add(0.0, m.target.getStandingEyeHeight() + 0.35, 0.0);

        VertexConsumerProvider consumers = context.consumers();
        if (consumers == null) {
            return;
        }

        // -- per-frame animation -------------------------------------------------
        // The banner pops in over the first 120 ms, holds, and fades out over the
        // last 300 ms. All math is cheap (a couple of float ops), so it runs at
        // your framerate without any meaningful cost.
        long now = System.currentTimeMillis();
        float elapsed = (now - m.startedAt) / 1000.0f;
        float remaining = (m.expiresAt - now) / 1000.0f;
        float popIn = clamp(elapsed / 0.12f, 0.0f, 1.0f);
        float fadeOut = clamp(remaining / 0.30f, 0.0f, 1.0f);
        float alpha = popIn * fadeOut;
        if (alpha <= 0.005f) {
            return;
        }
        // Slight overshoot at spawn for a punchy "drop" feel.
        float popScale = 0.85f + 0.25f * easeOutBack(popIn);
        // -----------------------------------------------------------------------

        MatrixStack matrices = context.matrices();
        matrices.push();
        matrices.translate(
                worldPos.x - cameraPos.x,
                worldPos.y - cameraPos.y,
                worldPos.z - cameraPos.z);
        matrices.multiply(camera.getRotation());
        // Crucial: ONLY Y is negated. Negating X too would back-face-cull the text quads.
        float s = 0.025f * Math.max(0.4f, config.bannerScale) * popScale;
        matrices.scale(s, -s, s);

        TextRenderer tr = client.textRenderer;
        Text text = Text.literal("MOGGED");
        int textWidth = tr.getWidth(text);
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        int fullBright = 0xF000F0;
        int alphaByte = Math.round(alpha * 255.0f) & 0xFF;
        int textColor = (alphaByte << 24) | 0x00FF1F1F;
        int textColorSeeThrough = (Math.round(alpha * 0x60) << 24) | 0x00FF1F1F;
        int bgColor = (Math.round(alpha * 0xCC) << 24) | 0x00000000;
        float x = -textWidth / 2.0f;
        float y = -tr.fontHeight / 2.0f;

        // First pass: faint visible-through-walls version (vanilla nametag trick).
        tr.draw(text, x, y, textColorSeeThrough, false, matrix, consumers,
                TextRenderer.TextLayerType.SEE_THROUGH, bgColor, fullBright);
        // Second pass: solid front-facing version on top.
        tr.draw(text, x, y, textColor, false, matrix, consumers,
                TextRenderer.TextLayerType.NORMAL, 0, fullBright);

        matrices.pop();
    }

    private static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    /** Slight overshoot easing — gives the banner a snappy "pop" feel on spawn. */
    private static float easeOutBack(float t) {
        float c1 = 1.70158f;
        float c3 = c1 + 1.0f;
        float p = t - 1.0f;
        return 1.0f + c3 * p * p * p + c1 * p * p;
    }

    private static final class Mogged {
        final Entity target;
        final long startedAt;
        final long expiresAt;
        final long totalMs;

        Mogged(Entity target, long startedAt, long expiresAt, long totalMs) {
            this.target = target;
            this.startedAt = startedAt;
            this.expiresAt = expiresAt;
            this.totalMs = totalMs;
        }
    }
}
