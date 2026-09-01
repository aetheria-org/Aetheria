package io.hamlook.aetheria.mixins.core;

import io.hamlook.aetheria.core.ATHRConfig;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public class MixinItem {

    @Inject(method = "shouldCauseReequipAnimation", at = @At("HEAD"), cancellable = true, remap = false)
    private void ATHR$cancelReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged, CallbackInfoReturnable<Boolean> ci) {
        if (ATHRConfig.feature == null) return;
        if (!ATHRConfig.feature.misc.noItemSwitchAnimation) return;
        ci.setReturnValue(false);
    }
}
