package io.hamlook.aetheria.mixins.renderer;

import io.hamlook.aetheria.utils.hooks.RendererLivingEntityHook;
import net.minecraft.client.render.entity.ArmorStandEntityRenderer;
import net.minecraft.entity.decoration.ArmorStandEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArmorStandEntityRenderer.class)
public abstract class MixinArmorStandEntityRenderer {

    @Inject(method = "hasLabel(Lnet/minecraft/entity/decoration/ArmorStandEntity;)Z", at = @At("HEAD"), cancellable = true)
    private void ATHR$hideDamageSplash(ArmorStandEntity entity, CallbackInfoReturnable<Boolean> cir) {
        Boolean hidden = RendererLivingEntityHook.shouldHideNametag(entity);
        if (hidden != null) {
            cir.setReturnValue(hidden);
        }
    }
}
