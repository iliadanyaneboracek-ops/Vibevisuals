package ru.suppelemen.vibevisuals.core.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.theme.HudVisualSettings;
import ru.suppelemen.vibevisuals.util.render.HudCardRenderer;

public abstract class HudCardElement extends HudElement {
    protected final VibeVisualsConfig.CardConfig cardConfig;
    protected final HudVisualSettings visualSettings = new HudVisualSettings();

    protected HudCardElement(String id, String displayName, VibeVisualsConfig.CardConfig cardConfig) {
        super(id, displayName, cardConfig.x, cardConfig.y, cardConfig.width, cardConfig.height);
        this.cardConfig = cardConfig;
        syncFromConfig();
    }

    @Override
    public final void render(DrawContext context, MinecraftClient client, float tickDelta, boolean editorMode) {
        syncFromConfig();

        if (!enabled || !shouldRenderCard(client)) {
            return;
        }

        updateLayout(client);

        int ix = (int) Math.round(x);
        int iy = (int) Math.round(y);

        HudCardRenderer.drawCard(context, ix, iy, width, height, visualSettings);
        renderContent(context, client, tickDelta, ix, iy);

        if (editorMode) {
            drawEditorOutline(context);
        }
    }

    protected abstract void renderContent(
            DrawContext context,
            MinecraftClient client,
            float tickDelta,
            int x,
            int y
    );

    protected boolean shouldRenderCard(MinecraftClient client) {
        return true;
    }

    protected void updateLayout(MinecraftClient client) {
    }

    private void syncFromConfig() {
        enabled = cardConfig.enabled;
        x = cardConfig.x;
        y = cardConfig.y;
        width = cardConfig.width;
        height = cardConfig.height;

        visualSettings.renderType = cardConfig.renderType;
        visualSettings.radius = cardConfig.radius;
        visualSettings.opacity = cardConfig.opacity;
        visualSettings.glow = cardConfig.glow;
        visualSettings.blur = cardConfig.blur;
    }
}
