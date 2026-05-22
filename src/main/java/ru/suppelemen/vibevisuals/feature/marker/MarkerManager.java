package ru.suppelemen.vibevisuals.feature.marker;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MarkerManager {
    private static final List<Marker> MARKERS = new ArrayList<>();
    private static int nextId = 1;

    private MarkerManager() {
    }

    public static List<Marker> markers() {
        return Collections.unmodifiableList(MARKERS);
    }

    public static void addAtCrosshair(MinecraftClient client) {
        if (client.player == null) {
            return;
        }

        Vec3d pos = client.player.getEyePos();
        HitResult hit = client.crosshairTarget;
        if (hit != null && hit.getType() != HitResult.Type.MISS) {
            pos = hit.getPos();
            if (hit instanceof EntityHitResult entityHit) {
                Entity entity = entityHit.getEntity();
                pos = new Vec3d(entity.getX(), entity.getY() + entity.getHeight() * 0.5, entity.getZ());
            }
        }

        VibeVisualsConfig.MarkersConfig config = VibeVisualsConfigManager.get().markers;
        while (MARKERS.size() >= config.maxMarkers) {
            MARKERS.remove(0);
        }
        MARKERS.add(new Marker("Marker " + nextId++, pos));
    }

    public static void removeLast() {
        if (!MARKERS.isEmpty()) {
            MARKERS.remove(MARKERS.size() - 1);
        }
    }

    public static void clear() {
        MARKERS.clear();
    }

    public static void render(WorldRenderContext context) {
        VibeVisualsConfig.MarkersConfig config = VibeVisualsConfigManager.get().markers;
        if (!config.enabled || MARKERS.isEmpty()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        Vec3d camera = client.gameRenderer.getCamera().getCameraPos();
        MatrixStack.Entry entry = context.matrices().peek();
        Matrix4f matrix = entry.getPositionMatrix();
        VertexConsumer consumer = context.consumers().getBuffer(RenderLayers.linesTranslucent());
        for (Marker marker : MARKERS) {
            drawMarker(consumer, entry, matrix, camera, marker.pos, config.radius, config.color, config.lineWidth);
        }
    }

    private static void drawMarker(VertexConsumer consumer, MatrixStack.Entry entry, Matrix4f matrix, Vec3d camera, Vec3d pos, float radius, int color, float lineWidth) {
        int segments = 32;
        Vec3d top = pos.add(0.0, radius * 2.2, 0.0);
        Vec3d bottom = pos.add(0.0, -radius * 0.4, 0.0);
        drawLine(consumer, entry, matrix, camera, top, bottom, color, 220, lineWidth);

        Vec3d previous = pos.add(radius, 0.0, 0.0);
        for (int index = 1; index <= segments; index++) {
            double angle = Math.PI * 2.0 * index / segments;
            Vec3d next = pos.add(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius);
            drawLine(consumer, entry, matrix, camera, previous, next, color, 190, lineWidth);
            previous = next;
        }

        drawLine(consumer, entry, matrix, camera, pos.add(-radius, 0.0, 0.0), pos.add(radius, 0.0, 0.0), color, 210, lineWidth);
        drawLine(consumer, entry, matrix, camera, pos.add(0.0, 0.0, -radius), pos.add(0.0, 0.0, radius), color, 210, lineWidth);
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

    public record Marker(String name, Vec3d pos) {
    }
}
