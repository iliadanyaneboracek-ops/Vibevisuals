package ru.suppelemen.vibevisuals.feature.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;
import ru.suppelemen.vibevisuals.core.hud.HudManager;
import ru.suppelemen.vibevisuals.feature.keybind.MultiKeyBinding;
import ru.suppelemen.vibevisuals.theme.HudCardRenderType;
import ru.suppelemen.vibevisuals.theme.HudVisualSettings;
import ru.suppelemen.vibevisuals.theme.MenuTheme;
import ru.suppelemen.vibevisuals.util.render.HudCardRenderer;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class VibeVisualsMenuScreen extends Screen {

    private static final int CARD_HEIGHT = 26;
    private static final int CARD_GAP = 8;
    private static final int HEADER_HEIGHT = 56;
    private static final int DOCK_HEIGHT = 36;
    private static final int SCROLLBAR_WIDTH = 3;

    private final HudVisualSettings windowSettings = new HudVisualSettings();
    private final HudVisualSettings sidePanelSettings = new HudVisualSettings();
    private final List<FeatureEntry> features = new ArrayList<>();
    private final List<SettingEntry> settingEntries = new ArrayList<>();

    private TextFieldWidget searchField;

    private Category selectedCategory = Category.VISUALS;
    private String searchQuery = "";

    private int scroll;
    private int maxScroll;
    private int gridContentHeight;

    private FeatureEntry hoveredFeature;
    private FeatureEntry settingsTarget;
    private Side settingsSide = Side.RIGHT;
    private int sidePanelHeight;
    private int settingsScroll;
    private float sideSlideProgress;

    private SettingEntry hoveredSettingEntry;
    private SettingEntry activeColorEntry;
    private int pickerX;
    private int pickerY;
    private int pickerWidth;
    private int pickerHeight;

    private int hoveredDockIndex = -1;
    private int hoveredCategoryIndex = -1;

    private long openedAtMs;
    private float openProgress;

    public VibeVisualsMenuScreen() {
        super(Text.literal("VibeVisuals"));
        windowSettings.renderType = HudCardRenderType.LIQUID_GLASS;
        windowSettings.radius = MenuTheme.RADIUS_WINDOW;
        windowSettings.opacity = 0.94f;
        sidePanelSettings.renderType = HudCardRenderType.LIQUID_GLASS;
        sidePanelSettings.radius = MenuTheme.RADIUS_WINDOW;
        sidePanelSettings.opacity = 0.94f;
    }

    @Override
    protected void init() {
        rebuildFeatures();
        ensureSelectedCategoryHasEntries();
        openedAtMs = System.currentTimeMillis();

        int windowW = windowWidth();
        int windowX = windowX();
        int windowY = windowY();

        searchField = new TextFieldWidget(textRenderer, windowX + windowW - 144, windowY + 30, 124, 14, Text.literal("search"));
        searchField.setDrawsBackground(false);
        searchField.setEditableColor(MenuTheme.TEXT_PRIMARY);
        searchField.setUneditableColor(MenuTheme.TEXT_MUTED);
        searchField.setMaxLength(48);
        searchField.setPlaceholder(Text.literal("Поиск"));
        searchField.setChangedListener(value -> {
            searchQuery = value == null ? "" : value;
            scroll = 0;
        });
        addDrawableChild(searchField);
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
        features.add(new FeatureEntry(Category.HUD, "Slot Timers", () -> config.slotTimers.enabled, value -> config.slotTimers.enabled = value, config.slotTimers));
        features.add(new FeatureEntry(Category.PVP, "PvP Combat", () -> config.pvpCard.enabled, value -> config.pvpCard.enabled = value, config.pvpCard));
        features.add(new FeatureEntry(Category.PVP, "Target ESP", () -> config.targetEsp.enabled, value -> config.targetEsp.enabled = value, config.targetEsp));
        features.add(new FeatureEntry(Category.PVP, "Saturation", () -> config.saturationDisplay.enabled, value -> config.saturationDisplay.enabled = value, config.saturationDisplay));
        features.add(new FeatureEntry(Category.PVP, "Crit Hit Sound", () -> config.customHitSound.enabled, value -> config.customHitSound.enabled = value, config.customHitSound));
        features.add(new FeatureEntry(Category.PVP, "Shift Up", () -> config.shiftUp.enabled, value -> config.shiftUp.enabled = value, config.shiftUp));
        features.add(new FeatureEntry(Category.PVP, "Healing Helper", () -> config.healingHelper.enabled, value -> config.healingHelper.enabled = value, config.healingHelper));
        features.add(new FeatureEntry(Category.VISUALS, "Sky Color", () -> config.visualEffects.skyColorEnabled, value -> config.visualEffects.skyColorEnabled = value, config.visualEffects));
        features.add(new FeatureEntry(Category.VISUALS, "Fog Color", () -> config.visualEffects.fogColorEnabled, value -> config.visualEffects.fogColorEnabled = value, config.visualEffects));
        features.add(new FeatureEntry(Category.VISUALS, "Particles", () -> config.visualEffects.customParticlesEnabled, value -> config.visualEffects.customParticlesEnabled = value, config.visualEffects));
        features.add(new FeatureEntry(Category.VISUALS, "Screen Fire", () -> config.fireOverlay.enabled, value -> config.fireOverlay.enabled = value, config.fireOverlay));
        features.add(new FeatureEntry(Category.VISUALS, "Crosshair", () -> config.customCrosshair.enabled, value -> config.customCrosshair.enabled = value, config.customCrosshair));
        features.add(new FeatureEntry(Category.VISUALS, "Custom Hand", () -> config.customHand.enabled, value -> config.customHand.enabled = value, config.customHand));
        features.add(new FeatureEntry(Category.UTILITIES, "Projectile Path", () -> config.projectilePrediction.enabled, value -> config.projectilePrediction.enabled = value, config.projectilePrediction));
        features.add(new FeatureEntry(Category.UTILITIES, "HUD Animations", () -> config.hudAnimations.enabled, value -> config.hudAnimations.enabled = value, config.hudAnimations));
        features.add(new FeatureEntry(Category.UTILITIES, "Markers", () -> config.markers.enabled, value -> config.markers.enabled = value, config.markers));
        features.add(new FeatureEntry(Category.UTILITIES, "AutoEat", () -> config.autoEat.enabled, value -> config.autoEat.enabled = value, config.autoEat));
        features.add(new FeatureEntry(Category.UTILITIES, "AutoPotion", () -> config.autoPotion.enabled, value -> config.autoPotion.enabled = value, config.autoPotion));
        features.add(new FeatureEntry(Category.UTILITIES, "AutoRespawn", () -> config.autoRespawn.enabled, value -> config.autoRespawn.enabled = value, config.autoRespawn));
        features.add(new FeatureEntry(Category.UTILITIES, "Tape Mouse", () -> config.tapeMouse.enabled, value -> config.tapeMouse.enabled = value, config.tapeMouse));
        features.add(new FeatureEntry(Category.UTILITIES, "Pickup Logger", () -> config.itemPickupLogger.enabled, value -> config.itemPickupLogger.enabled = value, config.itemPickupLogger));
        features.add(new FeatureEntry(Category.UTILITIES, "FullBright", () -> config.fullBrightStrength > 0.0f, value -> config.fullBrightStrength = value ? Math.max(0.6f, config.fullBrightStrength) : 0.0f, config));
        features.add(new FeatureEntry(Category.MENU, "Menu Settings", () -> config.menu.enabled, value -> config.menu.enabled = value, config.menu));
    }

    private void ensureSelectedCategoryHasEntries() {
        for (Category category : Category.values()) {
            if (hasAnyFeature(category)) {
                if (!hasAnyFeature(selectedCategory)) {
                    selectedCategory = category;
                }
                break;
            }
        }
    }

    private boolean hasAnyFeature(Category category) {
        for (FeatureEntry feature : features) {
            if (feature.category == category) {
                return true;
            }
        }
        return false;
    }

    private List<Category> visibleCategories() {
        List<Category> list = new ArrayList<>();
        for (Category category : Category.values()) {
            if (hasAnyFeature(category)) {
                list.add(category);
            }
        }
        return list;
    }

    private List<FeatureEntry> visibleFeatures() {
        List<FeatureEntry> list = new ArrayList<>();
        String query = searchQuery.trim().toLowerCase(Locale.ROOT);
        for (FeatureEntry feature : features) {
            if (!query.isEmpty()) {
                if (feature.name.toLowerCase(Locale.ROOT).contains(query)) {
                    list.add(feature);
                }
            } else if (feature.category == selectedCategory) {
                list.add(feature);
            }
        }
        return list;
    }

    private int windowWidth() {
        return Math.min(MenuTheme.WINDOW_WIDTH, width - 48);
    }

    private int windowHeight() {
        return Math.min(MenuTheme.WINDOW_HEIGHT, height - 70);
    }

    private int windowX() {
        return width / 2 - windowWidth() / 2;
    }

    private int windowY() {
        return height / 2 - windowHeight() / 2 + 6;
    }

    private int gridX() {
        return windowX() + MenuTheme.PADDING_WINDOW;
    }

    private int gridY() {
        return windowY() + HEADER_HEIGHT;
    }

    private int gridWidth() {
        return windowWidth() - MenuTheme.PADDING_WINDOW * 2;
    }

    private int gridHeight() {
        return windowHeight() - HEADER_HEIGHT - DOCK_HEIGHT;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        long now = System.currentTimeMillis();
        openProgress = Math.min(1.0f, Math.max(0.0f, (now - openedAtMs) / MenuTheme.OPEN_ANIM_DURATION_MS));
        float eased = easeOutCubic(openProgress);

        renderDim(context, eased);
        renderStatusPill(context, eased);
        renderHotKeysPanel(context, mouseX, mouseY, eased);
        renderMainWindow(context, mouseX, mouseY, eased);
        renderSidePanel(context, mouseX, mouseY, eased);
        renderColorPicker(context, mouseX, mouseY, eased);
        super.render(context, mouseX, mouseY, delta);
        renderSettingTooltip(context, mouseX, mouseY);
    }

    private void renderDim(DrawContext context, float eased) {
        int alpha = (int) (0xCC * eased) & 0xFF;
        context.fill(0, 0, width, height, (alpha << 24) | 0x05060A);
    }

    private void renderStatusPill(DrawContext context, float eased) {
        if (eased < 0.05f) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        int fps = client.getCurrentFps();
        int ping = currentPing(client);

        String text = "vibevisuals.pro  ·  " + fps + " fps  ·  " + ping + " ms";
        int textW = textRenderer.getWidth(text);
        int pillW = textW + 28;
        int pillH = 16;
        int px = width / 2 - pillW / 2;
        int py = Math.max(8, windowY() - pillH - 14);

        HudCardRenderer.drawOverlayCard(context, px, py, pillW, pillH, MenuTheme.RADIUS_PILL, MenuTheme.BG_PANEL_SOFT, 0.78f * eased);
        HudCardRenderer.drawShaderOutline(context, px, py, pillW, pillH, MenuTheme.RADIUS_PILL, 0.6f, 0.30f * eased);

        // small purple dot
        int dotX = px + 8;
        int dotY = py + pillH / 2;
        context.fill(dotX, dotY - 1, dotX + 3, dotY + 2, MenuTheme.withAlpha(MenuTheme.ACCENT, 0.95f * eased));

        int textColor = MenuTheme.withAlpha(MenuTheme.TEXT_SECONDARY | 0xFF000000, 0.85f * eased);
        context.drawText(textRenderer, Text.literal(text), px + 16, py + 4, textColor, false);
    }

    private int currentPing(MinecraftClient client) {
        try {
            if (client.player == null || client.getNetworkHandler() == null) {
                return 0;
            }
            PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
            return entry == null ? 0 : entry.getLatency();
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private void renderHotKeysPanel(DrawContext context, int mouseX, int mouseY, float eased) {
        if (eased < 0.05f) {
            return;
        }
        List<MultiKeyBinding> bindings = activeBindings();
        int panelW = 138;
        int rowH = 11;
        int panelH = 22 + Math.min(4, bindings.size()) * rowH + 6;
        int px = width - panelW - 14;
        int py = 14;

        HudCardRenderer.drawCard(context, px, py, panelW, panelH, sidePanelSettings);
        HudCardRenderer.drawOverlayCard(context, px, py, panelW, panelH, MenuTheme.RADIUS_CARD, MenuTheme.BG_PANEL, 0.55f * eased);
        HudCardRenderer.drawShaderOutline(context, px, py, panelW, panelH, MenuTheme.RADIUS_CARD, 0.5f, 0.22f * eased);

        // header
        drawKeyboardGlyph(context, px + 9, py + 7, MenuTheme.withAlpha(MenuTheme.ACCENT, eased));
        context.drawText(textRenderer, Text.literal("Hot Keys"), px + 24, py + 7, MenuTheme.withAlpha(MenuTheme.TEXT_PRIMARY, eased), false);

        int rowY = py + 22;
        int textColor = MenuTheme.withAlpha(MenuTheme.TEXT_SECONDARY | 0xFF000000, 0.92f * eased);
        int keyColor = MenuTheme.withAlpha(MenuTheme.TEXT_PRIMARY, eased);

        if (bindings.isEmpty()) {
            context.drawText(textRenderer, Text.literal("No bindings"), px + 10, rowY, MenuTheme.withAlpha(MenuTheme.TEXT_MUTED | 0xFF000000, 0.85f * eased), false);
            return;
        }

        for (int i = 0; i < Math.min(4, bindings.size()); i++) {
            MultiKeyBinding binding = bindings.get(i);
            String label = binding.displayName == null || binding.displayName.isBlank() ? binding.id : binding.displayName;
            String key = binding.primary == null || !binding.primary.isAssigned() ? "?" : binding.primary.describe();
            int trimW = panelW - 36 - textRenderer.getWidth(key);
            String trimmed = trimTo(label, trimW);
            context.drawText(textRenderer, Text.literal(trimmed), px + 10, rowY, textColor, false);
            context.drawText(textRenderer, Text.literal(key), px + panelW - 10 - textRenderer.getWidth(key), rowY, keyColor, false);
            rowY += rowH;
        }
    }

    private List<MultiKeyBinding> activeBindings() {
        List<MultiKeyBinding> list = new ArrayList<>();
        VibeVisualsConfig.MultiKeyBindingsConfig cfg = VibeVisualsConfigManager.get().multiKeyBindings;
        if (cfg == null || cfg.bindings == null) {
            return list;
        }
        for (MultiKeyBinding binding : cfg.bindings) {
            if (binding != null && binding.enabled) {
                list.add(binding);
            }
        }
        return list;
    }

    private String trimTo(String text, int pixelWidth) {
        if (pixelWidth <= 0) {
            return "";
        }
        if (textRenderer.getWidth(text) <= pixelWidth) {
            return text;
        }
        StringBuilder builder = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (textRenderer.getWidth(builder.toString() + c + "…") > pixelWidth) {
                builder.append('…');
                return builder.toString();
            }
            builder.append(c);
        }
        return builder.toString();
    }

    private void renderMainWindow(DrawContext context, int mouseX, int mouseY, float eased) {
        int wx = windowX();
        int wy = windowY();
        int ww = windowWidth();
        int wh = windowHeight();

        windowSettings.opacity = 0.94f * eased;
        HudCardRenderer.drawCard(context, wx, wy, ww, wh, windowSettings);
        HudCardRenderer.drawOverlayCard(context, wx, wy, ww, 38, MenuTheme.RADIUS_WINDOW, MenuTheme.PURPLE_DEEP, 0.55f * eased);
        HudCardRenderer.drawOverlayCard(context, wx, wy, ww, wh, MenuTheme.RADIUS_WINDOW, MenuTheme.BG_DEEP, 0.32f * eased);
        HudCardRenderer.drawShaderOutline(context, wx, wy, ww, wh, MenuTheme.RADIUS_WINDOW, 0.6f, 0.34f * eased);

        renderHeader(context, mouseX, mouseY, eased, wx, wy, ww);
        renderFeatureGrid(context, mouseX, mouseY, eased);
        renderDock(context, mouseX, mouseY, eased, wx, wy, ww, wh);
    }

    private void renderHeader(DrawContext context, int mouseX, int mouseY, float eased, int wx, int wy, int ww) {
        // Logo + brand
        int logoX = wx + MenuTheme.PADDING_WINDOW;
        int logoY = wy + 12;
        drawPulseLogo(context, logoX, logoY + 1, MenuTheme.ACCENT_BRIGHT, eased);
        context.drawText(textRenderer, Text.literal("vibevisuals"), logoX + 18, logoY + 2, MenuTheme.withAlpha(MenuTheme.TEXT_PRIMARY, eased), false);

        // Category tabs with slash separators
        List<Category> visible = visibleCategories();
        int tabsX = logoX;
        int tabsY = wy + 32;
        hoveredCategoryIndex = -1;
        int cursorX = tabsX;
        for (int i = 0; i < visible.size(); i++) {
            Category category = visible.get(i);
            String label = category.label;
            int labelW = textRenderer.getWidth(label);
            boolean selected = category == selectedCategory && searchQuery.isEmpty();
            int hoverPad = 4;
            boolean hovered = mouseX >= cursorX - hoverPad && mouseX <= cursorX + labelW + hoverPad
                    && mouseY >= tabsY - 3 && mouseY <= tabsY + 10;
            if (hovered) {
                hoveredCategoryIndex = i;
            }
            int color = selected ? MenuTheme.TEXT_PRIMARY : (hovered ? MenuTheme.TEXT_SECONDARY | 0xFF000000 : MenuTheme.TEXT_MUTED | 0xFF000000);
            context.drawText(textRenderer, Text.literal(label), cursorX, tabsY, MenuTheme.withAlpha(color, eased), false);
            if (selected) {
                int underY = tabsY + 11;
                context.fill(cursorX, underY, cursorX + labelW, underY + 1, MenuTheme.withAlpha(MenuTheme.ACCENT_BRIGHT, eased));
            }
            cursorX += labelW;
            if (i < visible.size() - 1) {
                context.drawText(textRenderer, Text.literal(" / "), cursorX, tabsY, MenuTheme.withAlpha(MenuTheme.TEXT_DISABLED | 0xFF000000, eased), false);
                cursorX += textRenderer.getWidth(" / ");
            }
        }

        // Search field background
        int searchW = 130;
        int searchH = 18;
        int searchX = wx + ww - MenuTheme.PADDING_WINDOW - searchW;
        int searchY = wy + 28;
        HudCardRenderer.drawOverlayCard(context, searchX, searchY, searchW, searchH, MenuTheme.RADIUS_INPUT, MenuTheme.BG_INPUT, 0.85f * eased);
        HudCardRenderer.drawShaderOutline(context, searchX, searchY, searchW, searchH, MenuTheme.RADIUS_INPUT, 0.5f, 0.28f * eased);
        // Magnifier glyph
        drawSearchGlyph(context, searchX + 8, searchY + 6, MenuTheme.withAlpha(MenuTheme.TEXT_MUTED | 0xFF000000, eased));
        // Right small button
        int sideBtn = 12;
        context.fill(searchX + searchW - sideBtn - 4, searchY + 3, searchX + searchW - 4, searchY + 3 + sideBtn,
                MenuTheme.withAlpha(MenuTheme.BG_PANEL, 0.85f * eased));
        context.drawText(textRenderer, Text.literal("⋯"), searchX + searchW - sideBtn + 1, searchY + 5,
                MenuTheme.withAlpha(MenuTheme.TEXT_MUTED | 0xFF000000, eased), false);

        // place search field widget over the visual frame
        if (searchField != null) {
            searchField.setX(searchX + 18);
            searchField.setY(searchY + 4);
            searchField.setWidth(searchW - 36);
            searchField.setHeight(12);
        }
    }

    private void renderFeatureGrid(DrawContext context, int mouseX, int mouseY, float eased) {
        int gx = gridX();
        int gy = gridY();
        int gw = gridWidth();
        int gh = gridHeight();

        // top fade separator
        context.fill(gx, gy - 6, gx + gw, gy - 5, MenuTheme.withAlpha(MenuTheme.BORDER_SUBTLE | 0xFF000000, 0.5f * eased));

        List<FeatureEntry> visible = visibleFeatures();
        int columnW = (gw - CARD_GAP - SCROLLBAR_WIDTH - 6) / 2;
        int rows = (visible.size() + 1) / 2;
        gridContentHeight = rows * (CARD_HEIGHT + CARD_GAP);
        maxScroll = Math.max(0, gridContentHeight - gh);
        if (scroll > maxScroll) {
            scroll = maxScroll;
        }

        context.enableScissor(gx, gy, gx + gw, gy + gh);
        hoveredFeature = null;
        for (int i = 0; i < visible.size(); i++) {
            FeatureEntry feature = visible.get(i);
            int column = i % 2;
            int row = i / 2;
            int fx = gx + column * (columnW + CARD_GAP);
            int fy = gy + row * (CARD_HEIGHT + CARD_GAP) - scroll;
            feature.x = fx;
            feature.y = fy;
            feature.width = columnW;
            feature.height = CARD_HEIGHT;
            feature.toggleWidth = MenuTheme.TOGGLE_WIDTH;
            feature.toggleHeight = MenuTheme.TOGGLE_HEIGHT;
            feature.toggleX = fx + columnW - feature.toggleWidth - 10;
            feature.toggleY = fy + (CARD_HEIGHT - feature.toggleHeight) / 2;
            renderFeatureCard(context, mouseX, mouseY, feature, eased);
        }
        context.disableScissor();

        if (maxScroll > 0) {
            int trackX = gx + gw - SCROLLBAR_WIDTH;
            int trackTop = gy + 2;
            int trackBottom = gy + gh - 2;
            int trackH = trackBottom - trackTop;
            int thumbH = Math.max(16, (int) ((float) gh / gridContentHeight * trackH));
            int thumbY = trackTop + (int) ((float) scroll / maxScroll * (trackH - thumbH));
            context.fill(trackX, trackTop, trackX + SCROLLBAR_WIDTH, trackBottom,
                    MenuTheme.withAlpha(MenuTheme.BG_CARD, 0.55f * eased));
            context.fill(trackX, thumbY, trackX + SCROLLBAR_WIDTH, thumbY + thumbH,
                    MenuTheme.withAlpha(MenuTheme.ACCENT, 0.55f * eased));
        }

        // close side panel if its target scrolled out of grid
        if (settingsTarget != null) {
            boolean visibleInGrid = settingsTarget.y + settingsTarget.height > gy + 2 && settingsTarget.y < gy + gh - 2;
            boolean stillListed = visible.contains(settingsTarget);
            if (!stillListed || !visibleInGrid) {
                closeSidePanel();
            }
        }
    }

    private void renderFeatureCard(DrawContext context, int mouseX, int mouseY, FeatureEntry feature, float eased) {
        int gx = gridX();
        int gy = gridY();
        int gw = gridWidth();
        int gh = gridHeight();
        boolean inViewport = feature.y + feature.height > gy && feature.y < gy + gh;
        if (!inViewport) {
            feature.hoveredLastFrame = false;
            return;
        }
        boolean hoverable = mouseX >= gx && mouseX <= gx + gw && mouseY >= gy && mouseY <= gy + gh;
        boolean hovered = hoverable && mouseX >= feature.x && mouseX <= feature.x + feature.width
                && mouseY >= feature.y && mouseY <= feature.y + feature.height;
        if (hovered) {
            hoveredFeature = feature;
            if (!feature.hoveredLastFrame) {
                playHoverSound();
            }
        }
        feature.hoveredLastFrame = hovered;

        float targetHover = (hovered || feature == settingsTarget) ? 1.0f : 0.0f;
        feature.hoverProgress += (targetHover - feature.hoverProgress) * MenuTheme.HOVER_LERP;

        boolean enabled = feature.enabled.get();
        int baseBg = enabled ? MenuTheme.BG_CARD_ACTIVE : MenuTheme.BG_CARD;
        int bg = MenuTheme.lerpColor(baseBg, MenuTheme.BG_CARD_HOVER, feature.hoverProgress * 0.55f);
        HudCardRenderer.drawOverlayCard(context, feature.x, feature.y, feature.width, feature.height, MenuTheme.RADIUS_CARD, bg, 0.82f * eased);
        HudCardRenderer.drawShaderOutline(context, feature.x, feature.y, feature.width, feature.height, MenuTheme.RADIUS_CARD,
                0.5f, (enabled ? 0.35f : 0.18f) * eased + feature.hoverProgress * 0.20f);

        if (enabled) {
            HudCardRenderer.drawShaderOutline(context, feature.x - 2, feature.y - 2, feature.width + 4, feature.height + 4,
                    MenuTheme.RADIUS_CARD + 2, 0.6f, 0.18f * eased);
        }

        int nameColor = enabled ? MenuTheme.TEXT_PRIMARY : MenuTheme.TEXT_SECONDARY | 0xFF000000;
        context.drawText(textRenderer, Text.literal(feature.name), feature.x + 12, feature.y + (CARD_HEIGHT - 8) / 2,
                MenuTheme.withAlpha(nameColor, eased), false);

        // animated toggle
        feature.knobProgress += ((enabled ? 1.0f : 0.0f) - feature.knobProgress) * MenuTheme.KNOB_LERP;
        drawAnimatedToggle(context, feature.toggleX, feature.toggleY, feature.toggleWidth, feature.toggleHeight, feature.knobProgress, eased);
    }

    private void drawAnimatedToggle(DrawContext context, int x, int y, int w, int h, float knobProgress, float eased) {
        int trackOff = MenuTheme.BG_CARD;
        int trackOn = MenuTheme.ACCENT;
        int trackColor = MenuTheme.lerpColor(trackOff, trackOn, knobProgress);
        HudCardRenderer.drawOverlayCard(context, x, y, w, h, h / 2.0f, trackColor, 0.85f * eased);
        if (knobProgress > 0.1f) {
            HudCardRenderer.drawShaderOutline(context, x - 1, y - 1, w + 2, h + 2, h / 2.0f + 1, 0.4f, 0.40f * knobProgress * eased);
        }
        int knobSize = h - 4;
        int knobMinX = x + 2;
        int knobMaxX = x + w - knobSize - 2;
        int knobX = Math.round(knobMinX + (knobMaxX - knobMinX) * knobProgress);
        int knobColor = MenuTheme.lerpColor(0xFFB5B7C2, 0xFFFFFFFF, knobProgress);
        HudCardRenderer.drawOverlayCard(context, knobX, y + 2, knobSize, knobSize, knobSize / 2.0f, knobColor, eased);
    }

    private void renderDock(DrawContext context, int mouseX, int mouseY, float eased, int wx, int wy, int ww, int wh) {
        int dockW = 168;
        int dockH = 26;
        int dockX = wx + ww / 2 - dockW / 2;
        int dockY = wy + wh - dockH - 6;

        HudCardRenderer.drawOverlayCard(context, dockX, dockY, dockW, dockH, MenuTheme.RADIUS_BUTTON, MenuTheme.BG_PANEL_SOFT, 0.85f * eased);
        HudCardRenderer.drawShaderOutline(context, dockX, dockY, dockW, dockH, MenuTheme.RADIUS_BUTTON, 0.5f, 0.30f * eased);

        List<Category> visible = visibleCategories();
        int slots = visible.size() + 2; // categories + multi-binding + config file
        int slotW = (dockW - 8) / slots;
        hoveredDockIndex = -1;

        for (int i = 0; i < visible.size(); i++) {
            int sx = dockX + 4 + i * slotW;
            int sy = dockY + 3;
            int sh = dockH - 6;
            boolean hovered = mouseX >= sx && mouseX <= sx + slotW - 2 && mouseY >= sy && mouseY <= sy + sh;
            boolean active = visible.get(i) == selectedCategory && searchQuery.isEmpty();
            if (hovered) {
                hoveredDockIndex = i;
            }
            if (active) {
                HudCardRenderer.drawOverlayCard(context, sx, sy, slotW - 2, sh, MenuTheme.RADIUS_BUTTON - 2, MenuTheme.ACCENT, 0.85f * eased);
                HudCardRenderer.drawShaderOutline(context, sx - 1, sy - 1, slotW, sh + 2, MenuTheme.RADIUS_BUTTON - 1, 0.5f, 0.45f * eased);
            } else if (hovered) {
                HudCardRenderer.drawOverlayCard(context, sx, sy, slotW - 2, sh, MenuTheme.RADIUS_BUTTON - 2, MenuTheme.BG_CARD_HOVER, 0.75f * eased);
            }
            drawCategoryIcon(context, sx + (slotW - 2) / 2, sy + sh / 2, visible.get(i), active ? 0xFFFFFFFF : (MenuTheme.TEXT_SECONDARY | 0xFF000000), eased);
        }

        int extraStart = dockX + 4 + visible.size() * slotW;
        for (int i = 0; i < 2; i++) {
            int sx = extraStart + i * slotW;
            int sy = dockY + 3;
            int sh = dockH - 6;
            boolean hovered = mouseX >= sx && mouseX <= sx + slotW - 2 && mouseY >= sy && mouseY <= sy + sh;
            if (hovered) {
                hoveredDockIndex = visible.size() + i;
                HudCardRenderer.drawOverlayCard(context, sx, sy, slotW - 2, sh, MenuTheme.RADIUS_BUTTON - 2, MenuTheme.BG_CARD_HOVER, 0.75f * eased);
            }
            if (i == 0) {
                drawKeyboardGlyph(context, sx + (slotW - 2) / 2 - 4, sy + sh / 2 - 3, MenuTheme.withAlpha(MenuTheme.TEXT_PRIMARY, eased));
            } else {
                drawFolderGlyph(context, sx + (slotW - 2) / 2 - 4, sy + sh / 2 - 3, MenuTheme.withAlpha(MenuTheme.TEXT_PRIMARY, eased));
            }
        }
    }

    private void renderSidePanel(DrawContext context, int mouseX, int mouseY, float eased) {
        if (settingsTarget == null) {
            sideSlideProgress *= 0.5f;
            return;
        }
        sideSlideProgress += (1.0f - sideSlideProgress) * MenuTheme.SIDE_SLIDE_LERP;
        float sideEased = easeOutCubic(sideSlideProgress) * eased;

        // ensure side panel size: 8px padding + entries * rowH
        int rowH = 16;
        int padding = 12;
        int titleArea = 38;
        int desiredH = titleArea + Math.max(1, settingEntries.size()) * rowH + padding;
        int maxH = height - 40;
        sidePanelHeight = Math.min(desiredH, maxH);

        int sw = MenuTheme.SIDE_PANEL_WIDTH;
        int sh = sidePanelHeight;
        int wx = windowX();
        int ww = windowWidth();

        int targetX;
        if (settingsSide == Side.RIGHT) {
            int slideOffset = (int) ((1.0f - sideEased) * 16.0f);
            targetX = wx + ww + 12 + slideOffset;
            if (targetX + sw > width - 8) {
                targetX = width - sw - 8;
            }
        } else {
            int slideOffset = (int) ((1.0f - sideEased) * 16.0f);
            targetX = wx - sw - 12 - slideOffset;
            if (targetX < 8) {
                targetX = 8;
            }
        }

        int targetY = settingsTarget.y + settingsTarget.height / 2 - sh / 2;
        targetY = Math.max(windowY(), Math.min(windowY() + windowHeight() - sh, targetY));
        targetY = Math.max(12, Math.min(height - sh - 12, targetY));

        sidePanelSettings.opacity = 0.96f * sideEased;
        HudCardRenderer.drawCard(context, targetX, targetY, sw, sh, sidePanelSettings);
        HudCardRenderer.drawOverlayCard(context, targetX, targetY, sw, sh, MenuTheme.RADIUS_WINDOW, MenuTheme.PURPLE_DEEP, 0.20f * sideEased);
        HudCardRenderer.drawShaderOutline(context, targetX, targetY, sw, sh, MenuTheme.RADIUS_WINDOW, 0.55f, 0.32f * sideEased);

        // header
        context.drawText(textRenderer, Text.literal(settingsTarget.name), targetX + 14, targetY + 12,
                MenuTheme.withAlpha(MenuTheme.TEXT_PRIMARY, sideEased), false);
        String status = settingsTarget.enabled.get() ? "Enabled" : "Disabled";
        int statusColor = settingsTarget.enabled.get() ? MenuTheme.ACCENT_BRIGHT : (MenuTheme.TEXT_MUTED | 0xFF000000);
        context.drawText(textRenderer, Text.literal(status), targetX + 14, targetY + 24,
                MenuTheme.withAlpha(statusColor, 0.95f * sideEased), false);

        // separator
        context.fill(targetX + 12, targetY + 36, targetX + sw - 12, targetY + 37,
                MenuTheme.withAlpha(MenuTheme.BORDER_SUBTLE | 0xFF000000, 0.7f * sideEased));

        int rowsTop = targetY + titleArea;
        int rowsBottom = targetY + sh - padding;
        int listHeight = rowsBottom - rowsTop;
        int contentHeight = settingEntries.size() * rowH;
        int maxSettingScroll = Math.max(0, contentHeight - listHeight);
        if (settingsScroll > maxSettingScroll) {
            settingsScroll = maxSettingScroll;
        }

        hoveredSettingEntry = null;

        context.enableScissor(targetX + 6, rowsTop, targetX + sw - 6, rowsBottom);
        if (settingEntries.isEmpty()) {
            context.drawText(textRenderer, Text.literal("No settings"), targetX + 14, rowsTop + 4,
                    MenuTheme.withAlpha(MenuTheme.TEXT_MUTED | 0xFF000000, sideEased), false);
        }

        for (int i = 0; i < settingEntries.size(); i++) {
            SettingEntry entry = settingEntries.get(i);
            int rowY = rowsTop + i * rowH - settingsScroll;
            entry.y = rowY;
            entry.x = targetX + 12;
            entry.width = sw - 24;
            entry.height = rowH - 2;
            boolean visible = rowY + rowH > rowsTop && rowY < rowsBottom;
            if (entry.input != null) {
                entry.input.setX(targetX + sw - 64);
                entry.input.setY(rowY);
                entry.input.setWidth(48);
                entry.input.setHeight(rowH - 4);
                entry.input.visible = visible;
            }
            if (!visible) {
                continue;
            }
            boolean rowHovered = mouseX >= entry.x && mouseX <= entry.x + entry.width
                    && mouseY >= rowY && mouseY <= rowY + entry.height;
            if (rowHovered) {
                hoveredSettingEntry = entry;
            }
            context.drawText(textRenderer, settingNameText(entry.field.getName()), targetX + 14, rowY + 4,
                    MenuTheme.withAlpha(MenuTheme.TEXT_SECONDARY | 0xFF000000, 0.9f * sideEased), false);
            if (entry.field.getType() == boolean.class) {
                boolean value = readBoolean(settingsTarget.config, entry.field);
                entry.knobProgress += ((value ? 1.0f : 0.0f) - entry.knobProgress) * MenuTheme.KNOB_LERP;
                drawAnimatedToggle(context, targetX + sw - 30, rowY + 2, 20, 10, entry.knobProgress, sideEased);
            } else if (isColorField(entry.field)) {
                int swatch = readColor(settingsTarget.config, entry.field);
                int sx = targetX + sw - 30;
                int sy = rowY + 3;
                context.fill(sx, sy, sx + 18, sy + 8, MenuTheme.withAlpha(MenuTheme.BG_DEEP, sideEased));
                context.fill(sx + 1, sy + 1, sx + 17, sy + 7, 0xFF000000 | (swatch & 0x00FFFFFF));
                HudCardRenderer.drawShaderOutline(context, sx - 1, sy - 1, 20, 10, 3.0f, 0.4f, 0.4f * sideEased);
            }
        }
        context.disableScissor();

        if (maxSettingScroll > 0) {
            int trackX = targetX + sw - 3;
            int trackTop = rowsTop;
            int trackBottom = rowsBottom;
            int trackH = trackBottom - trackTop;
            int thumbH = Math.max(14, (int) ((float) listHeight / contentHeight * trackH));
            int thumbY = trackTop + (int) ((float) settingsScroll / maxSettingScroll * (trackH - thumbH));
            context.fill(trackX, trackTop, trackX + 2, trackBottom, MenuTheme.withAlpha(MenuTheme.BG_CARD, 0.55f * sideEased));
            context.fill(trackX, thumbY, trackX + 2, thumbY + thumbH, MenuTheme.withAlpha(MenuTheme.ACCENT, 0.55f * sideEased));
        }
    }

    private void renderColorPicker(DrawContext context, int mouseX, int mouseY, float eased) {
        SettingEntry entry = activeColorEntry;
        if (settingsTarget == null || entry == null || !isColorField(entry.field)) {
            return;
        }
        int pickerSize = 86;
        int sw = MenuTheme.SIDE_PANEL_WIDTH;
        int basePanelX = settingsSide == Side.RIGHT ? windowX() + windowWidth() + 12 : windowX() - sw - 12;
        int x = basePanelX + (settingsSide == Side.RIGHT ? sw + 8 : -pickerSize - 18);
        if (x + pickerSize + 12 > width) {
            x = Math.max(8, width - pickerSize - 12);
        }
        if (x < 8) {
            x = 8;
        }
        int y = Math.max(12, Math.min(height - pickerSize - 36, entry.y));
        pickerX = x;
        pickerY = y;
        pickerWidth = pickerSize;
        pickerHeight = 38;

        HudCardRenderer.drawCard(context, x - 6, y - 8, pickerSize + 12, pickerHeight + 28, sidePanelSettings);
        int selected = readColor(settingsTarget.config, entry.field);
        for (int px = 0; px < pickerSize; px += 2) {
            float hue = px / (float) Math.max(1, pickerSize - 1);
            context.fill(x + px, y, x + Math.min(pickerSize, px + 2), y + 18, 0xFF000000 | hsvToRgb(hue, 0.92f, 1.0f));
        }
        int hueColor = 0xFF000000 | (selected & 0x00FFFFFF);
        for (int px = 0; px < pickerSize; px += 2) {
            float value = px / (float) Math.max(1, pickerSize - 1);
            context.fill(x + px, y + 22, x + Math.min(pickerSize, px + 2), y + 38, mixColor(0x000000, hueColor & 0x00FFFFFF, value) | 0xFF000000);
        }
        context.drawText(textRenderer, Text.literal("Color"), x, y + pickerHeight + 6, MenuTheme.withAlpha(MenuTheme.TEXT_PRIMARY, eased), false);
        context.fill(x + 42, y + pickerHeight + 6, x + 64, y + pickerHeight + 16, 0xFF000000);
        context.fill(x + 43, y + pickerHeight + 7, x + 63, y + pickerHeight + 15, 0xFF000000 | (selected & 0x00FFFFFF));
    }

    private void renderSettingTooltip(DrawContext context, int mouseX, int mouseY) {
        if (hoveredSettingEntry == null || settingsTarget == null) {
            return;
        }
        String name = hoveredSettingEntry.field.getName();
        Text description = Text.translatableWithFallback("screen.vibevisuals.setting." + name, humanize(name));
        int tw = Math.min(180, textRenderer.getWidth(description) + 14);
        int th = 16;
        int tx = Math.min(width - tw - 8, mouseX + 12);
        int ty = Math.min(height - th - 8, mouseY + 12);
        HudCardRenderer.drawOverlayCard(context, tx, ty, tw, th, MenuTheme.RADIUS_PILL, MenuTheme.BG_PANEL_SOFT, 0.90f);
        HudCardRenderer.drawShaderOutline(context, tx, ty, tw, th, MenuTheme.RADIUS_PILL, 0.4f, 0.3f);
        context.drawText(textRenderer, description, tx + 7, ty + 4, MenuTheme.TEXT_SECONDARY | 0xFF000000, false);
    }

    // ---------- Mouse handling ----------

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        int button = click.button();
        double mx = click.x();
        double my = click.y();

        // Search field
        if (searchField != null && searchField.mouseClicked(click, doubled)) {
            return true;
        }
        if (super.mouseClicked(click, doubled)) {
            return true;
        }

        // Color picker first (active)
        if (activeColorEntry != null && tryPickColor(activeColorEntry, mx, my)) {
            return true;
        }

        // Dock clicks
        if (hoveredDockIndex >= 0) {
            List<Category> visible = visibleCategories();
            if (hoveredDockIndex < visible.size()) {
                selectedCategory = visible.get(hoveredDockIndex);
                if (searchField != null) {
                    searchField.setText("");
                }
                searchQuery = "";
                scroll = 0;
                closeSidePanel();
                playToggleSound();
            } else if (hoveredDockIndex == visible.size()) {
                if (client != null) {
                    client.setScreen(new MultiKeyBindingsScreen(this));
                }
            } else {
                Util.getOperatingSystem().open(VibeVisualsConfigManager.getConfigPath().toFile());
            }
            return true;
        }

        // Category tabs
        if (hoveredCategoryIndex >= 0) {
            List<Category> visible = visibleCategories();
            selectedCategory = visible.get(hoveredCategoryIndex);
            if (searchField != null) {
                searchField.setText("");
            }
            searchQuery = "";
            scroll = 0;
            closeSidePanel();
            playToggleSound();
            return true;
        }

        // Feature card / toggle
        if (hoveredFeature != null) {
            if (button == 0) {
                // LMB anywhere on the card toggles
                hoveredFeature.enabledSetter.accept(!hoveredFeature.enabled.get());
                playToggleSound();
                saveAndReload();
                return true;
            }
            if (button == 1) {
                // RMB opens / toggles side panel
                if (settingsTarget == hoveredFeature) {
                    closeSidePanel();
                } else {
                    openSidePanel(hoveredFeature);
                }
                playHoverSound();
                return true;
            }
        }

        // Click inside side panel
        if (settingsTarget != null && isInsideSidePanel(mx, my)) {
            SettingEntry entry = settingAt(mx, my);
            if (entry != null) {
                Class<?> type = entry.field.getType();
                if (type == boolean.class && button == 0) {
                    writeBoolean(settingsTarget.config, entry.field, !readBoolean(settingsTarget.config, entry.field));
                    playToggleSound();
                    saveAndReload();
                    return true;
                }
                if (isColorField(entry.field) && button == 0) {
                    activeColorEntry = entry;
                    return true;
                }
            }
            return true;
        }

        // Clicking elsewhere closes side panel
        if (settingsTarget != null) {
            closeSidePanel();
        }
        // Clicking elsewhere also blurs picker
        activeColorEntry = null;
        return false;
    }

    private boolean isInsideSidePanel(double mx, double my) {
        if (settingsTarget == null) {
            return false;
        }
        int sw = MenuTheme.SIDE_PANEL_WIDTH;
        int sh = sidePanelHeight;
        int wx = windowX();
        int ww = windowWidth();
        int x = settingsSide == Side.RIGHT ? wx + ww + 12 : wx - sw - 12;
        if (settingsSide == Side.RIGHT && x + sw > width - 8) {
            x = width - sw - 8;
        }
        if (settingsSide == Side.LEFT && x < 8) {
            x = 8;
        }
        int y = settingsTarget.y + settingsTarget.height / 2 - sh / 2;
        y = Math.max(windowY(), Math.min(windowY() + windowHeight() - sh, y));
        y = Math.max(12, Math.min(height - sh - 12, y));
        return mx >= x && mx <= x + sw && my >= y && my <= y + sh;
    }

    private SettingEntry settingAt(double mx, double my) {
        if (settingsTarget == null) {
            return null;
        }
        for (SettingEntry entry : settingEntries) {
            if (mx >= entry.x && mx <= entry.x + entry.width
                    && my >= entry.y && my <= entry.y + entry.height) {
                return entry;
            }
        }
        return null;
    }

    private void openSidePanel(FeatureEntry feature) {
        settingsTarget = feature;
        // determine side: based on feature column (right column → side panel on right of window? per spec right column → right side)
        int gx = gridX();
        int gw = gridWidth();
        boolean leftColumn = feature.x < gx + gw / 2;
        settingsSide = leftColumn ? Side.LEFT : Side.RIGHT;
        sideSlideProgress = 0.0f;
        settingsScroll = 0;
        activeColorEntry = null;
        rebuildSettings();
    }

    private void closeSidePanel() {
        for (SettingEntry entry : settingEntries) {
            if (entry.input != null) {
                remove(entry.input);
            }
        }
        settingEntries.clear();
        settingsTarget = null;
        activeColorEntry = null;
        sideSlideProgress = 0.0f;
    }

    private void rebuildSettings() {
        for (SettingEntry entry : settingEntries) {
            if (entry.input != null) {
                remove(entry.input);
            }
        }
        settingEntries.clear();
        if (settingsTarget == null || settingsTarget.config == null) {
            return;
        }
        for (Field field : settingsTarget.config.getClass().getFields()) {
            if (Modifier.isStatic(field.getModifiers()) || !isEditable(field)) {
                continue;
            }
            TextFieldWidget input = null;
            Class<?> type = field.getType();
            if (type != boolean.class && !isColorField(field)) {
                input = new TextFieldWidget(textRenderer, 0, 0, 50, 12, Text.literal(field.getName()));
                input.setDrawsBackground(false);
                input.setEditableColor(MenuTheme.TEXT_PRIMARY);
                input.setMaxLength(48);
                input.setText(readFieldAsText(settingsTarget.config, field));
                input.setChangedListener(value -> writeFieldFromText(settingsTarget.config, field, value));
                addDrawableChild(input);
            }
            settingEntries.add(new SettingEntry(field, input));
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        // side panel scroll
        if (settingsTarget != null && isInsideSidePanel(mouseX, mouseY)) {
            settingsScroll = Math.max(0, settingsScroll - (int) Math.round(verticalAmount * 14.0));
            return true;
        }
        // grid scroll
        int gx = gridX();
        int gy = gridY();
        int gw = gridWidth();
        int gh = gridHeight();
        if (mouseX >= gx && mouseX <= gx + gw && mouseY >= gy && mouseY <= gy + gh) {
            scroll = Math.max(0, Math.min(maxScroll, scroll - (int) Math.round(verticalAmount * 16.0)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
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
        if (activeColorEntry != null) {
            activeColorEntry = null;
        }
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

    // ---------- Helpers ----------

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

    private static String readFieldAsText(Object config, Field field) {
        try {
            Object value = field.get(config);
            return value == null ? "" : value.toString();
        } catch (IllegalAccessException exception) {
            return "";
        }
    }

    private static void writeFieldFromText(Object config, Field field, String value) {
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

    private static Text settingNameText(String name) {
        return Text.translatableWithFallback("screen.vibevisuals.setting_name." + name, humanize(name));
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
        if (settingsTarget == null || entry == null || !isColorField(entry.field)) {
            return false;
        }
        if (pickerWidth <= 0 || pickerHeight <= 0 || mouseX < pickerX || mouseX > pickerX + pickerWidth
                || mouseY < pickerY || mouseY > pickerY + pickerHeight) {
            return false;
        }
        int previous = readColor(settingsTarget.config, entry.field);
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
            entry.field.setInt(settingsTarget.config, argb);
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
            case 0 -> { r = value; g = t; b = p; }
            case 1 -> { r = q; g = value; b = p; }
            case 2 -> { r = p; g = value; b = t; }
            case 3 -> { r = p; g = q; b = value; }
            case 4 -> { r = t; g = p; b = value; }
            default -> { r = value; g = p; b = q; }
        }
        return ((int) (r * 255.0f) << 16) | ((int) (g * 255.0f) << 8) | (int) (b * 255.0f);
    }

    private static int mixColor(int from, int to, float amount) {
        int red = (int) (((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * amount);
        int green = (int) (((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * amount);
        int blue = (int) ((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * amount);
        return (red << 16) | (green << 8) | blue;
    }

    private static float easeOutCubic(float t) {
        float p = 1.0f - t;
        return 1.0f - p * p * p;
    }

    private void playHoverSound() {
        MinecraftClient minecraft = MinecraftClient.getInstance();
        minecraft.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK.value(), 1.15f, 0.045f));
    }

    private void playToggleSound() {
        MinecraftClient minecraft = MinecraftClient.getInstance();
        minecraft.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK.value(), 1.25f, 0.16f));
    }

    // ---------- Vector glyphs ----------

    private void drawPulseLogo(DrawContext context, int x, int y, int color, float eased) {
        int alpha = MenuTheme.withAlpha(color, eased);
        // simple ECG-like glyph
        context.fill(x, y + 4, x + 2, y + 5, alpha);
        context.fill(x + 2, y + 4, x + 4, y + 5, alpha);
        context.fill(x + 4, y + 2, x + 5, y + 6, alpha);
        context.fill(x + 5, y, x + 6, y + 8, alpha);
        context.fill(x + 6, y + 3, x + 7, y + 5, alpha);
        context.fill(x + 7, y + 4, x + 11, y + 5, alpha);
    }

    private void drawSearchGlyph(DrawContext context, int x, int y, int color) {
        // 6x6 circle + 2x2 handle
        context.fill(x + 1, y, x + 5, y + 1, color);
        context.fill(x, y + 1, x + 1, y + 5, color);
        context.fill(x + 5, y + 1, x + 6, y + 5, color);
        context.fill(x + 1, y + 5, x + 5, y + 6, color);
        context.fill(x + 5, y + 5, x + 7, y + 7, color);
    }

    private void drawKeyboardGlyph(DrawContext context, int x, int y, int color) {
        context.fill(x, y, x + 10, y + 6, color & 0x44FFFFFF | (color & 0xFF000000));
        for (int row = 0; row < 2; row++) {
            int ky = y + 1 + row * 2;
            for (int col = 0; col < 4; col++) {
                int kx = x + 1 + col * 2;
                context.fill(kx, ky, kx + 1, ky + 1, color);
            }
        }
    }

    private void drawFolderGlyph(DrawContext context, int x, int y, int color) {
        context.fill(x, y + 1, x + 4, y + 2, color);
        context.fill(x, y + 2, x + 10, y + 7, color & 0x88FFFFFF | (color & 0xFF000000));
        context.fill(x + 1, y + 3, x + 9, y + 6, color);
    }

    private void drawCategoryIcon(DrawContext context, int cx, int cy, Category category, int color, float eased) {
        int c = MenuTheme.withAlpha(color, eased);
        switch (category) {
            case VISUALS -> {
                context.fill(cx - 4, cy - 1, cx + 4, cy + 1, c);
                context.fill(cx - 1, cy - 4, cx + 1, cy + 4, c);
            }
            case HUD -> {
                context.fill(cx - 4, cy - 3, cx + 4, cy - 2, c);
                context.fill(cx - 4, cy + 2, cx + 4, cy + 3, c);
                context.fill(cx - 4, cy - 2, cx - 3, cy + 2, c);
                context.fill(cx + 3, cy - 2, cx + 4, cy + 2, c);
            }
            case UTILITIES -> {
                context.fill(cx - 3, cy - 3, cx - 1, cy - 1, c);
                context.fill(cx + 1, cy + 1, cx + 3, cy + 3, c);
                context.fill(cx - 1, cy - 1, cx + 1, cy + 1, c);
            }
            case PVP -> {
                context.fill(cx - 1, cy - 4, cx + 1, cy + 4, c);
                context.fill(cx - 4, cy - 1, cx + 4, cy + 1, c);
            }
            case MENU -> {
                context.fill(cx - 4, cy - 3, cx + 4, cy - 2, c);
                context.fill(cx - 4, cy - 1, cx + 4, cy, c);
                context.fill(cx - 4, cy + 1, cx + 4, cy + 2, c);
            }
            case WORLD -> {
                context.fill(cx - 3, cy - 3, cx + 3, cy + 3, c);
            }
        }
    }

    // ---------- Inner types ----------

    private enum Category {
        VISUALS("Visuals"),
        HUD("HUD"),
        UTILITIES("Utilities"),
        PVP("PvP"),
        MENU("Menu"),
        WORLD("World");

        final String label;

        Category(String label) {
            this.label = label;
        }
    }

    private enum Side {
        LEFT,
        RIGHT
    }

    private static class FeatureEntry {
        final Category category;
        final String name;
        final Supplier<Boolean> enabled;
        final Consumer<Boolean> enabledSetter;
        final Object config;
        float hoverProgress;
        float knobProgress;
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
        float knobProgress;
        int x;
        int y;
        int width;
        int height;

        SettingEntry(Field field, TextFieldWidget input) {
            this.field = field;
            this.input = input;
        }
    }
}
