package ru.suppelemen.vibevisuals.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.suppelemen.vibevisuals.feature.hud.ShulkerPreviewRenderer;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {
    @Shadow
    @Nullable
    protected Slot focusedSlot;

    @Inject(method = "render", at = @At("TAIL"))
    private void vibevisuals$renderShulkerPreview(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (focusedSlot != null && focusedSlot.hasStack()) {
            ShulkerPreviewRenderer.render(context, focusedSlot.getStack(), mouseX, mouseY);
        }
    }

    @Inject(method = "drawMouseoverTooltip", at = @At("HEAD"), cancellable = true)
    private void vibevisuals$suppressTooltipForPreview(DrawContext context, int x, int y, CallbackInfo ci) {
        if (focusedSlot != null && focusedSlot.hasStack() && ShulkerPreviewRenderer.shouldPreview(focusedSlot.getStack())) {
            ci.cancel();
        }
    }
}