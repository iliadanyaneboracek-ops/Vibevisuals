package ru.suppelemen.vibevisuals.feature.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import ru.suppelemen.vibevisuals.config.ConfigShareManager;
import ru.suppelemen.vibevisuals.theme.MenuTheme;
import ru.suppelemen.vibevisuals.util.font.SmoothText;
import ru.suppelemen.vibevisuals.util.render.HudCardRenderer;

import java.util.List;

/**
 * Profile-management screen. Reachable from {@link VibeVisualsMenuScreen}
 * (the "Profiles" entry in the MENU category).
 *
 * Layout (top → bottom):
 *   ┌──────────────────────────────────────────────────┐
 *   │ ← Back                            Profiles       │
 *   ├──────────────────────────────────────────────────┤
 *   │ Your share code   [VV1:H4sIAAA…]   [Copy]        │
 *   │ Import code       [paste here…]    [Apply]       │
 *   │ ──────────────────────────────────────────       │
 *   │ Saved profiles                                   │
 *   │  ✓ default                          [×]          │
 *   │    pvp                              [×]          │
 *   │    imported-a1b2c3                  [×]          │
 *   └──────────────────────────────────────────────────┘
 */
public final class VibeVisualsProfilesScreen extends Screen {

    private static final float REFERENCE_WIDTH = 1920.0f;
    private static final float REFERENCE_HEIGHT = 1080.0f;
    private static final float TEXT_ONLY_SCALE = 0.80f;

    private static final int PANEL_W = 620;
    private static final int PANEL_H = 420;
    private static final int PANEL_RADIUS = 22;
    private static final int PAD_X = 22;
    private static final int HEADER_H = 44;

    private static final float TEXT_TITLE = 16.0f;
    private static final float TEXT_LABEL = 12.0f;
    private static final float TEXT_SUB = 10.0f;
    private static final float TEXT_CODE = 10.0f;
    private static final float TEXT_BTN = 11.0f;
    private static final float TEXT_BACK = 11.5f;

    private final Screen parent;

    private String importBuffer = "";
    private boolean importFocused;
    private String toast;             // transient status message
    private long toastUntilMs;

    // Hit-regions populated each frame for click detection.
    private int copyX, copyY, copyW, copyH;
    private int applyX, applyY, applyW, applyH;
    private int importBoxX, importBoxY, importBoxW, importBoxH;
    private int backX, backY, backW, backH;

    private int listScroll;
    private int listMaxScroll;

    private final java.util.List<int[]> rowSwitchHit = new java.util.ArrayList<>();
    private final java.util.List<int[]> rowDeleteHit = new java.util.ArrayList<>();
    private final java.util.List<String> rowNames = new java.util.ArrayList<>();

    public VibeVisualsProfilesScreen(Screen parent) {
        super(Text.literal("VibeVisuals Profiles"));
        this.parent = parent;
    }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() { return false; }

    // ---------- Render ----------

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        // Dim the background so the panel pops.
        ctx.fill(0, 0, width, height, 0xA0000000);

        int pw = panelW();
        int ph = panelH();
        int px = (width - pw) / 2;
        int py = (height - ph) / 2;

        // Panel.
        HudCardRenderer.drawOverlayCard(ctx, px, py, pw, ph, dp(PANEL_RADIUS),
                MenuTheme.MATERIAL_PANEL, MenuTheme.MATERIAL_OPACITY_PANEL);
        HudCardRenderer.drawShaderOutline(ctx, px, py, pw, ph, dp(PANEL_RADIUS),
                0.55f, MenuTheme.GLASS_OUTLINE_ALPHA);

        // Header: back chevron + title.
        renderHeader(ctx, mx, my, px, py, pw);

        // Body.
        int bodyTop = py + dp(HEADER_H);
        int bodyX = px + dp(PAD_X);
        int bodyW = pw - dp(PAD_X) * 2;

        int cursorY = bodyTop + dp(10);

        // Section 1: Your share code.
        cursorY = renderShareCodeBlock(ctx, mx, my, bodyX, cursorY, bodyW);
        cursorY += dp(14);

        // Section 2: Import.
        cursorY = renderImportBlock(ctx, mx, my, bodyX, cursorY, bodyW);
        cursorY += dp(14);

        // Section 3: Saved profiles list.
        renderProfilesList(ctx, mx, my, bodyX, cursorY, bodyW, py + ph - dp(14) - cursorY);

