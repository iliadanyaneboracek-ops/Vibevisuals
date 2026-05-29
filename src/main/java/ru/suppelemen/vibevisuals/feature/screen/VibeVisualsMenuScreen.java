package ru.suppelemen.vibevisuals.feature.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import ru.suppelemen.vibevisuals.VibeVisualsClient;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;
import ru.suppelemen.vibevisuals.core.hud.HudManager;
import ru.suppelemen.vibevisuals.theme.HudCardRenderType;
import ru.suppelemen.vibevisuals.theme.MenuTheme;
import ru.suppelemen.vibevisuals.util.font.SmoothText;
import ru.suppelemen.vibevisuals.util.render.HudCardRenderer;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Liquid-glass ClickGUI with drill-down navigation.
 *
 * Layout:
 *   ┌──────────────────────────────────┐
 *   │ <   vibevisuals                  │
 *   ├──────────┬───────────────────────┤
 *   │ Visuals  │  Sky Color       [⏵ ●]│   ← list view
 *   │ HUD      │  Fog Color       [⏵ ●]│
 *   │ Utilities│  ...                  │
 *   │ PvP      │                       │
 *   │ Menu     │                       │
 *   └──────────┴───────────────────────┘
 *
 * Clicking a module row drills into a settings page that slides in from the
 * right.  A < Back chevron at the top of that page returns to the list.
 */
public class VibeVisualsMenuScreen extends Screen {

    // Reference frame.
    private static final float REFERENCE_WIDTH = 1920.0f;
    private static final float REFERENCE_HEIGHT = 1080.0f;

    // Typography (design px).
    private static final float TEXT_TITLE = 14.0f;
    private static final float TEXT_BRAND = 18.0f;
    private static final float TEXT_BRAND_V = 22.0f;
    private static final float TEXT_CATEGORY = 13.0f;
    /** Section headers (MODULES, SYSTEM) — tiny captions, ~half of body. */
    private static final float TEXT_SECTION = 3.5f;
    private static final float TEXT_ROW = 12.5f;
    private static final float TEXT_ROW_SUB = 8.5f;
    private static final float TEXT_DETAIL_TITLE = 18.0f;
    private static final float TEXT_DETAIL_LABEL = 11.5f;
    private static final float TEXT_BACK = 11.5f;

    // Brand block above the panel.
    private static final int BRAND_ICON = 60;
    private static final int BRAND_GAP_X = 12;
    private static final int BRAND_BOTTOM_GAP = 16;

    // Panel layout (design px).
    private static final int PANEL_W = 624;
    private static final int PANEL_H = 384;
    private static final int PANEL_RADIUS = 24;
    private static final int HEADER_H = 44;
    private static final int SIDEBAR_W = 168;
    private static final int SIDEBAR_ROW_H = 24;
    private static final int SIDEBAR_ROW_RADIUS = 8;
    private static final int SIDEBAR_PILL_PAD = 8;
    private static final int SECTION_HEADER_GAP = 22;
    private static final int CATEGORY_ICON = 16;
    private static final int CATEGORY_ICON_GAP = 10;
    private static final int PAD_X = 18;

    // Module list rows (right side).
    private static final int ROW_H = 46;
    private static final int ROW_GAP = 1;
    private static final int ROW_RADIUS = 10;
    private static final int ROW_PAD_X = 14;

    // Apple-style switch — iOS-ish 1.78:1 ratio, large enough that the SDF
    // rounded-box shader has room to anti-alias the corners cleanly.
    private static final int SWITCH_W = 34;
    private static final int SWITCH_H = 19;

    // Search bar (top of right pane).
    private static final int SEARCH_H = 28;
    private static final int SEARCH_RADIUS = 12;
    private static final int SEARCH_PAD_TOP = 10;
    private static final int SEARCH_PAD_BOT = 10;
    private static final int SEARCH_PAD_X = 12;
    private static final float TEXT_SEARCH = 11.0f;

    // Separator opacities.
    private static final float SEPARATOR_ALPHA_HEADER = 0.04f;
    private static final float SEPARATOR_ALPHA_SIDEBAR = 0.06f;
    private static final float SECTION_HEADER_ALPHA = 0.35f;

    private final List<FeatureEntry> features = new ArrayList<>();
    private final List<SettingRow> detailSettings = new ArrayList<>();

    private long openedAtMs;

    private Category selected = Category.VISUALS;
    private int hoveredCategoryIndex = -1;
    private boolean hoveredBack;

    private FeatureEntry hoveredRow;
    private boolean hoveredOnSwitch;
    private FeatureEntry detailTarget;       // null → list view
    private float detailSlide;               // 0..1 — animates list↔detail transition
    private boolean hoveredDetailBack;
    private SettingRow hoveredSettingRow;

    private int contentScroll;
    private int contentMaxScroll;

    /** Setting row currently being dragged via slider thumb (null when idle). */
    private SettingRow draggingSlider;

    /** Index of the theme tile currently hovered in the Themes detail view. */
    private int hoveredThemeIndex = -1;

    /** Top-level view: outer rail on the left switches between these. */
    private enum AppView { SETTINGS, CONFIGURATIONS }
    private AppView currentView = AppView.SETTINGS;
    private int hoveredRailIndex = -1;

    /** App-rail (left-most strip) layout — compact dock, +15 % over baseline. */
    private static final int RAIL_W = 39;
    private static final int RAIL_GAP = 10;        // gap from main panel
    private static final int RAIL_BTN = 30;
    private static final int RAIL_BTN_RADIUS = 8;
    private static final int RAIL_RADIUS = 14;
    private static final int RAIL_PAD_Y = 5;
    private static final int RAIL_BTN_GAP = 5;
    private static final int RAIL_ICON = 15;

    // Search bar state.
    private String searchQuery = "";
    private boolean searchFocused;
    private int searchX, searchY, searchW, searchH;
    private boolean hoveredSearchClear;
    private boolean hoveredSearchBox;
    private float searchFocusAnim; // 0..1 — animates the focus ring

    public VibeVisualsMenuScreen() {
        super(Text.literal("VibeVisuals"));
    }

    @Override
    protected void init() {
        MenuTheme.applyTheme(VibeVisualsConfigManager.get().menu.theme);
        rebuildFeatures();
        openedAtMs = System.currentTimeMillis();
    }

    private void rebuildFeatures() {
        VibeVisualsConfig c = VibeVisualsConfigManager.get();
        features.clear();
        features.add(new FeatureEntry(Category.HUD, "Potions",        "Active potion effects",       "Активные эффекты зелий", ModuleIcon.POTION,
                () -> c.potionsCard.enabled,    v -> c.potionsCard.enabled = v, c.potionsCard));
        features.add(new FeatureEntry(Category.HUD, "Cooldowns",      "Ability cooldown timers",     "Таймеры перезарядок умений", ModuleIcon.CLOCK,
                () -> c.cooldownsCard.enabled,  v -> c.cooldownsCard.enabled = v, c.cooldownsCard));
        features.add(new FeatureEntry(Category.HUD, "Hot Keys",       "On-screen keybind hints",     "Подсказки клавиш на экране", ModuleIcon.KEY,
                () -> c.hotKeysCard.enabled,    v -> c.hotKeysCard.enabled = v, c.hotKeysCard));
        features.add(new FeatureEntry(Category.HUD, "Top Bar",        "Stats bar across the top",    "Полоса статов сверху", ModuleIcon.BAR,
                () -> c.topBar.enabled,         v -> c.topBar.enabled = v, c.topBar));
        features.add(new FeatureEntry(Category.HUD, "Inventory HUD",  "Inventory preview overlay",   "Превью инвентаря на HUD", ModuleIcon.GRID,
                () -> c.inventoryHud.enabled,   v -> c.inventoryHud.enabled = v, c.inventoryHud));
        features.add(new FeatureEntry(Category.HUD, "Armor HUD",      "Armor durability + pieces",   "Прочность и список брони", ModuleIcon.SHIELD,
                () -> c.armorHud.enabled,       v -> c.armorHud.enabled = v, c.armorHud));
        features.add(new FeatureEntry(Category.HUD, "Custom Hotbar",  "Sleek replacement hotbar",    "Свой минималистичный хотбар", ModuleIcon.HOTBAR,
                () -> c.hotbar.enabled,         v -> c.hotbar.enabled = v, c.hotbar));
        features.add(new FeatureEntry(Category.HUD, "Healing Helper", "Highlights best heal item",   "Подсветка лучшей лечилки", ModuleIcon.HEART,
                () -> c.healingHelper.enabled,  v -> c.healingHelper.enabled = v, c.healingHelper));
        features.add(new FeatureEntry(Category.HUD, "Slot Timers",    "Per-slot use cooldowns",      "Таймеры по слотам хотбара", ModuleIcon.CLOCK,
                () -> c.slotTimers.enabled,     v -> c.slotTimers.enabled = v, c.slotTimers));
        features.add(new FeatureEntry(Category.PVP, "PvP Combat",     "Combat status + opponent info","Состояние боя и инфа о цели", ModuleIcon.SWORD,
                () -> c.pvpCard.enabled,        v -> c.pvpCard.enabled = v, c.pvpCard));
        features.add(new FeatureEntry(Category.PVP, "Mogged",         "Banner + sound when you mog", "Баннер и звук после хита", ModuleIcon.CROWN,
                () -> c.mogged.enabled,         v -> c.mogged.enabled = v, c.mogged));
        features.add(new FeatureEntry(Category.PVP, "Target ESP",     "Outline ring around target",  "Кольцо-обводка вокруг цели", ModuleIcon.TARGET,
                () -> c.targetEsp.enabled,      v -> c.targetEsp.enabled = v, c.targetEsp));
        features.add(new FeatureEntry(Category.PVP, "Saturation",     "Show hunger saturation",      "Показ скрытой сатурации", ModuleIcon.DROP,
                () -> c.saturationDisplay.enabled, v -> c.saturationDisplay.enabled = v, c.saturationDisplay));
        features.add(new FeatureEntry(Category.PVP, "Crit Hit Sound", "Custom crit sound on hit",    "Свой звук удара по криту", ModuleIcon.SPARK,
                () -> c.customHitSound.enabled, v -> c.customHitSound.enabled = v, c.customHitSound));
        features.add(new FeatureEntry(Category.PVP, "Shift Up",       "Snap shift after crit",       "Авто-шифт после крита", ModuleIcon.ARROW_UP,
                () -> c.shiftUp.enabled,        v -> c.shiftUp.enabled = v, c.shiftUp));
        features.add(new FeatureEntry(Category.VISUALS, "Sky Color",   "Tint sky by biome",          "Окраска неба по биому", ModuleIcon.CLOUD,
                () -> c.visualEffects.skyColorEnabled,        v -> c.visualEffects.skyColorEnabled = v, c.visualEffects));
        features.add(new FeatureEntry(Category.VISUALS, "Fog Color",   "Recolor fog",                "Перекраска тумана", ModuleIcon.FOG,
                () -> c.visualEffects.fogColorEnabled,        v -> c.visualEffects.fogColorEnabled = v, c.visualEffects));
        features.add(new FeatureEntry(Category.VISUALS, "Particles",   "Custom ambient particles",   "Свои фоновые частицы", ModuleIcon.SPARK,
                () -> c.visualEffects.customParticlesEnabled, v -> c.visualEffects.customParticlesEnabled = v, c.visualEffects));
        features.add(new FeatureEntry(Category.VISUALS, "Screen Fire", "Fire overlay when burning",  "Огненный оверлей при горении", ModuleIcon.FLAME,
                () -> c.fireOverlay.enabled,                  v -> c.fireOverlay.enabled = v, c.fireOverlay));
        features.add(new FeatureEntry(Category.VISUALS, "Crosshair",   "Custom crosshair style",     "Свой стиль прицела", ModuleIcon.CROSSHAIR,
                () -> c.customCrosshair.enabled,              v -> c.customCrosshair.enabled = v, c.customCrosshair));
        features.add(new FeatureEntry(Category.VISUALS, "Custom Hand", "Reposition first-person hand","Положение руки от первого лица", ModuleIcon.HAND,
                () -> c.customHand.enabled,                   v -> c.customHand.enabled = v, c.customHand));
        features.add(new FeatureEntry(Category.UTILITIES, "Projectile Path", "Predict arrow trajectory","Прогноз траектории снарядов", ModuleIcon.CURVE,
                () -> c.projectilePrediction.enabled, v -> c.projectilePrediction.enabled = v, c.projectilePrediction));
        features.add(new FeatureEntry(Category.UTILITIES, "HUD Animations",  "Smooth HUD transitions","Плавные анимации HUD", ModuleIcon.WAVE,
                () -> c.hudAnimations.enabled,        v -> c.hudAnimations.enabled = v, c.hudAnimations));
        features.add(new FeatureEntry(Category.UTILITIES, "Markers",         "World-space waypoints", "Маркеры в мире", ModuleIcon.PIN,
                () -> c.markers.enabled,              v -> c.markers.enabled = v, c.markers));
        features.add(new FeatureEntry(Category.UTILITIES, "AutoEat",         "Auto-eat when low hunger","Автоматическая еда", ModuleIcon.APPLE,
                () -> c.autoEat.enabled,              v -> c.autoEat.enabled = v, c.autoEat));
        features.add(new FeatureEntry(Category.UTILITIES, "AutoPotion",      "Auto-drink potions",    "Автоматические зелья", ModuleIcon.POTION,
                () -> c.autoPotion.enabled,           v -> c.autoPotion.enabled = v, c.autoPotion));
        features.add(new FeatureEntry(Category.UTILITIES, "AutoRespawn",     "Skip respawn screen",   "Авто-респавн", ModuleIcon.REFRESH,
                () -> c.autoRespawn.enabled,          v -> c.autoRespawn.enabled = v, c.autoRespawn));
        features.add(new FeatureEntry(Category.UTILITIES, "Tape Mouse",      "Hold-to-attack tape",   "Залипание клавиши мыши", ModuleIcon.LINK,
                () -> c.tapeMouse.enabled,            v -> c.tapeMouse.enabled = v, c.tapeMouse));
        features.add(new FeatureEntry(Category.UTILITIES, "Pickup Logger",   "Log items you pick up", "Логирование подбора предметов", ModuleIcon.GRID,
                () -> c.itemPickupLogger.enabled,     v -> c.itemPickupLogger.enabled = v, c.itemPickupLogger));
        features.add(new FeatureEntry(Category.UTILITIES, "Lock Slot",       "Lock hotbar slots from drop/click", "Защита слотов хотбара", ModuleIcon.LINK,
                () -> c.lockSlot.enabled,             v -> c.lockSlot.enabled = v, c.lockSlot));
        features.add(new FeatureEntry(Category.UTILITIES, "Full Bright",    "Max brightness everywhere","Полная яркость везде", ModuleIcon.SUN,
                () -> c.fullBrightStrength > 0.0f,
                v -> c.fullBrightStrength = v ? Math.max(0.6f, c.fullBrightStrength) : 0.0f, c));
        features.add(new FeatureEntry(Category.MENU, "Liquid Glass Blur", "Frosted background blur", "Размытие фона как у стекла", ModuleIcon.BLUR,
                () -> c.menu.liquidGlassBlur, v -> c.menu.liquidGlassBlur = v, c.menu));
        // Light Theme was removed — single dark palette is the canonical look.
        // Themes — drill-down detail (handled by renderDetail's special branch).
        features.add(new FeatureEntry(Category.MENU, "Themes",            "Pick an accent palette", "Выбор цвета акцентов", ModuleIcon.THEME,
                () -> false, v -> {}, null));
        // (The old "Profiles" SYSTEM row was removed — profile sharing now
        //  lives in the dedicated CONFIGURATIONS view on the app-rail.)
    }

