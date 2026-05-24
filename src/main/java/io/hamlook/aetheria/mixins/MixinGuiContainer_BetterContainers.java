package io.hamlook.aetheria.mixins;

import io.hamlook.aetheria.features.qol.BetterContainers;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiContainer.class)
public class MixinGuiContainer_BetterContainers {

    @Inject(method = "onGuiClosed", at = @At("HEAD"))
    private void ATHR$onGuiClosed(CallbackInfo ci) {
        if ((Object) this instanceof GuiChest) {
            BetterContainers.getInstance().reset();
        }
    }
    @Inject(method = "drawSlot", at = @At("HEAD"), cancellable = true)
    private void ATHR$cancelBlankPaneRender(Slot slot, CallbackInfo ci) {
        if (slot == null) return;

        ItemStack stack = slot.getStack();

        if (BetterContainers.isEnabled()
                && BetterContainers.getInstance().isLoaded()
                && !BetterContainers.shouldRenderStack(slot.slotNumber, stack)) {
            ci.cancel();
        }
    }
}