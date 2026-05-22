package ru.suppelemen.vibevisuals.config;

import org.lwjgl.glfw.GLFW;
import ru.suppelemen.vibevisuals.feature.keybind.KeyStroke;
import ru.suppelemen.vibevisuals.feature.keybind.ModAction;
import ru.suppelemen.vibevisuals.feature.keybind.MultiKeyBinding;
import ru.suppelemen.vibevisuals.theme.HudCardRenderType;

import java.util.ArrayList;
import java.util.List;

public class VibeVisualsConfig {
    private static final int CURRENT_CONFIG_VERSION = 2;
    private static final int LEGACY_DEFAULT_MAX_EFFECTS = 3;

    public int configVersion = CURRENT_CONFIG_VERSION;
    public boolean hudEnabled = true;
    public float hudScale = 1.0f;
    public float fullBrightStrength = 1.0f;
    public HudAnimationConfig hudAnimations = HudAnimationConfig.defaults();

    public CardConfig potionsCard = CardConfig.potionsDefaults();
    public CardConfig cooldownsCard = CardConfig.cooldownsDefaults();
    public TopBarConfig topBar = TopBarConfig.defaults();
    public HotKeysConfig hotKeysCard = HotKeysConfig.defaults();
    public PvpCardConfig pvpCard = PvpCardConfig.defaults();
    public ArmorHudConfig armorHud = ArmorHudConfig.defaults();
    public InventoryHudConfig inventoryHud = InventoryHudConfig.defaults();
    public HotbarConfig hotbar = HotbarConfig.defaults();
    public FireOverlayConfig fireOverlay = FireOverlayConfig.defaults();
    public ProjectilePredictionConfig projectilePrediction = ProjectilePredictionConfig.defaults();
    public VisualEffectsConfig visualEffects = VisualEffectsConfig.defaults();
    public MultiKeyBindingsConfig multiKeyBindings = MultiKeyBindingsConfig.defaults();

