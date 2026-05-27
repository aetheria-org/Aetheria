package io.hamlook.aetheria.mixins;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.events.RenderEntityModelEvent;
import io.hamlook.aetheria.features.qol.DamageNameplates;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.common.MinecraftForge;
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
        return DamageNameplates.replaceName(entity);
    }

    @Inject(method = "canRenderName(Lnet/minecraft/entity/EntityLivingBase;)Z", at = @At("HEAD"), cancellable = true)
    private void ATHR$showOwnNametag(T entity, CallbackInfoReturnable<Boolean> cir) {
        if (ATHRConfig.feature == null) return;
        if (ATHRConfig.feature.misc.showOwnNametag && entity == Minecraft.getMinecraft().thePlayer)
            cir.setReturnValue(true);
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
        float limbSwing       = entity.limbSwing - entity.limbSwingAmount * (1.0F - partialTicks);
        float limbSwingAmount = entity.prevLimbSwingAmount + (entity.limbSwingAmount - entity.prevLimbSwingAmount) * partialTicks;
        float ageInTicks      = entity.ticksExisted + partialTicks;
        float headYaw         = entity.prevRotationYawHead + (entity.rotationYawHead - entity.prevRotationYawHead) * partialTicks;
        float headPitch       = entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks;

        MinecraftForge.EVENT_BUS.post(new RenderEntityModelEvent(
                entity, limbSwing, limbSwingAmount, ageInTicks, headYaw, headPitch, 0.0625F, mainModel
        ));
    }
}