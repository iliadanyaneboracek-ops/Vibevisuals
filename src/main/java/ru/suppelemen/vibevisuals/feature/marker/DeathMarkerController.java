package ru.suppelemen.vibevisuals.feature.marker;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;

public final class DeathMarkerController {
    private static boolean deathHandled;

    private DeathMarkerController() {
    }

    public static void tick(MinecraftClient client) {
        if (client == null || client.player == null || client.world == null) {
            deathHandled = false;
            return;
        }

        VibeVisualsConfig.DeathMarkerConfig config = VibeVisualsConfigManager.get().deathMarker;
        if (!config.enabled) {
            deathHandled = !client.player.isAlive();
            return;
        }

        if (client.player.isAlive()) {
            deathHandled = false;
            return;
        }

        if (deathHandled) {
            return;
        }
        deathHandled = true;

        MarkerManager.addDeath(client);

        if (config.announceInChat) {
            String coords = String.format("%d, %d, %d",
                    Math.round(client.player.getX()), Math.round(client.player.getY()), Math.round(client.player.getZ()));
            client.player.sendMessage(Text.translatable("vibevisuals.deathMarker.placed", coords), false);
        }
    }
}
