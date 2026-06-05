package ru.suppelemen.vibevisuals.feature.utility;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;

import java.util.Locale;

/**
 * Auto-leaves the server when a nearby player is detected through the server's /near command.
 * The mod periodically issues /near, captures the reply within a short window and matches any
 * online player name (excluding the local player and a configurable whitelist). When a stranger
 * is found a warning is shown and the client disconnects after a short, cancellable delay.
 */
public final class AutoLeaveController {
    private static final int RESPONSE_WINDOW_TICKS = 40; // 2s window to capture the /near reply

    private static int sinceLastCheckTicks;
    private static int listenTicks;
    private static int warningTicks;
    private static String pendingPlayer;

    private AutoLeaveController() {
    }

    public static void tick(MinecraftClient client) {
        VibeVisualsConfig.AutoLeaveConfig config = VibeVisualsConfigManager.get().autoLeave;

        if (client == null || client.player == null || client.world == null || !config.enabled) {
            reset();
            return;
        }

        if (listenTicks > 0) {
            listenTicks--;
        }

        // Warning countdown -> disconnect.
        if (warningTicks > 0) {
            if (config.cancelOnMovement && playerIsActive(client)) {
                cancelWarning(client);
                return;
            }

            warningTicks--;
            if (warningTicks <= 0) {
                disconnect(client);
            }
            return;
        }

        // Periodically request the /near list.
        if (config.autoSendNear) {
            sinceLastCheckTicks++;
            if (sinceLastCheckTicks >= config.checkIntervalSeconds * 20) {
                sinceLastCheckTicks = 0;
                requestNear(client);
            }
        }
    }

    /** Called from the network handler mixin for every incoming chat/system message. */
    public static void onGameMessage(String message) {
        if (message == null || listenTicks <= 0 || warningTicks > 0) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return;
        }

        ClientPlayNetworkHandler handler = client.getNetworkHandler();
        if (handler == null) {
            return;
        }

        VibeVisualsConfig.AutoLeaveConfig config = VibeVisualsConfigManager.get().autoLeave;
        if (!config.enabled) {
            return;
        }

        String selfName = client.player.getGameProfile().name();
        String stranger = findStranger(message, handler, selfName, config);
        if (stranger != null) {
            startWarning(client, stranger, config);
        }
    }

    private static String findStranger(String message, ClientPlayNetworkHandler handler,
                                       String selfName, VibeVisualsConfig.AutoLeaveConfig config) {
        // Tokenise the message into possible Minecraft names and match against online players.
        String[] tokens = message.split("[^A-Za-z0-9_]+");
        for (PlayerListEntry entry : handler.getPlayerList()) {
            String name = entry.getProfile().name();
            if (name == null || name.isBlank()) {
                continue;
            }
            if (selfName != null && name.equalsIgnoreCase(selfName)) {
                continue;
            }
            if (isWhitelisted(name, config)) {
                continue;
            }

            for (String token : tokens) {
                if (token.equalsIgnoreCase(name)) {
                    return name;
                }
            }
        }
        return null;
    }

    private static boolean isWhitelisted(String name, VibeVisualsConfig.AutoLeaveConfig config) {
        String lower = name.toLowerCase(Locale.ROOT);
        for (String allowed : config.whitelist) {
            if (allowed != null && allowed.toLowerCase(Locale.ROOT).equals(lower)) {
                return true;
            }
        }
        return false;
    }

    private static void requestNear(MinecraftClient client) {
        ClientPlayNetworkHandler handler = client.getNetworkHandler();
        if (handler == null) {
            return;
        }
        listenTicks = RESPONSE_WINDOW_TICKS;
        handler.sendChatCommand("near");
    }

    private static void startWarning(MinecraftClient client, String playerName,
                                     VibeVisualsConfig.AutoLeaveConfig config) {
        pendingPlayer = playerName;
        listenTicks = 0;

        if (config.warningSeconds <= 0) {
            disconnect(client);
            return;
        }

        warningTicks = config.warningSeconds * 20;
        client.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.0f, 1.5f);
        client.player.sendMessage(
                Text.translatable("vibevisuals.autoLeave.warning", playerName, config.warningSeconds)
                        .formatted(Formatting.RED),
                false);
    }

    private static void cancelWarning(MinecraftClient client) {
        warningTicks = 0;
        pendingPlayer = null;
        sinceLastCheckTicks = 0;
        if (client.player != null) {
            client.player.sendMessage(
                    Text.translatable("vibevisuals.autoLeave.cancelled").formatted(Formatting.GREEN),
                    true);
        }
    }

    private static void disconnect(MinecraftClient client) {
        String playerName = pendingPlayer != null ? pendingPlayer : "?";
        reset();

        ClientPlayNetworkHandler handler = client.getNetworkHandler();
        if (handler != null) {
            handler.getConnection().disconnect(
                    Text.translatable("vibevisuals.autoLeave.reason", playerName));
        }
    }

    private static boolean playerIsActive(MinecraftClient client) {
        if (client.currentScreen instanceof ChatScreen) {
            return true;
        }
        return client.options.forwardKey.isPressed()
                || client.options.backKey.isPressed()
                || client.options.leftKey.isPressed()
                || client.options.rightKey.isPressed()
                || client.options.jumpKey.isPressed()
                || client.options.sneakKey.isPressed()
                || client.options.attackKey.isPressed()
                || client.options.useKey.isPressed();
    }

    private static void reset() {
        sinceLastCheckTicks = 0;
        listenTicks = 0;
        warningTicks = 0;
        pendingPlayer = null;
    }
}