package ru.suppelemen.vibevisuals.feature.marker;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.joml.Matrix4f;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Xaero's-Minimap-style waypoints. Each marker shows a coloured square with its
 * initial, the name and the distance, billboarded toward the camera, visible
 * through walls, and kept at a roughly constant on-screen size by scaling with
 * distance. Optional beacon beam.
 */
public final class MarkerManager {
    private static final List<Marker> MARKERS = new ArrayList<>();
    private static int nextId = 1;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE =
            FabricLoader.getInstance().getConfigDir().resolve("vibevisuals/markers.json");

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
        MARKERS.add(new Marker("Marker " + nextId++, pos, config.color));
        save();
    }

    public static void removeLast() {
        if (!MARKERS.isEmpty()) {
            MARKERS.remove(MARKERS.size() - 1);
            save();
        }
    }

    public static void clear() {
        MARKERS.clear();
        save();
    }

    // ---- persistence ----

    /** Serializable form of a marker. */
    private static final class Entry {
        String name;
        double x, y, z;
        int color;
    }

    public static void load() {
        MARKERS.clear();
        try {
            if (!Files.exists(FILE)) {
                return;
            }
            String json = Files.readString(FILE);
            Entry[] entries = GSON.fromJson(json, Entry[].class);
            if (entries != null) {
                for (Entry e : entries) {
                    if (e == null || e.name == null) {
                        continue;
                    }
                    MARKERS.add(new Marker(e.name, new Vec3d(e.x, e.y, e.z), e.color));
                }
            }
        } catch (Exception ignored) {
        }
        nextId = MARKERS.size() + 1;
    }

    private static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            List<Entry> out = new ArrayList<>();
            for (Marker m : MARKERS) {
                Entry e = new Entry();
                e.name = m.name();
                e.x = m.pos().x;
                e.y = m.pos().y;
                e.z = m.pos().z;
                e.color = m.color();
                out.add(e);
            }
            Files.writeString(FILE, GSON.toJson(out));
        } catch (Exception ignored) {
        }
    }

    public static void render(WorldRenderContext context) {
        VibeVisualsConfig.MarkersConfig config = VibeVisualsConfigManager.get().markers;
        if (!config.enabled || MARKERS.isEmpty()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        VertexConsumerProvider consumers = context.consumers();
        if (consumers == null) {
            return;
        }
        Camera cam = client.gameRenderer.getCamera();
        Vec3d camPos = cam.getCameraPos();
        TextRenderer tr = client.textRenderer;
        MatrixStack matrices = context.matrices();

        if (config.beam) {
            VertexConsumer lines = consumers.getBuffer(RenderLayers.linesTranslucent());
            Matrix4f m = matrices.peek().getPositionMatrix();
            MatrixStack.Entry entry = matrices.peek();
            for (Marker marker : MARKERS) {
                drawBeam(lines, entry, m, camPos, marker, config);
            }
        }

        for (Marker marker : MARKERS) {
            drawLabel(tr, matrices, consumers, cam, camPos, marker, config);
        }
    }

    private static void drawLabel(TextRenderer tr, MatrixStack matrices, VertexConsumerProvider consumers,
                                  Camera cam, Vec3d camPos, Marker marker, VibeVisualsConfig.MarkersConfig config) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Vec3d rel = marker.pos().subtract(camPos);
        double dist = rel.length();

        // Constant on-screen size (Xaero-style): the billboard IS perspective-
        // divided, so scaling the world size proportionally to distance cancels
        // it out. Clamp the distance so very close / very far markers don't grow
        // or shrink past sensible limits.
        Vec3d fwd = mc.player != null ? mc.player.getRotationVector() : new Vec3d(0, 0, 1);
        if (rel.dotProduct(fwd) < 0.1) {
            return; // behind the camera
        }
        double scaleDist = Math.max(6.0, Math.min(160.0, dist));
        float base = (float) (scaleDist * 0.0025);
        float si = base * config.iconScale;
        float st = base * config.textScale;

        // Only show the name/distance when the crosshair is aimed at the marker.
        boolean focused = true;
        if (config.expandOnLook && mc.player != null) {
            Vec3d to = rel.normalize();
            focused = fwd.dotProduct(to) > 0.9986; // ~3°
        }

        float op = config.opacity;
        int light = 0xF000F0;
        int markerRgb = marker.color() & 0x00FFFFFF;
        int iconBg = withOpacity(0xFF000000 | markerRgb, op);
        int iconText = withOpacity(contrastColor(markerRgb), op);
        int nameColor = withOpacity(0xFFFFFFFF, op);
        int distColor = withOpacity(0xFFCFE3FF, op);
        int plateBg = withOpacity(0x90000000, op);
        String initial = marker.name().isBlank() ? "?" : marker.name().substring(0, 1).toUpperCase();

        matrices.push();
        matrices.translate(marker.pos().x - camPos.x, marker.pos().y - camPos.y, marker.pos().z - camPos.z);
        matrices.multiply(cam.getRotation());

        // Icon — always shown, centered on the marker.
        matrices.push();
        matrices.scale(si, -si, si);
        Matrix4f im = matrices.peek().getPositionMatrix();
        drawCentered(tr, im, consumers, initial, -tr.fontHeight / 2, iconText, iconBg, config.throughWalls, light);
        matrices.pop();

        // Name + distance — only when focused.
        if (focused && (config.showName || config.showDistance)) {
            matrices.push();
            matrices.scale(st, -st, st);
            Matrix4f lm = matrices.peek().getPositionMatrix();
            float y = (tr.fontHeight * 0.5f * si / st) + 2; // just below the icon
            if (config.showName) {
                drawCentered(tr, lm, consumers, marker.name(), Math.round(y), nameColor, plateBg, config.throughWalls, light);
                y += tr.fontHeight + 1;
            }
            if (config.showDistance) {
                drawCentered(tr, lm, consumers, (int) Math.round(dist) + "m", Math.round(y), distColor, plateBg, config.throughWalls, light);
            }
            matrices.pop();
        }
        matrices.pop();
    }

    private static int withOpacity(int argb, float op) {
        int a = Math.round(((argb >>> 24) & 0xFF) * op);
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    private static void drawCentered(TextRenderer tr, Matrix4f matrix, VertexConsumerProvider consumers,
                                     String text, int y, int color, int bg, boolean throughWalls, int light) {
        float x = -tr.getWidth(text) / 2.0f;
        if (throughWalls) {
            tr.draw(text, x, y, color, false, matrix, consumers,
                    TextRenderer.TextLayerType.SEE_THROUGH, bg, light);
        }
        tr.draw(text, x, y, color, false, matrix, consumers,
                TextRenderer.TextLayerType.NORMAL, throughWalls ? 0 : bg, light);
    }

    private static void drawBeam(VertexConsumer lines, MatrixStack.Entry entry, Matrix4f matrix,
                                 Vec3d cam, Marker marker, VibeVisualsConfig.MarkersConfig config) {
        Vec3d base = marker.pos();
        Vec3d top = base.add(0.0, config.beamHeight, 0.0);
        int color = marker.color();
        int red = (color >> 16) & 0xFF, green = (color >> 8) & 0xFF, blue = color & 0xFF;
        lines.vertex(matrix, (float) (base.x - cam.x), (float) (base.y - cam.y), (float) (base.z - cam.z))
                .color(red, green, blue, 200).normal(entry, 0f, 1f, 0f).lineWidth(config.lineWidth);
        lines.vertex(matrix, (float) (top.x - cam.x), (float) (top.y - cam.y), (float) (top.z - cam.z))
                .color(red, green, blue, 40).normal(entry, 0f, 1f, 0f).lineWidth(config.lineWidth);
    }

    private static int contrastColor(int rgb) {
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
        double lum = (0.299 * r + 0.587 * g + 0.114 * b);
        return lum > 140 ? 0xFF101010 : 0xFFFFFFFF;
    }

    public record Marker(String name, Vec3d pos, int color) {
    }
}
