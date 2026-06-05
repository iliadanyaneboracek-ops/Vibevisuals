package ru.suppelemen.vibevisuals.util.font;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;

/**
 * Bitmap text renderer that draws from our pre-baked {@link SmoothFontAtlas}
 * via {@link RenderPipelines#GUI_TEXTURED}. GUI_TEXTURED's sampler does
 * bilinear filtering for GUI sprites, which gives us smooth text scaling
 * that bypasses MC's nearest-neighbour font pipeline entirely.
 *
 * <p>Coordinate system: integer scaled-MC units; same as DrawContext.fill etc.
 * Caller decides how big text should be via {@code pxSize}; atlas glyph is
 * scaled by {@code pxSize / referencePx} where referencePx is the cap height
 * we baked at.
 */
public final class SmoothText {

    /** Reference cap height in atlas pixels — matches GLYPH_BOX * 0.78 from baking
     *  (cell has padding around the glyph to stop bilinear bleed from neighbours). */
    private static final float REFERENCE_PX = SmoothFontAtlas.GLYPH_BOX * 0.78f;

    private SmoothText() {}

    /** Width in scaled-MC units that {@link #drawText} would produce. */
    public static int measureText(String text, float pxSize) {
        return measureText(text, pxSize, false);
    }

    /** Bold variant of {@link #measureText}. */
    public static int measureTextBold(String text, float pxSize) {
        return measureText(text, pxSize, true);
    }

    public static int measureText(String text, float pxSize, boolean bold) {
        SmoothFontTexture.ensureInitialised();
        SmoothFontAtlas atlas = bold ? SmoothFontTexture.atlasBold() : SmoothFontTexture.atlas();
        if (atlas == null || text.isEmpty()) return 0;
        float scale = pxSize / REFERENCE_PX;
        float total = 0;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            SmoothFontAtlas.Glyph g = atlas.glyph(cp);
            if (g != null) total += g.advance() * scale;
            i += Character.charCount(cp);
        }
        return Math.round(total);
    }

    /** Height in scaled-MC units for text drawn at pxSize. */
    public static int textHeight(float pxSize) {
        return Math.round(pxSize);
    }

    /**
     * Draw a string at (x, y) where y is the TOP of the glyph (matches MC
     * textRenderer.draw convention).  Tint comes from {@code color} (ARGB).
     *
     * <p>Sub-pixel positioning: per-glyph advance is fractional in screen pixels.
     * Rounding {@code cursorX} to int per glyph (as we used to) produces visibly
     * uneven gaps at small sizes because a 6.49 px advance lands on either 6 or
     * 7 px depending on cumulative drift. To fix it we push a matrix translate
     * for the fractional remainder and draw the texture at int(0, 0); MC then
     * rasterises at sub-pixel offset and the bilinear sampler resolves it
     * smoothly across two columns of texels.
     */
    public static void drawText(DrawContext ctx, String text, int x, int y, float pxSize, int color) {
        drawText(ctx, text, x, y, pxSize, color, false);
    }

    /** SemiBold/bold variant — draws from the second atlas baked with Font.BOLD
     *  (or from inter-semibold.ttf if present in resources). */
    public static void drawTextBold(DrawContext ctx, String text, int x, int y, float pxSize, int color) {
        drawText(ctx, text, x, y, pxSize, color, true);
    }

    public static void drawText(DrawContext ctx, String text, int x, int y,
                                  float pxSize, int color, boolean bold) {
        SmoothFontTexture.ensureInitialised();
        SmoothFontTexture.ensureGpuReady();
        SmoothFontAtlas atlas = bold ? SmoothFontTexture.atlasBold() : SmoothFontTexture.atlas();
        var texId = bold ? SmoothFontTexture.TEXTURE_ID_BOLD : SmoothFontTexture.TEXTURE_ID;
        if (atlas == null) {
            // Fallback to vanilla text so we never render literally nothing.
            ctx.drawText(MinecraftClient.getInstance().textRenderer, text, x, y, color, false);
            return;
        }

        float scale = pxSize / REFERENCE_PX;
        float cursorX = x;
        int atlasSize = atlas.atlasSize();
        int cellPx = atlas.cellPx();
        int dstSize = Math.max(1, Math.round(cellPx * scale));

        var matrices = ctx.getMatrices();
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            SmoothFontAtlas.Glyph g = atlas.glyph(cp);
            if (g != null) {
                int baseX = (int) Math.floor(cursorX);
                float frac = cursorX - baseX;
                matrices.pushMatrix();
                matrices.translate(frac, 0f);
                ctx.drawTexture(
                        RenderPipelines.GUI_TEXTURED,
                        texId,
                        baseX, y,
                        g.u(), g.v(),
                        dstSize, dstSize,
                        cellPx, cellPx,
                        atlasSize, atlasSize,
                        color
                );
                matrices.popMatrix();
                cursorX += g.advance() * scale;
            }
            i += Character.charCount(cp);
        }
    }

    /** Width in scaled-MC units that {@link #drawTextTracked} would produce. */
    public static int measureTextTracked(String text, float pxSize, float trackingEm) {
        int base = measureText(text, pxSize);
        if (text.isEmpty()) return base;
        return base + Math.round(trackingEm * pxSize * (text.length() - 1));
    }

    /**
     * Same as {@link #drawText} but inserts {@code trackingEm × pxSize} extra
     * advance after every glyph. Used for tracked uppercase section headers
     * (CSS letter-spacing equivalent) — e.g. {@code 0.08em} matches the
     * "MODULES / SYSTEM" caption style.
     */
    public static void drawTextTracked(DrawContext ctx, String text, int x, int y,
                                        float pxSize, int color, float trackingEm) {
        if (trackingEm == 0f) {
            drawText(ctx, text, x, y, pxSize, color);
            return;
        }
        SmoothFontTexture.ensureInitialised();
        SmoothFontTexture.ensureGpuReady();
        SmoothFontAtlas atlas = SmoothFontTexture.atlas();
        if (atlas == null) {
            ctx.drawText(MinecraftClient.getInstance().textRenderer, text, x, y, color, false);
            return;
        }
        float scale = pxSize / REFERENCE_PX;
        float cursorX = x;
        int atlasSize = atlas.atlasSize();
        int cellPx = atlas.cellPx();
        int dstSize = Math.max(1, Math.round(cellPx * scale));
        float trackingPx = trackingEm * pxSize;
        var matrices = ctx.getMatrices();
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            SmoothFontAtlas.Glyph g = atlas.glyph(cp);
            if (g != null) {
                int baseX = (int) Math.floor(cursorX);
                float frac = cursorX - baseX;
                matrices.pushMatrix();
                matrices.translate(frac, 0f);
                ctx.drawTexture(
                        RenderPipelines.GUI_TEXTURED,
                        SmoothFontTexture.TEXTURE_ID,
                        baseX, y,
                        g.u(), g.v(),
                        dstSize, dstSize,
                        cellPx, cellPx,
                        atlasSize, atlasSize,
                        color
                );
                matrices.popMatrix();
                cursorX += g.advance() * scale + trackingPx;
            }
            i += Character.charCount(cp);
        }
    }

    /** Draw centred vertically around {@code rowCenterY}. */
    public static void drawCentered(DrawContext ctx, String text, int x, int rowCenterY,
                                     float pxSize, int color) {
        int em = Math.round(pxSize);
        // Inter visual centre is ~10 % above the em centre, push the text down a touch.
        int nudge = Math.round(pxSize * 0.08f);
        drawText(ctx, text, x, rowCenterY - em / 2 + nudge, pxSize, color);
    }
}