    // ---------- App-rail (outer view switcher) ----------

    /** Per-view hover animation progress (0..1). Same length as {@link AppView}. */
    private final float[] railHoverAnim = new float[AppView.values().length];

    /** Compact vertical icon dock to the left of the main panel.
     *
     *  Tone-down pass: no accent colour, no neon. Active state is a quietly
     *  elevated glass card with a brighter icon — same vocabulary the main
     *  panel's category sidebar uses, so the rail feels like part of the
     *  same UI rather than a foreign control.
     *
     *  Per-button states (lerp-animated):
     *   - inactive: subtle outline ring, dim neutral icon
     *   - hover  : ring brightens + faint glass tint + brighter icon
     *   - active : elevated glass card + primary-coloured icon (no fill colour)
     *
     *  Architecture supports N items — append to {@link AppView} and route in
     *  {@link #drawAppRailIcon}; layout reflows automatically.
     */
    private void renderAppRail(DrawContext ctx, int mx, int my, float eased,
                                int panelX, int panelY, int panelH) {
        int btn = dp(RAIL_BTN);
        int gap = dp(RAIL_BTN_GAP);
        int padY = dp(RAIL_PAD_Y);
        int count = AppView.values().length;
        int totalH = padY * 2 + btn * count + gap * (count - 1);
        int railW = dp(RAIL_W);
        int railX = panelX - dp(RAIL_GAP) - railW;
        int railY = panelY + (panelH - totalH) / 2;

        // Rail host — 9-slice texture card (NOT the SDF shader). The shader's
        // SDF AA is tuned for ~square aspect ratios; the rail is much taller
        // than wide (aspect ~0.5) and that produces visible corner stepping.
        // The 9-slice path bakes corner AA into the PNG, so it stays clean at
        // any aspect.
        HudCardRenderer.drawOverlayCard(ctx, railX, railY, railW, totalH,
                dp(RAIL_RADIUS),
                MenuTheme.MATERIAL_PANEL,
                MenuTheme.MATERIAL_OPACITY_PANEL * eased);

        hoveredRailIndex = -1;
        AppView[] views = AppView.values();
        // +dp(1) nudge — visually compensates for the SDF card's slight
        // left-bias at small radii.
        int btnX = railX + (railW - btn) / 2 + dp(1);
        int cursorY = railY + padY;
        boolean light = MenuTheme.current == MenuTheme.ThemeMode.LIGHT;

        for (int i = 0; i < count; i++) {
            AppView v = views[i];
            int by = cursorY;
            boolean hover = mx >= btnX && mx <= btnX + btn && my >= by && my <= by + btn;
            boolean active = currentView == v;
            if (hover) hoveredRailIndex = i;

            railHoverAnim[i] += (((hover && !active) ? 1f : 0f) - railHoverAnim[i]) * 0.22f;
            float hoverProgress = railHoverAnim[i];

            int btnRadius = dp(RAIL_BTN_RADIUS);

            if (active) {
                // ---- ACTIVE: quietly elevated glass card, no colour accent ----
                // Same vocabulary as the main panel's active category pill.
                float activeOpacity = light
                        ? MenuTheme.MATERIAL_OPACITY_CARD * 0.85f
                        : MenuTheme.MATERIAL_OPACITY_CARD + 0.18f;
                drawSdfCard(ctx, btnX, by, btn, btn, btnRadius, activeOpacity * eased);
                HudCardRenderer.drawShaderOutline(ctx, btnX, by, btn, btn, btnRadius,
                        0.5f, (light ? 0.20f : 0.22f) * eased);
            } else {
                // ---- INACTIVE / HOVER: outline ring + (on hover) glass fill ----
                if (hoverProgress > 0.02f) {
                    drawSdfCard(ctx, btnX, by, btn, btn, btnRadius,
                            (MenuTheme.MATERIAL_OPACITY_CARD * (light ? 0.35f : 0.45f))
                                    * hoverProgress * eased);
                }
                float ringAlpha = (light ? 0.14f : 0.16f)
                        + hoverProgress * (light ? 0.16f : 0.18f);
                HudCardRenderer.drawShaderOutline(ctx, btnX, by, btn, btn, btnRadius,
                        0.5f, ringAlpha * eased);
            }

            // Icon — fixed dp size so it stays consistent across themes/scales.
            int iconSize = dp(RAIL_ICON);
            int iconX = btnX + (btn - iconSize) / 2;
            int iconY = by + (btn - iconSize) / 2;
            float iconAlpha;
            int iconBase;
            if (active) {
                // Active rail icon — tinted accent.
                iconBase = MenuTheme.ACCENT_USER;
                iconAlpha = 1.0f;
            } else {
                iconBase = MenuTheme.TEXT_NEUTRAL;
                iconAlpha = 0.55f + hoverProgress * 0.30f;
            }
            drawAppRailIcon(ctx, v, iconX, iconY, iconSize,
                    MenuTheme.withAlpha(iconBase, iconAlpha * eased));

            cursorY += btn + gap;
        }
    }

    /** Minimalist icons for the app-rail. Same primitives style as the module icons. */
    private void drawAppRailIcon(DrawContext ctx, AppView v, int x, int y, int size, int color) {
        float alpha = ((color >>> 24) & 0xFF) / 255.0f;
        int rgb = (color & 0x00FFFFFF) | 0xFF000000;
        int cx = x + size / 2;
        int cy = y + size / 2;
        switch (v) {
            case SETTINGS -> {
                // Gear — outer ring + 4 teeth + central hub.
                int ringInset = Math.max(1, size / 8);
                HudCardRenderer.drawShaderOutline(ctx,
                        x + ringInset, y + ringInset,
                        size - ringInset * 2, size - ringInset * 2,
                        (size - ringInset * 2) / 2.0f, 1.4f, alpha);
                int tooth = Math.max(2, size / 5);
                int tw = Math.max(2, size / 5);
                HudCardRenderer.drawOverlayCard(ctx, cx - tw / 2, y,
                        tw, tooth, 0.5f, rgb, alpha);
                HudCardRenderer.drawOverlayCard(ctx, cx - tw / 2, y + size - tooth,
                        tw, tooth, 0.5f, rgb, alpha);
                HudCardRenderer.drawOverlayCard(ctx, x, cy - tw / 2,
                        tooth, tw, 0.5f, rgb, alpha);
                HudCardRenderer.drawOverlayCard(ctx, x + size - tooth, cy - tw / 2,
                        tooth, tw, 0.5f, rgb, alpha);
                int hub = Math.max(3, size / 4);
                HudCardRenderer.drawOverlayCard(ctx, cx - hub / 2, cy - hub / 2,
                        hub, hub, hub / 2.0f, rgb, alpha);
            }
            case CONFIGURATIONS -> {
                // Stacked horizontal cards — "profiles / saved configs" idea.
                int cardH = Math.max(2, size / 5);
                int gap = Math.max(1, cardH / 2);
                int totalH = cardH * 3 + gap * 2;
                int top = y + (size - totalH) / 2;
                HudCardRenderer.drawOverlayCard(ctx, x, top,
                        size, cardH, cardH * 0.4f, rgb, alpha * 0.65f);
                HudCardRenderer.drawOverlayCard(ctx, x, top + cardH + gap,
                        size, cardH, cardH * 0.4f, rgb, alpha);
                HudCardRenderer.drawOverlayCard(ctx, x, top + (cardH + gap) * 2,
                        size, cardH, cardH * 0.4f, rgb, alpha * 0.45f);
            }
        }
    }

    /** Placeholder content for the CONFIGURATIONS view — empty translucent
     *  area that fills the panel's interior. Will be wired to the profile-
     *  sharing UI in a follow-up. */
    private void renderConfigurationsView(DrawContext ctx, int mx, int my, float eased,
                                            int px, int py, int pw, int ph) {
        // Just a subtle inset card so the panel doesn't look unused.
        int inset = dp(20);
        drawLiquidGlass(ctx,
                px + inset, py + inset,
                pw - inset * 2, ph - inset * 2,
                dp(14),
                MenuTheme.MATERIAL_CARD,
                MenuTheme.MATERIAL_OPACITY_CARD * 0.4f, eased);

        // Tiny caption in the centre so it's clear this is a placeholder.
        String label = "Configurations";
        int tw = textWidth(label, TEXT_BRAND);
        drawScaledText(ctx, label,
                px + (pw - tw) / 2,
                py + (ph - textHeight(TEXT_BRAND)) / 2 - dp(10),
                TEXT_BRAND,
                MenuTheme.withAlpha(MenuTheme.TEXT_PRIMARY, 0.55f * eased));
        String sub = "Coming soon — share & switch your profiles here";
        int sw = textWidth(sub, TEXT_DETAIL_LABEL);
        drawScaledText(ctx, sub,
                px + (pw - sw) / 2,
                py + (ph - textHeight(TEXT_BRAND)) / 2 + dp(8),
                TEXT_DETAIL_LABEL,
                MenuTheme.withAlpha(MenuTheme.TEXT_NEUTRAL, 0.50f * eased));
    }

    // ---------- Render ----------

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        float eased = easeOutCubic(Math.min(1.0f,
                (System.currentTimeMillis() - openedAtMs) / MenuTheme.OPEN_ANIM_DURATION_MS));

        if (VibeVisualsConfigManager.get().menu.liquidGlassBlur) {
            applyMenuBlur(context);
        }
        renderDim(context, eased);

        int pw = panelW();
        int ph = panelH();
        int px = (width - pw) / 2;
        int py = panelY();

        renderBrandAbovePanel(context, eased, px, py, pw);

        // App-rail (left-most vertical strip with view-switcher icons).
        renderAppRail(context, mouseX, mouseY, eased, px, py, ph);

        // Main panel — SDF shader card so the corners stay pixel-perfect at
        // every aspect ratio (the 9-slice texture path showed visible stepping
        // on one of the corners when the panel landed on a high-contrast spot
        // in the world background).
        drawSdfCard(context, px, py, pw, ph, dp(PANEL_RADIUS),
                MenuTheme.MATERIAL_OPACITY_PANEL * eased);
        HudCardRenderer.drawShaderOutline(context, px, py, pw, ph, dp(PANEL_RADIUS),
                0.55f, MenuTheme.GLASS_OUTLINE_ALPHA * eased);

        // Inset top highlight — 1-px lighter line along the top inner edge.
        // Equivalent to CSS `inset 0 1px 0 rgba(255,255,255,0.06)` — sells the
        // "machined" / "real glass" feel without going skeuomorphic.
        int hlInset = dp(PANEL_RADIUS);
        int hlAlpha = Math.round(0.06f * 255f * eased);
        if (hlAlpha > 0) {
            int hlColor = (hlAlpha << 24) | 0x00FFFFFF;
            context.fill(px + hlInset, py, px + pw - hlInset, py + 1, hlColor);
        }

        // Advance drill-down animation.
        float target = detailTarget != null ? 1.0f : 0.0f;
        detailSlide += (target - detailSlide) * 0.28f;

