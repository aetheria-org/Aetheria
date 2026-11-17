package io.hamlook.aetheria.mixins.renderer;

import io.hamlook.aetheria.core.features.cosmetics.ItemAnimationsConfig;
import io.hamlook.aetheria.features.cosmetics.itemanimations.ItemAnimations;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityLivingBase.class)
public class MixinEntityLivingBaseSwingDuration {

    @Inject(method = "getArmSwingAnimationEnd", at = @At("RETURN"), cancellable = true)
    private void ATHR$applySwingSpeed(CallbackInfoReturnable<Integer> cir) {
        EntityLivingBase self = (EntityLivingBase) (Object) this;
        if (!(self instanceof EntityPlayerSP)) return;
        ItemAnimationsConfig config = ItemAnimations.config();
        if (config == null || !config.enabled) return;
        cir.setReturnValue(ItemAnimations.swingLength(cir.getReturnValue(), config.swingSpeed, config.ignoreHaste));
    }
}
