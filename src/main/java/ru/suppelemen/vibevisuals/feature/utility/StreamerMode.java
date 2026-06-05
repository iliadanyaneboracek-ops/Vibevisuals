package ru.suppelemen.vibevisuals.feature.utility;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Hides sensitive information (coordinates, player name, server address) from
 * the F3 debug overlay while streaming.
 */
public final class StreamerMode {
    private static final String MASK = "§k####§r";

    private static final String[] LOCATION_PREFIXES = {
            "xyz:", "block:", "chunk:", "facing:", "targeted block:", "targeted fluid:",
            "targeted entity:", "biome:", "looking at:"
    };

    private StreamerMode() {
    }

    public static boolean isEnabled() {
        return VibeVisualsConfigManager.get().streamerMode.enabled;
    }

    public static List<String> censor(List<String> lines) {
        VibeVisualsConfig.StreamerModeConfig config = VibeVisualsConfigManager.get().streamerMode;
        if (lines == null || !config.enabled) {
            return lines;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        String playerName = client.player != null ? client.player.getGameProfile().name() : null;
        String serverAddress = serverAddress(client);

        List<String> result = new ArrayList<>(lines.size());
        for (String original : lines) {
            result.add(censorLine(original, config, playerName, serverAddress));
        }
        return result;
    }

    private static String censorLine(String line, VibeVisualsConfig.StreamerModeConfig config,
                                     String playerName, String serverAddress) {
        if (line == null || line.isEmpty()) {
            return line;
        }

        String result = line;

        if (config.hideCoordinates) {
            String trimmed = result.trim().toLowerCase(Locale.ROOT);
            for (String prefix : LOCATION_PREFIXES) {
                if (trimmed.startsWith(prefix)) {
                    int colon = result.indexOf(':');
                    result = colon >= 0 ? result.substring(0, colon + 1) + " " + MASK : MASK;
                    return result;
                }
            }
        }

        if (config.hideName && playerName != null && !playerName.isEmpty()) {
            result = result.replace(playerName, MASK);
        }

        if (config.hideServer && serverAddress != null && !serverAddress.isEmpty()) {
            result = result.replace(serverAddress, MASK);
        }

        return result;
    }

    private static String serverAddress(MinecraftClient client) {
        ServerInfo info = client.getCurrentServerEntry();
        if (info == null) {
            return null;
        }
        return info.address;
    }
}
