package ru.suppelemen.vibevisuals.feature.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;
import ru.suppelemen.vibevisuals.feature.marker.MarkerManager;
import ru.suppelemen.vibevisuals.feature.marker.MarkerManager.Marker;
import ru.suppelemen.vibevisuals.theme.HudCardRenderType;
import ru.suppelemen.vibevisuals.theme.HudVisualSettings;
import ru.suppelemen.vibevisuals.util.render.HudCardRenderer;

import java.util.List;
import java.util.Locale;

/**
 * Styled markers manager (liquid-glass). Lists every waypoint with rename,
 * per-marker colour picker, visibility toggle and coordinate copy.
 */
public class MarkersScreen extends Screen {
    private final HudVisualSettings panel = new HudVisualSettings();

    private static final int PANEL_W = 360;
    private static final int PANEL_H = 248;
    private static final int HEADER_H = 26;
    private static final int TOOLBAR_H = 26;
    private static final int ROW_H = 30;
    private static final int ROW_GAP = 4;

    private int panelX, panelY, listTop, listBottom, listX, listW;
    private int scroll;

    private int editingIndex = -1;
    private String editBuffer = "";

    // Colour picker popup (open when pickerIndex >= 0).
    private int pickerIndex = -1;
    private float pkH, pkS, pkV, pkA = 1f;
    private int colorDrag; // 0 none, 1 SV, 2 hue, 3 alpha
    private int pkSvX, pkSvY, pkSvSize, pkHueX, pkAlphaX, pkBarW;

    public MarkersScreen() {
        super(Text.translatable("screen.vibevisuals.markers"));
    }

    @Override
    protected void init() {
        panel.renderType = HudCardRenderType.LIQUID_GLASS;
        panel.radius = VibeVisualsConfigManager.get().menu.radius;
        panel.opacity = VibeVisualsConfigManager.get().menu.opacity;
        panel.glow = false;
        panel.blur = false;
        panelX = width / 2 - PANEL_W / 2;
        panelY = height / 2 - PANEL_H / 2;
        listX = panelX + 10;
        listW = PANEL_W - 20;
        listTop = panelY + HEADER_H + TOOLBAR_H;
        listBottom = panelY + PANEL_H - 10;
    }

    private VibeVisualsConfig.MenuConfig menu() {
        return VibeVisualsConfigManager.get().menu;
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        renderInGameBackground(ctx);
        VibeVisualsConfig.MenuConfig m = menu();

        HudCardRenderer.drawCard(ctx, panelX, panelY, PANEL_W, PANEL_H, panel);
        HudCardRenderer.drawOverlayCard(ctx, panelX, panelY, PANEL_W, HEADER_H, m.radius, m.backgroundColor, m.headerOpacity);
        ctx.drawText(textRenderer, Text.literal("Markers"), panelX + 12, panelY + 9, m.titleColor, false);
        String count = MarkerManager.markers().size() + " saved";
        ctx.drawText(textRenderer, Text.literal(count), panelX + PANEL_W - 12 - textRenderer.getWidth(count), panelY + 9, m.mutedTextColor, false);

        // Toolbar buttons.
        int tbY = panelY + HEADER_H + 3;
        int bw = (PANEL_W - 20 - 2 * 6) / 3;
        drawButton(ctx, panelX + 10, tbY, bw, 20, "+ Crosshair", mx, my);
        drawButton(ctx, panelX + 10 + bw + 6, tbY, bw, 20, "+ At me", mx, my);
        drawButton(ctx, panelX + 10 + 2 * (bw + 6), tbY, bw, 20, "Clear", mx, my);

        // List.
        List<Marker> list = MarkerManager.markers();
        ctx.enableScissor(listX, listTop, listX + listW, listBottom);
        int y = listTop + 4 - scroll;
        for (int i = 0; i < list.size(); i++) {
            if (y + ROW_H >= listTop && y <= listBottom) {
                drawRow(ctx, list.get(i), i, listX, y, listW, mx, my);
            }
            y += ROW_H + ROW_GAP;
        }
        ctx.disableScissor();

        int contentH = list.size() * (ROW_H + ROW_GAP);
        int viewH = listBottom - listTop - 4;
        int maxScroll = Math.max(0, contentH - viewH);
        if (scroll > maxScroll) scroll = maxScroll;

        if (pickerIndex >= 0 && pickerIndex < list.size()) {
            drawPicker(ctx, list.get(pickerIndex));
        }

        super.render(ctx, mx, my, delta);
    }

