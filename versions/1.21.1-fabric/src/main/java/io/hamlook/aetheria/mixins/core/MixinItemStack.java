package io.hamlook.aetheria.mixins.core;

import io.hamlook.aetheria.core.ATHRConfig;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public class MixinItemStack {

    @Inject(method = "hasGlint", at = @At("HEAD"), cancellable = true)
    private void athr$disableEnchantGlint(CallbackInfoReturnable<Boolean> cir) {
        if (ATHRConfig.feature != null && ATHRConfig.feature.qol.disableEnchantGlint) {
            cir.setReturnValue(false);
        }
    }
}
