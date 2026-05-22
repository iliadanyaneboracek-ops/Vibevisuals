package ru.suppelemen.vibevisuals.mixin;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.SkyRendering;
import net.minecraft.client.render.state.SkyRenderState;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;
import ru.suppelemen.vibevisuals.feature.visual.VisualEffects;

@Mixin(SkyRendering.class)
public class SkyRenderingMixin {
    @Inject(method = "updateRenderState", at = @At("TAIL"))
    private void vibevisuals$overrideSkyColor(ClientWorld world, float tickProgress, Camera camera, SkyRenderState state, CallbackInfo ci) {
        VibeVisualsConfig.VisualEffectsConfig config = VibeVisualsConfigManager.get().visualEffects;
        if (config.skyColorEnabled) {
            state.skyColor = VisualEffects.withOpaqueAlpha(config.skyColor);
        }
    }
}