        if (currentView == AppView.SETTINGS) {
            renderHeader(context, mouseX, mouseY, eased, px, py, pw);
            renderSidebar(context, mouseX, mouseY, eased, px, py, ph);
            renderContent(context, mouseX, mouseY, eased, px, py, pw, ph);
        } else {
            renderConfigurationsView(context, mouseX, mouseY, eased, px, py, pw, ph);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private int brandBlockHeight() {
        return Math.max(dp(BRAND_ICON), textHeight(TEXT_BRAND)) + dp(BRAND_BOTTOM_GAP);
    }

    private int panelY() {
        int total = brandBlockHeight() + panelH();
        int top = Math.max(dp(20), (height - total) / 2);
        return top + brandBlockHeight();
    }

    /** Brand asset — single PNG containing the V plate + "VibeVisuals" wordmark. */
    private static final net.minecraft.util.Identifier BRAND_TEXTURE =
            net.minecraft.util.Identifier.of( VibeVisualsClient.MOD_ID, "textures/gui/brand.png");
    private static final int BRAND_TEX_W = 2508;
    private static final int BRAND_TEX_H = 627;

    private void renderBrandAbovePanel(DrawContext ctx, float eased, int px, int py, int pw) {
        // Display dims: anchor height to BRAND_ICON dp, width follows the PNG's aspect.
        int drawH = dp(BRAND_ICON);
        int drawW = Math.round(drawH * (float) BRAND_TEX_W / BRAND_TEX_H);

        int blockX = px + (pw - drawW) / 2;
        int blockTop = py - brandBlockHeight();
        int regionH = brandBlockHeight() - dp(BRAND_BOTTOM_GAP);
        int drawY = blockTop + (regionH - drawH) / 2;

        // Sample the full texture stretched to (drawW × drawH). Color tint =
        // pure white so the PNG's own colours come through verbatim; only the
        // alpha channel drives the open-animation fade.
        int tint = MenuTheme.withAlpha(0xFFFFFF, eased);
        ctx.drawTexture(
                net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED,
                BRAND_TEXTURE,
                blockX, drawY,
                0.0f, 0.0f,
                drawW, drawH,
                BRAND_TEX_W, BRAND_TEX_H,
                BRAND_TEX_W, BRAND_TEX_H,
                tint
        );
    }

    /**
     * App-icon-style "V" mark. Two colour variants: a dark navy plate with a
     * white V for the dark theme, a light plate with a dark V for the light
     * theme — matching the brand assets the user provided.
     */
    private void drawVLogo(DrawContext ctx, int x, int y, int size, float eased) {
        boolean light = MenuTheme.current == MenuTheme.ThemeMode.LIGHT;
        int plate = light ? 0xFFEFEFF6 : 0xFF0B0B16;
        int vColor = light ? 0xFF0E0E18 : 0xFFFFFFFF;
        int radius = Math.max(4, size * 22 / 100);
        // Plate background (solid, NOT translucent — brand mark stays readable).
        HudCardRenderer.drawOverlayCard(ctx, x, y, size, size, radius, plate, eased);
        // Thin contrasting outline so the plate reads on either theme's backdrop.
        HudCardRenderer.drawShaderOutline(ctx, x, y, size, size, radius, 0.55f, 0.25f * eased);
        // "V" letter centred in the plate. The optical centre of a "V" glyph
        // sits above the geometric centre (wide top, pointy bottom), so we
        // nudge it down a little to balance it inside the square.
        int vW = textWidth("V", TEXT_BRAND_V);
        int vH = textHeight(TEXT_BRAND_V);
        int vx = x + (size - vW) / 2;
        int vy = y + (size - vH) / 2 + Math.max(2, size / 10);
        drawScaledText(ctx, "V", vx, vy, TEXT_BRAND_V,
                MenuTheme.withAlpha(vColor, eased));
    }

    // ---------- Header ----------

    private void renderHeader(DrawContext ctx, int mx, int my, float eased,
                              int px, int py, int pw) {
        int hH = dp(HEADER_H);

        // Back chevron — only the screen's own close affordance.  Detail back is
        // rendered separately inside the content area.
        int backSize = dp(20);
        int backX = px + dp(PAD_X);
        int backY = py + (hH - backSize) / 2;
        hoveredBack = mx >= backX && mx <= backX + backSize
                && my >= backY && my <= backY + backSize;
        int backColor = hoveredBack
                ? MenuTheme.withAlpha(MenuTheme.TEXT_PRIMARY, eased)
                : MenuTheme.withAlpha(MenuTheme.TEXT_NEUTRAL, 0.55f * eased);
        drawChevronLeft(ctx, backX, backY, backSize, backColor);

        // Brand label is rendered above the panel — header keeps only the back chevron.

        // Search bar — right third of the header strip (only shown on list view).
        if (detailTarget == null) {
            int searchHpx = dp(SEARCH_H);
            int sw = Math.max(dp(140), pw / 3);
            int sx = px + pw - dp(PAD_X) - sw;
            // +dp(2) nudge so the bar sits a touch below pure-centre — looks more balanced
            // against the back chevron on the left, which has a slight optical-weight bottom.
            int sy = py + (hH - searchHpx) / 2 + dp(2);
            renderSearchBar(ctx, mx, my, sx, sy, sw, searchHpx, eased);
        } else {
            // Clear hit areas so stale clicks don't trigger inside detail view.
            hoveredSearchBox = false;
            hoveredSearchClear = false;
        }

        int sepY = py + hH;
        int sepStartX = px + dp(SIDEBAR_W);
        ctx.fill(sepStartX, sepY, px + pw, sepY + 1,
                MenuTheme.withAlpha(MenuTheme.TEXT_NEUTRAL, SEPARATOR_ALPHA_HEADER * eased));
    }

    // ---------- Sidebar ----------

    private void renderSidebar(DrawContext ctx, int mx, int my, float eased,
                                int px, int py, int ph) {
        int hH = dp(HEADER_H);
        int sbW = dp(SIDEBAR_W);
        int rowH = dp(SIDEBAR_ROW_H);
        int sbX = px;
        int sbY = py + hH + dp(10);
        int pillInset = dp(SIDEBAR_PILL_PAD);
        int pillX = sbX + pillInset;
        int pillW = sbW - pillInset * 2;
        int iconSize = dp(CATEGORY_ICON);
        int iconGap = dp(CATEGORY_ICON_GAP);

        Category[] cats = Category.values();
        hoveredCategoryIndex = -1;

        int cursorY = sbY;
        Section lastSection = null;
        for (int i = 0; i < cats.length; i++) {
            Category cat = cats[i];
            Section section = cat.section;
            if (section != lastSection) {
                if (lastSection != null) cursorY += dp(SECTION_HEADER_GAP - 6);
                // Tracked uppercase caption — 0.08em letter-spacing, like the
                // CSS `.section-label` recipe. Adds the premium "spaced caps"
                // look to MODULES / SYSTEM headers.
                drawScaledTextTracked(ctx, section.label,
                        pillX + dp(10), cursorY + dp(2), TEXT_SECTION,
                        MenuTheme.withAlpha(MenuTheme.TEXT_NEUTRAL, SECTION_HEADER_ALPHA * eased),
                        0.18f);
                cursorY += dp(SECTION_HEADER_GAP);
                lastSection = section;
            }

            int rowY = cursorY;
            boolean hov = mx >= pillX && mx <= pillX + pillW
                    && my >= rowY && my <= rowY + rowH;
            if (hov) hoveredCategoryIndex = i;

            boolean isSelected = cat == selected;
            if (isSelected) {
                // Active pill: base elevated glass + subtle accent tint overlay
                // so the user's chosen brand colour reads on the active category.
                drawLiquidGlass(ctx, pillX, rowY, pillW, rowH, dp(SIDEBAR_ROW_RADIUS),
                        MenuTheme.MATERIAL_CARD_ACTIVE,
                        MenuTheme.MATERIAL_OPACITY_CARD_ACTIVE + 0.05f, eased);
                HudCardRenderer.drawOverlayCard(ctx, pillX, rowY, pillW, rowH,
                        dp(SIDEBAR_ROW_RADIUS),
                        MenuTheme.ACCENT_USER, 0.10f * eased);
            } else if (hov) {
                drawLiquidGlass(ctx, pillX, rowY, pillW, rowH, dp(SIDEBAR_ROW_RADIUS),
                        MenuTheme.MATERIAL_CARD,
                        MenuTheme.MATERIAL_OPACITY_CARD + 0.05f, eased);
            }

            int contentX = pillX + dp(10);
            int iconY = rowY + (rowH - iconSize) / 2;
            int iconColor;
            int textColor;
            if (isSelected) {
                // Active category icon — tinted with the user's accent so the
                // brand colour spreads beyond just toggles.
                iconColor = MenuTheme.withAlpha(MenuTheme.ACCENT_USER, eased);
                textColor = MenuTheme.withAlpha(MenuTheme.TEXT_PRIMARY, eased);
            } else if (hov) {
                iconColor = MenuTheme.withAlpha(MenuTheme.TEXT_NEUTRAL, 0.80f * eased);
                textColor = MenuTheme.withAlpha(MenuTheme.TEXT_NEUTRAL, 0.85f * eased);
            } else {
                iconColor = MenuTheme.withAlpha(MenuTheme.TEXT_NEUTRAL, 0.45f * eased);
                textColor = MenuTheme.withAlpha(MenuTheme.TEXT_NEUTRAL, 0.55f * eased);
            }
            drawCategoryIcon(ctx, cat, contentX, iconY, iconSize, iconColor);
            int textY = rowY + (rowH - textHeight(TEXT_CATEGORY)) / 2;
            drawScaledText(ctx, cat.label, contentX + iconSize + iconGap, textY,
                    TEXT_CATEGORY, textColor);

            cursorY += rowH + dp(2);
        }

        int vsX = sbX + sbW;
        ctx.fill(vsX, py, vsX + 1, py + ph,
                MenuTheme.withAlpha(MenuTheme.TEXT_NEUTRAL, SEPARATOR_ALPHA_SIDEBAR * eased));
    }

    // ---------- Content area (list ↔ detail drill-down) ----------

    private void renderContent(DrawContext ctx, int mx, int my, float eased,
                                int px, int py, int pw, int ph) {
        int cx = px + dp(SIDEBAR_W) + 1;
        int cy = py + dp(HEADER_H);
        int cw = pw - dp(SIDEBAR_W) - 1;
        int chh = ph - dp(HEADER_H);

        ctx.enableScissor(cx, cy, cx + cw, cy + chh);

        // Slide: list pans out to the left, detail pans in from the right.
        int slidePx = Math.round(detailSlide * cw);
        // List sits offset by -slidePx, detail by (cw - slidePx).
        if (detailSlide < 0.99f || detailTarget == null) {
            renderModuleList(ctx, mx + slidePx, my, eased, cx - slidePx, cy, cw, chh);
        }
        if (detailSlide > 0.02f || detailTarget != null) {
            int detailX = cx + cw - slidePx;
            renderDetail(ctx, mx - (detailX - cx), my, eased, detailX, cy, cw, chh);
        }

        ctx.disableScissor();
    }

    private void renderModuleList(DrawContext ctx, int mx, int my, float eased,
                                   int cx, int cy, int cw, int chh) {
        int rowH = dp(ROW_H);
        int rowGap = dp(ROW_GAP);
        int padX = dp(PAD_X);
        int rowX = cx + padX;
        int rowW = cw - padX * 2;

        // Search bar lives in the panel header (renderHeader), so the list keeps
        // its full vertical space and starts at the top of the content area.
        int listTop = cy + dp(12);
        int listBot = cy + chh - dp(12);

        // Filter set: by search query if non-empty (across all categories), else by selected category.
        List<FeatureEntry> rows = new ArrayList<>();
        if (!searchQuery.isEmpty()) {
            String q = searchQuery.toLowerCase(Locale.ROOT);
            for (FeatureEntry f : features) {
                if (f.name.toLowerCase(Locale.ROOT).contains(q)) rows.add(f);
            }
        } else {
            for (FeatureEntry f : features) if (f.category == selected) rows.add(f);
        }

        // Empty-state when nothing matched the query.
        if (rows.isEmpty() && !searchQuery.isEmpty()) {
            int msgY = listTop + dp(16);
            String msg = "No modules match \"" + searchQuery + "\"";
            int tw = textWidth(msg, TEXT_ROW);
            drawScaledText(ctx, msg, cx + (cw - tw) / 2, msgY, TEXT_ROW,
                    MenuTheme.withAlpha(MenuTheme.TEXT_NEUTRAL, 0.55f * eased));
            return;
        }

        // Scroll calculations.
        int contentH = rows.size() * (rowH + rowGap) - rowGap;
        contentMaxScroll = Math.max(0, contentH - (listBot - listTop));
        if (contentScroll > contentMaxScroll) contentScroll = contentMaxScroll;

        hoveredRow = null;
        hoveredOnSwitch = false;

        int cursor = listTop - contentScroll;
        for (FeatureEntry f : rows) {
            int rowY = cursor;
            cursor += rowH + rowGap;
            if (rowY + rowH < listTop) continue;
            if (rowY > listBot) break;

            f.x = rowX;
            f.y = rowY;
            f.width = rowW;
            f.height = rowH;
            int swW = dp(SWITCH_W);
            int swH = dp(SWITCH_H);
            f.switchX = rowX + rowW - dp(ROW_PAD_X) - swW;
            f.switchY = rowY + (rowH - swH) / 2;
            f.switchWidth = swW;
            f.switchHeight = swH;

            boolean hov = mx >= f.x && mx <= f.x + f.width
                    && my >= f.y && my <= f.y + f.height;
            boolean onSwitch = mx >= f.switchX - dp(3) && mx <= f.switchX + f.switchWidth + dp(3)
                    && my >= f.switchY - dp(3) && my <= f.switchY + f.switchHeight + dp(3);
            if (hov) {
                hoveredRow = f;
                hoveredOnSwitch = onSwitch;
            }

            f.hoverProgress += ((hov ? 1.0f : 0.0f) - f.hoverProgress) * MenuTheme.HOVER_LERP;

            // Every row gets a barely-there card + hairline border. Enabled
            // rows additionally get a subtle accent tint on the background so
            // the accent colour reads as a brand element rather than only a
            // toggle pill. ChatGPT recipe: rgba(accent, 0.045) bg + 0.22 ring.
            int cardInset = dp(2);
            int cardY = f.y + cardInset;
            int cardH = f.height - cardInset * 2;
            boolean rowOn = f.enabled.get();

            // Base dark card so the row reads as inset against the panel.
            float restBgA   = 0.10f;
            float hoverBgA  = 0.14f - 0.10f;
            float bgAlpha   = restBgA + hoverBgA * f.hoverProgress;
            HudCardRenderer.drawOverlayCard(ctx, f.x, cardY, f.width, cardH,
                    dp(ROW_RADIUS), 0xFF000000, bgAlpha * eased);

            // Accent tint overlay — only when the module is ON.
            if (rowOn) {
                float accentBgA = 0.06f + 0.04f * f.hoverProgress;  // 0.06 → 0.10
                HudCardRenderer.drawOverlayCard(ctx, f.x, cardY, f.width, cardH,
                        dp(ROW_RADIUS), MenuTheme.ACCENT_USER, accentBgA * eased);
            }

            // Hairline ring — brighter when enabled so it reads as "active".
            float restRingA  = rowOn ? 0.20f : 0.045f;
            float hoverRingA = rowOn ? 0.28f - 0.20f : 0.090f - 0.045f;
            float ringAlpha  = restRingA + hoverRingA * f.hoverProgress;
            HudCardRenderer.drawShaderOutline(ctx, f.x, cardY, f.width, cardH,
                    dp(ROW_RADIUS), 0.5f, ringAlpha * eased);

            boolean enabled = f.enabled.get();
            // Hierarchy per ChatGPT's premium recipe:
            //   title       — rgba(white, 0.92)  (was 0.82, too pale)
            //   description — rgba(white, 0.42)
            // Disabled rows fade both lines so the toggle state reads at a glance.
            int nameColor = enabled
                    ? MenuTheme.withAlpha(MenuTheme.TEXT_PRIMARY, 0.92f * eased)
                    : MenuTheme.withAlpha(MenuTheme.TEXT_NEUTRAL, 0.48f * eased);
            int subColor = enabled
                    ? MenuTheme.withAlpha(MenuTheme.TEXT_NEUTRAL, 0.42f * eased)
                    : MenuTheme.withAlpha(MenuTheme.TEXT_NEUTRAL, 0.28f * eased);

            // Pick description for current MC locale. Name is always English.
            String desc = localizedDescription(f);

            // textHeight returns only the cap height (~0.72 of em) — the SmoothText
            // cell drawn for `name` actually extends below that point. dp(2) gap was
            // too tight and the description's caps clipped under the name's tail.
            // Bump gap to dp(6) so there's clear daylight between the two lines.
            int nameH = textHeight(TEXT_ROW);
            int subH  = textHeight(TEXT_ROW_SUB);
            int gap = dp(6);
            int totalH = nameH + gap + subH;
            int blockTop = rowY + (rowH - totalH) / 2;

            // Module icon — square, centered vertically, leftmost element of the row.
            // Enabled rows: tinted with the user's accent. Disabled: muted neutral.
            int iconSize = dp(14);
            int iconX = rowX + dp(ROW_PAD_X);
            int iconY = rowY + (rowH - iconSize) / 2;
            if (f.icon != null) {
                int moduleIconColor = rowOn
                        ? MenuTheme.withAlpha(MenuTheme.ACCENT_USER, 0.95f * eased)
                        : nameColor;
                drawModuleIcon(ctx, f.icon, iconX, iconY, iconSize, moduleIconColor);
            }

            // Text starts to the right of the icon.
            int textX = (f.icon != null) ? iconX + iconSize + dp(10) : rowX + dp(ROW_PAD_X);
            drawScaledText(ctx, f.name, textX, blockTop, TEXT_ROW, nameColor);
            if (desc != null && !desc.isEmpty()) {
                drawScaledText(ctx, desc,
                        textX, blockTop + nameH + gap, TEXT_ROW_SUB, subColor);
            }

            // (drill-into-detail chevron removed — it rasterised to a stepped 3-pixel
            // dot at row scale, adding noise without affordance: whole row is already
            // clickable to open detail, hovering the row highlights it.)

            // Apple-style switch — skipped for button-style rows (customAction set).
            if (f.customAction == null) {
                f.knobAnim += ((enabled ? 1.0f : 0.0f) - f.knobAnim) * MenuTheme.KNOB_LERP;
                drawAppleSwitch(ctx, f.switchX, f.switchY, f.switchWidth, f.switchHeight,
                        f.knobAnim, eased);
            } else {
                // For action rows draw a small chevron-right hint to signal "opens screen".
                int chevSize = dp(10);
                int chevX = f.switchX + f.switchWidth - chevSize;
                int chevY = f.switchY + (f.switchHeight - chevSize) / 2;
                drawChevronRight(ctx, chevX, chevY, chevSize,
                        MenuTheme.withAlpha(MenuTheme.TEXT_NEUTRAL, 0.55f * eased));
            }
        }

        // Scrollbar — drawn OUTSIDE the panel (to the right of it) so it reads
        // as a separate floating indicator instead of an inline rail. Needs the
        // scissor lifted because the content scissor would clip anything past
        // the panel's right edge.
        if (contentMaxScroll > 0) {
            int viewportH = listBot - listTop;
            int totalH = viewportH + contentMaxScroll;
            // Use panel bounds (not the content area) for vertical centring so
            // top/bottom margins match visually.
            int panelTop = cy - dp(HEADER_H);
            int panelBottom = cy + chh;
            ctx.disableScissor();
            drawScrollbar(ctx, cx + cw + dp(10), panelTop, panelBottom,
                    contentScroll, contentMaxScroll, viewportH, totalH, eased);
            ctx.enableScissor(cx, cy, cx + cw, cy + chh);
        }
    }

    /** Floating scrollbar sitting to the right of the panel. A small dark
     *  rounded card hosts the white thumb — reads as its own little capsule
     *  rather than a hairline rail. Centred against {@code panelTop/Bottom}. */
    private void drawScrollbar(DrawContext ctx, int rightEdge, int panelTop, int panelBottom,
                                int scroll, int maxScroll, int viewportH, int contentH,
                                float eased) {
        int panelH = panelBottom - panelTop;
        // Card geometry: tall thin capsule, ~45% of panel height, centred.
        int cardW = Math.max(dp(6), dp(7));
        int cardH = Math.min(panelH, (panelH * 2) / 5);
        int cardX = rightEdge - cardW;
        int cardY = panelTop + (panelH - cardH) / 2;
        int cardRadius = cardW / 2; // fully rounded ends

        // Plain dark capsule — no outline / glass highlights (those rendered as
        // bright slivers on the top/bottom that distracted from the thumb).
        boolean light = MenuTheme.current == MenuTheme.ThemeMode.LIGHT;
        int cardColor = light ? 0xFFB8B8C0 : 0xFF0A0A10;
        HudCardRenderer.drawOverlayCard(ctx, cardX, cardY, cardW, cardH, cardRadius,
                cardColor, 0.55f * eased);

        // Thumb — narrower than the card, white, sized by viewport/content ratio.
        int thumbW = Math.max(2, cardW - dp(3));
        int thumbX = cardX + (cardW - thumbW) / 2;
        int innerPad = dp(2);
        int trackH = cardH - innerPad * 2;
        int thumbH = Math.max(dp(8),
                Math.round((float) trackH * viewportH / Math.max(1, contentH)));
        if (thumbH > trackH) thumbH = trackH;
        int thumbY = cardY + innerPad + Math.round(
                (float) (trackH - thumbH) * scroll / Math.max(1, maxScroll));
        HudCardRenderer.drawOverlayCard(ctx, thumbX, thumbY, thumbW, thumbH,
                thumbW * 0.5f, 0xFFFFFFFF, 0.90f * eased);
    }

    /** Search bar inside the panel header (or wherever caller places it). */
    private void renderSearchBar(DrawContext ctx, int mx, int my,
                                  int sx, int sy, int sw, int sh, float eased) {
        searchX = sx;
        searchY = sy;
        searchW = sw;
        searchH = sh;

        hoveredSearchBox = mx >= searchX && mx <= searchX + searchW
                && my >= searchY && my <= searchY + searchH;
        searchFocusAnim += ((searchFocused ? 1.0f : 0.0f) - searchFocusAnim) * MenuTheme.HOVER_LERP;

        // Glass pill background.
        float baseOpacity = MenuTheme.MATERIAL_OPACITY_CARD * (0.55f + searchFocusAnim * 0.25f);
        drawLiquidGlass(ctx, searchX, searchY, searchW, searchH, dp(SEARCH_RADIUS),
                MenuTheme.MATERIAL_CARD, baseOpacity, eased);
        // Accent focus tint — fades in as the search field gains focus.
        if (searchFocusAnim > 0.02f) {
            HudCardRenderer.drawOverlayCard(ctx, searchX, searchY, searchW, searchH,
                    dp(SEARCH_RADIUS), MenuTheme.ACCENT_USER,
                    0.12f * searchFocusAnim * eased);
        }

        // Magnifier icon — small ring + diagonal stem, drawn from quads.
        int iconSize = dp(11);
        int iconX = searchX + dp(SEARCH_PAD_X);
        int iconY = searchY + (searchH - iconSize) / 2;
        int iconColor = MenuTheme.withAlpha(MenuTheme.TEXT_NEUTRAL,
                (searchFocused ? 0.85f : 0.55f) * eased);
        // Lerp icon colour towards the accent as focus animates in.
        int focusedColor = MenuTheme.withAlpha(MenuTheme.ACCENT_USER, 0.95f * eased);
        int finalIconColor = searchFocusAnim > 0.02f
                ? MenuTheme.lerpColor(iconColor, focusedColor, searchFocusAnim)
                : iconColor;
        drawSearchIcon(ctx, iconX, iconY, iconSize, finalIconColor);

        // Text: live query if any, placeholder otherwise.
        int textX = iconX + iconSize + dp(8);
        int textY = searchY + (searchH - textHeight(TEXT_SEARCH)) / 2;
        if (searchQuery.isEmpty()) {
            drawScaledText(ctx, "Search modules…", textX, textY, TEXT_SEARCH,
                    MenuTheme.withAlpha(MenuTheme.TEXT_NEUTRAL, 0.45f * eased));
        } else {
            drawScaledText(ctx, searchQuery, textX, textY, TEXT_SEARCH,
                    MenuTheme.withAlpha(MenuTheme.TEXT_PRIMARY, eased));
            // Blinking caret right after the query text — only when focused.
            if (searchFocused && (System.currentTimeMillis() / 530L) % 2L == 0L) {
                int caretX = textX + textWidth(searchQuery, TEXT_SEARCH) + dp(1);
                int caretH = textHeight(TEXT_SEARCH);
                ctx.fill(caretX, textY, caretX + Math.max(1, dp(1)), textY + caretH,
                        MenuTheme.withAlpha(MenuTheme.TEXT_PRIMARY, eased));
            }
        }

        // Clear `×` button on the right when there is a query.
        hoveredSearchClear = false;
        if (!searchQuery.isEmpty()) {
            int clearSize = dp(12);
            int clearX = searchX + searchW - dp(SEARCH_PAD_X) - clearSize;
            int clearY = searchY + (searchH - clearSize) / 2;
            hoveredSearchClear = mx >= clearX - dp(2) && mx <= clearX + clearSize + dp(2)
                    && my >= clearY - dp(2) && my <= clearY + clearSize + dp(2);
            int clearColor = MenuTheme.withAlpha(MenuTheme.TEXT_NEUTRAL,
                    (hoveredSearchClear ? 0.95f : 0.55f) * eased);
            drawCrossIcon(ctx, clearX, clearY, clearSize, clearColor);
        }
    }

    /** Tiny magnifier: a 5-segment ring + 2-segment stem, all 1-px thick. */
    private void drawSearchIcon(DrawContext ctx, int x, int y, int size, int color) {
        int thick = Math.max(1, size / 7);
        int ringSize = (int) Math.round(size * 0.72);
        int rx = x;
        int ry = y;
        // Ring as 4 edges of a square (visually reads as round at small sizes thanks to AA + mip).
        ctx.fill(rx, ry, rx + ringSize, ry + thick, color);                              // top
        ctx.fill(rx, ry + ringSize - thick, rx + ringSize, ry + ringSize, color);        // bottom
        ctx.fill(rx, ry, rx + thick, ry + ringSize, color);                              // left
        ctx.fill(rx + ringSize - thick, ry, rx + ringSize, ry + ringSize, color);        // right
        // Stem.
        int stemStartX = rx + ringSize - 1;
        int stemStartY = ry + ringSize - 1;
        int stemEnd = Math.max(2, size - ringSize + 1);
        for (int i = 0; i < stemEnd; i++) {
            ctx.fill(stemStartX + i, stemStartY + i,
                    stemStartX + i + thick, stemStartY + i + thick, color);
        }
    }

    /** Tiny cross / clear icon: two diagonal segments. */
    private void drawCrossIcon(DrawContext ctx, int x, int y, int size, int color) {
        int thick = Math.max(1, size / 7);
        int reach = size - thick;
        for (int i = 0; i < reach; i++) {
            ctx.fill(x + i, y + i, x + i + thick, y + i + thick, color);
            ctx.fill(x + reach - i, y + i, x + reach - i + thick, y + i + thick, color);
        }
    }

    private void renderDetail(DrawContext ctx, int mx, int my, float eased,
                               int cx, int cy, int cw, int chh) {
        FeatureEntry target = detailTarget;
        if (target == null) return;

        int padX = dp(PAD_X);

        // Back row.
        int backY = cy + dp(14);
        int backSize = dp(14);
        int backX = cx + padX;
        hoveredDetailBack = mx >= backX - dp(4) && mx <= backX + backSize + dp(40)
                && my >= backY - dp(4) && my <= backY + backSize + dp(4);
        int backColor = hoveredDetailBack
                ? MenuTheme.withAlpha(MenuTheme.ACCENT_USER, eased)
                : MenuTheme.withAlpha(MenuTheme.TEXT_NEUTRAL, 0.70f * eased);
        drawChevronLeft(ctx, backX, backY, backSize, backColor);
        drawScaledText(ctx, "Back", backX + backSize + dp(6),
                backY + (backSize - textHeight(TEXT_BACK)) / 2,
                TEXT_BACK, backColor);

        // Title row.
        int titleY = backY + backSize + dp(10);
        drawScaledText(ctx, target.name, cx + padX, titleY, TEXT_DETAIL_TITLE,
                MenuTheme.withAlpha(MenuTheme.TEXT_PRIMARY, eased));

        // ---- Special case: Themes ----
        // Replaces the reflective settings block with a tile grid.
        if ("Themes".equals(target.name)) {
            int gridTop = titleY + textHeight(TEXT_DETAIL_TITLE) + dp(16);
            renderThemesGrid(ctx, mx, my, cx + padX, gridTop,
                    cw - padX * 2,
                    (cy + chh) - gridTop - dp(10),
                    eased);
            return;
        }

        // Enable toggle row (always first, mirrors the list switch).
        int controlsTop = titleY + textHeight(TEXT_DETAIL_TITLE) + dp(20);
        int rowH = dp(34);
        int rowRadius = dp(10);
        int rowsX = cx + padX;
        int rowsW = cw - padX * 2;

        hoveredSettingRow = null;

        // Synthetic "Enabled" row.
        int enRowY = controlsTop;
        drawSettingRowBg(ctx, rowsX, enRowY, rowsW, rowH, rowRadius, eased);
        drawScaledText(ctx, "Enabled", rowsX + dp(14),
                enRowY + (rowH - textHeight(TEXT_DETAIL_LABEL)) / 2,
                TEXT_DETAIL_LABEL,
                MenuTheme.withAlpha(MenuTheme.TEXT_PRIMARY, eased));
        int swW = dp(SWITCH_W);
        int swH = dp(SWITCH_H);
        int enSwX = rowsX + rowsW - dp(14) - swW;
        int enSwY = enRowY + (rowH - swH) / 2;
        target.detailKnobAnim += ((target.enabled.get() ? 1.0f : 0.0f) - target.detailKnobAnim)
                * MenuTheme.KNOB_LERP;
        drawAppleSwitch(ctx, enSwX, enSwY, swW, swH, target.detailKnobAnim, eased);
        target.detailSwitchX = enSwX;
        target.detailSwitchY = enSwY;
        target.detailSwitchW = swW;
        target.detailSwitchH = swH;

        // Reflected setting rows.
        if (detailSettings.isEmpty()) {
            rebuildDetailSettings(target);
        }

        int cursorY = enRowY + rowH + dp(6);
        for (SettingRow row : detailSettings) {
            if (cursorY + rowH > cy + chh - dp(12)) break;
            drawSettingRowBg(ctx, rowsX, cursorY, rowsW, rowH, rowRadius, eased);

            row.x = rowsX;
            row.y = cursorY;
            row.width = rowsW;
            row.height = rowH;

            String label = humanize(row.field.getName());
            int labelY = cursorY + (rowH - textHeight(TEXT_DETAIL_LABEL)) / 2;
            drawScaledText(ctx, label, rowsX + dp(14), labelY, TEXT_DETAIL_LABEL,
                    MenuTheme.withAlpha(MenuTheme.TEXT_PRIMARY, 0.92f * eased));

            Class<?> type = row.field.getType();
            if (type == boolean.class) {
                int sx = rowsX + rowsW - dp(14) - swW;
                int sy = cursorY + (rowH - swH) / 2;
                row.switchX = sx;
                row.switchY = sy;
                row.switchW = swW;
                row.switchH = swH;
                boolean v = readBool(target.config, row.field);
                row.knobAnim += ((v ? 1.0f : 0.0f) - row.knobAnim) * MenuTheme.KNOB_LERP;
                drawAppleSwitch(ctx, sx, sy, swW, swH, row.knobAnim, eased);
            } else if (isColorField(row.field)) {
                // Read-only colour swatch + hex text. Click-to-edit can be added later.
                int swatch = dp(16);
                int sx = rowsX + rowsW - dp(14) - swatch;
                int sy = cursorY + (rowH - swatch) / 2;
                row.swatchX = sx; row.swatchY = sy; row.swatchSize = swatch;
                int argb = (int) readFloat(target.config, row.field);
                // Coerce: if alpha is 0 (legacy int colour), force opaque for preview.
                int previewColor = (argb & 0xFF000000) == 0 ? (argb | 0xFF000000) : argb;
                HudCardRenderer.drawOverlayCard(ctx, sx, sy, swatch, swatch, dp(4),
                        previewColor, 1.0f * eased);
                HudCardRenderer.drawShaderOutline(ctx, sx, sy, swatch, swatch, dp(4),
                        0.5f, 0.30f * eased);
                String hex = String.format(Locale.ROOT, "#%06X", argb & 0xFFFFFF);
                int tw = textWidth(hex, TEXT_DETAIL_LABEL);
                drawScaledText(ctx, hex, sx - tw - dp(8), labelY, TEXT_DETAIL_LABEL,
                        MenuTheme.withAlpha(MenuTheme.TEXT_NEUTRAL, 0.55f * eased));
            } else if (type == int.class || type == float.class) {
                // Slider for numeric fields. Width chosen so the value text + slider
                // still leave breathing room from the label on the left.
                float cur = readFloat(target.config, row.field);
                if (row.hint == null) row.hint = hintFor(row.field, cur);
                SliderHint h = row.hint;

                String valStr = formatValue(cur, h);
                int valW = textWidth(valStr, TEXT_DETAIL_LABEL);
                int sliderW = dp(110);
                int sliderH = dp(6);
                int valueRightEdge = rowsX + rowsW - dp(14);
                int valueX = valueRightEdge - valW;
                int sliderX = valueX - dp(10) - sliderW;
                int sliderY = cursorY + (rowH - sliderH) / 2;

                row.sliderX = sliderX; row.sliderY = sliderY;
                row.sliderW = sliderW; row.sliderH = sliderH;

                drawSlider(ctx, sliderX, sliderY, sliderW, sliderH, cur, h, eased);
                drawScaledText(ctx, valStr, valueX, labelY, TEXT_DETAIL_LABEL,
                        MenuTheme.withAlpha(MenuTheme.TEXT_PRIMARY, 0.85f * eased));
            } else {
                // String or unknown — read-only text.
                String text = fieldToString(target.config, row.field);
                int tw = textWidth(text, TEXT_DETAIL_LABEL);
                int tx = rowsX + rowsW - dp(14) - tw;
                drawScaledText(ctx, text, tx, labelY, TEXT_DETAIL_LABEL,
                        MenuTheme.withAlpha(MenuTheme.TEXT_NEUTRAL, 0.55f * eased));
            }

            boolean rowHov = mx >= row.x && mx <= row.x + row.width
                    && my >= row.y && my <= row.y + row.height;
            if (rowHov) hoveredSettingRow = row;

            cursorY += rowH + dp(4);
        }
    }

    /**
     * Grid of accent-theme tiles. Each tile is a small dark card with a
     * coloured swatch and the theme's display name. Auto-fits columns based on
     * available width.
     */
    private void renderThemesGrid(DrawContext ctx, int mx, int my,
                                    int gridX, int gridY, int gridW, int gridH, float eased) {
        ru.suppelemen.vibevisuals.theme.AccentTheme[] themes =
                ru.suppelemen.vibevisuals.theme.AccentTheme.values();
        int tileW = dp(96);
        int tileH = dp(78);
        int gap = dp(8);
        int radius = dp(12);

        int cols = Math.max(1, (gridW + gap) / (tileW + gap));
        int actualGridW = cols * tileW + Math.max(0, cols - 1) * gap;
        int startX = gridX + (gridW - actualGridW) / 2;

        hoveredThemeIndex = -1;
        String activeName = VibeVisualsConfigManager.get().menu.accent;

        for (int i = 0; i < themes.length; i++) {
            ru.suppelemen.vibevisuals.theme.AccentTheme t = themes[i];
            int col = i % cols;
            int row = i / cols;
            int tx = startX + col * (tileW + gap);
            int ty = gridY + row * (tileH + gap);
            if (ty + tileH > gridY + gridH) break;   // clip overflow

            boolean hov = mx >= tx && mx <= tx + tileW && my >= ty && my <= ty + tileH;
            boolean active = t.name().equalsIgnoreCase(activeName);
            if (hov) hoveredThemeIndex = i;

            // Card background.
            float bgAlpha = (active ? 0.20f : (hov ? 0.16f : 0.10f)) * eased;
            HudCardRenderer.drawOverlayCard(ctx, tx, ty, tileW, tileH, radius,
                    0xFF000000, bgAlpha);
            // Ring — accent colour when active, hairline white otherwise.
            float ringAlpha = (active ? 0.55f : (hov ? 0.18f : 0.08f)) * eased;
            HudCardRenderer.drawShaderOutline(ctx, tx, ty, tileW, tileH, radius, 0.5f, ringAlpha);
            if (active) {
                // Re-stroke in accent colour for emphasis.
                int outRgb = (t.color & 0xFFFFFF) | (Math.round(0.85f * 255f * eased) << 24);
                HudCardRenderer.drawShaderOutline(ctx, tx, ty, tileW, tileH, radius, 0.7f,
                        0.85f * eased);
                // Tint outline by stacking a coloured pass.
                HudCardRenderer.drawOverlayCard(ctx, tx, ty, tileW, tileH, radius,
                        t.color, 0.08f * eased);
            }

            // Colour swatch — small square in the upper half, centred horizontally.
            int swatch = dp(28);
            int sx = tx + (tileW - swatch) / 2;
            int sy = ty + dp(10);
            HudCardRenderer.drawOverlayCard(ctx, sx, sy, swatch, swatch, dp(7),
                    t.color, 1.0f * eased);
            // Tiny inner ring for premium feel.
            HudCardRenderer.drawShaderOutline(ctx, sx, sy, swatch, swatch, dp(7),
                    0.5f, 0.30f * eased);

            // Theme name centred below swatch.
            String name = t.label;
            int tw = textWidth(name, TEXT_DETAIL_LABEL);
            int nameY = sy + swatch + dp(8);
            int nameColor = active
                    ? MenuTheme.withAlpha(MenuTheme.TEXT_PRIMARY, eased)
                    : MenuTheme.withAlpha(MenuTheme.TEXT_PRIMARY, 0.85f * eased);
            drawScaledText(ctx, name, tx + (tileW - tw) / 2, nameY,
                    TEXT_DETAIL_LABEL, nameColor);
        }
    }

    private void drawSettingRowBg(DrawContext ctx, int x, int y, int w, int h,
                                   int radius, float eased) {
        drawLiquidGlass(ctx, x, y, w, h, radius,
                MenuTheme.MATERIAL_CARD,
                MenuTheme.MATERIAL_OPACITY_CARD + 0.05f, eased);
    }

    /**
     * Range slider. Three layers:
     *   - track (muted pill, full width)
     *   - filled portion (brighter pill, 0…thumbX)
     *   - thumb (circle, accent ring)
     * All shader-AA so they stay smooth at small heights.
     */
    private void drawSlider(DrawContext ctx, int x, int y, int w, int h,
                             float value, SliderHint hint, float eased) {
        boolean light = MenuTheme.current == MenuTheme.ThemeMode.LIGHT;
        float r = h * 0.5f;

        // Track — neutral grey, muted opacity. Lets the filled portion + thumb
        // do the visual heavy-lifting instead of the rail itself.
        int trackColor = light ? 0xFFC4C4C4 : 0xFF3A3A3A;
        HudCardRenderer.drawOverlayCard(ctx, x, y, w, h, r, trackColor, 0.45f * eased);

        // Normalised position 0..1 (clamped).
        float t = (hint.max > hint.min) ? (value - hint.min) / (hint.max - hint.min) : 0f;
        t = Math.max(0f, Math.min(1f, t));

        int filledW = Math.round(w * t);
        if (filledW > 0) {
            // Use the user-picked accent colour. DEFAULT accent = neutral
            // white/dark so it matches the original look.
            int fillColor = MenuTheme.ACCENT_USER;
            HudCardRenderer.drawOverlayCard(ctx, x, y, Math.max(filledW, h), h, r,
                    fillColor, 0.95f * eased);
        }

        // Thumb — slightly taller than track so it pops above.
        int thumbSize = Math.max(dp(10), h + dp(4));
        float thumbXf = x + (w - thumbSize) * t;
        int thumbX = Math.round(thumbXf);
        int thumbY = y + (h - thumbSize) / 2;
        float thumbR = thumbSize * 0.5f;
        // Shadow.
        HudCardRenderer.drawOverlayCard(ctx, thumbX, thumbY + 1,
                thumbSize, thumbSize, thumbR, 0xFF000000, 0.22f * eased);
        // Body.
        HudCardRenderer.drawOverlayCard(ctx, thumbX, thumbY,
                thumbSize, thumbSize, thumbR, 0xFFFFFFFF, 1.0f * eased);
        // Hairline ring.
        HudCardRenderer.drawShaderOutline(ctx, thumbX, thumbY,
                thumbSize, thumbSize, thumbR, 0.4f, 0.18f * eased);
    }

    /** Mouse-X → value mapped via slider geometry, snapped and clamped. */
    private float sliderValueFor(SettingRow row, double mouseX) {
        SliderHint h = row.hint;
        if (h == null) return 0f;
        float t = (float) ((mouseX - row.sliderX) / Math.max(1, row.sliderW));
        t = Math.max(0f, Math.min(1f, t));
        float raw = h.min + (h.max - h.min) * t;
        float snapped = snap(raw, h.step);
        return Math.max(h.min, Math.min(h.max, snapped));
    }

    // ---------- Apple switch + icons + chevrons ----------

    /**
     * iOS-style toggle. All three layers use shader-AA rounded boxes so the
     * pill stays smooth even at the small on-screen sizes a typical menu row
     * forces it into.
     *
     *   1. Track       — full pill, colour lerps OFF→ON with {@code knob}.
     *   2. Outline     — 0.5-px hairline so the off-state track is visible
     *                    against a glassy panel background.
     *   3. Knob        — circle, lerps colour & x. Subtle shadow underneath
     *                    gives a 1-px sense of depth without going skeuomorphic.
     */
    private void drawAppleSwitch(DrawContext ctx, int x, int y, int w, int h, float knob, float eased) {
        boolean light = MenuTheme.current == MenuTheme.ThemeMode.LIGHT;
        float r = h * 0.5f;

        // Track — muted in OFF state, fades to the user's accent colour as
        // the toggle animates ON (Apple-style coloured ON track). On the
        // DEFAULT accent the "on" colour stays neutral white, preserving the
        // original look.
        int trackOff = light ? 0xFFC8C8D0 : 0xFF2F2F3F;
        int trackOn  = MenuTheme.ACCENT_USER;
        int trackColor = MenuTheme.lerpColor(trackOff, trackOn, knob);
        HudCardRenderer.drawOverlayCard(ctx, x, y, w, h, r, trackColor, 0.94f * eased);

        // Hairline outline — sharper definition on glassy backgrounds.
        HudCardRenderer.drawShaderOutline(ctx, x, y, w, h, r, 0.4f, 0.18f * eased);

        // Knob geometry: 86 % of track height (Apple ratio ≈ 27/31).
        int knobInset = Math.max(2, Math.round(h * 0.10f));
        int knobSize = h - knobInset * 2;
        int knobMinX = x + knobInset;
        int knobMaxX = x + w - knobSize - knobInset;
        float knobXf = knobMinX + (knobMaxX - knobMinX) * knob;
        int knobX = Math.round(knobXf);
        float knobR = knobSize * 0.5f;

        // 1-px shadow underneath the knob — gives depth without skeuomorphism.
        HudCardRenderer.drawOverlayCard(ctx, knobX, y + knobInset + 1,
                knobSize, knobSize, knobR, 0xFF000000, 0.18f * eased);

        // Knob fill: dark when OFF, white when ON. Lerps smoothly during the
        // animation so the colour change reads as a single motion with the slide.
        int knobOff = light ? 0xFF6F6F7A : 0xFF1B1B26;
        int knobOn  = 0xFFFFFFFF;
        int knobColor = MenuTheme.lerpColor(knobOff, knobOn, knob);
        HudCardRenderer.drawOverlayCard(ctx, knobX, y + knobInset,
                knobSize, knobSize, knobR, knobColor, eased);

        // Tiny inner ring on the knob — adds a premium "machined" feel.
        HudCardRenderer.drawShaderOutline(ctx, knobX, y + knobInset,
                knobSize, knobSize, knobR, 0.3f, 0.10f * eased);
    }

    private void drawChevronLeft(DrawContext ctx, int x, int y, int size, int color) {
        float alpha = ((color >>> 24) & 0xFF) / 255.0f;
        int rgb = color & 0x00FFFFFF | 0xFF000000;
        int thick = Math.max(1, size / 8);
        int cx = x + size / 2;
        int cy = y + size / 2;
        int reach = size / 3;
        // Two AA pills forming the chevron's V (rotated approximation via stepped tiny pills).
        for (int i = 0; i <= reach; i++) {
            float fade = alpha * (1.0f - i * 0.025f);
            HudCardRenderer.drawOverlayCard(ctx, cx - i, cy - (reach - i),
                    thick, thick, thick / 2.0f, rgb, fade);
            HudCardRenderer.drawOverlayCard(ctx, cx - i, cy + (reach - i),
                    thick, thick, thick / 2.0f, rgb, fade);
        }
    }

    private void drawChevronRight(DrawContext ctx, int x, int y, int size, int color) {
        float alpha = ((color >>> 24) & 0xFF) / 255.0f;
        int rgb = color & 0x00FFFFFF | 0xFF000000;
        int thick = Math.max(1, size / 8);
        int cx = x + size / 2;
        int cy = y + size / 2;
        int reach = size / 3;
        for (int i = 0; i <= reach; i++) {
            float fade = alpha * (1.0f - i * 0.025f);
            HudCardRenderer.drawOverlayCard(ctx, cx + i, cy - (reach - i),
                    thick, thick, thick / 2.0f, rgb, fade);
            HudCardRenderer.drawOverlayCard(ctx, cx + i, cy + (reach - i),
                    thick, thick, thick / 2.0f, rgb, fade);
        }
    }

    /**
     * Smooth (shader-AA) category icons. All shapes stay strictly inside the
     * (x, y, size, size) bounding box so icons sit cleanly aligned with text.
     */
    /** Per-module mini icon. Drawn at very small sizes (~12 px), so each
     *  glyph is built from a handful of rectangles / outline calls — anything
     *  more elaborate would just be muddy at that scale. */
    private void drawModuleIcon(DrawContext ctx, ModuleIcon icon, int x, int y, int size, int color) {
        float alpha = ((color >>> 24) & 0xFF) / 255.0f;
        int rgb = (color & 0x00FFFFFF) | 0xFF000000;
        int cx = x + size / 2;
        int cy = y + size / 2;
        int s1 = Math.max(1, size / 8);  // thin stroke
        int s2 = Math.max(2, size / 5);  // chunky stroke
        switch (icon) {
            case POTION -> {
                // Flask: small neck on top + rounded body.
                int neckW = Math.max(2, size / 3);
                int neckH = Math.max(2, size / 4);
                int bodyW = Math.max(4, size - s1 * 2);
                int bodyH = Math.max(4, size - neckH - 1);
                HudCardRenderer.drawOverlayCard(ctx, cx - neckW / 2, y, neckW, neckH, 0.5f, rgb, alpha * 0.85f);
                HudCardRenderer.drawOverlayCard(ctx, cx - bodyW / 2, y + neckH, bodyW, bodyH,
                        bodyW * 0.4f, rgb, alpha);
            }
            case CLOCK -> {
                HudCardRenderer.drawShaderOutline(ctx, x, y, size, size, size / 2.0f, 1.0f, alpha);
                // Hour hand up, minute hand right.
                HudCardRenderer.drawOverlayCard(ctx, cx - s1 / 2, y + size / 4,
                        s1, cy - (y + size / 4), s1 / 2.0f, rgb, alpha);
                HudCardRenderer.drawOverlayCard(ctx, cx, cy - s1 / 2,
                        (x + size - 2) - cx, s1, s1 / 2.0f, rgb, alpha);
            }
            case KEY -> {
                // Rounded square + small tab on top.
                int tab = Math.max(2, size / 4);
                HudCardRenderer.drawShaderOutline(ctx, x, y + tab, size, size - tab,
                        Math.max(1, size / 5), 1.0f, alpha);
                HudCardRenderer.drawOverlayCard(ctx, cx - tab / 2, y, tab, tab + 1, tab * 0.3f, rgb, alpha);
            }
            case BAR -> {
                // Horizontal pill centred vertically.
                int bH = Math.max(3, size / 2);
                HudCardRenderer.drawOverlayCard(ctx, x, cy - bH / 2, size, bH, bH * 0.5f, rgb, alpha);
            }
            case GRID -> {
                // 3x3 of small squares.
                int cell = Math.max(2, size / 4);
                int gap = Math.max(1, (size - cell * 3) / 2);
                for (int row = 0; row < 3; row++) {
                    for (int col = 0; col < 3; col++) {
                        int px = x + col * (cell + gap);
                        int py = y + row * (cell + gap);
                        HudCardRenderer.drawOverlayCard(ctx, px, py, cell, cell, 0.5f, rgb, alpha);
                    }
                }
            }
            case SHIELD -> {
                // Rounded-top, pointed bottom — approximated via two stacked rounded rects.
                int topH = Math.max(3, size * 2 / 3);
                HudCardRenderer.drawOverlayCard(ctx, x, y, size, topH, size * 0.35f, rgb, alpha * 0.85f);
                int botSize = size - topH;
                HudCardRenderer.drawOverlayCard(ctx, x + size / 4, y + topH,
                        size / 2, botSize, size * 0.25f, rgb, alpha);
            }
            case HOTBAR -> {
                // 5 small slots horizontally (compressed version of MC hotbar).
                int slots = 5;
                int slotW = Math.max(1, (size - (slots - 1)) / slots);
                int slotH = Math.max(3, size * 2 / 3);
                for (int i = 0; i < slots; i++) {
                    int px = x + i * (slotW + 1);
                    HudCardRenderer.drawOverlayCard(ctx, px, cy - slotH / 2, slotW, slotH,
                            slotW * 0.25f, rgb, alpha * (i == 2 ? 1.0f : 0.6f));
                }
            }
            case HEART -> {
                // Two small lobes + downward triangle (approximated).
                int lobe = Math.max(3, size * 2 / 5);
                HudCardRenderer.drawOverlayCard(ctx, x, y + s1, lobe, lobe, lobe * 0.5f, rgb, alpha);
                HudCardRenderer.drawOverlayCard(ctx, x + size - lobe, y + s1, lobe, lobe, lobe * 0.5f, rgb, alpha);
                // Body
                HudCardRenderer.drawOverlayCard(ctx, x + s1, y + lobe / 2,
                        size - s1 * 2, size - lobe / 2 - s1, s1, rgb, alpha);
                // Bottom point — narrowing rects
                for (int i = 0; i < s2 && (y + size - i) > y; i++) {
                    int w = Math.max(1, size - i * 2 - s1 * 2);
                    HudCardRenderer.drawOverlayCard(ctx, cx - w / 2, y + size - s2 + i,
                            w, 1, 0.5f, rgb, alpha);
                }
            }
            case SWORD -> {
                // Diagonal blade (stepped rects from bottom-left to top-right).
                for (int i = 0; i < size - 2; i++) {
                    HudCardRenderer.drawOverlayCard(ctx, x + i, y + size - 2 - i, s1 + 1, s1 + 1,
                            s1 * 0.5f, rgb, alpha);
                }
                // Hilt at bottom-left, perpendicular short bar.
                HudCardRenderer.drawOverlayCard(ctx, x, y + size - s2, s2, s1, s1 * 0.5f, rgb, alpha);
            }
            case CROWN -> {
                // Three triangular spikes + base bar.
                int spikeH = Math.max(3, size / 2);
                int baseY = y + size - s2;
                HudCardRenderer.drawOverlayCard(ctx, x, baseY, size, s2, s2 * 0.3f, rgb, alpha);
                int spikeW = Math.max(1, size / 5);
                HudCardRenderer.drawOverlayCard(ctx, x, baseY - spikeH, spikeW, spikeH, 0.5f, rgb, alpha);
                HudCardRenderer.drawOverlayCard(ctx, cx - spikeW / 2, baseY - spikeH - 1,
                        spikeW, spikeH + 1, 0.5f, rgb, alpha);
                HudCardRenderer.drawOverlayCard(ctx, x + size - spikeW, baseY - spikeH,
                        spikeW, spikeH, 0.5f, rgb, alpha);
            }
            case TARGET -> {
                // Concentric rings + centre dot.
                HudCardRenderer.drawShaderOutline(ctx, x, y, size, size, size / 2.0f, 1.0f, alpha);
                int inner = size - s2 * 2;
                HudCardRenderer.drawShaderOutline(ctx, x + s2, y + s2, inner, inner, inner / 2.0f, 1.0f, alpha * 0.75f);
                int dot = Math.max(2, size / 4);
                HudCardRenderer.drawOverlayCard(ctx, cx - dot / 2, cy - dot / 2, dot, dot,
                        dot * 0.5f, rgb, alpha);
            }
            case DROP -> {
                // Teardrop: round bottom, pointed top.
                int dropW = Math.max(3, size * 3 / 5);
                int round = size - s2;
                HudCardRenderer.drawOverlayCard(ctx, cx - dropW / 2, y + s2, dropW, round - s2,
                        dropW * 0.5f, rgb, alpha);
                // Pointed top - stacked narrowing rects
                for (int i = 0; i < s2 + 1; i++) {
                    int w = Math.max(1, dropW - (s2 - i) * 2);
                    HudCardRenderer.drawOverlayCard(ctx, cx - w / 2, y + i, w, 1, 0.5f, rgb, alpha);
                }
            }
            case SPARK -> {
                // Centre dot + 4 short radial rays.
                int dot = Math.max(2, size / 4);
                HudCardRenderer.drawOverlayCard(ctx, cx - dot / 2, cy - dot / 2, dot, dot,
                        dot * 0.5f, rgb, alpha);
                int rayLen = Math.max(2, size / 3);
                HudCardRenderer.drawOverlayCard(ctx, cx - s1 / 2, y, s1, rayLen, s1 * 0.5f, rgb, alpha);
                HudCardRenderer.drawOverlayCard(ctx, cx - s1 / 2, y + size - rayLen, s1, rayLen, s1 * 0.5f, rgb, alpha);
                HudCardRenderer.drawOverlayCard(ctx, x, cy - s1 / 2, rayLen, s1, s1 * 0.5f, rgb, alpha);
                HudCardRenderer.drawOverlayCard(ctx, x + size - rayLen, cy - s1 / 2, rayLen, s1, s1 * 0.5f, rgb, alpha);
            }
            case ARROW_UP -> {
                // Arrow shaft + head (stacked widening rects).
                HudCardRenderer.drawOverlayCard(ctx, cx - s1 / 2, y + size / 3,
                        s1, size - size / 3, s1 * 0.5f, rgb, alpha);
                int head = size * 2 / 3;
                for (int i = 0; i < head / 2; i++) {
                    int w = (i + 1) * 2;
                    HudCardRenderer.drawOverlayCard(ctx, cx - w / 2, y + head / 2 - i,
                            w, 1, 0.5f, rgb, alpha);
                }
            }
            case CLOUD -> {
                // Three overlapping ellipses (approx with circles).
                int small = Math.max(3, size / 2);
                int big = Math.max(4, size * 3 / 5);
                HudCardRenderer.drawOverlayCard(ctx, x, cy - small / 2 + s1, small, small,
                        small * 0.5f, rgb, alpha);
                HudCardRenderer.drawOverlayCard(ctx, x + size - small, cy - small / 2 + s1,
                        small, small, small * 0.5f, rgb, alpha);
                HudCardRenderer.drawOverlayCard(ctx, cx - big / 2, y + 1, big, big,
                        big * 0.5f, rgb, alpha);
                // Flat bottom
                HudCardRenderer.drawOverlayCard(ctx, x, y + size - s2, size, s2,
                        s2 * 0.3f, rgb, alpha);
            }
            case FOG -> {
                // Three horizontal lines of varying widths and offsets.
                int lineH = Math.max(1, size / 6);
                int spaceY = (size - lineH * 3) / 4;
                HudCardRenderer.drawOverlayCard(ctx, x,           y + spaceY,                   size,     lineH, lineH * 0.5f, rgb, alpha);
                HudCardRenderer.drawOverlayCard(ctx, x + s1,      y + spaceY * 2 + lineH,       size - s2, lineH, lineH * 0.5f, rgb, alpha * 0.85f);
                HudCardRenderer.drawOverlayCard(ctx, x,           y + spaceY * 3 + lineH * 2,   size - s1, lineH, lineH * 0.5f, rgb, alpha * 0.7f);
            }
            case FLAME -> {
                // Teardrop pointing up.
                int w = Math.max(3, size * 3 / 5);
                HudCardRenderer.drawOverlayCard(ctx, cx - w / 2, y + size / 3, w, size - size / 3,
                        w * 0.5f, rgb, alpha);
                for (int i = 0; i < size / 3; i++) {
                    int ww = Math.max(1, w - (size / 3 - i) * 2);
                    HudCardRenderer.drawOverlayCard(ctx, cx - ww / 2, y + i, ww, 1, 0.5f, rgb, alpha);
                }
            }
            case CROSSHAIR -> {
                HudCardRenderer.drawShaderOutline(ctx, x, y, size, size, size / 2.0f, 1.0f, alpha);
                int tick = Math.max(2, size / 4);
                HudCardRenderer.drawOverlayCard(ctx, cx - s1 / 2, y + 1, s1, tick, s1 * 0.5f, rgb, alpha);
                HudCardRenderer.drawOverlayCard(ctx, cx - s1 / 2, y + size - tick - 1, s1, tick, s1 * 0.5f, rgb, alpha);
                HudCardRenderer.drawOverlayCard(ctx, x + 1, cy - s1 / 2, tick, s1, s1 * 0.5f, rgb, alpha);
                HudCardRenderer.drawOverlayCard(ctx, x + size - tick - 1, cy - s1 / 2, tick, s1, s1 * 0.5f, rgb, alpha);
            }
            case HAND -> {
                // Palm: rounded rect + 3 small "fingers" on top.
                int palmH = size * 2 / 3;
                HudCardRenderer.drawOverlayCard(ctx, x, y + size - palmH, size, palmH,
                        size * 0.25f, rgb, alpha);
                int fingerW = Math.max(1, size / 5);
                int fingerH = size - palmH + 1;
                int gap = (size - fingerW * 3) / 2;
                for (int i = 0; i < 3; i++) {
                    HudCardRenderer.drawOverlayCard(ctx, x + i * (fingerW + gap),
                            y + size - palmH - fingerH, fingerW, fingerH, fingerW * 0.5f, rgb, alpha);
                }
            }
            case CURVE -> {
                // Stepped arc rising from bottom-left to top-right.
                int steps = size;
                for (int i = 0; i < steps; i++) {
                    float t = i / (float) Math.max(1, steps - 1);
                    int px = x + i;
                    int py = y + size - 1 - Math.round((float)(Math.sin(t * Math.PI * 0.5) * (size - 2)));
                    HudCardRenderer.drawOverlayCard(ctx, px, py, s1, s1, s1 * 0.5f, rgb, alpha);
                }
            }
            case WAVE -> {
                // Sine wave across the width.
                int steps = size;
                int amp = size / 3;
                for (int i = 0; i < steps; i++) {
                    float t = i / (float) Math.max(1, steps - 1);
                    int px = x + i;
                    int py = cy + Math.round((float)(Math.sin(t * Math.PI * 2) * amp));
                    HudCardRenderer.drawOverlayCard(ctx, px, py - s1 / 2, s1, s1, s1 * 0.5f, rgb, alpha);
                }
            }
            case PIN -> {
                // Map pin: circle top + downward triangle.
                int circ = Math.max(4, size * 2 / 3);
                HudCardRenderer.drawShaderOutline(ctx, cx - circ / 2, y, circ, circ,
                        circ / 2.0f, 1.0f, alpha);
                int dot = Math.max(2, circ / 3);
                HudCardRenderer.drawOverlayCard(ctx, cx - dot / 2, y + circ / 2 - dot / 2,
                        dot, dot, dot * 0.5f, rgb, alpha);
                // Triangle tail
                int tail = size - circ;
                for (int i = 0; i < tail; i++) {
                    int w = Math.max(1, tail - i);
                    HudCardRenderer.drawOverlayCard(ctx, cx - w / 2, y + circ + i, w, 1, 0.5f, rgb, alpha);
                }
            }
            case APPLE -> {
                // Round body + small stem.
                int body = size - s2;
                HudCardRenderer.drawOverlayCard(ctx, x, y + s2, body, body,
                        body * 0.45f, rgb, alpha);
                HudCardRenderer.drawOverlayCard(ctx, x + size - s2, y + s2, s2, body,
                        body * 0.45f, rgb, alpha);
                // Stem
                HudCardRenderer.drawOverlayCard(ctx, cx - s1 / 2, y, s1, s2, s1 * 0.5f, rgb, alpha);
            }
            case REFRESH -> {
                // Open circle (gap on top-right) + arrowhead.
                HudCardRenderer.drawShaderOutline(ctx, x, y, size, size, size / 2.0f, 1.2f, alpha);
                // Mask the gap by drawing background-coloured square — actually skip and just add arrowhead.
                // Arrowhead at top-right.
                int ar = Math.max(2, size / 4);
                HudCardRenderer.drawOverlayCard(ctx, x + size - ar, y, ar, s1, s1 * 0.5f, rgb, alpha);
                HudCardRenderer.drawOverlayCard(ctx, x + size - s1, y, s1, ar, s1 * 0.5f, rgb, alpha);
            }
            case LINK -> {
                // Two interlocking ovals (chain links).
                int oW = Math.max(3, size / 2);
                int oH = Math.max(3, size * 2 / 5);
                HudCardRenderer.drawShaderOutline(ctx, x, cy - oH / 2, oW, oH, oH * 0.5f, 1.0f, alpha);
                HudCardRenderer.drawShaderOutline(ctx, x + size - oW, cy - oH / 2, oW, oH,
                        oH * 0.5f, 1.0f, alpha);
            }
            case SUN -> {
                int discR = Math.max(2, size / 3);
                HudCardRenderer.drawOverlayCard(ctx, cx - discR, cy - discR, discR * 2, discR * 2,
                        discR, rgb, alpha);
                int rayLen = Math.max(1, size / 4);
                // 8 rays N/S/E/W + diagonals
                HudCardRenderer.drawOverlayCard(ctx, cx - s1 / 2, y, s1, rayLen, s1 * 0.5f, rgb, alpha);
                HudCardRenderer.drawOverlayCard(ctx, cx - s1 / 2, y + size - rayLen, s1, rayLen, s1 * 0.5f, rgb, alpha);
                HudCardRenderer.drawOverlayCard(ctx, x, cy - s1 / 2, rayLen, s1, s1 * 0.5f, rgb, alpha);
                HudCardRenderer.drawOverlayCard(ctx, x + size - rayLen, cy - s1 / 2, rayLen, s1, s1 * 0.5f, rgb, alpha);
            }
            case BLUR -> {
                // Three horizontal soft pills of decreasing opacity.
                int bH = Math.max(2, size / 5);
                int spaceY = (size - bH * 3) / 4;
                HudCardRenderer.drawOverlayCard(ctx, x, y + spaceY, size, bH, bH * 0.5f, rgb, alpha * 0.4f);
                HudCardRenderer.drawOverlayCard(ctx, x, y + spaceY * 2 + bH, size, bH, bH * 0.5f, rgb, alpha * 0.7f);
                HudCardRenderer.drawOverlayCard(ctx, x, y + spaceY * 3 + bH * 2, size, bH, bH * 0.5f, rgb, alpha);
            }
            case THEME -> {
                // Half-circle (sun) + crescent (moon) merged.
                HudCardRenderer.drawOverlayCard(ctx, x, y, size / 2, size, 0.5f, rgb, alpha);
                HudCardRenderer.drawShaderOutline(ctx, x, y, size, size, size / 2.0f, 1.0f, alpha);
            }
        }
    }

    private void drawCategoryIcon(DrawContext ctx, Category cat, int x, int y, int size, int color) {
        float alpha = ((color >>> 24) & 0xFF) / 255.0f;
        int rgb = color & 0x00FFFFFF | 0xFF000000;
        int cx = x + size / 2;
        int cy = y + size / 2;
        switch (cat) {
            case VISUALS -> {
                // Eye: outlined oval + filled pupil.
                int eyeH = Math.max(4, size * 3 / 5);
                int eyeY = cy - eyeH / 2;
                HudCardRenderer.drawShaderOutline(ctx, x, eyeY, size, eyeH, eyeH / 2.0f, 1.2f, alpha);
                int pupil = Math.max(3, size / 3);
                HudCardRenderer.drawOverlayCard(ctx, cx - pupil / 2, cy - pupil / 2,
                        pupil, pupil, pupil / 2.0f, rgb, alpha);
            }
            case HUD -> {
                // Rounded window with a header bar.
                int radius = Math.max(2, size / 4);
                HudCardRenderer.drawShaderOutline(ctx, x, y, size, size, radius, 1.2f, alpha);
                int barH = Math.max(2, size / 5);
                int barInset = Math.max(2, size / 6);
                HudCardRenderer.drawOverlayCard(ctx, x + barInset, y + barInset,
                        size - barInset * 2, barH, barH / 2.0f, rgb, alpha * 0.80f);
            }
            case UTILITIES -> {
                // Three even sliders with offset knobs.
                int line = Math.max(2, size / 6);
                int totalSpace = size - line * 3;
                int gap = Math.max(1, totalSpace / 2);
                int knob = Math.max(3, line + 2);
                int[] knobPos = { size / 3, size * 2 / 3, size / 2 };
                for (int i = 0; i < 3; i++) {
                    int yy = y + i * (line + gap);
                    HudCardRenderer.drawOverlayCard(ctx, x, yy, size, line, line / 2.0f, rgb, alpha * 0.65f);
                    int kx = x + knobPos[i] - knob / 2;
                    int ky = yy + line / 2 - knob / 2;
                    // Clamp knob fully inside icon bounds.
                    kx = Math.max(x, Math.min(x + size - knob, kx));
                    ky = Math.max(y, Math.min(y + size - knob, ky));
                    HudCardRenderer.drawOverlayCard(ctx, kx, ky, knob, knob, knob / 2.0f, rgb, alpha);
                }
            }
            case PVP -> {
                // Crosshair: outlined ring + 4 tick marks + center dot.
                HudCardRenderer.drawShaderOutline(ctx, x, y, size, size, size / 2.0f, 1.2f, alpha);
                int tick = Math.max(2, size / 5);
                int tw = Math.max(1, size / 8);
                // Top, bottom, left, right ticks inside the ring.
                HudCardRenderer.drawOverlayCard(ctx, cx - tw / 2, y + 1,                  tw, tick, tw / 2.0f, rgb, alpha);
                HudCardRenderer.drawOverlayCard(ctx, cx - tw / 2, y + size - tick - 1,    tw, tick, tw / 2.0f, rgb, alpha);
                HudCardRenderer.drawOverlayCard(ctx, x + 1,                cy - tw / 2,   tick, tw, tw / 2.0f, rgb, alpha);
                HudCardRenderer.drawOverlayCard(ctx, x + size - tick - 1,  cy - tw / 2,   tick, tw, tw / 2.0f, rgb, alpha);
                int dot = Math.max(2, size / 5);
                HudCardRenderer.drawOverlayCard(ctx, cx - dot / 2, cy - dot / 2,
                        dot, dot, dot / 2.0f, rgb, alpha);
            }
            case MENU -> {
                // Gear: outer ring, four square teeth inset toward the edges, central hub.
                int ringInset = Math.max(1, size / 10);
                HudCardRenderer.drawShaderOutline(ctx, x + ringInset, y + ringInset,
                        size - ringInset * 2, size - ringInset * 2,
                        (size - ringInset * 2) / 2.0f, 1.4f, alpha);
                int tooth = Math.max(2, size / 5);
                int tw = Math.max(2, size / 5);
                // Vertical teeth top + bottom
                HudCardRenderer.drawOverlayCard(ctx, cx - tw / 2, y,
                        tw, tooth, 0.5f, rgb, alpha);
                HudCardRenderer.drawOverlayCard(ctx, cx - tw / 2, y + size - tooth,
                        tw, tooth, 0.5f, rgb, alpha);
                // Horizontal teeth left + right
                HudCardRenderer.drawOverlayCard(ctx, x,                cy - tw / 2,
                        tooth, tw, 0.5f, rgb, alpha);
                HudCardRenderer.drawOverlayCard(ctx, x + size - tooth, cy - tw / 2,
                        tooth, tw, 0.5f, rgb, alpha);
                int hub = Math.max(3, size / 4);
                HudCardRenderer.drawOverlayCard(ctx, cx - hub / 2, cy - hub / 2,
                        hub, hub, hub / 2.0f, rgb, alpha);
            }
        }
    }

    // ---------- Mouse ----------

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) return true;
        if (click.button() != 0) return false;

