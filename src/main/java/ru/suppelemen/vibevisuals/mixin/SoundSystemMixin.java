package ru.suppelemen.vibevisuals.mixin;

import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.suppelemen.vibevisuals.feature.sound.SoundController;

@Mixin(SoundSystem.class)
public class SoundSystemMixin {
    @Inject(method = "getAdjustedVolume(Lnet/minecraft/client/sound/SoundInstance;)F", at = @At("RETURN"), cancellable = true)
    private void vibevisuals$scaleVolume(SoundInstance instance, CallbackInfoReturnable<Float> cir) {
        float multiplier = SoundController.multiplierFor(instance);
        if (multiplier != 1.0f) {
            cir.setReturnValue(cir.getReturnValueF() * multiplier);
        }
    }
}