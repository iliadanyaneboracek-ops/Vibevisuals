package ru.suppelemen.vibevisuals.feature.visual;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.WindChargeEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.BowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class ProjectilePrediction {
    private static final List<List<Vec3d>> PREDICTION_LINES = new ArrayList<>();
    private static final List<Vec3d> MARKERS = new ArrayList<>();
    private static final List<RadiusMarker> RADIUS_MARKERS = new ArrayList<>();
    private static final Map<Integer, ArrayDeque<TrailPoint>> TRAILS = new HashMap<>();
    private static int tickCounter;
    private static long renderTick;

    private ProjectilePrediction() {
    }

    public static void tick(MinecraftClient client) {
        VibeVisualsConfig.ProjectilePredictionConfig config = VibeVisualsConfigManager.get().projectilePrediction;
        if (!config.enabled || client.player == null || client.world == null) {
            PREDICTION_LINES.clear();
            MARKERS.clear();
            RADIUS_MARKERS.clear();
            TRAILS.clear();
            return;
        }

        renderTick++;
        updateTrails(client, config);
        tickCounter++;
        if (tickCounter < config.spawnIntervalTicks) {
            return;
        }
        tickCounter = 0;

        PREDICTION_LINES.clear();
        MARKERS.clear();
        RADIUS_MARKERS.clear();
        ItemStack stack = client.player.getMainHandStack();
        PredictionType type = typeFor(stack);
        if (type == PredictionType.NONE) {
            stack = client.player.getOffHandStack();
            type = typeFor(stack);
        }

        if (type == PredictionType.NONE) {
            return;
        }

        Vec3d origin = client.player.getEyePos();
        Vec3d direction = client.player.getRotationVec(1.0f).normalize();
        if (type == PredictionType.CROSSBOW && isMultishot(stack)) {
            addTrace(client, origin, initialVelocity(client, direction.rotateY((float) Math.toRadians(-config.multishotAngle)), config.crossbowVelocity), gravityFor(type, config), 0.0f, config);
            addTrace(client, origin, initialVelocity(client, direction, config.crossbowVelocity), gravityFor(type, config), 0.0f, config);
            addTrace(client, origin, initialVelocity(client, direction.rotateY((float) Math.toRadians(config.multishotAngle)), config.crossbowVelocity), gravityFor(type, config), 0.0f, config);
            return;
        }

        addTrace(client, origin, initialVelocity(client, direction.rotateX(pitchOffset(type)), velocityFor(client, stack, type, config)), gravityFor(type, config), radiusFor(stack, type, config), config);
    }

    public static void render(WorldRenderContext context) {
        VibeVisualsConfig.ProjectilePredictionConfig config = VibeVisualsConfigManager.get().projectilePrediction;
        if (!config.enabled || (PREDICTION_LINES.isEmpty() && TRAILS.isEmpty())) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        Vec3d camera = client.gameRenderer.getCamera().getCameraPos();
        MatrixStack.Entry entry = context.matrices().peek();
        Matrix4f matrix = entry.getPositionMatrix();
        VertexConsumer consumer = context.consumers().getBuffer(RenderLayers.linesTranslucent());
        int color = config.color;
        for (List<Vec3d> line : PREDICTION_LINES) {
            drawPolyline(consumer, entry, matrix, camera, line, color, 210, config.lineWidth);
        }

        for (Vec3d marker : MARKERS) {
            drawMarker(consumer, entry, matrix, camera, marker, config.markerSize, config.markerColor, 230, config.lineWidth);
        }

        for (RadiusMarker radiusMarker : RADIUS_MARKERS) {
            drawCircle(consumer, entry, matrix, camera, radiusMarker.center, radiusMarker.radius, config.markerColor, 150, config.lineWidth);
        }

        for (ArrayDeque<TrailPoint> trail : TRAILS.values()) {
            drawTrail(consumer, entry, matrix, camera, trail, color, config);
        }
    }

    private static void addTrace(MinecraftClient client, Vec3d origin, Vec3d velocity, float gravity, float effectRadius, VibeVisualsConfig.ProjectilePredictionConfig config) {
        List<Vec3d> line = new ArrayList<>();
        line.add(origin);
        Vec3d previous = origin;
        Vec3d last = origin;
        Vec3d position = origin;
        Vec3d motion = velocity;
        int maxTicks = config.points * config.pointStepTicks;
        for (int tick = 1; tick <= maxTicks; tick++) {
            Vec3d next = position.add(motion);
            HitResult hit = client.world.raycast(new RaycastContext(previous, next, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, client.player));
            if (hit.getType() != HitResult.Type.MISS) {
                last = hit.getPos();
                line.add(last);
                PREDICTION_LINES.add(line);
                MARKERS.add(last);
                if (effectRadius > 0.0f) {
                    RADIUS_MARKERS.add(new RadiusMarker(last, effectRadius));
                }
                return;
            }

            if (tick % config.pointStepTicks == 0) {
                line.add(next);
            }
            previous = next;
            position = next;
            motion = motion.multiply(0.99).add(0.0, -gravity, 0.0);
            last = next;
        }

        PREDICTION_LINES.add(line);
        MARKERS.add(last);
        if (effectRadius > 0.0f) {
            RADIUS_MARKERS.add(new RadiusMarker(last, effectRadius));
        }
    }

    private static void updateTrails(MinecraftClient client, VibeVisualsConfig.ProjectilePredictionConfig config) {
        for (Entity entity : client.world.getEntities()) {
            if (!isTrackedProjectile(entity)) {
                continue;
            }

            ArrayDeque<TrailPoint> points = TRAILS.computeIfAbsent(entity.getId(), ignored -> new ArrayDeque<>());
            Vec3d pos = entity.getLerpedPos(1.0f);
            if (points.isEmpty() || points.getLast().pos.distanceTo(pos) > 0.08) {
                points.addLast(new TrailPoint(pos, renderTick));
            }
        }

        Iterator<Map.Entry<Integer, ArrayDeque<TrailPoint>>> iterator = TRAILS.entrySet().iterator();
        while (iterator.hasNext()) {
            ArrayDeque<TrailPoint> points = iterator.next().getValue();
            while (!points.isEmpty() && renderTick - points.getFirst().tick > config.trailTicks) {
                points.removeFirst();
            }
            if (points.size() < 2) {
                iterator.remove();
            }
        }
    }

    private static boolean isTrackedProjectile(Entity entity) {
        return entity instanceof EnderPearlEntity
                || entity instanceof ExperienceBottleEntity
                || entity instanceof PotionEntity
                || entity instanceof WindChargeEntity
                || entity instanceof TridentEntity
                || entity instanceof PersistentProjectileEntity;
    }

    private static void drawPolyline(VertexConsumer consumer, MatrixStack.Entry entry, Matrix4f matrix, Vec3d camera, List<Vec3d> points, int color, int alpha, float width) {
        for (int index = 1; index < points.size(); index++) {
            drawLine(consumer, entry, matrix, camera, points.get(index - 1), points.get(index), color, alpha, width);
        }
    }

    private static void drawTrail(VertexConsumer consumer, MatrixStack.Entry entry, Matrix4f matrix, Vec3d camera, ArrayDeque<TrailPoint> trail, int color, VibeVisualsConfig.ProjectilePredictionConfig config) {
        TrailPoint previous = null;
        for (TrailPoint point : trail) {
            if (previous != null) {
                float age = (renderTick - point.tick) / (float) config.trailTicks;
                int alpha = Math.max(0, Math.min(190, Math.round((1.0f - age) * 190.0f)));
                drawLine(consumer, entry, matrix, camera, previous.pos, point.pos, color, alpha, config.lineWidth);
            }
            previous = point;
        }
    }

    private static void drawMarker(VertexConsumer consumer, MatrixStack.Entry entry, Matrix4f matrix, Vec3d camera, Vec3d center, float size, int color, int alpha, float width) {
        drawLine(consumer, entry, matrix, camera, center.add(-size, 0.0, 0.0), center.add(size, 0.0, 0.0), color, alpha, width);
        drawLine(consumer, entry, matrix, camera, center.add(0.0, -size, 0.0), center.add(0.0, size, 0.0), color, alpha, width);
        drawLine(consumer, entry, matrix, camera, center.add(0.0, 0.0, -size), center.add(0.0, 0.0, size), color, alpha, width);
    }

    private static void drawCircle(VertexConsumer consumer, MatrixStack.Entry entry, Matrix4f matrix, Vec3d camera, Vec3d center, float radius, int color, int alpha, float width) {
        int segments = 48;
        Vec3d previous = center.add(radius, 0.03, 0.0);
        for (int index = 1; index <= segments; index++) {
            double angle = (Math.PI * 2.0 * index) / segments;
            Vec3d next = center.add(Math.cos(angle) * radius, 0.03, Math.sin(angle) * radius);
            drawLine(consumer, entry, matrix, camera, previous, next, color, alpha, width);
            previous = next;
        }
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

    private static PredictionType typeFor(ItemStack stack) {
        Item item = stack.getItem();
        if (item == Items.BOW) {
            return PredictionType.BOW;
        }
        if (item == Items.CROSSBOW && CrossbowItem.isCharged(stack)) {
            return PredictionType.CROSSBOW;
        }
        if (item == Items.ENDER_PEARL || item == Items.SNOWBALL || item == Items.EGG) {
            return PredictionType.PEARL;
        }
        if (item == Items.EXPERIENCE_BOTTLE) {
            return PredictionType.EXPERIENCE_BOTTLE;
        }
        if (item == Items.SPLASH_POTION || item == Items.LINGERING_POTION) {
            return PredictionType.POTION;
        }
        if (item == Items.WIND_CHARGE) {
            return PredictionType.WIND_CHARGE;
        }
        if (item == Items.TRIDENT) {
            return PredictionType.TRIDENT;
        }
        return PredictionType.NONE;
    }

    private static boolean isMultishot(ItemStack stack) {
        ChargedProjectilesComponent charged = stack.get(DataComponentTypes.CHARGED_PROJECTILES);
        return charged != null && charged.getProjectiles().size() >= 3;
    }

    private static Vec3d initialVelocity(MinecraftClient client, Vec3d direction, float speed) {
        Vec3d velocity = direction.normalize().multiply(speed);
        Vec3d movement = client.player.getMovement();
        return velocity.add(movement.x, client.player.isOnGround() ? 0.0 : movement.y, movement.z);
    }

    private static float velocityFor(MinecraftClient client, ItemStack stack, PredictionType type, VibeVisualsConfig.ProjectilePredictionConfig config) {
        return switch (type) {
            case BOW -> bowVelocity(client, stack, config);
            case CROSSBOW -> config.crossbowVelocity;
            case PEARL -> config.pearlVelocity;
            case TRIDENT -> config.tridentVelocity;
            case EXPERIENCE_BOTTLE -> config.experienceBottleVelocity;
            case POTION -> config.potionVelocity;
            case WIND_CHARGE -> config.windChargeVelocity;
            case NONE -> 0.0f;
        };
    }

    private static float bowVelocity(MinecraftClient client, ItemStack stack, VibeVisualsConfig.ProjectilePredictionConfig config) {
        if (client.player.isUsingItem() && client.player.getActiveItem() == stack) {
            return config.bowVelocity * BowItem.getPullProgress(client.player.getItemUseTime());
        }

        return config.bowVelocity;
    }

    private static float gravityFor(PredictionType type, VibeVisualsConfig.ProjectilePredictionConfig config) {
        return switch (type) {
            case PEARL, EXPERIENCE_BOTTLE, POTION -> 0.03f;
            case WIND_CHARGE -> 0.0f;
            default -> config.gravity;
        };
    }

    private static float pitchOffset(PredictionType type) {
        return switch (type) {
            case EXPERIENCE_BOTTLE, POTION -> (float) Math.toRadians(-20.0f);
            default -> 0.0f;
        };
    }

    private static float radiusFor(ItemStack stack, PredictionType type, VibeVisualsConfig.ProjectilePredictionConfig config) {
        if (type != PredictionType.POTION) {
            return 0.0f;
        }

        if (stack.isOf(Items.LINGERING_POTION)) {
            return config.lingeringPotionRadius;
        }
        return config.splashPotionRadius;
    }

    private record RadiusMarker(Vec3d center, float radius) {
    }

    private record TrailPoint(Vec3d pos, long tick) {
    }

    private enum PredictionType {
        NONE,
        BOW,
        CROSSBOW,
        PEARL,
        TRIDENT,
        EXPERIENCE_BOTTLE,
        POTION,
        WIND_CHARGE
    }
}
