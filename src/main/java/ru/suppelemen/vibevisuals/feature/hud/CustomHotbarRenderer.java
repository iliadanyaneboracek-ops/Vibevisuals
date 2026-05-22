package ru.suppelemen.vibevisuals.feature.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;
import ru.suppelemen.vibevisuals.core.hud.HudManager;
import ru.suppelemen.vibevisuals.theme.HudCardRenderType;
import ru.suppelemen.vibevisuals.theme.HudVisualSettings;
import ru.suppelemen.vibevisuals.util.render.HudCardRenderer;

public final class CustomHotbarRenderer {
    private static final HudVisualSettings SETTINGS = new HudVisualSettings();
    private static float animatedSelectedSlot = -1.0f;

    private CustomHotbarRenderer() {
    }

    public static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        VibeVisualsConfig.HotbarConfig config = VibeVisualsConfigManager.get().hotbar;
        SETTINGS.renderType = HudCardRenderType.LIQUID_GLASS;
        SETTINGS.radius = config.radius;
        SETTINGS.opacity = config.opacity;
        SETTINGS.glow = false;
        SETTINGS.blur = false;

        float hudScale = HudManager.getHudScale();
        int screenWidth = Math.max(1, Math.round(context.getScaledWindowWidth() / hudScale));
        int screenHeight = Math.max(1, Math.round(context.getScaledWindowHeight() / hudScale));
        int slots = PlayerInventory.getHotbarSize();
        int width = config.padding * 2 + slots * config.slotSize + (slots - 1) * config.slotGap;
        int height = config.padding * 2 + config.slotSize;
        int x = (screenWidth - width) / 2;
        int y = screenHeight - height - 4 + config.yOffset;

        context.getMatrices().pushMatrix();
        context.getMatrices().scale(hudScale, hudScale);

        HudCardRenderer.drawCard(context, x, y, width, height, SETTINGS);
        PlayerInventory inventory = client.player.getInventory();
        int selectedSlot = inventory.getSelectedSlot();
        if (animatedSelectedSlot < 0.0f) {
            animatedSelectedSlot = selectedSlot;
        }
        animatedSelectedSlot += (selectedSlot - animatedSelectedSlot) * config.selectedAnimationSpeed;

        int selectedX = Math.round(x + config.padding + animatedSelectedSlot * (config.slotSize + config.slotGap));
        int slotY = y + config.padding;
        HudCardRenderer.drawShaderOutline(context, selectedX - 2, slotY - 2, config.slotSize + 4, config.slotSize + 4, 5.0f, 1.2f, 0.96f);
        context.fill(selectedX, y + height - 3, selectedX + config.slotSize, y + height - 1, config.selectedColor);
        for (int slot = 0; slot < slots; slot++) {
            int slotX = x + config.padding + slot * (config.slotSize + config.slotGap);
            HudCardRenderer.drawOverlayCard(context, slotX, slotY, config.slotSize, config.slotSize, 4.0f, config.slotColor, config.slotOpacity);

            ItemStack stack = inventory.getStack(slot);
            if (!stack.isEmpty()) {
                drawItem(context, client, stack, slotX, slotY, config.slotSize);
            }
        }
        context.getMatrices().popMatrix();
    }

    private static void drawItem(DrawContext context, MinecraftClient client, ItemStack stack, int x, int y, int size) {
        int itemX = x + Math.max(0, (size - 16) / 2);
        int itemY = y + Math.max(0, (size - 16) / 2);
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(itemX, itemY);
        context.drawItem(stack, 0, 0);
        context.drawStackOverlay(client.textRenderer, stack, 0, 0);
        context.getMatrices().popMatrix();
    }
}
