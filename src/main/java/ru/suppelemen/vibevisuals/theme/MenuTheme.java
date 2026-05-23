package ru.suppelemen.vibevisuals.theme;

public final class MenuTheme {
    // Glass backgrounds (deep, near-black with a violet tint).
    public static final int BG_DEEP = 0xFF06060B;
    public static final int BG_PANEL = 0xFF07070D;
    public static final int BG_PANEL_SOFT = 0xFF0A0A12;
    public static final int BG_CARD = 0xFF11111C;
    public static final int BG_CARD_HOVER = 0xFF181428;
    public static final int BG_CARD_ACTIVE = 0xFF1F1738;
    public static final int BG_INPUT = 0xFF0A0911;
    public static final int BG_OVERLAY = 0xCC07050F;

    // Purple veil shades used as gradients on top of panels.
    public static final int PURPLE_DEEP = 0xFF160F2A;
    public static final int PURPLE_MID = 0xFF1F1438;
    public static final int PURPLE_GLOW = 0xFF8B5CF6;

    // Accent + glow.
    public static final int ACCENT = 0xFF7449FF;
    public static final int ACCENT_BRIGHT = 0xFF9D72FF;
    public static final int ACCENT_DEEP = 0xFF5A33FF;

    // Typography colours (ARGB).
    public static final int TEXT_PRIMARY = 0xFFF1EEFF;
    public static final int TEXT_SECONDARY = 0xB0FFFFFF;
    public static final int TEXT_MUTED = 0x6FFFFFFF;
    public static final int TEXT_DISABLED = 0x42FFFFFF;
    public static final int TEXT_OFF = 0x55FFFFFF;

    public static final int BORDER_SUBTLE = 0x12FFFFFF;
    public static final int BORDER_DEFAULT = 0x1EFFFFFF;
    public static final int BORDER_STRONG = 0x33FFFFFF;

    // Animation tunings.
    public static final float OPEN_ANIM_DURATION_MS = 200.0f;
    public static final float HOVER_LERP = 0.22f;
    public static final float KNOB_LERP = 0.30f;
    public static final float SIDE_SLIDE_LERP = 0.32f;

    private MenuTheme() {
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
