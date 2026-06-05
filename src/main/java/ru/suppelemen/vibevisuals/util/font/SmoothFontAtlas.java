package ru.suppelemen.vibevisuals.util.font;

import net.minecraft.client.texture.NativeImage;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * AWT-baked font atlas — pre-rasterises a TTF at a large size into a single
 * texture. Designed to be uploaded to MC as a NativeImageBackedTexture with
 * a LINEAR sampler (see {@link SmoothFontTexture}) so the renderer can scale
 * glyphs down smoothly instead of MC's default nearest-neighbour atlas.
 *
 * <p>Bake size is intentionally generous (CELL_PX = 96) so the runtime
 * downscale to ~10-13 px on screen is well above 4× — bilinear sampling at
 * that ratio gives near-vector quality.
 *
 * <p>Advances are stored as float (in atlas pixels) — this is the source of
 * kerning quality. Integer advances at small sizes round to the same pixel
 * for visually different glyphs, which is what makes vanilla MC text look
 * "monospaced-ish" up close.
 */
public final class SmoothFontAtlas {

    /** Each glyph cell side length in atlas pixels (rendered at ~0.78× this for cap height). */
    public static final int CELL_PX = 384;
    /** Transparent padding in pixels on each side of the glyph inside its cell. */
    public static final int CELL_PAD = 32;
    /** Inner area available to the actual glyph (≤ CELL_PX). */
    public static final int GLYPH_BOX = CELL_PX - 2 * CELL_PAD; // 320
    /** Atlas image side length. {@code ATLAS_SIZE / CELL_PX} = grid dim. */
    public static final int ATLAS_SIZE = 8192;
    public static final int GRID = ATLAS_SIZE / CELL_PX; // = 21 → 441 slots

    private final NativeImage atlas;
    private final Map<Integer, Glyph> glyphs;
    private final int ascent;

    public SmoothFontAtlas(NativeImage atlas, Map<Integer, Glyph> glyphs, int ascent) {
        this.atlas = atlas;
        this.glyphs = glyphs;
        this.ascent = ascent;
    }

    public NativeImage atlas() { return atlas; }
    public Glyph glyph(int codepoint) { return glyphs.get(codepoint); }
    public int atlasSize() { return ATLAS_SIZE; }
    public int cellPx() { return CELL_PX; }
    public int ascent() { return ascent; }

    /** Per-glyph metrics packed into the atlas. Advance is fractional in atlas pixels. */
    public record Glyph(int codepoint, int u, int v, int w, int h, float advance) {}

    /**
     * Bake an atlas from the provided TTF stream.  Atlas covers ASCII printable
     * + basic Cyrillic + a handful of punctuation.  Caller is responsible for
     * uploading the resulting NativeImage to a GPU texture.
     */
    public static SmoothFontAtlas bake(InputStream ttfStream) throws IOException {
        return bake(ttfStream, false);
    }

    /**
     * Bake an atlas, optionally with the {@code Font.BOLD} style applied. Use
     * this overload to produce a second atlas next to the regular one for UI
     * elements that need a heavier weight (titles, active labels, etc.).
     */
    public static SmoothFontAtlas bake(InputStream ttfStream, boolean bold) throws IOException {
        Font baseFont;
        try {
            baseFont = Font.createFont(Font.TRUETYPE_FONT, ttfStream)
                    .deriveFont((float) (GLYPH_BOX * 0.78f));
            if (bold) baseFont = baseFont.deriveFont(Font.BOLD);
        } catch (Exception e) {
            throw new IOException("Failed to load TTF for SmoothFontAtlas", e);
        }
        return bakeFromFont(baseFont);
    }

    private static SmoothFontAtlas bakeFromFont(Font font) {
        BufferedImage bi = new BufferedImage(ATLAS_SIZE, ATLAS_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bi.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setComposite(java.awt.AlphaComposite.Src);
        g.setColor(new Color(0, 0, 0, 0));
        g.fillRect(0, 0, ATLAS_SIZE, ATLAS_SIZE);
        g.setFont(font);
        FontMetrics m = g.getFontMetrics();
        FontRenderContext frc = g.getFontRenderContext();

        Map<Integer, Glyph> map = new HashMap<>();
        int slot = 0;
        slot = bakeRange(g, m, frc, 0x0020, 0x007E, slot, map);                // ASCII printable
        slot = bakeRange(g, m, frc, 0x0400, 0x04FF, slot, map);                // Basic Cyrillic
        // Useful punctuation
        for (int cp : new int[]{0x2026, 0x2013, 0x2014, 0x221E, 0x00A0, 0x00B7}) {
            if (slot >= GRID * GRID) break;
            slot = bakeOne(g, m, frc, cp, slot, map);
        }
        g.dispose();

        // Copy ARGB BufferedImage → NativeImage (which is RGBA in memory, NOT premultiplied).
        NativeImage native_ = new NativeImage(NativeImage.Format.RGBA, ATLAS_SIZE, ATLAS_SIZE, false);
        for (int y = 0; y < ATLAS_SIZE; y++) {
            for (int x = 0; x < ATLAS_SIZE; x++) {
                int argb = bi.getRGB(x, y);
                int a = (argb >>> 24) & 0xFF;
                int r = (argb >>> 16) & 0xFF;
                int gC = (argb >>> 8) & 0xFF;
                int b = argb & 0xFF;
                // NativeImage stores as ABGR little-endian per pixel.
                int abgr = (a << 24) | (b << 16) | (gC << 8) | r;
                native_.setColorArgb(x, y, abgr);
            }
        }
        return new SmoothFontAtlas(native_, map, m.getAscent());
    }

    private static int bakeRange(Graphics2D g, FontMetrics m, FontRenderContext frc, int from, int to, int slot, Map<Integer, Glyph> out) {
        for (int cp = from; cp <= to && slot < GRID * GRID; cp++) {
            slot = bakeOne(g, m, frc, cp, slot, out);
        }
        return slot;
    }

    private static int bakeOne(Graphics2D g, FontMetrics m, FontRenderContext frc, int codepoint, int slot, Map<Integer, Glyph> out) {
        if (!g.getFont().canDisplay(codepoint)) return slot;
        int cellX = (slot % GRID) * CELL_PX;
        int cellY = (slot / GRID) * CELL_PX;
        String s = new String(Character.toChars(codepoint));
        g.setColor(Color.WHITE);
        // Draw inside the inner padded box; padding stays transparent so bilinear
        // sampling at glyph edges fetches only zero-alpha neighbours.
        g.drawString(s, cellX + CELL_PAD, cellY + CELL_PAD + m.getAscent());

        // Fractional advance via GlyphVector — far more accurate than charWidth(int).
        GlyphVector gv = g.getFont().createGlyphVector(frc, s);
        float advance;
        if (gv.getNumGlyphs() > 0) {
            advance = (float) gv.getGlyphMetrics(0).getAdvanceX();
        } else {
            Rectangle2D b = g.getFont().getStringBounds(s, frc);
            advance = (float) b.getWidth();
        }

        out.put(codepoint, new Glyph(
                codepoint,
                cellX, cellY,
                CELL_PX, CELL_PX,
                advance
        ));
        return slot + 1;
    }
}
