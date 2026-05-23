package ru.suppelemen.vibevisuals.theme;

/**
 * ClickGUI theme palette.
 *
 * Colour fields are intentionally non-final: {@link #applyTheme(String)} swaps
 * the active palette at runtime (e.g. when the user toggles light/dark from
 * the ClickGUI), so screen code can keep reading {@code MenuTheme.BG_PANEL}
 * directly without dragging a palette instance through every call site.
 */
public final class MenuTheme {

    public enum ThemeMode { DARK, LIGHT }

    public static ThemeMode current = ThemeMode.DARK;

    // ---- Mutable palette (rewritten by applyTheme) ----
    public static int BG_DEEP;
    public static int BG_PANEL;
    public static int BG_PANEL_SOFT;
    public static int BG_CARD;
    public static int BG_CARD_HOVER;
    public static int BG_CARD_ACTIVE;
    public static int BG_INPUT;
    public static int BG_OVERLAY;

    public static int PURPLE_DEEP;
    public static int PURPLE_MID;
    public static int PURPLE_GLOW;

    public static int ACCENT;
    public static int ACCENT_BRIGHT;
    public static int ACCENT_DEEP;

    public static int TEXT_PRIMARY;
    public static int TEXT_SECONDARY;
    public static int TEXT_MUTED;
    public static int TEXT_DISABLED;
    public static int TEXT_OFF;
    /** RGB used as base for alpha-tinted text (white on dark, black on light). */
    public static int TEXT_NEUTRAL;

    public static int BORDER_SUBTLE;
    public static int BORDER_DEFAULT;
    public static int BORDER_STRONG;

    /** ARGB used for the world dim overlay when the menu opens (alpha is multiplied by ease). */
    public static int DIM_RGB;

    /** Specular highlight stroke color used to fake "liquid glass" edges on cards. */
    public static int GLASS_HIGHLIGHT;

    // ---- Liquid glass material (Apple-style) ----
    /** Neutral glass tint applied over the blurred backdrop for the main surface. */
    public static int MATERIAL_PANEL;
    /** Slightly brighter tint used for cards / chips sitting on top of MATERIAL_PANEL. */
    public static int MATERIAL_CARD;
    /** Material used for the active (enabled) card so the accent stays subtle. */
    public static int MATERIAL_CARD_ACTIVE;
    /** Base opacity of the glass material (lower = more backdrop bleed-through). */
    public static float MATERIAL_OPACITY_PANEL;
    public static float MATERIAL_OPACITY_CARD;
    public static float MATERIAL_OPACITY_CARD_ACTIVE;

    // ---- Animation tunings (palette-independent) ----
    public static final float OPEN_ANIM_DURATION_MS = 200.0f;
    public static final float HOVER_LERP = 0.22f;
    public static final float KNOB_LERP = 0.30f;
    public static final float SIDE_SLIDE_LERP = 0.32f;

    // ---- Tunable opacities (palette-independent) ----
    public static final float SEPARATOR_ALPHA = 0.18f;
    public static final float TAB_SLASH_ALPHA = 0.22f;
    public static final float TAB_INACTIVE_ALPHA = 0.30f;
    public static final float TAB_HOVER_ALPHA = 0.55f;
    public static final float CARD_TEXT_DISABLED_ALPHA = 0.22f;
    public static final float CARD_BG_OFF_OPACITY = 0.42f;
    public static final float CARD_BG_ON_OPACITY = 0.82f;
    public static final float CARD_OUTLINE_OFF_ALPHA = 0.10f;
    public static final float CARD_OUTLINE_ON_ALPHA = 0.30f;
    public static final float GLASS_HIGHLIGHT_ALPHA = 0.32f;   // inner top stroke alpha
    public static final float GLASS_SHADOW_ALPHA = 0.18f;      // inner bottom stroke alpha
    public static final float GLASS_OUTLINE_ALPHA = 0.22f;     // outer 1px ring alpha

    static { applyTheme("DARK"); }

    private MenuTheme() {
    }

    public static void applyTheme(String name) {
        ThemeMode mode = "LIGHT".equalsIgnoreCase(name) ? ThemeMode.LIGHT : ThemeMode.DARK;
        current = mode;
        if (mode == ThemeMode.LIGHT) {
            applyLight();
        } else {
            applyDark();
        }
    }

