package ru.suppelemen.vibevisuals.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;

/**
 * Suppresses vanilla first-person view-bob ("screen shake" while walking /
 * swinging) — but ONLY while a custom-hand item is actually held. With an
 * empty hand everything stays vanilla (bob + punch animation), so the player
 * gets normal feedback when not holding anything.
 */
@Mixin(GameRenderer.class)
public class GameRendererBobMixin {

    private static boolean vibevisuals$suppressBob() {
        VibeVisualsConfig.CustomHandConfig c = VibeVisualsConfigManager.get().customHand;
        if (!c.enabled) return false;
        // Only when the main hand holds something — empty hand stays vanilla.
        PlayerEntity p = MinecraftClient.getInstance().player;
        return p != null && !p.getStackInHand(Hand.MAIN_HAND).isEmpty();
    }

    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void vibevisuals$cancelBob(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        if (vibevisuals$suppressBob()) ci.cancel();
    }
}
