package ru.suppelemen.vibevisuals.feature.screen;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;
import ru.suppelemen.vibevisuals.core.hud.HudElement;
import ru.suppelemen.vibevisuals.core.hud.HudManager;
import ru.suppelemen.vibevisuals.util.render.HudCardRenderer;

public class HudDragController {
    private static final long PULSE_DURATION_MS = 500L;

    private HudElement selected;
    private HudElement dragged;
    private int dragOffsetX;
    private int dragOffsetY;
    private long dragPulseStartedAt;
    private long releasePulseStartedAt;

    public HudElement selected() {
        return selected;
    }

    public void renderOutline(DrawContext context, int mouseX, int mouseY) {
        HudElement hovered = findElementAt(mouseX, mouseY);
        HudElement outlined = dragged != null ? dragged : hovered != null ? hovered : selected;
        if (outlined != null) {
            drawShaderOutline(context, outlined, shouldPulse() ? 5 : 3);
        }
    }

    public boolean mouseClicked(Click click) {
        if (click.button() != 0) {
            return false;
        }

        selected = findElementAt(click.x(), click.y());
        if (selected == null) {
            return false;
        }

        dragged = selected;
        dragOffsetX = (int) Math.round(click.x()) - selected.getX();
        dragOffsetY = (int) Math.round(click.y()) - selected.getY();
        dragPulseStartedAt = System.currentTimeMillis();
        return true;
    }

    public boolean mouseDragged(Click click, int screenWidth, int screenHeight) {
        if (dragged == null) {
            return false;
        }

        int newX = clamp((int) Math.round(click.x()) - dragOffsetX, 0, Math.max(0, screenWidth - dragged.getWidth()));
        int newY = clamp((int) Math.round(click.y()) - dragOffsetY, 0, Math.max(0, screenHeight - dragged.getHeight()));
        dragged.setPosition(newX, newY);
        HudManager.saveElementPosition(dragged, newX, newY);
        return true;
    }

    public boolean mouseReleased() {
        if (dragged == null) {
            return false;
        }

        VibeVisualsConfigManager.save();
        HudManager.reload();
        selected = findById(dragged.getId());
        dragged = null;
        releasePulseStartedAt = System.currentTimeMillis();
        return true;
    }

    private boolean shouldPulse() {
        long now = System.currentTimeMillis();
        return now - dragPulseStartedAt < PULSE_DURATION_MS || now - releasePulseStartedAt < PULSE_DURATION_MS;
    }

    private static HudElement findElementAt(double mouseX, double mouseY) {
        for (int index = HudManager.getElements().size() - 1; index >= 0; index--) {
            HudElement element = HudManager.getElements().get(index);
            if (element.isEnabled() && element.contains(mouseX, mouseY)) {
                return element;
            }
        }

        return null;
    }

    private static HudElement findById(String id) {
        for (HudElement element : HudManager.getElements()) {
            if (element.getId().equals(id)) {
                return element;
            }
        }

        return null;
    }

    private static void drawShaderOutline(DrawContext context, HudElement element, int distance) {
        int x = element.getX() - distance;
        int y = element.getY() - distance;
        int width = element.getWidth() + distance * 2;
        int height = element.getHeight() + distance * 2;
        float radius = Math.min(14.0f, Math.max(6.0f, Math.min(width, height) / 5.0f));
        HudCardRenderer.drawShaderOutline(context, x, y, width, height, radius, 1.4f, 0.92f);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
