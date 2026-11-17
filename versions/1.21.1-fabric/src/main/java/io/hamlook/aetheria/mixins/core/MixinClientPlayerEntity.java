package io.hamlook.aetheria.mixins.core;

import io.hamlook.aetheria.events.ItemTossEvent;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerEntity.class)
public class MixinClientPlayerEntity {

    @Inject(method = "dropSelectedItem", at = @At("HEAD"), cancellable = true)
    private void athr$onDropSelectedItem(boolean entireStack, CallbackInfoReturnable<Boolean> cir) {
        ClientPlayerEntity self = (ClientPlayerEntity) (Object) this;
        ItemStack held = self.getMainHandStack();
        if (held == null || held.isEmpty()) return;
        ItemTossEvent event = new ItemTossEvent(held, entireStack);
        event.post();
        if (event.isCancelled()) {
            cir.setReturnValue(false);
        }
    }
}
