package ru.suppelemen.vibevisuals.feature.visual;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Block selection highlight. You mark single blocks or whole areas and they get
 * a solid coloured 3D outline. Each mark stores its own colour (taken from the
 * config "nextColor" at creation), so changing the colour only affects future
 * marks. Tell traps / pulls apart by simply using different colours.
 *
 * <p>Controls: HOLD the select key and drag the crosshair to box an area; a
 * quick tap marks the single block you're looking at (tap again to unmark).
 * The remove key deletes whatever selection your crosshair is inside.
 * Fully client-side, no packets.
 */
public final class TrapHighlight {

    public record Selection(BlockPos min, BlockPos max, int color) {
        boolean contains(BlockPos p) {
            return p.getX() >= min.getX() && p.getX() <= max.getX()
                    && p.getY() >= min.getY() && p.getY() <= max.getY()
                    && p.getZ() >= min.getZ() && p.getZ() <= max.getZ();
        }
    }

    private static final List<Selection> SELECTIONS = new ArrayList<>();

    // Area-drag state.
    private static boolean dragging;
    private static BlockPos dragStart;
    private static BlockPos dragCurrent;
    private static int lastDimHash;

    private TrapHighlight() {
    }

    // ---- input ----

    /** Drive the select key each tick. {@code down} = key currently held. */
    public static void handleSelectKey(MinecraftClient client, boolean down) {
        BlockPos looked = lookedBlock(client);
        if (down) {
            if (!dragging) {
                dragging = true;
                dragStart = looked;
                dragCurrent = looked;
            } else if (looked != null) {
                dragCurrent = looked;
                if (dragStart == null) {
                    dragStart = looked;
                }
            }
        } else if (dragging) {
            dragging = false;
            commit(dragStart, dragCurrent);
            dragStart = null;
            dragCurrent = null;
        }
    }

