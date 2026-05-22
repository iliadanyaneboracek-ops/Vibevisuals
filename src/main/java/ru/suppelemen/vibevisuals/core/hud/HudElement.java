package ru.suppelemen.vibevisuals.core.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.lang.reflect.Field;

public abstract class HudElement {
    protected final String id;
    protected final String displayName;

    protected double x;
    protected double y;
    protected int width;
    protected int height;
    protected boolean enabled = true;

    protected HudElement(String id, String displayName, double x, double y, int width, int height) {
        this.id = id;
        this.displayName = displayName;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public abstract void render(DrawContext context, MinecraftClient client, float tickDelta, boolean editorMode);

    public void renderForced(DrawContext context, MinecraftClient client, float tickDelta, boolean editorMode) {
        render(context, client, tickDelta, editorMode);
    }

    public boolean isVisibleForInteraction(MinecraftClient client, boolean editorMode) {
        return enabled;
    }

    protected void drawEditorOutline(DrawContext context) {
        // Selection/hover outlines are drawn by HudEditorScreen.
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getX() {
        return (int) Math.round(x);
    }

    public int getY() {
        return (int) Math.round(y);
    }

    public int getWidth() {
        return Math.round(width * getSizeMultiplier());
    }

    public int getHeight() {
        return Math.round(height * getSizeMultiplier());
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseY >= y && mouseX <= x + getWidth() && mouseY <= y + getHeight();
    }

    public float getSizeMultiplier() {
        Object config = HudManager.getElementConfig(this);
        if (config == null) {
            return 1.0f;
        }
        try {
            Field field = config.getClass().getField("size");
            return Math.max(0.25f, Math.min(4.0f, field.getFloat(config)));
        } catch (NoSuchFieldException | IllegalAccessException exception) {
            return 1.0f;
        }
    }
}
