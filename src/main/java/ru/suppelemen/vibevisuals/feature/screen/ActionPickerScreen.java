package ru.suppelemen.vibevisuals.feature.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;
import ru.suppelemen.vibevisuals.feature.keybind.ModAction;
import ru.suppelemen.vibevisuals.feature.keybind.MultiKeyBinding;
import ru.suppelemen.vibevisuals.theme.HudCardRenderType;
import ru.suppelemen.vibevisuals.theme.HudVisualSettings;
import ru.suppelemen.vibevisuals.util.render.HudCardRenderer;

import java.util.ArrayList;
import java.util.List;

public class ActionPickerScreen extends Screen {
    private static final int ROW_HEIGHT = 18;
    private static final int ROW_GAP = 3;

    private static final int COLOR_TEXT_PRIMARY = 0xFFEFEFF6;
    private static final int COLOR_TEXT_MUTED = 0xFFB7BBC9;
    private static final int COLOR_ACCENT = 0xFF7C5CFF;
    private static final int COLOR_CARD_OFF = 0xFF090B12;
    private static final int COLOR_CARD_ON = 0xFF201A42;
    private static final int COLOR_HEADER = 0xFF050710;

    private final HudVisualSettings panelSettings = new HudVisualSettings();
    private final Screen parent;
    private final MultiKeyBinding binding;
    private final List<String> selected;
    private int scrollOffset;
    private Hit hoveredHit;

    public ActionPickerScreen(Screen parent, MultiKeyBinding binding) {
        super(Text.literal("Actions: " + (binding.displayName == null ? "" : binding.displayName)));
        this.parent = parent;
        this.binding = binding;
        this.selected = new ArrayList<>(binding.actions);
    }

    @Override
    protected void init() {
        panelSettings.renderType = HudCardRenderType.LIQUID_GLASS;
        panelSettings.radius = 10.0f;
        panelSettings.opacity = 0.82f;
        panelSettings.glow = false;
        panelSettings.blur = false;
    }

    private int panelX() {
        return Math.max(20, width / 2 - panelW() / 2);
    }

    private int panelY() {
        return 30;
    }

    private int panelW() {
        return Math.min(380, width - 40);
    }

    private int panelH() {
        return Math.min(310, height - 60);
    }

    private int listTop() {
        return panelY() + 64;
    }

