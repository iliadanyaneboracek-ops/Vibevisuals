package ru.suppelemen.vibevisuals.feature.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import ru.suppelemen.vibevisuals.VibeVisualsClient;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;
import ru.suppelemen.vibevisuals.core.hud.HudManager;
import ru.suppelemen.vibevisuals.theme.HudCardRenderType;
import ru.suppelemen.vibevisuals.theme.HudVisualSettings;
import ru.suppelemen.vibevisuals.theme.MenuTheme;
import ru.suppelemen.vibevisuals.util.render.HudCardRenderer;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * ClickGUI for VibeVisuals.
 *
 * Layout is independent of Minecraft GUI scale: sizes are expressed in design
 * units (reference 1920x1080) and converted via {@link #dp(float)} which is
 * derived solely from the framebuffer/window size and counters the active
 * MC GUI scale.  This means the menu has a stable physical size regardless
 * of the player's "GUI Scale" setting.
 */
public class VibeVisualsMenuScreen extends Screen {

    private static final StyleSpriteSource MENU_FONT =
            new StyleSpriteSource.Font(Identifier.of(VibeVisualsClient.MOD_ID, "clickgui"));

    // Reference frame the design is authored against.
    private static final float REFERENCE_WIDTH = 1920.0f;
    private static final float REFERENCE_HEIGHT = 1080.0f;
    private static final float FONT_NATIVE_PX = 9.0f;

    // Text size tokens (design px).
    private static final float TEXT_LOGO = 14.0f;
    private static final float TEXT_TAB = 12.0f;
    private static final float TEXT_TAB_SLASH = 11.0f;
    private static final float TEXT_CARD = 12.5f;
    private static final float TEXT_SETTING_LABEL = 11.0f;
    private static final float TEXT_SETTING_VALUE = 10.5f;
    private static final float TEXT_SETTING_TITLE = 12.5f;
    private static final float TEXT_SETTING_STATUS = 10.0f;
    private static final float TEXT_SEARCH = 11.0f;

    // Window / layout tokens (design px).
    private static final int WINDOW_W = 740;
    private static final int WINDOW_H = 420;
    private static final int WINDOW_RADIUS = 20;
    private static final int PAD_WINDOW_X = 26;
    private static final int PAD_WINDOW_TOP = 24;
    private static final int HEADER_H = 76;
    private static final int CARD_H = 44;
    private static final int CARD_RADIUS = 14;
    private static final int CARD_GAP_X = 12;
    private static final int CARD_GAP_Y = 10;
    private static final int CARD_PAD_X = 18;
    private static final int TOGGLE_W = 30;
    private static final int TOGGLE_H = 16;
    private static final int SEARCH_W = 200;
    private static final int SEARCH_H = 28;
    private static final int SEARCH_RADIUS = 14;
    private static final int SIDE_PANEL_W = 248;
    private static final int SIDE_PANEL_RADIUS = 16;
    private static final int SIDE_ROW_H = 26;
    private static final int DOCK_H = 46;
    private static final int DOCK_RADIUS = 22;
    private static final int DOCK_ICON_SLOT = 36;
    private static final int DOCK_OFFSET_Y = 18;
    private static final int SCROLLBAR_W = 3;

    private final HudVisualSettings windowSettings = new HudVisualSettings();
    private final HudVisualSettings sideSettings = new HudVisualSettings();
    private final HudVisualSettings dockSettings = new HudVisualSettings();

    private final List<FeatureEntry> features = new ArrayList<>();

    private final SidePanelState leftPanel = new SidePanelState(Side.LEFT);
    private final SidePanelState rightPanel = new SidePanelState(Side.RIGHT);
    private final Map<Side, SidePanelState> panels = new EnumMap<>(Side.class);

    private TextFieldWidget searchField;
    private Category selectedCategory = Category.VISUALS;
    private String searchQuery = "";

    private int scroll;
    private int maxScroll;
    private int gridContentHeight;

    private FeatureEntry hoveredFeature;
    private boolean hoveredOverToggle;
    private int hoveredCategoryIndex = -1;
    private int hoveredDockIndex = -1;

    private long openedAtMs;
    private float openProgress;

    public VibeVisualsMenuScreen() {
        super(Text.literal("VibeVisuals"));
        windowSettings.renderType = HudCardRenderType.LIQUID_GLASS;
        windowSettings.opacity = 0.93f;
        sideSettings.renderType = HudCardRenderType.LIQUID_GLASS;
        sideSettings.opacity = 0.94f;
        dockSettings.renderType = HudCardRenderType.LIQUID_GLASS;
        dockSettings.opacity = 0.90f;
        panels.put(Side.LEFT, leftPanel);
        panels.put(Side.RIGHT, rightPanel);
    }

    // ---------- Scale helpers ----------

    private float dpScale() {
        MinecraftClient mc = client != null ? client : MinecraftClient.getInstance();
        if (mc == null) {
            return 1.0f;
        }
        float fbW = mc.getWindow().getWidth();
        float fbH = mc.getWindow().getHeight();
        float layoutScale = Math.min(fbW / REFERENCE_WIDTH, fbH / REFERENCE_HEIGHT);
        double guiScale = mc.getWindow().getScaleFactor();
        if (guiScale <= 0.0) {
            guiScale = 1.0;
        }
        float scaled = layoutScale / (float) guiScale;
        // Stay readable on small windows.
        return Math.max(scaled, 0.45f);
    }

    private int dp(float v) {
        return Math.round(v * dpScale());
    }

    private float textScale(float pxSize) {
        return dpScale() * (pxSize / FONT_NATIVE_PX);
    }

    private int textWidth(String text, float pxSize) {
        return Math.round(textRenderer.getWidth(text) * textScale(pxSize));
    }

    private int textHeight(float pxSize) {
        return Math.round(pxSize * dpScale());
    }

    private void drawScaledText(DrawContext context, String text, int x, int y, float pxSize, int color) {
        float scale = textScale(pxSize);
        context.getMatrices().pushMatrix();
        context.getMatrices().translate((float) x, (float) y);
        context.getMatrices().scale(scale, scale);
        context.drawText(textRenderer, styledText(text), 0, 0, color, false);
        context.getMatrices().popMatrix();
    }

    private static Text styledText(String text) {
        return Text.literal(text).styled(s -> s.withFont(MENU_FONT));
    }

    // ---------- Layout ----------

    private int windowW() { return Math.min(dp(WINDOW_W), width - dp(40)); }
    private int windowH() { return Math.min(dp(WINDOW_H), height - dp(96)); }
    private int windowX() { return (width - windowW()) / 2; }
    private int windowY() { return Math.max(dp(28), (height - windowH() - dp(DOCK_H + DOCK_OFFSET_Y)) / 2); }

    private int gridX() { return windowX() + dp(PAD_WINDOW_X); }
    private int gridY() { return windowY() + dp(HEADER_H); }
    private int gridW() { return windowW() - dp(PAD_WINDOW_X * 2); }
    private int gridH() { return windowY() + windowH() - dp(20) - gridY(); }

    // ---------- Setup ----------

    @Override
    protected void init() {
        rebuildFeatures();
        ensureSelectedCategoryHasEntries();
        openedAtMs = System.currentTimeMillis();

        int sw = dp(SEARCH_W);
        int sh = dp(SEARCH_H);
        int sx = windowX() + windowW() - dp(PAD_WINDOW_X) - sw;
        int sy = windowY() + dp(PAD_WINDOW_TOP + 24);

        searchField = new TextFieldWidget(textRenderer, sx + dp(26), sy + (sh - dp(12)) / 2,
                sw - dp(46), dp(12), Text.literal("search"));
        searchField.setDrawsBackground(false);
        searchField.setEditableColor(MenuTheme.TEXT_PRIMARY);
        searchField.setUneditableColor(MenuTheme.TEXT_MUTED);
        searchField.setMaxLength(48);
        // Placeholder rendered manually with scaled font; widget placeholder uses MC default font.
        searchField.setPlaceholder(Text.literal(""));
        searchField.setChangedListener(v -> {
            searchQuery = v == null ? "" : v;
            scroll = 0;
        });
        addDrawableChild(searchField);
    }

    private void rebuildFeatures() {
        VibeVisualsConfig c = VibeVisualsConfigManager.get();
        features.clear();
        // HUD
        features.add(new FeatureEntry(Category.HUD, "Potions", () -> c.potionsCard.enabled, v -> c.potionsCard.enabled = v, c.potionsCard));
        features.add(new FeatureEntry(Category.HUD, "Cooldowns", () -> c.cooldownsCard.enabled, v -> c.cooldownsCard.enabled = v, c.cooldownsCard));
        features.add(new FeatureEntry(Category.HUD, "Hot Keys", () -> c.hotKeysCard.enabled, v -> c.hotKeysCard.enabled = v, c.hotKeysCard));
        features.add(new FeatureEntry(Category.HUD, "Top Bar", () -> c.topBar.enabled, v -> c.topBar.enabled = v, c.topBar));
        features.add(new FeatureEntry(Category.HUD, "Inventory HUD", () -> c.inventoryHud.enabled, v -> c.inventoryHud.enabled = v, c.inventoryHud));
        features.add(new FeatureEntry(Category.HUD, "Armor HUD", () -> c.armorHud.enabled, v -> c.armorHud.enabled = v, c.armorHud));
        features.add(new FeatureEntry(Category.HUD, "Custom Hotbar", () -> c.hotbar.enabled, v -> c.hotbar.enabled = v, c.hotbar));
        // PVP
        features.add(new FeatureEntry(Category.PVP, "PvP Combat", () -> c.pvpCard.enabled, v -> c.pvpCard.enabled = v, c.pvpCard));
        features.add(new FeatureEntry(Category.PVP, "Target ESP", () -> c.targetEsp.enabled, v -> c.targetEsp.enabled = v, c.targetEsp));
        features.add(new FeatureEntry(Category.PVP, "Saturation", () -> c.saturationDisplay.enabled, v -> c.saturationDisplay.enabled = v, c.saturationDisplay));
        features.add(new FeatureEntry(Category.PVP, "Crit Hit Sound", () -> c.customHitSound.enabled, v -> c.customHitSound.enabled = v, c.customHitSound));
        features.add(new FeatureEntry(Category.PVP, "Shift Up", () -> c.shiftUp.enabled, v -> c.shiftUp.enabled = v, c.shiftUp));
        // VISUALS
        features.add(new FeatureEntry(Category.VISUALS, "Sky Color", () -> c.visualEffects.skyColorEnabled, v -> c.visualEffects.skyColorEnabled = v, c.visualEffects));
        features.add(new FeatureEntry(Category.VISUALS, "Fog Color", () -> c.visualEffects.fogColorEnabled, v -> c.visualEffects.fogColorEnabled = v, c.visualEffects));
        features.add(new FeatureEntry(Category.VISUALS, "Particles", () -> c.visualEffects.customParticlesEnabled, v -> c.visualEffects.customParticlesEnabled = v, c.visualEffects));
        features.add(new FeatureEntry(Category.VISUALS, "Screen Fire", () -> c.fireOverlay.enabled, v -> c.fireOverlay.enabled = v, c.fireOverlay));
        features.add(new FeatureEntry(Category.VISUALS, "Crosshair", () -> c.customCrosshair.enabled, v -> c.customCrosshair.enabled = v, c.customCrosshair));
        features.add(new FeatureEntry(Category.VISUALS, "Custom Hand", () -> c.customHand.enabled, v -> c.customHand.enabled = v, c.customHand));
        // UTILITIES
        features.add(new FeatureEntry(Category.UTILITIES, "Projectile Path", () -> c.projectilePrediction.enabled, v -> c.projectilePrediction.enabled = v, c.projectilePrediction));
        features.add(new FeatureEntry(Category.UTILITIES, "HUD Animations", () -> c.hudAnimations.enabled, v -> c.hudAnimations.enabled = v, c.hudAnimations));
        features.add(new FeatureEntry(Category.UTILITIES, "Markers", () -> c.markers.enabled, v -> c.markers.enabled = v, c.markers));
        features.add(new FeatureEntry(Category.UTILITIES, "AutoEat", () -> c.autoEat.enabled, v -> c.autoEat.enabled = v, c.autoEat));
        features.add(new FeatureEntry(Category.UTILITIES, "AutoPotion", () -> c.autoPotion.enabled, v -> c.autoPotion.enabled = v, c.autoPotion));
        features.add(new FeatureEntry(Category.UTILITIES, "AutoRespawn", () -> c.autoRespawn.enabled, v -> c.autoRespawn.enabled = v, c.autoRespawn));
        features.add(new FeatureEntry(Category.UTILITIES, "Tape Mouse", () -> c.tapeMouse.enabled, v -> c.tapeMouse.enabled = v, c.tapeMouse));
        features.add(new FeatureEntry(Category.UTILITIES, "Full Bright", () -> c.fullBrightStrength > 0.0f,
                v -> c.fullBrightStrength = v ? Math.max(0.6f, c.fullBrightStrength) : 0.0f, c));
        // MENU
        features.add(new FeatureEntry(Category.MENU, "Menu Settings", () -> c.menu.enabled, v -> c.menu.enabled = v, c.menu));
    }

    private void ensureSelectedCategoryHasEntries() {
        if (hasAnyFeature(selectedCategory)) return;
        for (Category cat : Category.values()) {
            if (hasAnyFeature(cat)) {
                selectedCategory = cat;
                return;
            }
        }
    }

    private boolean hasAnyFeature(Category cat) {
        for (FeatureEntry f : features) if (f.category == cat) return true;
        return false;
    }

    private List<Category> visibleCategories() {
        List<Category> out = new ArrayList<>();
        for (Category cat : Category.values()) if (hasAnyFeature(cat)) out.add(cat);
        return out;
    }

    private List<FeatureEntry> visibleFeatures() {
        List<FeatureEntry> out = new ArrayList<>();
        String q = searchQuery.trim().toLowerCase(Locale.ROOT);
        for (FeatureEntry f : features) {
            if (!q.isEmpty()) {
                if (f.name.toLowerCase(Locale.ROOT).contains(q)) out.add(f);
            } else if (f.category == selectedCategory) {
                out.add(f);
            }
        }
        return out;
    }

    // ---------- Render entry ----------

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        long now = System.currentTimeMillis();
        openProgress = Math.min(1.0f, Math.max(0.0f, (now - openedAtMs) / MenuTheme.OPEN_ANIM_DURATION_MS));
        float eased = easeOutCubic(openProgress);

        // Reposition search/setting inputs to current dp scale each frame.
        positionSearchField();
        positionSidePanelInputs(leftPanel);
        positionSidePanelInputs(rightPanel);

        renderDim(context, eased);
        renderMainWindow(context, mouseX, mouseY, eased);
        renderDock(context, mouseX, mouseY, eased);
        advanceSidePanel(leftPanel);
        advanceSidePanel(rightPanel);
        renderSidePanel(context, mouseX, mouseY, eased, leftPanel);
        renderSidePanel(context, mouseX, mouseY, eased, rightPanel);
        super.render(context, mouseX, mouseY, delta);
    }

    private void renderDim(DrawContext context, float eased) {
        int alpha = (int) (0x9C * eased) & 0xFF;
        context.fill(0, 0, width, height, (alpha << 24) | 0x07050F);
    }

    // ---------- Main window ----------

    private void renderMainWindow(DrawContext context, int mouseX, int mouseY, float eased) {
        int wx = windowX();
        int wy = windowY();
        int ww = windowW();
        int wh = windowH();

        windowSettings.radius = dp(WINDOW_RADIUS);
        windowSettings.opacity = 0.92f * eased;
        HudCardRenderer.drawCard(context, wx, wy, ww, wh, windowSettings);

        // Subtle deep tint baseline.
        HudCardRenderer.drawOverlayCard(context, wx, wy, ww, wh, dp(WINDOW_RADIUS),
                MenuTheme.BG_PANEL, 0.30f * eased);

        // Top purple glow gradient (fades down) – approximated with 3 thin overlay strips.
        int gradH = dp(70);
        int gradient = MenuTheme.PURPLE_DEEP;
        HudCardRenderer.drawOverlayCard(context, wx, wy, ww, gradH, dp(WINDOW_RADIUS), gradient, 0.55f * eased);
        HudCardRenderer.drawOverlayCard(context, wx, wy, ww, gradH * 2 / 3, dp(WINDOW_RADIUS), gradient, 0.35f * eased);
        HudCardRenderer.drawOverlayCard(context, wx, wy, ww, gradH / 3, dp(WINDOW_RADIUS), gradient, 0.30f * eased);

        // Outline (very subtle).
        HudCardRenderer.drawShaderOutline(context, wx, wy, ww, wh, dp(WINDOW_RADIUS), 0.6f, 0.28f * eased);

        renderHeader(context, mouseX, mouseY, eased, wx, wy, ww);
        renderFeatureGrid(context, mouseX, mouseY, eased);
    }

    private void renderHeader(DrawContext context, int mouseX, int mouseY, float eased, int wx, int wy, int ww) {
        // Brand (centered).
        String brand = "vibevisuals";
        int brandWidth = textWidth(brand, TEXT_LOGO);
        int iconSize = dp(16);
        int gap = dp(8);
        int brandBlockW = iconSize + gap + brandWidth;
        int brandX = wx + (ww - brandBlockW) / 2;
        int brandY = wy + dp(PAD_WINDOW_TOP);

        drawPulseLogo(context, brandX, brandY + dp(1), iconSize,
                MenuTheme.withAlpha(MenuTheme.ACCENT_BRIGHT, eased));
        drawScaledText(context, brand, brandX + iconSize + gap, brandY,
                TEXT_LOGO, MenuTheme.withAlpha(MenuTheme.TEXT_PRIMARY, eased));

        // Tabs row (left) + search (right).
        int rowY = wy + dp(PAD_WINDOW_TOP + 24);
        renderTabs(context, mouseX, mouseY, eased, wx + dp(PAD_WINDOW_X), rowY);
        renderSearch(context, eased, wx + ww - dp(PAD_WINDOW_X + SEARCH_W), rowY - (dp(SEARCH_H) - textHeight(TEXT_TAB)) / 2);

        // Separator under header row.
        int sepY = wy + dp(HEADER_H - 6);
        context.fill(wx + dp(PAD_WINDOW_X), sepY, wx + ww - dp(PAD_WINDOW_X), sepY + 1,
                MenuTheme.withAlpha(MenuTheme.BORDER_SUBTLE | 0xFF000000, 0.55f * eased));
    }

    private void renderTabs(DrawContext context, int mouseX, int mouseY, float eased, int x, int y) {
        List<Category> visible = visibleCategories();
        hoveredCategoryIndex = -1;
        int cursor = x;
        int tabHitTop = y - dp(4);
        int tabHitBot = y + textHeight(TEXT_TAB) + dp(4);
        for (int i = 0; i < visible.size(); i++) {
            Category cat = visible.get(i);
            String label = cat.label;
            int labelW = textWidth(label, TEXT_TAB);
            boolean selected = cat == selectedCategory && searchQuery.isEmpty();
            boolean hovered = mouseX >= cursor && mouseX <= cursor + labelW
                    && mouseY >= tabHitTop && mouseY <= tabHitBot;
            if (hovered) hoveredCategoryIndex = i;

            int color;
            if (selected) color = MenuTheme.withAlpha(MenuTheme.TEXT_PRIMARY, eased);
            else if (hovered) color = MenuTheme.withAlpha(MenuTheme.TEXT_SECONDARY | 0xFF000000, 0.95f * eased);
            else color = MenuTheme.withAlpha(MenuTheme.TEXT_MUTED | 0xFF000000, eased);

            drawScaledText(context, label, cursor, y, TEXT_TAB, color);
            if (selected) {
                int underY = y + textHeight(TEXT_TAB) + dp(4);
                int underH = Math.max(1, dp(1.4f));
                context.fill(cursor, underY, cursor + labelW, underY + underH,
                        MenuTheme.withAlpha(MenuTheme.ACCENT_BRIGHT, eased));
            }
            cursor += labelW;
            if (i < visible.size() - 1) {
                String sep = "  /  ";
                int sepW = textWidth(sep, TEXT_TAB_SLASH);
                drawScaledText(context, sep, cursor, y, TEXT_TAB_SLASH,
                        MenuTheme.withAlpha(MenuTheme.TEXT_DISABLED | 0xFF000000, eased));
                cursor += sepW;
            }
        }
    }

    private void renderSearch(DrawContext context, float eased, int sx, int sy) {
        int sw = dp(SEARCH_W);
        int sh = dp(SEARCH_H);
        HudCardRenderer.drawOverlayCard(context, sx, sy, sw, sh, dp(SEARCH_RADIUS),
                MenuTheme.BG_INPUT, 0.85f * eased);
        HudCardRenderer.drawShaderOutline(context, sx, sy, sw, sh, dp(SEARCH_RADIUS),
                0.6f, 0.55f * eased);
        // Magnifier glyph on the left.
        drawMagnifier(context, sx + dp(12), sy + sh / 2 - dp(4),
                MenuTheme.withAlpha(MenuTheme.TEXT_MUTED | 0xFF000000, eased));
        // Tiny pencil dot on the right.
        int rx = sx + sw - dp(14);
        int ry = sy + sh / 2;
        context.fill(rx, ry - 1, rx + dp(4), ry + dp(2),
                MenuTheme.withAlpha(MenuTheme.TEXT_MUTED | 0xFF000000, eased));
        // Custom placeholder when empty (and not focused), to avoid huge MC default font.
        if (searchField != null && searchField.getText().isEmpty()) {
            int phY = sy + (sh - textHeight(TEXT_SEARCH)) / 2;
            drawScaledText(context, "Поиск", sx + dp(26), phY, TEXT_SEARCH,
                    MenuTheme.withAlpha(MenuTheme.TEXT_MUTED | 0xFF000000, 0.85f * eased));
        }
    }

    private void positionSearchField() {
        if (searchField == null) return;
        int sw = dp(SEARCH_W);
        int sh = dp(SEARCH_H);
        int sx = windowX() + windowW() - dp(PAD_WINDOW_X) - sw;
        int sy = windowY() + dp(PAD_WINDOW_TOP + 24) - (sh - textHeight(TEXT_TAB)) / 2;
        searchField.setX(sx + dp(24));
        searchField.setY(sy + (sh - dp(12)) / 2);
        searchField.setWidth(sw - dp(44));
        searchField.setHeight(dp(12));
    }

    // ---------- Feature grid ----------

    private void renderFeatureGrid(DrawContext context, int mouseX, int mouseY, float eased) {
        int gx = gridX();
        int gy = gridY();
        int gw = gridW();
        int gh = gridH();
        if (gh <= 0) return;

        List<FeatureEntry> visible = visibleFeatures();
        int colW = (gw - dp(CARD_GAP_X) - dp(SCROLLBAR_W + 4)) / 2;
        int cardH = dp(CARD_H);
        int gap = dp(CARD_GAP_Y);
        int rows = (visible.size() + 1) / 2;
        gridContentHeight = rows * (cardH + gap);
        maxScroll = Math.max(0, gridContentHeight - gh);
        if (scroll > maxScroll) scroll = maxScroll;

        hoveredFeature = null;
        hoveredOverToggle = false;

        context.enableScissor(gx, gy, gx + gw, gy + gh);
        for (int i = 0; i < visible.size(); i++) {
            FeatureEntry f = visible.get(i);
            int col = i % 2;
            int row = i / 2;
            f.x = gx + col * (colW + dp(CARD_GAP_X));
            f.y = gy + row * (cardH + gap) - scroll;
            f.width = colW;
            f.height = cardH;
            f.toggleWidth = dp(TOGGLE_W);
            f.toggleHeight = dp(TOGGLE_H);
            f.toggleX = f.x + colW - dp(CARD_PAD_X) - f.toggleWidth;
            f.toggleY = f.y + (cardH - f.toggleHeight) / 2;
            renderCard(context, mouseX, mouseY, f, eased);
        }
        context.disableScissor();

        // Scrollbar
        if (maxScroll > 0) {
            int trackX = gx + gw - dp(SCROLLBAR_W);
            int trackTop = gy + dp(2);
            int trackBot = gy + gh - dp(2);
            int trackH = trackBot - trackTop;
            int thumbH = Math.max(dp(18), (int) ((float) gh / gridContentHeight * trackH));
            int thumbY = trackTop + (int) ((float) scroll / maxScroll * (trackH - thumbH));
            context.fill(trackX, trackTop, trackX + dp(SCROLLBAR_W), trackBot,
                    MenuTheme.withAlpha(MenuTheme.BG_CARD, 0.45f * eased));
            context.fill(trackX, thumbY, trackX + dp(SCROLLBAR_W), thumbY + thumbH,
                    MenuTheme.withAlpha(MenuTheme.ACCENT, 0.55f * eased));
        }

        // If a panel target scrolls out of view, close that side.
        closePanelIfOrphaned(leftPanel, visible);
        closePanelIfOrphaned(rightPanel, visible);
    }

    private void closePanelIfOrphaned(SidePanelState p, List<FeatureEntry> visible) {
        if (p.target == null) return;
        boolean stillListed = visible.contains(p.target);
        int gy = gridY();
        int gh = gridH();
        boolean inGrid = p.target.y + p.target.height > gy && p.target.y < gy + gh;
        if (!stillListed || !inGrid) closePanel(p);
    }

    private void renderCard(DrawContext context, int mouseX, int mouseY, FeatureEntry f, float eased) {
        int gy = gridY();
        int gh = gridH();
        if (f.y + f.height < gy || f.y > gy + gh) {
            f.hoveredLastFrame = false;
            return;
        }
        boolean inGrid = mouseX >= gridX() && mouseX <= gridX() + gridW()
                && mouseY >= gy && mouseY <= gy + gh;
        boolean hovered = inGrid && mouseX >= f.x && mouseX <= f.x + f.width
                && mouseY >= f.y && mouseY <= f.y + f.height;
        if (hovered) {
            hoveredFeature = f;
            hoveredOverToggle = isMouseInsideToggle(f, mouseX, mouseY);
            if (!f.hoveredLastFrame) playHoverSound();
        }
        f.hoveredLastFrame = hovered;

        boolean enabled = f.enabled.get();
        boolean isPanelTarget = leftPanel.target == f || rightPanel.target == f;
        float targetHover = (hovered || isPanelTarget) ? 1.0f : 0.0f;
        f.hoverProgress += (targetHover - f.hoverProgress) * MenuTheme.HOVER_LERP;

        int baseBg = enabled ? MenuTheme.BG_CARD_ACTIVE : MenuTheme.BG_CARD;
        int bg = MenuTheme.lerpColor(baseBg, MenuTheme.BG_CARD_HOVER, f.hoverProgress * 0.55f);
        HudCardRenderer.drawOverlayCard(context, f.x, f.y, f.width, f.height, dp(CARD_RADIUS), bg, 0.82f * eased);
        HudCardRenderer.drawShaderOutline(context, f.x, f.y, f.width, f.height, dp(CARD_RADIUS),
                0.5f, (enabled ? 0.32f : 0.15f) * eased + f.hoverProgress * 0.18f);

        int nameColor = enabled
                ? MenuTheme.withAlpha(MenuTheme.TEXT_PRIMARY, eased)
                : MenuTheme.withAlpha(MenuTheme.TEXT_OFF | 0xFF000000, eased);
        int textY = f.y + (f.height - textHeight(TEXT_CARD)) / 2;
        drawScaledText(context, f.name, f.x + dp(CARD_PAD_X), textY, TEXT_CARD, nameColor);

        f.knobProgress += ((enabled ? 1.0f : 0.0f) - f.knobProgress) * MenuTheme.KNOB_LERP;
        drawAnimatedToggle(context, f.toggleX, f.toggleY, f.toggleWidth, f.toggleHeight, f.knobProgress, eased);
    }

    private boolean isMouseInsideToggle(FeatureEntry f, double mx, double my) {
        int pad = dp(3);
        return mx >= f.toggleX - pad && mx <= f.toggleX + f.toggleWidth + pad
                && my >= f.toggleY - pad && my <= f.toggleY + f.toggleHeight + pad;
    }

    private void drawAnimatedToggle(DrawContext context, int x, int y, int w, int h, float knob, float eased) {
        int trackOff = MenuTheme.BG_CARD;
        int trackOn = MenuTheme.ACCENT;
        int trackColor = MenuTheme.lerpColor(trackOff, trackOn, knob);
        HudCardRenderer.drawOverlayCard(context, x, y, w, h, h / 2.0f, trackColor, 0.88f * eased);
        if (knob > 0.05f) {
            HudCardRenderer.drawShaderOutline(context, x - 1, y - 1, w + 2, h + 2,
                    h / 2.0f + 1, 0.5f, 0.40f * knob * eased);
        }
        int knobSize = h - dp(4);
        if (knobSize < 4) knobSize = h - 2;
        int knobMinX = x + (h - knobSize) / 2;
        int knobMaxX = x + w - knobSize - (h - knobSize) / 2;
        int knobX = Math.round(knobMinX + (knobMaxX - knobMinX) * knob);
        int knobColor = MenuTheme.lerpColor(0xFFBDBECC, 0xFFFFFFFF, knob);
        HudCardRenderer.drawOverlayCard(context, knobX, y + (h - knobSize) / 2,
                knobSize, knobSize, knobSize / 2.0f, knobColor, eased);
    }

    // ---------- Dock ----------

    private void renderDock(DrawContext context, int mouseX, int mouseY, float eased) {
        DockItem[] items = DockItem.values();
        int slotsW = items.length * dp(DOCK_ICON_SLOT);
        int dockH = dp(DOCK_H);
        int dockW = slotsW + dp(20);
        int dockX = windowX() + (windowW() - dockW) / 2;
        int dockY = windowY() + windowH() + dp(DOCK_OFFSET_Y);

        dockSettings.radius = dp(DOCK_RADIUS);
        dockSettings.opacity = 0.90f * eased;
        HudCardRenderer.drawCard(context, dockX, dockY, dockW, dockH, dockSettings);
        HudCardRenderer.drawOverlayCard(context, dockX, dockY, dockW, dockH, dp(DOCK_RADIUS),
                MenuTheme.BG_PANEL_SOFT, 0.30f * eased);
        HudCardRenderer.drawShaderOutline(context, dockX, dockY, dockW, dockH, dp(DOCK_RADIUS),
                0.5f, 0.22f * eased);

        hoveredDockIndex = -1;
        int innerStart = dockX + dp(10);
        for (int i = 0; i < items.length; i++) {
            int slot = dp(DOCK_ICON_SLOT);
            int sx = innerStart + i * slot;
            int sy = dockY + (dockH - slot) / 2;
            int sw = slot;
            int sh = slot;
            boolean hovered = mouseX >= sx && mouseX <= sx + sw && mouseY >= sy && mouseY <= sy + sh;
            if (hovered) hoveredDockIndex = i;
            if (hovered) {
                int pad = dp(5);
                HudCardRenderer.drawOverlayCard(context, sx + pad, sy + pad, sw - pad * 2, sh - pad * 2,
                        dp(9), MenuTheme.BG_CARD_HOVER, 0.70f * eased);
            }
            int iconColor = hovered
                    ? MenuTheme.withAlpha(MenuTheme.TEXT_PRIMARY, eased)
                    : MenuTheme.withAlpha(MenuTheme.TEXT_SECONDARY | 0xFF000000, 0.85f * eased);
            drawDockIcon(context, items[i], sx + sw / 2, sy + sh / 2, iconColor);
        }
    }

    // ---------- Side panel ----------

    private void advanceSidePanel(SidePanelState p) {
        float target = p.target != null ? 1.0f : 0.0f;
        p.slide += (target - p.slide) * MenuTheme.SIDE_SLIDE_LERP;
    }

    private void renderSidePanel(DrawContext context, int mouseX, int mouseY, float eased, SidePanelState p) {
        if (p.target == null && p.slide < 0.02f) return;
        if (p.target == null) {
            // Fading out — still render with current slide.
        }
        float sideEased = easeOutCubic(Math.max(0.0f, Math.min(1.0f, p.slide))) * eased;
        if (sideEased < 0.02f) return;

        int sw = dp(SIDE_PANEL_W);
        int rowH = dp(SIDE_ROW_H);
        int padBottom = dp(14);
        int titleArea = dp(46);
        int desiredH = titleArea + Math.max(1, p.entries.size()) * rowH + padBottom;
        int maxH = (int) (height * 0.78);
        p.height = Math.min(desiredH, maxH);

        int wx = windowX();
        int ww = windowW();
        int gap = dp(14);
        int slideOffset = (int) ((1.0f - sideEased) * dp(14));
        int sx;
        if (p.side == Side.RIGHT) {
            sx = wx + ww + gap + slideOffset;
            if (sx + sw > width - dp(8)) sx = width - sw - dp(8);
        } else {
            sx = wx - sw - gap - slideOffset;
            if (sx < dp(8)) sx = dp(8);
        }
        int targetY = (p.target != null ? p.target.y : p.lastY) + (p.target != null ? p.target.height : p.lastH) / 2 - p.height / 2;
        targetY = Math.max(windowY(), Math.min(windowY() + windowH() - p.height, targetY));
        targetY = Math.max(dp(12), Math.min(height - p.height - dp(12), targetY));
        int sy = targetY;
        p.x = sx; p.y = sy; p.width = sw;
        if (p.target != null) { p.lastY = p.target.y; p.lastH = p.target.height; }

        sideSettings.radius = dp(SIDE_PANEL_RADIUS);
        sideSettings.opacity = 0.95f * sideEased;
        HudCardRenderer.drawCard(context, sx, sy, sw, p.height, sideSettings);
        HudCardRenderer.drawOverlayCard(context, sx, sy, sw, dp(58), dp(SIDE_PANEL_RADIUS),
                MenuTheme.PURPLE_DEEP, 0.50f * sideEased);
        HudCardRenderer.drawOverlayCard(context, sx, sy, sw, p.height, dp(SIDE_PANEL_RADIUS),
                MenuTheme.BG_PANEL, 0.22f * sideEased);
        HudCardRenderer.drawShaderOutline(context, sx, sy, sw, p.height, dp(SIDE_PANEL_RADIUS),
                0.55f, 0.30f * sideEased);

        FeatureEntry titleSrc = p.target != null ? p.target : null;
        String title = titleSrc != null ? titleSrc.name : "";
        String status = titleSrc != null ? (titleSrc.enabled.get() ? "Enabled" : "Disabled") : "";
        int titleX = sx + dp(16);
        drawScaledText(context, title, titleX, sy + dp(14), TEXT_SETTING_TITLE,
                MenuTheme.withAlpha(MenuTheme.TEXT_PRIMARY, sideEased));
        int statusColor = (titleSrc != null && titleSrc.enabled.get())
                ? MenuTheme.ACCENT_BRIGHT
                : (MenuTheme.TEXT_MUTED | 0xFF000000);
        drawScaledText(context, status, titleX, sy + dp(28), TEXT_SETTING_STATUS,
                MenuTheme.withAlpha(statusColor, 0.95f * sideEased));

        // separator
        context.fill(sx + dp(14), sy + titleArea - dp(4), sx + sw - dp(14), sy + titleArea - dp(3),
                MenuTheme.withAlpha(MenuTheme.BORDER_SUBTLE | 0xFF000000, 0.6f * sideEased));

        int rowsTop = sy + titleArea;
        int rowsBot = sy + p.height - padBottom;
        int listH = rowsBot - rowsTop;
        int contentH = p.entries.size() * rowH;
        int maxScroll = Math.max(0, contentH - listH);
        if (p.scroll > maxScroll) p.scroll = maxScroll;
        p.hoveredEntry = null;

        context.enableScissor(sx + dp(4), rowsTop, sx + sw - dp(4), rowsBot);
        for (int i = 0; i < p.entries.size(); i++) {
            SettingEntry e = p.entries.get(i);
            int rowY = rowsTop + i * rowH - p.scroll;
            e.x = sx + dp(14);
            e.y = rowY;
            e.width = sw - dp(28);
            e.height = rowH - dp(4);
            boolean rowVisible = rowY + rowH > rowsTop && rowY < rowsBot;
            if (e.input != null) {
                e.input.setX(sx + sw - dp(58));
                e.input.setY(rowY + (rowH - dp(11)) / 2);
                e.input.setWidth(dp(44));
                e.input.setHeight(dp(11));
                e.input.visible = rowVisible;
            }
            if (!rowVisible) continue;
            boolean rowHovered = mouseX >= e.x && mouseX <= e.x + e.width
                    && mouseY >= rowY && mouseY <= rowY + e.height;
            if (rowHovered) p.hoveredEntry = e;

            String label = humanize(e.field.getName());
            int labelY = rowY + (rowH - textHeight(TEXT_SETTING_LABEL)) / 2;
            drawScaledText(context, label, e.x, labelY, TEXT_SETTING_LABEL,
                    MenuTheme.withAlpha(MenuTheme.TEXT_SECONDARY | 0xFF000000, 0.92f * sideEased));

            Class<?> type = e.field.getType();
            if (type == boolean.class) {
                boolean v = readBoolean(p.target.config, e.field);
                e.knobProgress += ((v ? 1.0f : 0.0f) - e.knobProgress) * MenuTheme.KNOB_LERP;
                int tw = dp(24);
                int th = dp(12);
                int tx = sx + sw - dp(14) - tw;
                int ty = rowY + (rowH - th) / 2;
                drawAnimatedToggle(context, tx, ty, tw, th, e.knobProgress, sideEased);
            } else if (isColorField(e.field)) {
                int sxColor = sx + sw - dp(34);
                int syColor = rowY + (rowH - dp(12)) / 2;
                int swatch = readColor(p.target.config, e.field);
                context.fill(sxColor - 1, syColor - 1, sxColor + dp(20) + 1, syColor + dp(12) + 1,
                        MenuTheme.withAlpha(MenuTheme.BG_DEEP, sideEased));
                context.fill(sxColor, syColor, sxColor + dp(20), syColor + dp(12),
                        0xFF000000 | (swatch & 0x00FFFFFF));
            }
        }
        context.disableScissor();

        if (maxScroll > 0) {
            int trackX = sx + sw - dp(3);
            int trackTop = rowsTop;
            int trackBot = rowsBot;
            int trackH = trackBot - trackTop;
            int thumbH = Math.max(dp(14), (int) ((float) listH / contentH * trackH));
            int thumbY = trackTop + (int) ((float) p.scroll / maxScroll * (trackH - thumbH));
            context.fill(trackX, trackTop, trackX + dp(2), trackBot,
                    MenuTheme.withAlpha(MenuTheme.BG_CARD, 0.50f * sideEased));
            context.fill(trackX, thumbY, trackX + dp(2), thumbY + thumbH,
                    MenuTheme.withAlpha(MenuTheme.ACCENT, 0.55f * sideEased));
        }
    }

    private void positionSidePanelInputs(SidePanelState p) {
        // Layout occurs in renderSidePanel; nothing to do here unless target was just removed.
        if (p.target == null) {
            for (SettingEntry e : p.entries) {
                if (e.input != null) e.input.visible = false;
            }
        }
    }

    // ---------- Mouse / interaction ----------

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        int button = click.button();
        double mx = click.x();
        double my = click.y();

        if (searchField != null && searchField.mouseClicked(click, doubled)) return true;
        if (super.mouseClicked(click, doubled)) return true;

        // Dock buttons.
        if (hoveredDockIndex >= 0) {
            DockItem item = DockItem.values()[hoveredDockIndex];
            handleDockClick(item);
            return true;
        }

        // Top tabs.
        if (hoveredCategoryIndex >= 0) {
            List<Category> visible = visibleCategories();
            selectedCategory = visible.get(hoveredCategoryIndex);
            if (searchField != null) searchField.setText("");
            searchQuery = "";
            scroll = 0;
            closePanel(leftPanel);
            closePanel(rightPanel);
            playToggleSound();
            return true;
        }

        // Feature cards.
        if (hoveredFeature != null) {
            if (button == 0) {
                if (hoveredOverToggle) {
                    hoveredFeature.enabledSetter.accept(!hoveredFeature.enabled.get());
                    playToggleSound();
                    saveAndReload();
                } else {
                    // LMB outside the toggle hitbox should not toggle (per spec).
                }
                return true;
            }
            if (button == 1) {
                Side side = sideFor(hoveredFeature);
                SidePanelState panel = panels.get(side);
                if (panel.target == hoveredFeature) {
                    closePanel(panel);
                } else {
                    openPanel(panel, hoveredFeature);
                }
                playHoverSound();
                return true;
            }
        }

        // Click inside an open side panel (toggle / color swatch).
        for (SidePanelState p : panels.values()) {
            if (p.target == null) continue;
            if (!isInsideSidePanel(p, mx, my)) continue;
            SettingEntry e = settingAt(p, mx, my);
            if (e != null && button == 0) {
                if (e.field.getType() == boolean.class) {
                    writeBoolean(p.target.config, e.field, !readBoolean(p.target.config, e.field));
                    playToggleSound();
                    saveAndReload();
                    return true;
                }
            }
            return true; // click landed in panel; consume
        }

        // Click elsewhere — close both panels.
        closePanel(leftPanel);
        closePanel(rightPanel);
        return false;
    }

    private boolean isInsideSidePanel(SidePanelState p, double mx, double my) {
        return mx >= p.x && mx <= p.x + p.width && my >= p.y && my <= p.y + p.height;
    }

    private SettingEntry settingAt(SidePanelState p, double mx, double my) {
        for (SettingEntry e : p.entries) {
            if (mx >= e.x && mx <= e.x + e.width && my >= e.y && my <= e.y + e.height) return e;
        }
        return null;
    }

    private Side sideFor(FeatureEntry f) {
        int gridMid = gridX() + gridW() / 2;
        return f.x + f.width / 2 < gridMid ? Side.LEFT : Side.RIGHT;
    }

    private void openPanel(SidePanelState p, FeatureEntry target) {
        closePanel(p);
        p.target = target;
        p.slide = 0.0f;
        p.scroll = 0;
        p.lastY = target.y;
        p.lastH = target.height;
        rebuildPanelSettings(p);
    }

    private void closePanel(SidePanelState p) {
        for (SettingEntry e : p.entries) {
            if (e.input != null) remove(e.input);
        }
        p.entries.clear();
        p.target = null;
        p.slide = 0.0f;
    }

    private void rebuildPanelSettings(SidePanelState p) {
        if (p.target == null || p.target.config == null) return;
        for (Field field : p.target.config.getClass().getFields()) {
            if (Modifier.isStatic(field.getModifiers()) || !isEditable(field)) continue;
            TextFieldWidget input = null;
            Class<?> type = field.getType();
            if (type != boolean.class && !isColorField(field)) {
                input = new TextFieldWidget(textRenderer, 0, 0, 50, 11, Text.literal(field.getName()));
                input.setDrawsBackground(false);
                input.setEditableColor(MenuTheme.TEXT_PRIMARY);
                input.setMaxLength(48);
                input.setText(readFieldAsText(p.target.config, field));
                final Object cfg = p.target.config;
                final Field f = field;
                input.setChangedListener(v -> writeFieldFromText(cfg, f, v));
                addDrawableChild(input);
            }
            p.entries.add(new SettingEntry(field, input));
        }
    }

    private void handleDockClick(DockItem item) {
        switch (item) {
            case SETTINGS -> { /* no-op: visual anchor */ }
            case THEME -> { /* future: theme switcher */ }
            case KEYBINDS -> { if (client != null) client.setScreen(new MultiKeyBindingsScreen(this)); }
            case PROFILES -> { if (client != null) client.setScreen(new MarkersScreen()); }
            case CONFIG -> Util.getOperatingSystem().open(VibeVisualsConfigManager.getConfigPath().toFile());
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        for (SidePanelState p : panels.values()) {
            if (p.target != null && isInsideSidePanel(p, mouseX, mouseY)) {
                p.scroll = Math.max(0, p.scroll - (int) Math.round(verticalAmount * 16.0));
                return true;
            }
        }
        if (mouseX >= gridX() && mouseX <= gridX() + gridW()
                && mouseY >= gridY() && mouseY <= gridY() + gridH()) {
            scroll = Math.max(0, Math.min(maxScroll, scroll - (int) Math.round(verticalAmount * 18.0)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void close() {
        saveAndReload();
        if (client != null) client.setScreen(null);
    }

    @Override
    public boolean shouldPause() { return false; }

    // ---------- Reflection helpers ----------

    private static boolean isEditable(Field f) {
        Class<?> t = f.getType();
        return t == int.class || t == float.class || t == boolean.class || t == String.class
                || t == HudCardRenderType.class;
    }

    private static boolean isColorField(Field f) {
        return f.getType() == int.class && f.getName().toLowerCase(Locale.ROOT).contains("color");
    }

    private static int readColor(Object cfg, Field f) {
        try { return f.getInt(cfg); } catch (IllegalAccessException e) { return 0xFFFFFFFF; }
    }
    private static boolean readBoolean(Object cfg, Field f) {
        try { return f.getBoolean(cfg); } catch (IllegalAccessException e) { return false; }
    }
    private static void writeBoolean(Object cfg, Field f, boolean v) {
        try { f.setBoolean(cfg, v); } catch (IllegalAccessException ignored) {}
    }
    private static String readFieldAsText(Object cfg, Field f) {
        try { Object v = f.get(cfg); return v == null ? "" : v.toString(); }
        catch (IllegalAccessException e) { return ""; }
    }
    private static void writeFieldFromText(Object cfg, Field f, String v) {
        try {
            Class<?> t = f.getType();
            if (t == int.class) f.setInt(cfg, Integer.parseInt(v.trim()));
            else if (t == float.class) f.setFloat(cfg, Float.parseFloat(v.trim()));
            else if (t == boolean.class) f.setBoolean(cfg, Boolean.parseBoolean(v.trim()));
            else if (t == HudCardRenderType.class) f.set(cfg, HudCardRenderType.valueOf(v.trim().toUpperCase(Locale.ROOT)));
            else if (t == String.class) f.set(cfg, v);
            saveAndReload();
        } catch (IllegalArgumentException | IllegalAccessException ignored) {}
    }

    private static void saveAndReload() {
        VibeVisualsConfigManager.get().validate();
        VibeVisualsConfigManager.save();
        HudManager.reload();
    }

    private static String humanize(String name) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (i > 0 && Character.isUpperCase(c)) b.append(' ');
            b.append(i == 0 ? Character.toUpperCase(c) : c);
        }
        return b.toString();
    }

    private static float easeOutCubic(float t) {
        float p = 1.0f - t;
        return 1.0f - p * p * p;
    }

    private void playHoverSound() {
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK.value(), 1.18f, 0.05f));
    }
    private void playToggleSound() {
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK.value(), 1.25f, 0.16f));
    }

    // ---------- Vector glyphs ----------

    private void drawPulseLogo(DrawContext context, int x, int y, int size, int color) {
        // Stylised ECG pulse, scales with `size`.
        int s = Math.max(8, size);
        int unit = Math.max(1, s / 10);
        int baseY = y + s / 2;
        // baseline
        context.fill(x, baseY, x + 2 * unit, baseY + unit, color);
        // up spike
        context.fill(x + 2 * unit, baseY - 3 * unit, x + 3 * unit, baseY + unit, color);
        // tall peak
        context.fill(x + 3 * unit, baseY - 5 * unit, x + 4 * unit, baseY + 2 * unit, color);
        // down spike
        context.fill(x + 4 * unit, baseY + unit, x + 5 * unit, baseY + 3 * unit, color);
        // recovery
        context.fill(x + 5 * unit, baseY, x + 7 * unit, baseY + unit, color);
        // tail
        context.fill(x + 7 * unit, baseY, x + s, baseY + unit, color);
    }

    private void drawMagnifier(DrawContext context, int x, int y, int color) {
        // 8x8 circle outline + handle
        int u = Math.max(1, dp(1));
        context.fill(x + u, y, x + 5 * u, y + u, color);
        context.fill(x, y + u, x + u, y + 5 * u, color);
        context.fill(x + 5 * u, y + u, x + 6 * u, y + 5 * u, color);
        context.fill(x + u, y + 5 * u, x + 5 * u, y + 6 * u, color);
        context.fill(x + 5 * u, y + 5 * u, x + 7 * u, y + 7 * u, color);
    }

    private void drawDockIcon(DrawContext context, DockItem item, int cx, int cy, int color) {
        int u = Math.max(1, dp(1.4f));
        switch (item) {
            case SETTINGS -> {
                // gear-ish: small plus + corner dots
                context.fill(cx - u, cy - 3 * u, cx + u, cy + 3 * u, color);
                context.fill(cx - 3 * u, cy - u, cx + 3 * u, cy + u, color);
                context.fill(cx - 3 * u, cy - 3 * u, cx - 2 * u, cy - 2 * u, color);
                context.fill(cx + 2 * u, cy + 2 * u, cx + 3 * u, cy + 3 * u, color);
                context.fill(cx - 3 * u, cy + 2 * u, cx - 2 * u, cy + 3 * u, color);
                context.fill(cx + 2 * u, cy - 3 * u, cx + 3 * u, cy - 2 * u, color);
            }
            case THEME -> {
                // window-ish: outlined square with header bar
                context.fill(cx - 4 * u, cy - 3 * u, cx + 4 * u, cy - 2 * u, color);
                context.fill(cx - 4 * u, cy - 3 * u, cx - 3 * u, cy + 3 * u, color);
                context.fill(cx + 3 * u, cy - 3 * u, cx + 4 * u, cy + 3 * u, color);
                context.fill(cx - 4 * u, cy + 2 * u, cx + 4 * u, cy + 3 * u, color);
            }
            case KEYBINDS -> {
                context.fill(cx - 4 * u, cy - 2 * u, cx + 4 * u, cy + 2 * u, color);
                for (int i = -3; i <= 3; i += 2) {
                    context.fill(cx + i * u, cy - u, cx + (i + 1) * u, cy, color);
                    context.fill(cx + i * u, cy + u, cx + (i + 1) * u, cy + 2 * u, color);
                }
            }
            case PROFILES -> {
                // person glyph
                context.fill(cx - u, cy - 3 * u, cx + u, cy - u, color);
                context.fill(cx - 3 * u, cy, cx + 3 * u, cy + 3 * u, color);
            }
            case CONFIG -> {
                // folder
                context.fill(cx - 4 * u, cy - 2 * u, cx, cy - u, color);
                context.fill(cx - 4 * u, cy - u, cx + 4 * u, cy + 3 * u, color);
            }
        }
    }

    // ---------- Inner types ----------

    private enum Category {
        VISUALS("Visuals"),
        HUD("HUD"),
        UTILITIES("Utilities"),
        PVP("PvP"),
        MENU("Menu");

        final String label;
        Category(String label) { this.label = label; }
    }

    private enum Side { LEFT, RIGHT }

    private enum DockItem { SETTINGS, THEME, KEYBINDS, PROFILES, CONFIG }

    private static class FeatureEntry {
        final Category category;
        final String name;
        final Supplier<Boolean> enabled;
        final Consumer<Boolean> enabledSetter;
        final Object config;
        float hoverProgress;
        float knobProgress;
        boolean hoveredLastFrame;
        int x, y, width, height;
        int toggleX, toggleY, toggleWidth, toggleHeight;

        FeatureEntry(Category category, String name, Supplier<Boolean> enabled,
                     Consumer<Boolean> enabledSetter, Object config) {
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
        int x, y, width, height;

        SettingEntry(Field field, TextFieldWidget input) {
            this.field = field;
            this.input = input;
        }
    }

    private static class SidePanelState {
        final Side side;
        final List<SettingEntry> entries = new ArrayList<>();
        FeatureEntry target;
        float slide;
        int scroll;
        int x, y, width, height;
        int lastY, lastH;
        SettingEntry hoveredEntry;

        SidePanelState(Side side) { this.side = side; }
    }
}
