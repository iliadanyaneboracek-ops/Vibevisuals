package ru.suppelemen.vibevisuals.feature.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;
import ru.suppelemen.vibevisuals.core.hud.HudElement;
import ru.suppelemen.vibevisuals.theme.HudCardRenderType;
import ru.suppelemen.vibevisuals.theme.HudVisualSettings;
import ru.suppelemen.vibevisuals.util.render.HudCardRenderer;

public class CardTestHudElement extends HudElement {
    private final HudVisualSettings settings = new HudVisualSettings();

    public CardTestHudElement() {
        super(
                "card_test",
                "Card Test",
                VibeVisualsConfigManager.get().cardX,
                VibeVisualsConfigManager.get().cardY,
                VibeVisualsConfigManager.get().cardWidth,
                VibeVisualsConfigManager.get().cardHeight
        );

        settings.renderType = HudCardRenderType.LIQUID_GLASS;
        settings.radius = VibeVisualsConfigManager.get().cardRadius;
        settings.opacity = VibeVisualsConfigManager.get().cardOpacity;
        settings.glow = true;
        settings.blur = false;
    }

    @Override
    public void render(DrawContext context, MinecraftClient client, float tickDelta, boolean editorMode) {
        int ix = (int) Math.round(x);
        int iy = (int) Math.round(y);
        VibeVisualsConfig config = VibeVisualsConfigManager.get();

        HudCardRenderer.drawCard(context, ix, iy, width, height, settings);

        context.drawTextWithShadow(
                client.textRenderer,
                Text.literal("Potions"),
                ix + 16,
                iy + 12,
                config.titleColor
        );

        context.drawTextWithShadow(
                client.textRenderer,
                Text.literal("Invisibility I"),
                ix + 16,
                iy + 43,
                config.subtitleColor
        );

        String timer = "0:39";
        context.drawTextWithShadow(
                client.textRenderer,
                Text.literal(timer),
                ix + width - 14 - client.textRenderer.getWidth(timer),
                iy + 43,
                config.timerColor
        );

        if (editorMode) {
            drawEditorOutline(context);
        }
    }
}
