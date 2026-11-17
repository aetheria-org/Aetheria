package io.hamlook.aetheria.mixins.bugfixes;

import io.hamlook.aetheria.utils.hooks.SwapAnimationFixHook;
import io.hamlook.aetheria.utils.item.ItemUtils;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class MixinReequipAnimationFix {

    @Inject(method = "shouldCauseReequipAnimation", at = @At("HEAD"), cancellable = true, remap = false)
    private void ATHR$keepSwapAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged,
                                        CallbackInfoReturnable<Boolean> cir) {
        if (!SwapAnimationFixHook.isEnabled()) return;
        if (slotChanged) return;
        if (ItemUtils.isSameTool(oldStack, newStack)) cir.setReturnValue(false);
    }
}
