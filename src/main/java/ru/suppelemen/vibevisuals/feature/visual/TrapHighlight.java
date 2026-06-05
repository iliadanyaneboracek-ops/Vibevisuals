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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Block selection highlight. Marks are stored per-block, so adjacent blocks
 * merge into a single continuous outline (shared interior edges are not drawn).
 * Each block remembers the colour it was created with.
 *
 * <p>Controls: HOLD the select key and drag to fill an area; TAP it on a block
 * to toggle that single block (tap a selected block to unselect just it). The
 * remove key clears the block under the crosshair, or everything if you're not
 * aiming at a selected block.
 */
public final class TrapHighlight {

    // block -> colour (insertion-ordered for predictable rebuilds)
    private static final Map<BlockPos, Integer> CELLS = new LinkedHashMap<>();
    private static final int MAX_CELLS = 20000;

    // Cached boundary edges, rebuilt only when the selection changes.
    private record Edge(double ax, double ay, double az, double bx, double by, double bz, int color) {}
    private static final List<Edge> EDGES = new ArrayList<>();
    private static boolean dirty;

    // Area-drag state.
    private static boolean dragging;
    private static BlockPos dragStart;
    private static BlockPos dragCurrent;
    private static int lastDimHash;

    private TrapHighlight() {
    }

    // ---- input ----

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
        if (a == null) a = b;
        if (b == null) b = a;
        if (a == null) return;

        int color = VibeVisualsConfigManager.get().trapHighlight.nextColor;

        // Single-block tap toggles that block.
        if (a.equals(b)) {
            BlockPos p = a.toImmutable();
            if (CELLS.remove(p) == null) {
                CELLS.put(p, color);
            }
            dirty = true;
            return;
        }

