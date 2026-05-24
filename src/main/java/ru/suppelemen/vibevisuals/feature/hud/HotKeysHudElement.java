package ru.suppelemen.vibevisuals.feature.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import ru.suppelemen.vibevisuals.VibeVisualsClient;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;
import ru.suppelemen.vibevisuals.core.hud.HudElement;
import ru.suppelemen.vibevisuals.theme.MenuTheme;
import ru.suppelemen.vibevisuals.util.render.HudCardRenderer;
import ru.suppelemen.vibevisuals.util.render.HudGlass;

import java.util.List;

public class HotKeysHudElement extends HudElement {
    private static final StyleSpriteSource HUD_FONT = new StyleSpriteSource.Font(Identifier.of(VibeVisualsClient.MOD_ID, "hud"));

    private final VibeVisualsConfig.HotKeysConfig config;

    public HotKeysHudElement() {
        super("hot_keys", "Hot Keys", 0, 0, 0, 0);
        this.config = VibeVisualsConfigManager.get().hotKeysCard;
    }

    @Override
    public void render(DrawContext context, MinecraftClient client, float tickDelta, boolean editorMode) {
        syncFromConfig();
        if (!enabled) return;

        int ix = (int) Math.round(x);
        int iy = (int) Math.round(y);
        int radius = Math.round(config.radius);

        HudGlass.drawPanel(context, ix, iy, width, height, radius);

        int iconX = ix + config.padding + config.iconXOffset;
        int titleCenterY = iy + config.titleY + Math.max(config.iconSize, scaledTextHeight(config.titleTextScale)) / 2;
        int iconY = titleCenterY - config.iconSize / 2 + config.iconYOffset;
        int titleY = titleCenterY - scaledTextHeight(config.titleTextScale) / 2 + config.titleTextYOffset;

        drawKeyboardIcon(context, iconX, iconY, config.iconSize);
        drawScaledText(context, client, hudText("Hot Keys", false),
                ix + config.padding + config.iconSize + 6 + config.titleTextXOffset,
                titleY, HudGlass.textPrimary(), config.titleTextScale);

        // Subtle separator under the title.
        int sepY = titleY + scaledTextHeight(config.titleTextScale) + 3;
        context.fill(ix + config.padding, sepY, ix + width - config.padding, sepY + 1,
                MenuTheme.withAlpha(MenuTheme.TEXT_NEUTRAL, 0.10f));

        List<HotKeyEntry> entries = getEntries();
        int rowHeight = scaledTextHeight(config.rowTextScale);
        int rowY = iy + config.rowY;
        for (int index = 0; index < entries.size(); index++) {
            HotKeyEntry entry = entries.get(index);
            int entryY = rowY + index * (rowHeight + config.rowGap);
            drawScaledText(context, client, hudText(entry.label(), false),
                    ix + config.padding, entryY, HudGlass.textSecondary(), config.rowTextScale);

            Text keyText = hudText(entry.key(), true);
            int keyW = scaledTextWidth(client, keyText, config.rowTextScale);
            // Pill behind the key binding.
            int chipPad = 4;
            int chipH = rowHeight + 4;
            int chipX = ix + width - config.padding - keyW - chipPad * 2;
            int chipY = entryY - 2;
            HudGlass.drawChip(context, chipX, chipY, keyW + chipPad * 2, chipH, chipH / 2);
            drawScaledText(context, client, keyText,
                    chipX + chipPad,
                    entryY + config.keyTextYOffset,
                    HudGlass.textPrimary(), config.rowTextScale);
        }

        if (editorMode) {
            drawEditorOutline(context);
        }
    }

    @Override
    public boolean isVisibleForInteraction(MinecraftClient client, boolean editorMode) {
        syncFromConfig();
        return enabled;
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
    }

    private static void drawKeyboardIcon(DrawContext context, int x, int y, int size) {
        if (size <= 0) return;
        // Rounded accent square with a small key grid — matches the menu icon style.
        int radius = Math.max(2, size / 4);
        HudCardRenderer.drawOverlayCard(context, x, y, size, size, radius,
                MenuTheme.ACCENT_BRIGHT, 0.95f);
        int keySize = Math.max(1, size / 6);
        int gap = Math.max(1, size / 8);
        int startX = x + Math.max(2, size / 5);
        int startY = y + Math.max(2, size / 4);
        int keyColor = 0xFFFFFFFF;
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 3; col++) {
                int kx = startX + col * (keySize + gap);
                int ky = startY + row * (keySize + gap);
                HudCardRenderer.drawOverlayCard(context, kx, ky, keySize, keySize,
                        keySize / 2.0f, keyColor, 0.85f);
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
