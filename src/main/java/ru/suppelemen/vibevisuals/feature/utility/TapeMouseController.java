package ru.suppelemen.vibevisuals.feature.utility;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.EntityHitResult;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;

public final class TapeMouseController {
    private static int clickCooldown;

    private TapeMouseController() {
    }

    public static void tick(MinecraftClient client) {
        if (client == null || client.player == null || client.world == null) {
            return;
        }

        VibeVisualsConfig.TapeMouseConfig config = VibeVisualsConfigManager.get().tapeMouse;
        if (!config.enabled || client.currentScreen != null || client.interactionManager == null) {
            return;
        }

        if (clickCooldown > 0) {
            clickCooldown--;
            return;
        }

        if (!(client.crosshairTarget instanceof EntityHitResult entityHit)) {
            return;
        }

        Entity target = entityHit.getEntity();
        if (!target.isAlive() || target == client.player) {
            return;
        }

        client.interactionManager.attackEntity(client.player, target);
        client.player.swingHand(client.player.getActiveHand());
        clickCooldown = Math.max(1, config.clickDelayTicks);
    }
}
