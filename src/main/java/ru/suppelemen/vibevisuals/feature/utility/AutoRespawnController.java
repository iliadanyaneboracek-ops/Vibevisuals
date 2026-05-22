package ru.suppelemen.vibevisuals.feature.utility;

import net.minecraft.client.MinecraftClient;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;

public final class AutoRespawnController {
    private static boolean respawnRequested;
    private static int commandDelayTicks = -1;

    private AutoRespawnController() {
    }

    public static void tick(MinecraftClient client) {
        if (client == null || client.player == null || client.world == null) {
            respawnRequested = false;
            commandDelayTicks = -1;
            return;
        }

        VibeVisualsConfig.AutoRespawnConfig config = VibeVisualsConfigManager.get().autoRespawn;
        if (!config.enabled) {
            respawnRequested = false;
            commandDelayTicks = -1;
            return;
        }

        if (!client.player.isAlive()) {
            if (!respawnRequested) {
                client.player.requestRespawn();
                respawnRequested = true;
                commandDelayTicks = Math.max(0, config.commandDelayTicks);
            }
            return;
        }

        if (respawnRequested) {
            if (commandDelayTicks > 0) {
                commandDelayTicks--;
                return;
            }

            runCommand(client, config.command);
            respawnRequested = false;
            commandDelayTicks = -1;
        }
    }

    private static void runCommand(MinecraftClient client, String command) {
        if (client.getNetworkHandler() == null || command == null || command.isBlank()) {
            return;
        }

        String normalized = command.trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        if (!normalized.isBlank()) {
            client.getNetworkHandler().sendChatCommand(normalized);
        }
    }
}
