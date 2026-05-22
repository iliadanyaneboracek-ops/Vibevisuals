package ru.suppelemen.vibevisuals.feature.pvp;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;

public final class ShiftUpController {
    private static int activeTicks;
    private static boolean resetPending;

    private ShiftUpController() {
    }

    public static void onCritHit() {
        if (!VibeVisualsConfigManager.get().shiftUp.enabled) {
            return;
        }

        activeTicks = 1;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.options != null) {
            client.options.sneakKey.setPressed(true);
        }
    }

    public static void tick(MinecraftClient client) {
        if (client == null || client.options == null) {
            return;
        }

        KeyBinding sneak = client.options.sneakKey;
        if (activeTicks > 0) {
            sneak.setPressed(true);
            activeTicks--;
            if (activeTicks == 0) {
                resetPending = true;
            }
            return;
        }

        if (resetPending) {
            sneak.setPressed(false);
            resetPending = false;
        }
    }
}
