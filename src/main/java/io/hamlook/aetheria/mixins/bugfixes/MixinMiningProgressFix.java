package io.hamlook.aetheria.mixins.bugfixes;

import io.hamlook.aetheria.utils.hooks.MiningProgressHook;
import io.hamlook.aetheria.utils.item.ItemUtils;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerControllerMP.class)
public abstract class MixinMiningProgressFix {

    @Redirect(method = "isHittingPosition",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;areItemStackTagsEqual(Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemStack;)Z"))
    private boolean ATHR$sameTool(ItemStack stackA, ItemStack stackB) {
        if (MiningProgressHook.isEnabled() && ItemUtils.isSameTool(stackA, stackB)) return true;
        return ItemStack.areItemStackTagsEqual(stackA, stackB);
    }
}
