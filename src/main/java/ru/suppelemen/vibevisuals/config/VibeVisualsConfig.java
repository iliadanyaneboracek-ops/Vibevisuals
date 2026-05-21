package ru.suppelemen.vibevisuals.config;

import ru.suppelemen.vibevisuals.theme.HudCardRenderType;

public class VibeVisualsConfig {
    private static final int CURRENT_CONFIG_VERSION = 2;
    private static final int LEGACY_DEFAULT_MAX_EFFECTS = 3;

    public int configVersion = CURRENT_CONFIG_VERSION;
    public boolean hudEnabled = true;

    public CardConfig potionsCard = CardConfig.potionsDefaults();
    public TopBarConfig topBar = TopBarConfig.defaults();
    public HotKeysConfig hotKeysCard = HotKeysConfig.defaults();
    public PvpCardConfig pvpCard = PvpCardConfig.defaults();
    public ArmorHudConfig armorHud = ArmorHudConfig.defaults();

    public void validate() {
        if (potionsCard == null) {
            potionsCard = CardConfig.potionsDefaults();
        }

        if (topBar == null) {
            topBar = TopBarConfig.defaults();
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

        if (configVersion < CURRENT_CONFIG_VERSION && potionsCard.maxEffects == LEGACY_DEFAULT_MAX_EFFECTS) {
            potionsCard.maxEffects = CardConfig.DEFAULT_MAX_EFFECTS;
        }

        potionsCard.validate();
        topBar.validate();
        hotKeysCard.validate();
        pvpCard.validate();
        armorHud.validate();
        configVersion = CURRENT_CONFIG_VERSION;
    }

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static class CardConfig {
        public static final int DEFAULT_MAX_EFFECTS = 64;

        public boolean enabled = true;

        public int x = 20;
        public int y = 50;
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
        public int y = 8;
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

        public int x = 470;
        public int y = 30;
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

        public int x = 80;
        public int y = 280;
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
        public int totemCountYOffset = 0;
        public int goldenAppleCountXOffset = 0;
        public int goldenAppleCountYOffset = 0;
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

        public int x = 220;
        public int y = 300;
        public int width = 78;
        public int height = 24;
        public float radius = 7.0f;
        public float opacity = 0.75f;

        public int padding = 5;
        public int iconSize = 14;
        public int iconGap = 4;
        public int iconYOffset = 0;
        public boolean showDurability = true;
        public int durabilityBarHeight = 2;
        public int durabilityBarYOffset = 16;
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
}
