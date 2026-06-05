package ru.suppelemen.vibevisuals.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.suppelemen.vibevisuals.feature.pvp.PvpCombatTracker;
import ru.suppelemen.vibevisuals.feature.pvp.TotemCounter;
import ru.suppelemen.vibevisuals.feature.utility.AutoLeaveController;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    @Inject(method = "onEntityStatus", at = @At("HEAD"))
    private void vibevisuals$trackCombatEntityStatus(EntityStatusS2CPacket packet, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return;
        }

        Entity entity = packet.getEntity(client.world);
        if (entity != null) {
            PvpCombatTracker.onEntityStatus(entity, packet.getStatus());
            TotemCounter.onEntityStatus(entity, packet.getStatus());
        }
    }

    @Inject(method = "onGameMessage", at = @At("HEAD"))
    private void vibevisuals$autoLeaveOnGameMessage(GameMessageS2CPacket packet, CallbackInfo ci) {
        if (packet.content() != null) {
            AutoLeaveController.onGameMessage(packet.content().getString());
        }
    }
}
