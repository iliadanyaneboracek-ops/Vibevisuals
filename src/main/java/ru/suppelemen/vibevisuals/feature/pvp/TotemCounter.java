package ru.suppelemen.vibevisuals.feature.pvp;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Counts Totem of Undying pops (entity status 35) for the local player and
 * other players, printing a running total to chat.
 */
public final class TotemCounter {
    private static final long DEDUPE_MS = 800L;

    private static final Map<UUID, Integer> COUNTS = new HashMap<>();
    private static final Map<UUID, Long> LAST_POP_MS = new HashMap<>();

    private TotemCounter() {
    }

    public static void onEntityStatus(Entity entity, byte status) {
        if (status != 35 || !(entity instanceof PlayerEntity player)) {
            return;
        }

        VibeVisualsConfig.TotemCounterConfig config = VibeVisualsConfigManager.get().totemCounter;
        if (!config.enabled) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        boolean self = player.getUuid().equals(client.player.getUuid());
        if (self && !config.trackSelf) {
            return;
        }
        if (!self && !config.trackOthers) {
            return;
        }
        if (config.onlyInPvp && !PvpCombatTracker.isActive()) {
            return;
        }

        long now = System.currentTimeMillis();
        UUID uuid = player.getUuid();
        Long last = LAST_POP_MS.get(uuid);
        if (last != null && now - last < DEDUPE_MS) {
            return;
        }
        LAST_POP_MS.put(uuid, now);

        int count = COUNTS.merge(uuid, 1, Integer::sum);
        String name = player.getName().getString();

        Text message = Text.literal("")
                .append(Text.literal("Тотем").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD))
                .append(Text.literal(" » ").formatted(Formatting.DARK_GRAY))
                .append(Text.literal(self ? "Вы" : name).formatted(self ? Formatting.AQUA : Formatting.WHITE))
                .append(Text.literal(" снесли тотем ").formatted(Formatting.GRAY))
                .append(Text.literal("(" + count + ")").formatted(Formatting.YELLOW));
        client.player.sendMessage(message, false);

        if (config.playSound) {
            client.getSoundManager().play(
                    PositionedSoundInstance.master(SoundEvents.ENTITY_ARROW_HIT_PLAYER, 1.0f, 0.6f));
        }
    }

    public static int getCount(UUID uuid) {
        return COUNTS.getOrDefault(uuid, 0);
    }

    public static void reset() {
        COUNTS.clear();
        LAST_POP_MS.clear();
    }
}
