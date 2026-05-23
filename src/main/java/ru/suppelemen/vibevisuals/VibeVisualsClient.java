package ru.suppelemen.vibevisuals;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
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
import ru.suppelemen.vibevisuals.feature.keybind.FullBrightController;
import ru.suppelemen.vibevisuals.feature.keybind.MultiKeyBindingManager;
import ru.suppelemen.vibevisuals.feature.marker.MarkerManager;
import ru.suppelemen.vibevisuals.feature.pvp.PvpCombatTracker;
import ru.suppelemen.vibevisuals.feature.pvp.ShiftUpController;
import ru.suppelemen.vibevisuals.feature.screen.MarkersScreen;
import ru.suppelemen.vibevisuals.feature.sound.CustomHitSoundPlayer;
import ru.suppelemen.vibevisuals.feature.utility.AutoEatController;
import ru.suppelemen.vibevisuals.feature.utility.AutoPotionController;
import ru.suppelemen.vibevisuals.feature.utility.AutoRespawnController;
import ru.suppelemen.vibevisuals.feature.utility.ItemPickupLogger;
import ru.suppelemen.vibevisuals.feature.utility.TapeMouseController;
import ru.suppelemen.vibevisuals.feature.visual.ProjectilePrediction;
import ru.suppelemen.vibevisuals.feature.visual.TargetEsp;
import ru.suppelemen.vibevisuals.feature.visual.VisualEffects;

public class VibeVisualsClient implements ClientModInitializer {
    public static final String MOD_ID = "vibevisuals";
    private static final KeyBinding.Category CONTROLS_CATEGORY = KeyBinding.Category.create(Identifier.of(MOD_ID, "controls"));
    private static KeyBinding reloadConfigKey;
    private static KeyBinding fullBrightKey;
    private static KeyBinding markersMenuKey;

    @Override
    public void onInitializeClient() {
        VibeVisualsConfigManager.load();
        CustomHitSoundPlayer.init();
        HudManager.init();
        registerConfigReloadKey();
        registerFullBrightKey();
        registerMarkersMenuKey();
        registerPvpCombatHooks();
        registerVisualEffectsTick();
        registerMultiKeyBindings();
        registerUtilityTick();

        HudElementRegistry.addLast(
                Identifier.of(MOD_ID, "main_hud"),
                (DrawContext context, RenderTickCounter tickCounter) -> {
                    MinecraftClient client = MinecraftClient.getInstance();

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
                CONTROLS_CATEGORY
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

    private static void registerFullBrightKey() {
        fullBrightKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.vibevisuals.fullbright",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                CONTROLS_CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (fullBrightKey.wasPressed()) {
                FullBrightController.toggle(client);
            }
        });
    }

    private static void registerMarkersMenuKey() {
        markersMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.vibevisuals.markers_menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_N,
                CONTROLS_CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (markersMenuKey.wasPressed()) {
                client.setScreen(new MarkersScreen());
            }
        });
    }

    private static void registerMultiKeyBindings() {
        ClientTickEvents.END_CLIENT_TICK.register(MultiKeyBindingManager::tick);
    }

    private static void registerUtilityTick() {
        ClientTickEvents.END_CLIENT_TICK.register(AutoEatController::tick);
        ClientTickEvents.END_CLIENT_TICK.register(AutoPotionController::tick);
        ClientTickEvents.END_CLIENT_TICK.register(AutoRespawnController::tick);
        ClientTickEvents.END_CLIENT_TICK.register(TapeMouseController::tick);
        ClientTickEvents.END_CLIENT_TICK.register(ItemPickupLogger::tick);
    }

    private static void registerPvpCombatHooks() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient() && entity instanceof PlayerEntity target && player != target) {
                PvpCombatTracker.startCombat(target);
                if (isCriticalHit(player)) {
                    CustomHitSoundPlayer.playCrit();
                    ShiftUpController.onCritHit();
                }
            }

            return ActionResult.PASS;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            PvpCombatTracker.clearIfExpired();
            PvpCombatTracker.tick(client);
            ShiftUpController.tick(client);
        });
    }

    private static void registerVisualEffectsTick() {
        ClientTickEvents.END_CLIENT_TICK.register(VisualEffects::tick);
        ClientTickEvents.END_CLIENT_TICK.register(ProjectilePrediction::tick);
        ClientTickEvents.END_CLIENT_TICK.register(TargetEsp::tick);
        WorldRenderEvents.AFTER_ENTITIES.register(ProjectilePrediction::render);
        WorldRenderEvents.AFTER_ENTITIES.register(TargetEsp::render);
        WorldRenderEvents.AFTER_ENTITIES.register(MarkerManager::render);
    }

    private static boolean isCriticalHit(PlayerEntity player) {
        return player.fallDistance > 0.0f && !player.isOnGround();
    }

    public static KeyBinding getReloadConfigKey() {
        return reloadConfigKey;
    }

    public static KeyBinding getFullBrightKey() {
        return fullBrightKey;
    }

    public static KeyBinding getMarkersMenuKey() {
        return markersMenuKey;
    }
}
