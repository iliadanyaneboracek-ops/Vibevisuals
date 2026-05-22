package ru.suppelemen.vibevisuals.feature.keybind;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;
import ru.suppelemen.vibevisuals.core.hud.HudManager;
import ru.suppelemen.vibevisuals.feature.screen.HudEditorScreen;
import ru.suppelemen.vibevisuals.feature.screen.VibeVisualsMenuScreen;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public enum ModAction {
    RELOAD_CONFIG("reload_config", "Reload Config", client -> {
        VibeVisualsConfigManager.load();
        HudManager.reload();
        notifyPlayer(client, "VibeVisuals config reloaded");
    }),
    TOGGLE_FULLBRIGHT("toggle_fullbright", "Toggle FullBright", FullBrightController::toggle),
    OPEN_MENU("open_menu", "Open VibeVisuals Menu", client -> client.setScreen(new VibeVisualsMenuScreen())),
    OPEN_HUD_EDITOR("open_hud_editor", "Open HUD Editor", client -> client.setScreen(new HudEditorScreen())),
    TOGGLE_HUD("toggle_hud", "Toggle Master HUD", client -> {
        VibeVisualsConfig config = VibeVisualsConfigManager.get();
        config.hudEnabled = !config.hudEnabled;
        VibeVisualsConfigManager.save();
        notifyPlayer(client, "HUD " + onOff(config.hudEnabled));
    }),
    TOGGLE_POTIONS_CARD("toggle_potions_card", "Toggle Potions Card", client -> toggleCard(client, "Potions",
            () -> VibeVisualsConfigManager.get().potionsCard.enabled,
            value -> VibeVisualsConfigManager.get().potionsCard.enabled = value)),
    TOGGLE_COOLDOWNS_CARD("toggle_cooldowns_card", "Toggle Cooldowns Card", client -> toggleCard(client, "Cooldowns",
            () -> VibeVisualsConfigManager.get().cooldownsCard.enabled,
            value -> VibeVisualsConfigManager.get().cooldownsCard.enabled = value)),
    TOGGLE_HOTKEYS_CARD("toggle_hotkeys_card", "Toggle Hot Keys Card", client -> toggleCard(client, "Hot Keys",
            () -> VibeVisualsConfigManager.get().hotKeysCard.enabled,
            value -> VibeVisualsConfigManager.get().hotKeysCard.enabled = value)),
    TOGGLE_TOP_BAR("toggle_top_bar", "Toggle Top Bar", client -> toggleCard(client, "Top Bar",
            () -> VibeVisualsConfigManager.get().topBar.enabled,
            value -> VibeVisualsConfigManager.get().topBar.enabled = value)),
    TOGGLE_INVENTORY_HUD("toggle_inventory_hud", "Toggle Inventory HUD", client -> toggleCard(client, "Inventory HUD",
            () -> VibeVisualsConfigManager.get().inventoryHud.enabled,
            value -> VibeVisualsConfigManager.get().inventoryHud.enabled = value)),
    TOGGLE_ARMOR_HUD("toggle_armor_hud", "Toggle Armor HUD", client -> toggleCard(client, "Armor HUD",
            () -> VibeVisualsConfigManager.get().armorHud.enabled,
            value -> VibeVisualsConfigManager.get().armorHud.enabled = value)),
    TOGGLE_HOTBAR("toggle_hotbar", "Toggle Custom Hotbar", client -> toggleCard(client, "Custom Hotbar",
            () -> VibeVisualsConfigManager.get().hotbar.enabled,
            value -> VibeVisualsConfigManager.get().hotbar.enabled = value)),
    TOGGLE_PVP_CARD("toggle_pvp_card", "Toggle PvP Card", client -> toggleCard(client, "PvP",
            () -> VibeVisualsConfigManager.get().pvpCard.enabled,
            value -> VibeVisualsConfigManager.get().pvpCard.enabled = value)),
    TOGGLE_FIRE_OVERLAY("toggle_fire_overlay", "Toggle Fire Overlay", client -> toggleCard(client, "Fire Overlay",
            () -> VibeVisualsConfigManager.get().fireOverlay.enabled,
            value -> VibeVisualsConfigManager.get().fireOverlay.enabled = value)),
    TOGGLE_PROJECTILE_PREDICTION("toggle_projectile_prediction", "Toggle Projectile Prediction",
            client -> toggleCard(client, "Projectile Prediction",
                    () -> VibeVisualsConfigManager.get().projectilePrediction.enabled,
                    value -> VibeVisualsConfigManager.get().projectilePrediction.enabled = value)),
    TOGGLE_AUTO_EAT("toggle_auto_eat", "Toggle AutoEat", client -> toggleCard(client, "AutoEat",
            () -> VibeVisualsConfigManager.get().autoEat.enabled,
            value -> VibeVisualsConfigManager.get().autoEat.enabled = value)),
    TOGGLE_AUTO_POTION("toggle_auto_potion", "Toggle AutoPotion", client -> toggleCard(client, "AutoPotion",
            () -> VibeVisualsConfigManager.get().autoPotion.enabled,
            value -> VibeVisualsConfigManager.get().autoPotion.enabled = value)),
    TOGGLE_AUTO_RESPAWN("toggle_auto_respawn", "Toggle AutoRespawn", client -> toggleCard(client, "AutoRespawn",
            () -> VibeVisualsConfigManager.get().autoRespawn.enabled,
            value -> VibeVisualsConfigManager.get().autoRespawn.enabled = value)),
    TOGGLE_TAPE_MOUSE("toggle_tape_mouse", "Toggle Tape Mouse", client -> toggleCard(client, "Tape Mouse",
            () -> VibeVisualsConfigManager.get().tapeMouse.enabled,
            value -> VibeVisualsConfigManager.get().tapeMouse.enabled = value)),
    TOGGLE_VFX_PARTICLES("toggle_vfx_particles", "Toggle Custom Particles", client -> toggleCard(client, "Custom Particles",
            () -> VibeVisualsConfigManager.get().visualEffects.customParticlesEnabled,
            value -> VibeVisualsConfigManager.get().visualEffects.customParticlesEnabled = value)),
    TOGGLE_VFX_SKY("toggle_vfx_sky", "Toggle Sky Tint", client -> toggleCard(client, "Sky Tint",
            () -> VibeVisualsConfigManager.get().visualEffects.skyColorEnabled,
            value -> VibeVisualsConfigManager.get().visualEffects.skyColorEnabled = value)),
    TOGGLE_VFX_FOG("toggle_vfx_fog", "Toggle Fog Tint", client -> toggleCard(client, "Fog Tint",
            () -> VibeVisualsConfigManager.get().visualEffects.fogColorEnabled,
            value -> VibeVisualsConfigManager.get().visualEffects.fogColorEnabled = value));

    private static final Map<String, ModAction> BY_ID = buildIdMap();

    private final String id;
    private final String displayName;
    private final Consumer<MinecraftClient> handler;

    ModAction(String id, String displayName, Consumer<MinecraftClient> handler) {
        this.id = id;
        this.displayName = displayName;
        this.handler = handler;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public void execute(MinecraftClient client) {
        if (client == null) {
            return;
        }
        handler.accept(client);
    }

    public static ModAction fromId(String id) {
        if (id == null) {
            return null;
        }
        return BY_ID.get(id);
    }

    private static Map<String, ModAction> buildIdMap() {
        Map<String, ModAction> map = new HashMap<>();
        for (ModAction action : values()) {
            map.put(action.id, action);
        }
        return map;
    }

    private static void toggleCard(MinecraftClient client, String label,
                                   java.util.function.BooleanSupplier getter,
                                   java.util.function.Consumer<Boolean> setter) {
        boolean next = !getter.getAsBoolean();
        setter.accept(next);
        VibeVisualsConfigManager.save();
        notifyPlayer(client, label + " " + onOff(next));
    }

    private static void notifyPlayer(MinecraftClient client, String message) {
        if (client.player != null) {
            client.player.sendMessage(Text.literal(message), true);
        }
    }

    private static String onOff(boolean value) {
        return value ? "enabled" : "disabled";
    }
}
