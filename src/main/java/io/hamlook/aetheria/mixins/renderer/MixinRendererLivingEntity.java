package io.hamlook.aetheria.mixins.renderer;

import io.hamlook.aetheria.utils.hooks.RendererLivingEntityHook;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.IChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RendererLivingEntity.class)
public abstract class MixinRendererLivingEntity<T extends EntityLivingBase> extends Render<T> {

    @Shadow protected ModelBase mainModel;

    protected MixinRendererLivingEntity(RenderManager renderManager) {
        super(renderManager);
    }

    @Redirect(
            method = "renderName(Lnet/minecraft/entity/EntityLivingBase;DDD)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;getDisplayName()Lnet/minecraft/util/IChatComponent;")
    )
    public IChatComponent ATHR$renderName_getDisplayName(EntityLivingBase entity) {
        return RendererLivingEntityHook.getDisplayName(entity);
    }

    @Inject(method = "canRenderName(Lnet/minecraft/entity/EntityLivingBase;)Z", at = @At("HEAD"), cancellable = true)
    private void ATHR$showOwnNametag(T entity, CallbackInfoReturnable<Boolean> cir) {
        Boolean hidden = RendererLivingEntityHook.shouldHideNametag(entity);
        if (hidden != null) {
            cir.setReturnValue(hidden);
            return;
        }
        Boolean result = RendererLivingEntityHook.shouldShowOwnNametag(entity);
        if (result != null) cir.setReturnValue(result);
    }

    @Inject(
            method = "doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/RendererLivingEntity;renderLayers(Lnet/minecraft/entity/EntityLivingBase;FFFFFFF)V",
                    shift = At.Shift.AFTER
            )
    )
    private void ATHR$fireRenderEntityModelEvent(T entity, double x, double y, double z,
                                                 float entityYaw, float partialTicks, CallbackInfo ci) {
        RendererLivingEntityHook.fireRenderEntityModelEvent(entity, partialTicks, mainModel);
    }
}
