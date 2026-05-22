package ru.suppelemen.vibevisuals.feature.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;
import ru.suppelemen.vibevisuals.feature.keybind.KeyStroke;
import ru.suppelemen.vibevisuals.feature.keybind.ModAction;
import ru.suppelemen.vibevisuals.feature.keybind.MultiKeyBinding;
import ru.suppelemen.vibevisuals.feature.keybind.MultiKeyBindingManager;
import ru.suppelemen.vibevisuals.theme.HudCardRenderType;
import ru.suppelemen.vibevisuals.theme.HudVisualSettings;
import ru.suppelemen.vibevisuals.util.render.HudCardRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MultiKeyBindingsScreen extends Screen {
    private static final int ROW_HEIGHT = 46;
    private static final int ROW_GAP = 6;

    private static final int COLOR_TEXT_PRIMARY = 0xFFEFEFF6;
    private static final int COLOR_TEXT_MUTED = 0xFFB7BBC9;
    private static final int COLOR_TEXT_DIM = 0xFF9DA2B3;
    private static final int COLOR_ACCENT = 0xFF7C5CFF;
    private static final int COLOR_CARD_BG_OFF = 0xFF090B12;
    private static final int COLOR_CARD_BG_ON = 0xFF201A42;
    private static final int COLOR_HEADER = 0xFF050710;

    private final HudVisualSettings panelSettings = new HudVisualSettings();
    private final List<Row> rows = new ArrayList<>();
    private final Screen parent;

    private CaptureTarget capturing;
    private int scrollOffset;
    private int sideAnimation;
    private Hit hoveredHit;

    public MultiKeyBindingsScreen(Screen parent) {
        super(Text.translatable("screen.vibevisuals.multiKeyBindings.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        panelSettings.renderType = HudCardRenderType.LIQUID_GLASS;
        panelSettings.radius = 10.0f;
        panelSettings.opacity = 0.82f;
        panelSettings.glow = false;
        panelSettings.blur = false;
        rebuildRows();
    }

    private void rebuildRows() {
        for (Row row : rows) {
            if (row.nameField != null) {
                remove(row.nameField);
            }
        }
        rows.clear();

        for (MultiKeyBinding binding : VibeVisualsConfigManager.get().multiKeyBindings.bindings) {
            Row row = new Row(binding);
            row.nameField = new TextFieldWidget(textRenderer, 0, 0, 1, 14, Text.literal("name"));
            row.nameField.setDrawsBackground(false);
            row.nameField.setEditableColor(COLOR_TEXT_PRIMARY);
            row.nameField.setMaxLength(40);
            row.nameField.setText(binding.displayName == null ? "" : binding.displayName);
            row.nameField.setChangedListener(value -> binding.displayName = value);
            addDrawableChild(row.nameField);
            rows.add(row);
        }
    }

    private int panelX() {
        return Math.max(20, width / 2 - panelW() / 2);
    }

    private int panelY() {
        return 30;
    }

    private int panelW() {
        return Math.min(420, width - 40);
    }

    private int panelH() {
        return Math.min(310, height - 60);
    }

    private int listTop() {
        return panelY() + 64;
    }

    private int listBottom() {
        return panelY() + panelH() - 14;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderInGameBackground(context);
        sideAnimation = Math.min(12, sideAnimation + 1);
        hoveredHit = null;

        int px = panelX();
        int py = panelY();
        int pw = panelW();
        int ph = panelH();

        HudCardRenderer.drawCard(context, px, py, pw, ph, panelSettings);
        HudCardRenderer.drawOverlayCard(context, px, py, pw, 26, 10.0f, COLOR_HEADER, 0.34f);
        context.drawTextWithShadow(textRenderer, title, px + 12, py + 8, COLOR_TEXT_PRIMARY);

        renderActionBar(context, mouseX, mouseY, px + 10, py + 32, pw - 20);

        context.enableScissor(px + 4, listTop() - 2, px + pw - 4, listBottom());
        renderRows(context, mouseX, mouseY, px + 10, listTop() - scrollOffset, pw - 20);
        context.disableScissor();

        renderScrollIndicator(context, px + pw - 6, listTop(), listBottom());

        if (capturing != null) {
            renderCaptureOverlay(context);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderActionBar(DrawContext context, int mouseX, int mouseY, int x, int y, int w) {
        VibeVisualsConfig.MultiKeyBindingsConfig cfg = VibeVisualsConfigManager.get().multiKeyBindings;
        int buttonH = 18;
        int addW = 56;
        int saveW = 96;
        int systemW = w - addW - saveW - 12;

        hitButton(context, mouseX, mouseY, x, y, addW, buttonH,
                Text.literal("+ Add"), COLOR_ACCENT, 0.62f, Hit.Type.ADD, null);

        hitButton(context, mouseX, mouseY, x + addW + 6, y, systemW, buttonH,
                Text.literal(cfg.enabled ? "System: ON" : "System: OFF"),
                cfg.enabled ? COLOR_ACCENT : 0xFF252936, cfg.enabled ? 0.62f : 0.5f,
                Hit.Type.SYSTEM_TOGGLE, null);

        hitButton(context, mouseX, mouseY, x + w - saveW, y, saveW, buttonH,
                Text.literal("Save & Close"), 0xFF252936, 0.55f, Hit.Type.CLOSE, null);
    }

    private void renderRows(DrawContext context, int mouseX, int mouseY, int x, int yStart, int w) {
        int y = yStart;
        for (Row row : rows) {
            renderRow(context, mouseX, mouseY, row, x, y, w);
            y += ROW_HEIGHT + ROW_GAP;
        }

        if (rows.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("No bindings yet — press + Add"),
                    panelX() + panelW() / 2, listTop() + 16, COLOR_TEXT_DIM);
        }
    }

    private void renderRow(DrawContext context, int mouseX, int mouseY, Row row, int x, int y, int w) {
        MultiKeyBinding binding = row.binding;

        row.x = x;
        row.y = y;
        row.w = w;

        boolean rowVisible = y + ROW_HEIGHT > listTop() - 2 && y < listBottom();
        if (!rowVisible) {
            row.nameField.visible = false;
            return;
        }

        boolean hoveredCard = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + ROW_HEIGHT;
        float baseOpacity = binding.enabled ? 0.54f : 0.32f;
        float opacity = baseOpacity + (hoveredCard ? 0.10f : 0.0f);
        int bg = binding.enabled ? COLOR_CARD_BG_ON : COLOR_CARD_BG_OFF;
        HudCardRenderer.drawOverlayCard(context, x, y, w, ROW_HEIGHT, 7.0f, bg, opacity);

        // Top row: toggle | name | primary | chord
        int innerX = x + 8;
        int topY = y + 7;
        int toggleW = 20;
        int toggleH = 10;
        drawToggle(context, innerX, topY + 1, toggleW, toggleH, binding.enabled);
        registerHit(mouseX, mouseY, innerX, topY + 1, toggleW, toggleH, Hit.Type.ENABLE, row);

        int nameX = innerX + toggleW + 8;
        int nameW = 130;
        context.fill(nameX - 2, topY - 1, nameX + nameW, topY + 13, 0x55171B28);
        row.nameField.setX(nameX);
        row.nameField.setY(topY + 2);
        row.nameField.setWidth(nameW);
        row.nameField.visible = true;

        int keyX = nameX + nameW + 8;
        int keyW = 78;
        boolean primaryAssigned = binding.primary != null && binding.primary.isAssigned();
        drawKeyPill(context, mouseX, mouseY, keyX, topY, keyW, 12,
                primaryAssigned ? binding.primary.describe() : "—",
                primaryAssigned, Hit.Type.PRIMARY, row);

        int chordX = keyX + keyW + 6;
        int chordW = w - (chordX - x) - 8;
        if (chordW > 30) {
            boolean hasChord = binding.hasChord();
            drawKeyPill(context, mouseX, mouseY, chordX, topY, chordW, 12,
                    hasChord ? binding.chord.describe() : "+ chord",
                    hasChord, Hit.Type.CHORD, row);
        }

        // Bottom row: actions [edit] | actions list | delete
        int botY = y + 26;
        int actionsBtnX = innerX;
        int actionsBtnW = 86;
        hitButton(context, mouseX, mouseY, actionsBtnX, botY, actionsBtnW, 14,
                Text.literal("Actions (" + binding.actions.size() + ")"),
                COLOR_HEADER, 0.55f, Hit.Type.ACTIONS, row);

        String actionSummary = binding.actions.isEmpty() ? "no actions" : summarizeActions(binding.actions);
        int summaryX = actionsBtnX + actionsBtnW + 8;
        int deleteW = 16;
        int summaryMaxW = w - (summaryX - x) - deleteW - 16;
        String truncated = truncateToWidth(actionSummary, summaryMaxW);
        context.drawTextWithShadow(textRenderer, Text.literal(truncated),
                summaryX, botY + 3, binding.actions.isEmpty() ? COLOR_TEXT_DIM : COLOR_TEXT_MUTED);

        int deleteX = x + w - deleteW - 8;
        hitButton(context, mouseX, mouseY, deleteX, botY, deleteW, 14,
                Text.literal("X"), 0xFF4A1B1B, 0.62f, Hit.Type.DELETE, row);
    }

    private void drawToggle(DrawContext context, int x, int y, int w, int h, boolean enabled) {
        HudCardRenderer.drawOverlayCard(context, x, y, w, h, h / 2.0f,
                enabled ? COLOR_ACCENT : 0xFF252936, enabled ? 0.72f : 0.70f);
        int knob = h - 3;
        int knobX = enabled ? x + w - knob - 2 : x + 2;
        HudCardRenderer.drawOverlayCard(context, knobX, y + 2, knob, knob, knob / 2.0f,
                enabled ? 0xFFFFFFFF : COLOR_TEXT_DIM, 0.96f);
    }

    private void drawKeyPill(DrawContext context, int mouseX, int mouseY,
                             int x, int y, int w, int h, String label, boolean assigned,
                             Hit.Type type, Row row) {
        boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        int bg = assigned ? COLOR_ACCENT : COLOR_HEADER;
        float opacity = assigned ? (hovered ? 0.78f : 0.62f) : (hovered ? 0.62f : 0.42f);
        HudCardRenderer.drawOverlayCard(context, x, y, w, h, 5.0f, bg, opacity);
        int color = assigned ? 0xFFFFFFFF : COLOR_TEXT_MUTED;
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(truncateToWidth(label, w - 6)),
                x + w / 2, y + 2, color);
        registerHit(mouseX, mouseY, x, y, w, h, type, row);
    }

    private void hitButton(DrawContext context, int mouseX, int mouseY,
                           int x, int y, int w, int h, Text label, int color, float opacity,
                           Hit.Type type, Row row) {
        boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        HudCardRenderer.drawOverlayCard(context, x, y, w, h, h / 3.0f, color, hovered ? opacity + 0.16f : opacity);
        context.drawCenteredTextWithShadow(textRenderer, label, x + w / 2, y + (h - 8) / 2, COLOR_TEXT_PRIMARY);
        registerHit(mouseX, mouseY, x, y, w, h, type, row);
    }

    private void registerHit(int mouseX, int mouseY, int x, int y, int w, int h, Hit.Type type, Row row) {
        if (mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h) {
            hoveredHit = new Hit(type, row);
        }
    }

    private void renderScrollIndicator(DrawContext context, int x, int top, int bottom) {
        int totalRows = rows.size();
        int totalHeight = totalRows * (ROW_HEIGHT + ROW_GAP);
        int visible = bottom - top;
        if (totalHeight <= visible) {
            return;
        }
        float ratio = (float) visible / (float) totalHeight;
        int trackH = bottom - top;
        int thumbH = Math.max(20, (int) (trackH * ratio));
        int maxScroll = totalHeight - visible;
        float scrollRatio = maxScroll == 0 ? 0 : (float) scrollOffset / maxScroll;
        int thumbY = top + (int) ((trackH - thumbH) * scrollRatio);
        context.fill(x, top, x + 3, bottom, 0x33000000);
        context.fill(x, thumbY, x + 3, thumbY + thumbH, COLOR_ACCENT);
    }

    private void renderCaptureOverlay(DrawContext context) {
        context.fill(0, 0, width, height, 0xCC000000);
        int boxW = 280;
        int boxH = 64;
        int bx = width / 2 - boxW / 2;
        int by = height / 2 - boxH / 2;
        HudCardRenderer.drawCard(context, bx, by, boxW, boxH, panelSettings);
        HudCardRenderer.drawOverlayCard(context, bx, by, boxW, 22, 10.0f, COLOR_HEADER, 0.40f);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Press a key for: " + capturing.label),
                width / 2, by + 7, COLOR_TEXT_PRIMARY);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Esc — cancel, Backspace — clear"),
                width / 2, by + 36, COLOR_TEXT_MUTED);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) {
            return true;
        }

        if (capturing != null) {
            return true;
        }

        if (hoveredHit == null) {
            return false;
        }

        Row row = hoveredHit.row;
        MultiKeyBinding binding = row == null ? null : row.binding;
        playClickSound();

        switch (hoveredHit.type) {
            case ADD -> addBinding();
            case SYSTEM_TOGGLE -> {
                VibeVisualsConfig.MultiKeyBindingsConfig cfg = VibeVisualsConfigManager.get().multiKeyBindings;
                cfg.enabled = !cfg.enabled;
                save();
            }
            case CLOSE -> close();
            case ENABLE -> {
                if (binding != null) {
                    binding.enabled = !binding.enabled;
                    save();
                }
            }
            case PRIMARY -> {
                if (binding != null) {
                    beginCapture(binding.primary, "primary", binding, false);
                }
            }
            case CHORD -> {
                if (binding != null) {
                    if (binding.chord == null) {
                        binding.chord = new KeyStroke();
                    }
                    beginCapture(binding.chord, "chord", binding, true);
                }
            }
            case ACTIONS -> {
                if (binding != null && client != null) {
                    client.setScreen(new ActionPickerScreen(this, binding));
                }
            }
            case DELETE -> {
                if (binding != null) {
                    deleteBinding(binding);
                }
            }
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int totalHeight = rows.size() * (ROW_HEIGHT + ROW_GAP);
        int visible = listBottom() - listTop();
        int maxScroll = Math.max(0, totalHeight - visible);
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) (verticalAmount * 18)));
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int keyCode = input.key();
        if (capturing != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                capturing = null;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                capturing.target.type = KeyStroke.TYPE_NONE;
                capturing.target.code = -1;
                capturing.target.ctrl = false;
                capturing.target.shift = false;
                capturing.target.alt = false;
                capturing.apply();
                capturing = null;
                save();
                return true;
            }
            if (isModifierOnly(keyCode)) {
                return true;
            }
            int mods = input.modifiers();
            capturing.target.type = KeyStroke.TYPE_KEYSYM;
            capturing.target.code = keyCode;
            capturing.target.ctrl = (mods & GLFW.GLFW_MOD_CONTROL) != 0;
            capturing.target.shift = (mods & GLFW.GLFW_MOD_SHIFT) != 0;
            capturing.target.alt = (mods & GLFW.GLFW_MOD_ALT) != 0;
            capturing.apply();
            capturing = null;
            save();
            return true;
        }
        return super.keyPressed(input);
    }

    private static boolean isModifierOnly(int keyCode) {
        return keyCode == GLFW.GLFW_KEY_LEFT_CONTROL || keyCode == GLFW.GLFW_KEY_RIGHT_CONTROL
                || keyCode == GLFW.GLFW_KEY_LEFT_SHIFT || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT
                || keyCode == GLFW.GLFW_KEY_LEFT_ALT || keyCode == GLFW.GLFW_KEY_RIGHT_ALT;
    }

    @Override
    public void close() {
        save();
        if (client != null) {
            client.setScreen(parent);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void addBinding() {
        MultiKeyBinding binding = new MultiKeyBinding();
        binding.id = "binding_" + UUID.randomUUID().toString().substring(0, 8);
        binding.displayName = "New Binding";
        binding.enabled = false;
        binding.primary = new KeyStroke();
        VibeVisualsConfigManager.get().multiKeyBindings.bindings.add(binding);
        save();
        rebuildRows();
    }

    private void deleteBinding(MultiKeyBinding binding) {
        VibeVisualsConfigManager.get().multiKeyBindings.bindings.remove(binding);
        save();
        rebuildRows();
    }

    private void save() {
        VibeVisualsConfigManager.get().validate();
        VibeVisualsConfigManager.save();
        MultiKeyBindingManager.resetState();
    }

    private void beginCapture(KeyStroke target, String label, MultiKeyBinding binding, boolean isChord) {
        capturing = new CaptureTarget(target, label, binding, isChord);
    }

    private void playClickSound() {
        MinecraftClient minecraft = MinecraftClient.getInstance();
        minecraft.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK.value(), 1.05f, 0.045f));
    }

    private String summarizeActions(List<String> actions) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < actions.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            ModAction action = ModAction.fromId(actions.get(i));
            builder.append(action == null ? actions.get(i) : action.displayName());
        }
        return builder.toString();
    }

    private String truncateToWidth(String text, int maxWidth) {
        if (textRenderer.getWidth(text) <= maxWidth) {
            return text;
        }
        String trimmed = text;
        while (trimmed.length() > 1 && textRenderer.getWidth(trimmed + "…") > maxWidth) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed + "…";
    }

    private static class Row {
        final MultiKeyBinding binding;
        TextFieldWidget nameField;
        int x;
        int y;
        int w;

        Row(MultiKeyBinding binding) {
            this.binding = binding;
        }
    }

    private static class Hit {
        enum Type { ADD, SYSTEM_TOGGLE, CLOSE, ENABLE, PRIMARY, CHORD, ACTIONS, DELETE }

        final Type type;
        final Row row;

        Hit(Type type, Row row) {
            this.type = type;
            this.row = row;
        }
    }

    private static class CaptureTarget {
        final KeyStroke target;
        final String label;
        final MultiKeyBinding binding;
        final boolean isChord;

        CaptureTarget(KeyStroke target, String label, MultiKeyBinding binding, boolean isChord) {
            this.target = target;
            this.label = label;
            this.binding = binding;
            this.isChord = isChord;
        }

        void apply() {
            if (isChord && !target.isAssigned()) {
                binding.chord = null;
            }
        }
    }
}