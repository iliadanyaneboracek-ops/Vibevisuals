package ru.suppelemen.vibevisuals.core.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;
import ru.suppelemen.vibevisuals.feature.hud.CardTestHudElement;

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

        ELEMENTS.clear();

        // Start with one test card while the rest of the HUD is still being wired up.
        ELEMENTS.add(new CardTestHudElement());

        initialized = true;
        System.out.println("[vibevisuals] HUD initialized, elements=" + ELEMENTS.size());
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
}
