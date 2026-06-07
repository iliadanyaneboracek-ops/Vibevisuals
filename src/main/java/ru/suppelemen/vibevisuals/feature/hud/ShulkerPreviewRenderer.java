package ru.suppelemen.vibevisuals.feature.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import org.lwjgl.glfw.GLFW;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;

/**
 * Shows the contents of a container item (shulker box, ...) by drawing the real
 * shulker box GUI texture with the items in their slots, as if it were opened,
 * when the player hovers over it in a screen.
 */
public final class ShulkerPreviewRenderer {
    private static final Identifier SHULKER_TEXTURE = Identifier.ofVanilla("textures/gui/container/shulker_box.png");
    private static final int TEXTURE_SIZE = 256;

    private static final int BOX_WIDTH = 176;
    private static final int SLOTS_HEIGHT = 72;   // top border + title bar + 3 slot rows
    private static final int BORDER_HEIGHT = 6;    // bottom edge of the GUI panel
    private static final int BOX_HEIGHT = SLOTS_HEIGHT + BORDER_HEIGHT;

    private static final int COLUMNS = 9;
    private static final int SLOT_COUNT = 27;
    private static final int SLOT_SPACING = 18;
    private static final int GRID_X = 8;
    private static final int GRID_Y = 18;

    private ShulkerPreviewRenderer() {
    }

    private static boolean isShiftDown(MinecraftClient client) {
        return InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    /**
     * Whether the shulker preview should be shown for this stack right now. Used both to draw the
     * preview and to suppress the vanilla item tooltip so only the picture is shown.
     */
    public static boolean shouldPreview(ItemStack stack) {
        VibeVisualsConfig.ShulkerPreviewConfig config = VibeVisualsConfigManager.get().shulkerPreview;
        if (!config.enabled) {
            return false;
        }
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (stack.get(DataComponentTypes.CONTAINER) == null) {
            return false;
        }
        // Shift is always required: holding it shows the picture (and hides the name);
        // releasing it shows the normal item tooltip again.
        return isShiftDown(MinecraftClient.getInstance());
    }

    public static void render(DrawContext context, ItemStack stack, int mouseX, int mouseY) {
        if (!shouldPreview(stack)) {
            return;
        }

        VibeVisualsConfig.ShulkerPreviewConfig config = VibeVisualsConfigManager.get().shulkerPreview;
        MinecraftClient client = MinecraftClient.getInstance();
        ContainerComponent container = stack.get(DataComponentTypes.CONTAINER);

        DefaultedList<ItemStack> slots = DefaultedList.ofSize(SLOT_COUNT, ItemStack.EMPTY);
        container.copyTo(slots);

        int screenWidth = context.getScaledWindowWidth();
        int screenHeight = context.getScaledWindowHeight();

        int x = mouseX + 14;
        if (x + BOX_WIDTH > screenWidth - 4) {
            x = mouseX - BOX_WIDTH - 14;
        }
        x = Math.max(4, Math.min(x, screenWidth - BOX_WIDTH - 4));

        int y = mouseY - BOX_HEIGHT - 8;
        if (y < 4) {
            y = mouseY + 14;
        }
        y = Math.max(4, Math.min(y, screenHeight - BOX_HEIGHT - 4));

        int tint = colorWithOpacity(config.color, config.opacity);

        // Top of the GUI (border + title bar + 3 slot rows) then the bottom edge of the panel.
        context.drawTexture(RenderPipelines.GUI_TEXTURED, SHULKER_TEXTURE,
                x, y, 0.0f, 0.0f, BOX_WIDTH, SLOTS_HEIGHT, BOX_WIDTH, SLOTS_HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE, tint);
        context.drawTexture(RenderPipelines.GUI_TEXTURED, SHULKER_TEXTURE,
                x, y + SLOTS_HEIGHT, 0.0f, 160.0f, BOX_WIDTH, BORDER_HEIGHT, BOX_WIDTH, BORDER_HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE, tint);

        TextRenderer textRenderer = client.textRenderer;
        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack contained = slots.get(i);
            if (contained.isEmpty()) {
                continue;
            }
            int column = i % COLUMNS;
            int row = i / COLUMNS;
            int itemX = x + GRID_X + column * SLOT_SPACING;
            int itemY = y + GRID_Y + row * SLOT_SPACING;
            context.drawItem(contained, itemX, itemY);
            context.drawStackOverlay(textRenderer, contained, itemX, itemY);
        }
    }

    private static int colorWithOpacity(int color, float opacity) {
        int alpha = Math.max(0, Math.min(255, Math.round(opacity * 255.0f)));
        return (alpha << 24) | (color & 0x00FFFFFF);
    }
}