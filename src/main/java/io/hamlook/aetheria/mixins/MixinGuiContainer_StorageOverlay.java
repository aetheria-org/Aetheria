package io.hamlook.aetheria.mixins;

import io.hamlook.aetheria.features.storage.StorageManager;
import io.hamlook.aetheria.utils.ContainerUtils;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GuiContainer.class)
public class MixinGuiContainer_StorageOverlay {

    @Inject(method = "isMouseOverSlot", at = @At("HEAD"), cancellable = true)
    public void isMouseOverSlot(Slot slotIn, int mouseX, int mouseY, CallbackInfoReturnable<Boolean> cir) {
        StorageManager.overrideIsMouseOverSlot(slotIn, mouseX, mouseY, cir);
    }

    @Inject(method = "drawSlot", at = @At("HEAD"), cancellable = true)
    public void drawSlot(Slot slot, CallbackInfo ci) {
        if (StorageManager.isOverlayActive() && ContainerUtils.isChestOpen()) {
            ci.cancel();
        }
    }
}