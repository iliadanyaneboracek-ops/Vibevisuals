package ru.suppelemen.vibevisuals.mixin;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;

@Mixin(HandledScreen.class)
public class HandledScreenLockSlotMixin {
    @Inject(method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void vibevisuals$blockLockedSlotClick(Slot slot, int slotId, int button, SlotActionType actionType, CallbackInfo ci) {
        VibeVisualsConfig.LockSlotConfig config = VibeVisualsConfigManager.get().lockSlot;
        if (!config.enabled || !config.blockInventoryClicks) {
            return;
        }

        if (slot != null && slot.inventory instanceof PlayerInventory && config.isHotbarSlotLocked(slot.getIndex())) {
            ci.cancel();
            return;
        }

        if (actionType == SlotActionType.SWAP && button >= 0 && button < 9 && config.isHotbarSlotLocked(button)) {
            ci.cancel();
        }
    }
}