    private int listBottom() {
        return panelY() + panelH() - 14;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderInGameBackground(context);
        hoveredHit = null;

        int px = panelX();
        int py = panelY();
        int pw = panelW();
        int ph = panelH();

        HudCardRenderer.drawCard(context, px, py, pw, ph, panelSettings);
        HudCardRenderer.drawOverlayCard(context, px, py, pw, 26, 10.0f, COLOR_HEADER, 0.34f);
        context.drawTextWithShadow(textRenderer, title, px + 12, py + 8, COLOR_TEXT_PRIMARY);

        int barY = py + 32;
        int barH = 18;
        int saveW = 80;
        int cancelW = 80;
        int clearW = 80;
        int barX = px + 10;
        int barW = pw - 20;

        hitButton(context, mouseX, mouseY, barX, barY, saveW, barH,
                Text.literal("Save"), COLOR_ACCENT, 0.62f, Hit.Type.SAVE);
        hitButton(context, mouseX, mouseY, barX + saveW + 6, barY, cancelW, barH,
                Text.literal("Cancel"), 0xFF252936, 0.55f, Hit.Type.CANCEL);
        hitButton(context, mouseX, mouseY, barX + barW - clearW, barY, clearW, barH,
                Text.literal("Clear all"), 0xFF4A1B1B, 0.55f, Hit.Type.CLEAR);

        context.enableScissor(px + 4, listTop() - 2, px + pw - 4, listBottom());
        renderList(context, mouseX, mouseY, px + 10, listTop() - scrollOffset, pw - 20);
        context.disableScissor();

        renderScrollIndicator(context, px + pw - 6, listTop(), listBottom());

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderList(DrawContext context, int mouseX, int mouseY, int x, int yStart, int w) {
        int y = yStart;
        ModAction[] values = ModAction.values();
        for (int i = 0; i < values.length; i++) {
            ModAction action = values[i];
            renderRow(context, mouseX, mouseY, action, x, y, w, i);
            y += ROW_HEIGHT + ROW_GAP;
        }
    }

    private void renderRow(DrawContext context, int mouseX, int mouseY, ModAction action,
                           int x, int y, int w, int index) {
        if (y + ROW_HEIGHT < listTop() || y > listBottom()) {
            return;
        }

        boolean isSelected = selected.contains(action.id());
        boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + ROW_HEIGHT;
        int bg = isSelected ? COLOR_CARD_ON : COLOR_CARD_OFF;
        float opacity = (isSelected ? 0.54f : 0.32f) + (hovered ? 0.10f : 0.0f);
        HudCardRenderer.drawOverlayCard(context, x, y, w, ROW_HEIGHT, 5.0f, bg, opacity);

        // Checkbox indicator
        int boxSize = 9;
        int boxX = x + 8;
        int boxY = y + (ROW_HEIGHT - boxSize) / 2;
        HudCardRenderer.drawOverlayCard(context, boxX, boxY, boxSize, boxSize, 2.0f,
                isSelected ? COLOR_ACCENT : 0xFF252936, 0.85f);
        if (isSelected) {
            context.fill(boxX + 2, boxY + 2, boxX + boxSize - 2, boxY + boxSize - 2, 0xFFFFFFFF);
        }

        int textX = boxX + boxSize + 8;
        context.drawTextWithShadow(textRenderer, Text.literal(action.displayName()),
                textX, y + 5, isSelected ? COLOR_TEXT_PRIMARY : COLOR_TEXT_MUTED);

        String idText = action.id();
        int idW = textRenderer.getWidth(idText);
        context.drawTextWithShadow(textRenderer, Text.literal(idText),
                x + w - idW - 8, y + 5, 0xFF6E7282);

        if (hovered) {
            hoveredHit = new Hit(Hit.Type.TOGGLE_ACTION, action);
        }
    }

    private void hitButton(DrawContext context, int mouseX, int mouseY,
                           int x, int y, int w, int h, Text label, int color, float opacity, Hit.Type type) {
        boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        HudCardRenderer.drawOverlayCard(context, x, y, w, h, h / 3.0f, color, hovered ? opacity + 0.16f : opacity);
        context.drawCenteredTextWithShadow(textRenderer, label, x + w / 2, y + (h - 8) / 2, COLOR_TEXT_PRIMARY);
        if (hovered) {
            hoveredHit = new Hit(type, null);
        }
    }

    private void renderScrollIndicator(DrawContext context, int x, int top, int bottom) {
        int total = ModAction.values().length * (ROW_HEIGHT + ROW_GAP);
        int visible = bottom - top;
        if (total <= visible) {
            return;
        }
        float ratio = (float) visible / (float) total;
        int trackH = bottom - top;
        int thumbH = Math.max(20, (int) (trackH * ratio));
        int maxScroll = total - visible;
        float scrollRatio = maxScroll == 0 ? 0 : (float) scrollOffset / maxScroll;
        int thumbY = top + (int) ((trackH - thumbH) * scrollRatio);
        context.fill(x, top, x + 3, bottom, 0x33000000);
        context.fill(x, thumbY, x + 3, thumbY + thumbH, COLOR_ACCENT);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) {
            return true;
        }
        if (hoveredHit == null) {
            return false;
        }

        playClickSound();
        switch (hoveredHit.type) {
            case SAVE -> {
                binding.actions.clear();
                binding.actions.addAll(selected);
                VibeVisualsConfigManager.get().validate();
                VibeVisualsConfigManager.save();
                if (client != null) {
                    client.setScreen(parent);
                }
            }
            case CANCEL -> {
                if (client != null) {
                    client.setScreen(parent);
                }
            }
            case CLEAR -> selected.clear();
            case TOGGLE_ACTION -> {
                if (hoveredHit.action != null) {
                    if (selected.contains(hoveredHit.action.id())) {
                        selected.remove(hoveredHit.action.id());
                    } else {
                        selected.add(hoveredHit.action.id());
                    }
                }
            }
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int total = ModAction.values().length * (ROW_HEIGHT + ROW_GAP);
        int visible = listBottom() - listTop();
        int maxScroll = Math.max(0, total - visible);
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) (verticalAmount * 18)));
        return true;
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void playClickSound() {
        MinecraftClient minecraft = MinecraftClient.getInstance();
        minecraft.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK.value(), 1.05f, 0.045f));
    }

    private static class Hit {
        enum Type { SAVE, CANCEL, CLEAR, TOGGLE_ACTION }

        final Type type;
        final ModAction action;

        Hit(Type type, ModAction action) {
            this.type = type;
            this.action = action;
        }
    }
}