        // App-rail (left strip) — top priority so it works from any view.
        if (hoveredRailIndex >= 0) {
            AppView clicked = AppView.values()[hoveredRailIndex];
            if (clicked != currentView) {
                currentView = clicked;
                // Reset drill-down so switching back to SETTINGS lands on the list.
                detailTarget = null;
                detailSettings.clear();
                contentScroll = 0;
                searchFocused = false;
            }
            return true;
        }

        // Search bar: focus on click in, clear on `×`, unfocus on click outside.
        if (detailTarget == null) {
            if (hoveredSearchClear) {
                searchQuery = "";
                contentScroll = 0;
                return true;
            }
            if (hoveredSearchBox) {
                searchFocused = true;
                return true;
            }
            // Anywhere else: drop focus.
            if (searchFocused) searchFocused = false;
        }

        // Header `<` closes the screen.
        if (hoveredBack) { this.close(); return true; }

        // Detail back chevron.
        if (detailTarget != null && hoveredDetailBack) {
            detailTarget = null;
            detailSettings.clear();
            return true;
        }

        // Sidebar category change resets drill-down.
        if (hoveredCategoryIndex >= 0) {
            Category cat = Category.values()[hoveredCategoryIndex];
            if (cat != selected) {
                selected = cat;
                detailTarget = null;
                detailSettings.clear();
                contentScroll = 0;
            }
            return true;
        }