    private static void commit(BlockPos a, BlockPos b) {
        if (a == null) {
            a = b;
        }
        if (b == null) {
            b = a;
        }
        if (a == null) {
            return;
        }
        BlockPos min = new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()));
        BlockPos max = new BlockPos(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()));

        // Single-block tap toggles: if that exact 1×1 mark exists, remove it.
        if (min.equals(max)) {
            for (int i = 0; i < SELECTIONS.size(); i++) {
                Selection s = SELECTIONS.get(i);
                if (s.min().equals(min) && s.max().equals(max)) {
                    SELECTIONS.remove(i);
                    return;
                }
            }
        }
        int color = VibeVisualsConfigManager.get().trapHighlight.nextColor;
        SELECTIONS.add(new Selection(min, max, color));
    }

    /** Remove whatever selection the crosshair is currently inside. */
    public static void removeUnderCrosshair(MinecraftClient client) {
        BlockPos looked = lookedBlock(client);
        if (looked == null) {
            return;
        }
        for (int i = SELECTIONS.size() - 1; i >= 0; i--) {
            if (SELECTIONS.get(i).contains(looked)) {
                SELECTIONS.remove(i);
                return;
            }
        }
    }

    public static void clearMarks() {
        SELECTIONS.clear();
        dragging = false;
        dragStart = null;
        dragCurrent = null;
    }

    private static BlockPos lookedBlock(MinecraftClient client) {
        HitResult hit = client.crosshairTarget;
        if (hit instanceof BlockHitResult bhr && hit.getType() == HitResult.Type.BLOCK) {
            return bhr.getBlockPos();
        }
        return null;
    }

    // ---- tick (just dimension-change cleanup) ----

    public static void tick(MinecraftClient client) {
        if (client.world == null) {
            return;
        }
        int dim = client.world.getRegistryKey().getValue().hashCode();
        if (dim != lastDimHash) {
            lastDimHash = dim;
            clearMarks();
        }
    }

    // ---- render ----

    public static void render(WorldRenderContext context) {
        VibeVisualsConfig.TrapHighlightConfig c = VibeVisualsConfigManager.get().trapHighlight;
        if (!c.enabled) {
            return;
        }
        if (SELECTIONS.isEmpty() && !dragging) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        Vec3d cam = client.gameRenderer.getCamera().getCameraPos();
        MatrixStack.Entry entry = context.matrices().peek();
        Matrix4f matrix = entry.getPositionMatrix();
        VertexConsumer quads = context.consumers().getBuffer(RenderLayers.debugQuads());

        for (Selection s : SELECTIONS) {
            int col = c.rainbow ? rainbow(c, s.min()) : s.color();
            drawBox(quads, matrix, cam, s.min(), s.max(), col, c.thickness);
        }

        // Live preview of the in-progress drag.
        if (dragging && dragStart != null) {
            BlockPos b = dragCurrent != null ? dragCurrent : dragStart;
            BlockPos min = new BlockPos(Math.min(dragStart.getX(), b.getX()), Math.min(dragStart.getY(), b.getY()), Math.min(dragStart.getZ(), b.getZ()));
            BlockPos max = new BlockPos(Math.max(dragStart.getX(), b.getX()), Math.max(dragStart.getY(), b.getY()), Math.max(dragStart.getZ(), b.getZ()));
            int col = c.rainbow ? rainbow(c, min) : c.nextColor;
            // dim the preview a touch
            col = (0xB0 << 24) | (col & 0x00FFFFFF);
            drawBox(quads, matrix, cam, min, max, col, c.thickness);
        }
    }

    private static void drawBox(VertexConsumer q, Matrix4f matrix, Vec3d cam,
                                BlockPos min, BlockPos max, int color, float t) {
        double e = 0.003;
        double x0 = min.getX() - e, y0 = min.getY() - e, z0 = min.getZ() - e;
        double x1 = max.getX() + 1 + e, y1 = max.getY() + 1 + e, z1 = max.getZ() + 1 + e;
        // 4 bottom + 4 top + 4 pillars, each a solid beam.
        beam(q, matrix, cam, x0, y0, z0, x1, y0, z0, color, t);
        beam(q, matrix, cam, x1, y0, z0, x1, y0, z1, color, t);
        beam(q, matrix, cam, x1, y0, z1, x0, y0, z1, color, t);
        beam(q, matrix, cam, x0, y0, z1, x0, y0, z0, color, t);
        beam(q, matrix, cam, x0, y1, z0, x1, y1, z0, color, t);
        beam(q, matrix, cam, x1, y1, z0, x1, y1, z1, color, t);
        beam(q, matrix, cam, x1, y1, z1, x0, y1, z1, color, t);
        beam(q, matrix, cam, x0, y1, z1, x0, y1, z0, color, t);
        beam(q, matrix, cam, x0, y0, z0, x0, y1, z0, color, t);
        beam(q, matrix, cam, x1, y0, z0, x1, y1, z0, color, t);
        beam(q, matrix, cam, x1, y0, z1, x1, y1, z1, color, t);
        beam(q, matrix, cam, x0, y0, z1, x0, y1, z1, color, t);
    }

    /** A solid thick edge: a thin rectangular prism (4 faces) along the edge. */
    private static void beam(VertexConsumer q, Matrix4f matrix, Vec3d cam,
                             double ax, double ay, double az, double bx, double by, double bz,
                             int color, float t) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (color >>> 24) & 0xFF;
        if (a == 0) a = 235;
        float h = t * 0.5f;

        // Perpendicular axes for the (axis-aligned) edge.
        double ux, uy, uz, vx, vy, vz;
        if (Math.abs(bx - ax) > 1e-6) {        // along X
            ux = 0; uy = h; uz = 0; vx = 0; vy = 0; vz = h;
        } else if (Math.abs(by - ay) > 1e-6) { // along Y
            ux = h; uy = 0; uz = 0; vx = 0; vy = 0; vz = h;
        } else {                               // along Z
            ux = h; uy = 0; uz = 0; vx = 0; vy = h; vz = 0;
        }

        // 4 corners of the cross-section at A and at B.
        double[][] off = {
                {-1, -1}, {1, -1}, {1, 1}, {-1, 1}
        };
        double[][] ca = new double[4][3];
        double[][] cb = new double[4][3];
        for (int i = 0; i < 4; i++) {
            double su = off[i][0], sv = off[i][1];
            ca[i][0] = ax + ux * su + vx * sv;
            ca[i][1] = ay + uy * su + vy * sv;
            ca[i][2] = az + uz * su + vz * sv;
            cb[i][0] = bx + ux * su + vx * sv;
            cb[i][1] = by + uy * su + vy * sv;
            cb[i][2] = bz + uz * su + vz * sv;
        }
        // 4 side faces (double-sided).
        for (int i = 0; i < 4; i++) {
            int j = (i + 1) % 4;
            face(q, matrix, cam, ca[i], ca[j], cb[j], cb[i], r, g, b, a);
            face(q, matrix, cam, cb[i], cb[j], ca[j], ca[i], r, g, b, a);
        }
    }

    private static void face(VertexConsumer q, Matrix4f matrix, Vec3d cam,
                             double[] p1, double[] p2, double[] p3, double[] p4,
                             int r, int g, int b, int a) {
        vert(q, matrix, cam, p1, r, g, b, a);
        vert(q, matrix, cam, p2, r, g, b, a);
        vert(q, matrix, cam, p3, r, g, b, a);
        vert(q, matrix, cam, p4, r, g, b, a);
    }

    private static void vert(VertexConsumer q, Matrix4f matrix, Vec3d cam, double[] p,
                             int r, int g, int b, int a) {
        q.vertex(matrix, (float) (p[0] - cam.x), (float) (p[1] - cam.y), (float) (p[2] - cam.z))
                .color(r, g, b, a);
    }

    private static int rainbow(VibeVisualsConfig.TrapHighlightConfig c, BlockPos p) {
        double period = 3000.0 / Math.max(0.05f, c.rainbowSpeed);
        double phase = (p.getX() + p.getY() + p.getZ()) * 0.04;
        float hue = (float) (((System.currentTimeMillis() / period) + phase) % 1.0);
        if (hue < 0) hue += 1f;
        return 0xFF000000 | hsvToRgb(hue);
    }

    private static int hsvToRgb(float h) {
        float r = 0, g = 0, b = 0;
        int i = (int) (h * 6f) % 6;
        float f = h * 6f - (float) Math.floor(h * 6f);
        float q = 1f - f;
        switch (i) {
            case 0 -> { r = 1; g = f; b = 0; }
            case 1 -> { r = q; g = 1; b = 0; }
            case 2 -> { r = 0; g = 1; b = f; }
            case 3 -> { r = 0; g = q; b = 1; }
            case 4 -> { r = f; g = 0; b = 1; }
            default -> { r = 1; g = 0; b = q; }
        }
        return (Math.round(r * 255) << 16) | (Math.round(g * 255) << 8) | Math.round(b * 255);
    }
}
