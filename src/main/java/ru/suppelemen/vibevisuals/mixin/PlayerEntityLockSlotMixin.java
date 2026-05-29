package ru.suppelemen.vibevisuals.mixin;

import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;

@Mixin(ClientPlayerEntity.class)
public class PlayerEntityLockSlotMixin {
    @Inject(method = "dropSelectedItem(Z)Z", at = @At("HEAD"), cancellable = true)
    private void vibevisuals$blockLockedSlotDrop(boolean entireStack, CallbackInfoReturnable<Boolean> cir) {
        VibeVisualsConfig.LockSlotConfig config = VibeVisualsConfigManager.get().lockSlot;
        if (!config.enabled || !config.blockDrop) {
            return;
        }
        ClientPlayerEntity self = (ClientPlayerEntity) (Object) this;
        int selected = self.getInventory().getSelectedSlot();
        if (config.isHotbarSlotLocked(selected)) {
            cir.setReturnValue(false);
        }
    }
}