        // Transient toast.
        renderToast(ctx, px, py + ph);
    }

    private void renderHeader(DrawContext ctx, int mx, int my, int px, int py, int pw) {
        int hH = dp(HEADER_H);
        int backSize = dp(18);
        int bx = px + dp(PAD_X);
        int by = py + (hH - backSize) / 2;
        backX = bx - dp(6); backY = by - dp(6);
        backW = backSize + dp(60); backH = backSize + dp(12);
        boolean hov = mx >= backX && mx <= backX + backW && my >= backY && my <= backY + backH;
        int backColor = hov ? MenuTheme.TEXT_PRIMARY : MenuTheme.withAlpha(MenuTheme.TEXT_NEUTRAL, 0.65f);
        // Tiny `<` glyph.
        int thick = Math.max(1, backSize / 8);
        int cxx = bx + backSize / 2;
        int cyy = by + backSize / 2;
        int reach = backSize / 3;
        for (int i = 0; i <= reach; i++) {
            float fade = ((backColor >>> 24) & 0xFF) / 255.0f * (1.0f - i * 0.025f);
            HudCardRenderer.drawOverlayCard(ctx, cxx - i, cyy - (reach - i), thick, thick, thick / 2.0f, backColor | 0xFF000000, fade);
            HudCardRenderer.drawOverlayCard(ctx, cxx - i, cyy + (reach - i), thick, thick, thick / 2.0f, backColor | 0xFF000000, fade);
        }
        drawText(ctx, "Back", bx + backSize + dp(8), by + (backSize - txtH(TEXT_BACK)) / 2,
                TEXT_BACK, backColor);

        // Title centred.
        String title = "Profiles";
        int tw = txtW(title, TEXT_TITLE);
        drawText(ctx, title, px + (pw - tw) / 2, py + (hH - txtH(TEXT_TITLE)) / 2,
                TEXT_TITLE, MenuTheme.TEXT_PRIMARY);

        // 1-px separator under header.
        ctx.fill(px + dp(PAD_X), py + hH, px + pw - dp(PAD_X), py + hH + 1,
                MenuTheme.withAlpha(MenuTheme.TEXT_NEUTRAL, 0.10f));
    }

    private int renderShareCodeBlock(DrawContext ctx, int mx, int my, int x, int y, int w) {
        drawText(ctx, "Your share code", x, y, TEXT_LABEL, MenuTheme.TEXT_PRIMARY);
        drawText(ctx, "Send this to a friend so they can load your settings.",
                x, y + txtH(TEXT_LABEL) + dp(2), TEXT_SUB,
                MenuTheme.withAlpha(MenuTheme.TEXT_NEUTRAL, 0.65f));
        int rowY = y + txtH(TEXT_LABEL) + dp(2) + txtH(TEXT_SUB) + dp(8);
        int rowH = dp(28);
        int btnW = dp(58);
        int btnGap = dp(8);
        int fieldW = w - btnW - btnGap;

        // Field with the code (truncated for display).
        HudCardRenderer.drawOverlayCard(ctx, x, rowY, fieldW, rowH, dp(8),
                MenuTheme.MATERIAL_CARD, MenuTheme.MATERIAL_OPACITY_CARD * 0.7f);
        HudCardRenderer.drawShaderOutline(ctx, x, rowY, fieldW, rowH, dp(8),
                0.4f, 0.20f);
        String code = ConfigShareManager.exportCurrent();
        // Fit code visually — abbreviate to ~58 chars.
        String display = ConfigShareManager.abbreviate(code, 58);
        drawText(ctx, display, x + dp(10),
                rowY + (rowH - txtH(TEXT_CODE)) / 2,
                TEXT_CODE, MenuTheme.withAlpha(MenuTheme.TEXT_NEUTRAL, 0.85f));

        // Copy button.
        copyX = x + fieldW + btnGap; copyY = rowY; copyW = btnW; copyH = rowH;
        boolean copyHov = mx >= copyX && mx <= copyX + copyW && my >= copyY && my <= copyY + copyH;
        drawButton(ctx, copyX, copyY, copyW, copyH, "Copy", copyHov);

        return rowY + rowH;
    }

    private int renderImportBlock(DrawContext ctx, int mx, int my, int x, int y, int w) {
        drawText(ctx, "Import code", x, y, TEXT_LABEL, MenuTheme.TEXT_PRIMARY);
        drawText(ctx, "Paste a code starting with VV1: — it'll be saved as a new profile.",
                x, y + txtH(TEXT_LABEL) + dp(2), TEXT_SUB,
                MenuTheme.withAlpha(MenuTheme.TEXT_NEUTRAL, 0.65f));
        int rowY = y + txtH(TEXT_LABEL) + dp(2) + txtH(TEXT_SUB) + dp(8);
        int rowH = dp(28);
        int btnW = dp(58);
        int btnGap = dp(8);
        int fieldW = w - btnW - btnGap;

        importBoxX = x; importBoxY = rowY; importBoxW = fieldW; importBoxH = rowH;
        // Input box.
        HudCardRenderer.drawOverlayCard(ctx, x, rowY, fieldW, rowH, dp(8),
                MenuTheme.MATERIAL_CARD,
                MenuTheme.MATERIAL_OPACITY_CARD * (importFocused ? 0.85f : 0.7f));
        HudCardRenderer.drawShaderOutline(ctx, x, rowY, fieldW, rowH, dp(8),
                0.4f, importFocused ? 0.40f : 0.20f);

        if (importBuffer.isEmpty()) {
            drawText(ctx, "Paste share code here…", x + dp(10),
                    rowY + (rowH - txtH(TEXT_CODE)) / 2, TEXT_CODE,
                    MenuTheme.withAlpha(MenuTheme.TEXT_NEUTRAL, 0.40f));
        } else {
            String display = ConfigShareManager.abbreviate(importBuffer, 58);
            drawText(ctx, display, x + dp(10),
                    rowY + (rowH - txtH(TEXT_CODE)) / 2, TEXT_CODE,
                    MenuTheme.TEXT_PRIMARY);
            // Blink caret if focused.
            if (importFocused && (System.currentTimeMillis() / 530L) % 2L == 0L) {
                int caretX = x + dp(10) + txtW(display, TEXT_CODE) + dp(1);
                int caretH = txtH(TEXT_CODE);
                int caretY = rowY + (rowH - caretH) / 2;
                ctx.fill(caretX, caretY, caretX + 1, caretY + caretH, MenuTheme.TEXT_PRIMARY);
            }
        }

        // Apply button.
        applyX = x + fieldW + btnGap; applyY = rowY; applyW = btnW; applyH = rowH;
        boolean applyHov = mx >= applyX && mx <= applyX + applyW && my >= applyY && my <= applyY + applyH;
        drawButton(ctx, applyX, applyY, applyW, applyH, "Apply", applyHov);

        return rowY + rowH;
    }

    private void renderProfilesList(DrawContext ctx, int mx, int my, int x, int y, int w, int h) {
        drawText(ctx, "Saved profiles", x, y, TEXT_LABEL, MenuTheme.TEXT_PRIMARY);
        int listY = y + txtH(TEXT_LABEL) + dp(8);
        int listMaxY = y + h - dp(2);

        List<String> profiles = ConfigShareManager.listProfiles();
        String active = ConfigShareManager.activeProfile();

        rowSwitchHit.clear();
        rowDeleteHit.clear();
        rowNames.clear();

        int rowH = dp(26);
        int rowGap = dp(4);
        int contentH = profiles.size() * (rowH + rowGap) - rowGap;
        listMaxScroll = Math.max(0, contentH - (listMaxY - listY));
        if (listScroll > listMaxScroll) listScroll = listMaxScroll;

        int cursorY = listY - listScroll;
        for (String name : profiles) {
            if (cursorY + rowH < listY) { cursorY += rowH + rowGap; continue; }
            if (cursorY > listMaxY) break;

            boolean isActive = name.equals(active);
            boolean rowHov = mx >= x && mx <= x + w && my >= cursorY && my <= cursorY + rowH;
            // Background — slight tint for active or hover.
            float bgOp = isActive ? 0.20f : (rowHov ? 0.10f : 0.05f);
            HudCardRenderer.drawOverlayCard(ctx, x, cursorY, w, rowH, dp(8),
                    MenuTheme.MATERIAL_CARD, MenuTheme.MATERIAL_OPACITY_CARD * bgOp);

            // Check icon for active.
            int markX = x + dp(10);
            int markSize = dp(10);
            int markY = cursorY + (rowH - markSize) / 2;
            if (isActive) {
                // Tiny check: two perpendicular short rects.
                int t = Math.max(1, markSize / 5);
                HudCardRenderer.drawOverlayCard(ctx, markX, markY + markSize - t, t * 2, t,
                        t * 0.5f, MenuTheme.TEXT_PRIMARY, 1.0f);
                HudCardRenderer.drawOverlayCard(ctx, markX + t, markY + markSize - t * 3,
                        t, t * 3, t * 0.5f, MenuTheme.TEXT_PRIMARY, 1.0f);
            }

            drawText(ctx, name, x + dp(28), cursorY + (rowH - txtH(TEXT_LABEL)) / 2,
                    TEXT_LABEL,
                    isActive
                            ? MenuTheme.TEXT_PRIMARY
                            : MenuTheme.withAlpha(MenuTheme.TEXT_PRIMARY, 0.80f));

            // Delete button — only for non-active profiles.
            int delSize = dp(14);
            int delX = x + w - dp(10) - delSize;
            int delY = cursorY + (rowH - delSize) / 2;
            if (!isActive) {
                boolean delHov = mx >= delX - dp(2) && mx <= delX + delSize + dp(2)
                        && my >= delY - dp(2) && my <= delY + delSize + dp(2);
                int delColor = delHov ? 0xFFFF6B6B : MenuTheme.withAlpha(MenuTheme.TEXT_NEUTRAL, 0.55f);
                // Cross icon.
                int thick = Math.max(1, delSize / 7);
                int reach = delSize - thick;
                for (int i = 0; i < reach; i++) {
                    ctx.fill(delX + i, delY + i, delX + i + thick, delY + i + thick, delColor);
                    ctx.fill(delX + reach - i, delY + i, delX + reach - i + thick, delY + i + thick, delColor);
                }
                rowDeleteHit.add(new int[]{delX - dp(2), delY - dp(2),
                        delX + delSize + dp(2), delY + delSize + dp(2)});
            } else {
                rowDeleteHit.add(null);
            }

            // The whole row (minus the delete button) activates the profile on click.
            rowSwitchHit.add(new int[]{x, cursorY, x + w - delSize - dp(20), cursorY + rowH});
            rowNames.add(name);

            cursorY += rowH + rowGap;
        }

        // Mini scrollbar on the right if overflow.
        if (listMaxScroll > 0) {
            int barW = 2;
            int barX = x + w - barW;
            int trackH = listMaxY - listY;
            HudCardRenderer.drawOverlayCard(ctx, barX, listY, barW, trackH, 1f,
                    0xFF000000, 0.20f);
            int viewportH = trackH;
            int totalH = viewportH + listMaxScroll;
            int thumbH = Math.max(dp(14), Math.round((float) trackH * viewportH / Math.max(1, totalH)));
            int thumbY = listY + Math.round((float)(trackH - thumbH) * listScroll / Math.max(1, listMaxScroll));
            HudCardRenderer.drawOverlayCard(ctx, barX, thumbY, barW, thumbH, 1f,
                    0xFFFFFFFF, 0.80f);
        }
    }

    private void renderToast(DrawContext ctx, int px, int panelBottom) {
        if (toast == null || System.currentTimeMillis() > toastUntilMs) {
            toast = null;
            return;
        }
        long left = toastUntilMs - System.currentTimeMillis();
        float a = Math.min(1f, left / 400f);
        int tw = txtW(toast, TEXT_LABEL);
        int tpad = dp(12);
        int bw = tw + tpad * 2;
        int bh = dp(26);
        int bx = px + (panelW() - bw) / 2;
        int by = panelBottom - dp(40);
        HudCardRenderer.drawOverlayCard(ctx, bx, by, bw, bh, dp(8),
                0xFF000000, 0.80f * a);
        drawText(ctx, toast, bx + tpad, by + (bh - txtH(TEXT_LABEL)) / 2, TEXT_LABEL,
                MenuTheme.withAlpha(0xFFFFFFFF, a));
    }

    private void drawButton(DrawContext ctx, int x, int y, int w, int h, String label, boolean hover) {
        boolean light = MenuTheme.current == MenuTheme.ThemeMode.LIGHT;
        int color = hover
                ? (light ? 0xFF1B1B26 : 0xFFFFFFFF)
                : (light ? 0xFF2B2B36 : 0xFFE5E5EE);
        float op = hover ? 1.00f : 0.85f;
        HudCardRenderer.drawOverlayCard(ctx, x, y, w, h, dp(8), color, op);
        int textColor = light
                ? (hover ? 0xFFFFFFFF : 0xFFFFFFFF)
                : (hover ? 0xFF14141C : 0xFF1B1B26);
        int tw = txtW(label, TEXT_BTN);
        drawText(ctx, label, x + (w - tw) / 2, y + (h - txtH(TEXT_BTN)) / 2,
                TEXT_BTN, textColor);
    }

    // ---------- Input ----------

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) return true;
        if (click.button() != 0) return false;
        double x = click.x(), y = click.y();

        if (x >= backX && x <= backX + backW && y >= backY && y <= backY + backH) {
            close();
            return true;
        }
        if (x >= copyX && x <= copyX + copyW && y >= copyY && y <= copyY + copyH) {
            String code = ConfigShareManager.exportCurrent();
            if (client != null) client.keyboard.setClipboard(code);
            flash("Copied share code to clipboard");
            return true;
        }
        if (x >= importBoxX && x <= importBoxX + importBoxW
                && y >= importBoxY && y <= importBoxY + importBoxH) {
            importFocused = true;
            // Paste from clipboard on click if empty — convenient default.
            if (importBuffer.isEmpty() && client != null) {
                String clip = client.keyboard.getClipboard();
                if (clip != null && clip.trim().startsWith("VV1:")) {
                    importBuffer = clip.trim();
                }
            }
            return true;
        } else {
            importFocused = false;
        }
        if (x >= applyX && x <= applyX + applyW && y >= applyY && y <= applyY + applyH) {
            applyImport();
            return true;
        }

        for (int i = 0; i < rowNames.size(); i++) {
            int[] del = rowDeleteHit.get(i);
            if (del != null && x >= del[0] && x <= del[2] && y >= del[1] && y <= del[3]) {
                if (ConfigShareManager.deleteProfile(rowNames.get(i))) {
                    flash("Deleted profile " + rowNames.get(i));
                }
                return true;
            }
            int[] sw = rowSwitchHit.get(i);
            if (sw != null && x >= sw[0] && x <= sw[2] && y >= sw[1] && y <= sw[3]) {
                if (ConfigShareManager.switchToProfile(rowNames.get(i))) {
                    flash("Switched to " + rowNames.get(i));
                }
                return true;
            }
        }
        return false;
    }

    private void applyImport() {
        String code = importBuffer.trim();
        if (code.isEmpty() && client != null) {
            String clip = client.keyboard.getClipboard();
            if (clip != null) code = clip.trim();
        }
        if (code.isEmpty()) {
            flash("Nothing to import");
            return;
        }
        String name = ConfigShareManager.importCode(code);
        if (name == null) {
            flash("Invalid share code");
        } else {
            flash("Imported as " + name);
            importBuffer = "";
            importFocused = false;
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        listScroll = Math.max(0, Math.min(listMaxScroll, listScroll - (int) Math.round(dy * 18.0)));
        return true;
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (importFocused && input.isValidChar()) {
            int cp = input.codepoint();
            if (cp >= ' ' && cp != 127 && importBuffer.length() < 4096) {
                importBuffer += new String(Character.toChars(cp));
            }
            return true;
        }
        return super.charTyped(input);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int k = input.key();
        if (importFocused) {
            if (k == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE) {
                if (!importBuffer.isEmpty()) {
                    importBuffer = importBuffer.substring(0, importBuffer.length() - 1);
                }
                return true;
            }
            if (k == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || k == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
                applyImport();
                return true;
            }
            // Ctrl/Cmd+V pastes from clipboard.
            if ((input.modifiers() & (org.lwjgl.glfw.GLFW.GLFW_MOD_CONTROL | org.lwjgl.glfw.GLFW.GLFW_MOD_SUPER)) != 0
                    && k == org.lwjgl.glfw.GLFW.GLFW_KEY_V) {
                if (client != null) {
                    String clip = client.keyboard.getClipboard();
                    if (clip != null) importBuffer = clip.trim();
                }
                return true;
            }
        }
        return super.keyPressed(input);
    }

    private void flash(String msg) {
        toast = msg;
        toastUntilMs = System.currentTimeMillis() + 2000L;
    }

    // ---------- Layout helpers ----------

    private float dpScale() {
        MinecraftClient mc = client != null ? client : MinecraftClient.getInstance();
        if (mc == null) return 1f;
        float fbW = mc.getWindow().getWidth();
        float fbH = mc.getWindow().getHeight();
        float layoutScale = Math.min(fbW / REFERENCE_WIDTH, fbH / REFERENCE_HEIGHT);
        double guiScale = mc.getWindow().getScaleFactor();
        if (guiScale <= 0.0) guiScale = 1.0;
        return Math.max((float)(layoutScale / guiScale), 0.45f);
    }
    private int dp(float v) { return Math.round(v * dpScale()); }
    private int panelW() { return Math.min(dp(PANEL_W), width - dp(40)); }
    private int panelH() { return Math.min(dp(PANEL_H), height - dp(40)); }

    private int targetGlyph(float px) {
        return Math.max(8, Math.round(px * dpScale() * TEXT_ONLY_SCALE));
    }
    private int txtW(String s, float px) { return SmoothText.measureText(s, targetGlyph(px)); }
    private int txtH(float px) { return Math.round(targetGlyph(px) * 0.72f); }
    private void drawText(DrawContext ctx, String s, int x, int y, float px, int color) {
        int g = targetGlyph(px);
        int nudge = Math.round(g * 0.27f);
        SmoothText.drawText(ctx, s, x, y - nudge, g, color);
    }
}
