package ru.suppelemen.vibevisuals.feature.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;
import ru.suppelemen.vibevisuals.core.hud.HudElement;
import ru.suppelemen.vibevisuals.theme.HudCardRenderType;
import ru.suppelemen.vibevisuals.theme.HudVisualSettings;
import ru.suppelemen.vibevisuals.util.render.HudCardRenderer;

import java.util.ArrayList;
import java.util.List;

public class ArmorHudElement extends HudElement {
    private static final List<EquipmentSlot> ARMOR_SLOTS = List.of(
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    );
    private static final List<ItemStack> EDITOR_ARMOR = List.of(
            new ItemStack(Items.NETHERITE_HELMET),
            new ItemStack(Items.NETHERITE_CHESTPLATE),
            new ItemStack(Items.NETHERITE_LEGGINGS),
            new ItemStack(Items.NETHERITE_BOOTS)
    );

    private final VibeVisualsConfig.ArmorHudConfig config;
    private final HudVisualSettings visualSettings = new HudVisualSettings();

    public ArmorHudElement() {
        super("armor_hud", "Armor HUD", 0, 0, 0, 0);
        this.config = VibeVisualsConfigManager.get().armorHud;
    }

    @Override
    public void render(DrawContext context, MinecraftClient client, float tickDelta, boolean editorMode) {
        syncFromConfig();

        if (!enabled) {
            return;
        }

        List<ItemStack> armor = getVisibleArmor(client, editorMode);
        if (armor.isEmpty()) {
            return;
        }

        int contentWidth = config.padding * 2
                + armor.size() * config.iconSize
                + Math.max(0, armor.size() - 1) * config.iconGap;
        width = Math.max(config.width, contentWidth);

        int ix = (int) Math.round(x);
        int iy = (int) Math.round(y);
        HudCardRenderer.drawCard(context, ix, iy, width, height, visualSettings);

        int iconX = ix + config.padding;
        int iconY = iy + Math.max(0, (height - config.iconSize) / 2) + config.iconYOffset;
        for (ItemStack stack : armor) {
            drawArmorItem(context, stack, iconX, iconY);
            if (config.showDurability && stack.isDamageable()) {
                drawDurability(context, stack, iconX, iy + config.durabilityBarYOffset, config.iconSize, config.durabilityBarHeight);
            }
            iconX += config.iconSize + config.iconGap;
        }

        if (editorMode) {
            drawEditorOutline(context);
        }
    }

    @Override
    public boolean isVisibleForInteraction(MinecraftClient client, boolean editorMode) {
        syncFromConfig();
        if (!enabled) {
            return false;
        }

        List<ItemStack> armor = getVisibleArmor(client, editorMode);
        if (armor.isEmpty()) {
            return false;
        }

        int contentWidth = config.padding * 2
                + armor.size() * config.iconSize
                + Math.max(0, armor.size() - 1) * config.iconGap;
        width = Math.max(config.width, contentWidth);
        return true;
    }

    private void syncFromConfig() {
        enabled = config.enabled;
        x = config.x;
        y = config.y;
        width = config.width;
        height = config.height;
        visualSettings.renderType = HudCardRenderType.LIQUID_GLASS;
        visualSettings.radius = config.radius;
        visualSettings.opacity = config.opacity;
        visualSettings.glow = false;
        visualSettings.blur = false;
    }

    private static List<ItemStack> getVisibleArmor(MinecraftClient client, boolean editorMode) {
        if (client.player == null) {
            return editorMode ? EDITOR_ARMOR : List.of();
        }

        List<ItemStack> armor = new ArrayList<>();
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = client.player.getEquippedStack(slot);
            if (!stack.isEmpty()) {
                armor.add(stack);
            }
        }

        if (armor.isEmpty() && editorMode) {
            return EDITOR_ARMOR;
        }

        return armor;
    }

    private void drawArmorItem(DrawContext context, ItemStack stack, int x, int y) {
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(x, y);
        context.getMatrices().scale(config.iconSize / 16.0f, config.iconSize / 16.0f);
        context.drawItem(stack, 0, 0);
        context.getMatrices().popMatrix();
    }

    private void drawDurability(DrawContext context, ItemStack stack, int x, int y, int width, int height) {
        int maxDamage = stack.getMaxDamage();
        if (maxDamage <= 0) {
            return;
        }

        float durability = 1.0f - (float) stack.getDamage() / maxDamage;
        durability = Math.max(0.0f, Math.min(1.0f, durability));
        context.fill(x, y, x + width, y + height, config.durabilityBackgroundColor);
        context.fill(x, y, x + Math.round(width * durability), y + height, config.durabilityColor);
    }
}
