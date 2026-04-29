package com.jef.justenoughfakepixel.mixins;

import com.jef.justenoughfakepixel.core.JefConfig;
import com.jef.justenoughfakepixel.events.SlotClickEvent;
import com.jef.justenoughfakepixel.features.misc.ProtectItemFeature;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiContainer.class)
public class MixinGuiContainer_ProtectItem {

    @Shadow public Slot theSlot;

    @Inject(
            method = "handleMouseClick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/PlayerControllerMP;windowClick(IIIILnet/minecraft/entity/player/EntityPlayer;)Lnet/minecraft/item/ItemStack;"
            ),
            cancellable = true
    )
    private void onHandleMouseClick(Slot slot, int slotId, int clickedButton, int clickType, CallbackInfo ci) {
        GuiContainer gui = (GuiContainer) (Object) this;
        SlotClickEvent event = new SlotClickEvent(gui, slot, slotId, clickedButton, clickType);
        MinecraftForge.EVENT_BUS.post(event);
        if (event.isCanceled()) {
            ci.cancel();
        }
    }

    @Inject(method = "keyTyped", at = @At("HEAD"), cancellable = true)
    private void onKeyTyped(char typedChar, int keyCode, CallbackInfo ci) {
        if (JefConfig.feature == null) return;
        int protectionKey = JefConfig.feature.misc.protectItem.protectionKey;
        if (protectionKey == Keyboard.KEY_NONE || keyCode != protectionKey) return;

        Slot hovered = this.theSlot;
        if (hovered != null && hovered.getStack() != null) {
            ProtectItemFeature.toggleProtection(hovered.getStack());
        }
        ci.cancel();
    }
}