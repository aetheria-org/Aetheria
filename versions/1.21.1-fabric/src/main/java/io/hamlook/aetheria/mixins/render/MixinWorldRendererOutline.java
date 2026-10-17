package io.hamlook.aetheria.mixins.render;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import io.hamlook.aetheria.utils.render.EntityRenderUtils;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class MixinWorldRendererOutline {

    @Inject(method = "render", at = @At("HEAD"))
    private void ATHR$collectOutlines(CallbackInfo ci) {
        EntityRenderUtils.beginFrame();
    }

    @ModifyExpressionValue(method = "render",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/MinecraftClient;hasOutline(Lnet/minecraft/entity/Entity;)Z"))
    private boolean ATHR$forceOutline(boolean original, @Local Entity entity) {
        return EntityRenderUtils.shouldOutline(entity, original);
    }

    @ModifyExpressionValue(method = "render",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/Entity;getTeamColorValue()I"))
    private int ATHR$outlineColor(int original, @Local Entity entity) {
        return EntityRenderUtils.outlineColor(entity, original);
    }
}
