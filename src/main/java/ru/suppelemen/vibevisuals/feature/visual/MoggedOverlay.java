package ru.suppelemen.vibevisuals.feature.visual;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;
import ru.suppelemen.vibevisuals.feature.sound.CustomHitSoundPlayer;

import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Drops a bold red <strong>MOGGED</strong> banner above any player you hit
 * and plays a user-supplied .wav. Rendering happens in HUD space — we
 * project the target's head from world coordinates to screen coordinates
 * using the camera, then draw the banner directly with a DrawContext.  This
 * avoids the matrix-stack quirks of world-space text and guarantees the
 * banner shows up regardless of render layer pipeline state.
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

        long expiresAt = System.currentTimeMillis() + Math.round(config.displayDurationSeconds * 1000.0f);
        ACTIVE.removeIf(m -> m.target == target);
        ACTIVE.add(new Mogged(target, expiresAt));
        System.out.println("[vibevisuals.mogged] added to ACTIVE (size=" + ACTIVE.size() + ")");

        if (config.playSound) {
            CustomHitSoundPlayer.playSoundFile(config.soundFile, config.volume);
        }
    }

    public static void clear() {
        ACTIVE.clear();
    }

    /** HUD render — called from the main HUD element registry. */
    public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        VibeVisualsConfig.MoggedConfig config = VibeVisualsConfigManager.get().mogged;
        if (!config.enabled || ACTIVE.isEmpty()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) {
            return;
        }
        Camera camera = client.gameRenderer.getCamera();

        long now = System.currentTimeMillis();
        Iterator<Mogged> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Mogged m = it.next();
            if (now >= m.expiresAt || m.target.isRemoved() || !m.target.isAlive()) {
                ACTIVE.remove(m);
                continue;
            }
            // Eye-level position of the target so the banner anchors near their face.
            Vec3d lerped = m.target.getLerpedPos(tickCounter.getTickProgress(true));
            Vec3d worldPos = lerped.add(0.0, m.target.getStandingEyeHeight() + 0.35, 0.0);
            int scaledW = client.getWindow().getScaledWidth();
            int scaledH = client.getWindow().getScaledHeight();
            ScreenPoint sp = worldToScreen(camera, worldPos, scaledW, scaledH);
            // Fallback: if projection failed (target behind camera, etc.), draw centred so the
            // user still sees the banner.  Better than silently rendering nothing.
            int cx = sp == null ? scaledW / 2 : sp.x;
            int cy = sp == null ? scaledH / 2 : sp.y;
            drawBanner(ctx, client, cx, cy, config.bannerScale);
        }
    }

    private static void drawBanner(DrawContext ctx, MinecraftClient client,
                                    int cx, int cy, float scale) {
        Text text = Text.literal("MOGGED");
        int textW = client.textRenderer.getWidth(text);
        int textH = client.textRenderer.fontHeight;

        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate((float) cx, (float) cy);
        ctx.getMatrices().scale(scale, scale);
        // Pad-and-plate background.
        int padX = 6;
        int padY = 3;
        int bgX1 = -textW / 2 - padX;
        int bgY1 = -textH / 2 - padY;
        int bgX2 = textW / 2 + padX;
        int bgY2 = textH / 2 + padY;
        ctx.fill(bgX1, bgY1, bgX2, bgY2, 0xFF000000);
        // Solid bright-red MOGGED label, centred on (0, 0).
        ctx.drawText(client.textRenderer, text, -textW / 2, -textH / 2, 0xFFFF1F1F, false);
        ctx.getMatrices().popMatrix();
    }

    /**
     * Standard pinhole projection.  Returns null if the point is behind the
     * camera or far outside the viewport.
     */
    private static ScreenPoint worldToScreen(Camera camera, Vec3d world, int scaledW, int scaledH) {
        Vec3d rel = world.subtract(camera.getCameraPos());
        // Camera rotation rotates world->view; invert it so we can transform a world point into view space.
        Quaternionf viewRot = new Quaternionf(camera.getRotation()).conjugate();
        Vector3f p = new Vector3f((float) rel.x, (float) rel.y, (float) rel.z);
        viewRot.transform(p);
        // In MC's view space the camera looks along +Z (after the conjugate).  Points in front have z > 0.
        if (p.z <= 0.05f) {
            return null;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        double fov = client.options.getFov().getValue();
        float aspect = (float) scaledW / Math.max(1, scaledH);
        float tanHalfFov = (float) Math.tan(Math.toRadians(fov / 2.0));

        // Standard pinhole projection to NDC (-1..1).
        float ndcX = p.x / (p.z * tanHalfFov * aspect);
        float ndcY = p.y / (p.z * tanHalfFov);

        int sx = Math.round((ndcX + 1.0f) * 0.5f * scaledW);
        int sy = Math.round((1.0f - ndcY) * 0.5f * scaledH);
        // Skip points wildly off-screen but allow a margin so banners can fade at the edge.
        if (sx < -scaledW || sx > scaledW * 2 || sy < -scaledH || sy > scaledH * 2) {
            return null;
        }
        return new ScreenPoint(sx, sy);
    }

    private record ScreenPoint(int x, int y) {}

    private static final class Mogged {
        final Entity target;
        final long expiresAt;

        Mogged(Entity target, long expiresAt) {
            this.target = target;
            this.expiresAt = expiresAt;
        }
    }
}