        // Area drag adds every block in the cuboid.
        int minX = Math.min(a.getX(), b.getX()), maxX = Math.max(a.getX(), b.getX());
        int minY = Math.min(a.getY(), b.getY()), maxY = Math.max(a.getY(), b.getY());
        int minZ = Math.min(a.getZ(), b.getZ()), maxZ = Math.max(a.getZ(), b.getZ());
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (CELLS.size() >= MAX_CELLS) {
                        dirty = true;
                        return;
                    }
                    CELLS.put(new BlockPos(x, y, z), color);
                }
            }
        }
        dirty = true;
    }

    /** Remove the block under the crosshair; if none selected there, clear all. */
    public static void removeUnderCrosshair(MinecraftClient client) {
        BlockPos looked = lookedBlock(client);
        if (looked != null && CELLS.remove(looked) != null) {
            dirty = true;
            return;
        }
        if (!CELLS.isEmpty()) {
            CELLS.clear();
            dirty = true;
        }
    }

    public static void clearMarks() {
        CELLS.clear();
        dragging = false;
        dragStart = null;
        dragCurrent = null;
        dirty = true;
    }

    private static BlockPos lookedBlock(MinecraftClient client) {
        HitResult hit = client.crosshairTarget;
        if (hit instanceof BlockHitResult bhr && hit.getType() == HitResult.Type.BLOCK) {
            return bhr.getBlockPos();
        }
        return null;
    }

    // ---- tick (dimension-change cleanup) ----

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

    // ---- merged boundary-edge build ----

    private static boolean has(int x, int y, int z) {
        return CELLS.containsKey(new BlockPos(x, y, z));
    }

    private static void rebuildEdges() {
        EDGES.clear();
        Set<Long> done = new HashSet<>();
        for (Map.Entry<BlockPos, Integer> e : CELLS.entrySet()) {
            BlockPos p = e.getKey();
            int color = e.getValue();
            int x = p.getX(), y = p.getY(), z = p.getZ();
            for (int dy = 0; dy <= 1; dy++)
                for (int dz = 0; dz <= 1; dz++)
                    tryEdge(done, 0, x, y + dy, z + dz, color);
            for (int dx = 0; dx <= 1; dx++)
                for (int dz = 0; dz <= 1; dz++)
                    tryEdge(done, 1, x + dx, y, z + dz, color);
            for (int dx = 0; dx <= 1; dx++)
                for (int dy = 0; dy <= 1; dy++)
                    tryEdge(done, 2, x + dx, y + dy, z, color);
        }
        dirty = false;
    }

    /** axis: 0=X,1=Y,2=Z. (gx,gy,gz) is the lower grid endpoint of the edge. */
    private static void tryEdge(Set<Long> done, int axis, int gx, int gy, int gz, int color) {
        long key = (((long) axis) << 60) ^ pack(gx, gy, gz);
        if (!done.add(key)) {
            return;
        }
        boolean c00, c01, c10, c11;
        if (axis == 0) {            // edge along X → vary Y,Z
            c00 = has(gx, gy - 1, gz - 1); c01 = has(gx, gy - 1, gz);
            c10 = has(gx, gy, gz - 1);     c11 = has(gx, gy, gz);
        } else if (axis == 1) {     // edge along Y → vary X,Z
            c00 = has(gx - 1, gy, gz - 1); c01 = has(gx - 1, gy, gz);
            c10 = has(gx, gy, gz - 1);     c11 = has(gx, gy, gz);
        } else {                    // edge along Z → vary X,Y
            c00 = has(gx - 1, gy - 1, gz); c01 = has(gx - 1, gy, gz);
            c10 = has(gx, gy - 1, gz);     c11 = has(gx, gy, gz);
        }
        int count = (c00 ? 1 : 0) + (c01 ? 1 : 0) + (c10 ? 1 : 0) + (c11 ? 1 : 0);
        boolean draw;
        if (count == 1 || count == 3) {
            draw = true;
        } else if (count == 2) {
            draw = (c00 && c11) || (c01 && c10); // diagonal corner only
        } else {
            draw = false;
        }
        if (!draw) {
            return;
        }
        double ax = gx, ay = gy, az = gz, bx = gx, by = gy, bz = gz;
        if (axis == 0) bx = gx + 1;
        else if (axis == 1) by = gy + 1;
        else bz = gz + 1;
        EDGES.add(new Edge(ax, ay, az, bx, by, bz, color));
    }

    private static long pack(int x, int y, int z) {
        return ((long) (x & 0xFFFFF)) | (((long) (y & 0xFFFFF)) << 20) | (((long) (z & 0xFFFFF)) << 40);
    }

    // ---- render ----

    public static void render(WorldRenderContext context) {
        VibeVisualsConfig.TrapHighlightConfig c = VibeVisualsConfigManager.get().trapHighlight;
        if (!c.enabled) {
            return;
        }
        if (CELLS.isEmpty() && !dragging) {
            return;
        }
        if (dirty) {
            rebuildEdges();
        }

        MinecraftClient client = MinecraftClient.getInstance();
        Vec3d cam = client.gameRenderer.getCamera().getCameraPos();
        Matrix4f matrix = context.matrices().peek().getPositionMatrix();
        VertexConsumer quads = context.consumers().getBuffer(RenderLayers.debugQuads());

        for (Edge e : EDGES) {
            int col = c.rainbow ? rainbow(c, e.ax(), e.ay(), e.az()) : e.color();
            beam(quads, matrix, cam, e.ax(), e.ay(), e.az(), e.bx(), e.by(), e.bz(), col, c.thickness);
        }

        if (dragging && dragStart != null) {
            BlockPos b = dragCurrent != null ? dragCurrent : dragStart;
            int minX = Math.min(dragStart.getX(), b.getX()), maxX = Math.max(dragStart.getX(), b.getX());
            int minY = Math.min(dragStart.getY(), b.getY()), maxY = Math.max(dragStart.getY(), b.getY());
            int minZ = Math.min(dragStart.getZ(), b.getZ()), maxZ = Math.max(dragStart.getZ(), b.getZ());
            int col = (0xB0 << 24) | (c.nextColor & 0x00FFFFFF);
            previewBox(quads, matrix, cam, minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1, col, c.thickness);
        }
    }

    private static void previewBox(VertexConsumer q, Matrix4f matrix, Vec3d cam,
                                   double x0, double y0, double z0, double x1, double y1, double z1,
                                   int col, float t) {
        beam(q, matrix, cam, x0, y0, z0, x1, y0, z0, col, t);
        beam(q, matrix, cam, x1, y0, z0, x1, y0, z1, col, t);
        beam(q, matrix, cam, x1, y0, z1, x0, y0, z1, col, t);
        beam(q, matrix, cam, x0, y0, z1, x0, y0, z0, col, t);
        beam(q, matrix, cam, x0, y1, z0, x1, y1, z0, col, t);
        beam(q, matrix, cam, x1, y1, z0, x1, y1, z1, col, t);
        beam(q, matrix, cam, x1, y1, z1, x0, y1, z1, col, t);
        beam(q, matrix, cam, x0, y1, z1, x0, y1, z0, col, t);
        beam(q, matrix, cam, x0, y0, z0, x0, y1, z0, col, t);
        beam(q, matrix, cam, x1, y0, z0, x1, y1, z0, col, t);
        beam(q, matrix, cam, x1, y0, z1, x1, y1, z1, col, t);
        beam(q, matrix, cam, x0, y0, z1, x0, y1, z1, col, t);
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

        double ux, uy, uz, vx, vy, vz;
        if (Math.abs(bx - ax) > 1e-6) {        // along X
            ux = 0; uy = h; uz = 0; vx = 0; vy = 0; vz = h;
        } else if (Math.abs(by - ay) > 1e-6) { // along Y
            ux = h; uy = 0; uz = 0; vx = 0; vy = 0; vz = h;
        } else {                               // along Z
            ux = h; uy = 0; uz = 0; vx = 0; vy = h; vz = 0;
        }

        double[][] off = {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}};
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

    private static int rainbow(VibeVisualsConfig.TrapHighlightConfig c, double x, double y, double z) {
        double period = 3000.0 / Math.max(0.05f, c.rainbowSpeed);
        double phase = (x + y + z) * 0.04;
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
