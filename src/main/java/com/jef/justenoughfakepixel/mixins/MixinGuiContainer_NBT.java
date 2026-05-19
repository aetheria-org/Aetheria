package com.jef.justenoughfakepixel.mixins;

import com.jef.justenoughfakepixel.core.JefConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiContainer.class)
public class MixinGuiContainer_NBT {

    @Shadow
    private Slot theSlot;

    @Inject(method = "keyTyped", at = @At("HEAD"))
    private void onKeyTyped(char typedChar, int keyCode, CallbackInfo ci) {
        if (keyCode == JefConfig.feature.debug.copyNBTKey && JefConfig.feature.debug.copyNBTData) {
            if (this.theSlot != null && this.theSlot.getHasStack()) {
                ItemStack stack = this.theSlot.getStack();

                if (stack.hasTagCompound()) {
                    String nbtString = stack.getTagCompound().toString();
                    GuiScreen.setClipboardString(nbtString);

                    Minecraft.getMinecraft().thePlayer.addChatMessage(
                            new ChatComponentText(EnumChatFormatting.GREEN + "Copied NBT to clipboard!")
                    );
                } else {
                    Minecraft.getMinecraft().thePlayer.addChatMessage(
                            new ChatComponentText(EnumChatFormatting.RED + "This item has no NBT data.")
                    );
                }
            }
        }
    }

}
