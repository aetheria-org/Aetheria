package io.hamlook.aetheria.mixins.renderer;

import io.hamlook.aetheria.core.features.cosmetics.ItemAnimationsConfig;
import io.hamlook.aetheria.features.cosmetics.itemanimations.ItemAnimations;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.util.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public class MixinItemRenderer {

    @Redirect(method = "renderItemInFirstPerson",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/AbstractClientPlayer;getSwingProgress(F)F"))
    private float ATHR$remapSwingProgress(AbstractClientPlayer player, float partialTicks) {
        ItemAnimationsConfig config = ItemAnimations.config();
        if (config == null) return player.getSwingProgress(partialTicks);
        return ItemAnimations.swingProgress(player.getSwingProgress(partialTicks), config.swingSpeed);
    }

    @Inject(method = "transformFirstPersonItem", at = @At("HEAD"), cancellable = true)
    private void ATHR$applyItemAnimations(float equipProgress, float swingProgress, CallbackInfo ci) {
        ItemAnimationsConfig config = ItemAnimations.config();
        if (config == null || !config.enabled) return;
        ci.cancel();

        float range = ItemAnimations.swingRange();
        float size = ItemAnimations.BASE_SCALE * config.scale;
        float swingSin = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
        float swingSqrtSin = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float) Math.PI);

        GlStateManager.translate(ItemAnimations.pivotX(1f, config.offsetX),
                ItemAnimations.pivotY(config.offsetY),
                ItemAnimations.pivotZ(config.offsetZ));
        GlStateManager.translate(0.0F, equipProgress * ItemAnimations.EQUIP_DROP, 0.0F);
        GlStateManager.rotate(config.pitch, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(config.yaw, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(config.roll, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(swingSin * -20.0F * range, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(swingSqrtSin * -20.0F * range, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(swingSqrtSin * -80.0F * range, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(size, size, size);
    }

    @Inject(method = "doItemUsedTransformations", at = @At("HEAD"), cancellable = true)
    private void ATHR$applySwingBob(float swingProgress, CallbackInfo ci) {
        ItemAnimationsConfig config = ItemAnimations.config();
        if (config == null || !config.enabled) return;
        float range = ItemAnimations.swingRange();
        if (Math.abs(range - ItemAnimations.VANILLA_SWING_RANGE) < 0.001f) return;
        ci.cancel();

        float sqrt = MathHelper.sqrt_float(swingProgress);
        float bobX = -0.4F * MathHelper.sin(sqrt * (float) Math.PI);
        float bobY = 0.2F * MathHelper.sin(sqrt * (float) Math.PI * 2.0F);
        float bobZ = -0.2F * MathHelper.sin(swingProgress * (float) Math.PI);
        GlStateManager.translate(bobX * range, bobY * range, bobZ * range);
    }
}
