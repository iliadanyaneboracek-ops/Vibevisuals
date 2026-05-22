package ru.suppelemen.vibevisuals.core.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;
import ru.suppelemen.vibevisuals.feature.hud.ArmorHudElement;
import ru.suppelemen.vibevisuals.feature.hud.CardTestHudElement;
import ru.suppelemen.vibevisuals.feature.hud.CooldownsHudElement;
import ru.suppelemen.vibevisuals.feature.hud.HotKeysHudElement;
import ru.suppelemen.vibevisuals.feature.hud.InventoryHudElement;
import ru.suppelemen.vibevisuals.feature.hud.PvpCombatHudElement;
import ru.suppelemen.vibevisuals.feature.hud.TopStatusHudElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class HudManager {
    private static final List<HudElement> ELEMENTS = new ArrayList<>();
    private static final Map<String, Float> APPEAR_PROGRESS = new HashMap<>();
    private static boolean initialized = false;

    private HudManager() {
    }

    public static void init() {
        if (initialized) {
            return;
        }

        reload();
        initialized = true;
        System.out.println("[vibevisuals] HUD initialized, elements=" + ELEMENTS.size());
    }

    public static void reload() {
        ELEMENTS.clear();

        // Start with one test card while the rest of the HUD is still being wired up.
        ELEMENTS.add(new TopStatusHudElement());
        ELEMENTS.add(new CardTestHudElement());
        ELEMENTS.add(new CooldownsHudElement());
        ELEMENTS.add(new HotKeysHudElement());
        ELEMENTS.add(new PvpCombatHudElement());
        ELEMENTS.add(new ArmorHudElement());
        ELEMENTS.add(new InventoryHudElement());
    }

    public static void render(DrawContext context, float tickDelta, boolean editorMode) {
        if (!VibeVisualsConfigManager.get().hudEnabled) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();

        float hudScale = getHudScale();
        context.getMatrices().pushMatrix();
        context.getMatrices().scale(hudScale, hudScale);
        try {
            for (HudElement element : ELEMENTS) {
                if (!element.isEnabled()) {
                    continue;
                }

                renderElement(context, client, tickDelta, editorMode, element);
            }
        } finally {
            context.getMatrices().popMatrix();
        }
    }

    private static void renderElement(DrawContext context, MinecraftClient client, float tickDelta, boolean editorMode, HudElement element) {
        if (editorMode || !VibeVisualsConfigManager.get().hudAnimations.enabled) {
            element.render(context, client, tickDelta, editorMode);
            APPEAR_PROGRESS.put(element.getId(), 1.0f);
            return;
        }

        float target = element.isVisibleForInteraction(client, false) ? 1.0f : 0.0f;
        float current = APPEAR_PROGRESS.getOrDefault(element.getId(), target);
        float speed = VibeVisualsConfigManager.get().hudAnimations.speed;
        current += (target - current) * speed;
        if (target > 0.0f && current < 0.01f) {
            current = 0.01f;
        }
        APPEAR_PROGRESS.put(element.getId(), current);

        if (current <= 0.01f && target <= 0.0f) {
            return;
        }

        float scale = VibeVisualsConfigManager.get().hudAnimations.startScale
                + (1.0f - VibeVisualsConfigManager.get().hudAnimations.startScale) * current;
        float slide = (1.0f - current) * VibeVisualsConfigManager.get().hudAnimations.slideDistance;
        int originX = element.getX() + element.getWidth() / 2;
        int originY = element.getY() + element.getHeight() / 2;
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(originX, originY + slide);
        context.getMatrices().scale(scale, scale);
        context.getMatrices().translate(-originX, -originY);
        element.render(context, client, tickDelta, editorMode);
        context.getMatrices().popMatrix();
    }

    public static float getHudScale() {
        return Math.max(0.25f, Math.min(3.0f, VibeVisualsConfigManager.get().hudScale));
    }

    public static List<HudElement> getElements() {
        return Collections.unmodifiableList(ELEMENTS);
    }

    public static void saveElementPosition(HudElement element, int x, int y) {
        switch (element.getId()) {
            case "top_status" -> {
                VibeVisualsConfigManager.get().topBar.x = x;
                VibeVisualsConfigManager.get().topBar.y = y;
            }
            case "potions" -> {
                VibeVisualsConfigManager.get().potionsCard.x = x;
                VibeVisualsConfigManager.get().potionsCard.y = y;
            }
            case "cooldowns" -> {
                VibeVisualsConfigManager.get().cooldownsCard.x = x;
                VibeVisualsConfigManager.get().cooldownsCard.y = y;
            }
            case "hot_keys" -> {
                VibeVisualsConfigManager.get().hotKeysCard.x = x;
                VibeVisualsConfigManager.get().hotKeysCard.y = y;
            }
            case "pvp_combat" -> {
                VibeVisualsConfigManager.get().pvpCard.x = x;
                VibeVisualsConfigManager.get().pvpCard.y = y;
            }
            case "armor_hud" -> {
                VibeVisualsConfigManager.get().armorHud.x = x;
                VibeVisualsConfigManager.get().armorHud.y = y;
            }
            case "inventory_hud" -> {
                VibeVisualsConfigManager.get().inventoryHud.x = x;
                VibeVisualsConfigManager.get().inventoryHud.y = y;
            }
            default -> {
                return;
            }
        }
    }

    public static Object getElementConfig(HudElement element) {
        return getElementConfigFrom(VibeVisualsConfigManager.get(), element.getId());
    }

    public static Object getElementConfigFrom(ru.suppelemen.vibevisuals.config.VibeVisualsConfig config, String elementId) {
        return switch (elementId) {
            case "top_status" -> config.topBar;
            case "potions" -> config.potionsCard;
            case "cooldowns" -> config.cooldownsCard;
            case "hot_keys" -> config.hotKeysCard;
            case "pvp_combat" -> config.pvpCard;
            case "armor_hud" -> config.armorHud;
            case "inventory_hud" -> config.inventoryHud;
            default -> null;
        };
    }
}
