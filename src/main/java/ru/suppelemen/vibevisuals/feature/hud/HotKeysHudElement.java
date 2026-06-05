package ru.suppelemen.vibevisuals.feature.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
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
import ru.suppelemen.vibevisuals.util.render.HudGlass;

import java.util.List;

public class HotKeysHudElement extends HudElement {
    private static final StyleSpriteSource HUD_FONT =
            new StyleSpriteSource.Font(Identifier.of(VibeVisualsClient.MOD_ID, "hud"));
    private static final Identifier KEYBOARD_ICON =
            Identifier.of(VibeVisualsClient.MOD_ID, "textures/gui/hotkeys_keyboard_icon.png");
    private static final int KEYBOARD_ICON_TEX_SIZE = 256;

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

        List<HotKeyEntry> entries = getEntries();
        // Body rows are rendered at 80 % of the configured row scale — gives
        // a tighter, calmer hierarchy against the title.
        float bodyScale = config.rowTextScale * 0.80f;
        int rowHeight = scaledTextHeight(bodyScale);
        int rowY = iy + config.rowY;
        for (int index = 0; index < entries.size(); index++) {
            HotKeyEntry entry = entries.get(index);
            int entryY = rowY + index * (rowHeight + config.rowGap);
            drawScaledText(context, client, hudText(entry.label(), false),
                    ix + config.padding, entryY, HudGlass.textSecondary(), bodyScale);

            // No chip / pill behind the binding any more — read as plain text
            // aligned to the right edge.
            Text keyText = hudText(entry.key(), true);
            int keyW = scaledTextWidth(client, keyText, bodyScale);
            int keyX = ix + width - config.padding - keyW;
            drawScaledText(context, client, keyText,
                    keyX,
                    entryY + config.keyTextYOffset,
                    HudGlass.textPrimary(), bodyScale);
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
        drawKeyboardTexture(context, x + 1, y + 1, size, MenuTheme.withAlpha(MenuTheme.ACCENT_USER, 0.42f));
        drawKeyboardTexture(context, x, y, size, MenuTheme.withAlpha(HudGlass.textPrimary(), 0.94f));
    }

    private static void drawKeyboardTexture(DrawContext context, int x, int y, int size, int color) {
        context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                KEYBOARD_ICON,
                x,
                y,
                0.0f,
                0.0f,
                size,
                size,
                KEYBOARD_ICON_TEX_SIZE,
                KEYBOARD_ICON_TEX_SIZE,
                KEYBOARD_ICON_TEX_SIZE,
                KEYBOARD_ICON_TEX_SIZE,
                color
        );
    }

    private static void drawScaledText(DrawContext context, MinecraftClient client, Text text, int x, int y, int color, float scale) {
        int glyph = Math.max(6, Math.round(9.0f * scale));
        int nudgeUp = Math.round(glyph * 0.27f);
        ru.suppelemen.vibevisuals.util.font.SmoothText
                .drawTextBold(context, text.getString(), x, y - nudgeUp, glyph, color);
    }

    private static int scaledTextWidthBold(Text text, float scale) {
        int glyph = Math.max(6, Math.round(9.0f * scale));
        return ru.suppelemen.vibevisuals.util.font.SmoothText
                .measureTextBold(text.getString(), glyph);
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

    private static int scaledTextWidth(MinecraftClient client, Text text, float scale) {
        return scaledTextWidthBold(text, scale);
    }

    private static int scaledTextHeight(float scale) {
        return Math.round(9.0f * scale);
    }

    private static Text hudText(String text, boolean bold) {
        return Text.literal(text).styled(style -> style.withFont(HUD_FONT).withBold(bold));
    }

    private record HotKeyEntry(String label, String key) {
    }
}
