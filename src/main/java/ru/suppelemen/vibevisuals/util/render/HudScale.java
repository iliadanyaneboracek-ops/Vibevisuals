package ru.suppelemen.vibevisuals.util.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import ru.suppelemen.vibevisuals.VibeVisualsClient;

/**
 * Framebuffer-based HUD coordinate system.
 *
 * <p>HUD widgets express sizes and font heights in <em>design pixels</em>
 * (reference resolution 1920×1080). At render time they call {@link #dp}
 * to convert design px into Minecraft scaled units, divided by the active
 * GUI scale. Net effect: the widget keeps a constant physical pixel size
 * on screen at every GUI Scale (1/2/3/4/Auto), the way a system overlay
 * does — no banner-blowup at small GUI scales, no shrinking at large ones.
 *
 * <p>Mirrors the math the ClickGUI uses (see {@code dpScale} in
 * VibeVisualsMenuScreen) so HUD and menu visually agree.
 */
public final class HudScale {

    /** Reference framebuffer size the HUD is authored against. */
    public static final float REFERENCE_WIDTH = 1920.0f;
    public static final float REFERENCE_HEIGHT = 1080.0f;

    /** Glyph atlas size for the HUD font (see assets/vibevisuals/font/hud.json). */
    public static final float HUD_FONT_NATIVE_PX = 24.0f;

    /**
     * Font atlases pre-rasterised at their target sizes (in scaled-MC units).
     * Crucial for crispness: by picking the closest atlas and drawing at scale 1.0
     * we avoid the nearest-neighbor downsampling that produces "pixelated text"
     * when matrix-scaling a larger atlas down.
     */
    private static final int[] NATIVE_FONT_SIZES = { 6, 7, 8, 9, 10, 11, 12, 13, 14, 16, 18, 22 };

    private static final StyleSpriteSource[] NATIVE_FONTS = new StyleSpriteSource[NATIVE_FONT_SIZES.length];
    static {
        for (int i = 0; i < NATIVE_FONT_SIZES.length; i++) {
            NATIVE_FONTS[i] = new StyleSpriteSource.Font(
                    Identifier.of(VibeVisualsClient.MOD_ID, "hud_" + NATIVE_FONT_SIZES[i]));
        }
    }

    private HudScale() {
    }

    /** Returns the font atlas closest in size to the requested px target. */
    public static StyleSpriteSource fontFor(int pxSize) {
        int bestIdx = 0;
        int bestDiff = Math.abs(NATIVE_FONT_SIZES[0] - pxSize);
        for (int i = 1; i < NATIVE_FONT_SIZES.length; i++) {
            int diff = Math.abs(NATIVE_FONT_SIZES[i] - pxSize);
            if (diff < bestDiff) {
                bestDiff = diff;
                bestIdx = i;
            }
        }
        return NATIVE_FONTS[bestIdx];
    }

    /** Returns the actual atlas size chosen by {@link #fontFor}. */
    public static int actualFontSize(int pxSize) {
        int bestIdx = 0;
        int bestDiff = Math.abs(NATIVE_FONT_SIZES[0] - pxSize);
        for (int i = 1; i < NATIVE_FONT_SIZES.length; i++) {
            int diff = Math.abs(NATIVE_FONT_SIZES[i] - pxSize);
            if (diff < bestDiff) {
                bestDiff = diff;
                bestIdx = i;
            }
        }
        return NATIVE_FONT_SIZES[bestIdx];
    }

    /** Build a Text styled with the closest pre-rasterised font for the size. */
    public static Text styled(String text, int pxSize, boolean bold) {
        StyleSpriteSource font = fontFor(pxSize);
        return Text.literal(text).styled(s -> s.withFont(font).withBold(bold));
    }

    /** Width in scaled-MC pixels for a Text drawn at scale 1.0. */
    public static int crispTextWidth(MinecraftClient client, Text styled) {
        return client.textRenderer.getWidth(styled);
    }

    /**
     * Returns the visible glyph height (= chosen native atlas size).  Used for
     * vertical centring math when drawing native-sized text.
     */
    public static int crispTextHeight(int pxSize) {
        return actualFontSize(pxSize);
    }

