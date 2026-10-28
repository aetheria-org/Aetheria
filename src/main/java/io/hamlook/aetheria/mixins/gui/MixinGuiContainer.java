package io.hamlook.aetheria.mixins.gui;

import io.hamlook.aetheria.features.storage.StorageManager;
import io.hamlook.aetheria.utils.compat.DrawContext;
import io.hamlook.aetheria.utils.hooks.GuiContainerHook;
//#if MC < 1.21
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
//#else
//$$ import net.minecraft.client.gui.screen.Screen;
//$$ import net.minecraft.client.gui.screen.ingame.HandledScreen;
//$$ import net.minecraft.client.gui.widget.ClickableWidget;
//$$ import net.minecraft.text.Text;
//#endif
import net.minecraft.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//#if MC < 1.21
@Mixin(GuiContainer.class)
public abstract class MixinGuiContainer extends GuiScreen {
//#else
//$$ @Mixin(HandledScreen.class)
//$$ public abstract class MixinGuiContainer extends Screen {
//$$     public MixinGuiContainer() { super(Text.empty()); }
//#endif

    //#if MC < 1.21
    @Shadow
    public int guiLeft;
    @Shadow
    public int guiTop;
    //#endif
    //#if MC < 1.16
    @Unique
    public GuiButton aetheria$button;
    //#else
    //$$ @Unique
    //$$ public ClickableWidget aetheria$button;
    //#endif
    //#if MC < 1.21
    @Shadow
    private Slot theSlot;
    //#endif

    //#if MC < 1.21
    @Inject(method = "drawScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/inventory/GuiContainer;drawGuiContainerForegroundLayer(II)V", shift = At.Shift.AFTER))
    public void ATHR$afterDrawForeground(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        GuiContainerHook.afterDrawForeground(new DrawContext(), (GuiContainer) (Object) this, mouseX, mouseY);
    }
    //#endif

    //#if MC < 1.21
    @Inject(method = "onGuiClosed", at = @At("HEAD"))
    private void ATHR$onGuiClosed(CallbackInfo ci) {
        GuiContainerHook.onGuiClosed((GuiContainer) (Object) this);
    }

    @Inject(method = "drawSlot", at = @At("HEAD"), cancellable = true)
    private void ATHR$cancelBlankPaneRender(Slot slot, CallbackInfo ci) {
        if (slot != null && GuiContainerHook.cancelBlankPaneRender(slot)) {
            ci.cancel();
        }
    }

    @Redirect(method = "drawScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/inventory/Slot;canBeHovered()Z"))
    public boolean ATHR$blankPaneHover(Slot slot) {
        if (GuiContainerHook.suppressBlankPaneHover(slot)) return false;
        return slot.canBeHovered();
    }

    @Inject(method = "keyTyped", at = @At("HEAD"))
    private void ATHR$nbtCopy(char typedChar, int keyCode, CallbackInfo ci) {
        GuiContainerHook.nbtCopy(this.theSlot, keyCode);
    }
    //#endif



    @Inject(method = "handleMouseClick", at = @At("HEAD"), cancellable = true)
    private void ATHR$protectItemClick(Slot slot, int slotId, int clickedButton, int clickType, CallbackInfo ci) {
        if (GuiContainerHook.protectItemClick((GuiContainer) (Object) this, slot, slotId, clickedButton, clickType)) {
            ci.cancel();
        }
    }

    @Inject(method = "keyTyped", at = @At("HEAD"), cancellable = true)
    private void ATHR$protectItemKey(char typedChar, int keyCode, CallbackInfo ci) {
        if (GuiContainerHook.protectItemKey(this.theSlot, keyCode)) {
            ci.cancel();
        }
    }

    @Inject(method = "drawSlot", at = @At("RETURN"))
    private void ATHR$searchHighlight(Slot slot, CallbackInfo ci) {
        GuiContainerHook.searchHighlight((GuiContainer) (Object) this, slot);
    }

    @Inject(method = "isMouseOverSlot", at = @At("HEAD"), cancellable = true)
    public void ATHR$storageIsMouseOverSlot(Slot slotIn, int mouseX, int mouseY, CallbackInfoReturnable<Boolean> cir) {
        StorageManager.overrideIsMouseOverSlot(slotIn, mouseX, mouseY, cir);
    }

    @Inject(method = "drawSlot", at = @At("HEAD"), cancellable = true)
    public void ATHR$storageDrawSlot(Slot slot, CallbackInfo ci) {
        if (StorageManager.isOverlayActive() && StorageManager.isStorageChest()) {
            ci.cancel();
        }
    }
}
