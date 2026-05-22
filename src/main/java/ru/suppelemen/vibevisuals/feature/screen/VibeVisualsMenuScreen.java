package ru.suppelemen.vibevisuals.feature.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;
import ru.suppelemen.vibevisuals.core.hud.HudManager;
import ru.suppelemen.vibevisuals.theme.HudCardRenderType;
import ru.suppelemen.vibevisuals.theme.HudVisualSettings;
import ru.suppelemen.vibevisuals.util.render.HudCardRenderer;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class VibeVisualsMenuScreen extends Screen {
    private final HudVisualSettings panelSettings = new HudVisualSettings();
    private final List<FeatureEntry> features = new ArrayList<>();
    private final List<SettingEntry> settingEntries = new ArrayList<>();
    private Category selectedCategory = Category.HUD;
    private FeatureEntry selectedFeature;
    private FeatureEntry hoveredFeature;
    private int sideAnimation;

    public VibeVisualsMenuScreen() {
        super(Text.literal("VibeVisuals"));
    }

    @Override
    protected void init() {
        panelSettings.renderType = HudCardRenderType.LIQUID_GLASS;
        panelSettings.radius = 10.0f;
        panelSettings.opacity = 0.82f;
        panelSettings.glow = false;
        panelSettings.blur = false;
        rebuildFeatures();
        rebuildSettings();
    }

    private void rebuildFeatures() {
        VibeVisualsConfig config = VibeVisualsConfigManager.get();
        features.clear();
        features.add(new FeatureEntry(Category.HUD, "Potions", () -> config.potionsCard.enabled, value -> config.potionsCard.enabled = value, config.potionsCard));
        features.add(new FeatureEntry(Category.HUD, "Cooldowns", () -> config.cooldownsCard.enabled, value -> config.cooldownsCard.enabled = value, config.cooldownsCard));
        features.add(new FeatureEntry(Category.HUD, "Hot Keys", () -> config.hotKeysCard.enabled, value -> config.hotKeysCard.enabled = value, config.hotKeysCard));
        features.add(new FeatureEntry(Category.HUD, "Top Bar", () -> config.topBar.enabled, value -> config.topBar.enabled = value, config.topBar));
        features.add(new FeatureEntry(Category.HUD, "Inventory HUD", () -> config.inventoryHud.enabled, value -> config.inventoryHud.enabled = value, config.inventoryHud));
        features.add(new FeatureEntry(Category.HUD, "Armor HUD", () -> config.armorHud.enabled, value -> config.armorHud.enabled = value, config.armorHud));
        features.add(new FeatureEntry(Category.HUD, "Custom Hotbar", () -> config.hotbar.enabled, value -> config.hotbar.enabled = value, config.hotbar));
        features.add(new FeatureEntry(Category.PVP, "PvP Combat", () -> config.pvpCard.enabled, value -> config.pvpCard.enabled = value, config.pvpCard));
        features.add(new FeatureEntry(Category.VISUALS, "Sky Color", () -> config.visualEffects.skyColorEnabled, value -> config.visualEffects.skyColorEnabled = value, config.visualEffects));
        features.add(new FeatureEntry(Category.VISUALS, "Fog Color", () -> config.visualEffects.fogColorEnabled, value -> config.visualEffects.fogColorEnabled = value, config.visualEffects));
        features.add(new FeatureEntry(Category.VISUALS, "Particles", () -> config.visualEffects.customParticlesEnabled, value -> config.visualEffects.customParticlesEnabled = value, config.visualEffects));
        features.add(new FeatureEntry(Category.VISUALS, "Screen Fire", () -> config.fireOverlay.enabled, value -> config.fireOverlay.enabled = value, config.fireOverlay));
        features.add(new FeatureEntry(Category.UTILITIES, "Projectile Prediction", () -> config.projectilePrediction.enabled, value -> config.projectilePrediction.enabled = value, config.projectilePrediction));
        features.add(new FeatureEntry(Category.UTILITIES, "HUD Animations", () -> config.hudAnimations.enabled, value -> config.hudAnimations.enabled = value, config.hudAnimations));
        features.add(new FeatureEntry(Category.UTILITIES, "FullBright", () -> config.fullBrightStrength > 0.0f, value -> config.fullBrightStrength = value ? Math.max(0.6f, config.fullBrightStrength) : 0.0f, config));
    }

    private void rebuildSettings() {
        settingEntries.clear();
        clearChildren();
        if (selectedFeature == null || selectedFeature.config == null) {
            return;
        }

        int panelX = settingsX();
        int y = 75;
        for (Field field : selectedFeature.config.getClass().getFields()) {
            if (Modifier.isStatic(field.getModifiers()) || !isEditable(field)) {
                continue;
            }

            TextFieldWidget input = new TextFieldWidget(textRenderer, panelX + 82, y, 64, 14, Text.literal(field.getName()));
            input.setDrawsBackground(false);
            input.setEditableColor(0xFFEFEFF6);
            input.setMaxLength(48);
            input.setText(readField(selectedFeature.config, field));
            input.setChangedListener(value -> writeField(selectedFeature.config, field, value));
            addDrawableChild(input);
            settingEntries.add(new SettingEntry(field, input));
            y += 16;
            if (y > height - 34) {
                break;
            }
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderInGameBackground(context);
        sideAnimation = Math.min(12, sideAnimation + 1);

        int mainX = mainX();
        int mainY = 50;
        int mainW = mainWidth();
        int mainH = Math.min(174, height - 100);
        HudCardRenderer.drawCard(context, mainX, mainY, mainW, mainH, panelSettings);
        HudCardRenderer.drawOverlayCard(context, mainX, mainY, mainW, 22, 10.0f, 0xFF050710, 0.30f);
        context.drawTextWithShadow(textRenderer, Text.literal("VibeVisuals"), mainX + 12, mainY + 5, 0xFFEFEFF6);

        renderCategories(context, mouseX, mouseY, mainX + 9, mainY + 28, mainW - 18);
        renderFeatures(context, mouseX, mouseY, mainX + 9, mainY + 47, mainW - 18);
        renderSettingsPanel(context);
        super.render(context, mouseX, mouseY, delta);
    }

    private void renderCategories(DrawContext context, int mouseX, int mouseY, int x, int y, int width) {
        int tabW = Math.max(34, width / Category.values().length - 4);
        for (int i = 0; i < Category.values().length; i++) {
            Category category = Category.values()[i];
            int tabX = x + i * (tabW + 4);
            boolean selected = selectedCategory == category;
            HudCardRenderer.drawOverlayCard(context, tabX, y, tabW, 13, 5.0f, selected ? 0xFF7C5CFF : 0xFF090B12, selected ? 0.52f : 0.34f);
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(category.label), tabX + tabW / 2, y + 2, selected ? 0xFFFFFFFF : 0xFFB7BBC9);
        }
    }

    private void renderFeatures(DrawContext context, int mouseX, int mouseY, int x, int y, int width) {
        hoveredFeature = null;
        int columnW = (width - 6) / 2;
        int index = 0;
        for (FeatureEntry feature : features) {
            if (feature.category != selectedCategory) {
                continue;
            }

            int column = index % 2;
            int row = index / 2;
            int fx = x + column * (columnW + 6);
            int fy = y + row * 19;
            int featureH = 16;
            boolean hovered = mouseX >= fx && mouseX <= fx + columnW && mouseY >= fy && mouseY <= fy + featureH;
            if (hovered) {
                hoveredFeature = feature;
                if (!feature.hoveredLastFrame) {
                    playHoverSound();
                }
            }
            feature.hoveredLastFrame = hovered;
            float targetHover = (hovered || feature == selectedFeature) ? 1.0f : 0.0f;
            feature.hoverProgress += (targetHover - feature.hoverProgress) * 0.25f;
            float activeOpacity = feature.enabled.get() ? 0.54f : 0.28f;
            float opacity = activeOpacity + feature.hoverProgress * 0.16f;
            feature.x = fx;
            feature.y = fy;
            feature.width = columnW;
            feature.height = featureH;
            feature.toggleX = fx + columnW - 24;
            feature.toggleY = fy + 3;
            feature.toggleWidth = 17;
            feature.toggleHeight = 9;
            HudCardRenderer.drawOverlayCard(context, fx, fy, columnW, featureH, 6.0f, feature.enabled.get() ? 0xFF201A42 : 0xFF090B12, opacity);
            if (feature == selectedFeature) {
                HudCardRenderer.drawShaderOutline(context, fx - 3, fy - 3, columnW + 6, featureH + 6, 7.0f, 1.1f, 0.86f);
            }
            context.drawTextWithShadow(textRenderer, Text.literal(feature.name), fx + 7, fy + 4, feature.enabled.get() ? 0xFFFFFFFF : 0xFF9DA2B3);
            drawToggle(context, feature);
            index++;
        }
    }

    private void drawToggle(DrawContext context, FeatureEntry feature) {
        boolean enabled = feature.enabled.get();
        HudCardRenderer.drawOverlayCard(context, feature.toggleX, feature.toggleY, feature.toggleWidth, feature.toggleHeight, feature.toggleHeight / 2.0f, enabled ? 0xFF7C5CFF : 0xFF252936, enabled ? 0.72f : 0.70f);
        int knob = feature.toggleHeight - 3;
        int knobX = enabled ? feature.toggleX + feature.toggleWidth - knob - 2 : feature.toggleX + 2;
        HudCardRenderer.drawOverlayCard(context, knobX, feature.toggleY + 2, knob, knob, knob / 2.0f, enabled ? 0xFFFFFFFF : 0xFF9DA2B3, 0.96f);
    }

    private void renderSettingsPanel(DrawContext context) {
        if (selectedFeature == null) {
            return;
        }

        int x = settingsX() + Math.max(0, 12 - sideAnimation);
        int y = 50;
        int w = 164;
        int h = Math.min(174, height - 100);
        HudCardRenderer.drawCard(context, x, y, w, h, panelSettings);
        HudCardRenderer.drawOverlayCard(context, x, y, w, 26, 10.0f, 0xFF050710, 0.34f);
        context.drawTextWithShadow(textRenderer, Text.literal(selectedFeature.name), x + 10, y + 5, 0xFFEFEFF6);
        context.drawTextWithShadow(textRenderer, Text.literal(selectedFeature.enabled.get() ? "Enabled" : "Disabled"), x + 10, y + 16, selectedFeature.enabled.get() ? 0xFFB7A5FF : 0xFF8C92A4);

        for (SettingEntry entry : settingEntries) {
            TextFieldWidget input = entry.input;
            input.setX(x + 82);
            int rowY = input.getY();
            if (rowY < 72 || rowY > height - 38) {
                input.visible = false;
                continue;
            }
            input.visible = true;
            HudCardRenderer.drawOverlayCard(context, x + 6, rowY - 2, w - 12, 15, 5.0f, 0xFF090B12, 0.45f);
            context.drawTextWithShadow(textRenderer, Text.literal(shortName(entry.field.getName())), x + 11, rowY + 3, 0xFFD7DAE8);
            context.fill(x + 78, rowY - 1, x + 150, rowY + 13, 0x55171B28);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) {
            return true;
        }

        int mainX = mainX();
        int mainY = 50;
        int mainW = mainWidth();
        int tabsX = mainX + 9;
        int tabsY = mainY + 28;
        int tabW = Math.max(34, (mainW - 18) / Category.values().length - 4);
        for (int i = 0; i < Category.values().length; i++) {
            int tabX = tabsX + i * (tabW + 4);
            if (click.x() >= tabX && click.x() <= tabX + tabW && click.y() >= tabsY && click.y() <= tabsY + 20) {
                selectedCategory = Category.values()[i];
                selectedFeature = null;
                rebuildSettings();
                return true;
            }
        }

        if (hoveredFeature != null) {
            selectedFeature = hoveredFeature;
            if (click.x() >= hoveredFeature.toggleX && click.x() <= hoveredFeature.toggleX + hoveredFeature.toggleWidth
                    && click.y() >= hoveredFeature.toggleY && click.y() <= hoveredFeature.toggleY + hoveredFeature.toggleHeight) {
                hoveredFeature.enabledSetter.accept(!hoveredFeature.enabled.get());
                saveAndReload();
            }
            sideAnimation = 0;
            rebuildSettings();
            return true;
        }

        return false;
    }

    @Override
    public void close() {
        saveAndReload();
        if (client != null) {
            client.setScreen(null);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private int settingsX() {
        return mainX() + mainWidth() + 10;
    }

    private int mainWidth() {
        return Math.min(260, width - 36);
    }

    private int mainX() {
        return Math.max(18, width / 2 - mainWidth() / 2);
    }

    private static String shortName(String name) {
        return name.length() <= 10 ? name : name.substring(0, 9) + ".";
    }

    private static boolean isEditable(Field field) {
        Class<?> type = field.getType();
        return type == int.class || type == float.class || type == boolean.class || type == String.class || type == HudCardRenderType.class;
    }

    private static String readField(Object config, Field field) {
        try {
            Object value = field.get(config);
            return value == null ? "" : value.toString();
        } catch (IllegalAccessException exception) {
            return "";
        }
    }

    private static void writeField(Object config, Field field, String value) {
        try {
            Class<?> type = field.getType();
            if (type == int.class) {
                field.setInt(config, Integer.parseInt(value.trim()));
            } else if (type == float.class) {
                field.setFloat(config, Float.parseFloat(value.trim()));
            } else if (type == boolean.class) {
                field.setBoolean(config, Boolean.parseBoolean(value.trim()));
            } else if (type == HudCardRenderType.class) {
                field.set(config, HudCardRenderType.valueOf(value.trim().toUpperCase(Locale.ROOT)));
            } else if (type == String.class) {
                field.set(config, value);
            }
            saveAndReload();
        } catch (IllegalArgumentException | IllegalAccessException ignored) {
        }
    }

    private static void saveAndReload() {
        VibeVisualsConfigManager.get().validate();
        VibeVisualsConfigManager.save();
        HudManager.reload();
    }

    private void playHoverSound() {
        MinecraftClient minecraft = MinecraftClient.getInstance();
        minecraft.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK.value(), 1.05f, 0.045f));
    }

    private enum Category {
        HUD("HUD"),
        VISUALS("Visuals"),
        UTILITIES("Utilities"),
        PVP("PvP"),
        WORLD("World");

        final String label;

        Category(String label) {
            this.label = label;
        }
    }

    private static class FeatureEntry {
        final Category category;
        final String name;
        final Supplier<Boolean> enabled;
        final Consumer<Boolean> enabledSetter;
        final Object config;
        float hoverProgress;
        boolean hoveredLastFrame;
        int x;
        int y;
        int width;
        int height;
        int toggleX;
        int toggleY;
        int toggleWidth;
        int toggleHeight;

        FeatureEntry(Category category, String name, Supplier<Boolean> enabled, Consumer<Boolean> enabledSetter, Object config) {
            this.category = category;
            this.name = name;
            this.enabled = enabled;
            this.enabledSetter = enabledSetter;
            this.config = config;
        }
    }

    private record SettingEntry(Field field, TextFieldWidget input) {
    }
}
