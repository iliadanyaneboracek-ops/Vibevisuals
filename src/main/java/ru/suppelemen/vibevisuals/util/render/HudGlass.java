package ru.suppelemen.vibevisuals.util.render;

import net.minecraft.client.gui.DrawContext;
import ru.suppelemen.vibevisuals.theme.MenuTheme;

/**
 * Liquid-glass material drawing helpers shared by every HUD element so they
 * all match the ClickGUI look and react to the dark/light theme switch.
 *
 * <p>Two surfaces are provided:
 * <ul>
 *   <li>{@link #drawPanel} — heavier material used for full HUD cards (top bar,
 *       PvP card, armor card, etc.). Sits over the world like a frosted panel.</li>
 *   <li>{@link #drawChip} — lighter material used for inline rows / chips inside
 *       a card.</li>
 * </ul>
 */
public final class HudGlass {

    /** Outer ring opacity for HUD surfaces. Slightly stronger than the menu so they read on busy worlds. */
    public static final float OUTLINE_ALPHA = 0.28f;

    /** Reusable settings for the SDF (LIQUID_GLASS) card render path. */
    private static final ru.suppelemen.vibevisuals.theme.HudVisualSettings SDF =
            new ru.suppelemen.vibevisuals.theme.HudVisualSettings();
    static { SDF.renderType = ru.suppelemen.vibevisuals.theme.HudCardRenderType.LIQUID_GLASS; }

    private HudGlass() {
    }

    /** Heavier frosted panel — for the outer HUD card. */
    public static void drawPanel(DrawContext ctx, int x, int y, int w, int h, int radius) {
        drawPanel(ctx, x, y, w, h, radius, 1.0f);
    }

    public static void drawPanel(DrawContext ctx, int x, int y, int w, int h, int radius, float opacityMultiplier) {
        if (w <= 0 || h <= 0) return;
        float op = clamp01(MenuTheme.MATERIAL_OPACITY_PANEL * opacityMultiplier);
        // SDF shader card — corners stay pixel-perfect at any aspect ratio,
        // unlike the 9-slice texture path which steps on wide-and-short cards.
        // No outline — iOS-style clean surface.
        SDF.radius = radius;
        SDF.opacity = op;
        HudCardRenderer.drawCard(ctx, x, y, w, h, SDF);
    }

    /** Lighter inline chip / pill — for rows inside a panel. */
    public static void drawChip(DrawContext ctx, int x, int y, int w, int h, int radius) {
        drawChip(ctx, x, y, w, h, radius, 1.0f, false);
    }

    public static void drawChip(DrawContext ctx, int x, int y, int w, int h, int radius,
                                 float opacityMultiplier, boolean active) {
        if (w <= 0 || h <= 0) return;
        float baseOp = active
                ? MenuTheme.MATERIAL_OPACITY_CARD_ACTIVE
                : MenuTheme.MATERIAL_OPACITY_CARD;
        float op = clamp01(baseOp * opacityMultiplier);
        // Same iOS-style clean surface — no outline.
        SDF.radius = radius;
        SDF.opacity = op;
        HudCardRenderer.drawCard(ctx, x, y, w, h, SDF);
    }

    /** Theme-aware primary text colour (white in dark, dark navy in light). */
    public static int textPrimary() { return MenuTheme.TEXT_PRIMARY; }

    /** Theme-aware secondary text colour for muted values. */
    public static int textSecondary() {
        return MenuTheme.withAlpha(MenuTheme.TEXT_NEUTRAL, 0.65f);
    }

    /** Very muted text — slash separators, units. */
    public static int textMuted() {
        return MenuTheme.withAlpha(MenuTheme.TEXT_NEUTRAL, 0.40f);
    }

    /** Theme-aware accent (used by HUD highlights — kept the same on both palettes). */
    public static int accent() { return MenuTheme.ACCENT_BRIGHT; }

    private static float clamp01(float v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }
}
