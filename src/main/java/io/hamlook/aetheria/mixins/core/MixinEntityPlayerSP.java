package io.hamlook.aetheria.mixins.core;

import io.hamlook.aetheria.events.ItemTossEvent;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityPlayerSP.class)
public class MixinEntityPlayerSP {

    @Inject(method = "dropOneItem", at = @At("HEAD"), cancellable = true)
    private void ATHR$onDropOneItem(boolean dropAll, CallbackInfoReturnable<EntityItem> cir) {
        EntityPlayerSP self = (EntityPlayerSP) (Object) this;
        ItemStack held = self.inventory.getCurrentItem();
        if (held == null) return;
        ItemTossEvent event = new ItemTossEvent(held, dropAll);
        event.post();
        if (event.isCancelled()) {
            cir.setReturnValue(null);
        }
    }
}
