package ru.suppelemen.vibevisuals.config;

import org.lwjgl.glfw.GLFW;
import ru.suppelemen.vibevisuals.feature.keybind.KeyStroke;
import ru.suppelemen.vibevisuals.feature.keybind.ModAction;
import ru.suppelemen.vibevisuals.feature.keybind.MultiKeyBinding;
import ru.suppelemen.vibevisuals.theme.HudCardRenderType;

import java.util.ArrayList;
import java.util.List;

public class VibeVisualsConfig {
    private static final int CURRENT_CONFIG_VERSION = 6;
    private static final int LEGACY_DEFAULT_MAX_EFFECTS = 3;
    private static final int LEGACY_DEFAULT_TITLE_ICON_SIZE = 10;
    private static final int PREVIOUS_DEFAULT_TITLE_ICON_SIZE = 14;
    private static final int LEGACY_DEFAULT_TITLE_ICON_Y_OFFSET = -2;
    private static final int LEGACY_DEFAULT_HOTKEYS_ICON_SIZE = 7;
    private static final int PREVIOUS_DEFAULT_HOTKEYS_ICON_SIZE = 14;
    private static final int LEGACY_DEFAULT_HOTKEYS_ICON_Y_OFFSET = -2;

    public int configVersion = CURRENT_CONFIG_VERSION;
    public boolean hudEnabled = true;
    public float hudScale = 1.0f;
    public float fullBrightStrength = 1.0f;
    public MenuConfig menu = MenuConfig.defaults();
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
    public TargetEspConfig targetEsp = TargetEspConfig.defaults();
    public SaturationDisplayConfig saturationDisplay = SaturationDisplayConfig.defaults();
    public CustomHandConfig customHand = CustomHandConfig.defaults();
    public CustomCrosshairConfig customCrosshair = CustomCrosshairConfig.defaults();
    public MarkersConfig markers = MarkersConfig.defaults();
    public CustomHitSoundConfig customHitSound = CustomHitSoundConfig.defaults();
    public ShiftUpConfig shiftUp = ShiftUpConfig.defaults();
    public VisualEffectsConfig visualEffects = VisualEffectsConfig.defaults();
    public AutoEatConfig autoEat = AutoEatConfig.defaults();
    public AutoPotionConfig autoPotion = AutoPotionConfig.defaults();
    public AutoRespawnConfig autoRespawn = AutoRespawnConfig.defaults();
    public TapeMouseConfig tapeMouse = TapeMouseConfig.defaults();
    public HealingHelperConfig healingHelper = HealingHelperConfig.defaults();
    public SlotTimersConfig slotTimers = SlotTimersConfig.defaults();
    public MoggedConfig mogged = MoggedConfig.defaults();
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

        if (targetEsp == null) {
            targetEsp = TargetEspConfig.defaults();
        }
        if (saturationDisplay == null) {
            saturationDisplay = SaturationDisplayConfig.defaults();
        }
        if (customHand == null) {
            customHand = CustomHandConfig.defaults();
        }
        if (customCrosshair == null) {
            customCrosshair = CustomCrosshairConfig.defaults();
        }
        if (markers == null) {
            markers = MarkersConfig.defaults();
        }
        if (customHitSound == null) {
            customHitSound = CustomHitSoundConfig.defaults();
        }
        if (shiftUp == null) {
            shiftUp = ShiftUpConfig.defaults();
        }

        if (hudAnimations == null) {
            hudAnimations = HudAnimationConfig.defaults();
        }

        if (visualEffects == null) {
            visualEffects = VisualEffectsConfig.defaults();
        }

        if (autoEat == null) {
            autoEat = AutoEatConfig.defaults();
        }

        if (autoPotion == null) {
            autoPotion = AutoPotionConfig.defaults();
        }

        if (autoRespawn == null) {
            autoRespawn = AutoRespawnConfig.defaults();
        }

        if (tapeMouse == null) {
            tapeMouse = TapeMouseConfig.defaults();
        }

        if (healingHelper == null) {
            healingHelper = HealingHelperConfig.defaults();
        }

        if (slotTimers == null) {
            slotTimers = SlotTimersConfig.defaults();
        }