        // Detail: enabled toggle.
        if (detailTarget != null) {
            FeatureEntry t = detailTarget;
            // Themes-detail: clicking a tile applies + persists the accent.
            if ("Themes".equals(t.name) && hoveredThemeIndex >= 0) {
                ru.suppelemen.vibevisuals.theme.AccentTheme picked =
                        ru.suppelemen.vibevisuals.theme.AccentTheme.values()[hoveredThemeIndex];
                VibeVisualsConfigManager.get().menu.accent = picked.name();
                VibeVisualsConfigManager.save();
                MenuTheme.applyAccent(picked.name());
                return true;
            }
            if (insideRect(click.x(), click.y(), t.detailSwitchX, t.detailSwitchY, t.detailSwitchW, t.detailSwitchH, dp(3))) {
                t.enabledSetter.accept(!t.enabled.get());
                saveAndReload();
                return true;
            }
            if (hoveredSettingRow != null) {
                SettingRow r = hoveredSettingRow;
                if (r.field.getType() == boolean.class
                        && insideRect(click.x(), click.y(), r.switchX, r.switchY, r.switchW, r.switchH, dp(3))) {
                    writeBool(t.config, r.field, !readBool(t.config, r.field));
                    saveAndReload();
                    return true;
                }
                // Slider — click anywhere on the track jumps the thumb and starts a drag.
                if ((r.field.getType() == int.class || r.field.getType() == float.class)
                        && !isColorField(r.field) && r.hint != null
                        && insideRect(click.x(), click.y(), r.sliderX, r.sliderY - dp(4),
                                       r.sliderW, r.sliderH + dp(8), dp(2))) {
                    draggingSlider = r;
                    float v = sliderValueFor(r, click.x());
                    writeNumber(t.config, r.field, v);
                    return true;
                }
            }
            return true; // consume clicks inside detail page
        }

