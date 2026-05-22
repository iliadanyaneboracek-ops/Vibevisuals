package ru.suppelemen.vibevisuals.feature.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
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
    private SettingEntry hoveredColorEntry;
    private SettingEntry previewColorEntry;
    private SettingEntry activeColorEntry;
    private SettingEntry selectedSettingEntry;
    private SettingEntry hoveredHelpEntry;
    private int settingScroll;
    private int sideAnimation;
    private int pickerX;
    private int pickerY;
    private int pickerWidth;
    private int pickerHeight;
    private final FooterButton multiBindingButton = new FooterButton("Multi-Binding", true);
    private final FooterButton openConfigButton = new FooterButton("Open Config File", false);
    private FooterButton hoveredFooterButton;

    public VibeVisualsMenuScreen() {
        super(Text.literal("VibeVisuals"));
    }

    @Override
    protected void init() {
        panelSettings.renderType = HudCardRenderType.LIQUID_GLASS;
        panelSettings.radius = menuConfig().radius;
        panelSettings.opacity = menuConfig().opacity;
        panelSettings.glow = false;
        panelSettings.blur = false;
        rebuildFeatures();
        rebuildSettings();
        layoutFooterButtons();
    }

    private void layoutFooterButtons() {
        int buttonH = 20;
        int gap = 6;
        int totalW = Math.min(360, width - 40);
        int buttonW = (totalW - gap) / 2;
        int x = width / 2 - totalW / 2;
        int y = height - buttonH - 8;

        multiBindingButton.x = x;
        multiBindingButton.y = y;
        multiBindingButton.w = buttonW;
        multiBindingButton.h = buttonH;

        openConfigButton.x = x + buttonW + gap;
        openConfigButton.y = y;
        openConfigButton.w = buttonW;
        openConfigButton.h = buttonH;
    }

    private void renderFooterButtons(DrawContext context, int mouseX, int mouseY) {
        hoveredFooterButton = null;
        renderFooterButton(context, mouseX, mouseY, multiBindingButton);
        renderFooterButton(context, mouseX, mouseY, openConfigButton);
    }

    private void renderFooterButton(DrawContext context, int mouseX, int mouseY, FooterButton button) {
        boolean hovered = mouseX >= button.x && mouseX <= button.x + button.w
                && mouseY >= button.y && mouseY <= button.y + button.h;
        if (hovered) {
            hoveredFooterButton = button;
            if (!button.hoveredLastFrame) {
                playHoverSound();
            }
        }
        button.hoveredLastFrame = hovered;
        float targetHover = hovered ? 1.0f : 0.0f;
        button.hoverProgress += (targetHover - button.hoverProgress) * 0.25f;

        int bg = button.accent ? 0xFF7C5CFF : 0xFF090B12;
        float baseOpacity = button.accent ? 0.62f : 0.50f;
        float opacity = baseOpacity + button.hoverProgress * 0.18f;
        HudCardRenderer.drawOverlayCard(context, button.x, button.y, button.w, button.h, 7.0f, bg, opacity);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(button.label),
                button.x + button.w / 2, button.y + (button.h - 8) / 2,
                button.accent ? 0xFFFFFFFF : 0xFFEFEFF6);
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
        features.add(new FeatureEntry(Category.PVP, "Target ESP", () -> config.targetEsp.enabled, value -> config.targetEsp.enabled = value, config.targetEsp));
        features.add(new FeatureEntry(Category.PVP, "Saturation Display", () -> config.saturationDisplay.enabled, value -> config.saturationDisplay.enabled = value, config.saturationDisplay));
        features.add(new FeatureEntry(Category.PVP, "Crit Hit Sound", () -> config.customHitSound.enabled, value -> config.customHitSound.enabled = value, config.customHitSound));
        features.add(new FeatureEntry(Category.VISUALS, "Sky Color", () -> config.visualEffects.skyColorEnabled, value -> config.visualEffects.skyColorEnabled = value, config.visualEffects));
        features.add(new FeatureEntry(Category.VISUALS, "Fog Color", () -> config.visualEffects.fogColorEnabled, value -> config.visualEffects.fogColorEnabled = value, config.visualEffects));
        features.add(new FeatureEntry(Category.VISUALS, "Particles", () -> config.visualEffects.customParticlesEnabled, value -> config.visualEffects.customParticlesEnabled = value, config.visualEffects));
        features.add(new FeatureEntry(Category.VISUALS, "Screen Fire", () -> config.fireOverlay.enabled, value -> config.fireOverlay.enabled = value, config.fireOverlay));
        features.add(new FeatureEntry(Category.VISUALS, "Custom Crosshair", () -> config.customCrosshair.enabled, value -> config.customCrosshair.enabled = value, config.customCrosshair));
        features.add(new FeatureEntry(Category.VISUALS, "Custom Hand", () -> config.customHand.enabled, value -> config.customHand.enabled = value, config.customHand));
        features.add(new FeatureEntry(Category.UTILITIES, "Projectile Prediction", () -> config.projectilePrediction.enabled, value -> config.projectilePrediction.enabled = value, config.projectilePrediction));
        features.add(new FeatureEntry(Category.UTILITIES, "HUD Animations", () -> config.hudAnimations.enabled, value -> config.hudAnimations.enabled = value, config.hudAnimations));
        features.add(new FeatureEntry(Category.UTILITIES, "Markers", () -> config.markers.enabled, value -> config.markers.enabled = value, config.markers));
        features.add(new FeatureEntry(Category.UTILITIES, "AutoEat", () -> config.autoEat.enabled, value -> config.autoEat.enabled = value, config.autoEat));
        features.add(new FeatureEntry(Category.UTILITIES, "AutoPotion", () -> config.autoPotion.enabled, value -> config.autoPotion.enabled = value, config.autoPotion));
        features.add(new FeatureEntry(Category.UTILITIES, "AutoRespawn", () -> config.autoRespawn.enabled, value -> config.autoRespawn.enabled = value, config.autoRespawn));
        features.add(new FeatureEntry(Category.UTILITIES, "Tape Mouse", () -> config.tapeMouse.enabled, value -> config.tapeMouse.enabled = value, config.tapeMouse));
        features.add(new FeatureEntry(Category.UTILITIES, "FullBright", () -> config.fullBrightStrength > 0.0f, value -> config.fullBrightStrength = value ? Math.max(0.6f, config.fullBrightStrength) : 0.0f, config));
        features.add(new FeatureEntry(Category.MENU, "Menu Settings", () -> config.menu.enabled, value -> config.menu.enabled = value, config.menu));
    }

    private void rebuildSettings() {
        for (SettingEntry entry : settingEntries) {
            remove(entry.input);
        }
        settingEntries.clear();
        clearChildren();
        settingScroll = 0;
        selectedSettingEntry = null;
        activeColorEntry = null;
        if (selectedFeature == null || selectedFeature.config == null) {
            return;
        }

        int panelX = settingsX();
        for (Field field : selectedFeature.config.getClass().getFields()) {
            if (Modifier.isStatic(field.getModifiers()) || !isEditable(field)) {
                continue;
            }

            TextFieldWidget input = null;
            if (field.getType() != boolean.class && !isColorField(field)) {
                input = new TextFieldWidget(textRenderer, panelX + 82, 0, 64, 12, Text.literal(field.getName()));
                input.setDrawsBackground(false);
                input.setEditableColor(0xFFEFEFF6);
                input.setMaxLength(48);
                input.setText(readField(selectedFeature.config, field));
                input.setChangedListener(value -> writeField(selectedFeature.config, field, value));
                addDrawableChild(input);
            }
            settingEntries.add(new SettingEntry(field, input));
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderInGameBackground(context);
        sideAnimation = Math.min(12, sideAnimation + 1);
        VibeVisualsConfig.MenuConfig menu = menuConfig();
        panelSettings.radius = menu.radius;
        panelSettings.opacity = menu.opacity;

        int mainX = mainX();
        int mainY = mainY();
        int mainW = mainWidth();
        int mainH = mainHeight();
        HudCardRenderer.drawCard(context, mainX, mainY, mainW, mainH, panelSettings);
        HudCardRenderer.drawOverlayCard(context, mainX, mainY, mainW, 22, menu.radius, menu.backgroundColor, menu.headerOpacity);
        context.drawTextWithShadow(textRenderer, Text.literal("VibeVisuals"), mainX + 12, mainY + 5, menu.titleColor);

        renderCategories(context, mouseX, mouseY, mainX + 9, mainY + 28, mainW - 18);
        renderFeatures(context, mouseX, mouseY, mainX + 9, mainY + 47, mainW - 18);
        renderSettingsPanel(context, mouseX, mouseY);
        renderFooterButtons(context, mouseX, mouseY);
        super.render(context, mouseX, mouseY, delta);
        renderColorPicker(context, mouseX, mouseY);
    }

    private void renderCategories(DrawContext context, int mouseX, int mouseY, int x, int y, int width) {
        VibeVisualsConfig.MenuConfig menu = menuConfig();
        int tabW = Math.max(34, width / Category.values().length - 4);
        for (int i = 0; i < Category.values().length; i++) {
            Category category = Category.values()[i];
            int tabX = x + i * (tabW + 4);
            boolean selected = selectedCategory == category;
            HudCardRenderer.drawOverlayCard(context, tabX, y, tabW, menu.tabHeight, 5.0f, selected ? menu.accentColor : menu.cardColor, selected ? 0.52f : menu.cardOpacity);
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(category.label), tabX + tabW / 2, y + 2, selected ? menu.outlineColor : menu.textColor);
        }
    }

    private void renderFeatures(DrawContext context, int mouseX, int mouseY, int x, int y, int width) {
        VibeVisualsConfig.MenuConfig menu = menuConfig();
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
            int fy = y + row * (menu.featureHeight + menu.featureGap / 2);
            int featureH = menu.featureHeight;
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
            float activeOpacity = feature.enabled.get() ? menu.activeOpacity : menu.cardOpacity;
            float opacity = activeOpacity + feature.hoverProgress * 0.16f;
            feature.x = fx;
            feature.y = fy;
            feature.width = columnW;
            feature.height = featureH;
            feature.toggleX = fx + columnW - 24;
            feature.toggleY = fy + 3;
            feature.toggleWidth = 17;
            feature.toggleHeight = 9;
            HudCardRenderer.drawOverlayCard(context, fx, fy, columnW, featureH, 6.0f, feature.enabled.get() ? menu.activeColor : menu.cardColor, opacity);
            if (feature == selectedFeature) {
                HudCardRenderer.drawShaderOutline(context, fx - 3, fy - 3, columnW + 6, featureH + 6, 7.0f, 1.1f, 0.86f);
            }
            context.drawTextWithShadow(textRenderer, Text.literal(feature.name), fx + 7, fy + 4, feature.enabled.get() ? menu.outlineColor : menu.mutedTextColor);
            drawToggle(context, feature);
            index++;
        }
    }

    private void drawToggle(DrawContext context, FeatureEntry feature) {
        boolean enabled = feature.enabled.get();
        VibeVisualsConfig.MenuConfig menu = menuConfig();
        HudCardRenderer.drawOverlayCard(context, feature.toggleX, feature.toggleY, feature.toggleWidth, feature.toggleHeight, feature.toggleHeight / 2.0f, enabled ? menu.accentColor : 0xFF252936, enabled ? 0.72f : 0.70f);
        int knob = feature.toggleHeight - 3;
        int knobX = enabled ? feature.toggleX + feature.toggleWidth - knob - 2 : feature.toggleX + 2;
        HudCardRenderer.drawOverlayCard(context, knobX, feature.toggleY + 2, knob, knob, knob / 2.0f, enabled ? menu.outlineColor : menu.mutedTextColor, 0.96f);
    }

    private void renderSettingsPanel(DrawContext context, int mouseX, int mouseY) {
        if (selectedFeature == null) {
            return;
        }

        VibeVisualsConfig.MenuConfig menu = menuConfig();
        hoveredColorEntry = null;
        hoveredHelpEntry = null;
        int x = settingsX() + menu.sideXOffset + Math.max(0, 12 - sideAnimation);
        int y = mainY() + menu.sideYOffset;
        int w = menu.sideWidth;
        int h = mainHeight();
        int contentTop = y + 29;
        int contentBottom = y + h - 8;
        HudCardRenderer.drawCard(context, x, y, w, h, panelSettings);
        HudCardRenderer.drawOverlayCard(context, x, y, w, 26, menu.radius, menu.backgroundColor, menu.headerOpacity);
        context.drawTextWithShadow(textRenderer, featureText(selectedFeature.name), x + 10, y + 5, menu.titleColor);
        context.drawTextWithShadow(textRenderer, Text.literal(selectedFeature.enabled.get() ? "Enabled" : "Disabled"), x + 10, y + 16, selectedFeature.enabled.get() ? menu.accentColor : menu.mutedTextColor);

        for (int index = 0; index < settingEntries.size(); index++) {
            SettingEntry entry = settingEntries.get(index);
            TextFieldWidget input = entry.input;
            int rowY = contentTop + index * menu.rowHeight - settingScroll;
            entry.y = rowY;
            boolean visible = rowY + menu.rowHeight > contentTop && rowY < contentBottom;
            if (input != null) {
                input.setX(x + w - 78);
                input.setY(rowY + 1);
                input.setWidth(58);
                input.visible = visible;
            }
            if (!visible) {
                continue;
            }

            boolean color = isColorField(entry.field);
            boolean hovered = mouseX >= x + 6 && mouseX <= x + w - 6 && mouseY >= rowY - 2 && mouseY <= rowY + 13;
            if (color && hovered) {
                hoveredColorEntry = entry;
            }
            HudCardRenderer.drawOverlayCard(context, x + 6, rowY - 1, w - 12, menu.rowHeight - 1, 5.0f, menu.cardColor, 0.45f);
            if (entry == selectedSettingEntry) {
                HudCardRenderer.drawShaderOutline(context, x + 4, rowY - 3, w - 8, menu.rowHeight + 3, 6.0f, 0.8f, 0.78f);
            }
            drawScaledText(context, settingNameText(entry.field.getName()), x + 11, rowY + 3, menu.textColor, menu.settingTextScale);

            int helpX = x + w - 16;
            int helpY = rowY + 2;
            boolean helpHovered = mouseX >= helpX && mouseX <= helpX + 8 && mouseY >= helpY && mouseY <= helpY + 8;
            if (helpHovered) {
                hoveredHelpEntry = entry;
            }
            HudCardRenderer.drawOverlayCard(context, helpX, helpY, 8, 8, 4.0f, helpHovered ? menu.accentColor : menu.cardColor, helpHovered ? 0.72f : 0.64f);
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("?"), helpX + 4, helpY, menu.outlineColor);

            if (entry.field.getType() == boolean.class) {
                drawSettingToggle(context, x + w - 64, rowY + 2, 24, 10, readBoolean(selectedFeature.config, entry.field));
            } else if (color) {
                int swatch = readColor(selectedFeature.config, entry.field);
                int swatchX = x + w - 42;
                context.fill(swatchX, rowY + 1, swatchX + 28, rowY + 12, 0xFF000000);
                context.fill(swatchX + 1, rowY + 2, swatchX + 27, rowY + 11, 0xFF000000 | (swatch & 0x00FFFFFF));
            } else {
                context.fill(x + w - 82, rowY, x + w - 18, rowY + 12, 0x55171B28);
            }
        }

        renderHelpTooltip(context, mouseX, mouseY);
    }

    private void renderColorPicker(DrawContext context, int mouseX, int mouseY) {
        SettingEntry entry = activeColorEntry != null ? activeColorEntry : selectedSettingEntry;
        if (selectedFeature == null || entry == null || !isColorField(entry.field)) {
            return;
        }

        VibeVisualsConfig.MenuConfig menu = menuConfig();
        int pickerSize = menu.colorPickerSize;
        int settingsRight = settingsX() + menu.sideXOffset + menu.sideWidth;
        int x = settingsRight + pickerSize + 18 < width ? settingsRight + 8 : settingsX() + menu.sideXOffset + 6;
        int y = settingsRight + pickerSize + 18 < width ? Math.max(12, Math.min(height - pickerSize - 34, entry.y - 4)) : mainY() + menu.sideYOffset + mainHeight() + 8;
        if (y + pickerSize + 24 > height) {
            y = Math.max(12, height - pickerSize - 28);
        }
        pickerX = x;
        pickerY = y;
        pickerWidth = pickerSize;
        pickerHeight = 34;
        HudCardRenderer.drawCard(context, x - 5, y - 5, pickerSize + 10, pickerHeight + 24, panelSettings);
        context.drawTextWithShadow(textRenderer, Text.literal("Color"), x, y + pickerHeight + 6, menu.titleColor);
        int selected = readColor(selectedFeature.config, entry.field);
        context.fill(x + 42, y + pickerHeight + 6, x + 58, y + pickerHeight + 16, 0xFF000000);
        context.fill(x + 43, y + pickerHeight + 7, x + 57, y + pickerHeight + 15, 0xFF000000 | (selected & 0x00FFFFFF));

        for (int px = 0; px < pickerSize; px += 2) {
            float hue = px / (float) Math.max(1, pickerSize - 1);
            context.fill(x + px, y, x + Math.min(pickerSize, px + 2), y + 18, 0xFF000000 | hsvToRgb(hue, 0.92f, 1.0f));
        }
        int hueColor = 0xFF000000 | (selected & 0x00FFFFFF);
        for (int px = 0; px < pickerSize; px += 2) {
            float value = px / (float) Math.max(1, pickerSize - 1);
            context.fill(x + px, y + 22, x + Math.min(pickerSize, px + 2), y + 34, mixColor(0x000000, hueColor & 0x00FFFFFF, value) | 0xFF000000);
        }
        HudCardRenderer.drawShaderOutline(context, x - 1, y - 1, pickerSize + 2, pickerHeight + 2, 4.0f, 0.8f, 0.70f);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) {
            return true;
        }

        if (hoveredFooterButton != null) {
            playHoverSound();
            if (hoveredFooterButton == multiBindingButton) {
                if (client != null) {
                    client.setScreen(new MultiKeyBindingsScreen(this));
                }
            } else if (hoveredFooterButton == openConfigButton) {
                Util.getOperatingSystem().open(VibeVisualsConfigManager.getConfigPath().toFile());
            }
            return true;
        }

        int mainX = mainX();
        int mainY = mainY();
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

        if (activeColorEntry != null || selectedSettingEntry != null) {
            SettingEntry entry = activeColorEntry != null ? activeColorEntry : selectedSettingEntry;
            if (tryPickColor(entry, click.x(), click.y())) {
                activeColorEntry = entry;
                return true;
            }
        }

        if (hoveredFeature != null) {
            selectedFeature = hoveredFeature;
            if (click.x() >= hoveredFeature.toggleX && click.x() <= hoveredFeature.toggleX + hoveredFeature.toggleWidth
                    && click.y() >= hoveredFeature.toggleY && click.y() <= hoveredFeature.toggleY + hoveredFeature.toggleHeight) {
                hoveredFeature.enabledSetter.accept(!hoveredFeature.enabled.get());
                playToggleSound();
                saveAndReload();
            }
            sideAnimation = 0;
            rebuildSettings();
            return true;
        }

        SettingEntry clickedSetting = settingAt(click.x(), click.y());
        if (clickedSetting != null) {
            selectedSettingEntry = clickedSetting;
            if (clickedSetting.field.getType() == boolean.class) {
                writeBoolean(selectedFeature.config, clickedSetting.field, !readBoolean(selectedFeature.config, clickedSetting.field));
                playToggleSound();
                saveAndReload();
            }
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (selectedFeature == null) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        VibeVisualsConfig.MenuConfig menu = menuConfig();
        int x = settingsX() + menu.sideXOffset;
        int y = mainY() + menu.sideYOffset;
        int w = menu.sideWidth;
        int h = mainHeight();
        if (mouseX < x || mouseX > x + w || mouseY < y || mouseY > y + h) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        int listHeight = Math.max(0, h - 37);
        int maxScroll = Math.max(0, settingEntries.size() * menu.rowHeight - listHeight);
        settingScroll = Math.max(0, Math.min(maxScroll, settingScroll - (int) Math.round(verticalAmount * 18.0)));
        return true;
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (activeColorEntry != null && tryPickColor(activeColorEntry, click.x(), click.y())) {
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        activeColorEntry = null;
        return super.mouseReleased(click);
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
        VibeVisualsConfig.MenuConfig menu = menuConfig();
        return width - menu.sideWidth - 12;
    }

    private int mainWidth() {
        return Math.min(menuConfig().width, width - 36);
    }

    private int mainX() {
        return Math.max(18, width / 2 - mainWidth() / 2 + menuConfig().xOffset);
    }

    private int mainY() {
        return Math.max(12, 50 + menuConfig().yOffset);
    }

    private int mainHeight() {
        return Math.min(menuConfig().height, height - 100);
    }

    private static String shortName(String name) {
        return name.length() <= 10 ? name : name.substring(0, 9) + ".";
    }

    private static boolean isEditable(Field field) {
        Class<?> type = field.getType();
        return type == int.class || type == float.class || type == boolean.class || type == String.class || type == HudCardRenderType.class;
    }

    private static boolean isColorField(Field field) {
        return field.getType() == int.class && field.getName().toLowerCase(Locale.ROOT).contains("color");
    }

    private static int readColor(Object config, Field field) {
        try {
            return field.getInt(config);
        } catch (IllegalAccessException exception) {
            return 0xFFFFFFFF;
        }
    }

    private static boolean readBoolean(Object config, Field field) {
        try {
            return field.getBoolean(config);
        } catch (IllegalAccessException exception) {
            return false;
        }
    }

    private static void writeBoolean(Object config, Field field, boolean value) {
        try {
            field.setBoolean(config, value);
        } catch (IllegalAccessException ignored) {
        }
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

    private SettingEntry settingAt(double mouseX, double mouseY) {
        if (selectedFeature == null) {
            return null;
        }
        VibeVisualsConfig.MenuConfig menu = menuConfig();
        int x = settingsX() + menu.sideXOffset;
        int y = mainY() + menu.sideYOffset;
        int contentTop = y + 29;
        int contentBottom = y + mainHeight() - 8;
        if (mouseX < x + 6 || mouseX > x + menu.sideWidth - 6 || mouseY < contentTop || mouseY > contentBottom) {
            return null;
        }

        for (SettingEntry entry : settingEntries) {
            if (mouseY >= entry.y - 1 && mouseY <= entry.y + menu.rowHeight - 1) {
                return entry;
            }
        }
        return null;
    }

    private void drawSettingToggle(DrawContext context, int x, int y, int width, int height, boolean enabled) {
        VibeVisualsConfig.MenuConfig menu = menuConfig();
        HudCardRenderer.drawOverlayCard(context, x, y, width, height, height / 2.0f, enabled ? menu.accentColor : 0xFF252936, enabled ? 0.74f : 0.70f);
        int knob = height - 3;
        int knobX = enabled ? x + width - knob - 2 : x + 2;
        HudCardRenderer.drawOverlayCard(context, knobX, y + 2, knob, knob, knob / 2.0f, enabled ? menu.outlineColor : menu.mutedTextColor, 0.96f);
    }

    private void renderHelpTooltip(DrawContext context, int mouseX, int mouseY) {
        if (hoveredHelpEntry == null || selectedFeature == null) {
            return;
        }
        VibeVisualsConfig.MenuConfig menu = menuConfig();
        String name = hoveredHelpEntry.field.getName();
        Text title = settingNameText(name);
        Text description = Text.translatableWithFallback(settingDescriptionKey(name), humanize(name));
        Text defaultValue = Text.translatableWithFallback("screen.vibevisuals.setting.default", "Default: %s", defaultValueFor(selectedFeature.config, hoveredHelpEntry.field));
        int boxW = Math.min(190, Math.max(textRenderer.getWidth(title), Math.max(textRenderer.getWidth(description), textRenderer.getWidth(defaultValue))) + 14);
        int boxH = 38;
        int x = Math.min(width - boxW - 8, mouseX + 10);
        int y = Math.min(height - boxH - 8, mouseY + 10);
        HudCardRenderer.drawCard(context, x, y, boxW, boxH, panelSettings);
        HudCardRenderer.drawOverlayCard(context, x, y, boxW, boxH, 6.0f, menu.backgroundColor, 0.50f);
        context.drawTextWithShadow(textRenderer, title, x + 7, y + 5, menu.titleColor);
        context.drawTextWithShadow(textRenderer, description, x + 7, y + 16, menu.textColor);
        context.drawTextWithShadow(textRenderer, defaultValue, x + 7, y + 27, menu.mutedTextColor);
    }

    private static String defaultValueFor(Object config, Field field) {
        Object defaults = defaultSectionFor(config);
        if (defaults == null) {
            return "";
        }
        try {
            Object value = defaults.getClass().getField(field.getName()).get(defaults);
            return value == null ? "" : value.toString();
        } catch (NoSuchFieldException | IllegalAccessException exception) {
            return "";
        }
    }

    private static Object defaultSectionFor(Object config) {
        VibeVisualsConfig defaults = new VibeVisualsConfig();
        Class<?> type = config.getClass();
        for (Field field : defaults.getClass().getFields()) {
            try {
                Object value = field.get(defaults);
                if (value != null && value.getClass() == type) {
                    return value;
                }
            } catch (IllegalAccessException ignored) {
            }
        }
        return type == VibeVisualsConfig.class ? defaults : null;
    }

    private void drawScaledText(DrawContext context, Text text, int x, int y, int color, float scale) {
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(x, y);
        context.getMatrices().scale(scale, scale);
        context.drawTextWithShadow(textRenderer, text, 0, 0, color);
        context.getMatrices().popMatrix();
    }

    private static Text featureText(String name) {
        return Text.translatableWithFallback("screen.vibevisuals.feature." + name.toLowerCase(Locale.ROOT).replace(" ", "_"), name);
    }

    private static Text settingNameText(String name) {
        return Text.translatableWithFallback("screen.vibevisuals.setting_name." + name, humanize(name));
    }

    private static String settingDescriptionKey(String name) {
        return "screen.vibevisuals.setting." + name;
    }

    private static String humanize(String name) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (i > 0 && Character.isUpperCase(c)) {
                builder.append(' ');
            }
            builder.append(i == 0 ? Character.toUpperCase(c) : c);
        }
        return builder.toString();
    }

    private boolean tryPickColor(SettingEntry entry, double mouseX, double mouseY) {
        if (selectedFeature == null || entry == null || !isColorField(entry.field)) {
            return false;
        }
        if (pickerWidth <= 0 || pickerHeight <= 0 || mouseX < pickerX || mouseX > pickerX + pickerWidth || mouseY < pickerY || mouseY > pickerY + pickerHeight) {
            return false;
        }
        int previous = readColor(selectedFeature.config, entry.field);
        int rgb;
        if (mouseY <= pickerY + 18) {
            float hue = (float) ((mouseX - pickerX) / Math.max(1.0, pickerWidth - 1.0));
            rgb = hsvToRgb(clamp01(hue), 0.92f, 1.0f);
        } else if (mouseY >= pickerY + 22) {
            float value = (float) ((mouseX - pickerX) / Math.max(1.0, pickerWidth - 1.0));
            rgb = mixColor(0x000000, previous & 0x00FFFFFF, clamp01(value));
        } else {
            return false;
        }
        int argb = (previous & 0xFF000000) | rgb;
        if ((argb & 0xFF000000) == 0) {
            argb |= 0xFF000000;
        }
        try {
            entry.field.setInt(selectedFeature.config, argb);
            saveAndReload();
            return true;
        } catch (IllegalAccessException exception) {
            return false;
        }
    }

    private static float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private static int hsvToRgb(float hue, float saturation, float value) {
        float h = (hue - (float) Math.floor(hue)) * 6.0f;
        int sector = (int) Math.floor(h);
        float f = h - sector;
        float p = value * (1.0f - saturation);
        float q = value * (1.0f - f * saturation);
        float t = value * (1.0f - (1.0f - f) * saturation);
        float r;
        float g;
        float b;
        switch (sector % 6) {
            case 0 -> {
                r = value;
                g = t;
                b = p;
            }
            case 1 -> {
                r = q;
                g = value;
                b = p;
            }
            case 2 -> {
                r = p;
                g = value;
                b = t;
            }
            case 3 -> {
                r = p;
                g = q;
                b = value;
            }
            case 4 -> {
                r = t;
                g = p;
                b = value;
            }
            default -> {
                r = value;
                g = p;
                b = q;
            }
        }
        return ((int) (r * 255.0f) << 16) | ((int) (g * 255.0f) << 8) | (int) (b * 255.0f);
    }

    private static int mixColor(int from, int to, float amount) {
        int red = (int) (((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * amount);
        int green = (int) (((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * amount);
        int blue = (int) ((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * amount);
        return (red << 16) | (green << 8) | blue;
    }

    private static VibeVisualsConfig.MenuConfig menuConfig() {
        return VibeVisualsConfigManager.get().menu;
    }

    private void playHoverSound() {
        MinecraftClient minecraft = MinecraftClient.getInstance();
        minecraft.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK.value(), 1.05f, 0.045f));
    }

    private void playToggleSound() {
        MinecraftClient minecraft = MinecraftClient.getInstance();
        minecraft.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK.value(), 1.2f, 0.18f));
    }

    private enum Category {
        HUD("HUD"),
        VISUALS("Visuals"),
        UTILITIES("Utilities"),
        PVP("PvP"),
        WORLD("World"),
        MENU("Menu");

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

    private static class SettingEntry {
        final Field field;
        final TextFieldWidget input;
        int y;

        SettingEntry(Field field, TextFieldWidget input) {
            this.field = field;
            this.input = input;
        }
    }

    private static class FooterButton {
        final String label;
        final boolean accent;
        int x;
        int y;
        int w;
        int h;
        float hoverProgress;
        boolean hoveredLastFrame;

        FooterButton(String label, boolean accent) {
            this.label = label;
            this.accent = accent;
        }
    }
}