        if (mogged == null) {
            mogged = MoggedConfig.defaults();
        }

        if (menu == null) {
            menu = MenuConfig.defaults();
        }

        if (multiKeyBindings == null) {
            multiKeyBindings = MultiKeyBindingsConfig.defaults();
        }

        if (configVersion < CURRENT_CONFIG_VERSION && potionsCard.maxEffects == LEGACY_DEFAULT_MAX_EFFECTS) {
            potionsCard.maxEffects = CardConfig.DEFAULT_MAX_EFFECTS;
        }
        // (Removed: legacy titleIconSize → 16 migration was hijacking any user
        //  who chose 10 manually because it can't tell "user kept old default"
        //  from "user explicitly picked 10". The current default is 10 and
        //  we trust whatever's in the file.)
        if (configVersion < CURRENT_CONFIG_VERSION && potionsCard.titleIconYOffset == LEGACY_DEFAULT_TITLE_ICON_Y_OFFSET) {
            potionsCard.titleIconYOffset = -3;
        }
        if (configVersion < CURRENT_CONFIG_VERSION
                && (hotKeysCard.iconSize == LEGACY_DEFAULT_HOTKEYS_ICON_SIZE
                || hotKeysCard.iconSize == PREVIOUS_DEFAULT_HOTKEYS_ICON_SIZE)) {
            hotKeysCard.iconSize = 18;
        }
        if (configVersion < CURRENT_CONFIG_VERSION && hotKeysCard.iconYOffset == LEGACY_DEFAULT_HOTKEYS_ICON_Y_OFFSET) {
            hotKeysCard.iconYOffset = -3;
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
        targetEsp.validate();
        saturationDisplay.validate();
        customHand.validate();
        customCrosshair.validate();
        markers.validate();
        customHitSound.validate();
        shiftUp.validate();
        hudAnimations.validate();
        visualEffects.validate();
        autoEat.validate();
        autoPotion.validate();
        autoRespawn.validate();
        tapeMouse.validate();
        healingHelper.validate();
        slotTimers.validate();
        mogged.validate();
        menu.validate();
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
        public float size = 1.0f;

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
        public int titleIconYOffset = -3;
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
            size = clamp(size, 0.25f, 4.0f);
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
        public float size = 1.0f;
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
            size = clamp(size, 0.25f, 4.0f);
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
        public float size = 1.0f;
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
        public int iconSize = 18;
        public int iconXOffset = 0;
        public int iconYOffset = -3;
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
            size = clamp(size, 0.25f, 4.0f);
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
        public float size = 1.0f;
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
            size = clamp(size, 0.25f, 4.0f);
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
        public float size = 1.0f;
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
            size = clamp(size, 0.25f, 4.0f);
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
        public float size = 1.0f;
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
            size = clamp(size, 0.25f, 4.0f);
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
        public boolean appearEnabled = true;
        public boolean disappearEnabled = true;
        public float speed = 0.24f;
        public float disappearSpeed = 0.28f;
        public float slideDistance = 6.0f;
        public float startScale = 0.92f;

        public static HudAnimationConfig defaults() {
            return new HudAnimationConfig();
        }

        public void validate() {
            speed = clamp(speed, 0.02f, 1.0f);
            disappearSpeed = clamp(disappearSpeed, 0.02f, 1.0f);
            slideDistance = clamp(slideDistance, 0.0f, 32.0f);
            startScale = clamp(startScale, 0.50f, 1.0f);
        }
    }

    public static class MenuConfig {
        public boolean enabled = true;

        public int width = 250;
        public int height = 166;
        public int sideWidth = 142;
        public int xOffset = 0;
        public int yOffset = 0;
        public int sideXOffset = 0;
        public int sideYOffset = 0;
        public float radius = 10.0f;
        public float opacity = 0.82f;
        public float headerOpacity = 0.34f;
        public float cardOpacity = 0.28f;
        public float activeOpacity = 0.54f;

