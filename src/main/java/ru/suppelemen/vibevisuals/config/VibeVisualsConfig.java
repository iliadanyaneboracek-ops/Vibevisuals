package ru.suppelemen.vibevisuals.config;

import ru.suppelemen.vibevisuals.theme.HudCardRenderType;

public class VibeVisualsConfig {
    private static final int CURRENT_CONFIG_VERSION = 2;
    private static final int LEGACY_DEFAULT_MAX_EFFECTS = 3;

    public int configVersion = CURRENT_CONFIG_VERSION;
    public boolean hudEnabled = true;

    public CardConfig potionsCard = CardConfig.potionsDefaults();

    public void validate() {
        if (potionsCard == null) {
            potionsCard = CardConfig.potionsDefaults();
        }

        if (configVersion < CURRENT_CONFIG_VERSION && potionsCard.maxEffects == LEGACY_DEFAULT_MAX_EFFECTS) {
            potionsCard.maxEffects = CardConfig.DEFAULT_MAX_EFFECTS;
        }

        potionsCard.validate();
        configVersion = CURRENT_CONFIG_VERSION;
    }

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static class CardConfig {
        public static final int DEFAULT_MAX_EFFECTS = 64;

        public boolean enabled = true;

        public int x = 18;
        public int y = 120;
        public int width = 116;
        public int height = 39;

        public float radius = 8.0f;
        public float opacity = 0.90f;
        public boolean glow = true;
        public boolean blur = false;
        public HudCardRenderType renderType = HudCardRenderType.LIQUID_GLASS;

        public int titleColor = 0xFFEFEFF6;
        public int subtitleColor = 0xFFD7DAE8;
        public int timerColor = 0xFFB7BBC9;

        public float textScale = 0.26f;
        public float effectTextScale = 0.20f;
        public int iconSize = 6;
        public int titleIconSize = 8;
        public int effectIconYOffset = -2;
        public int titleIconYOffset = 0;
        public int padding = 9;
        public int titleY = 6;
        public int effectsStartY = 23;
        public int rowGap = 9;
        public int bottomPadding = 8;
        public int maxEffects = DEFAULT_MAX_EFFECTS;

        public static CardConfig potionsDefaults() {
            return new CardConfig();
        }

        public void validate() {
            width = Math.max(24, width);
            height = Math.max(16, height);
            radius = clamp(radius, 0.0f, 24.0f);
            opacity = clamp(opacity, 0.0f, 1.0f);
            textScale = clamp(textScale, 0.10f, 2.0f);
            effectTextScale = clamp(effectTextScale, 0.10f, 1.50f);
            iconSize = Math.max(3, Math.min(32, iconSize));
            titleIconSize = Math.max(3, Math.min(32, titleIconSize));
            effectIconYOffset = Math.max(-16, Math.min(16, effectIconYOffset));
            titleIconYOffset = Math.max(-16, Math.min(16, titleIconYOffset));
            padding = Math.max(2, Math.min(32, padding));
            titleY = Math.max(2, Math.min(64, titleY));
            effectsStartY = Math.max(titleY + 4, Math.min(96, effectsStartY));
            rowGap = Math.max(4, Math.min(40, rowGap));
            bottomPadding = Math.max(2, Math.min(32, bottomPadding));
            maxEffects = Math.max(1, Math.min(128, maxEffects));

            if (renderType == null) {
                renderType = HudCardRenderType.LIQUID_GLASS;
            }
        }
    }
}
