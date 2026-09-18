package io.hamlook.aetheria.mixins.renderer;

import io.hamlook.aetheria.core.ATHRConfig;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public class MixinRender {

    @Inject(method = "renderFire", at = @At("HEAD"), cancellable = true)
    private void ATHR$disableEntityFire(CallbackInfo ci) {
        if (ATHRConfig.feature != null && ATHRConfig.feature.misc.disableEntityFire)
            ci.cancel();
    }
}