    private void drawRow(DrawContext ctx, Marker mk, int index, int x, int y, int w, int mx, int my) {
        VibeVisualsConfig.MenuConfig m = menu();
        boolean hov = inside(mx, my, x, y, w, ROW_H) && pickerIndex < 0;
        HudCardRenderer.drawOverlayCard(ctx, x, y, w, ROW_H, 6, hov ? m.activeColor : m.cardColor,
                hov ? m.activeOpacity : m.cardOpacity);

        // Colour swatch with initial.
        int sw = 18;
        int swX = x + 6, swY = y + (ROW_H - sw) / 2;
        int rgb = mk.color() & 0x00FFFFFF;
        ctx.fill(swX, swY, swX + sw, swY + sw, 0xFF000000 | rgb);
        ctx.fill(swX, swY, swX + sw, swY + 1, 0x40FFFFFF);
        String initial = mk.name().isBlank() ? "?" : mk.name().substring(0, 1).toUpperCase(Locale.ROOT);
        int ic = contrast(rgb);
        ctx.drawText(textRenderer, initial, swX + (sw - textRenderer.getWidth(initial)) / 2, swY + 5, ic, false);

        // Right-side controls: eye, trash.
        int eyeX = x + w - 6 - 16;
        int trashX = eyeX - 4 - 16;
        boolean visHov = inside(mx, my, eyeX, y + 4, 16, 16);
        ctx.drawText(textRenderer, mk.visible() ? "◉" : "○", eyeX, y + 6, mk.visible() ? 0xFF8FE3FF : m.mutedTextColor, false);
        ctx.drawText(textRenderer, "✕", trashX + 4, y + 6, inside(mx, my, trashX, y + 4, 16, 16) ? 0xFFFF6B6B : m.mutedTextColor, false);

        int textX = swX + sw + 8;
        int rightLimit = trashX - 6;

        // Line 1: name (or edit buffer).
        if (editingIndex == index) {
            String shown = editBuffer + ((System.currentTimeMillis() / 500) % 2 == 0 ? "_" : "");
            ctx.fill(textX - 2, y + 4, rightLimit, y + 15, 0x66000000);
            ctx.drawText(textRenderer, shown, textX, y + 5, 0xFFFFFFFF, false);
        } else {
            ctx.drawText(textRenderer, trim(mk.name(), rightLimit - textX), textX, y + 5, m.titleColor, false);
        }

        // Line 2: coords + distance + copy.
        String coords = (int) Math.floor(mk.pos().x) + " " + (int) Math.floor(mk.pos().y) + " " + (int) Math.floor(mk.pos().z);
        String dist = "";
        if (client != null && client.player != null) {
            dist = "  ·  " + (int) Math.round(Math.sqrt(mk.pos().squaredDistanceTo(client.player.getLerpedPos(1.0f)))) + "m";
        }
        ctx.drawText(textRenderer, coords + dist, textX, y + 17, m.mutedTextColor, false);
        int copyX = rightLimit - textRenderer.getWidth("copy");
        ctx.drawText(textRenderer, "copy", copyX, y + 17, inside(mx, my, copyX, y + 16, 30, 10) ? 0xFF8FE3FF : m.mutedTextColor, false);
    }

    private void drawButton(DrawContext ctx, int x, int y, int w, int h, String label, int mx, int my) {
        VibeVisualsConfig.MenuConfig m = menu();
        boolean hov = inside(mx, my, x, y, w, h) && pickerIndex < 0;
        HudCardRenderer.drawOverlayCard(ctx, x, y, w, h, 6, hov ? m.accentColor : m.cardColor, hov ? 0.6f : m.cardOpacity);
        ctx.drawText(textRenderer, label, x + (w - textRenderer.getWidth(label)) / 2, y + (h - 8) / 2, m.titleColor, false);
    }