    public void validate() {
        if (potionsCard == null) {
            potionsCard = CardConfig.potionsDefaults();
        }

        if (topBar == null) {
            topBar = TopBarConfig.defaults();
        }

        if (cooldownsCard == null) {
            cooldownsCard = CardConfig.cooldownsDefaults();
        }

        if (hotKeysCard == null) {
            hotKeysCard = HotKeysConfig.defaults();
        }

        if (pvpCard == null) {
            pvpCard = PvpCardConfig.defaults();
        }

        if (armorHud == null) {
            armorHud = ArmorHudConfig.defaults();
        }

        if (inventoryHud == null) {
            inventoryHud = InventoryHudConfig.defaults();
        }

        if (hotbar == null) {
            hotbar = HotbarConfig.defaults();
        }

        if (fireOverlay == null) {
            fireOverlay = FireOverlayConfig.defaults();
        }

        if (projectilePrediction == null) {
            projectilePrediction = ProjectilePredictionConfig.defaults();
        }

        if (hudAnimations == null) {
            hudAnimations = HudAnimationConfig.defaults();
        }

        if (visualEffects == null) {
            visualEffects = VisualEffectsConfig.defaults();
        }

        if (multiKeyBindings == null) {
            multiKeyBindings = MultiKeyBindingsConfig.defaults();
        }

        if (configVersion < CURRENT_CONFIG_VERSION && potionsCard.maxEffects == LEGACY_DEFAULT_MAX_EFFECTS) {
            potionsCard.maxEffects = CardConfig.DEFAULT_MAX_EFFECTS;
        }

        potionsCard.validate();
        cooldownsCard.validate();
        topBar.validate();
        hotKeysCard.validate();
        pvpCard.validate();
        armorHud.validate();
        inventoryHud.validate();
        hotbar.validate();
        fireOverlay.validate();
        projectilePrediction.validate();
        hudAnimations.validate();
        visualEffects.validate();
        multiKeyBindings.validate();
        hudScale = clamp(hudScale, 0.25f, 3.0f);
        fullBrightStrength = clamp(fullBrightStrength, 0.0f, 1.0f);
        configVersion = CURRENT_CONFIG_VERSION;
    }

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static class CardConfig {
        public static final int DEFAULT_MAX_EFFECTS = 64;

        public boolean enabled = true;

        public int x = 7;
        public int y = 11;
        public int width = 110;
        public int height = 20;

        public float radius = 8.0f;
        public float opacity = 0.70f;
        public boolean glow = true;
        public boolean blur = false;
        public HudCardRenderType renderType = HudCardRenderType.LIQUID_GLASS;

        public int titleColor = 0xFFEFEFF6;
        public int subtitleColor = 0xFFD7DAE8;
        public int timerColor = 0xFFB7BBC9;
        public int titleBarColor = 0xFF050710;
        public float titleBarOpacity = 0.28f;

        public float textScale = 0.26f;
        public float effectTextScale = 0.20f;
        public float timerTextScale = 0.20f;
        public int iconSize = 8;
        public int titleIconSize = 10;
        public int titleIconXOffset = 0;
        public int effectIconYOffset = -1;
        public int titleIconYOffset = -2;
        public int titleTextXOffset = 0;
        public int titleTextYOffset = 0;
        public int timerXOffset = 0;
        public int timerYOffset = 0;
        public int padding = 9;
        public int titleY = 3;
        public int titleBarHeight = 13;
        public int effectsStartY = 18;
        public int rowGap = 4;
        public int bottomPadding = 3;
        public int maxEffects = DEFAULT_MAX_EFFECTS;

        public static CardConfig potionsDefaults() {
            return new CardConfig();
        }

        public static CardConfig cooldownsDefaults() {
            CardConfig config = new CardConfig();
            config.x = 336;
            config.y = 300;
            config.width = 96;
            config.height = 20;
            config.maxEffects = 16;
            return config;
        }

        public void validate() {
            width = Math.max(24, width);
            height = Math.max(16, height);
            radius = clamp(radius, 0.0f, 24.0f);
            opacity = clamp(opacity, 0.0f, 1.0f);
            titleBarOpacity = clamp(titleBarOpacity, 0.0f, 1.0f);
            textScale = clamp(textScale, 0.10f, 2.0f);
            effectTextScale = clamp(effectTextScale, 0.10f, 1.50f);
            timerTextScale = clamp(timerTextScale, 0.10f, 1.50f);
            iconSize = Math.max(3, Math.min(32, iconSize));
            titleIconSize = Math.max(3, Math.min(32, titleIconSize));
            titleIconXOffset = Math.max(-32, Math.min(32, titleIconXOffset));
            effectIconYOffset = Math.max(-16, Math.min(16, effectIconYOffset));
            titleIconYOffset = Math.max(-16, Math.min(16, titleIconYOffset));
            titleTextXOffset = Math.max(-32, Math.min(32, titleTextXOffset));
            titleTextYOffset = Math.max(-16, Math.min(16, titleTextYOffset));
            timerXOffset = Math.max(-64, Math.min(64, timerXOffset));
            timerYOffset = Math.max(-16, Math.min(16, timerYOffset));
            padding = Math.max(2, Math.min(32, padding));
            titleY = Math.max(2, Math.min(64, titleY));
            titleBarHeight = Math.max(titleY + 4, Math.min(96, titleBarHeight));
            effectsStartY = Math.max(titleY + 4, Math.min(96, effectsStartY));
            rowGap = Math.max(4, Math.min(40, rowGap));
            bottomPadding = Math.max(2, Math.min(32, bottomPadding));
            maxEffects = Math.max(1, Math.min(128, maxEffects));

            if (renderType == null) {
                renderType = HudCardRenderType.LIQUID_GLASS;
            }
        }
    }

    public static class TopBarConfig {
        public boolean enabled = true;

        public int x = -1;
        public int y = 6;
        public int width = 150;
        public int height = 15;
        public float radius = 8.0f;
        public float opacity = 0.80f;

        public int padding = 8;
        public int iconSize = 8;
        public int iconXOffset = 0;
        public int iconYOffset = 0;
        public int textYOffset = 1;
        public int gap = 8;

        public float textScale = 0.25f;
        public int titleColor = 0xFFEFEFF6;
        public int statColor = 0xFFD7DAE8;
        public int separatorColor = 0xFF6E7282;

        public static TopBarConfig defaults() {
            return new TopBarConfig();
        }

