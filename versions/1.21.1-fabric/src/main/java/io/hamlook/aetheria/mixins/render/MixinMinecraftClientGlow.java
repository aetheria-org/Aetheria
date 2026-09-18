package io.hamlook.aetheria.mixins.render;

import io.hamlook.aetheria.utils.render.EntityOutlines;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class MixinMinecraftClientGlow {

    @Inject(method = "hasOutline(Lnet/minecraft/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void ATHR$forceGlow(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!entity.isInvisible() && EntityOutlines.glowColor(entity) != null) {
            cir.setReturnValue(true);
        }
    }
}
