package ru.suppelemen.vibevisuals.feature.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;
import ru.suppelemen.vibevisuals.feature.marker.MarkerManager;
import ru.suppelemen.vibevisuals.theme.HudCardRenderType;
import ru.suppelemen.vibevisuals.theme.HudVisualSettings;
import ru.suppelemen.vibevisuals.util.render.HudCardRenderer;

public class MarkersScreen extends Screen {
    private final HudVisualSettings settings = new HudVisualSettings();

    public MarkersScreen() {
        super(Text.translatable("screen.vibevisuals.markers"));
    }

    @Override
    protected void init() {
        settings.renderType = HudCardRenderType.LIQUID_GLASS;
        settings.radius = VibeVisualsConfigManager.get().menu.radius;
        settings.opacity = VibeVisualsConfigManager.get().menu.opacity;
        settings.glow = false;
        settings.blur = false;

        int panelW = 200;
        int panelX = width / 2 - panelW / 2;
        int panelY = height / 2 - 84;
        int buttonW = panelW - 24;
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.vibevisuals.marker.add_self"), button -> MarkerManager.addManualAtSelf(MinecraftClient.getInstance()))
                .dimensions(panelX + 12, panelY + 32, buttonW, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.vibevisuals.marker.add"), button -> MarkerManager.addManualAtCrosshair(MinecraftClient.getInstance()))
                .dimensions(panelX + 12, panelY + 56, buttonW, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.vibevisuals.marker.remove_last"), button -> MarkerManager.removeLast())
                .dimensions(panelX + 12, panelY + 80, buttonW, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.vibevisuals.marker.clear"), button -> MarkerManager.clear())
                .dimensions(panelX + 12, panelY + 104, buttonW, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.vibevisuals.back"), button -> close())
                .dimensions(panelX + 48, panelY + 142, panelW - 96, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderInGameBackground(context);
        VibeVisualsConfig.MenuConfig menu = VibeVisualsConfigManager.get().menu;
        int panelW = 200;
        int panelH = 178;
        int panelX = width / 2 - panelW / 2;
        int panelY = height / 2 - 90;
        HudCardRenderer.drawCard(context, panelX, panelY, panelW, panelH, settings);
        HudCardRenderer.drawOverlayCard(context, panelX, panelY, panelW, 24, menu.radius, menu.backgroundColor, menu.headerOpacity);
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("screen.vibevisuals.markers"), width / 2, panelY + 7, menu.titleColor);
        int count = MarkerManager.count();
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("screen.vibevisuals.marker.count", count), width / 2, panelY + 130, menu.mutedTextColor);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
