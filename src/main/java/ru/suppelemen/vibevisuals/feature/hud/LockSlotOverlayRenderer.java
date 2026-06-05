package ru.suppelemen.vibevisuals.feature.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerInventory;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;

public final class LockSlotOverlayRenderer {
    private LockSlotOverlayRenderer() {
    }

    public static void renderHotbarRow(DrawContext context, int firstSlotX, int slotY, int slotWidth, int slotHeight, int slotPitch) {
        VibeVisualsConfig.LockSlotConfig config = VibeVisualsConfigManager.get().lockSlot;
        if (!config.enabled || !config.showLockIcon) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }
        int slots = PlayerInventory.getHotbarSize();
        for (int slot = 0; slot < slots; slot++) {
            if (!config.isHotbarSlotLocked(slot)) {
                continue;
            }
            int slotX = firstSlotX + slot * slotPitch;
            drawLockGlyph(context, slotX + slotWidth - 7, slotY + 1, config.lockIconColor);
        }
    }

    public static void drawLockGlyph(DrawContext context, int x, int y, int color) {
        int outline = 0xCC000000;
        // shackle (arch)
        context.fill(x + 1, y, x + 5, y + 1, outline);
        context.fill(x, y + 1, x + 1, y + 3, outline);
        context.fill(x + 5, y + 1, x + 6, y + 3, outline);
        // body outline
        context.fill(x - 1, y + 2, x + 7, y + 3, outline);
        context.fill(x - 1, y + 6, x + 7, y + 7, outline);
        context.fill(x - 1, y + 3, x, y + 6, outline);
        context.fill(x + 6, y + 3, x + 7, y + 6, outline);
        // body fill
        context.fill(x, y + 3, x + 6, y + 6, color);
        // keyhole
        context.fill(x + 2, y + 4, x + 4, y + 5, outline);
    }
}
