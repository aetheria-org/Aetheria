package io.hamlook.aetheria.mixins.bugfixes;

import io.hamlook.aetheria.utils.hooks.SwapAnimationFixHook;
import io.hamlook.aetheria.utils.item.ItemUtils;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(HeldItemRenderer.class)
public abstract class MixinHeldItemRendererSwapFix {

    @Redirect(method = "updateHeldItems",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;areEqual(Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemStack;)Z"))
    private static boolean ATHR$sameTool(ItemStack first, ItemStack second) {
        if (ItemStack.areEqual(first, second)) return true;
        return SwapAnimationFixHook.isEnabled() && ItemUtils.isSameTool(first, second);
    }
}
