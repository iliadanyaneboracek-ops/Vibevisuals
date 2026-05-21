package ru.suppelemen.vibevisuals.feature.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import ru.suppelemen.vibevisuals.VibeVisualsClient;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;
import ru.suppelemen.vibevisuals.core.hud.HudElement;
import ru.suppelemen.vibevisuals.feature.pvp.PvpCombatTracker;
import ru.suppelemen.vibevisuals.theme.HudCardRenderType;
import ru.suppelemen.vibevisuals.theme.HudVisualSettings;
import ru.suppelemen.vibevisuals.util.render.HudCardRenderer;

public class PvpCombatHudElement extends HudElement {
    private static final StyleSpriteSource HUD_FONT = new StyleSpriteSource.Font(Identifier.of(VibeVisualsClient.MOD_ID, "hud"));
    private static final ItemStack TOTEM_STACK = new ItemStack(Items.TOTEM_OF_UNDYING);
    private static final ItemStack GOLDEN_APPLE_STACK = new ItemStack(Items.GOLDEN_APPLE);

    private final VibeVisualsConfig.PvpCardConfig config;
    private final HudVisualSettings visualSettings = new HudVisualSettings();

    public PvpCombatHudElement() {
        super("pvp_combat", "PvP Combat", 0, 0, 0, 0);
        this.config = VibeVisualsConfigManager.get().pvpCard;
    }

    @Override
    public void render(DrawContext context, MinecraftClient client, float tickDelta, boolean editorMode) {
        syncFromConfig();

        if (!enabled || (!editorMode && !PvpCombatTracker.isActive())) {
            return;
        }

        AbstractClientPlayerEntity target = PvpCombatTracker.getTarget(client);
        int ix = (int) Math.round(x);
        int iy = (int) Math.round(y);
        HudCardRenderer.drawCard(context, ix, iy, width, height, visualSettings);

        int avatarX = ix + config.padding + config.avatarXOffset;
        int avatarY = iy + config.padding + config.avatarYOffset;
        drawAvatar(context, target, avatarX, avatarY, config.avatarSize);

        int textX = avatarX + config.avatarSize + 7;
        int nameY = iy + config.padding + config.nameYOffset;
        String name = target != null ? target.getName().getString() : editorMode ? "Player" : PvpCombatTracker.getTargetName();
        drawScaledText(context, client, hudText(name, true), textX + config.nameXOffset, nameY, config.nameColor, config.nameTextScale);

        float health = target != null ? Math.max(0.0f, target.getHealth() + target.getAbsorptionAmount()) : 0.0f;
        float maxHealth = target != null ? Math.max(1.0f, target.getMaxHealth() + target.getAbsorptionAmount()) : 20.0f;
        String stats = "HP / " + formatHealth(health);
        drawScaledText(context, client, hudText(stats, true), textX + config.statsXOffset, nameY + scaledTextHeight(config.nameTextScale) + 2 + config.statsYOffset, config.statsColor, config.statsTextScale);

        int totems = editorMode ? 0 : PvpCombatTracker.getTotemPops();
        int goldenApples = editorMode ? 0 : PvpCombatTracker.getGoldenApples();

        drawItemCounter(
                context,
                client,
                TOTEM_STACK,
                totems,
                textX,
                iy + config.itemY,
                config.totemCountXOffset,
                config.totemCountYOffset,
                config
        );
        drawItemCounter(
                context,
                client,
                GOLDEN_APPLE_STACK,
                goldenApples,
                textX + 32,
                iy + config.itemY,
                config.goldenAppleCountXOffset,
                config.goldenAppleCountYOffset,
                config
        );
        drawHealthBar(context, ix + config.padding, iy + config.barY, width - config.padding * 2, config.barHeight, health / maxHealth);

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
        visualSettings.renderType = HudCardRenderType.LIQUID_GLASS;
        visualSettings.radius = config.radius;
        visualSettings.opacity = config.opacity;
        visualSettings.glow = false;
        visualSettings.blur = false;
    }

    private static void drawAvatar(DrawContext context, AbstractClientPlayerEntity target, int x, int y, int size) {
        if (target == null) {
            context.fill(x, y, x + size, y + size, 0xFFEFEFF6);
            context.fill(x + size / 4, y + size / 3, x + size / 3, y + size / 2, 0xFF1A1D26);
            context.fill(x + size * 2 / 3, y + size / 3, x + size * 3 / 4, y + size / 2, 0xFF1A1D26);
            context.fill(x + size / 4, y + size * 2 / 3, x + size * 3 / 4, y + size * 3 / 4, 0xFF1A1D26);
            return;
        }

        Identifier skin = target.getSkin().body().texturePath();
        context.drawTexture(RenderPipelines.GUI_TEXTURED, skin, x, y, 8.0f, 8.0f, size, size, 8, 8, 64, 64);
        context.drawTexture(RenderPipelines.GUI_TEXTURED, skin, x, y, 40.0f, 8.0f, size, size, 8, 8, 64, 64);
    }

    private static void drawItemCounter(
            DrawContext context,
            MinecraftClient client,
            ItemStack stack,
            int count,
            int x,
            int y,
            int countXOffset,
            int countYOffset,
            VibeVisualsConfig.PvpCardConfig config
    ) {
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(x, y);
        context.getMatrices().scale(config.itemIconSize / 16.0f, config.itemIconSize / 16.0f);
        context.drawItem(stack, 0, 0);
        context.getMatrices().popMatrix();

        drawScaledText(
                context,
                client,
                hudText(Integer.toString(count), true),
                x + config.itemIconSize + config.itemGap + countXOffset,
                y + 1 + countYOffset,
                config.itemColor,
                config.itemTextScale
        );
    }

    private static void drawHealthBar(DrawContext context, int x, int y, int width, int height, float progress) {
        progress = Math.max(0.0f, Math.min(1.0f, progress));
        context.fill(x, y, x + width, y + height, VibeVisualsConfigManager.get().pvpCard.barBackgroundColor);
        context.fill(x, y, x + Math.round(width * progress), y + height, VibeVisualsConfigManager.get().pvpCard.barColor);
    }

    private static void drawScaledText(DrawContext context, MinecraftClient client, Text text, int x, int y, int color, float scale) {
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(x, y);
        context.getMatrices().scale(scale, scale);
        context.drawText(client.textRenderer, text, 0, 0, color, false);
        context.getMatrices().popMatrix();
    }

    private static String formatHealth(float health) {
        return String.format(java.util.Locale.ROOT, "%.1f", health);
    }

    private static int scaledTextHeight(float scale) {
        return Math.round(9.0f * scale);
    }

    private static Text hudText(String text, boolean bold) {
        return Text.literal(text).styled(style -> style.withFont(HUD_FONT).withBold(bold));
    }
}
