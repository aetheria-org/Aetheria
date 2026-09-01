package io.hamlook.aetheria.mixins.renderer;

import io.hamlook.aetheria.core.ATHRConfig;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Render.class)
public class MixinRender {

    @Inject(method = "renderEntityOnFire", at = @At("HEAD"), cancellable = true)
    private void ATHR$disableEntityFire(Entity entity, double x, double y, double z, float partialTicks, CallbackInfo ci) {
        if (ATHRConfig.feature != null && ATHRConfig.feature.misc.disableEntityFire)
            ci.cancel();
    }
}
