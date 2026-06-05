package ru.suppelemen.vibevisuals.mixin;

import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Accessors for HeldItemRenderer's private helpers so the SPIN custom-hand
 * mode can fully take over first-person item rendering instead of fighting
 * vanilla's transform stack.
 */
@Mixin(HeldItemRenderer.class)
public interface HeldItemRendererInvoker {

    @Invoker("applyEquipOffset")
    void vibevisuals$applyEquipOffset(MatrixStack matrices, Arm arm, float equipProgress);

    @Invoker("renderItem")
    void vibevisuals$renderItem(LivingEntity entity, ItemStack stack, ItemDisplayContext context,
                                 MatrixStack matrices, OrderedRenderCommandQueue queue, int light);
}
