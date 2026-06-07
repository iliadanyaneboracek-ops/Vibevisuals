package ru.suppelemen.vibevisuals.feature.screen;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;
import ru.suppelemen.vibevisuals.feature.marker.MarkerManager;
import ru.suppelemen.vibevisuals.feature.marker.MarkerManager.Marker;
import ru.suppelemen.vibevisuals.theme.HudCardRenderType;
import ru.suppelemen.vibevisuals.theme.HudVisualSettings;
import ru.suppelemen.vibevisuals.util.font.SmoothText;
import ru.suppelemen.vibevisuals.util.render.HudCardRenderer;

import java.util.List;
import java.util.Locale;

/** Styled markers manager (liquid-glass, our SmoothText font). */
public class MarkersScreen extends Screen {
    private final HudVisualSettings panel = new HudVisualSettings();

    private static final int PANEL_W = 380;
    private static final int PANEL_H = 250;
    private static final int HEADER_H = 28;
    private static final int TOOLBAR_H = 28;
    private static final int ROW_H = 32;
    private static final int ROW_GAP = 4;
    private static final float TITLE_PX = 12f;
    private static final float BODY_PX = 9.5f;
    private static final float SMALL_PX = 8f;

    private int panelX, panelY, listTop, listBottom, listX, listW;
    private int scroll;

    // Row rename.
    private int editingIndex = -1;

    // Create dialog.
    private boolean creating;
    private String cName = "", cx = "", cy = "", cz = "";
    private int cColor = 0xFF7C5CFF;
    private int cField = -1; // 0 name, 1 x, 2 y, 3 z

    // Colour picker.
    private boolean pickerOpen;
    private boolean pickerForCreate;
    private int pickerMarker = -1;
    private float pkH, pkS, pkV;
    private int colorDrag;
    private int pkSvX, pkSvY, pkSvSize, pkHueX, pkBarW;

    public MarkersScreen() {
        super(Text.literal("Markers"));
    }

    @Override
    protected void init() {
        panel.renderType = HudCardRenderType.LIQUID_GLASS;
        panel.radius = menu().radius;
        panel.opacity = menu().opacity;
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

    // ---------- render ----------

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        renderInGameBackground(ctx);
        VibeVisualsConfig.MenuConfig m = menu();

        HudCardRenderer.drawCard(ctx, panelX, panelY, PANEL_W, PANEL_H, panel);
        HudCardRenderer.drawOverlayCard(ctx, panelX, panelY, PANEL_W, HEADER_H, m.radius, m.backgroundColor, m.headerOpacity);
        txt(ctx, "Markers", panelX + 12, panelY + 9, TITLE_PX, m.titleColor);
        String count = MarkerManager.markers().size() + " saved";
        txt(ctx, count, panelX + PANEL_W - 12 - tw(count, BODY_PX), panelY + 10, BODY_PX, m.mutedTextColor);

        boolean blockHover = pickerOpen || creating;

        int tbY = panelY + HEADER_H + 4;
        int bw = (PANEL_W - 20 - 2 * 6) / 3;
        drawButton(ctx, panelX + 10, tbY, bw, 20, "+ Crosshair", mx, my, blockHover);
        drawButton(ctx, panelX + 10 + bw + 6, tbY, bw, 20, "+ At me", mx, my, blockHover);
        drawButton(ctx, panelX + 10 + 2 * (bw + 6), tbY, bw, 20, "Clear", mx, my, blockHover);

        List<Marker> list = MarkerManager.markers();
        ctx.enableScissor(listX, listTop, listX + listW, listBottom);
        int y = listTop + 4 - scroll;
        for (int i = 0; i < list.size(); i++) {
            if (y + ROW_H >= listTop && y <= listBottom) {
                drawRow(ctx, list.get(i), i, listX, y, listW, mx, my, blockHover);
            }
            y += ROW_H + ROW_GAP;
        }
        ctx.disableScissor();

        int contentH = list.size() * (ROW_H + ROW_GAP);
        int maxScroll = Math.max(0, contentH - (listBottom - listTop - 4));
        if (scroll > maxScroll) scroll = maxScroll;

        if (creating) {
            drawCreate(ctx, mx, my);
        }
        if (pickerOpen) {
            drawPicker(ctx);
        }
        super.render(ctx, mx, my, delta);
    }

