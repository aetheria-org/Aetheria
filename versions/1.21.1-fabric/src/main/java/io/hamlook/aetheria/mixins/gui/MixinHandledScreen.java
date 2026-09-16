package io.hamlook.aetheria.mixins.gui;

import io.hamlook.aetheria.events.GuiContainerRenderBeforeTooltipEvent;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public abstract class MixinHandledScreen {

    @Inject(method = "drawForeground(Lnet/minecraft/client/gui/DrawContext;II)V", at = @At("TAIL"))
    private void athr$afterDrawForeground(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
        new GuiContainerRenderBeforeTooltipEvent(context, (HandledScreen) (Object) this, mouseX, mouseY).post();
    }
}