        public void validate() {
            width = Math.max(48, Math.min(320, width));
            height = Math.max(10, Math.min(64, height));
            x = Math.max(-1, Math.min(4096, x));
            y = Math.max(0, Math.min(512, y));
            radius = clamp(radius, 0.0f, 24.0f);
            opacity = clamp(opacity, 0.0f, 1.0f);
            padding = Math.max(2, Math.min(32, padding));
            iconSize = Math.max(0, Math.min(32, iconSize));
            iconXOffset = Math.max(-32, Math.min(32, iconXOffset));
            iconYOffset = Math.max(-16, Math.min(16, iconYOffset));
            textYOffset = Math.max(-16, Math.min(16, textYOffset));
            gap = Math.max(1, Math.min(24, gap));
            textScale = clamp(textScale, 0.10f, 1.50f);
        }
    }

    public static class HotKeysConfig {
        public boolean enabled = true;

        public int x = 11;
        public int y = 115;
        public int width = 75;
        public int height = 25;
        public float radius = 7.0f;
        public float opacity = 0.70f;
        public int titleBarColor = 0xFF050710;
        public float titleBarOpacity = 0.24f;
        public int titleBarHeight = 14;

        public int padding = 8;
        public int titleY = 6;
        public int rowY = 20;
        public int rowGap = 5;
        public int bottomPadding = 4;
        public int iconSize = 7;
        public int iconXOffset = 0;
        public int iconYOffset = -2;
        public int titleTextXOffset = 0;
        public int titleTextYOffset = -1;
        public int keyTextXOffset = 0;
        public int keyTextYOffset = 0;

        public float titleTextScale = 0.24f;
        public float rowTextScale = 0.18f;
        public int titleColor = 0xFFEFEFF6;
        public int actionColor = 0xFFD7DAE8;
        public int keyColor = 0xFFEFEFF6;

        public static HotKeysConfig defaults() {
            return new HotKeysConfig();
        }

        public void validate() {
            width = Math.max(48, Math.min(240, width));
            height = Math.max(24, Math.min(160, height));
            x = Math.max(-512, Math.min(4096, x));
            y = Math.max(-512, Math.min(4096, y));
            radius = clamp(radius, 0.0f, 24.0f);
            opacity = clamp(opacity, 0.0f, 1.0f);
            titleBarOpacity = clamp(titleBarOpacity, 0.0f, 1.0f);
            titleBarHeight = Math.max(titleY + 4, Math.min(96, titleBarHeight));
            padding = Math.max(2, Math.min(32, padding));
            titleY = Math.max(2, Math.min(96, titleY));
            rowY = Math.max(titleY + 4, Math.min(128, rowY));
            rowGap = Math.max(0, Math.min(32, rowGap));
            bottomPadding = Math.max(2, Math.min(32, bottomPadding));
            iconSize = Math.max(0, Math.min(32, iconSize));
            iconXOffset = Math.max(-32, Math.min(32, iconXOffset));
            iconYOffset = Math.max(-16, Math.min(16, iconYOffset));
            titleTextXOffset = Math.max(-32, Math.min(32, titleTextXOffset));
            titleTextYOffset = Math.max(-16, Math.min(16, titleTextYOffset));
            keyTextXOffset = Math.max(-64, Math.min(64, keyTextXOffset));
            keyTextYOffset = Math.max(-16, Math.min(16, keyTextYOffset));
            titleTextScale = clamp(titleTextScale, 0.10f, 1.50f);
            rowTextScale = clamp(rowTextScale, 0.10f, 1.50f);
        }
    }

    public static class PvpCardConfig {
        public boolean enabled = true;

        public int x = 448;
        public int y = 16;
        public int width = 112;
        public int height = 44;
        public float radius = 8.0f;
        public float opacity = 0.80f;

        public int padding = 6;
        public int avatarSize = 20;
        public int avatarXOffset = 0;
        public int avatarYOffset = 0;
        public int nameXOffset = 0;
        public int nameYOffset = 3;
        public int statsXOffset = 1;
        public int statsYOffset = 3;
        public int itemY = 22;
        public int itemIconSize = 10;
        public int itemGap = 5;
        public int totemCountXOffset = 0;
        public int totemCountYOffset = 5;
        public int goldenAppleCountXOffset = 0;
        public int goldenAppleCountYOffset = 5;
        public int barY = 36;
        public int barHeight = 3;

