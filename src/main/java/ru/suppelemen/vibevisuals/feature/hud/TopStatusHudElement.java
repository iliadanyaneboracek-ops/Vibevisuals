package ru.suppelemen.vibevisuals.feature.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import ru.suppelemen.vibevisuals.VibeVisualsClient;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;
import ru.suppelemen.vibevisuals.core.hud.HudElement;
import ru.suppelemen.vibevisuals.theme.HudVisualSettings;
import ru.suppelemen.vibevisuals.util.render.HudCardRenderer;

public class TopStatusHudElement extends HudElement {
    private static final StyleSpriteSource HUD_FONT = new StyleSpriteSource.Font(Identifier.of(VibeVisualsClient.MOD_ID, "hud"));

    private final VibeVisualsConfig.TopBarConfig config;
    private final HudVisualSettings visualSettings = new HudVisualSettings();

    public TopStatusHudElement() {
        super("top_status", "Top Status", 0, 0, 0, 0);
        this.config = VibeVisualsConfigManager.get().topBar;
    }

    @Override
    public void render(DrawContext context, MinecraftClient client, float tickDelta, boolean editorMode) {
        syncFromConfig(client);

        if (!enabled) {
            return;
        }

        int ix = (int) Math.round(x);
        int iy = (int) Math.round(y);
        HudCardRenderer.drawCard(context, ix, iy, width, height, visualSettings);

        int cursorX = ix + config.padding;
        int centerY = iy + height / 2;
        if (config.iconSize > 0) {
            drawIconPlaceholder(context, cursorX + config.iconXOffset, centerY - config.iconSize / 2 + config.iconYOffset, config.iconSize);
            cursorX += config.iconSize + config.gap;
        }

        int textY = centerY - scaledTextHeight(config.textScale) / 2 + config.textYOffset;
        cursorX = drawText(context, client, hudText("vibevisuals", true), cursorX, textY, config.titleColor, config.textScale);
        cursorX = drawSeparator(context, client, cursorX, textY);
        cursorX = drawText(context, client, hudText(getPing(client) + " ms", true), cursorX, textY, config.statColor, config.textScale);
        cursorX = drawSeparator(context, client, cursorX, textY);
        drawText(context, client, hudText(client.getCurrentFps() + " FPS", true), cursorX, textY, config.statColor, config.textScale);

        if (editorMode) {
            drawEditorOutline(context);
        }
    }

    @Override
    public boolean isVisibleForInteraction(MinecraftClient client, boolean editorMode) {
        syncFromConfig(client);
        return enabled;
    }

    private void syncFromConfig(MinecraftClient client) {
        enabled = config.enabled;
        width = config.width;
        height = config.height;
        x = config.x >= 0 ? config.x : (client.getWindow().getScaledWidth() - width) / 2.0;
        y = config.y;

        visualSettings.radius = config.radius;
        visualSettings.opacity = config.opacity;
        visualSettings.renderType = ru.suppelemen.vibevisuals.theme.HudCardRenderType.LIQUID_GLASS;
        visualSettings.glow = false;
        visualSettings.blur = false;
    }

    private static int drawText(DrawContext context, MinecraftClient client, Text text, int x, int y, int color, float scale) {
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(x, y);
        context.getMatrices().scale(scale, scale);
        context.drawText(client.textRenderer, text, 0, 0, color, false);
        context.getMatrices().popMatrix();
        return x + scaledTextWidth(client, text, scale);
    }

    private int drawSeparator(DrawContext context, MinecraftClient client, int x, int y) {
        Text separator = hudText("/", true);
        int separatorX = x + config.gap;
        drawText(context, client, separator, separatorX, y, config.separatorColor, config.textScale);
        return separatorX + scaledTextWidth(client, separator, config.textScale) + config.gap;
    }

    private static void drawIconPlaceholder(DrawContext context, int x, int y, int size) {
        int color = 0xAA7C5CFF;
        int centerX = x + size / 2;
        int centerY = y + size / 2;
        context.fill(centerX - 1, y, centerX + 1, y + size, color);
        context.fill(x, centerY - 1, x + size, centerY + 1, color);
        context.fill(x + 1, y + 1, x + 2, y + 2, 0xFFB6A7FF);
        context.fill(x + size - 2, y + size - 2, x + size - 1, y + size - 1, 0xFFB6A7FF);
    }

    private static int getPing(MinecraftClient client) {
        if (client.player == null || client.getNetworkHandler() == null) {
            return 0;
        }

        PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
        return entry != null ? entry.getLatency() : 0;
    }

    private static int scaledTextWidth(MinecraftClient client, Text text, float scale) {
        return Math.round(client.textRenderer.getWidth(text) * scale);
    }

    private static int scaledTextHeight(float scale) {
        return Math.round(9.0f * scale);
    }

    private static Text hudText(String text, boolean bold) {
        return Text.literal(text).styled(style -> style.withFont(HUD_FONT).withBold(bold));
    }
}