    private void drawRow(DrawContext ctx, Marker mk, int index, int x, int y, int w, int mx, int my, boolean block) {
        VibeVisualsConfig.MenuConfig m = menu();
        boolean hov = !block && inside(mx, my, x, y, w, ROW_H);
        HudCardRenderer.drawOverlayCard(ctx, x, y, w, ROW_H, 6, hov ? m.activeColor : m.cardColor,
                hov ? m.activeOpacity : m.cardOpacity);

        int sw = 20, swX = x + 6, swY = y + (ROW_H - sw) / 2;
        int rgb = mk.color() & 0x00FFFFFF;
        ctx.fill(swX, swY, swX + sw, swY + sw, 0xFF000000 | rgb);
        ctx.fill(swX, swY, swX + sw, swY + 1, 0x40FFFFFF);
        String initial = mk.name().isBlank() ? "?" : mk.name().substring(0, 1).toUpperCase(Locale.ROOT);
        txt(ctx, initial, swX + (sw - tw(initial, BODY_PX)) / 2, swY + 6, BODY_PX, contrast(rgb));

        // Right controls: visibility square + delete X.
        int eyeX = x + w - 8 - 12;
        int trashX = eyeX - 10 - 12;
        // visibility
        if (mk.visible()) {
            ctx.fill(eyeX, y + 9, eyeX + 12, y + 21, 0xFF8FE3FF);
        } else {
            ctx.fill(eyeX, y + 9, eyeX + 12, y + 21, 0x40FFFFFF);
            ctx.fill(eyeX + 2, y + 11, eyeX + 10, y + 19, 0xFF15151D);
        }
        txt(ctx, "X", trashX + 2, y + 10, BODY_PX, inside(mx, my, trashX, y + 6, 16, 18) ? 0xFFFF6B6B : m.mutedTextColor);

        int textX = swX + sw + 8;
        int rightLimit = trashX - 8;

        if (editingIndex == index) {
            String shown = cName + caret();
            ctx.fill(textX - 2, y + 5, rightLimit, y + 17, 0x66000000);
            txt(ctx, shown, textX, y + 6, BODY_PX, 0xFFFFFFFF);
        } else {
            txt(ctx, trim(mk.name(), rightLimit - textX, BODY_PX), textX, y + 6, BODY_PX, m.titleColor);
        }

        String coords = (int) Math.floor(mk.pos().x) + " " + (int) Math.floor(mk.pos().y) + " " + (int) Math.floor(mk.pos().z);
        if (client != null && client.player != null) {
            coords += "  |  " + (int) Math.round(Math.sqrt(mk.pos().squaredDistanceTo(client.player.getLerpedPos(1.0f)))) + "m";
        }
        txt(ctx, coords, textX, y + 19, SMALL_PX, m.mutedTextColor);
        String copy = "copy";
        int copyX = rightLimit - tw(copy, SMALL_PX);
        txt(ctx, copy, copyX, y + 19, SMALL_PX, inside(mx, my, copyX, y + 18, 30, 12) ? 0xFF8FE3FF : m.mutedTextColor);
    }

    private void drawButton(DrawContext ctx, int x, int y, int w, int h, String label, int mx, int my, boolean block) {
        VibeVisualsConfig.MenuConfig m = menu();
        boolean hov = !block && inside(mx, my, x, y, w, h);
        HudCardRenderer.drawOverlayCard(ctx, x, y, w, h, 6, hov ? m.accentColor : m.cardColor, hov ? 0.6f : m.cardOpacity);
        txt(ctx, label, x + (w - tw(label, BODY_PX)) / 2, y + (h - (int) BODY_PX) / 2, BODY_PX, m.titleColor);
    }

    // ---------- create dialog ----------

    private int cpX, cpY, cpW = 250, cpH = 168;