        public float nameTextScale = 0.35f;
        public float statsTextScale = 0.25f;
        public float itemTextScale = 0.20f;
        public int nameColor = 0xFFEFEFF6;
        public int statsColor = 0xFFD7DAE8;
        public int itemColor = 0xFFEFEFF6;
        public int barColor = 0xFF7C5CFF;
        public int barBackgroundColor = 0x33262A3A;

        public static PvpCardConfig defaults() {
            return new PvpCardConfig();
        }

        public void validate() {
            width = Math.max(64, Math.min(280, width));
            height = Math.max(32, Math.min(180, height));
            x = Math.max(-512, Math.min(4096, x));
            y = Math.max(-512, Math.min(4096, y));
            radius = clamp(radius, 0.0f, 24.0f);
            opacity = clamp(opacity, 0.0f, 1.0f);
            padding = Math.max(2, Math.min(32, padding));
            avatarSize = Math.max(8, Math.min(48, avatarSize));
            avatarXOffset = Math.max(-32, Math.min(32, avatarXOffset));
            avatarYOffset = Math.max(-32, Math.min(32, avatarYOffset));
            nameXOffset = Math.max(-64, Math.min(64, nameXOffset));
            nameYOffset = Math.max(-32, Math.min(32, nameYOffset));
            statsXOffset = Math.max(-64, Math.min(64, statsXOffset));
            statsYOffset = Math.max(-32, Math.min(32, statsYOffset));
            itemY = Math.max(0, Math.min(128, itemY));
            itemIconSize = Math.max(4, Math.min(32, itemIconSize));
            itemGap = Math.max(1, Math.min(32, itemGap));
            totemCountXOffset = Math.max(-64, Math.min(64, totemCountXOffset));
            totemCountYOffset = Math.max(-32, Math.min(32, totemCountYOffset));
            goldenAppleCountXOffset = Math.max(-64, Math.min(64, goldenAppleCountXOffset));
            goldenAppleCountYOffset = Math.max(-32, Math.min(32, goldenAppleCountYOffset));
            barY = Math.max(0, Math.min(160, barY));
            barHeight = Math.max(1, Math.min(16, barHeight));
            nameTextScale = clamp(nameTextScale, 0.10f, 1.50f);
            statsTextScale = clamp(statsTextScale, 0.10f, 1.50f);
            itemTextScale = clamp(itemTextScale, 0.10f, 1.50f);
        }
    }

    public static class ArmorHudConfig {
        public boolean enabled = true;

        public int x = 391;
        public int y = 340;
        public int width = 85;
        public int height = 18;
        public float radius = 8.0f;
        public float opacity = 0.70f;

        public int padding = 5;
        public int iconSize = 14;
        public int iconGap = 5;
        public int iconYOffset = -1;
        public boolean showDurability = true;
        public int durabilityBarHeight = 2;
        public int durabilityBarYOffset = 14;
        public int durabilityColor = 0xFF7C5CFF;
        public int durabilityBackgroundColor = 0x33262A3A;

        public static ArmorHudConfig defaults() {
            return new ArmorHudConfig();
        }

        public void validate() {
            width = Math.max(24, Math.min(220, width));
            height = Math.max(16, Math.min(96, height));
            x = Math.max(-512, Math.min(4096, x));
            y = Math.max(-512, Math.min(4096, y));
            radius = clamp(radius, 0.0f, 24.0f);
            opacity = clamp(opacity, 0.0f, 1.0f);
            padding = Math.max(1, Math.min(32, padding));
            iconSize = Math.max(4, Math.min(32, iconSize));
            iconGap = Math.max(0, Math.min(24, iconGap));
            iconYOffset = Math.max(-32, Math.min(32, iconYOffset));
            durabilityBarHeight = Math.max(1, Math.min(8, durabilityBarHeight));
            durabilityBarYOffset = Math.max(-16, Math.min(48, durabilityBarYOffset));
        }
    }

    public static class InventoryHudConfig {
        public boolean enabled = true;

        public int x = 174;
        public int y = 276;
        public int width = 184;
        public int height = 70;
        public float radius = 8.0f;
        public float opacity = 0.70f;

        public int padding = 6;
        public int columns = 9;
        public int slotSize = 16;
        public int slotGap = 3;
        public int slotColor = 0xFF050710;
        public float slotOpacity = 0.28f;
        public boolean showHotbar = false;

