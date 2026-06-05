package ru.suppelemen.vibevisuals.feature.utility;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Logs items the local player picks up from the ground. Pickups of the same item are merged
 * over a short idle window so a full stack becomes a single "+64 Cobblestone" message instead
 * of dozens of lines. Items are classified into groups so logging can be filtered or highlighted.
 */
public final class ItemPickupLogger {
    private enum Group {
        VALUABLE,
        EQUIPMENT,
        FOOD,
        BLOCK,
        OTHER
    }

    private static final Set<Item> VALUABLES = Set.of(
            Items.DIAMOND, Items.DIAMOND_BLOCK,
            Items.NETHERITE_INGOT, Items.NETHERITE_SCRAP, Items.NETHERITE_BLOCK, Items.ANCIENT_DEBRIS,
            Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS,
            Items.EMERALD, Items.EMERALD_BLOCK,
            Items.GOLD_INGOT, Items.GOLD_BLOCK, Items.RAW_GOLD, Items.RAW_GOLD_BLOCK,
            Items.NETHER_STAR, Items.TOTEM_OF_UNDYING, Items.ELYTRA,
            Items.ENCHANTED_GOLDEN_APPLE, Items.ENCHANTED_BOOK,
            Items.BEACON, Items.CONDUIT, Items.DRAGON_EGG, Items.DRAGON_HEAD,
            Items.HEART_OF_THE_SEA, Items.NAUTILUS_SHELL, Items.WITHER_SKELETON_SKULL,
            Items.MACE, Items.TRIAL_KEY, Items.OMINOUS_TRIAL_KEY,
            Items.ECHO_SHARD, Items.RECOVERY_COMPASS, Items.END_CRYSTAL
    );

    private static final Map<Item, Pending> PENDING = new LinkedHashMap<>();
    private static final Map<Item, Integer> SESSION_TOTALS = new HashMap<>();
    private static long tickCounter;

    private ItemPickupLogger() {
    }

    /** Called from the network mixin when the local player collects a ground item. */
    public static void onPickup(ItemStack stack, int amount) {
        if (stack == null || stack.isEmpty() || amount <= 0) {
            return;
        }

        VibeVisualsConfig.ItemPickupLoggerConfig config = VibeVisualsConfigManager.get().itemPickupLogger;
        if (!config.enabled) {
            return;
        }

        Group group = classify(stack);
        if (!shouldLog(group, config)) {
            return;
        }

        Item item = stack.getItem();
        Pending entry = PENDING.get(item);
        if (entry == null) {
            entry = new Pending(stack.getName().copy(), group, tickCounter);
            PENDING.put(item, entry);
        }
        entry.count += amount;
        entry.lastTick = tickCounter;
    }

    public static void tick(MinecraftClient client) {
        if (client == null || client.player == null || client.world == null) {
            PENDING.clear();
            SESSION_TOTALS.clear();
            return;
        }

        VibeVisualsConfig.ItemPickupLoggerConfig config = VibeVisualsConfigManager.get().itemPickupLogger;
        if (!config.enabled) {
            if (!PENDING.isEmpty()) {
                PENDING.clear();
            }
            return;
        }

        tickCounter++;
        if (PENDING.isEmpty()) {
            return;
        }

        int idle = config.aggregateWindowTicks;
        int maxAge = Math.max(60, idle * 4);
        Iterator<Map.Entry<Item, Pending>> iterator = PENDING.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Item, Pending> mapEntry = iterator.next();
            Pending pending = mapEntry.getValue();
            boolean idleElapsed = tickCounter - pending.lastTick >= idle;
            boolean maxElapsed = tickCounter - pending.firstTick >= maxAge;
            if (idleElapsed || maxElapsed) {
                flush(client, mapEntry.getKey(), pending, config);
                iterator.remove();
            }
        }
    }

    private static void flush(MinecraftClient client, Item item, Pending pending, VibeVisualsConfig.ItemPickupLoggerConfig config) {
        if (pending.count < config.minCount) {
            return;
        }

        int total = SESSION_TOTALS.merge(item, pending.count, Integer::sum);
        boolean valuable = config.highlightValuables && pending.group == Group.VALUABLE;
        int nameColor = valuable ? config.valuableColor : config.defaultColor;

        MutableText message = Text.literal("+" + pending.count)
                .styled(style -> style.withColor(TextColor.fromRgb(config.countColor & 0xFFFFFF)));
        message.append(Text.literal(" "));
        message.append(pending.name.copy().styled(style -> style.withColor(TextColor.fromRgb(nameColor & 0xFFFFFF))));
        if (config.showSessionTotal && total != pending.count) {
            message.append(Text.literal(" (×" + total + ")")
                    .styled(style -> style.withColor(TextColor.fromRgb(config.totalColor & 0xFFFFFF))));
        }

        client.player.sendMessage(message, config.useActionBar);

        if (valuable && config.playValuableSound) {
            client.getSoundManager().play(
                    PositionedSoundInstance.master(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.5f, 0.6f));
        }
    }

    private static boolean shouldLog(Group group, VibeVisualsConfig.ItemPickupLoggerConfig config) {
        if (config.logAllItems) {
            return true;
        }
        return switch (group) {
            case VALUABLE -> config.logValuables;
            case EQUIPMENT -> config.logEquipment;
            case FOOD -> config.logFood;
            case BLOCK -> config.logBlocks;
            case OTHER -> config.logOther;
        };
    }

    private static Group classify(ItemStack stack) {
        if (isValuable(stack)) {
            return Group.VALUABLE;
        }
        if (isEquipment(stack)) {
            return Group.EQUIPMENT;
        }
        if (stack.get(DataComponentTypes.FOOD) != null) {
            return Group.FOOD;
        }
        if (stack.getItem() instanceof BlockItem) {
            return Group.BLOCK;
        }
        return Group.OTHER;
    }

    private static boolean isValuable(ItemStack stack) {
        if (VALUABLES.contains(stack.getItem())) {
            return true;
        }
        if (stack.hasEnchantments()) {
            return true;
        }
        ItemEnchantmentsComponent stored = stack.get(DataComponentTypes.STORED_ENCHANTMENTS);
        if (stored != null && !stored.isEmpty()) {
            return true;
        }
        return Registries.ITEM.getId(stack.getItem()).getPath().endsWith("shulker_box");
    }

    private static boolean isEquipment(ItemStack stack) {
        return stack.get(DataComponentTypes.MAX_DAMAGE) != null
                || stack.get(DataComponentTypes.TOOL) != null
                || stack.get(DataComponentTypes.EQUIPPABLE) != null;
    }

    private static final class Pending {
        final Text name;
        final Group group;
        final long firstTick;
        int count;
        long lastTick;

        Pending(Text name, Group group, long tick) {
            this.name = name;
            this.group = group;
            this.firstTick = tick;
            this.lastTick = tick;
        }
    }
}
