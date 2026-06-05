package ru.suppelemen.vibevisuals.theme;

/**
 * Accent palettes the user can pick from "Themes" in the menu. Drives the
 * colour of toggle ON track, slider fill, active category pill, etc.
 *
 * <p>{@link #DEFAULT} keeps the original monochrome look — same as the menu
 * shipped with before themes existed.
 */
public enum AccentTheme {
    DEFAULT       ("Default",        0xFFE5E5EE),  // soft neutral white — original look
    OCEAN_BLUE    ("Ocean Blue",     0xFF5BA3FF),
    DEEP_MAGENTA  ("Deep Magenta",   0xFFC247B6),
    SUNSET_CORAL  ("Sunset Coral",   0xFFFF6E5A),
    FOREST_MINT   ("Forest Mint",    0xFF5BC8A4),
    ROYAL_LAVENDER("Royal Lavender", 0xFF9B7BFF),
    AMBER_GLOW    ("Amber Glow",     0xFFF5B544),
    CHERRY_BLOSSOM("Cherry Blossom", 0xFFFF7AB6);

    public final String label;
    /** Solid ARGB used for "on" states. */
    public final int color;

    AccentTheme(String label, int color) {
        this.label = label;
        this.color = color;
    }

    public static AccentTheme fromId(String id) {
        if (id == null) return DEFAULT;
        try { return AccentTheme.valueOf(id.toUpperCase().replace(' ', '_')); }
        catch (IllegalArgumentException e) { return DEFAULT; }
    }
}
