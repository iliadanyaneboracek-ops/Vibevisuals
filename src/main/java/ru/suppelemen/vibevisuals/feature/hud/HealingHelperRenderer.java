package ru.suppelemen.vibevisuals.feature.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;
import ru.suppelemen.vibevisuals.util.render.HudCardRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class HealingHelperRenderer {
    private enum Target {
        GOLDEN_CARROT,
        HEALING_POTION,
        GOLDEN_APPLE,
        ENCHANTED_GOLDEN_APPLE
    }

    private static final int MAX_FOOD_LEVEL = 20;

    private HealingHelperRenderer() {
    }

    public static void render(DrawContext context, int startX, int startY, int slotStep, int slotSize) {
        VibeVisualsConfig.HealingHelperConfig config = VibeVisualsConfigManager.get().healingHelper;
        if (!config.enabled) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }
        // Creative / spectator never need health or hunger — suppress the helper entirely.
        if (client.interactionManager == null
                || client.interactionManager.getCurrentGameMode() == null
                || client.interactionManager.getCurrentGameMode().isCreative()
                || client.player.isSpectator()) {
            return;
        }

        PlayerInventory inventory = client.player.getInventory();
        int slots = PlayerInventory.getHotbarSize();

        int foodLevel = client.player.getHungerManager().getFoodLevel();
        boolean hungerFull = foodLevel >= MAX_FOOD_LEVEL;
        boolean healthFull = client.player.getHealth() >= client.player.getMaxHealth();

        // Skip the helper unless there is at least one true healing item
        // (golden apple, enchanted golden apple, or instant-health potion) in
        // the hotbar. Carrots alone aren't enough — they restore hunger only,
        // and a stand-alone carrot highlight is just noise for the user.
        boolean hasHealing =
                findFirstSlot(inventory, slots, matcherFor(Target.HEALING_POTION)) >= 0
             || findFirstSlot(inventory, slots, matcherFor(Target.GOLDEN_APPLE)) >= 0
             || findFirstSlot(inventory, slots, matcherFor(Target.ENCHANTED_GOLDEN_APPLE)) >= 0;
        if (!hasHealing) {
            return;
        }

        List<Target> order = new ArrayList<>(4);
        if (!hungerFull) {
            order.add(Target.GOLDEN_CARROT);
        }
        if (!healthFull) {
            order.add(Target.HEALING_POTION);
        }
        order.add(Target.GOLDEN_APPLE);
        order.add(Target.ENCHANTED_GOLDEN_APPLE);

        int currentSlot = -1;
        int nextSlot = -1;
        for (Target target : order) {
            int slot = findFirstSlot(inventory, slots, matcherFor(target));
            if (slot < 0) {
                continue;
            }
            if (currentSlot < 0) {
                currentSlot = slot;
            } else {
                nextSlot = slot;
                break;
            }
        }

        if (currentSlot < 0) {
            return;
        }

        float pulse = computePulse(config);
        drawSlotHighlight(
                context, startX, startY, slotStep, slotSize, currentSlot,
                config.currentColor,
                config.currentOpacity * pulse,
                config.currentFillOpacity * pulse,
                config.outlineThickness,
                config.padding
        );

        if (nextSlot >= 0) {
            drawSlotHighlight(
                    context, startX, startY, slotStep, slotSize, nextSlot,
                    config.nextColor,
                    config.nextOpacity,
                    config.nextFillOpacity,
                    config.outlineThickness,
                    config.padding
            );
        }
    }

    private static void drawSlotHighlight(
            DrawContext context,
            int startX,
            int startY,
            int slotStep,
            int slotSize,
            int slot,
            int rgb,
            float strokeOpacity,
            float fillOpacity,
            float outlineThickness,
            int padding
    ) {
        int x = startX + slot * slotStep - padding;
        int y = startY - padding;
        int w = slotSize + padding * 2;
        int h = slotSize + padding * 2;

        float clampedFill = clamp(fillOpacity, 0.0f, 1.0f);
        float clampedStroke = clamp(strokeOpacity, 0.0f, 1.0f);
        if (clampedFill > 0.0f) {
            HudCardRenderer.drawOverlayCard(context, x, y, w, h, 3.0f, rgb & 0x00FFFFFF, clampedFill);
        }
        drawBorder(context, x, y, w, h, outlineThickness, rgb & 0x00FFFFFF, clampedStroke);
    }

    private static Predicate<ItemStack> matcherFor(Target target) {
        return switch (target) {
            case GOLDEN_CARROT -> stack -> stack.isOf(Items.GOLDEN_CARROT);
            case HEALING_POTION -> HealingHelperRenderer::isHealingPotion;
            case GOLDEN_APPLE -> stack -> stack.isOf(Items.GOLDEN_APPLE);
            case ENCHANTED_GOLDEN_APPLE -> stack -> stack.isOf(Items.ENCHANTED_GOLDEN_APPLE);
        };
    }

    private static int findFirstSlot(PlayerInventory inventory, int slots, Predicate<ItemStack> matcher) {
        for (int slot = 0; slot < slots; slot++) {
            ItemStack stack = inventory.getStack(slot);
            if (stack != null && !stack.isEmpty() && matcher.test(stack)) {
                return slot;
            }
        }
        return -1;
    }

    private static boolean isHealingPotion(ItemStack stack) {
        if (!stack.isOf(Items.POTION) && !stack.isOf(Items.SPLASH_POTION) && !stack.isOf(Items.LINGERING_POTION)) {
            return false;
        }
        PotionContentsComponent potion = stack.get(DataComponentTypes.POTION_CONTENTS);
        if (potion == null || !potion.hasEffects()) {
            return false;
        }
        for (StatusEffectInstance effect : potion.getEffects()) {
            if (effect.getEffectType().equals(StatusEffects.INSTANT_HEALTH)) {
                return true;
            }
        }
        return false;
    }

    private static float computePulse(VibeVisualsConfig.HealingHelperConfig config) {
        if (config.pulseAmplitude <= 0.0f || config.pulseSpeed <= 0.0f) {
            return 1.0f;
        }
        double phase = (System.currentTimeMillis() / 1000.0) * config.pulseSpeed * (Math.PI * 2.0);
        double sine = Math.sin(phase);
        return clamp((float) (1.0 + sine * config.pulseAmplitude), 0.0f, 2.0f);
    }

    private static void drawBorder(DrawContext context, int x, int y, int w, int h, float thickness, int rgb, float opacity) {
        int t = Math.max(1, Math.round(thickness));
        int alpha = Math.max(0, Math.min(255, Math.round(opacity * 255.0f)));
        if (alpha <= 0) {
            return;
        }
        int color = (alpha << 24) | (rgb & 0x00FFFFFF);
        context.fill(x, y, x + w, y + t, color);
        context.fill(x, y + h - t, x + w, y + h, color);
        context.fill(x, y + t, x + t, y + h - t, color);
        context.fill(x + w - t, y + t, x + w, y + h - t, color);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}