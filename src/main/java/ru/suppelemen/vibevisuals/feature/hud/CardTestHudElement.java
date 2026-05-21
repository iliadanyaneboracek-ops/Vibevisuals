package ru.suppelemen.vibevisuals.feature.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.registry.Registries;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import ru.suppelemen.vibevisuals.VibeVisualsClient;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;
import ru.suppelemen.vibevisuals.core.hud.HudCardElement;
import ru.suppelemen.vibevisuals.util.render.HudCardRenderer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CardTestHudElement extends HudCardElement {
    private static final Identifier POTION_ICON = Identifier.of(VibeVisualsClient.MOD_ID, "textures/gui/potion_bottle.png");
    private static final StyleSpriteSource HUD_FONT = new StyleSpriteSource.Font(Identifier.of(VibeVisualsClient.MOD_ID, "hud"));

    public CardTestHudElement() {
        super("potions", "Potions", VibeVisualsConfigManager.get().potionsCard);
    }

    @Override
    protected boolean shouldRenderCard(MinecraftClient client) {
        return !getVisibleEffects(client).isEmpty();
    }

    @Override
    protected void updateLayout(MinecraftClient client) {
        int effectCount = getVisibleEffects(client).size();
        int rowHeight = effectRowHeight(cardConfig);
        int contentHeight = cardConfig.effectsStartY
                + rowHeight
                + Math.max(0, effectCount - 1) * (rowHeight + cardConfig.rowGap)
                + cardConfig.bottomPadding;

        height = Math.max(cardConfig.height, contentHeight);
    }

    @Override
    protected void renderContent(DrawContext context, MinecraftClient client, float tickDelta, int x, int y) {
        VibeVisualsConfig.CardConfig config = cardConfig;
        int padding = config.padding;
        float textScale = config.textScale;
        int titleTextHeight = scaledTextHeight(textScale);
        int titleIconY = y + config.titleY + Math.max(0, (titleTextHeight - config.titleIconSize) / 2) + config.titleIconYOffset;
        int titleTextY = y + config.titleY + Math.max(0, (config.titleIconSize - titleTextHeight) / 2) + config.titleTextYOffset;

        drawTitleBar(context, x, y, config);
        drawTitleIcon(context, x + padding + config.titleIconXOffset, titleIconY, config.titleIconSize);
        drawScaledText(
                context,
                client,
                hudText("Potions", true),
                x + padding + config.titleIconSize + 6 + config.titleTextXOffset,
                titleTextY,
                config.titleColor,
                textScale
        );

        List<StatusEffectInstance> effects = getVisibleEffects(client);
        for (int index = 0; index < effects.size(); index++) {
            StatusEffectInstance effect = effects.get(index);
            drawEffectRow(
                    context,
                    client,
                    effect,
                    getEffectName(effect),
                    StatusEffectUtil.getDurationText(effect, 1.0f, 20.0f).getString(),
                    x,
                    y,
                    index,
                    config
            );
        }
    }

    private static void drawEffectRow(
            DrawContext context,
            MinecraftClient client,
            StatusEffectInstance effect,
            String name,
            String duration,
            int cardX,
            int cardY,
            int index,
            VibeVisualsConfig.CardConfig config
    ) {
        int iconX = cardX + config.padding;
        int rowHeight = effectRowHeight(config);
        int rowTop = cardY + config.effectsStartY + index * (rowHeight + config.rowGap);
        int rowCenterY = rowTop + rowHeight / 2;
        int textX = iconX + config.iconSize + 4;
        int textHeight = scaledTextHeight(config.effectTextScale);
        int timerHeight = scaledTextHeight(config.timerTextScale);
        int iconY = rowCenterY - config.iconSize / 2 + config.effectIconYOffset;
        int textY = rowCenterY - textHeight / 2;
        int timerY = rowCenterY - timerHeight / 2 + config.timerYOffset;
        Text durationText = hudText(duration, true);

        drawEffectIcon(context, effect, iconX, iconY, config.iconSize);
        drawScaledText(context, client, hudText(name, true), textX, textY, config.subtitleColor, config.effectTextScale);
        drawScaledText(
                context,
                client,
                durationText,
                cardX + config.width - config.padding - scaledTextWidth(client, durationText, config.timerTextScale) + config.timerXOffset,
                timerY,
                config.timerColor,
                config.timerTextScale
        );
    }

    private static void drawTitleBar(DrawContext context, int x, int y, VibeVisualsConfig.CardConfig config) {
        int barHeight = Math.min(config.titleBarHeight, config.height);
        if (barHeight <= 0 || config.titleBarOpacity <= 0.0f) {
            return;
        }

        context.enableScissor(x, y, x + config.width, y + barHeight);
        HudCardRenderer.drawOverlayCard(
                context,
                x,
                y,
                config.width,
                config.height,
                config.radius,
                config.titleBarColor,
                config.titleBarOpacity
        );
        context.disableScissor();
    }

    private static List<StatusEffectInstance> getVisibleEffects(MinecraftClient client) {
        if (client.player == null) {
            return List.of();
        }

        List<StatusEffectInstance> effects = new ArrayList<>(client.player.getStatusEffects());
        effects.removeIf(effect -> !effect.shouldShowIcon());
        effects.sort(Comparator.naturalOrder());
        return effects;
    }

    private static void drawEffectIcon(
            DrawContext context,
            StatusEffectInstance effect,
            int x,
            int y,
            int size
    ) {
        Identifier effectId = Registries.STATUS_EFFECT.getId(effect.getEffectType().value());
        context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                Identifier.of(effectId.getNamespace(), "textures/mob_effect/" + effectId.getPath() + ".png"),
                x,
                y,
                0.0f,
                0.0f,
                size,
                size,
                18,
                18,
                18,
                18
        );
    }

    private static void drawTitleIcon(DrawContext context, int x, int y, int size) {
        context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                POTION_ICON,
                x,
                y,
                0.0f,
                0.0f,
                size,
                size,
                32,
                32,
                32,
                32
        );
    }

    private static String getEffectName(StatusEffectInstance effect) {
        String name = Text.translatable(effect.getTranslationKey()).getString();
        String amplifier = getAmplifierName(effect.getAmplifier());
        return amplifier.isEmpty() ? name : name + " " + amplifier;
    }

    private static String getAmplifierName(int amplifier) {
        return switch (amplifier) {
            case 0 -> "";
            case 1 -> "II";
            case 2 -> "III";
            case 3 -> "IV";
            case 4 -> "V";
            default -> amplifier > 0 ? Integer.toString(amplifier + 1) : "";
        };
    }

    private static void drawScaledText(
            DrawContext context,
            MinecraftClient client,
            Text text,
            int x,
            int y,
            int color,
            float scale
    ) {
        if (scale == 1.0f) {
            context.drawText(client.textRenderer, text, x, y, color, false);
            return;
        }

        context.getMatrices().pushMatrix();
        context.getMatrices().translate(x, y);
        context.getMatrices().scale(scale, scale);
        context.drawText(client.textRenderer, text, 0, 0, color, false);
        context.getMatrices().popMatrix();
    }

    private static int scaledTextWidth(MinecraftClient client, Text text, float scale) {
        return Math.round(client.textRenderer.getWidth(text) * scale);
    }

    private static int scaledTextHeight(float scale) {
        return Math.round(9.0f * scale);
    }

    private static int effectRowHeight(VibeVisualsConfig.CardConfig config) {
        return Math.max(config.iconSize, scaledTextHeight(config.effectTextScale));
    }

    private static Text hudText(String text, boolean bold) {
        return Text.literal(text).styled(style -> style.withFont(HUD_FONT).withBold(bold));
    }
}
