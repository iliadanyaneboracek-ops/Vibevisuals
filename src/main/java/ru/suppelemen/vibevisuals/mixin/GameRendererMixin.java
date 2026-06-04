package ru.suppelemen.vibevisuals.mixin;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.suppelemen.vibevisuals.feature.marker.MarkerManager;
import ru.suppelemen.vibevisuals.feature.utility.ZoomController;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "getFov(Lnet/minecraft/client/render/Camera;FZ)F", at = @At("RETURN"), cancellable = true)
    private void vibevisuals$applyZoom(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Float> cir) {
        float factor = ZoomController.currentFactor();
        if (factor < 0.999f) {
            cir.setReturnValue(cir.getReturnValueF() * factor);
        }
    }

    @Inject(method = "getProjectionMatrix(F)Lorg/joml/Matrix4f;", at = @At("RETURN"))
    private void vibevisuals$captureProjection(float fov, CallbackInfoReturnable<Matrix4f> cir) {
        Matrix4f matrix = cir.getReturnValue();
        if (matrix != null) {
            MarkerManager.setProjectionMatrix(matrix);
        }
    }
}