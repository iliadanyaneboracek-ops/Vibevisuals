package ru.suppelemen.vibevisuals.feature.screen;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.MinecraftClient;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;
import ru.suppelemen.vibevisuals.core.hud.HudElement;
import ru.suppelemen.vibevisuals.core.hud.HudManager;
import ru.suppelemen.vibevisuals.util.render.HudCardRenderer;

public class HudDragController {
    private static final long PULSE_DURATION_MS = 500L;
    private static final float DRAG_SCALE = 1.06f;

    private final boolean editorMode;
    private HudElement selected;
    private HudElement dragged;
    private int dragOffsetX;
    private int dragOffsetY;
    private long dragPulseStartedAt;
    private long releasePulseStartedAt;

    public HudDragController() {
        this(false);
    }

    public HudDragController(boolean editorMode) {
        this.editorMode = editorMode;
    }

    public HudElement selected() {
        return selected;
    }

    public void renderOutline(DrawContext context, int mouseX, int mouseY) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (selected != null && !selected.isVisibleForInteraction(client, editorMode)) {
            selected = null;
        }

        double logicalMouseX = toLogical(mouseX);
        double logicalMouseY = toLogical(mouseY);
        HudElement hovered = findElementAt(logicalMouseX, logicalMouseY, editorMode);
        HudElement outlined = dragged != null ? dragged : hovered != null ? hovered : selected;
        if (outlined != null) {
            if (dragged != null) {
                drawDragBodyHighlight(context, dragged);
            } else {
                drawShaderOutline(context, outlined, shouldPulse() ? 5 : 3);
            }
        }
    }

    public boolean mouseClicked(Click click) {
        if (click.button() != 0) {
            return false;
        }

        double logicalX = toLogical(click.x());
        double logicalY = toLogical(click.y());
        selected = findElementAt(logicalX, logicalY, editorMode);
        if (selected == null) {
            return false;
        }

        dragged = selected;
        dragOffsetX = (int) Math.round(logicalX) - selected.getX();
        dragOffsetY = (int) Math.round(logicalY) - selected.getY();
        dragPulseStartedAt = System.currentTimeMillis();
        return true;
    }

    public boolean mouseDragged(Click click, int screenWidth, int screenHeight) {
        if (dragged == null) {
            return false;
        }

        float scale = HudManager.getHudScale();
        int logicalScreenWidth = Math.max(1, Math.round(screenWidth / scale));
        int logicalScreenHeight = Math.max(1, Math.round(screenHeight / scale));
        int newX = clamp((int) Math.round(toLogical(click.x())) - dragOffsetX, 0, Math.max(0, logicalScreenWidth - dragged.getWidth()));
        int newY = clamp((int) Math.round(toLogical(click.y())) - dragOffsetY, 0, Math.max(0, logicalScreenHeight - dragged.getHeight()));
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

    private static HudElement findElementAt(double mouseX, double mouseY, boolean editorMode) {
        MinecraftClient client = MinecraftClient.getInstance();
        for (int index = HudManager.getElements().size() - 1; index >= 0; index--) {
            HudElement element = HudManager.getElements().get(index);
            if (element.isVisibleForInteraction(client, editorMode) && element.contains(mouseX, mouseY)) {
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
        float scale = HudManager.getHudScale();
        int scaledDistance = Math.max(1, Math.round(distance * scale));
        int x = Math.round(element.getX() * scale) - scaledDistance;
        int y = Math.round(element.getY() * scale) - scaledDistance;
        int width = Math.round(element.getWidth() * scale) + scaledDistance * 2;
        int height = Math.round(element.getHeight() * scale) + scaledDistance * 2;
        float radius = Math.min(14.0f, Math.max(6.0f, Math.min(width, height) / 5.0f));
        HudCardRenderer.drawShaderOutline(context, x, y, width, height, radius, Math.max(1.0f, 1.4f * scale), 0.92f);
    }

    private static void drawDragBodyHighlight(DrawContext context, HudElement element) {
        float scale = HudManager.getHudScale();
        int x = Math.round(element.getX() * scale);
        int y = Math.round(element.getY() * scale);
        int width = Math.round(element.getWidth() * scale);
        int height = Math.round(element.getHeight() * scale);
        int growX = Math.max(1, Math.round(width * (DRAG_SCALE - 1.0f) * 0.5f));
        int growY = Math.max(1, Math.round(height * (DRAG_SCALE - 1.0f) * 0.5f));
        HudCardRenderer.drawOverlayCard(context, x - growX, y - growY, width + growX * 2, height + growY * 2, Math.min(16.0f, Math.max(6.0f, height / 5.0f)), 0xFFFFFFFF, 0.10f);
        HudCardRenderer.drawShaderOutline(context, x - growX, y - growY, width + growX * 2, height + growY * 2, Math.min(16.0f, Math.max(6.0f, height / 5.0f)), Math.max(1.0f, 1.2f * scale), 0.80f);
    }

    private static double toLogical(double coordinate) {
        return coordinate / HudManager.getHudScale();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
