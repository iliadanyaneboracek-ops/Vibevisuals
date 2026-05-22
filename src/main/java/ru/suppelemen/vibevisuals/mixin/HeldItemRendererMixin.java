package ru.suppelemen.vibevisuals.mixin;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;

import java.util.Locale;

@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {
    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"))
    private void vibevisuals$customHand(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, OrderedRenderCommandQueue queue, int light, CallbackInfo ci) {
        VibeVisualsConfig.CustomHandConfig config = VibeVisualsConfigManager.get().customHand;
        if (!config.enabled || hand != Hand.MAIN_HAND) {
            return;
        }

        float swing = (float) Math.sin(swingProgress * Math.PI);
        switch (config.mode.trim().toUpperCase(Locale.ROOT)) {
            case "HORIZONTAL" -> {
                matrices.translate(0.08f, -0.05f, -0.10f);
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(82.0f));
            }
            case "LOW" -> matrices.translate(0.0f, -0.34f, 0.10f);
            case "SIDE" -> {
                matrices.translate(0.36f, -0.05f, -0.06f);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-18.0f));
            }
            case "STAB" -> {
                matrices.translate(0.0f, 0.0f, -swing * config.swingAmount);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-10.0f * swing));
            }
            case "SWING" -> {
                matrices.translate(0.0f, swing * config.swingAmount * 0.22f, -swing * config.swingAmount * 0.30f);
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-26.0f * swing));
            }
            default -> {
            }
        }

        matrices.translate(config.x, config.y, config.z);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(config.pitch));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(config.yaw));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(config.roll));
        matrices.scale(config.scale, config.scale, config.scale);
    }
}
