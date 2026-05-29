package ru.suppelemen.vibevisuals.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;
import ru.suppelemen.vibevisuals.feature.hud.CustomCrosshairRenderer;
import ru.suppelemen.vibevisuals.feature.hud.CustomHotbarRenderer;
import ru.suppelemen.vibevisuals.feature.hud.FireOverlayRenderer;
import ru.suppelemen.vibevisuals.feature.hud.HealingHelperRenderer;
import ru.suppelemen.vibevisuals.feature.hud.LockSlotOverlayRenderer;
import ru.suppelemen.vibevisuals.feature.hud.SaturationDisplayRenderer;
import ru.suppelemen.vibevisuals.feature.hud.SlotTimersRenderer;
import net.minecraft.entity.player.PlayerEntity;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true)
    private void vibevisuals$renderCustomHotbar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        VibeVisualsConfig config = VibeVisualsConfigManager.get();
        FireOverlayRenderer.render(context);
        if (config.hudEnabled && config.hotbar.enabled) {
            CustomHotbarRenderer.render(context);
            ci.cancel();
        }
    }

    @Inject(method = "renderHotbar", at = @At("TAIL"))
    private void vibevisuals$renderHotbarOverlays(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        int barX = context.getScaledWindowWidth() / 2 - 91;
        int barY = context.getScaledWindowHeight() - 22;
        HealingHelperRenderer.render(context, barX + 1, barY + 1, 20, 18);
        SlotTimersRenderer.render(context, barX + 3, barY + 3, 20, 16);
        LockSlotOverlayRenderer.renderHotbarRow(context, barX + 3, barY + 3, 16, 16, 20);
    }

    @Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"), cancellable = true)
    private void vibevisuals$hideVanillaStatusEffects(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        VibeVisualsConfig config = VibeVisualsConfigManager.get();
        if (config.hudEnabled && config.potionsCard.enabled) {
            ci.cancel();
        }
    }

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void vibevisuals$renderCustomCrosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        VibeVisualsConfig config = VibeVisualsConfigManager.get();
        if (config.customCrosshair.enabled) {
            CustomCrosshairRenderer.render(context);
            if (config.customCrosshair.hideVanilla) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "renderFood", at = @At("TAIL"))
    private void vibevisuals$renderSaturationDisplay(DrawContext context, PlayerEntity player, int top, int right, CallbackInfo ci) {
        SaturationDisplayRenderer.render(context, player, top, right);
    }
}