        public static InventoryHudConfig defaults() {
            return new InventoryHudConfig();
        }

        public void validate() {
            width = Math.max(60, Math.min(360, width));
            height = Math.max(28, Math.min(220, height));
            x = Math.max(-512, Math.min(4096, x));
            y = Math.max(-512, Math.min(4096, y));
            radius = clamp(radius, 0.0f, 24.0f);
            opacity = clamp(opacity, 0.0f, 1.0f);
            padding = Math.max(1, Math.min(32, padding));
            columns = Math.max(1, Math.min(12, columns));
            slotSize = Math.max(8, Math.min(32, slotSize));
            slotGap = Math.max(0, Math.min(16, slotGap));
            slotOpacity = clamp(slotOpacity, 0.0f, 1.0f);
        }
    }

    public static class HotbarConfig {
        public boolean enabled = true;

        public int yOffset = 0;
        public int slotSize = 20;
        public int slotGap = 2;
        public int padding = 4;
        public float radius = 8.0f;
        public float opacity = 0.72f;
        public int slotColor = 0xFF050710;
        public float slotOpacity = 0.30f;
        public int selectedColor = 0xFF7C5CFF;
        public int selectedOutlineColor = 0xFFFFFFFF;
        public float selectedAnimationSpeed = 0.34f;

        public static HotbarConfig defaults() {
            return new HotbarConfig();
        }

        public void validate() {
            yOffset = Math.max(-96, Math.min(96, yOffset));
            slotSize = Math.max(16, Math.min(40, slotSize));
            slotGap = Math.max(0, Math.min(16, slotGap));
            padding = Math.max(1, Math.min(24, padding));
            radius = clamp(radius, 0.0f, 24.0f);
            opacity = clamp(opacity, 0.0f, 1.0f);
            slotOpacity = clamp(slotOpacity, 0.0f, 1.0f);
            selectedAnimationSpeed = clamp(selectedAnimationSpeed, 0.02f, 1.0f);
        }
    }

    public static class HudAnimationConfig {
        public boolean enabled = true;
        public float speed = 0.24f;
        public float slideDistance = 6.0f;
        public float startScale = 0.92f;

        public static HudAnimationConfig defaults() {
            return new HudAnimationConfig();
        }

        public void validate() {
            speed = clamp(speed, 0.02f, 1.0f);
            slideDistance = clamp(slideDistance, 0.0f, 32.0f);
            startScale = clamp(startScale, 0.50f, 1.0f);
        }
    }

    public static class FireOverlayConfig {
        public boolean enabled = false;

        public int yOffset = 0;
        public int height = 58;
        public int color = 0xFFFF6A2A;
        public float opacity = 0.82f;
        public float animationSpeed = 1.0f;
        public int detail = 18;

        public static FireOverlayConfig defaults() {
            return new FireOverlayConfig();
        }

        public void validate() {
            yOffset = Math.max(-128, Math.min(128, yOffset));
            height = Math.max(8, Math.min(180, height));
            opacity = clamp(opacity, 0.0f, 1.0f);
            animationSpeed = clamp(animationSpeed, 0.0f, 4.0f);
            detail = Math.max(6, Math.min(48, detail));
        }
    }

    public static class ProjectilePredictionConfig {
        public boolean enabled = true;
        public int color = 0xFF7C5CFF;
        public int markerColor = 0xFFFFFFFF;
        public float lineWidth = 6.0f;
        public float markerSize = 0.22f;
        public int trailTicks = 10;
        public int points = 36;
        public int pointStepTicks = 2;
        public int spawnIntervalTicks = 2;
        public float gravity = 0.05f;
        public float bowVelocity = 3.0f;
        public float crossbowVelocity = 3.15f;
        public float pearlVelocity = 1.5f;
        public float tridentVelocity = 2.5f;
        public float experienceBottleVelocity = 0.7f;
        public float potionVelocity = 0.5f;
        public float windChargeVelocity = 1.5f;
        public float splashPotionRadius = 2.5f;
        public float lingeringPotionRadius = 2.0f;
        public float multishotAngle = 10.0f;

        public static ProjectilePredictionConfig defaults() {
            return new ProjectilePredictionConfig();
        }

