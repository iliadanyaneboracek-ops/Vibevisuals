package ru.suppelemen.vibevisuals.core.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;
import ru.suppelemen.vibevisuals.feature.hud.ArmorHudElement;
import ru.suppelemen.vibevisuals.feature.hud.CardTestHudElement;
import ru.suppelemen.vibevisuals.feature.hud.HotKeysHudElement;
import ru.suppelemen.vibevisuals.feature.hud.PvpCombatHudElement;
import ru.suppelemen.vibevisuals.feature.hud.TopStatusHudElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class HudManager {
    private static final List<HudElement> ELEMENTS = new ArrayList<>();
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
        ELEMENTS.add(new HotKeysHudElement());
        ELEMENTS.add(new PvpCombatHudElement());
        ELEMENTS.add(new ArmorHudElement());
    }

    public static void render(DrawContext context, float tickDelta, boolean editorMode) {
        if (!VibeVisualsConfigManager.get().hudEnabled) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();

        for (HudElement element : ELEMENTS) {
            if (!element.isEnabled()) {
                continue;
            }

            element.render(context, client, tickDelta, editorMode);
        }
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
            case "hot_keys" -> config.hotKeysCard;
            case "pvp_combat" -> config.pvpCard;
            case "armor_hud" -> config.armorHud;
            default -> null;
        };
    }
}
