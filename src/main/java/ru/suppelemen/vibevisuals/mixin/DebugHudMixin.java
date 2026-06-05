package ru.suppelemen.vibevisuals.mixin;

import net.minecraft.client.gui.hud.DebugHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import ru.suppelemen.vibevisuals.feature.utility.StreamerMode;

import java.util.List;

@Mixin(DebugHud.class)
public class DebugHudMixin {
    @ModifyVariable(
            method = "drawText(Lnet/minecraft/client/gui/DrawContext;Ljava/util/List;Z)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private List<String> vibevisuals$censorDebugText(List<String> lines) {
        return StreamerMode.censor(lines);
    }
}