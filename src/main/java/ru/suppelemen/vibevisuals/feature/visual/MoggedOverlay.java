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

    /** World-render hook (WorldRenderEvents.AFTER_ENTITIES). */
    public static void render(WorldRenderContext context) {
        VibeVisualsConfig.MoggedConfig config = VibeVisualsConfigManager.get().mogged;
        if (!config.enabled || ACTIVE.isEmpty()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        Camera camera = client.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getCameraPos();
        long now = System.currentTimeMillis();
        Iterator<Mogged> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Mogged m = it.next();
            if (now >= m.expiresAt || m.target.isRemoved() || !m.target.isAlive()) {
                ACTIVE.remove(m);
                continue;
            }
            renderBanner(context, client, camera, cameraPos, m, config);
        }
    }

    private static void renderBanner(WorldRenderContext context, MinecraftClient client,
                                      Camera camera, Vec3d cameraPos, Mogged m,
                                      VibeVisualsConfig.MoggedConfig config) {
        Vec3d lerped = m.target.getLerpedPos(1.0f);
        // Eye height + small lift = banner sits right by the opponent's face.
        Vec3d worldPos = lerped.add(0.0, m.target.getStandingEyeHeight() + 0.35, 0.0);

        VertexConsumerProvider consumers = context.consumers();
        if (consumers == null) {
            return;
        }

        MatrixStack matrices = context.matrices();
        matrices.push();
        matrices.translate(
                worldPos.x - cameraPos.x,
                worldPos.y - cameraPos.y,
                worldPos.z - cameraPos.z);
        // Billboard toward camera (vanilla nametag rotation).
        // Camera rotation is what billboards face — equivalent to the dispatcher rotation
        // vanilla nametags use.
        matrices.multiply(camera.getRotation());
        // Crucial: ONLY Y is negated. Negating X too would back-face-cull the text quads.
        float s = 0.025f * Math.max(0.4f, config.bannerScale);
        matrices.scale(s, -s, s);

        TextRenderer tr = client.textRenderer;
        Text text = Text.literal("MOGGED");
        int textWidth = tr.getWidth(text);
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        int textColor = 0xFFFF1F1F;          // bright red
        int bgColor = 0xCC000000;            // 80% black plate
        int fullBright = 0xF000F0;
        float x = -textWidth / 2.0f;
        float y = -tr.fontHeight / 2.0f;

        // First pass: faint visible-through-walls version (vanilla nametag trick).
        tr.draw(text, x, y, 0x60FF1F1F, false, matrix, consumers,
                TextRenderer.TextLayerType.SEE_THROUGH, bgColor, fullBright);
        // Second pass: solid front-facing version on top.
        tr.draw(text, x, y, textColor, false, matrix, consumers,
                TextRenderer.TextLayerType.NORMAL, 0, fullBright);

        matrices.pop();
    }

    private static final class Mogged {
        final Entity target;
        final long expiresAt;

        Mogged(Entity target, long expiresAt) {
            this.target = target;
            this.expiresAt = expiresAt;
        }
    }
}
