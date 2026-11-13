package io.hamlook.aetheria.mixins.renderer;

import io.hamlook.aetheria.core.features.cosmetics.ItemAnimationsConfig;
import io.hamlook.aetheria.features.cosmetics.itemanimations.ItemAnimations;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public class MixinHeldItemRendererAnimations {

    @Redirect(method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;"
            + "Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;"
            + "Lnet/minecraft/client/network/ClientPlayerEntity;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getHandSwingProgress(F)F"), require = 1)
    private float ATHR$remapSwingProgress(ClientPlayerEntity player, float tickDelta) {
        ItemAnimationsConfig config = ItemAnimations.config();
        if (config == null) return player.getHandSwingProgress(tickDelta);
        return ItemAnimations.swingProgress(player.getHandSwingProgress(tickDelta), config.swingSpeed);
    }

    @Redirect(method = "renderFirstPersonItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V", ordinal = 12), require = 1)
    private void ATHR$scaleSwingBob(MatrixStack matrices, float x, float y, float z) {
        ItemAnimationsConfig config = ItemAnimations.config();
        float range = config == null || !config.enabled
                ? ItemAnimations.VANILLA_SWING_RANGE
                : ItemAnimations.swingRange();
        matrices.translate(x * range, y * range, z * range);
    }

    @Inject(method = "applyEquipOffset", at = @At("HEAD"), cancellable = true, require = 1)
    private void ATHR$applyItemAnimations(MatrixStack matrices, Arm arm, float equipProgress, CallbackInfo ci) {
        ItemAnimationsConfig config = ItemAnimations.config();
        if (config == null || !config.enabled) return;
        ci.cancel();

        float sign = arm == Arm.RIGHT ? 1.0F : -1.0F;
        float size = config.scale;

        matrices.translate(ItemAnimations.pivotX(sign, config.offsetX),
                ItemAnimations.pivotY(config.offsetY) + equipProgress * ItemAnimations.EQUIP_DROP,
                ItemAnimations.pivotZ(config.offsetZ));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(config.pitch));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(config.yaw));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(config.roll));
        matrices.scale(size, size, size);
    }

    @Inject(method = "applySwingOffset", at = @At("HEAD"), cancellable = true, require = 1)
    private void ATHR$applySwingOffset(MatrixStack matrices, Arm arm, float swingProgress, CallbackInfo ci) {
        ItemAnimationsConfig config = ItemAnimations.config();
        if (config == null || !config.enabled) return;
        float range = ItemAnimations.swingRange();
        if (Math.abs(range - ItemAnimations.VANILLA_SWING_RANGE) < 0.001f) return;
        ci.cancel();

        float sign = arm == Arm.RIGHT ? 1.0F : -1.0F;
        float swingSin = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
        float swingSqrtSin = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);

        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sign * (45.0F + swingSin * -20.0F * range)));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(sign * swingSqrtSin * -20.0F * range));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(swingSqrtSin * -80.0F * range));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sign * -45.0F));
    }
}
