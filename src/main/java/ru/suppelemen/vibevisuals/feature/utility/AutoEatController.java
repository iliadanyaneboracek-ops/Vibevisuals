package ru.suppelemen.vibevisuals.feature.utility;

import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;

public final class AutoEatController {
    private static boolean eating;
    private static int previousSlot = -1;
    private static int eatingSlot = -1;
    private static int cooldownTicks;

    private AutoEatController() {
    }

    public static void tick(MinecraftClient client) {
        if (cooldownTicks > 0) {
            cooldownTicks--;
        }

        if (client == null || client.player == null || client.world == null) {
            stop(client, true);
            return;
        }

        VibeVisualsConfig.AutoEatConfig config = VibeVisualsConfigManager.get().autoEat;
        if (!config.enabled || client.currentScreen != null || client.player.isCreative() || client.player.isSpectator()) {
            stop(client, true);
            return;
        }

        int foodLevel = client.player.getHungerManager().getFoodLevel();
        int threshold = hungerThreshold(config.hungerPercent);
        boolean shouldEat = foodLevel <= threshold && foodLevel < 20;

        if (eating) {
            if (!shouldEat || !isAllowedFood(client.player.getInventory().getSelectedStack())) {
                stop(client, true);
                return;
            }

            client.options.useKey.setPressed(true);
            return;
        }

        if (!shouldEat || cooldownTicks > 0 || client.player.isUsingItem()) {
            return;
        }

        int slot = findFoodSlot(client.player.getInventory());
        if (slot < 0) {
            return;
        }

        previousSlot = client.player.getInventory().getSelectedSlot();
        eatingSlot = slot;
        if (previousSlot != eatingSlot) {
            client.player.getInventory().setSelectedSlot(eatingSlot);
        }

        eating = true;
        client.options.useKey.setPressed(true);
    }

    public static void stop(MinecraftClient client, boolean restoreSlot) {
        if (!eating) {
            return;
        }

        if (client != null) {
            client.options.useKey.setPressed(false);
            if (restoreSlot && client.player != null && previousSlot >= 0 && previousSlot < PlayerInventory.getHotbarSize()) {
                client.player.getInventory().setSelectedSlot(previousSlot);
            }
        }

        eating = false;
        previousSlot = -1;
        eatingSlot = -1;
        cooldownTicks = 4;
    }

    public static boolean isEating() {
        return eating;
    }

    private static int hungerThreshold(int hungerPercent) {
        int percent = Math.max(1, Math.min(100, hungerPercent));
        return Math.max(1, Math.min(19, Math.round(20.0f * percent / 100.0f)));
    }

    private static int findFoodSlot(PlayerInventory inventory) {
        for (int slot = 0; slot < PlayerInventory.getHotbarSize(); slot++) {
            if (isAllowedFood(inventory.getStack(slot))) {
                return slot;
            }
        }
        return -1;
    }

    private static boolean isAllowedFood(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        if (stack.isOf(Items.GOLDEN_APPLE) || stack.isOf(Items.ENCHANTED_GOLDEN_APPLE)) {
            return false;
        }

        return stack.get(DataComponentTypes.FOOD) != null;
    }
}