    private void drawCreate(DrawContext ctx, int mx, int my) {
        VibeVisualsConfig.MenuConfig m = menu();
        ctx.fill(panelX, panelY, panelX + PANEL_W, panelY + PANEL_H, 0x88000000); // dim panel
        cpX = width / 2 - cpW / 2;
        cpY = height / 2 - cpH / 2;
        HudCardRenderer.drawOverlayCard(ctx, cpX, cpY, cpW, cpH, 8, 0xFF0A0A12, 0.97f);
        txt(ctx, "New marker", cpX + 12, cpY + 10, TITLE_PX, m.titleColor);

        int fx = cpX + 12, fw = cpW - 24;
        // Name
        txt(ctx, "Name", fx, cpY + 32, SMALL_PX, m.mutedTextColor);
        field(ctx, fx, cpY + 42, fw, cName, cField == 0);
        // Coords
        txt(ctx, "Coordinates", fx, cpY + 66, SMALL_PX, m.mutedTextColor);
        int cw = (fw - 2 * 6) / 3;
        field(ctx, fx, cpY + 76, cw, cx, cField == 1);
        field(ctx, fx + cw + 6, cpY + 76, cw, cy, cField == 2);
        field(ctx, fx + 2 * (cw + 6), cpY + 76, cw, cz, cField == 3);
        // Color
        txt(ctx, "Color", fx, cpY + 102, SMALL_PX, m.mutedTextColor);
        ctx.fill(fx, cpY + 112, fx + 30, cpY + 128, 0xFF000000 | (cColor & 0x00FFFFFF));
        ctx.fill(fx, cpY + 112, fx + 30, cpY + 113, 0x40FFFFFF);
        // Buttons
        int by = cpY + cpH - 28;
        boolean cancelHov = inside(mx, my, cpX + 12, by, 100, 20) && !pickerOpen;
        boolean createHov = inside(mx, my, cpX + cpW - 12 - 100, by, 100, 20) && !pickerOpen;
        HudCardRenderer.drawOverlayCard(ctx, cpX + 12, by, 100, 20, 6, cancelHov ? m.activeColor : m.cardColor, cancelHov ? m.activeOpacity : m.cardOpacity);
        txt(ctx, "Cancel", cpX + 12 + (100 - tw("Cancel", BODY_PX)) / 2, by + 6, BODY_PX, m.titleColor);
        HudCardRenderer.drawOverlayCard(ctx, cpX + cpW - 12 - 100, by, 100, 20, 6, m.accentColor, createHov ? 0.75f : 0.55f);
        txt(ctx, "Create", cpX + cpW - 12 - 100 + (100 - tw("Create", BODY_PX)) / 2, by + 6, BODY_PX, 0xFFFFFFFF);
    }

    private void field(DrawContext ctx, int x, int y, int w, String value, boolean focused) {
        ctx.fill(x, y, x + w, y + 16, focused ? 0xCC1A1A26 : 0x99121219);
        if (focused) {
            ctx.fill(x, y + 15, x + w, y + 16, menu().accentColor);
        }
        String shown = value + (focused ? caret() : "");
        txt(ctx, trim(shown, w - 8, BODY_PX), x + 4, y + 4, BODY_PX, 0xFFFFFFFF);
    }

    private void openCreate(Vec3d pos) {
        commitEdit();
        creating = true;
        cName = "Marker " + (MarkerManager.markers().size() + 1);
        cx = Integer.toString((int) Math.floor(pos.x));
        cy = Integer.toString((int) Math.floor(pos.y));
        cz = Integer.toString((int) Math.floor(pos.z));
        cColor = menu().accentColor == 0 ? 0xFF7C5CFF : VibeVisualsConfigManager.get().markers.color;
        cField = 0;
    }

    private void confirmCreate() {
        double x = parse(cx), y = parse(cy), z = parse(cz);
        MarkerManager.add(cName.trim(), new Vec3d(x + 0.5, y, z + 0.5), cColor);
        creating = false;
        cField = -1;
    }

    private Vec3d crosshairPos() {
        Vec3d pos = client.player != null ? client.player.getLerpedPos(1.0f) : Vec3d.ZERO;
        HitResult hit = client.crosshairTarget;
        if (hit != null && hit.getType() != HitResult.Type.MISS) {
            pos = hit.getPos();
            if (hit instanceof EntityHitResult eh) {
                Entity e = eh.getEntity();
                pos = new Vec3d(e.getX(), e.getY() + e.getHeight() * 0.5, e.getZ());
            }
        }
        return pos;
    }

    // ---------- input ----------

