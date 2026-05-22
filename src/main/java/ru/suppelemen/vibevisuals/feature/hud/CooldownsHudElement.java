package ru.suppelemen.vibevisuals.feature.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;
import ru.suppelemen.vibevisuals.core.hud.HudCardElement;
import ru.suppelemen.vibevisuals.mixin.ItemCooldownEntryAccessor;
import ru.suppelemen.vibevisuals.mixin.ItemCooldownManagerAccessor;
import ru.suppelemen.vibevisuals.util.render.HudCardRenderer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CooldownsHudElement extends HudCardElement {
    private static final StyleSpriteSource HUD_FONT = new StyleSpriteSource.Font(Identifier.of("vibevisuals", "hud"));

    public CooldownsHudElement() {
        super("cooldowns", "Cooldowns", VibeVisualsConfigManager.get().cooldownsCard);
    }

    @Override
    protected boolean shouldRenderCard(MinecraftClient client) {
        return !cooldowns(client).isEmpty();
    }

    @Override
    protected void updateLayout(MinecraftClient client) {
        int count = cooldowns(client).size();
        int rowHeight = Math.max(cardConfig.iconSize, scaledTextHeight(cardConfig.effectTextScale));
        height = Math.max(cardConfig.height, cardConfig.effectsStartY + rowHeight + Math.max(0, count - 1) * (rowHeight + cardConfig.rowGap) + cardConfig.bottomPadding);
    }

    @Override
    protected void renderContent(DrawContext context, MinecraftClient client, float tickDelta, int x, int y) {
        VibeVisualsConfig.CardConfig config = cardConfig;
        drawTitleBar(context, x, y, config);
        drawTitleIcon(context, x + config.padding + config.titleIconXOffset, y + config.titleY + config.titleIconYOffset, config.titleIconSize);
        drawScaledText(context, client, hudText("Cooldowns", true), x + config.padding + config.titleIconSize + 6 + config.titleTextXOffset, y + config.titleY + config.titleTextYOffset, config.titleColor, config.textScale);

        List<CooldownEntry> entries = cooldowns(client);
        for (int index = 0; index < entries.size(); index++) {
            CooldownEntry entry = entries.get(index);
            int rowHeight = Math.max(config.iconSize, scaledTextHeight(config.effectTextScale));
            int rowTop = y + config.effectsStartY + index * (rowHeight + config.rowGap);
            int iconX = x + config.padding;
            int iconY = rowTop + rowHeight / 2 - config.iconSize / 2 + config.effectIconYOffset;
            int textY = rowTop + rowHeight / 2 - scaledTextHeight(config.effectTextScale) / 2;
            drawItem(context, entry.stack, iconX, iconY, config.iconSize);
            drawScaledText(context, client, hudText(entry.name, true), iconX + config.iconSize + 4, textY, config.subtitleColor, config.effectTextScale);
            Text time = hudText(formatCooldown(entry.remainingTicks), true);
            drawScaledText(context, client, time, x + config.width - config.padding - scaledTextWidth(client, time, config.timerTextScale), textY + config.timerYOffset, config.timerColor, config.timerTextScale);
        }
    }

    private static List<CooldownEntry> cooldowns(MinecraftClient client) {
        if (client.player == null) {
            return List.of();
        }

        List<CooldownEntry> entries = new ArrayList<>();
        Set<String> seenGroups = new HashSet<>();
        PlayerInventory inventory = client.player.getInventory();
        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = inventory.getStack(slot);
            if (stack.isEmpty() || !client.player.getItemCooldownManager().isCoolingDown(stack)) {
                continue;
            }

            String group = client.player.getItemCooldownManager().getGroup(stack).toString();
            if (!seenGroups.add(group)) {
                continue;
            }

            int remainingTicks = remainingTicks(client, group);
            entries.add(new CooldownEntry(stack, stack.getName().getString(), remainingTicks));
        }

        entries.sort(Comparator.comparing(entry -> entry.name));
        return entries.stream().limit(VibeVisualsConfigManager.get().cooldownsCard.maxEffects).toList();
    }

    private static int remainingTicks(MinecraftClient client, String groupId) {
        ItemCooldownManagerAccessor manager = (ItemCooldownManagerAccessor) client.player.getItemCooldownManager();
        Object entry = manager.vibevisuals$getEntries().get(Identifier.tryParse(groupId));
        if (entry instanceof ItemCooldownEntryAccessor accessor) {
            return Math.max(0, accessor.vibevisuals$getEndTick() - manager.vibevisuals$getTick());
        }

        return 0;
    }

    private static String formatCooldown(int ticks) {
        int seconds = Math.max(1, (int) Math.ceil(ticks / 20.0));
        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;
        return minutes > 0
                ? minutes + ":" + (remainingSeconds < 10 ? "0" : "") + remainingSeconds
                : "0:" + (seconds < 10 ? "0" : "") + seconds;
    }

    private static void drawTitleBar(DrawContext context, int x, int y, VibeVisualsConfig.CardConfig config) {
        int barHeight = Math.min(config.titleBarHeight, config.height);
        if (barHeight <= 0 || config.titleBarOpacity <= 0.0f) {
            return;
        }

        context.enableScissor(x, y, x + config.width, y + barHeight);
        HudCardRenderer.drawOverlayCard(context, x, y, config.width, config.height, config.radius, config.titleBarColor, config.titleBarOpacity);
        context.disableScissor();
    }

    private static void drawTitleIcon(DrawContext context, int x, int y, int size) {
        int ring = 0xFF7C5CFF;
        int glow = 0x887C5CFF;
        int face = 0xFFEFEFF6;
        HudCardRenderer.drawOverlayCard(context, x, y, size, size, size / 2.0f, ring, 0.34f);
        HudCardRenderer.drawOverlayCard(context, x + 1, y + 1, Math.max(1, size - 2), Math.max(1, size - 2), Math.max(1.0f, size / 2.0f - 1.0f), glow, 0.34f);
        int centerX = x + size / 2;
        int centerY = y + size / 2;
        context.fill(centerX - 1, centerY - Math.max(2, size / 4), centerX + 1, centerY + 1, face);
        context.fill(centerX, centerY, centerX + Math.max(2, size / 3), centerY + 1, face);
        context.fill(centerX - 1, centerY - 1, centerX + 2, centerY + 2, face);
    }

    private static void drawItem(DrawContext context, ItemStack stack, int x, int y, int size) {
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(x, y);
        context.getMatrices().scale(size / 16.0f, size / 16.0f);
        context.drawItem(stack, 0, 0);
        context.getMatrices().popMatrix();
    }

    private static void drawScaledText(DrawContext context, MinecraftClient client, Text text, int x, int y, int color, float scale) {
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(x, y);
        context.getMatrices().scale(scale, scale);
        context.drawText(client.textRenderer, text, 0, 0, color, false);
        context.getMatrices().popMatrix();
    }

    private static int scaledTextWidth(MinecraftClient client, Text text, float scale) {
        return Math.round(client.textRenderer.getWidth(text) * scale);
    }

    private static int scaledTextHeight(float scale) {
        return Math.round(9.0f * scale);
    }

    private static Text hudText(String text, boolean bold) {
        return Text.literal(text).styled(style -> style.withFont(HUD_FONT).withBold(bold));
    }

    private record CooldownEntry(ItemStack stack, String name, int remainingTicks) {
    }
}
