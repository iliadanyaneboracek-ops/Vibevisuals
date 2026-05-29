package ru.suppelemen.vibevisuals.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.ItemPickupAnimationS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.suppelemen.vibevisuals.feature.utility.ItemPickupLogger;

@Mixin(ClientPlayNetworkHandler.class)
public class ItemPickupLoggerMixin {
    @Inject(method = "onItemPickupAnimation", at = @At("HEAD"))
    private void vibevisuals$logItemPickup(ItemPickupAnimationS2CPacket packet, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) {
            return;
        }

        // Only log items the local player picks up, not nearby players.
        if (packet.getCollectorEntityId() != client.player.getId()) {
            return;
        }

        Entity entity = client.world.getEntityById(packet.getEntityId());
        if (entity instanceof ItemEntity itemEntity) {
            ItemStack stack = itemEntity.getStack();
            if (stack != null && !stack.isEmpty()) {
                ItemPickupLogger.onPickup(stack, packet.getStackAmount());
            }
        }
    }
}
