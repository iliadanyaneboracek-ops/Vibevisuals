package ru.suppelemen.vibevisuals.feature.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;
import ru.suppelemen.vibevisuals.core.hud.HudElement;
import ru.suppelemen.vibevisuals.util.render.HudGlass;

import java.util.ArrayList;
import java.util.List;

public class InventoryHudElement extends HudElement {
    private static final List<ItemStack> EDITOR_STACKS = List.of(
            new ItemStack(Items.TOTEM_OF_UNDYING, 2),
            new ItemStack(Items.ENDER_PEARL, 16),
            new ItemStack(Items.OBSIDIAN, 64),
            new ItemStack(Items.SAND, 64),
            new ItemStack(Items.GOLDEN_APPLE, 8),
            new ItemStack(Items.EXPERIENCE_BOTTLE, 64),
            new ItemStack(Items.FIRE_CHARGE, 20),
            new ItemStack(Items.WATER_BUCKET),
            new ItemStack(Items.DIAMOND_SWORD)
    );

    private final VibeVisualsConfig.InventoryHudConfig config;

    public InventoryHudElement() {
        super("inventory_hud", "Inventory HUD", 0, 0, 0, 0);
        this.config = VibeVisualsConfigManager.get().inventoryHud;
    }

    @Override
    public void render(DrawContext context, MinecraftClient client, float tickDelta, boolean editorMode) {
        syncFromConfig();
        if (!enabled) {
            return;
        }

        List<ItemStack> stacks = getStacks(client, editorMode);
        updateSize(stacks.size());
        if (stacks.isEmpty() && !editorMode) {
            return;
        }

        int ix = getX();
        int iy = getY();
        HudGlass.drawPanel(context, ix, iy, width, height, Math.round(config.radius));

        int slot = 0;
        for (ItemStack stack : stacks) {
            int column = slot % config.columns;
            int row = slot / config.columns;
            int slotX = ix + config.padding + column * (config.slotSize + config.slotGap);
            int slotY = iy + config.padding + row * (config.slotSize + config.slotGap);
            HudGlass.drawChip(context, slotX, slotY, config.slotSize, config.slotSize, 4);
            if (!stack.isEmpty()) {
                drawItem(context, client, stack, slotX, slotY);
            }
            slot++;
        }
    }

    @Override
    public boolean isVisibleForInteraction(MinecraftClient client, boolean editorMode) {
        syncFromConfig();
        if (!enabled) {
            return false;
        }

        List<ItemStack> stacks = getStacks(client, editorMode);
        updateSize(stacks.size());
        return !stacks.isEmpty();
    }

    private void syncFromConfig() {
        enabled = config.enabled;
        x = config.x;
        y = config.y;
    }

    private void updateSize(int slots) {
        int rows = Math.max(1, (int) Math.ceil(slots / (double) config.columns));
        int contentWidth = config.padding * 2 + config.columns * config.slotSize + Math.max(0, config.columns - 1) * config.slotGap;
        int contentHeight = config.padding * 2 + rows * config.slotSize + Math.max(0, rows - 1) * config.slotGap;
        width = Math.max(config.width, contentWidth);
        height = Math.max(config.height, contentHeight);
    }

    private List<ItemStack> getStacks(MinecraftClient client, boolean editorMode) {
        if (client.player == null) {
            return editorMode ? EDITOR_STACKS : List.of();
        }

        PlayerInventory inventory = client.player.getInventory();
        int start = config.showHotbar ? 0 : PlayerInventory.getHotbarSize();
        int end = Math.min(inventory.size(), 36);
        List<ItemStack> stacks = new ArrayList<>();
        for (int index = start; index < end; index++) {
            stacks.add(inventory.getStack(index));
        }

        if (isEmpty(stacks) && editorMode) {
            return EDITOR_STACKS;
        }

        return stacks;
    }

    private static boolean isEmpty(List<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private void drawItem(DrawContext context, MinecraftClient client, ItemStack stack, int x, int y) {
        int itemX = x + Math.max(0, (config.slotSize - 16) / 2);
        int itemY = y + Math.max(0, (config.slotSize - 16) / 2);
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(itemX, itemY);
        context.drawItem(stack, 0, 0);
        context.drawStackOverlay(client.textRenderer, stack, 0, 0);
        context.getMatrices().popMatrix();
    }
}