        // List view: switch toggles, row body opens detail.
        if (hoveredRow != null) {
            if (hoveredRow.customAction != null) {
                hoveredRow.customAction.run();
                return true;
            }
            if (hoveredOnSwitch) {
                hoveredRow.enabledSetter.accept(!hoveredRow.enabled.get());
                saveAndReload();
                return true;
            } else {
                openDetail(hoveredRow);
                return true;
            }
        }
        return false;
    }

    private void openDetail(FeatureEntry f) {
        detailTarget = f;
        rebuildDetailSettings(f);
    }

    private void rebuildDetailSettings(FeatureEntry f) {
        detailSettings.clear();
        if (f == null || f.config == null) return;
        for (Field field : f.config.getClass().getFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            if (!isEditable(field)) continue;
            // Skip the "enabled" boolean — represented by the top toggle.
            if (field.getName().equalsIgnoreCase("enabled")) continue;
            detailSettings.add(new SettingRow(field));
        }
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (draggingSlider != null && detailTarget != null) {
            float v = sliderValueFor(draggingSlider, click.x());
            writeNumber(detailTarget.config, draggingSlider.field, v);
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (draggingSlider != null) {
            // Persist + re-apply HUD only once the drag finishes — keeps the file
            // write rate sane.
            saveAndReload();
            draggingSlider = null;
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        // Scroll the content area when in list view.
        int pw = panelW();
        int ph = panelH();
        int px = (width - pw) / 2;
        int py = (height - ph) / 2;
        int cx = px + dp(SIDEBAR_W);
        int cy = py + dp(HEADER_H);
        if (detailTarget == null
                && mouseX >= cx && mouseX <= px + pw
                && mouseY >= cy && mouseY <= py + ph) {
            contentScroll = Math.max(0, Math.min(contentMaxScroll,
                    contentScroll - (int) Math.round(verticalAmount * 18.0)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharInput input) {
        if (searchFocused && detailTarget == null && input.isValidChar()) {
            int cp = input.codepoint();
            if (cp >= ' ' && cp != 127 && searchQuery.length() < 40) {
                searchQuery += new String(Character.toChars(cp));
                contentScroll = 0;
            }
            return true;
        }
        return super.charTyped(input);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyInput input) {
        int keyCode = input.key();
        int modifiers = input.modifiers();

        // Esc: clear query → drop focus → fall through to default close behaviour.
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            if (searchFocused && !searchQuery.isEmpty()) {
                searchQuery = "";
                contentScroll = 0;
                return true;
            }
            if (searchFocused) {
                searchFocused = false;
                return true;
            }
        }
        if (searchFocused && detailTarget == null) {
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE) {
                if (!searchQuery.isEmpty()) {
                    searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                    contentScroll = 0;
                }
                return true;
            }
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER
                    || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
                searchFocused = false;
                return true;
            }
        }
        // Ctrl/Cmd+F focuses search like in any modern app.
        if ((modifiers & (org.lwjgl.glfw.GLFW.GLFW_MOD_CONTROL | org.lwjgl.glfw.GLFW.GLFW_MOD_SUPER)) != 0
                && keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_F) {
            searchFocused = true;
            return true;
        }
        return super.keyPressed(input);
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
    private static boolean readBool(Object cfg, Field f) {
        try { return f.getBoolean(cfg); } catch (IllegalAccessException e) { return false; }
    }
    private static void writeBool(Object cfg, Field f, boolean v) {
        try { f.setBoolean(cfg, v); } catch (IllegalAccessException ignored) {}
    }
    private static float readFloat(Object cfg, Field f) {
        try {
            if (f.getType() == float.class) return f.getFloat(cfg);
            if (f.getType() == int.class) return (float) f.getInt(cfg);
            return 0f;
        } catch (IllegalAccessException e) { return 0f; }
    }
    private static void writeNumber(Object cfg, Field f, float v) {
        try {
            if (f.getType() == float.class) f.setFloat(cfg, v);
            else if (f.getType() == int.class) f.setInt(cfg, Math.round(v));
        } catch (IllegalAccessException ignored) {}
    }
    private static float snap(float v, float step) {
        if (step <= 0f) return v;
        return Math.round(v / step) * step;
    }
    private static String formatValue(float v, SliderHint h) {
        if (h.integer) return Integer.toString(Math.round(v));
        // 2 decimals for small steps, 1 for medium, 0 for whole.
        if (h.step >= 1f) return Integer.toString(Math.round(v));
        if (h.step >= 0.1f) return String.format(Locale.ROOT, "%.1f", v);
        return String.format(Locale.ROOT, "%.2f", v);
    }
    private static String fieldToString(Object cfg, Field f) {
        try {
            Object v = f.get(cfg);
            return v == null ? "" : v.toString();
        } catch (IllegalAccessException e) { return ""; }
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

    private static boolean insideRect(double x, double y, int rx, int ry, int rw, int rh, int pad) {
        return x >= rx - pad && x <= rx + rw + pad && y >= ry - pad && y <= ry + rh + pad;
    }

    // ---------- Glass + blur ----------

    private void renderDim(DrawContext context, float eased) {
        boolean light = MenuTheme.current == MenuTheme.ThemeMode.LIGHT;
        boolean blur = VibeVisualsConfigManager.get().menu.liquidGlassBlur;
        // Light theme keeps the world very visible; dark theme is dim but not pitch.
        int baseAlpha = blur ? (light ? 0x0E : 0x54) : (light ? 0x30 : 0x88);
        int alpha = (int) (baseAlpha * eased) & 0xFF;
        int rgb = MenuTheme.DIM_RGB & 0x00FFFFFF;
        context.fill(0, 0, width, height, (alpha << 24) | rgb);
    }

    private void applyMenuBlur(DrawContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        try {
            if (mc != null && mc.gameRenderer != null) {
                mc.gameRenderer.renderBlur();
                return;
            }
        } catch (Throwable ignored) {}
        try { this.applyBlur(context); } catch (Throwable ignored) {}
    }

    /** Reusable HudVisualSettings for the SDF-shader card render path. */
    private static final ru.suppelemen.vibevisuals.theme.HudVisualSettings SDF_CARD_SETTINGS =
            new ru.suppelemen.vibevisuals.theme.HudVisualSettings();
    static { SDF_CARD_SETTINGS.renderType = ru.suppelemen.vibevisuals.theme.HudCardRenderType.LIQUID_GLASS; }

    /** Smooth shader-AA rounded card — exact same code path the HUD uses, so
     *  corners are pixel-perfect at any size (not the 9-slice texture which
     *  shows tiny stretching artefacts at extreme radii). */
    private static void drawSdfCard(DrawContext ctx, int x, int y, int w, int h,
                                     int radius, float opacity) {
        if (w <= 0 || h <= 0 || opacity <= 0f) return;
        SDF_CARD_SETTINGS.radius = radius;
        SDF_CARD_SETTINGS.opacity = opacity;
        HudCardRenderer.drawCard(ctx, x, y, w, h, SDF_CARD_SETTINGS);
    }

    /**
     * Per-row rounded rectangle fill — bypasses the 9-slice card texture used
     * by {@link HudCardRenderer#drawOverlayCard} so the corner geometry stays
     * mathematically symmetric even when {@code radius ≈ width/2} (which the
     * texture path can render slightly off-centre due to corner-patch
     * stretching). Used for elements where horizontal centering must be exact
     * — the app-rail and its buttons.
     */
    private static void fillRoundedSymmetric(DrawContext ctx, int x, int y, int w, int h,
                                              int radius, int rgb, float opacity) {
        if (w <= 0 || h <= 0 || opacity <= 0f) return;
        int alpha = Math.max(0, Math.min(255, Math.round(opacity * 255f)));
        int color = (alpha << 24) | (rgb & 0x00FFFFFF);
        radius = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
        if (radius <= 0) {
            ctx.fill(x, y, x + w, y + h, color);
            return;
        }
        for (int row = 0; row < h; row++) {
            int inset;
            if (row < radius) {
                inset = symmCornerInset(row, radius);
            } else if (row >= h - radius) {
                inset = symmCornerInset(h - row - 1, radius);
            } else {
                inset = 0;
            }
            ctx.fill(x + inset, y + row, x + w - inset, y + row + 1, color);
        }
    }

    private static int symmCornerInset(int row, int radius) {
        double dy = radius - row - 0.5;
        double dx = radius - Math.sqrt(Math.max(0.0, radius * radius - dy * dy));
        return Math.max(0, (int) Math.ceil(dx));
    }

    private void drawLiquidGlass(DrawContext ctx, int x, int y, int w, int h,
                                  int radius, int material, float materialOpacity, float eased) {
        if (w <= 0 || h <= 0) return;
        HudCardRenderer.drawOverlayCard(ctx, x, y, w, h, radius, material, materialOpacity * eased);
        HudCardRenderer.drawShaderOutline(ctx, x, y, w, h, radius, 0.55f,
                MenuTheme.GLASS_OUTLINE_ALPHA * eased);
    }


    // ---------- Scale + text helpers ----------

    private float dpScale() {
        MinecraftClient mc = client != null ? client : MinecraftClient.getInstance();
        if (mc == null) return 1.0f;
        float fbW = mc.getWindow().getWidth();
        float fbH = mc.getWindow().getHeight();
        float layoutScale = Math.min(fbW / REFERENCE_WIDTH, fbH / REFERENCE_HEIGHT);
        double guiScale = mc.getWindow().getScaleFactor();
        if (guiScale <= 0.0) guiScale = 1.0;
        return Math.max((float) (layoutScale / guiScale), 0.45f);
    }

    private int dp(float v) { return Math.round(v * dpScale()); }
    private int panelW() { return Math.min(dp(PANEL_W), width - dp(40)); }
    private int panelH() { return Math.min(dp(PANEL_H), height - dp(40)); }

    /** Global text-only shrink — does NOT affect rows/icons/panel sizing. */
    private static final float TEXT_ONLY_SCALE = 0.88f;

    /** Resolve a design-px text size into actual atlas px after dp scaling.
     *  Floored at 4 so section captions can render visibly smaller than body
     *  labels (body lives at ~6-8 px, captions at 4-5 px). */
    private int targetGlyphSize(float pxSize) {
        int scaled = Math.round(pxSize * dpScale() * TEXT_ONLY_SCALE);
        return Math.max(4, scaled);
    }

    private int textWidth(String text, float pxSize) {
        return SmoothText.measureText(text, targetGlyphSize(pxSize));
    }
    private int textHeight(float pxSize) {
        // ~0.72 of em to mirror what the matrix-scaled vanilla path returned —
        // keeps existing (rowH - textHeight)/2 vertical-centring math intact.
        return Math.round(targetGlyphSize(pxSize) * 0.72f);
    }

    private void drawScaledText(DrawContext ctx, String text, int x, int y, float pxSize, int color) {
        // SmoothText draws a full glyph CELL whose visible cap sits ~PAD*scale
        // below the cell's top, so the caller-supplied y (which assumes "y =
        // top of cap") would render the text slightly lower than icons.
        // Lift it by the padding-equivalent so cap-center aligns with row centre.
        // SemiBold-by-default for the whole menu — gives a denser, more premium
        // type feel. Caller can still pick the regular atlas explicitly via
        // SmoothText.drawText(... false).
        int g = targetGlyphSize(pxSize);
        int nudgeUp = Math.round(g * 0.27f);
        SmoothText.drawTextBold(ctx, text, x, y - nudgeUp, g, color);
    }

    /** Tracked variant — inserts {@code trackingEm × pxSize} between glyphs. */
    private void drawScaledTextTracked(DrawContext ctx, String text, int x, int y,
                                        float pxSize, int color, float trackingEm) {
        int g = targetGlyphSize(pxSize);
        int nudgeUp = Math.round(g * 0.27f);
        // Tracked text typically goes on section headers ("MODULES", "SYSTEM");
        // SemiBold the only weight currently exposed by drawTextTracked path
        // is regular — keep section captions on regular so they read as a
        // calmer caption layer next to the SemiBold body.
        SmoothText.drawTextTracked(ctx, text, x, y - nudgeUp, g, color, trackingEm);
    }

    /** Pick the localised description for the current MC language.
     *  Rule: ru-locale → Russian (English fallback if Russian missing);
     *        every other locale → English (Russian fallback if English missing). */
    private static String localizedDescription(FeatureEntry f) {
        boolean ru = false;
        var mc = MinecraftClient.getInstance();
        if (mc != null && mc.options != null) {
            String lang = mc.options.language;
            ru = lang != null && lang.toLowerCase().startsWith("ru");
        }
        if (ru) {
            return (f.descriptionRu != null && !f.descriptionRu.isEmpty()) ? f.descriptionRu : f.descriptionEn;
        }
        return (f.descriptionEn != null && !f.descriptionEn.isEmpty()) ? f.descriptionEn : f.descriptionRu;
    }

    private static float easeOutCubic(float t) { float p = 1.0f - t; return 1.0f - p * p * p; }

    // ---------- Types ----------

    private enum Section {
        MODULES("MODULES"),
        SYSTEM("SYSTEM");

        final String label;
        Section(String label) { this.label = label; }
    }

    private enum Category {
        VISUALS  ("Visuals",   Section.MODULES),
        HUD      ("HUD",       Section.MODULES),
        UTILITIES("Utilities", Section.MODULES),
        PVP      ("PvP",       Section.MODULES),
        MENU     ("Menu",      Section.SYSTEM);

        final String label;
        final Section section;
        Category(String label, Section section) { this.label = label; this.section = section; }
    }

    private static class FeatureEntry {
        final Category category;
        /** Module name — always English (acts as the identifier in the UI). */
        final String name;
        final String descriptionEn;
        final String descriptionRu;
        final ModuleIcon icon;
        final Supplier<Boolean> enabled;
        final Consumer<Boolean> enabledSetter;
        final Object config;
        /** Optional alternate behaviour — when set, clicking the row triggers
         *  this instead of toggling/opening details. Used for entries that act
         *  as buttons (e.g. "Profiles…"). */
        final Runnable customAction;
        float hoverProgress;
        float knobAnim;
        float detailKnobAnim;
        int x, y, width, height;
        int switchX, switchY, switchWidth, switchHeight;
        int detailSwitchX, detailSwitchY, detailSwitchW, detailSwitchH;

        FeatureEntry(Category category, String name, String descriptionEn, String descriptionRu,
                     ModuleIcon icon,
                     Supplier<Boolean> enabled, Consumer<Boolean> enabledSetter, Object config) {
            this(category, name, descriptionEn, descriptionRu, icon, enabled, enabledSetter, config, null);
        }

        FeatureEntry(Category category, String name, String descriptionEn, String descriptionRu,
                     ModuleIcon icon,
                     Supplier<Boolean> enabled, Consumer<Boolean> enabledSetter, Object config,
                     Runnable customAction) {
            this.category = category;
            this.name = name;
            this.descriptionEn = descriptionEn;
            this.descriptionRu = descriptionRu;
            this.icon = icon;
            this.enabled = enabled;
            this.enabledSetter = enabledSetter;
            this.config = config;
            this.customAction = customAction;
        }
    }

    /** Per-module glyphs. Reused for similar modules (e.g. POTION for both
     *  Potions and AutoPotion) — keeps the visual vocabulary tight. */
    private enum ModuleIcon {
        POTION, CLOCK, KEY, BAR, GRID, SHIELD, HOTBAR, HEART,
        SWORD, CROWN, TARGET, DROP, SPARK, ARROW_UP, CLOUD, FOG,
        FLAME, CROSSHAIR, HAND, CURVE, WAVE, PIN, APPLE, REFRESH,
        LINK, SUN, BLUR, THEME
    }

    private static class SettingRow {
        final Field field;
        float knobAnim;
        int x, y, width, height;
        int switchX, switchY, switchW, switchH;
        // Slider geometry (for int/float).
        int sliderX, sliderY, sliderW, sliderH;
        SliderHint hint;
        // Color swatch.
        int swatchX, swatchY, swatchSize;

        SettingRow(Field field) { this.field = field; }
    }

    /** Range/step descriptor inferred for a slider-editable field. */
    private static final class SliderHint {
        final float min, max, step;
        final boolean integer;
        SliderHint(float min, float max, float step, boolean integer) {
            this.min = min; this.max = max; this.step = step; this.integer = integer;
        }
    }

    /** Heuristic bounds based on field name + type. Generous defaults — most
     *  config values pin to common semantic ranges (opacity ∈ [0,1], etc). */
    private static SliderHint hintFor(Field f, float currentValue) {
        String n = f.getName().toLowerCase(Locale.ROOT);
        boolean isInt = (f.getType() == int.class);
        if (n.contains("opacity") || n.contains("alpha") || n.contains("strength"))
            return new SliderHint(0f, 1f, 0.01f, false);
        if (n.contains("scale") || (n.endsWith("size") && !isInt))
            return new SliderHint(0.25f, 3f, 0.05f, false);
        if (n.contains("radius"))
            return new SliderHint(0f, 30f, isInt ? 1f : 0.5f, isInt);
        if (n.contains("padding") || n.contains("gap") || n.contains("inset")
                || n.contains("margin") || n.contains("border"))
            return new SliderHint(0f, 40f, 1f, true);
        if (n.contains("duration") || n.contains("delay") || n.contains("ms"))
            return new SliderHint(0f, 5000f, 50f, true);
        if (n.contains("speed") || n.contains("ratio"))
            return new SliderHint(0f, 2f, 0.05f, false);
        if (isInt) {
            // Generic int: range around current value, ±64.
            float c = Math.round(currentValue);
            return new SliderHint(c - 64f, c + 64f, 1f, true);
        }
        // Generic float.
        return new SliderHint(0f, 1f, 0.01f, false);
    }

    private static boolean isColorField(Field f) {
        if (f.getType() != int.class) return false;
        String n = f.getName().toLowerCase(Locale.ROOT);
        return n.endsWith("color") || n.contains("color") || n.endsWith("colour");
    }
}
