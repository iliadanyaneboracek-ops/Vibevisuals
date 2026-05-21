package ru.suppelemen.vibevisuals.feature.screen;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import ru.suppelemen.vibevisuals.core.hud.HudManager;
import ru.suppelemen.vibevisuals.theme.HudCardRenderType;
import ru.suppelemen.vibevisuals.theme.HudVisualSettings;
import ru.suppelemen.vibevisuals.util.render.HudCardRenderer;

public class HudEditorScreen extends Screen {
    private final HudVisualSettings headerSettings = new HudVisualSettings();
    private final HudDragController dragController = new HudDragController();
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
            if (dragController.selected() != null && client != null) {
                client.setScreen(new HudSettingsScreen(this, dragController.selected()));
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

        dragController.renderOutline(context, mouseX, mouseY);

        int headerWidth = 210;
        int headerX = width / 2 - headerWidth / 2;
        HudCardRenderer.drawCard(context, headerX, 8, headerWidth, 32, headerSettings);
        HudCardRenderer.drawOverlayCard(context, headerX, 8, headerWidth, 32, 8.0f, 0xFF090B12, 0.28f);
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("screen.vibevisuals.hud_editor"), width / 2, 13, 0xFFEFEFF6);
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("screen.vibevisuals.drag_hint"), width / 2, 26, 0xFFB7BBC9);

        configureButton.active = dragController.selected() != null;
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

        return dragController.mouseClicked(click);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        return dragController.mouseDragged(click, width, height) || super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        return dragController.mouseReleased() || super.mouseReleased(click);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

}
