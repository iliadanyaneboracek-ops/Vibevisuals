package ru.suppelemen.vibevisuals.feature.marker;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MarkerManager {
    private static final List<Marker> MARKERS = new ArrayList<>();
    private static final Pattern COORD_PATTERN = Pattern.compile("(-?\\d{1,7})\\D{1,6}(-?\\d{1,4})\\D{1,6}(-?\\d{1,7})");
    private static int nextId = 1;

    // captured each world-render frame, consumed by the HUD label pass
    private static Matrix4f projectionMatrix;
    private static Matrix4f modelViewMatrix;
    private static Vec3d cameraPos;

    private MarkerManager() {
    }

    public static List<Marker> markers() {
        return Collections.unmodifiableList(MARKERS);
    }

    public static int count() {
        return MARKERS.size();
    }

    public static void addManualAtCrosshair(MinecraftClient client) {
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

        add(client, pos, MarkerType.MANUAL, "Marker " + nextId++, VibeVisualsConfigManager.get().markers.color);
    }

    public static void addManualAtSelf(MinecraftClient client) {
        if (client.player == null) {
            return;
        }
        add(client, playerPos(client), MarkerType.MANUAL, "Marker " + nextId++, VibeVisualsConfigManager.get().markers.color);
    }

    public static void addDeath(MinecraftClient client) {
        if (client.player == null) {
            return;
        }
        VibeVisualsConfig.DeathMarkerConfig config = VibeVisualsConfigManager.get().deathMarker;
        if (config.keepOnlyLast) {
            MARKERS.removeIf(marker -> marker.type == MarkerType.DEATH);
        }
        add(client, playerPos(client), MarkerType.DEATH, "Death", config.color);
    }

    public static void onChatMessage(String message) {
        if (message == null || message.isBlank()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }

        VibeVisualsConfig.ChatEventConfig config = VibeVisualsConfigManager.get().chatEvents;
        if (!config.enabled || config.keywords == null || config.keywords.isEmpty()) {
            return;
        }

        String lower = message.toLowerCase(Locale.ROOT);
        boolean matched = false;
        for (String keyword : config.keywords) {
            if (keyword != null && !keyword.isBlank() && lower.contains(keyword.toLowerCase(Locale.ROOT))) {
                matched = true;
                break;
            }
        }
        if (!matched) {
            return;
        }

        Vec3d pos = playerPos(client);
        if (config.useChatCoordinates) {
            Vec3d parsed = parseCoordinates(message);
            if (parsed != null) {
                pos = parsed;
            }
        }
        add(client, pos, MarkerType.EVENT, eventName(message), config.color);
    }

    private static String eventName(String message) {
        // collapse whitespace and trim to a short, readable title taken from the chat line
        String cleaned = message.replaceAll("\\s+", " ").trim();
        if (cleaned.isEmpty()) {
            return "Event";
        }
        int limit = 28;
        if (cleaned.length() > limit) {
            cleaned = cleaned.substring(0, limit).trim() + "…";
        }
        return cleaned;
    }

    private static Vec3d parseCoordinates(String message) {
        Matcher matcher = COORD_PATTERN.matcher(message);
        if (!matcher.find()) {
            return null;
        }
        try {
            double x = Double.parseDouble(matcher.group(1)) + 0.5;
            double y = Double.parseDouble(matcher.group(2));
            double z = Double.parseDouble(matcher.group(3)) + 0.5;
            return new Vec3d(x, y, z);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Vec3d playerPos(MinecraftClient client) {
        return new Vec3d(client.player.getX(), client.player.getY(), client.player.getZ());
    }

    private static void add(MinecraftClient client, Vec3d pos, MarkerType type, String name, int color) {
        VibeVisualsConfig.MarkersConfig config = VibeVisualsConfigManager.get().markers;
        String dimension = client.world == null ? "" : client.world.getRegistryKey().getValue().toString();
        while (MARKERS.size() >= config.maxMarkers && !MARKERS.isEmpty()) {
            MARKERS.remove(0);
        }
        MARKERS.add(new Marker(name, pos, type, color, dimension));
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
        if (client.world == null) {
            return;
        }
        String currentDimension = client.world.getRegistryKey().getValue().toString();

        Camera cameraInstance = client.gameRenderer.getCamera();
        Vec3d camera = cameraInstance.getCameraPos();
        double maxDistance = maxRenderDistance(client);
        double worldBottom = client.world.getBottomY();
        double worldTop = client.world.getBottomY() + client.world.getHeight();
        MatrixStack matrices = context.matrices();
        MatrixStack.Entry entry = matrices.peek();
        Matrix4f matrix = entry.getPositionMatrix();
        VertexConsumer consumer = context.consumers().getBuffer(RenderLayers.linesTranslucent());

        for (Marker marker : MARKERS) {
            if (config.onlyCurrentDimension && !marker.dimension.equals(currentDimension)) {
                continue;
            }
            double realDistance = Math.sqrt(camera.squaredDistanceTo(marker.pos));
            boolean clamped = realDistance > maxDistance;
            Vec3d renderPos = clamped ? clampToDistance(camera, marker.pos, maxDistance) : marker.pos;
            // skip the ground ring/cross when clamped: it would float in mid-air, only the beam keeps pointing
            drawMarker(consumer, entry, matrix, camera, renderPos, config.radius, marker.color, config.lineWidth, worldBottom, worldTop, !clamped);
        }

        // capture the modelview + camera so the HUD pass can project labels to screen space (reliable at any distance);
        // the projection matrix is supplied separately by GameRendererMixin
        if (config.showLabels) {
            modelViewMatrix = new Matrix4f(matrix);
            cameraPos = camera;
        }
    }

    public static void setProjectionMatrix(Matrix4f matrix) {
        projectionMatrix = new Matrix4f(matrix);
    }

    public static void renderLabels(DrawContext context) {
        VibeVisualsConfig.MarkersConfig config = VibeVisualsConfigManager.get().markers;
        if (!config.enabled || !config.showLabels || MARKERS.isEmpty()
                || projectionMatrix == null || modelViewMatrix == null || cameraPos == null) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return;
        }
        String currentDimension = client.world.getRegistryKey().getValue().toString();
        TextRenderer textRenderer = client.textRenderer;
        int screenW = context.getScaledWindowWidth();
        int screenH = context.getScaledWindowHeight();
        double maxDistance = maxRenderDistance(client);
        double worldBottom = client.world.getBottomY();
        double worldTop = client.world.getBottomY() + client.world.getHeight();

        for (Marker marker : MARKERS) {
            if (config.onlyCurrentDimension && !marker.dimension.equals(currentDimension)) {
                continue;
            }

            double realDistance = Math.sqrt(cameraPos.squaredDistanceTo(marker.pos));
            // anchor the label to the same beam the world pass draws (clamped when far), at eye height,
            // so the icon and distance sit still on the beam instead of drifting around the screen
            Vec3d basePos = realDistance > maxDistance ? clampToDistance(cameraPos, marker.pos, maxDistance) : marker.pos;
            double anchorY = Math.max(worldBottom + 1.0, Math.min(worldTop - 1.0, cameraPos.y));
            Vec3d anchor = new Vec3d(basePos.x, anchorY, basePos.z);
            float[] screen = projectToScreen(anchor, screenW, screenH);
            if (screen == null) {
                continue;
            }

            String title = iconFor(marker.type) + " " + marker.name;
            int titleColor = 0xFF000000 | (marker.color & 0x00FFFFFF);
            int x = Math.round(screen[0]);
            int y = Math.round(screen[1]);
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(title), x, y, titleColor);

            // distance label only once the marker is far enough to be worth navigating back to
            if (config.showDistance && realDistance > 50.0) {
                String distText = Math.round(realDistance) + "m";
                context.drawCenteredTextWithShadow(textRenderer, Text.literal(distText), x, y + 10, 0xFFD7DAE8);
            }
        }
    }

    private static float[] projectToScreen(Vec3d worldPos, int screenW, int screenH) {
        Vector4f pos = new Vector4f(
                (float) (worldPos.x - cameraPos.x),
                (float) (worldPos.y - cameraPos.y),
                (float) (worldPos.z - cameraPos.z),
                1.0f);
        pos.mul(modelViewMatrix);
        pos.mul(projectionMatrix);
        if (pos.w() <= 1.0e-4f) {
            return null; // behind the camera
        }
        float ndcX = pos.x() / pos.w();
        float ndcY = pos.y() / pos.w();
        float screenX = (ndcX * 0.5f + 0.5f) * screenW;
        float screenY = (1.0f - (ndcY * 0.5f + 0.5f)) * screenH;
        return new float[]{screenX, screenY};
    }

    private static double maxRenderDistance(MinecraftClient client) {
        int chunks = client.options.getViewDistance().getValue();
        return Math.max(48.0, chunks * 16.0 - 8.0);
    }

    private static Vec3d clampToDistance(Vec3d camera, Vec3d target, double maxDistance) {
        Vec3d direction = target.subtract(camera);
        double length = direction.length();
        if (length <= 1.0e-4) {
            return target;
        }
        return camera.add(direction.multiply(maxDistance / length));
    }

    private static void drawMarker(VertexConsumer consumer, MatrixStack.Entry entry, Matrix4f matrix, Vec3d camera, Vec3d pos, float radius, int color, float lineWidth, double worldBottom, double worldTop, boolean drawGround) {
        // full-height beacon beam from world bottom (bedrock) to the very top
        Vec3d top = new Vec3d(pos.x, worldTop, pos.z);
        Vec3d bottom = new Vec3d(pos.x, worldBottom, pos.z);
        drawLine(consumer, entry, matrix, camera, top, bottom, color, 255, lineWidth);

        if (!drawGround) {
            return;
        }

        int segments = 32;
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

    private static String iconFor(MarkerType type) {
        return switch (type) {
            case DEATH -> "☠";   // skull and crossbones
            case EVENT -> "★";   // star
            case MANUAL -> "⚑";  // flag
        };
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

    public enum MarkerType {
        MANUAL,
        DEATH,
        EVENT
    }

    public record Marker(String name, Vec3d pos, MarkerType type, int color, String dimension) {
    }
}