    @Override
    public boolean mouseClicked(Click click, boolean dbl) {
        double mx = click.x(), my = click.y();
        if (click.button() != 0) return super.mouseClicked(click, dbl);

        if (pickerOpen) {
            return pickerClick(mx, my);
        }
        if (creating) {
            return createClick(mx, my);
        }

        int tbY = panelY + HEADER_H + 4;
        int bw = (PANEL_W - 20 - 2 * 6) / 3;
        if (inside(mx, my, panelX + 10, tbY, bw, 20)) { openCreate(crosshairPos()); return true; }
        if (inside(mx, my, panelX + 10 + bw + 6, tbY, bw, 20)) {
            openCreate(client.player != null ? client.player.getLerpedPos(1.0f) : Vec3d.ZERO);
            return true;
        }
        if (inside(mx, my, panelX + 10 + 2 * (bw + 6), tbY, bw, 20)) { MarkerManager.clear(); commitEdit(); return true; }

        List<Marker> list = MarkerManager.markers();
        int y = listTop + 4 - scroll;
        for (int i = 0; i < list.size(); i++) {
            if (y + ROW_H >= listTop && y <= listBottom && inside(mx, my, listX, y, listW, ROW_H)) {
                Marker mk = list.get(i);
                int sw = 20, swX = listX + 6, swY = y + (ROW_H - sw) / 2;
                int eyeX = listX + listW - 8 - 12;
                int trashX = eyeX - 10 - 12;
                int copyX = (trashX - 8) - tw("copy", SMALL_PX);
                if (inside(mx, my, swX, swY, sw, sw)) { openPicker(i, false, mk.color()); return true; }
                if (inside(mx, my, eyeX, y + 9, 12, 12)) { mk.setVisible(!mk.visible()); MarkerManager.save(); return true; }
                if (inside(mx, my, trashX, y + 6, 16, 18)) { commitEdit(); MarkerManager.remove(i); return true; }
                if (inside(mx, my, copyX, y + 18, 34, 12) && client != null) {
                    client.keyboard.setClipboard((int) Math.floor(mk.pos().x) + " " + (int) Math.floor(mk.pos().y) + " " + (int) Math.floor(mk.pos().z));
                    return true;
                }
                commitEdit();
                editingIndex = i;
                cName = mk.name();
                return true;
            }
            y += ROW_H + ROW_GAP;
        }
        commitEdit();
        return super.mouseClicked(click, dbl);
    }

