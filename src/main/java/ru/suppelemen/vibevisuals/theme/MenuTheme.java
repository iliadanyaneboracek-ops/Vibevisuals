package ru.suppelemen.vibevisuals.theme;

public final class MenuTheme {
    public static final int BG_DEEP = 0xFF07070D;
    public static final int BG_PANEL = 0xFF0B0B14;
    public static final int BG_PANEL_SOFT = 0xFF12101D;
    public static final int BG_CARD = 0xFF14101F;
    public static final int BG_CARD_HOVER = 0xFF1B1530;
    public static final int BG_CARD_ACTIVE = 0xFF221944;
    public static final int BG_INPUT = 0xFF0C0A14;
    public static final int BG_OVERLAY = 0xCC050308;

    public static final int PURPLE_DEEP = 0xFF171126;
    public static final int PURPLE_MID = 0xFF211735;
    public static final int PURPLE_MUTED = 0xFF2C1D47;

    public static final int ACCENT = 0xFF7B4DFF;
    public static final int ACCENT_BRIGHT = 0xFF8B5CF6;
    public static final int ACCENT_DEEP = 0xFF5C35FF;
    public static final int ACCENT_GLOW = 0x597C4DFF;

    public static final int TEXT_PRIMARY = 0xFFF1F1F6;
    public static final int TEXT_SECONDARY = 0xA6FFFFFF;
    public static final int TEXT_MUTED = 0x66FFFFFF;
    public static final int TEXT_DISABLED = 0x33FFFFFF;

    public static final int BORDER_SUBTLE = 0x14FFFFFF;
    public static final int BORDER_DEFAULT = 0x1FFFFFFF;
    public static final int BORDER_STRONG = 0x33FFFFFF;

    public static final int RADIUS_WINDOW = 18;
    public static final int RADIUS_CARD = 12;
    public static final int RADIUS_PILL = 10;
    public static final int RADIUS_BUTTON = 10;
    public static final int RADIUS_INPUT = 9;

    public static final int PADDING_WINDOW = 18;
    public static final int PADDING_CARD = 10;
    public static final int GAP_CARD = 8;

    public static final int WINDOW_WIDTH = 472;
    public static final int WINDOW_HEIGHT = 286;
    public static final int SIDE_PANEL_WIDTH = 232;

    public static final float OPEN_ANIM_DURATION_MS = 180.0f;
    public static final float HOVER_LERP = 0.22f;
    public static final float KNOB_LERP = 0.28f;
    public static final float SIDE_SLIDE_LERP = 0.30f;

    public static final int TOGGLE_WIDTH = 22;
    public static final int TOGGLE_HEIGHT = 12;

    private MenuTheme() {
    }

    public static int withAlpha(int argb, float alpha) {
        int a = (int) (alpha * 255.0f);
        a = Math.max(0, Math.min(255, a));
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