    // ---------- input ----------

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean dbl) {
        double mx = click.x(), my = click.y();
        if (click.button() != 0) return super.mouseClicked(click, dbl);

        if (pickerIndex >= 0) {
            return pickerClick(mx, my);
        }

        // Toolbar.
        int tbY = panelY + HEADER_H + 3;
        int bw = (PANEL_W - 20 - 2 * 6) / 3;
        if (inside(mx, my, panelX + 10, tbY, bw, 20)) { MarkerManager.addAtCrosshair(client); return true; }
        if (inside(mx, my, panelX + 10 + bw + 6, tbY, bw, 20)) { MarkerManager.addAtPlayer(client); return true; }
        if (inside(mx, my, panelX + 10 + 2 * (bw + 6), tbY, bw, 20)) { MarkerManager.clear(); commitEdit(); return true; }

        // Rows.
        List<Marker> list = MarkerManager.markers();
        int y = listTop + 4 - scroll;
        for (int i = 0; i < list.size(); i++) {
            if (y + ROW_H >= listTop && y <= listBottom && inside(mx, my, listX, y, listW, ROW_H)) {
                Marker mk = list.get(i);
                int sw = 18, swX = listX + 6, swY = y + (ROW_H - sw) / 2;
                int eyeX = listX + listW - 6 - 16;
                int trashX = eyeX - 4 - 16;
                int copyX = (trashX - 6) - textRenderer.getWidth("copy");
                if (inside(mx, my, swX, swY, sw, sw)) { openPicker(i, mk.color()); return true; }
                if (inside(mx, my, eyeX, y + 4, 16, 16)) { mk.setVisible(!mk.visible()); MarkerManager.save(); return true; }
                if (inside(mx, my, trashX, y + 4, 16, 16)) { commitEdit(); MarkerManager.remove(i); return true; }
                if (inside(mx, my, copyX, y + 16, 34, 12) && client != null) {
                    client.keyboard.setClipboard((int) Math.floor(mk.pos().x) + " " + (int) Math.floor(mk.pos().y) + " " + (int) Math.floor(mk.pos().z));
                    return true;
                }
                // Click on the name area → rename.
                commitEdit();
                editingIndex = i;
                editBuffer = mk.name();
                return true;
            }
            y += ROW_H + ROW_GAP;
        }
        commitEdit();
        return super.mouseClicked(click, dbl);
    }

    @Override
    public boolean mouseDragged(net.minecraft.client.gui.Click click, double dx, double dy) {
        if (pickerIndex >= 0 && colorDrag != 0) {
            pickerDrag(click.x(), click.y());
            return true;
        }
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseReleased(net.minecraft.client.gui.Click click) {
        if (pickerIndex >= 0 && colorDrag != 0) {
            colorDrag = 0;
            MarkerManager.save();
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmount, double vAmount) {
        if (pickerIndex < 0 && inside(mx, my, listX, listTop, listW, listBottom - listTop)) {
            scroll = Math.max(0, scroll - (int) Math.round(vAmount * 16));
            return true;
        }
        return super.mouseScrolled(mx, my, hAmount, vAmount);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (editingIndex >= 0) {
            int key = input.key();
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) { commitEdit(); return true; }
            if (key == GLFW.GLFW_KEY_ESCAPE) { editingIndex = -1; return true; }
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                if (!editBuffer.isEmpty()) editBuffer = editBuffer.substring(0, editBuffer.length() - 1);
                return true;
            }
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (editingIndex >= 0) {
            int cp = input.codepoint();
            if (cp >= ' ' && cp != 127 && editBuffer.length() < 32) {
                editBuffer += new String(Character.toChars(cp));
            }
            return true;
        }
        return super.charTyped(input);
    }

    private void commitEdit() {
        if (editingIndex >= 0) {
            List<Marker> list = MarkerManager.markers();
            if (editingIndex < list.size() && !editBuffer.isBlank()) {
                list.get(editingIndex).setName(editBuffer.trim());
                MarkerManager.save();
            }
            editingIndex = -1;
        }
    }

    // ---------- colour picker ----------

    private void openPicker(int index, int argb) {
        commitEdit();
        pickerIndex = index;
        colorDrag = 0;
        pkA = 1f; // marker colours are opaque; alpha drives nothing here but kept for UI
        float[] hsv = rgbToHsv((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF);
        pkH = hsv[0]; pkS = hsv[1]; pkV = hsv[2];
    }

    private void applyPicker() {
        if (pickerIndex < 0) return;
        List<Marker> list = MarkerManager.markers();
        if (pickerIndex >= list.size()) { pickerIndex = -1; return; }
        int[] c = hsvToRgb(pkH, pkS, pkV);
        list.get(pickerIndex).setColor(0xFF000000 | (c[0] << 16) | (c[1] << 8) | c[2]);
    }

    private void drawPicker(DrawContext ctx, Marker mk) {
        int sv = 90, bar = 12, gap = 8, pad = 10;
        int popW = pad * 2 + sv + gap + bar;
        int popH = pad * 2 + sv + 14;
        int ax = panelX + PANEL_W / 2 - popW / 2;
        int ay = panelY + PANEL_H / 2 - popH / 2;
        HudCardRenderer.drawOverlayCard(ctx, ax, ay, popW, popH, 8, 0xFF0A0A12, 0.96f);

        pkSvX = ax + pad; pkSvY = ay + pad; pkSvSize = sv; pkBarW = bar;
        pkHueX = pkSvX + sv + gap; pkAlphaX = -1;

        int n = 24; float cw = sv / (float) n;
        for (int xi = 0; xi < n; xi++) {
            for (int yi = 0; yi < n; yi++) {
                int[] c = hsvToRgb(pkH, xi / (float) (n - 1), 1f - yi / (float) (n - 1));
                ctx.fill(pkSvX + Math.round(xi * cw), pkSvY + Math.round(yi * cw),
                        pkSvX + Math.round((xi + 1) * cw), pkSvY + Math.round((yi + 1) * cw),
                        0xFF000000 | (c[0] << 16) | (c[1] << 8) | c[2]);
            }
        }
        int curX = pkSvX + Math.round(pkS * sv), curY = pkSvY + Math.round((1 - pkV) * sv);
        ctx.fill(curX - 2, curY - 2, curX + 2, curY + 2, 0xFFFFFFFF);

        for (int yi = 0; yi < sv; yi++) {
            int[] c = hsvToRgb(yi / (float) (sv - 1), 1f, 1f);
            ctx.fill(pkHueX, pkSvY + yi, pkHueX + bar, pkSvY + yi + 1, 0xFF000000 | (c[0] << 16) | (c[1] << 8) | c[2]);
        }
        int hy = pkSvY + Math.round(pkH * sv);
        ctx.fill(pkHueX - 1, hy - 1, pkHueX + bar + 1, hy + 1, 0xFFFFFFFF);

        int[] full = hsvToRgb(pkH, pkS, pkV);
        String hex = String.format(Locale.ROOT, "#%02X%02X%02X", full[0], full[1], full[2]);
        ctx.drawText(textRenderer, hex, pkSvX, ay + popH - 11, 0xFFFFFFFF, false);
    }

    private boolean pickerClick(double mx, double my) {
        if (inBox(mx, my, pkSvX, pkSvY, pkSvSize, pkSvSize)) { colorDrag = 1; pickerDrag(mx, my); return true; }
        if (inBox(mx, my, pkHueX, pkSvY, pkBarW, pkSvSize)) { colorDrag = 2; pickerDrag(mx, my); return true; }
        // Outside → close + persist.
        pickerIndex = -1;
        MarkerManager.save();
        return true;
    }

    private void pickerDrag(double mx, double my) {
        if (colorDrag == 1) {
            pkS = clampf((float) (mx - pkSvX) / pkSvSize, 0, 1);
            pkV = clampf(1f - (float) (my - pkSvY) / pkSvSize, 0, 1);
        } else if (colorDrag == 2) {
            pkH = clampf((float) (my - pkSvY) / pkSvSize, 0, 1);
        }
        applyPicker();
    }

    // ---------- helpers ----------

    private String trim(String s, int maxW) {
        if (textRenderer.getWidth(s) <= maxW) return s;
        while (s.length() > 1 && textRenderer.getWidth(s + "…") > maxW) s = s.substring(0, s.length() - 1);
        return s + "…";
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private static boolean inBox(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private static float clampf(float v, float lo, float hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    private static int contrast(int rgb) {
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
        return (0.299 * r + 0.587 * g + 0.114 * b) > 140 ? 0xFF101010 : 0xFFFFFFFF;
    }

    private static float[] rgbToHsv(int r, int g, int b) {
        float rf = r / 255f, gf = g / 255f, bf = b / 255f;
        float max = Math.max(rf, Math.max(gf, bf)), min = Math.min(rf, Math.min(gf, bf));
        float d = max - min, h = 0;
        if (d > 1e-5) {
            if (max == rf) h = ((gf - bf) / d) % 6f;
            else if (max == gf) h = (bf - rf) / d + 2f;
            else h = (rf - gf) / d + 4f;
            h /= 6f; if (h < 0) h += 1f;
        }
        return new float[]{h, max <= 0 ? 0 : d / max, max};
    }

    private static int[] hsvToRgb(float h, float s, float v) {
        float r = 0, g = 0, b = 0;
        int i = (int) Math.floor(h * 6) % 6;
        if (i < 0) i += 6;
        float f = h * 6 - (float) Math.floor(h * 6);
        float p = v * (1 - s), q = v * (1 - f * s), t = v * (1 - (1 - f) * s);
        switch (i) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        return new int[]{Math.round(r * 255), Math.round(g * 255), Math.round(b * 255)};
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
