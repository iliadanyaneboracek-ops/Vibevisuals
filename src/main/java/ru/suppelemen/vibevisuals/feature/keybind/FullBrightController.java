package ru.suppelemen.vibevisuals.feature.keybind;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;

public final class FullBrightController {
    private static boolean enabled;
    private static double previousGamma = 0.5D;

    private FullBrightController() {
    }

    public static void toggle(MinecraftClient client) {
        enabled = !enabled;

        if (enabled) {
            previousGamma = client.options.getGamma().getValue();
            double strength = VibeVisualsConfigManager.get().fullBrightStrength;
            client.options.getGamma().setValue(previousGamma + (1.0D - previousGamma) * strength);
        } else {
            client.options.getGamma().setValue(previousGamma);
        }

        if (client.player != null) {
            client.player.sendMessage(Text.literal("FullBright " + (enabled ? "enabled" : "disabled")), true);
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }
}