package ru.suppelemen.vibevisuals.feature.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;
import ru.suppelemen.vibevisuals.mixin.ItemCooldownEntryAccessor;
import ru.suppelemen.vibevisuals.mixin.ItemCooldownManagerAccessor;

import java.util.Locale;

public final class SlotTimersRenderer {
    private SlotTimersRenderer() {
    }

    public static void render(DrawContext context, int startX, int startY, int slotStep, int slotSize) {
        VibeVisualsConfig.SlotTimersConfig config = VibeVisualsConfigManager.get().slotTimers;
        if (!config.enabled) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        PlayerInventory inventory = client.player.getInventory();
        ItemCooldownManager cooldowns = client.player.getItemCooldownManager();
        int slots = PlayerInventory.getHotbarSize();

        for (int slot = 0; slot < slots; slot++) {
            ItemStack stack = inventory.getStack(slot);
            if (stack.isEmpty() || !cooldowns.isCoolingDown(stack)) {
                continue;
            }

            int remaining = remainingTicks(cooldowns, stack);
            if (remaining <= 0) {
                continue;
            }

            String label = formatCooldown(remaining, config.showSubsecond);
            int color = (remaining / 20.0f) <= config.urgentThresholdSeconds ? config.urgentColor : config.textColor;
            drawTimer(context, client, label, startX + slot * slotStep, startY, slotSize, color, config);
        }
    }

    private static int remainingTicks(ItemCooldownManager cooldowns, ItemStack stack) {
        Identifier group = cooldowns.getGroup(stack);
        if (group == null) {
            return 0;
        }
        ItemCooldownManagerAccessor manager = (ItemCooldownManagerAccessor) cooldowns;
        Object entry = manager.vibevisuals$getEntries().get(group);
        if (entry instanceof ItemCooldownEntryAccessor accessor) {
            return Math.max(0, accessor.vibevisuals$getEndTick() - manager.vibevisuals$getTick());
        }
        return 0;
    }

    private static String formatCooldown(int ticks, boolean showSubsecond) {
        float seconds = ticks / 20.0f;
        if (seconds >= 60.0f) {
            int minutes = (int) Math.ceil(seconds / 60.0f);
            return minutes + "m";
        }
        if (seconds >= 10.0f) {
            return String.valueOf((int) Math.ceil(seconds));
        }
        if (showSubsecond && seconds < 1.0f) {
            return String.format(Locale.ROOT, ".%d", Math.max(1, (int) Math.ceil(seconds * 10.0f)));
        }
        return String.valueOf((int) Math.ceil(seconds));
    }

    private static void drawTimer(
            DrawContext context,
            MinecraftClient client,
            String label,
            int slotX,
            int slotY,
            int slotSize,
            int color,
            VibeVisualsConfig.SlotTimersConfig config
    ) {
        Text text = Text.literal(label);
        float scale = config.textScale;
        float textWidth = client.textRenderer.getWidth(text) * scale;
        float textHeight = client.textRenderer.fontHeight * scale;
        float cx = slotX + (slotSize - textWidth) / 2.0f + config.xOffset;
        float cy = slotY + (slotSize - textHeight) / 2.0f + config.yOffset;

        context.getMatrices().pushMatrix();
        context.getMatrices().translate(cx, cy);
        context.getMatrices().scale(scale, scale);
        context.drawText(client.textRenderer, text, 0, 0, color, config.showShadow);
        context.getMatrices().popMatrix();
    }
}