    private static void applyDark() {
        BG_DEEP        = 0xFF06060B;
        BG_PANEL       = 0xFF07070D;
        BG_PANEL_SOFT  = 0xFF0A0A12;
        BG_CARD        = 0xFF11111C;
        BG_CARD_HOVER  = 0xFF181428;
        BG_CARD_ACTIVE = 0xFF1F1738;
        BG_INPUT       = 0xFF0A0911;
        BG_OVERLAY     = 0xCC07050F;

        PURPLE_DEEP  = 0xFF160F2A;
        PURPLE_MID   = 0xFF1F1438;
        PURPLE_GLOW  = 0xFF8B5CF6;

        ACCENT        = 0xFF7449FF;
        ACCENT_BRIGHT = 0xFF9D72FF;
        ACCENT_DEEP   = 0xFF5A33FF;

        TEXT_PRIMARY  = 0xFFF1EEFF;
        TEXT_SECONDARY = 0xB0FFFFFF;
        TEXT_MUTED    = 0x6FFFFFFF;
        TEXT_DISABLED = 0x42FFFFFF;
        TEXT_OFF      = 0x55FFFFFF;
        TEXT_NEUTRAL  = 0xFFFFFFFF;

        BORDER_SUBTLE  = 0x12FFFFFF;
        BORDER_DEFAULT = 0x1EFFFFFF;
        BORDER_STRONG  = 0x33FFFFFF;

        DIM_RGB         = 0xFF07050F;
        GLASS_HIGHLIGHT = 0xFFFFFFFF;

        MATERIAL_PANEL       = 0xFF13131C;
        MATERIAL_CARD        = 0xFF1C1C26;
        MATERIAL_CARD_ACTIVE = 0xFF272735;
        MATERIAL_OPACITY_PANEL       = 0.56f;
        MATERIAL_OPACITY_CARD        = 0.36f;
        MATERIAL_OPACITY_CARD_ACTIVE = 0.65f;
    }

    private static void applyLight() {
        BG_DEEP        = 0xFFE5E4EE;
        BG_PANEL       = 0xFFF4F3F8;
        BG_PANEL_SOFT  = 0xFFEDECF3;
        BG_CARD        = 0xFFFAFAFD;
        BG_CARD_HOVER  = 0xFFF1EEFA;
        BG_CARD_ACTIVE = 0xFFE6DFFB;
        BG_INPUT       = 0xFFFFFFFF;
        BG_OVERLAY     = 0xCCB6B4C4;

        PURPLE_DEEP  = 0xFFE6DEFB;
        PURPLE_MID   = 0xFFD9CFFA;
        PURPLE_GLOW  = 0xFF8B5CF6;

        ACCENT        = 0xFF7449FF;
        ACCENT_BRIGHT = 0xFF8B5CF6;
        ACCENT_DEEP   = 0xFF5A33FF;

        // Cool dark navy works better than pure black against the bluish glass.
        TEXT_PRIMARY  = 0xFF1B2030;
        TEXT_SECONDARY = 0xB0181D2A;
        TEXT_MUTED    = 0x6F181D2A;
        TEXT_DISABLED = 0x42181D2A;
        TEXT_OFF      = 0x55181D2A;
        TEXT_NEUTRAL  = 0xFF161B26;

        BORDER_SUBTLE  = 0x14000000;
        BORDER_DEFAULT = 0x20000000;
        BORDER_STRONG  = 0x36000000;

        // Cool light-blue dim tint, very subtle — lets the world breathe through.
        DIM_RGB         = 0xFFC4CCDC;
        GLASS_HIGHLIGHT = 0xFFFFFFFF;

        // Cool, slightly bluish white — gives the airy "frosted iCloud" look
        // from the reference instead of a flat warm grey.
        MATERIAL_PANEL       = 0xFFF2F5FE;
        MATERIAL_CARD        = 0xFFFFFFFF;
        MATERIAL_CARD_ACTIVE = 0xFFDDE4F4;
        MATERIAL_OPACITY_PANEL       = 0.30f;   // lower opacity = more backdrop bleed-through
        MATERIAL_OPACITY_CARD        = 0.22f;
        MATERIAL_OPACITY_CARD_ACTIVE = 0.42f;
    }

    public static int withAlpha(int argb, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255.0f)));
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    public static int withAlphaMultiplied(int argb, float multiplier) {
        int existing = (argb >>> 24) & 0xFF;
        int a = Math.max(0, Math.min(255, Math.round(existing * multiplier)));
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    public static int lerpColor(int from, int to, float t) {
        t = Math.max(0.0f, Math.min(1.0f, t));
        int fa = (from >>> 24) & 0xFF;
        int fr = (from >>> 16) & 0xFF;
        int fg = (from >>> 8) & 0xFF;
        int fb = from & 0xFF;
        int ta = (to >>> 24) & 0xFF;
        int tr = (to >>> 16) & 0xFF;
        int tg = (to >>> 8) & 0xFF;
        int tb = to & 0xFF;
        int a = Math.round(fa + (ta - fa) * t);
        int r = Math.round(fr + (tr - fr) * t);
        int g = Math.round(fg + (tg - fg) * t);
        int b = Math.round(fb + (tb - fb) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
