package ru.suppelemen.vibevisuals.mixin;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.fog.FogRenderer;
import net.minecraft.client.world.ClientWorld;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;
import ru.suppelemen.vibevisuals.feature.visual.VisualEffects;

@Mixin(FogRenderer.class)
public class FogRendererMixin {
    @Inject(method = "getFogColor", at = @At("RETURN"), cancellable = true)
    private void vibevisuals$overrideFogColor(Camera camera, float tickProgress, ClientWorld world, int viewDistance, float skyDarkness, CallbackInfoReturnable<Vector4f> cir) {
        VibeVisualsConfig.VisualEffectsConfig config = VibeVisualsConfigManager.get().visualEffects;
        if (!config.fogColorEnabled) {
            return;
        }

        Vector4f original = cir.getReturnValue();
        float mix = config.fogOpacity;
        float red = lerp(original.x, VisualEffects.red(config.fogColor), mix);
        float green = lerp(original.y, VisualEffects.green(config.fogColor), mix);
        float blue = lerp(original.z, VisualEffects.blue(config.fogColor), mix);
        cir.setReturnValue(new Vector4f(red, green, blue, original.w));
    }

    private static float lerp(float from, float to, float delta) {
        return from + (to - from) * delta;
    }
}