    /**
     * Draw text using its native atlas — no matrix scale, no downsampling,
     * pixel-perfect crisp at any resolution.
     */
    public static void drawCrispText(DrawContext ctx, MinecraftClient client, Text styled,
                                      int x, int y, int color) {
        ctx.drawText(client.textRenderer, styled, x, y, color, false);
    }

    /** Crisp text vertically centred around {@code rowCenterY}. */
    public static void drawCrispCentered(DrawContext ctx, MinecraftClient client, Text styled,
                                          int x, int rowCenterY, int pxSize, int color) {
        int em = crispTextHeight(pxSize);
        // Inter optical centre is biased ~10 % low for caps.
        int y = rowCenterY - em / 2 + Math.round(em * 0.08f);
        ctx.drawText(client.textRenderer, styled, x, y, color, false);
    }

    /**
     * Returns the scale factor that converts one design pixel into scaled-MC
     * units for the current frame. Multiplied by the user's {@code hudScale}
     * preference if you want a global "HUD bigger/smaller" knob.
     */
    public static float dpScale() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return 1.0f;
        float fbW = mc.getWindow().getWidth();
        float fbH = mc.getWindow().getHeight();
        float layoutScale = Math.min(fbW / REFERENCE_WIDTH, fbH / REFERENCE_HEIGHT);
        double guiScale = mc.getWindow().getScaleFactor();
        if (guiScale <= 0) guiScale = 1.0;
        // Clamp so tiny windows stay legible.
        return Math.max((float) (layoutScale / guiScale), 0.45f);
    }

    /** Convert design px → scaled-MC units, rounded. */
    public static int dp(float v) {
        return Math.round(v * dpScale());
    }

    /** Matrix scale to draw {@code pxSize}-design-px text via the HUD font atlas. */
    public static float textScale(float pxSize) {
        return dpScale() * (pxSize / HUD_FONT_NATIVE_PX);
    }

    /** Width of a piece of text rendered at the given target px size. */
    public static int textWidth(MinecraftClient client, Text text, float pxSize) {
        // Route through the SemiBold SmoothText atlas (same as drawScaledText)
        // so layout math agrees with what's drawn.
        int glyph = Math.max(6, Math.round(pxSize * dpScale()));
        return ru.suppelemen.vibevisuals.util.font.SmoothText
                .measureTextBold(text.getString(), glyph);
    }

    /**
     * Em-box height of the rendered text in scaled-MC units — equals the
     * actual on-screen pixel height of the glyphs (atlas scaled by textScale).
     * Use this for vertical centring so text really sits in the middle of a row.
     */
    public static int textHeight(float pxSize) {
        return Math.round(pxSize * dpScale());
    }

    /** Draw text scaled to the given design-pixel size. Routes through the
     *  SemiBold SmoothText atlas (mipmapped + LINEAR sampler) for a
     *  weight-matched look with the menu. */
    public static void drawScaledText(DrawContext ctx, MinecraftClient client, Text text,
                                       int x, int y, float pxSize, int color) {
        int glyph = Math.max(6, Math.round(pxSize * dpScale()));
        // SmoothText draws the full glyph cell which has padding above the cap;
        // lift y so cap-top aligns with the caller's expectation.
        int nudgeUp = Math.round(glyph * 0.27f);
        ru.suppelemen.vibevisuals.util.font.SmoothText
                .drawTextBold(ctx, text.getString(), x, y - nudgeUp, glyph, color);
    }

    /**
     * Draw text centred vertically around {@code rowCenterY}.
     *
     * <p>Handles two things HUD elements kept doing by hand:
     * <ul>
     *   <li>em-box vertical centring;</li>
     *   <li>a small optical nudge downward — Inter caps sit slightly above
     *       the em-centre because the glyph has more ascender space than
     *       descender; without this push, text reads as "floating up".</li>
     * </ul>
     */
    public static void drawCenteredText(DrawContext ctx, MinecraftClient client, Text text,
                                         int x, int rowCenterY, float pxSize, int color) {
        int em = textHeight(pxSize);
        // Inter optical centre is ~6 % of em below the em-centre.  Letting the
        // value round naturally (no Math.max floor) keeps small text from
        // jumping a full px down at low scales.
        int opticalNudge = Math.round(pxSize * 0.06f * dpScale());
        int y = rowCenterY - em / 2 + opticalNudge;
        drawScaledText(ctx, client, text, x, y, pxSize, color);
    }
}
