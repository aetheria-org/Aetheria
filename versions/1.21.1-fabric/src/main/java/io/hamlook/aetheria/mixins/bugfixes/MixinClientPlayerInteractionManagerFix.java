package io.hamlook.aetheria.mixins.bugfixes;

import io.hamlook.aetheria.utils.hooks.MiningProgressHook;
import io.hamlook.aetheria.utils.item.ItemUtils;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class MixinClientPlayerInteractionManagerFix {

    @Redirect(method = "isCurrentlyBreaking",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;areItemsAndComponentsEqual(Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemStack;)Z"))
    private static boolean ATHR$sameTool(ItemStack first, ItemStack second) {
        if (MiningProgressHook.isEnabled() && ItemUtils.isSameTool(first, second)) return true;
        return ItemStack.areItemsAndComponentsEqual(first, second);
    }
}
