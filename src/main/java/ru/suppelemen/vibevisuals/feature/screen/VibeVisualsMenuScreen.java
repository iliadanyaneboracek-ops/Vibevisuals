package ru.suppelemen.vibevisuals.feature.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import ru.suppelemen.vibevisuals.VibeVisualsClient;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;
import ru.suppelemen.vibevisuals.core.hud.HudManager;
import ru.suppelemen.vibevisuals.theme.HudCardRenderType;
import ru.suppelemen.vibevisuals.theme.MenuTheme;
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

    private static final StyleSpriteSource MENU_FONT =
            new StyleSpriteSource.Font(Identifier.of(VibeVisualsClient.MOD_ID, "clickgui"));

    // Reference frame.
    private static final float REFERENCE_WIDTH = 1920.0f;
    private static final float REFERENCE_HEIGHT = 1080.0f;
    // Font atlas is rasterised at this height (see clickgui.json "size").
    // A larger native size + higher oversample give crisper text once we
    // scale glyphs down via matrix scaling at draw time.
    private static final float FONT_NATIVE_PX = 20.0f;

    // Typography (design px).
    private static final float TEXT_TITLE = 14.0f;
    private static final float TEXT_BRAND = 18.0f;
    private static final float TEXT_BRAND_V = 22.0f;
    private static final float TEXT_CATEGORY = 12.0f;
    private static final float TEXT_SECTION = 9.5f;
    private static final float TEXT_ROW = 12.5f;
    private static final float TEXT_DETAIL_TITLE = 18.0f;
    private static final float TEXT_DETAIL_LABEL = 11.5f;
    private static final float TEXT_BACK = 11.5f;

    // Brand block above the panel.
    private static final int BRAND_ICON = 30;
    private static final int BRAND_GAP_X = 12;
    private static final int BRAND_BOTTOM_GAP = 16;

    // Panel layout (design px).
    private static final int PANEL_W = 624;
    private static final int PANEL_H = 384;
    private static final int PANEL_RADIUS = 24;
    private static final int HEADER_H = 44;
    private static final int SIDEBAR_W = 168;
    private static final int SIDEBAR_ROW_H = 30;
    private static final int SIDEBAR_ROW_RADIUS = 8;
    private static final int SIDEBAR_PILL_PAD = 8;
    private static final int SECTION_HEADER_GAP = 14;
    private static final int CATEGORY_ICON = 16;
    private static final int CATEGORY_ICON_GAP = 10;
    private static final int PAD_X = 18;

    // Module list rows (right side).
    private static final int ROW_H = 38;
    private static final int ROW_GAP = 6;
    private static final int ROW_RADIUS = 10;
    private static final int ROW_PAD_X = 14;

    // Apple-style switch.
    private static final int SWITCH_W = 32;
    private static final int SWITCH_H = 18;

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
        features.add(new FeatureEntry(Category.HUD, "Potions",        () -> c.potionsCard.enabled,    v -> c.potionsCard.enabled = v, c.potionsCard));
        features.add(new FeatureEntry(Category.HUD, "Cooldowns",      () -> c.cooldownsCard.enabled,  v -> c.cooldownsCard.enabled = v, c.cooldownsCard));
        features.add(new FeatureEntry(Category.HUD, "Hot Keys",       () -> c.hotKeysCard.enabled,    v -> c.hotKeysCard.enabled = v, c.hotKeysCard));
        features.add(new FeatureEntry(Category.HUD, "Top Bar",        () -> c.topBar.enabled,         v -> c.topBar.enabled = v, c.topBar));
        features.add(new FeatureEntry(Category.HUD, "Inventory HUD",  () -> c.inventoryHud.enabled,   v -> c.inventoryHud.enabled = v, c.inventoryHud));
        features.add(new FeatureEntry(Category.HUD, "Armor HUD",      () -> c.armorHud.enabled,       v -> c.armorHud.enabled = v, c.armorHud));
        features.add(new FeatureEntry(Category.HUD, "Custom Hotbar",  () -> c.hotbar.enabled,         v -> c.hotbar.enabled = v, c.hotbar));
        features.add(new FeatureEntry(Category.PVP, "PvP Combat",     () -> c.pvpCard.enabled,        v -> c.pvpCard.enabled = v, c.pvpCard));
        features.add(new FeatureEntry(Category.PVP, "Target ESP",     () -> c.targetEsp.enabled,      v -> c.targetEsp.enabled = v, c.targetEsp));
        features.add(new FeatureEntry(Category.PVP, "Saturation",     () -> c.saturationDisplay.enabled, v -> c.saturationDisplay.enabled = v, c.saturationDisplay));
        features.add(new FeatureEntry(Category.PVP, "Crit Hit Sound", () -> c.customHitSound.enabled, v -> c.customHitSound.enabled = v, c.customHitSound));
        features.add(new FeatureEntry(Category.PVP, "Shift Up",       () -> c.shiftUp.enabled,        v -> c.shiftUp.enabled = v, c.shiftUp));
        features.add(new FeatureEntry(Category.VISUALS, "Sky Color",   () -> c.visualEffects.skyColorEnabled,        v -> c.visualEffects.skyColorEnabled = v, c.visualEffects));
        features.add(new FeatureEntry(Category.VISUALS, "Fog Color",   () -> c.visualEffects.fogColorEnabled,        v -> c.visualEffects.fogColorEnabled = v, c.visualEffects));
        features.add(new FeatureEntry(Category.VISUALS, "Particles",   () -> c.visualEffects.customParticlesEnabled, v -> c.visualEffects.customParticlesEnabled = v, c.visualEffects));
        features.add(new FeatureEntry(Category.VISUALS, "Screen Fire", () -> c.fireOverlay.enabled,                  v -> c.fireOverlay.enabled = v, c.fireOverlay));
        features.add(new FeatureEntry(Category.VISUALS, "Crosshair",   () -> c.customCrosshair.enabled,              v -> c.customCrosshair.enabled = v, c.customCrosshair));
        features.add(new FeatureEntry(Category.VISUALS, "Custom Hand", () -> c.customHand.enabled,                   v -> c.customHand.enabled = v, c.customHand));
        features.add(new FeatureEntry(Category.UTILITIES, "Projectile Path", () -> c.projectilePrediction.enabled, v -> c.projectilePrediction.enabled = v, c.projectilePrediction));
        features.add(new FeatureEntry(Category.UTILITIES, "HUD Animations",  () -> c.hudAnimations.enabled,        v -> c.hudAnimations.enabled = v, c.hudAnimations));
        features.add(new FeatureEntry(Category.UTILITIES, "Markers",         () -> c.markers.enabled,              v -> c.markers.enabled = v, c.markers));
        features.add(new FeatureEntry(Category.UTILITIES, "AutoEat",         () -> c.autoEat.enabled,              v -> c.autoEat.enabled = v, c.autoEat));
        features.add(new FeatureEntry(Category.UTILITIES, "AutoPotion",      () -> c.autoPotion.enabled,           v -> c.autoPotion.enabled = v, c.autoPotion));
        features.add(new FeatureEntry(Category.UTILITIES, "AutoRespawn",     () -> c.autoRespawn.enabled,          v -> c.autoRespawn.enabled = v, c.autoRespawn));
        features.add(new FeatureEntry(Category.UTILITIES, "Tape Mouse",      () -> c.tapeMouse.enabled,            v -> c.tapeMouse.enabled = v, c.tapeMouse));
        features.add(new FeatureEntry(Category.UTILITIES, "Full Bright",
                () -> c.fullBrightStrength > 0.0f,
                v -> c.fullBrightStrength = v ? Math.max(0.6f, c.fullBrightStrength) : 0.0f, c));
        features.add(new FeatureEntry(Category.MENU, "Liquid Glass Blur",
                () -> c.menu.liquidGlassBlur, v -> c.menu.liquidGlassBlur = v, c.menu));
        features.add(new FeatureEntry(Category.MENU, "Light Theme",
                () -> "LIGHT".equalsIgnoreCase(c.menu.theme),
                v -> { c.menu.theme = v ? "LIGHT" : "DARK"; MenuTheme.applyTheme(c.menu.theme); }, c.menu));
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

        drawLiquidGlass(context, px, py, pw, ph, dp(PANEL_RADIUS),
                MenuTheme.MATERIAL_PANEL, MenuTheme.MATERIAL_OPACITY_PANEL, eased);

        // Advance drill-down animation.
        float target = detailTarget != null ? 1.0f : 0.0f;
        detailSlide += (target - detailSlide) * 0.28f;

        renderHeader(context, mouseX, mouseY, eased, px, py, pw);
        renderSidebar(context, mouseX, mouseY, eased, px, py, ph);
        renderContent(context, mouseX, mouseY, eased, px, py, pw, ph);

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

    private void renderBrandAbovePanel(DrawContext ctx, float eased, int px, int py, int pw) {
        int icon = dp(BRAND_ICON);
        int gap = dp(BRAND_GAP_X);
        int textW = textWidth("VibeVisuals", TEXT_BRAND);
        int blockW = icon + gap + textW;
        int blockX = px + (pw - blockW) / 2;
        int blockTop = py - brandBlockHeight();
        int regionH = brandBlockHeight() - dp(BRAND_BOTTOM_GAP);
        int iconY = blockTop + (regionH - icon) / 2;
        int textY = blockTop + (regionH - textHeight(TEXT_BRAND)) / 2;
        drawVLogo(ctx, blockX, iconY, icon, eased);
        drawScaledText(ctx, "VibeVisuals", blockX + icon + gap, textY, TEXT_BRAND,
                MenuTheme.withAlpha(MenuTheme.TEXT_PRIMARY, eased));
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
                drawScaledText(ctx, section.label,
                        pillX + dp(10), cursorY + dp(2), TEXT_SECTION,
                        MenuTheme.withAlpha(MenuTheme.TEXT_NEUTRAL, SECTION_HEADER_ALPHA * eased));
                cursorY += dp(SECTION_HEADER_GAP);
                lastSection = section;
            }

            int rowY = cursorY;
            boolean hov = mx >= pillX && mx <= pillX + pillW
                    && my >= rowY && my <= rowY + rowH;
            if (hov) hoveredCategoryIndex = i;

            boolean isSelected = cat == selected;
            if (isSelected) {
                drawLiquidGlass(ctx, pillX, rowY, pillW, rowH, dp(SIDEBAR_ROW_RADIUS),
                        MenuTheme.MATERIAL_CARD_ACTIVE,
                        MenuTheme.MATERIAL_OPACITY_CARD_ACTIVE + 0.05f, eased);
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
                iconColor = MenuTheme.withAlpha(MenuTheme.TEXT_PRIMARY, eased);
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
        int listTop = cy + dp(12);
        int listBot = cy + chh - dp(12);

        // Collect features in current category.
        List<FeatureEntry> rows = new ArrayList<>();
        for (FeatureEntry f : features) if (f.category == selected) rows.add(f);

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

            // Row hover background (only when hovering NOT over switch — to hint drill-down).
            if (f.hoverProgress > 0.02f && !onSwitch) {
                drawLiquidGlass(ctx, f.x, f.y, f.width, f.height, dp(ROW_RADIUS),
                        MenuTheme.MATERIAL_CARD,
                        (MenuTheme.MATERIAL_OPACITY_CARD * 0.6f) * f.hoverProgress + 0.04f, eased);
            }

            boolean enabled = f.enabled.get();
            int labelColor = enabled
                    ? MenuTheme.withAlpha(MenuTheme.TEXT_PRIMARY, eased)
                    : MenuTheme.withAlpha(MenuTheme.TEXT_NEUTRAL, 0.55f * eased);
            int labelY = rowY + (rowH - textHeight(TEXT_ROW)) / 2;
            drawScaledText(ctx, f.name, rowX + dp(ROW_PAD_X), labelY, TEXT_ROW, labelColor);

            // Chevron `>` to the left of the switch — drill hint.
            int chevX = f.switchX - dp(14);
            int chevY = rowY + (rowH - dp(10)) / 2;
            int chevColor = MenuTheme.withAlpha(MenuTheme.TEXT_NEUTRAL,
                    (0.30f + f.hoverProgress * 0.30f) * eased);
            drawChevronRight(ctx, chevX, chevY, dp(10), chevColor);

            // Apple-style switch.
            f.knobAnim += ((enabled ? 1.0f : 0.0f) - f.knobAnim) * MenuTheme.KNOB_LERP;
            drawAppleSwitch(ctx, f.switchX, f.switchY, f.switchWidth, f.switchHeight,
                    f.knobAnim, eased);
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
                ? MenuTheme.withAlpha(MenuTheme.TEXT_PRIMARY, eased)
                : MenuTheme.withAlpha(MenuTheme.TEXT_NEUTRAL, 0.70f * eased);
        drawChevronLeft(ctx, backX, backY, backSize, backColor);
        drawScaledText(ctx, "Back", backX + backSize + dp(6),
                backY + (backSize - textHeight(TEXT_BACK)) / 2,
                TEXT_BACK, backColor);

        // Title row.
        int titleY = backY + backSize + dp(10);
        drawScaledText(ctx, target.name, cx + padX, titleY, TEXT_DETAIL_TITLE,
                MenuTheme.withAlpha(MenuTheme.TEXT_PRIMARY, eased));

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
            } else {
                // Non-boolean: show value as text on right (read-only for now).
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

    private void drawSettingRowBg(DrawContext ctx, int x, int y, int w, int h,
                                   int radius, float eased) {
        drawLiquidGlass(ctx, x, y, w, h, radius,
                MenuTheme.MATERIAL_CARD,
                MenuTheme.MATERIAL_OPACITY_CARD + 0.05f, eased);
    }

    // ---------- Apple switch + icons + chevrons ----------

    private void drawAppleSwitch(DrawContext ctx, int x, int y, int w, int h, float knob, float eased) {
        boolean light = MenuTheme.current == MenuTheme.ThemeMode.LIGHT;
        // Neutral palette — no purple.  Track OFF is muted, track ON is a
        // bright neutral (white in dark mode, near-black in light mode).
        int trackOff = light ? 0xFFB6B6BD : 0xFF34344A;
        int trackOn  = light ? 0xFF1F1F2A : 0xFFE9E9F2;
        int track = MenuTheme.lerpColor(trackOff, trackOn, knob);
        HudCardRenderer.drawOverlayCard(ctx, x, y, w, h, h / 2.0f, track, 0.92f * eased);
        HudCardRenderer.drawShaderOutline(ctx, x, y, w, h, h / 2.0f, 0.5f, 0.20f * eased);
        int knobPad = Math.max(2, dp(2));
        int knobSize = h - knobPad * 2;
        int knobMinX = x + knobPad;
        int knobMaxX = x + w - knobSize - knobPad;
        int knobX = Math.round(knobMinX + (knobMaxX - knobMinX) * knob);
        // Knob colour flips with theme so it stays visible against the track.
        int knobOff = light ? 0xFFFFFFFF : 0xFFFFFFFF;
        int knobOn  = light ? 0xFFFFFFFF : 0xFF1A1A26;
        int knobColor = MenuTheme.lerpColor(knobOff, knobOn, knob);
        HudCardRenderer.drawOverlayCard(ctx, knobX, y + knobPad,
                knobSize, knobSize, knobSize / 2.0f, knobColor, eased);
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
            }
            return true; // consume clicks inside detail page
        }

        // List view: switch toggles, row body opens detail.
        if (hoveredRow != null) {
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

    private float textScale(float pxSize) { return dpScale() * (pxSize / FONT_NATIVE_PX); }
    private int textWidth(String text, float pxSize) {
        return Math.round(textRenderer.getWidth(text) * textScale(pxSize));
    }
    private int textHeight(float pxSize) {
        // Inter cap height ≈ 0.73 of em. Returning a value close to cap height
        // (not the full em box) makes (rowH - textHeight) / 2 give a visually
        // centred result instead of one biased toward the top.
        return Math.round(pxSize * 0.72f * dpScale());
    }

    private void drawScaledText(DrawContext ctx, String text, int x, int y, float pxSize, int color) {
        float scale = textScale(pxSize);
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate((float) x, (float) y);
        ctx.getMatrices().scale(scale, scale);
        ctx.drawText(textRenderer,
                Text.literal(text).styled(s -> s.withFont(MENU_FONT)),
                0, 0, color, false);
        ctx.getMatrices().popMatrix();
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
        final String name;
        final Supplier<Boolean> enabled;
        final Consumer<Boolean> enabledSetter;
        final Object config;
        float hoverProgress;
        float knobAnim;
        float detailKnobAnim;
        int x, y, width, height;
        int switchX, switchY, switchWidth, switchHeight;
        int detailSwitchX, detailSwitchY, detailSwitchW, detailSwitchH;

        FeatureEntry(Category category, String name, Supplier<Boolean> enabled,
                     Consumer<Boolean> enabledSetter, Object config) {
            this.category = category;
            this.name = name;
            this.enabled = enabled;
            this.enabledSetter = enabledSetter;
            this.config = config;
        }
    }

    private static class SettingRow {
        final Field field;
        float knobAnim;
        int x, y, width, height;
        int switchX, switchY, switchW, switchH;

        SettingRow(Field field) { this.field = field; }
    }
}
