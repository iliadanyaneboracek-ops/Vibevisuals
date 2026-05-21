package ru.suppelemen.vibevisuals;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;
import ru.suppelemen.vibevisuals.core.hud.HudManager;
import ru.suppelemen.vibevisuals.feature.pvp.PvpCombatTracker;

public class VibeVisualsClient implements ClientModInitializer {
    public static final String MOD_ID = "vibevisuals";
    private static KeyBinding reloadConfigKey;

    @Override
    public void onInitializeClient() {
        VibeVisualsConfigManager.load();
        HudManager.init();
        registerConfigReloadKey();
        registerPvpCombatHooks();

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

    private static void registerConfigReloadKey() {
        reloadConfigKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.vibevisuals.reload_config",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F8,
                KeyBinding.Category.create(Identifier.of(MOD_ID, "controls"))
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (reloadConfigKey.wasPressed()) {
                VibeVisualsConfigManager.load();
                HudManager.reload();

                if (client.player != null) {
                    client.player.sendMessage(Text.literal("VibeVisuals config reloaded"), true);
                }
            }
        });
    }

    private static void registerPvpCombatHooks() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient() && entity instanceof PlayerEntity target && player != target) {
                PvpCombatTracker.startCombat(target);
            }

            return ActionResult.PASS;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            PvpCombatTracker.clearIfExpired();
            PvpCombatTracker.tick(client);
        });
    }

    public static KeyBinding getReloadConfigKey() {
        return reloadConfigKey;
    }
}