        public void validate() {
            lineWidth = clamp(lineWidth, 1.0f, 12.0f);
            markerSize = clamp(markerSize, 0.03f, 1.0f);
            trailTicks = Math.max(1, Math.min(80, trailTicks));
            points = Math.max(4, Math.min(96, points));
            pointStepTicks = Math.max(1, Math.min(8, pointStepTicks));
            spawnIntervalTicks = Math.max(1, Math.min(20, spawnIntervalTicks));
            gravity = clamp(gravity, 0.0f, 0.2f);
            bowVelocity = clamp(bowVelocity, 0.2f, 8.0f);
            crossbowVelocity = clamp(crossbowVelocity, 0.2f, 8.0f);
            pearlVelocity = clamp(pearlVelocity, 0.2f, 8.0f);
            tridentVelocity = clamp(tridentVelocity, 0.2f, 8.0f);
            experienceBottleVelocity = clamp(experienceBottleVelocity, 0.2f, 8.0f);
            potionVelocity = clamp(potionVelocity, 0.2f, 8.0f);
            windChargeVelocity = clamp(windChargeVelocity, 0.2f, 8.0f);
            splashPotionRadius = clamp(splashPotionRadius, 0.0f, 16.0f);
            lingeringPotionRadius = clamp(lingeringPotionRadius, 0.0f, 16.0f);
            multishotAngle = clamp(multishotAngle, 0.0f, 35.0f);
        }
    }

    public static class VisualEffectsConfig {
        public boolean skyColorEnabled = true;
        public int skyColor = 0xFF7C5CFF;

        public boolean fogColorEnabled = true;
        public int fogColor = 0xFF7C5CFF;
        public float fogOpacity = 1.0f;

        public boolean customParticlesEnabled = true;
        public int particleColor = 0xFF7C5CFF;
        public float particleSize = 2.0f;
        public int particlesPerTick = 1;
        public int particleSpawnIntervalTicks = 1;
        public float particleRadius = 50.0f;
        public float particleYOffset = 0.8f;
        public float particleVelocity = 0.015f;

        public static VisualEffectsConfig defaults() {
            return new VisualEffectsConfig();
        }

        public void validate() {
            fogOpacity = clamp(fogOpacity, 0.0f, 1.0f);
            particleSize = clamp(particleSize, 0.05f, 4.0f);
            particlesPerTick = Math.max(0, Math.min(24, particlesPerTick));
            particleSpawnIntervalTicks = Math.max(1, Math.min(200, particleSpawnIntervalTicks));
            particleRadius = clamp(particleRadius, 0.0f, 256.0f);
            particleYOffset = clamp(particleYOffset, -4.0f, 4.0f);
            particleVelocity = clamp(particleVelocity, 0.0f, 0.5f);
        }
    }

    public static class MultiKeyBindingsConfig {
        public boolean enabled = true;
        public List<MultiKeyBinding> bindings = new ArrayList<>();

        public static MultiKeyBindingsConfig defaults() {
            MultiKeyBindingsConfig config = new MultiKeyBindingsConfig();
            config.bindings.add(buildMacroExample());
            config.bindings.add(buildSequenceExample());
            return config;
        }

        private static MultiKeyBinding buildMacroExample() {
            MultiKeyBinding binding = new MultiKeyBinding(
                    "macro_reload_and_fullbright",
                    "Reload + FullBright",
                    KeyStroke.keyWithModifiers(GLFW.GLFW_KEY_B, true, false, false),
                    List.of(ModAction.RELOAD_CONFIG.id(), ModAction.TOGGLE_FULLBRIGHT.id())
            );
            binding.enabled = false;
            return binding;
        }

        private static MultiKeyBinding buildSequenceExample() {
            MultiKeyBinding binding = new MultiKeyBinding(
                    "sequence_open_menu",
                    "Open VibeVisuals Menu",
                    KeyStroke.key(GLFW.GLFW_KEY_G),
                    List.of(ModAction.OPEN_MENU.id())
            );
            binding.chord = KeyStroke.key(GLFW.GLFW_KEY_F);
            binding.chordTimeoutMs = 1000;
            binding.enabled = false;
            return binding;
        }

        public void validate() {
            if (bindings == null) {
                bindings = new ArrayList<>();
            }
            bindings.removeIf(java.util.Objects::isNull);
            for (MultiKeyBinding binding : bindings) {
                binding.validate();
            }
        }
    }
}
