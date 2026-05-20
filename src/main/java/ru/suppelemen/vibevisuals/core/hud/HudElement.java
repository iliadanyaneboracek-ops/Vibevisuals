package ru.suppelemen.vibevisuals.core.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

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

    protected void drawEditorOutline(DrawContext context) {
        int ix = (int) Math.round(x);
        int iy = (int) Math.round(y);
        int color = 0xAA8B5CF6;

        context.fill(ix, iy, ix + width, iy + 1, color);
        context.fill(ix, iy + height - 1, ix + width, iy + height, color);
        context.fill(ix, iy, ix + 1, iy + height, color);
        context.fill(ix + width - 1, iy, ix + width, iy + height, color);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
