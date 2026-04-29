package com.jef.justenoughfakepixel.mixins;

import com.jef.justenoughfakepixel.utils.render.HighlightUtils;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiContainer.class)
public class MixinGuiContainer_SearchHighlight {

    @Inject(method = "drawSlot", at = @At("RETURN"))
    public void drawSlot(Slot slot, CallbackInfo ci) {
        if (slot == null || slot.getStack() == null) return;
        HighlightUtils.renderAllHighlights((GuiContainer) (Object) this, slot);
    }
}