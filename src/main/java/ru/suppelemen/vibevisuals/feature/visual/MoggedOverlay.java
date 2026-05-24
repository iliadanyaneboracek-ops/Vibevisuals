package ru.suppelemen.vibevisuals.feature.visual;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;

import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Joke feature: whenever the local player hits another player, drop a bold
 * red <strong>MOGGED</strong> banner above the target's head and play a
 * meme-style impact sound. The banner stays for a short configurable
 * duration and is rendered as billboarded text in world space.
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
        // Replace any existing entry for the same target.
        ACTIVE.removeIf(m -> m.target == target);
        ACTIVE.add(new Mogged(target, expiresAt));

        if (config.playSound) {
            try {
                MinecraftClient client = MinecraftClient.getInstance();
                client.getSoundManager().play(PositionedSoundInstance.master(
                        SoundEvents.ENTITY_WARDEN_SONIC_BOOM,
                        Math.max(0.5f, Math.min(2.0f, config.pitch)),
                        Math.max(0.0f, Math.min(2.0f, config.volume))));
            } catch (Throwable ignored) {
            }
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
        // Place the banner a bit above the player's head / nametag.
        Vec3d pos = lerped.add(0.0, m.target.getHeight() + 0.85, 0.0);

        MatrixStack matrices = context.matrices();
        matrices.push();
        matrices.translate(
                (float) (pos.x - cameraPos.x),
                (float) (pos.y - cameraPos.y),
                (float) (pos.z - cameraPos.z));
        matrices.multiply(camera.getRotation());
        // 0.025 is the standard nametag world scale in vanilla; negate X/Y so text faces the camera.
        float s = 0.025f * Math.max(0.4f, config.bannerScale);
        matrices.scale(-s, -s, s);

        Text text = Text.literal("MOGGED").styled(style -> style.withBold(true));
        int textWidth = tr.getWidth(text);
        float x = -textWidth / 2.0f;
        float y = -tr.fontHeight / 2.0f;

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        VertexConsumerProvider consumers = context.consumers();
        if (consumers == null) {
            matrices.pop();
            return;
        }

        // The TextRenderer.draw overload that takes a backgroundColor paints a
        // single colored rect behind the glyphs — perfect for the meme banner.
        int textColor = 0xFFFF1F1F;        // bright red glyphs
        int backgroundColor = 0xFF000000;  // solid black plate
        int fullBrightLight = 0xF000F0;
        tr.draw(text, x, y, textColor, false, matrix, consumers,
                TextRenderer.TextLayerType.SEE_THROUGH, backgroundColor, fullBrightLight);

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
