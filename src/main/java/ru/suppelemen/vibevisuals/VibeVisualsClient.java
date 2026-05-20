package ru.suppelemen.vibevisuals;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;
import ru.suppelemen.vibevisuals.core.hud.HudManager;

public class VibeVisualsClient implements ClientModInitializer {
    public static final String MOD_ID = "vibevisuals";

    @Override
    public void onInitializeClient() {
        VibeVisualsConfigManager.load();
        HudManager.init();

        HudElementRegistry.addLast(
                Identifier.of(MOD_ID, "main_hud"),
                (DrawContext context, RenderTickCounter tickCounter) -> {
                    MinecraftClient client = MinecraftClient.getInstance();

                    if (client.getDebugHud().shouldShowDebugHud()) {
                        return;
                    }

                    if (client.currentScreen != null && !(client.currentScreen instanceof ChatScreen)) {
                        return;
                    }

                    HudManager.render(context, 0.0f, false);
                }
        );

        System.out.println("[vibevisuals] Fresh baseline initialized");
    }
}
