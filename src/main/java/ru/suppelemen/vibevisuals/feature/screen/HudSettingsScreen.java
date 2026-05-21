package ru.suppelemen.vibevisuals.feature.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;
import ru.suppelemen.vibevisuals.core.hud.HudElement;
import ru.suppelemen.vibevisuals.core.hud.HudManager;
import ru.suppelemen.vibevisuals.theme.HudCardRenderType;
import ru.suppelemen.vibevisuals.theme.HudVisualSettings;
import ru.suppelemen.vibevisuals.util.render.HudCardRenderer;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public class HudSettingsScreen extends Screen {
    private static final int PANEL_COLOR = 0xFF11131C;
    private static final int ROW_COLOR = 0xFF090B12;
    private static final float PANEL_OPACITY = 0.80f;
    private static final float ROW_OPACITY = 0.52f;
    private static final float PANEL_RADIUS = 8.0f;
    private static final float ROW_RADIUS = 5.0f;

    private final Screen parent;
    private final HudElement element;
    private final List<FieldEntry> entries = new ArrayList<>();
    private final HudVisualSettings panelSettings = new HudVisualSettings();
    private int scroll;

    public HudSettingsScreen(Screen parent, HudElement element) {
        super(Text.translatable("screen.vibevisuals.hud_settings", element.getDisplayName()));
        this.parent = parent;
        this.element = element;
    }

    @Override
    protected void init() {
        panelSettings.renderType = HudCardRenderType.LIQUID_GLASS;
        panelSettings.radius = PANEL_RADIUS;
        panelSettings.opacity = PANEL_OPACITY;
        panelSettings.glow = false;
        panelSettings.blur = false;

        entries.clear();
        Object config = HudManager.getElementConfig(element);
        if (config == null) {
            return;
        }

        int index = 0;
        for (Field field : config.getClass().getFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            TextFieldWidget input = new TextFieldWidget(textRenderer, width - 150, 40 + index * 24, 92, 18, Text.literal(field.getName()));
            input.setMaxLength(48);
            input.setText(readField(config, field));
            input.setDrawsBackground(false);
            input.setEditableColor(0xFFEFEFF6);
            input.setChangedListener(value -> writeField(config, field, value));
            addDrawableChild(input);

            ButtonWidget help = ButtonWidget.builder(Text.literal("?"), button -> {
            }).dimensions(width - 52, 40 + index * 24, 18, 18)
                    .tooltip(Tooltip.of(description(field.getName(), defaultValueFor(element, field))))
                    .build();
            addDrawableChild(help);

            entries.add(new FieldEntry(field, input, help));
            index++;
        }

        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.vibevisuals.back"), button -> {
            VibeVisualsConfigManager.get().validate();
            VibeVisualsConfigManager.save();
            HudManager.reload();
            if (client != null) {
                client.setScreen(parent);
            }
        }).dimensions(width / 2 - 50, height - 26, 100, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderInGameBackground(context);

        int panelX = Math.max(10, width / 2 - 170);
        int panelY = 8;
        int panelWidth = Math.min(340, width - 20);
        int footerTop = height - 38;
        int panelHeight = Math.max(80, footerTop - panelY - 6);
        HudCardRenderer.drawCard(context, panelX, panelY, panelWidth, panelHeight, panelSettings);
        HudCardRenderer.drawOverlayCard(context, panelX, panelY, panelWidth, panelHeight, PANEL_RADIUS, PANEL_COLOR, 0.36f);

        context.drawCenteredTextWithShadow(textRenderer, Text.literal(element.getDisplayName()), width / 2, 17, 0xFFEFEFF6);
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("screen.vibevisuals.settings_hint"), width / 2, 30, 0xFFB7BBC9);

        for (int i = 0; i < entries.size(); i++) {
            FieldEntry entry = entries.get(i);
            int y = 50 + i * 24 - scroll;
            boolean visible = y > 42 && y + 20 < footerTop;
            entry.input.setX(panelX + panelWidth - 128);
            entry.input.setY(y - 1);
            entry.help.setX(panelX + panelWidth - 27);
            entry.help.setY(y - 1);
            entry.input.visible = visible;
            entry.help.visible = visible;

            if (visible) {
                HudCardRenderer.drawOverlayCard(context, panelX + 8, y - 4, panelWidth - 16, 20, ROW_RADIUS, ROW_COLOR, ROW_OPACITY);
                context.drawTextWithShadow(textRenderer, Text.literal(entry.field.getName()), panelX + 17, y + 4, 0xFFEFEFF6);
                context.fill(panelX + panelWidth - 134, y, panelX + panelWidth - 36, y + 16, 0x55262A3A);
            }
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int listHeight = Math.max(0, height - 90);
        int maxScroll = Math.max(0, entries.size() * 24 - listHeight);
        scroll = Math.max(0, Math.min(maxScroll, scroll - (int) Math.round(verticalAmount * 18.0)));
        return true;
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
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
                field.set(config, HudCardRenderType.valueOf(value.trim()));
            } else if (type == String.class) {
                field.set(config, value);
            } else {
                return;
            }

            VibeVisualsConfigManager.get().validate();
            VibeVisualsConfigManager.save();
            HudManager.reload();
        } catch (IllegalArgumentException | IllegalAccessException exception) {
            // While typing, temporary invalid values are expected. Keep the old config value.
        }
    }

    private static Text description(String name, String defaultValue) {
        return Text.translatable("screen.vibevisuals.setting.tooltip", Text.translatable("screen.vibevisuals.setting." + name), defaultValue);
    }

    private static String defaultValueFor(HudElement element, Field field) {
        Object defaults = HudManager.getElementConfigFrom(new VibeVisualsConfig(), element.getId());
        if (defaults == null) {
            return "";
        }

        try {
            Object value = field.get(defaults);
            return value == null ? "" : value.toString();
        } catch (IllegalAccessException exception) {
            return "";
        }
    }

    private record FieldEntry(Field field, TextFieldWidget input, ButtonWidget help) {
    }
}
