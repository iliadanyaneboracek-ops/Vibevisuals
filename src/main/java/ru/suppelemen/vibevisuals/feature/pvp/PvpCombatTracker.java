package ru.suppelemen.vibevisuals.feature.pvp;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.UUID;

public final class PvpCombatTracker {
    private static final long COMBAT_DURATION_MS = 30_000L;
    private static final long ABSORPTION_COUNT_COOLDOWN_MS = 1_500L;
    private static final long TOTEM_STATUS_DEDUPE_MS = 1_000L;
    private static final long ABSORPTION_IGNORE_AFTER_TOTEM_MS = 2_000L;

    private static UUID targetUuid;
    private static String targetName = "";
    private static long expiresAtMs;
    private static int totemPops;
    private static int goldenApples;
    private static float lastAbsorption;
    private static long lastAbsorptionCountMs;
    private static long lastTotemPopMs;
    private static long ignoreAbsorptionUntilMs;

    private PvpCombatTracker() {
    }

    public static void startCombat(PlayerEntity target) {
        long now = System.currentTimeMillis();
        UUID uuid = target.getUuid();

        if (!uuid.equals(targetUuid) || now >= expiresAtMs) {
            totemPops = 0;
            goldenApples = 0;
            lastAbsorption = target.getAbsorptionAmount();
            lastAbsorptionCountMs = 0L;
            lastTotemPopMs = 0L;
            ignoreAbsorptionUntilMs = 0L;
        }

        targetUuid = uuid;
        targetName = target.getName().getString();
        expiresAtMs = now + COMBAT_DURATION_MS;
    }

    public static void tick(MinecraftClient client) {
        if (!isActive()) {
            return;
        }

        AbstractClientPlayerEntity target = getTarget(client);
        if (target == null) {
            clear();
            return;
        }

        if (target.isDead() || target.getHealth() <= 0.0f) {
            clear();
            return;
        }

        float absorption = target.getAbsorptionAmount();
        long now = System.currentTimeMillis();
        if (now >= ignoreAbsorptionUntilMs
                && absorption > lastAbsorption + 1.0f
                && now - lastAbsorptionCountMs > ABSORPTION_COUNT_COOLDOWN_MS) {
            goldenApples++;
            lastAbsorptionCountMs = now;
        }
        lastAbsorption = absorption;
    }

    public static void onEntityStatus(Entity entity, byte status) {
        if (status != 35 || targetUuid == null || !targetUuid.equals(entity.getUuid()) || !isActive()) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastTotemPopMs < TOTEM_STATUS_DEDUPE_MS) {
            return;
        }

        totemPops++;
        lastTotemPopMs = now;
        ignoreAbsorptionUntilMs = now + ABSORPTION_IGNORE_AFTER_TOTEM_MS;
        if (entity instanceof PlayerEntity player) {
            lastAbsorption = player.getAbsorptionAmount();
        }
        expiresAtMs = now + COMBAT_DURATION_MS;
    }

    public static boolean isActive() {
        return targetUuid != null && System.currentTimeMillis() < expiresAtMs;
    }

    public static void clearIfExpired() {
        if (targetUuid != null && System.currentTimeMillis() >= expiresAtMs) {
            clear();
        }
    }

    private static void clear() {
        targetUuid = null;
        targetName = "";
        totemPops = 0;
        goldenApples = 0;
        lastAbsorption = 0.0f;
        lastAbsorptionCountMs = 0L;
        lastTotemPopMs = 0L;
        ignoreAbsorptionUntilMs = 0L;
    }

    public static AbstractClientPlayerEntity getTarget(MinecraftClient client) {
        if (targetUuid == null || client.world == null) {
            return null;
        }

        for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
            if (targetUuid.equals(player.getUuid())) {
                return player;
            }
        }

        return null;
    }

    public static String getTargetName() {
        return targetName;
    }

    public static int getTotemPops() {
        return totemPops;
    }

    public static int getGoldenApples() {
        return goldenApples;
    }
}
