package ru.suppelemen.vibevisuals.feature.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import ru.suppelemen.vibevisuals.VibeVisualsClient;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;
import ru.suppelemen.vibevisuals.core.hud.HudElement;
import ru.suppelemen.vibevisuals.theme.HudCardRenderType;
import ru.suppelemen.vibevisuals.theme.HudVisualSettings;
import ru.suppelemen.vibevisuals.util.render.HudCardRenderer;

import java.util.List;

public class HotKeysHudElement extends HudElement {
    private static final StyleSpriteSource HUD_FONT = new StyleSpriteSource.Font(Identifier.of(VibeVisualsClient.MOD_ID, "hud"));

    private final VibeVisualsConfig.HotKeysConfig config;
    private final HudVisualSettings visualSettings = new HudVisualSettings();

    public HotKeysHudElement() {
        super("hot_keys", "Hot Keys", 0, 0, 0, 0);
        this.config = VibeVisualsConfigManager.get().hotKeysCard;
    }

    @Override
    public void render(DrawContext context, MinecraftClient client, float tickDelta, boolean editorMode) {
        syncFromConfig();

        if (!enabled) {
            return;
        }

        int ix = (int) Math.round(x);
        int iy = (int) Math.round(y);
        HudCardRenderer.drawCard(context, ix, iy, width, height, visualSettings);
        drawTitleBar(context, ix, iy);

        int iconX = ix + config.padding + config.iconXOffset;
        int titleCenterY = iy + config.titleY + Math.max(config.iconSize, scaledTextHeight(config.titleTextScale)) / 2;
        int iconY = titleCenterY - config.iconSize / 2 + config.iconYOffset;
        int titleY = titleCenterY - scaledTextHeight(config.titleTextScale) / 2 + config.titleTextYOffset;

        drawKeyboardIcon(context, iconX, iconY, config.iconSize);
        drawScaledText(
                context,
                client,
                hudText("Hot Keys", true),
                ix + config.padding + config.iconSize + 5 + config.titleTextXOffset,
                titleY,
                config.titleColor,
                config.titleTextScale
        );

        List<HotKeyEntry> entries = getEntries();
        int rowHeight = scaledTextHeight(config.rowTextScale);
        int rowY = iy + config.rowY;
        for (int index = 0; index < entries.size(); index++) {
            HotKeyEntry entry = entries.get(index);
            int entryY = rowY + index * (rowHeight + config.rowGap);
            drawScaledText(context, client, hudText(entry.label(), true), ix + config.padding, entryY, config.actionColor, config.rowTextScale);

            Text keyText = hudText(entry.key(), true);
            drawScaledText(
                    context,
                    client,
                    keyText,
                    ix + width - config.padding - scaledTextWidth(client, keyText, config.rowTextScale) + config.keyTextXOffset,
                    entryY + config.keyTextYOffset,
                    config.keyColor,
                    config.rowTextScale
            );
        }

        if (editorMode) {
            drawEditorOutline(context);
        }
    }

    private void syncFromConfig() {
        enabled = config.enabled;
        x = config.x;
        y = config.y;
        width = config.width;
        height = config.height;
        int entries = getEntries().size();
        if (entries > 0) {
            int rowHeight = scaledTextHeight(config.rowTextScale);
            int contentHeight = config.rowY
                    + entries * rowHeight
                    + Math.max(0, entries - 1) * config.rowGap
                    + config.bottomPadding;
            height = Math.max(height, contentHeight);
        }
        visualSettings.renderType = HudCardRenderType.LIQUID_GLASS;
        visualSettings.radius = config.radius;
        visualSettings.opacity = config.opacity;
        visualSettings.glow = false;
        visualSettings.blur = false;
    }

    private void drawTitleBar(DrawContext context, int x, int y) {
        int barHeight = Math.min(config.titleBarHeight, height);
        if (barHeight <= 0 || config.titleBarOpacity <= 0.0f) {
            return;
        }

        context.enableScissor(x, y, x + width, y + barHeight);
        HudCardRenderer.drawOverlayCard(context, x, y, width, height, config.radius, config.titleBarColor, config.titleBarOpacity);
        context.disableScissor();
    }

    private static void drawKeyboardIcon(DrawContext context, int x, int y, int size) {
        if (size <= 0) {
            return;
        }

        int color = 0xCC7C5CFF;
        int glow = 0x557C5CFF;
        context.fill(x, y + 1, x + size, y + size - 1, glow);
        context.fill(x + 1, y, x + size - 1, y + size, glow);
        context.fill(x, y, x + size, y + 1, color);
        context.fill(x, y + size - 1, x + size, y + size, color);
        context.fill(x, y, x + 1, y + size, color);
        context.fill(x + size - 1, y, x + size, y + size, color);

        int keyColor = 0xDD9D8CFF;
        int keySize = Math.max(1, size / 5);
        int gap = Math.max(1, size / 6);
        int startX = x + Math.max(2, size / 5);
        int startY = y + Math.max(2, size / 4);
        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 3; column++) {
                int keyX = startX + column * (keySize + gap);
                int keyY = startY + row * (keySize + gap);
                context.fill(keyX, keyY, keyX + keySize, keyY + keySize, keyColor);
            }
        }
    }

    private static void drawScaledText(DrawContext context, MinecraftClient client, Text text, int x, int y, int color, float scale) {
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(x, y);
        context.getMatrices().scale(scale, scale);
        context.drawText(client.textRenderer, text, 0, 0, color, false);
        context.getMatrices().popMatrix();
    }

    private static String getReloadKeyName() {
        KeyBinding keyBinding = VibeVisualsClient.getReloadConfigKey();
        return keyBinding != null ? keyBinding.getBoundKeyLocalizedText().getString() : "F8";
    }

    private static List<HotKeyEntry> getEntries() {
        return List.of(
                new HotKeyEntry("Reload Config", getReloadKeyName()),
                new HotKeyEntry("FullBright", getFullBrightKeyName())
        );
    }

    private static String getFullBrightKeyName() {
        KeyBinding keyBinding = VibeVisualsClient.getFullBrightKey();
        return keyBinding != null ? keyBinding.getBoundKeyLocalizedText().getString() : "B";
    }

    private record HotKeyEntry(String label, String key) {
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
