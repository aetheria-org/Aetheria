package io.hamlook.aetheria.mixins.renderer;

import io.hamlook.aetheria.core.ATHRConfig;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ArmorFeatureRenderer.class)
public class MixinLayerArmorBase {

    @Inject(method = "renderGlint", at = @At("HEAD"), cancellable = true)
    private void ATHR$disableEnchantGlint(CallbackInfo ci) {
        if (ATHRConfig.feature != null && ATHRConfig.feature.qol.disableEnchantGlint)
            ci.cancel();
    }
}
