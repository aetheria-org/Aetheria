package io.hamlook.aetheria.mixins.renderer;

import io.hamlook.aetheria.utils.hooks.RendererLivingEntityHook;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class MixinLivingEntityRenderer {

    @Shadow public abstract EntityModel<?> getModel();

    @Inject(method = "hasLabel(Lnet/minecraft/entity/LivingEntity;)Z", at = @At("HEAD"), cancellable = true)
    private void ATHR$showOwnNametag(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        Boolean hidden = RendererLivingEntityHook.shouldHideNametag(entity);
        if (hidden != null) {
            cir.setReturnValue(hidden);
            return;
        }
        Boolean result = RendererLivingEntityHook.shouldShowOwnNametag(entity);
        if (result != null) cir.setReturnValue(result);
    }

    @Inject(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("TAIL"))
    private void ATHR$fireRenderEntityModelEvent(LivingEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                                                 VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        RendererLivingEntityHook.fireRenderEntityModelEvent(entity, tickDelta, getModel());
    }
}
