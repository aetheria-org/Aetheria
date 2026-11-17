package io.hamlook.aetheria.mixins.gui;

import io.hamlook.aetheria.events.GuiContainerRenderBeforeTooltipEvent;
import io.hamlook.aetheria.utils.hooks.GuiChestHook;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * {@code InventoryScreen} declares its own {@code drawForeground} override, so
 * the {@code HandledScreen} mixin only patches that class's bytecode and never
 * fires for the player inventory. Post the before-tooltip event here instead,
 * and before {@code drawMouseoverTooltip} so tooltips stay on top.
 */
@Mixin(InventoryScreen.class)
public abstract class MixinInventoryScreen {

    @ModifyConstant(method = "drawForeground(Lnet/minecraft/client/gui/DrawContext;II)V",
            constant = @Constant(intValue = 4210752))
    private int athr$modifyContainerTitleColor(int original) {
        return GuiChestHook.modifyContainerTitleColor(original);
    }

    @Inject(method = "drawForeground(Lnet/minecraft/client/gui/DrawContext;II)V", at = @At("TAIL"))
    private void athr$afterDrawForeground(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
        new GuiContainerRenderBeforeTooltipEvent(context, (HandledScreen) (Object) this, mouseX, mouseY, true).post();
    }
}
