package ru.suppelemen.vibevisuals.feature.utility;

import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;

public final class AutoPotionController {
    private static boolean drinking;
    private static int previousSlot = -1;
    private static int drinkingSlot = -1;
    private static int swappedInventorySlot = -1;
    private static int cooldownTicks;

    private AutoPotionController() {
    }

    public static void tick(MinecraftClient client) {
        if (cooldownTicks > 0) {
            cooldownTicks--;
        }

        if (client == null || client.player == null || client.world == null) {
            stop(client, true);
            return;
        }

        VibeVisualsConfig.AutoPotionConfig config = VibeVisualsConfigManager.get().autoPotion;
        if (!config.enabled || client.player.isCreative() || client.player.isSpectator()) {
            stop(client, true);
            return;
        }

        if (drinking) {
            if (!isWantedPotion(client.player.getInventory().getSelectedStack(), config)) {
                stop(client, true);
                return;
            }

            client.options.useKey.setPressed(true);
            return;
        }

        if (cooldownTicks > 0 || client.player.isUsingItem() || AutoEatController.isEating()) {
            return;
        }

        RegistryEntry<StatusEffect> wantedEffect = nextEffectToRefresh(client, config);
        if (wantedEffect == null) {
            return;
        }

        int slot = findPotionSlot(client.player.getInventory(), wantedEffect);
        if (slot < 0) {
            return;
        }

        previousSlot = client.player.getInventory().getSelectedSlot();
        if (slot < PlayerInventory.getHotbarSize()) {
            drinkingSlot = slot;
            if (previousSlot != drinkingSlot) {
                client.player.getInventory().setSelectedSlot(drinkingSlot);
            }
        } else {
            drinkingSlot = previousSlot;
            swappedInventorySlot = slot;
            client.player.getInventory().swapSlotWithHotbar(swappedInventorySlot);
        }

        drinking = true;
        client.options.useKey.setPressed(true);
    }

    public static void stop(MinecraftClient client, boolean restoreSlot) {
        if (!drinking) {
            return;
        }

        if (client != null && client.player != null) {
            client.options.useKey.setPressed(false);
            if (swappedInventorySlot >= PlayerInventory.getHotbarSize()) {
                client.player.getInventory().swapSlotWithHotbar(swappedInventorySlot);
            }
            if (restoreSlot && previousSlot >= 0 && previousSlot < PlayerInventory.getHotbarSize()) {
                client.player.getInventory().setSelectedSlot(previousSlot);
            }
        } else if (client != null) {
            client.options.useKey.setPressed(false);
        }

        drinking = false;
        previousSlot = -1;
        drinkingSlot = -1;
        swappedInventorySlot = -1;
        cooldownTicks = 8;
    }

    private static RegistryEntry<StatusEffect> nextEffectToRefresh(MinecraftClient client, VibeVisualsConfig.AutoPotionConfig config) {
        int refreshTicks = Math.max(0, config.refreshSeconds) * 20;

        if (config.useInvisibility && shouldRefresh(client, StatusEffects.INVISIBILITY, refreshTicks)) {
            return StatusEffects.INVISIBILITY;
        }

        if (config.useSpeed && shouldRefresh(client, StatusEffects.SPEED, refreshTicks)) {
            return StatusEffects.SPEED;
        }

        return null;
    }

    private static boolean shouldRefresh(MinecraftClient client, RegistryEntry<StatusEffect> effect, int refreshTicks) {
        StatusEffectInstance active = client.player.getStatusEffect(effect);
        return active == null || active.getDuration() <= refreshTicks;
    }

    private static int findPotionSlot(PlayerInventory inventory, RegistryEntry<StatusEffect> wantedEffect) {
        for (int slot = 0; slot < PlayerInventory.MAIN_SIZE; slot++) {
            if (containsEffect(inventory.getStack(slot), wantedEffect)) {
                return slot;
            }
        }
        return -1;
    }

    private static boolean isWantedPotion(ItemStack stack, VibeVisualsConfig.AutoPotionConfig config) {
        return (config.useInvisibility && containsEffect(stack, StatusEffects.INVISIBILITY))
                || (config.useSpeed && containsEffect(stack, StatusEffects.SPEED));
    }

    private static boolean containsEffect(ItemStack stack, RegistryEntry<StatusEffect> wantedEffect) {
        if (stack == null || stack.isEmpty() || !stack.isOf(Items.POTION)) {
            return false;
        }

        PotionContentsComponent potion = stack.get(DataComponentTypes.POTION_CONTENTS);
        if (potion == null || !potion.hasEffects()) {
            return false;
        }

        for (StatusEffectInstance effect : potion.getEffects()) {
            if (effect.getEffectType().equals(wantedEffect)) {
                return true;
            }
        }

        return false;
    }
}