    private boolean createClick(double mx, double my) {
        int fx = cpX + 12, fw = cpW - 24;
        int cw = (fw - 2 * 6) / 3;
        if (inside(mx, my, fx, cpY + 42, fw, 16)) { cField = 0; return true; }
        if (inside(mx, my, fx, cpY + 76, cw, 16)) { cField = 1; return true; }
        if (inside(mx, my, fx + cw + 6, cpY + 76, cw, 16)) { cField = 2; return true; }
        if (inside(mx, my, fx + 2 * (cw + 6), cpY + 76, cw, 16)) { cField = 3; return true; }
        if (inside(mx, my, fx, cpY + 112, 30, 16)) { openPicker(-1, true, cColor); return true; }
        int by = cpY + cpH - 28;
        if (inside(mx, my, cpX + 12, by, 100, 20)) { creating = false; cField = -1; return true; }
        if (inside(mx, my, cpX + cpW - 12 - 100, by, 100, 20)) { confirmCreate(); return true; }
        return true; // swallow clicks inside the dialog area
    }

    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        if (pickerOpen && colorDrag != 0) { pickerDrag(click.x(), click.y()); return true; }
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (pickerOpen && colorDrag != 0) {
            colorDrag = 0;
            if (!pickerForCreate) MarkerManager.save();
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        if (!pickerOpen && !creating && inside(mx, my, listX, listTop, listW, listBottom - listTop)) {
            scroll = Math.max(0, scroll - (int) Math.round(v * 16));
            return true;
        }
        return super.mouseScrolled(mx, my, h, v);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int key = input.key();
        if (editingIndex >= 0 || (creating && cField >= 0)) {
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                if (creating) confirmCreate(); else commitEdit();
                return true;
            }
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                if (creating) { creating = false; cField = -1; } else editingIndex = -1;
                return true;
            }
            if (key == GLFW.GLFW_KEY_BACKSPACE) { backspace(); return true; }
            return true;
        }
        if (pickerOpen && key == GLFW.GLFW_KEY_ESCAPE) { closePicker(); return true; }
        if (creating && key == GLFW.GLFW_KEY_ESCAPE) { creating = false; return true; }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        int cp = input.codepoint();
        if (cp < ' ' || cp == 127) return super.charTyped(input);
        if (editingIndex >= 0) {
            if (cName.length() < 32) cName += new String(Character.toChars(cp));
            return true;
        }
        if (creating && cField >= 0) {
            String add = new String(Character.toChars(cp));
            switch (cField) {
                case 0 -> { if (cName.length() < 32) cName += add; }
                case 1 -> { if (cx.length() < 10) cx += add; }
                case 2 -> { if (cy.length() < 10) cy += add; }
                case 3 -> { if (cz.length() < 10) cz += add; }
            }
            return true;
        }
        return super.charTyped(input);
    }

    private void backspace() {
        if (editingIndex >= 0) { if (!cName.isEmpty()) cName = cName.substring(0, cName.length() - 1); return; }
        switch (cField) {
            case 0 -> { if (!cName.isEmpty()) cName = cName.substring(0, cName.length() - 1); }
            case 1 -> { if (!cx.isEmpty()) cx = cx.substring(0, cx.length() - 1); }
            case 2 -> { if (!cy.isEmpty()) cy = cy.substring(0, cy.length() - 1); }
            case 3 -> { if (!cz.isEmpty()) cz = cz.substring(0, cz.length() - 1); }
        }
    }

    private void commitEdit() {
        if (editingIndex >= 0) {
            List<Marker> list = MarkerManager.markers();
            if (editingIndex < list.size() && !cName.isBlank()) {
                list.get(editingIndex).setName(cName.trim());
                MarkerManager.save();
            }
            editingIndex = -1;
        }
    }

    // ---------- colour picker ----------

    private void openPicker(int markerIndex, boolean forCreate, int argb) {
        commitEdit();
        pickerOpen = true;
        pickerForCreate = forCreate;
        pickerMarker = markerIndex;
        colorDrag = 0;
        float[] hsv = rgbToHsv((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF);
        pkH = hsv[0]; pkS = hsv[1]; pkV = hsv[2];
    }

    private void closePicker() {
        pickerOpen = false;
        if (!pickerForCreate) MarkerManager.save();
    }

    private void applyPicker() {
        int[] c = hsvToRgb(pkH, pkS, pkV);
        int argb = 0xFF000000 | (c[0] << 16) | (c[1] << 8) | c[2];
        if (pickerForCreate) {
            cColor = argb;
        } else {
            List<Marker> list = MarkerManager.markers();
            if (pickerMarker >= 0 && pickerMarker < list.size()) list.get(pickerMarker).setColor(argb);
        }
    }

    private void drawPicker(DrawContext ctx) {
        int sv = 96, bar = 12, gap = 8, pad = 10;
        int popW = pad * 2 + sv + gap + bar;
        int popH = pad * 2 + sv + 14;
        int ax = width / 2 - popW / 2, ay = height / 2 - popH / 2;
        HudCardRenderer.drawOverlayCard(ctx, ax, ay, popW, popH, 8, 0xFF0A0A12, 0.98f);
        pkSvX = ax + pad; pkSvY = ay + pad; pkSvSize = sv; pkBarW = bar;
        pkHueX = pkSvX + sv + gap;

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
        txt(ctx, String.format(Locale.ROOT, "#%02X%02X%02X", full[0], full[1], full[2]), pkSvX, ay + popH - 11, SMALL_PX, 0xFFFFFFFF);
    }

    private boolean pickerClick(double mx, double my) {
        if (inside(mx, my, pkSvX, pkSvY, pkSvSize, pkSvSize)) { colorDrag = 1; pickerDrag(mx, my); return true; }
        if (inside(mx, my, pkHueX, pkSvY, pkBarW, pkSvSize)) { colorDrag = 2; pickerDrag(mx, my); return true; }
        closePicker();
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

    /**
     * Draw SmoothText with the same baseline lift the ClickGUI uses: SmoothText
     * draws a full glyph CELL whose visible cap sits ~0.27·size below the cell
     * top, so we lift it so {@code y} means "top of the cap" (otherwise letters
     * drift downward).
     */
    private static void txt(DrawContext ctx, String s, int x, int y, float px, int color) {
        int g = Math.max(4, Math.round(px));
        SmoothText.drawText(ctx, s, x, y - Math.round(g * 0.27f), g, color);
    }

    private static int tw(String s, float px) {
        return SmoothText.measureText(s, Math.max(4, Math.round(px)));
    }

    private static String caret() {
        return (System.currentTimeMillis() / 500) % 2 == 0 ? "_" : "";
    }

    private static double parse(String s) {
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return 0; }
    }

    private String trim(String s, int maxW, float px) {
        if (tw(s, px) <= maxW) return s;
        while (s.length() > 1 && tw(s + "..", px) > maxW) s = s.substring(0, s.length() - 1);
        return s + "..";
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
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