        public int backgroundColor = 0xFF050710;
        public int cardColor = 0xFF090B12;
        public int activeColor = 0xFF201A42;
        public int accentColor = 0xFF7C5CFF;
        public int outlineColor = 0xFFFFFFFF;
        public int titleColor = 0xFFEFEFF6;
        public int textColor = 0xFFD7DAE8;
        public int mutedTextColor = 0xFF9DA2B3;

        public int tabHeight = 13;
        public int featureHeight = 16;
        public int featureGap = 6;
        public int rowHeight = 14;
        public float settingTextScale = 0.82f;
        public int colorPickerSize = 76;

        // ClickGUI appearance toggles.
        public boolean liquidGlassBlur = true; // enables MC backdrop blur behind the menu
        public String theme = "DARK";          // DARK or LIGHT
        public String accent = "DEFAULT";      // AccentTheme enum name (DEFAULT, OCEAN_BLUE, …)

        public static MenuConfig defaults() {
            return new MenuConfig();
        }

        public void validate() {
            width = Math.max(190, Math.min(420, width));
            height = Math.max(120, Math.min(320, height));
            sideWidth = Math.max(120, Math.min(260, sideWidth));
            xOffset = Math.max(-512, Math.min(512, xOffset));
            yOffset = Math.max(-256, Math.min(256, yOffset));
            sideXOffset = Math.max(-512, Math.min(512, sideXOffset));
            sideYOffset = Math.max(-256, Math.min(256, sideYOffset));
            radius = clamp(radius, 0.0f, 24.0f);
            opacity = clamp(opacity, 0.0f, 1.0f);
            headerOpacity = clamp(headerOpacity, 0.0f, 1.0f);
            cardOpacity = clamp(cardOpacity, 0.0f, 1.0f);
            activeOpacity = clamp(activeOpacity, 0.0f, 1.0f);
            tabHeight = Math.max(10, Math.min(26, tabHeight));
            featureHeight = Math.max(12, Math.min(32, featureHeight));
            featureGap = Math.max(2, Math.min(18, featureGap));
            rowHeight = Math.max(12, Math.min(28, rowHeight));
            settingTextScale = clamp(settingTextScale, 0.50f, 1.0f);
            colorPickerSize = Math.max(48, Math.min(128, colorPickerSize));
            if (theme == null || (!theme.equalsIgnoreCase("DARK") && !theme.equalsIgnoreCase("LIGHT"))) {
                theme = "DARK";
            } else {
                theme = theme.toUpperCase();
            }
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

    public static class TargetEspConfig {
        public boolean enabled = true;
        public String mode = "COMBO";
        public int color = 0xFF7C5CFF;
        public int secondaryColor = 0xFFFFFFFF;
        public float radius = 0.82f;
        public float heightOffset = 0.05f;
        public float lineWidth = 4.0f;
        public float spinSpeed = 0.10f;
        public float particleSize = 0.10f;
        public int particles = 12;
        public int segments = 48;
        public int targetHoldTicks = 6;

        public static TargetEspConfig defaults() {
            return new TargetEspConfig();
        }

        public void validate() {
            if (mode == null || mode.isBlank()) {
                mode = "COMBO";
            }
            radius = clamp(radius, 0.20f, 4.0f);
            heightOffset = clamp(heightOffset, -1.0f, 3.0f);
            lineWidth = clamp(lineWidth, 1.0f, 12.0f);
            spinSpeed = clamp(spinSpeed, 0.0f, 1.0f);
            particleSize = clamp(particleSize, 0.02f, 0.50f);
            particles = Math.max(3, Math.min(64, particles));
            segments = Math.max(12, Math.min(128, segments));
            targetHoldTicks = Math.max(0, Math.min(40, targetHoldTicks));
        }
    }

    public static class SaturationDisplayConfig {
        public boolean enabled = true;
        public int color = 0xFFFFD866;
        public int backgroundColor = 0x66000000;
        public int xOffset = 0;
        public int yOffset = -3;
        public int segmentWidth = 7;
        public int segmentHeight = 2;
        public int segmentGap = 1;
        public float opacity = 0.95f;
        public boolean showExhaustion = true;
        public int exhaustionColor = 0xFFFF8A3D;

        public static SaturationDisplayConfig defaults() {
            return new SaturationDisplayConfig();
        }

        public void validate() {
            xOffset = Math.max(-96, Math.min(96, xOffset));
            yOffset = Math.max(-32, Math.min(32, yOffset));
            segmentWidth = Math.max(2, Math.min(16, segmentWidth));
            segmentHeight = Math.max(1, Math.min(8, segmentHeight));
            segmentGap = Math.max(0, Math.min(8, segmentGap));
            opacity = clamp(opacity, 0.0f, 1.0f);
        }
    }

    public static class CustomHandConfig {
        public boolean enabled = false;
        public String mode = "HORIZONTAL";
        public float x = 0.0f;
        public float y = 0.0f;
        public float z = 0.0f;
        public float pitch = 0.0f;
        public float yaw = 0.0f;
        public float roll = 0.0f;
        public float scale = 1.0f;
        public float swingAmount = 0.45f;

        public static CustomHandConfig defaults() {
            return new CustomHandConfig();
        }

        public void validate() {
            if (mode == null || mode.isBlank()) {
                mode = "HORIZONTAL";
            }
            x = clamp(x, -2.0f, 2.0f);
            y = clamp(y, -2.0f, 2.0f);
            z = clamp(z, -2.0f, 2.0f);
            pitch = clamp(pitch, -180.0f, 180.0f);
            yaw = clamp(yaw, -180.0f, 180.0f);
            roll = clamp(roll, -180.0f, 180.0f);
            scale = clamp(scale, 0.20f, 3.0f);
            swingAmount = clamp(swingAmount, 0.0f, 2.0f);
        }
    }

    public static class CustomCrosshairConfig {
        public boolean enabled = false;
        public boolean hideVanilla = true;
        public int color = 0xFFFFFFFF;
        public int gap = 4;
        public int length = 7;
        public int thickness = 1;
        public int width = 1;
        public float angle = 0.0f;
        public int xOffset = 0;
        public int yOffset = 0;
        public boolean dot = true;
        public int dotSize = 2;

        public static CustomCrosshairConfig defaults() {
            return new CustomCrosshairConfig();
        }

        public void validate() {
            gap = Math.max(0, Math.min(64, gap));
            length = Math.max(1, Math.min(96, length));
            thickness = Math.max(1, Math.min(16, thickness));
            width = Math.max(1, Math.min(16, width));
            angle = clamp(angle, -180.0f, 180.0f);
            xOffset = Math.max(-512, Math.min(512, xOffset));
            yOffset = Math.max(-512, Math.min(512, yOffset));
            dotSize = Math.max(1, Math.min(16, dotSize));
        }
    }

    public static class MarkersConfig {
        public boolean enabled = true;
        public int color = 0xFF7C5CFF;
        public int maxMarkers = 32;
        public float lineWidth = 3.0f;
        public float radius = 0.35f;
        public boolean showDistance = true;

        public static MarkersConfig defaults() {
            return new MarkersConfig();
        }

        public void validate() {
            maxMarkers = Math.max(1, Math.min(128, maxMarkers));
            lineWidth = clamp(lineWidth, 1.0f, 10.0f);
            radius = clamp(radius, 0.05f, 2.0f);
        }
    }

    public static class CustomHitSoundConfig {
        public boolean enabled = false;
        public String soundFile = "crit.wav";
        public float volume = 0.85f;
        public float cooldownTicks = 2.0f;

        public static CustomHitSoundConfig defaults() {
            return new CustomHitSoundConfig();
        }

        public void validate() {
            if (soundFile == null || soundFile.isBlank()) {
                soundFile = "crit.wav";
            }
            volume = clamp(volume, 0.0f, 2.0f);
            cooldownTicks = clamp(cooldownTicks, 0.0f, 20.0f);
        }
    }

    public static class ShiftUpConfig {
        public boolean enabled = false;

        public static ShiftUpConfig defaults() {
            return new ShiftUpConfig();
        }

        public void validate() {
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

    public static class AutoEatConfig {
        public boolean enabled = false;
        public int hungerPercent = 60;

        public static AutoEatConfig defaults() {
            return new AutoEatConfig();
        }

        public void validate() {
            hungerPercent = Math.max(5, Math.min(95, hungerPercent));
        }
    }

    public static class AutoPotionConfig {
        public boolean enabled = false;
        public boolean useSpeed = true;
        public boolean useInvisibility = true;
        public int refreshSeconds = 15;

        public static AutoPotionConfig defaults() {
            return new AutoPotionConfig();
        }

        public void validate() {
            refreshSeconds = Math.max(0, Math.min(120, refreshSeconds));
        }
    }

    public static class AutoRespawnConfig {
        public boolean enabled = false;
        public String command = "";
        public int commandDelayTicks = 20;

        public static AutoRespawnConfig defaults() {
            return new AutoRespawnConfig();
        }

        public void validate() {
            if (command == null) {
                command = "";
            }
            commandDelayTicks = Math.max(0, Math.min(200, commandDelayTicks));
        }
    }

    public static class TapeMouseConfig {
        public boolean enabled = false;
        public int clickDelayTicks = 4;

        public static TapeMouseConfig defaults() {
            return new TapeMouseConfig();
        }

        public void validate() {
            clickDelayTicks = Math.max(1, Math.min(200, clickDelayTicks));
        }
    }

    public static class MoggedConfig {
        public boolean enabled = false;          // off by default — it's a joke feature
        public float displayDurationSeconds = 1.4f;
        public float bannerScale = 2.0f;         // size multiplier for the world banner
        public boolean playSound = true;
        public String soundFile = "mogged.wav";  // dropped into <config>/vibevisuals/sounds/
        public float volume = 1.0f;

        public static MoggedConfig defaults() {
            return new MoggedConfig();
        }

        public void validate() {
            displayDurationSeconds = clamp(displayDurationSeconds, 0.2f, 6.0f);
            bannerScale = clamp(bannerScale, 0.4f, 6.0f);
            volume = clamp(volume, 0.0f, 2.0f);
            if (soundFile == null || soundFile.isBlank()) {
                soundFile = "mogged.wav";
            }
        }
    }

    public static class SlotTimersConfig {
        public boolean enabled = true;
        public int textColor = 0xFFFF4D4D;
        public int urgentColor = 0xFFFFD24D;
        public float urgentThresholdSeconds = 1.0f;
        public boolean showShadow = true;
        public boolean showSubsecond = true;
        public float textScale = 0.7f;
        public int xOffset = 0;
        public int yOffset = 0;

        public static SlotTimersConfig defaults() {
            return new SlotTimersConfig();
        }

        public void validate() {
            textScale = clamp(textScale, 0.25f, 2.0f);
            urgentThresholdSeconds = clamp(urgentThresholdSeconds, 0.0f, 10.0f);
            xOffset = Math.max(-16, Math.min(16, xOffset));
            yOffset = Math.max(-16, Math.min(16, yOffset));
        }
    }

    public static class HealingHelperConfig {
        public boolean enabled = true;
        public int currentColor = 0xFF5CE38B;
        public int nextColor = 0xFFFFFFFF;
        public float currentOpacity = 0.70f;
        public float currentFillOpacity = 0.28f;
        public float nextOpacity = 0.22f;
        public float nextFillOpacity = 0.08f;
        public float outlineThickness = 1.2f;
        public float pulseSpeed = 1.0f;
        public float pulseAmplitude = 0.25f;
        public int padding = 0;

        public static HealingHelperConfig defaults() {
            return new HealingHelperConfig();
        }

        public void validate() {
            currentOpacity = clamp(currentOpacity, 0.0f, 1.0f);
            currentFillOpacity = clamp(currentFillOpacity, 0.0f, 1.0f);
            nextOpacity = clamp(nextOpacity, 0.0f, 1.0f);
            nextFillOpacity = clamp(nextFillOpacity, 0.0f, 1.0f);
            outlineThickness = clamp(outlineThickness, 0.0f, 4.0f);
            pulseSpeed = clamp(pulseSpeed, 0.0f, 4.0f);
            pulseAmplitude = clamp(pulseAmplitude, 0.0f, 1.0f);
            padding = Math.max(-4, Math.min(6, padding));
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
