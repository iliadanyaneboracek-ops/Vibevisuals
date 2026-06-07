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
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;
import ru.suppelemen.vibevisuals.core.hud.HudManager;
import ru.suppelemen.vibevisuals.feature.keybind.FullBrightController;
import ru.suppelemen.vibevisuals.feature.keybind.MultiKeyBindingManager;
import ru.suppelemen.vibevisuals.feature.marker.MarkerManager;
import ru.suppelemen.vibevisuals.feature.pvp.PvpCombatTracker;
import ru.suppelemen.vibevisuals.feature.pvp.ShiftUpController;
import ru.suppelemen.vibevisuals.feature.screen.MarkersScreen;
import ru.suppelemen.vibevisuals.feature.sound.CustomHitSoundPlayer;
import ru.suppelemen.vibevisuals.feature.pvp.TotemCounter;
import ru.suppelemen.vibevisuals.feature.utility.AutoEatController;
import ru.suppelemen.vibevisuals.feature.utility.AutoLeaveController;
import ru.suppelemen.vibevisuals.feature.utility.ZoomController;
import ru.suppelemen.vibevisuals.feature.utility.AutoPotionController;
import ru.suppelemen.vibevisuals.feature.utility.AutoRespawnController;
import ru.suppelemen.vibevisuals.feature.utility.ItemPickupLogger;
import ru.suppelemen.vibevisuals.feature.utility.TapeMouseController;
import ru.suppelemen.vibevisuals.feature.visual.ChinaHatCosmetic;
import ru.suppelemen.vibevisuals.feature.visual.CodexWheelchairCosmetic;
import ru.suppelemen.vibevisuals.feature.visual.CombatVisualsTracker;
import ru.suppelemen.vibevisuals.feature.visual.DamageIndicators;
import ru.suppelemen.vibevisuals.feature.visual.KillEffect;
import ru.suppelemen.vibevisuals.feature.marker.MarkerManager;
import ru.suppelemen.vibevisuals.feature.visual.MaceShockwave;
import ru.suppelemen.vibevisuals.feature.visual.MoggedOverlay;
import ru.suppelemen.vibevisuals.feature.visual.TrapHighlight;
import ru.suppelemen.vibevisuals.feature.visual.ProjectilePrediction;
import ru.suppelemen.vibevisuals.feature.visual.TargetEsp;
import ru.suppelemen.vibevisuals.feature.visual.VisualEffects;

public class VibeVisualsClient implements ClientModInitializer {
    public static final String MOD_ID = "vibevisuals";
    private static final KeyBinding.Category CONTROLS_CATEGORY = KeyBinding.Category.create(Identifier.of(MOD_ID, "controls"));
    private static KeyBinding reloadConfigKey;
    private static KeyBinding fullBrightKey;
    private static KeyBinding markersMenuKey;
    private static KeyBinding markPullKey;
    private static KeyBinding zoomKey;
    private static boolean wasInWorld;

