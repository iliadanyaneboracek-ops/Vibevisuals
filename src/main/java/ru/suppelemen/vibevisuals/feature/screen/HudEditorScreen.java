package ru.suppelemen.vibevisuals.feature.screen;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;
import ru.suppelemen.vibevisuals.core.hud.HudElement;
import ru.suppelemen.vibevisuals.core.hud.HudManager;
import ru.suppelemen.vibevisuals.theme.HudCardRenderType;
import ru.suppelemen.vibevisuals.theme.HudVisualSettings;
import ru.suppelemen.vibevisuals.util.render.HudCardRenderer;

public class HudEditorScreen extends Screen {
    private final HudVisualSettings headerSettings = new HudVisualSettings();
    private HudElement selected;
    private HudElement dragged;
    private int dragOffsetX;
    private int dragOffsetY;
    private long dragPulseStartedAt;
    private long releasePulseStartedAt;
    private ButtonWidget configureButton;

    public HudEditorScreen() {
        super(Text.translatable("screen.vibevisuals.hud_editor"));
    }

    @Override
    protected void init() {
        headerSettings.renderType = HudCardRenderType.LIQUID_GLASS;
        headerSettings.radius = 8.0f;
        headerSettings.opacity = 0.80f;
        headerSettings.glow = false;
        headerSettings.blur = false;

        configureButton = ButtonWidget.builder(Text.translatable("screen.vibevisuals.configure"), button -> {
            if (selected != null && client != null) {
                client.setScreen(new HudSettingsScreen(this, selected));
            }
        }).dimensions(width / 2 - 50, height - 28, 100, 20).build();
        configureButton.active = false;
        addDrawableChild(configureButton);

        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.vibevisuals.done"), button -> close())
                .dimensions(width - 74, height - 28, 64, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderInGameBackground(context);
        HudManager.render(context, delta, true);

        HudElement hovered = findElementAt(mouseX, mouseY);
        HudElement outlined = dragged != null ? dragged : hovered != null ? hovered : selected;
        if (outlined != null) {
            drawRoundedOutline(context, outlined, shouldPulse() ? 5 : 3);
        }

        int headerWidth = 210;
        int headerX = width / 2 - headerWidth / 2;
        HudCardRenderer.drawCard(context, headerX, 8, headerWidth, 32, headerSettings);
        HudCardRenderer.drawOverlayCard(context, headerX, 8, headerWidth, 32, 8.0f, 0xFF090B12, 0.28f);
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("screen.vibevisuals.hud_editor"), width / 2, 13, 0xFFEFEFF6);
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("screen.vibevisuals.drag_hint"), width / 2, 26, 0xFFB7BBC9);

        configureButton.active = selected != null;
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) {
            return true;
        }

        if (click.button() != 0) {
            return false;
        }

        selected = findElementAt(click.x(), click.y());
        if (selected != null) {
            dragged = selected;
            dragOffsetX = (int) Math.round(click.x()) - selected.getX();
            dragOffsetY = (int) Math.round(click.y()) - selected.getY();
            dragPulseStartedAt = System.currentTimeMillis();
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (dragged == null) {
            return super.mouseDragged(click, offsetX, offsetY);
        }

        int newX = clamp((int) Math.round(click.x()) - dragOffsetX, 0, Math.max(0, width - dragged.getWidth()));
        int newY = clamp((int) Math.round(click.y()) - dragOffsetY, 0, Math.max(0, height - dragged.getHeight()));
        dragged.setPosition(newX, newY);
        HudManager.saveElementPosition(dragged, newX, newY);
        return true;
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (dragged != null) {
            VibeVisualsConfigManager.save();
            HudManager.reload();
            selected = findById(dragged.getId());
            dragged = null;
            releasePulseStartedAt = System.currentTimeMillis();
            return true;
        }

        return super.mouseReleased(click);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private boolean shouldPulse() {
        long now = System.currentTimeMillis();
        return now - dragPulseStartedAt < 1_000L || now - releasePulseStartedAt < 1_000L;
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

    private static void drawRoundedOutline(DrawContext context, HudElement element, int distance) {
        int x = element.getX() - distance;
        int y = element.getY() - distance;
        int width = element.getWidth() + distance * 2;
        int height = element.getHeight() + distance * 2;
        int right = x + width;
        int bottom = y + height;
        int radius = Math.min(8, Math.max(3, Math.min(width, height) / 5));
        int color = 0xEFFFFFFF;

        context.fill(x + radius, y, right - radius, y + 1, color);
        context.fill(x + radius, bottom - 1, right - radius, bottom, color);
        context.fill(x, y + radius, x + 1, bottom - radius, color);
        context.fill(right - 1, y + radius, right, bottom - radius, color);

        context.fill(x + radius - 3, y + 1, x + radius, y + 2, color);
        context.fill(x + 1, y + radius - 3, x + 2, y + radius, color);
        context.fill(right - radius, y + 1, right - radius + 3, y + 2, color);
        context.fill(right - 2, y + radius - 3, right - 1, y + radius, color);
        context.fill(x + radius - 3, bottom - 2, x + radius, bottom - 1, color);
        context.fill(x + 1, bottom - radius, x + 2, bottom - radius + 3, color);
        context.fill(right - radius, bottom - 2, right - radius + 3, bottom - 1, color);
        context.fill(right - 2, bottom - radius, right - 1, bottom - radius + 3, color);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
