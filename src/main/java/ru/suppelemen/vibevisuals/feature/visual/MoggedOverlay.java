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
 * Joke feature: whenever the local player hits another player, drop a bold
 * red <strong>MOGGED</strong> banner above the target's head and play a
 * user-supplied meme sound from the vibevisuals sounds dir.
 *
 * Banner rendering mirrors vanilla's nametag pipeline (translate -> billboard
 * rotation -> negative-Y scale).  Sound is loaded as a plain .wav file from
 * the vibevisuals sounds dir via {@link CustomHitSoundPlayer#playSoundFile}.
 */
public final class MoggedOverlay {

    private static final CopyOnWriteArrayList<Mogged> ACTIVE = new CopyOnWriteArrayList<>();

    private MoggedOverlay() {
    }

    /** Called whenever the local player attacks another player. */
    public static void onHit(PlayerEntity target) {
        VibeVisualsConfig.MoggedConfig config = VibeVisualsConfigManager.get().mogged;
        if (!config.enabled || target == null) {
            return;
        }

        long expiresAt = System.currentTimeMillis() + Math.round(config.displayDurationSeconds * 1000.0f);
        // Replace any existing entry for the same target so the timer restarts.
        ACTIVE.removeIf(m -> m.target == target);
        ACTIVE.add(new Mogged(target, expiresAt));

        if (config.playSound) {
            CustomHitSoundPlayer.playSoundFile(config.soundFile, config.volume);
        }
    }

    public static void clear() {
        ACTIVE.clear();
    }

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
            renderBanner(context, client.textRenderer, camera, cameraPos, m, config);
        }
    }

    private static void renderBanner(WorldRenderContext context, TextRenderer tr,
                                      Camera camera, Vec3d cameraPos, Mogged m,
                                      VibeVisualsConfig.MoggedConfig config) {
        Vec3d lerped = m.target.getLerpedPos(1.0f);
        Vec3d pos = lerped.add(0.0, m.target.getHeight() + 0.85, 0.0);

        MatrixStack matrices = context.matrices();
        matrices.push();
        matrices.translate(
                (float) (pos.x - cameraPos.x),
                (float) (pos.y - cameraPos.y),
                (float) (pos.z - cameraPos.z));
        matrices.multiply(camera.getRotation());
        // 0.025 is the vanilla nametag world scale.
        float s = 0.025f * Math.max(0.4f, config.bannerScale);
        matrices.scale(-s, -s, s);

        String label = "MOGGED";
        int textWidth = tr.getWidth(label);
        int textHeight = tr.fontHeight;
        // Padded plate around the glyphs.
        int padX = 6;
        int padY = 3;
        float bgLeft = -textWidth / 2.0f - padX;
        float bgRight = textWidth / 2.0f + padX;
        float bgTop = -padY;
        float bgBottom = textHeight + padY;

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        VertexConsumerProvider consumers = context.consumers();
        if (consumers == null) {
            matrices.pop();
            return;
        }

        int textColor = 0xFFFF1F1F;
        int bgColor = 0xFF000000;
        int fullBright = 0xF000F0;

        // 1) Render text twice (vanilla nametag pattern): a low-alpha SEE_THROUGH pass
        //    so the banner stays readable through walls, then a solid NORMAL pass on top.
        Text text = Text.literal(label);
        float textX = -textWidth / 2.0f;
        // Background plate is supplied by passing a non-zero backgroundColor.
        tr.draw(text, textX, 0, 0x60FF1F1F, false, matrix, consumers,
                TextRenderer.TextLayerType.SEE_THROUGH, bgColor, fullBright);
        tr.draw(text, textX, 0, textColor, false, matrix, consumers,
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