    @Override
    public void onInitializeClient() {
        VibeVisualsConfigManager.load();
        MarkerManager.load();
        ru.suppelemen.vibevisuals.config.ConfigShareManager.init();
        // Apply the menu palette early so the HUD (which uses it now) picks the
        // right theme before the first frame instead of waiting for the user
        // to open the ClickGUI.
        ru.suppelemen.vibevisuals.theme.MenuTheme.applyAccent(
                VibeVisualsConfigManager.get().menu.accent);
        ru.suppelemen.vibevisuals.theme.MenuTheme.applyTheme(
                VibeVisualsConfigManager.get().menu.theme);
        // Bake the smooth-text atlas + try to force a LINEAR sampler on it.
        // Atlas dumps to .minecraft/config/vibevisuals/smoothfont-atlas-debug.png
        // for visual verification; sampler hook result is printed to the log.
        ru.suppelemen.vibevisuals.util.font.SmoothFontTexture.ensureInitialised();
        CustomHitSoundPlayer.init();
        HudManager.init();
        registerConfigReloadKey();
        registerFullBrightKey();
        registerMarkersMenuKey();
        registerTrapKeys();
        registerZoomKey();
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
                GLFW.GLFW_KEY_LEFT_ALT,
                CONTROLS_CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (markersMenuKey.wasPressed()) {
                client.setScreen(new MarkersScreen());
            }
        });
    }

    private static void registerTrapKeys() {
        markPullKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.vibevisuals.mark_pull",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                CONTROLS_CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Hold to drag an area selection; tap to toggle a single block.
            TrapHighlight.handleSelectKey(client, markPullKey.isPressed());
        });
    }

    private static void registerZoomKey() {
        zoomKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.vibevisuals.zoom",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_C,
                CONTROLS_CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ZoomController.setActive(zoomKey.isPressed());
            ZoomController.tick(client);

            boolean inWorld = client.world != null && client.player != null;
            if (wasInWorld && !inWorld) {
                TotemCounter.reset();
            }
            wasInWorld = inWorld;
        });
    }

    private static void registerMultiKeyBindings() {
        ClientTickEvents.END_CLIENT_TICK.register(MultiKeyBindingManager::tick);
    }

    private static void registerUtilityTick() {
        ClientTickEvents.END_CLIENT_TICK.register(AutoEatController::tick);
        ClientTickEvents.END_CLIENT_TICK.register(AutoPotionController::tick);
        ClientTickEvents.END_CLIENT_TICK.register(AutoRespawnController::tick);
        ClientTickEvents.END_CLIENT_TICK.register(AutoLeaveController::tick);
        ClientTickEvents.END_CLIENT_TICK.register(TapeMouseController::tick);
        ClientTickEvents.END_CLIENT_TICK.register(ItemPickupLogger::tick);
    }

    private static void registerPvpCombatHooks() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient() && entity instanceof PlayerEntity target && player != target) {
                PvpCombatTracker.startCombat(target);
                MoggedOverlay.onHit(target);
                if (isCriticalHit(player)) {
                    CustomHitSoundPlayer.playCrit();
                    ShiftUpController.onCritHit();
                }
            }

            if (world.isClient() && entity != null && player != entity) {
                tryMaceShockwave(player, entity);
                // Other players' real health isn't synced to the client, so a
                // health-delta read can't see PvP damage. Estimate from the
                // attack instead — works for players and mobs alike.
                boolean crit = isCriticalHit(player);
                DamageIndicators.spawn(entity, estimateAttackDamage(player, crit), crit);
                CombatVisualsTracker.onAttack(player, entity);
            }

            return ActionResult.PASS;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            PvpCombatTracker.clearIfExpired();
            PvpCombatTracker.tick(client);
            ShiftUpController.tick(client);
            CombatVisualsTracker.tick(client);
            KillEffect.tick(client);
            TrapHighlight.tick(client);
        });
    }

    private static void registerVisualEffectsTick() {
        // TEMPORARILY DISABLED — world-space overlays (ESP ring, projectile trails,
        // markers, Mogged banner) are off. Their classes stay so we can re-enable
        // them on the new design system. Ticks for VisualEffects (particles, sky,
        // fog) are config-gated already so they stay registered.
        ClientTickEvents.END_CLIENT_TICK.register(VisualEffects::tick);
        // ClientTickEvents.END_CLIENT_TICK.register(ProjectilePrediction::tick);
        // ClientTickEvents.END_CLIENT_TICK.register(TargetEsp::tick);
        WorldRenderEvents.AFTER_ENTITIES.register(CodexWheelchairCosmetic::render);
        WorldRenderEvents.AFTER_ENTITIES.register(ChinaHatCosmetic::render);
        WorldRenderEvents.AFTER_ENTITIES.register(MaceShockwave::render);
        WorldRenderEvents.AFTER_ENTITIES.register(DamageIndicators::render);
        WorldRenderEvents.AFTER_ENTITIES.register(KillEffect::render);
        WorldRenderEvents.AFTER_ENTITIES.register(MoggedOverlay::render);
        WorldRenderEvents.AFTER_ENTITIES.register(MarkerManager::render);
        WorldRenderEvents.AFTER_ENTITIES.register(TrapHighlight::render);
        // WorldRenderEvents.AFTER_ENTITIES.register(ProjectilePrediction::render);
        // WorldRenderEvents.AFTER_ENTITIES.register(TargetEsp::render);
        // WorldRenderEvents.AFTER_ENTITIES.register(MarkerManager::render);
        // WorldRenderEvents.AFTER_ENTITIES.register(MoggedOverlay::render);
    }

    private static boolean isCriticalHit(PlayerEntity player) {
        return player.fallDistance > 0.0f && !player.isOnGround();
    }

    /**
     * Client estimate of the outgoing melee damage for the Damage Numbers
     * overlay: the player's attack-damage attribute (weapon + Strength) times
     * the vanilla 1.5× crit multiplier. Doesn't subtract the target's armour
     * (the client can't know it reliably) — it's a cosmetic readout, not exact.
     */
    private static float estimateAttackDamage(PlayerEntity player, boolean crit) {
        double base;
        try {
            base = player.getAttributeValue(net.minecraft.entity.attribute.EntityAttributes.ATTACK_DAMAGE);
        } catch (Throwable ignored) {
            base = 1.0;
        }
        if (crit) {
            base *= 1.5;
        }
        return (float) base;
    }

    /**
     * Mace SMASH detection: the player struck an entity with a mace while
     * falling far enough that vanilla would apply the smash bonus. Fires a
     * client-side shockwave ripple at the target's feet.
     */
    private static void tryMaceShockwave(PlayerEntity player, net.minecraft.entity.Entity target) {
        VibeVisualsConfig.MaceShockwaveConfig config = VibeVisualsConfigManager.get().maceShockwave;
        if (!config.enabled) {
            return;
        }
        if (player.getMainHandStack().getItem() != net.minecraft.item.Items.MACE) {
            return;
        }
        if (player.isOnGround() || player.fallDistance < config.minFallDistance) {
            return;
        }
        MaceShockwave.spawn(target.getLerpedPos(1.0f), estimateSmashDamage(player));
    }

    /**
     * Client-side estimate of the mace SMASH damage: the player's attack-damage
     * attribute (includes the mace's bonus + Strength etc.) plus the vanilla
     * fall-distance smash bonus (4/blk first 3, 2/blk next 5, 1/blk beyond).
     * Used only to size the cosmetic shockwave — not gameplay-authoritative.
     */
    private static float estimateSmashDamage(PlayerEntity player) {
        double base;
        try {
            base = player.getAttributeValue(net.minecraft.entity.attribute.EntityAttributes.ATTACK_DAMAGE);
        } catch (Throwable ignored) {
            base = 6.0;
        }
        float f = (float) player.fallDistance;
        double smashBonus;
        if (f <= 3.0f) {
            smashBonus = 4.0 * f;
        } else if (f <= 8.0f) {
            smashBonus = 12.0 + 2.0 * (f - 3.0);
        } else {
            smashBonus = 22.0 + 1.0 * (f - 8.0);
        }
        return (float) (base + smashBonus);
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
