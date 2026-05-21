package ru.suppelemen.vibevisuals.mixin;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.suppelemen.vibevisuals.feature.screen.HudDragController;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin extends Screen {
    @Unique
    private final HudDragController vibevisuals$dragController = new HudDragController();

    protected ChatScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void vibevisuals$renderHudDragOutline(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        vibevisuals$dragController.renderOutline(context, mouseX, mouseY);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void vibevisuals$startHudDrag(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (vibevisuals$dragController.mouseClicked(click)) {
            cir.setReturnValue(true);
        }
    }

    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        return vibevisuals$dragController.mouseDragged(click, width, height) || super.mouseDragged(click, offsetX, offsetY);
    }

    public boolean mouseReleased(Click click) {
        return vibevisuals$dragController.mouseReleased() || super.mouseReleased(click);
    }
}
