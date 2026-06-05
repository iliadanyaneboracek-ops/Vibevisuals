package ru.suppelemen.vibevisuals.mixin;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfig;
import ru.suppelemen.vibevisuals.config.VibeVisualsConfigManager;

import java.util.Locale;

@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {

    // Self-driven SPIN animation state. Each hit adds one full turn to a target
    // angle; the current angle eases toward it. Spamming just stacks targets, so
    // the blade keeps spinning smoothly instead of snapping back to 0.
    private double vibevisuals$spinAngle = 0.0;    // current degrees (rendered)
    private double vibevisuals$spinTarget = 0.0;   // goal degrees (whole turns)
    private int vibevisuals$lastSwingTicks = -1;   // detects each new hit, even when spamming
    private long vibevisuals$lastMs = System.currentTimeMillis();

    /**
     * Kill vanilla's swing bob in SPIN mode. The method's 3rd float arg
     * (ordinal 2 among floats: tickDelta=0, pitch=1, swingProgress=2) is the
     * swing progress fed into applySwingOffset — zeroing it removes the
     * forward/back lunge so our own twirl is the only motion.
     */
    @ModifyVariable(method = "renderFirstPersonItem", at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private float vibevisuals$zeroSwing(float swingProgress, AbstractClientPlayerEntity player,
                                         float tickDelta, float pitch, Hand hand) {
        VibeVisualsConfig.CustomHandConfig config = VibeVisualsConfigManager.get().customHand;
        // Only suppress swing for a NON-EMPTY main hand in SPIN mode. Empty hand
        // keeps vanilla swing so the normal punch animation still plays.
        boolean held = !player.getStackInHand(hand).isEmpty();
        boolean spin = config.enabled && hand == Hand.MAIN_HAND && held
                && config.mode.trim().equalsIgnoreCase("SPIN");
        return spin ? 0.0f : swingProgress;
    }

    /**
     * SPIN custom-hand: take over rendering entirely. We cancel vanilla, build
     * a clean matrix at the item position, lay the blade flat and twirl it
     * around the vertical axis (helicopter), then render the model ourselves.
     * Full control = no fighting vanilla's leftover transforms.
     */
    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"), cancellable = true)
    private void vibevisuals$spinTakeover(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, OrderedRenderCommandQueue queue, int light, CallbackInfo ci) {
        VibeVisualsConfig.CustomHandConfig config = VibeVisualsConfigManager.get().customHand;
        if (!config.enabled || hand != Hand.MAIN_HAND || item.isEmpty()) return;
        if (!config.mode.trim().equalsIgnoreCase("SPIN")) return;

        Arm arm = player.getMainArm();
        HeldItemRendererInvoker inv = (HeldItemRendererInvoker) (Object) this;

        // Each hit ADDS one full 360° turn to the target; the current angle eases
        // toward it at a constant speed. Spamming stacks turns → continuous spin,
        // no snap-back. handSwingTicks resets on every hit (even mid-swing), so a
        // tick decrease = a fresh hit, reliable even under fast spam.
        long now = System.currentTimeMillis();
        int swingTicks = player.handSwingTicks;
        boolean newHit = player.handSwinging
                && (vibevisuals$lastSwingTicks < 0 || swingTicks < vibevisuals$lastSwingTicks);
        vibevisuals$lastSwingTicks = player.handSwinging ? swingTicks : -1;
        if (newHit) {
            // Queue at most ONE extra turn beyond the one in progress. If the
            // remaining angle is already more than a full turn, a spare circle
            // is already queued, so further hits during this circle are ignored.
            double remaining = vibevisuals$spinTarget - vibevisuals$spinAngle;
            if (remaining <= 360.0) {
                vibevisuals$spinTarget += 360.0;
            }
        }
        // Advance current angle toward target at a constant deg/sec (one turn
        // per 300 ms at speed 1.0). Never overshoots; settles exactly on target.
        float dt = (now - vibevisuals$lastMs) / 1000f;
        if (dt < 0f || dt > 0.25f) dt = 0f;
        double degPerSec = 360.0 * (config.spinSpeed / 0.30);
        if (vibevisuals$spinAngle < vibevisuals$spinTarget) {
            vibevisuals$spinAngle = Math.min(vibevisuals$spinTarget,
                    vibevisuals$spinAngle + degPerSec * dt);
        }
        // Keep the numbers from growing unbounded once caught up.
        if (vibevisuals$spinAngle >= vibevisuals$spinTarget) {
            double wrapped = vibevisuals$spinAngle % 360.0;
            vibevisuals$spinAngle = wrapped;
            vibevisuals$spinTarget = wrapped;
        }
        vibevisuals$lastMs = now;
        float spinDeg = (float) vibevisuals$spinAngle;   // absolute degrees

        matrices.push();
        // FULLY INDEPENDENT of vanilla: no applyEquipOffset → no equip-bob, no
        // swing lunge, NO view-shake. Built from scratch in camera space.
        //
        // 1. Move to the custom-hand anchor point (camera space). This single
        //    translate is the PIVOT — everything after rotates around it, so the
        //    spin happens about the hand position exactly as intended.
        //    Baked base = the user's tuned in-game pose; config offsets add on top.
        float baseX = 0.94f, baseY = -0.55f, baseZ = -1.60f;
        float ax = (baseX + config.x + config.spinX) * (arm == Arm.RIGHT ? 1f : -1f);
        float ay = baseY + config.y + config.spinY;
        float az = baseZ + config.z + config.spinZ;
        matrices.translate(ax, ay, az);
        // 2. Spin around the anchor (vertical Y axis = horizontal twirl). Being
        //    first after the translate, the pivot is the anchor itself — no arc.
        //    NEGATIVE = clockwise when viewed from above.
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-spinDeg));
        // 3. Lay the sword flat & horizontal (orientation only, no translation).
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-90.0f));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0f));
        // 4. Size (baked base × config multiplier).
        float baseScale = 1.0f;
        matrices.scale(baseScale * config.scale, baseScale * config.scale, baseScale * config.scale);
        // 5. Render the item model ourselves, then stop vanilla.
        net.minecraft.item.ItemDisplayContext ctx = arm == Arm.RIGHT
                ? net.minecraft.item.ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                : net.minecraft.item.ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
        inv.vibevisuals$renderItem(player, item, ctx, matrices, queue, light);
        matrices.pop();
        ci.cancel();
    }

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"))
    private void vibevisuals$customHand(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, OrderedRenderCommandQueue queue, int light, CallbackInfo ci) {
        VibeVisualsConfig.CustomHandConfig config = VibeVisualsConfigManager.get().customHand;
        if (!config.enabled || hand != Hand.MAIN_HAND) {
            return;
        }
        // SPIN is handled by the cancellable takeover above.
        if (config.mode.trim().equalsIgnoreCase("SPIN")) return;

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
            case "SPIN" -> {
                // SPIN fully takes over rendering (separate cancellable inject),
                // so nothing to do in the additive path here.
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
