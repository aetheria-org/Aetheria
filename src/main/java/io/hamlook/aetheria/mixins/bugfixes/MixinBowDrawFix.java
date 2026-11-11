package io.hamlook.aetheria.mixins.bugfixes;

import io.hamlook.aetheria.utils.compat.InventoryCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.hooks.BowDrawHook;
import io.hamlook.aetheria.utils.item.ItemUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityPlayer.class)
public abstract class MixinBowDrawFix {

    @Shadow
    private ItemStack itemInUse;

    @Inject(method = "onUpdate", at = @At("HEAD"))
    private void ATHR$keepBowDraw(CallbackInfo ci) {
        if (!BowDrawHook.isEnabled()) return;
        EntityPlayer self = (EntityPlayer) (Object) this;
        if (self != MinecraftCompat.getLocalPlayer()) return;
        ItemStack inUse = this.itemInUse;
        if (inUse == null) return;
        ItemStack held = InventoryCompat.getCurrentItem(self);
        if (held == null || held == inUse) return;
        if (held.getItem() != Items.bow) return;
        if (!ItemUtils.isSameTool(held, inUse)) return;
        if (!BowDrawHook.canKeepDrawing(held)) return;
        this.itemInUse = held;
    }
}
