package ru.suppelemen.vibevisuals.config;

public class VibeVisualsConfig {
    public boolean hudEnabled = true;

    public int cardX = 18;
    public int cardY = 120;
    public int cardWidth = 170;
    public int cardHeight = 68;

    public float cardRadius = 9.0f;
    public float cardOpacity = 0.92f;

    public int titleColor = 0xFFEFEFF6;
    public int subtitleColor = 0xFFD7DAE8;
    public int timerColor = 0xFFB7BBC9;

    public void validate() {
        cardWidth = Math.max(32, cardWidth);
        cardHeight = Math.max(24, cardHeight);
        cardRadius = clamp(cardRadius, 0.0f, 24.0f);
        cardOpacity = clamp(cardOpacity, 0.0f, 1.0f);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
