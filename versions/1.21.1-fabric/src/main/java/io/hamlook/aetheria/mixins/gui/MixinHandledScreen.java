package io.hamlook.aetheria.mixins.gui;

import io.hamlook.aetheria.events.GuiContainerRenderBeforeTooltipEvent;
import io.hamlook.aetheria.features.dungeons.DungeonLeapOverlay;
import io.hamlook.aetheria.features.storage.StorageManager;
import io.hamlook.aetheria.utils.compat.DrawContextUtils;
import io.hamlook.aetheria.utils.hooks.GuiChestHook;
import io.hamlook.aetheria.utils.hooks.GuiContainerHook;
import io.hamlook.aetheria.utils.hooks.GuiScreenHook;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HandledScreen.class)
public abstract class MixinHandledScreen {

    @Shadow
    protected Slot focusedSlot;

    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;IIF)V", at = @At("HEAD"), cancellable = true)
    private void athr$suppressLeapMenu(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // Fabric's ScreenEvents.beforeRender cannot cancel a screen, so the leap
        // overlay's cancel is a no-op here. Forge suppresses the whole render via
        // DrawScreenEvent.Pre; this reproduces that so only the leap map shows.
        if (DungeonLeapOverlay.isLeapGUI) {
            ci.cancel();
        }
    }

    @ModifyConstant(method = "drawForeground(Lnet/minecraft/client/gui/DrawContext;II)V",
            constant = @Constant(intValue = 4210752))
    private int athr$modifyContainerTitleColor(int original) {
        return GuiChestHook.modifyContainerTitleColor(original);
    }

    @Redirect(method = "render(Lnet/minecraft/client/gui/DrawContext;IIF)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/screen/slot/Slot;canBeHighlighted()Z"))
    private boolean athr$blankPaneHoverHighlight(Slot slot) {
        if (GuiContainerHook.suppressBlankPaneHover(slot)) return false;
        return slot.canBeHighlighted();
    }

    @Inject(method = "drawForeground(Lnet/minecraft/client/gui/DrawContext;II)V", at = @At("TAIL"))
    private void athr$afterDrawForeground(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
        new GuiContainerRenderBeforeTooltipEvent(context, (HandledScreen) (Object) this, mouseX, mouseY, true).post();
    }

    @Inject(method = "drawForeground(Lnet/minecraft/client/gui/DrawContext;II)V",
            at = @At("HEAD"), cancellable = true)
    private void athr$cancelDrawForeground(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
        if (GuiChestHook.shouldCancelDraw()) {
            ci.cancel();
        }
    }

    @Inject(method = "drawMouseoverTooltip(Lnet/minecraft/client/gui/DrawContext;II)V",
            at = @At("HEAD"), cancellable = true)
    private void athr$tooltip(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
        if (GuiContainerHook.suppressBlankPaneHover(this.focusedSlot)) {
            ci.cancel();
            return;
        }
        ItemStack stack = this.focusedSlot == null ? ItemStack.EMPTY : this.focusedSlot.getStack();
        DrawContextUtils.setContext(context);
        try {
            if (GuiScreenHook.handleTooltip(stack, mouseX, mouseY)) {
                ci.cancel();
            }
        } finally {
            DrawContextUtils.clearContext();
        }
    }

    @Inject(method = "drawForeground(Lnet/minecraft/client/gui/DrawContext;II)V",
            at = @At("RETURN"))
    private void athr$drawWatermark(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
        if (!((Object) this instanceof GenericContainerScreen)) return;
        DrawContextUtils.setContext(context);
        try {
            GuiChestHook.drawWatermark((HandledScreen) (Object) this);
        } finally {
            DrawContextUtils.clearContext();
        }
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void athr$onGuiClosed(CallbackInfo ci) {
        GuiContainerHook.onGuiClosed((HandledScreen) (Object) this);
    }

    @Inject(method = "drawSlot(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/screen/slot/Slot;)V",
            at = @At("HEAD"), cancellable = true)
    private void athr$cancelSlotRender(DrawContext context, Slot slot, CallbackInfo ci) {
        if (slot != null && GuiContainerHook.cancelBlankPaneRender(slot)) {
            ci.cancel();
            return;
        }
        if (StorageManager.isOverlayActive() && StorageManager.isStorageChest()) {
            ci.cancel();
        }
    }

    @Inject(method = "isPointOverSlot(Lnet/minecraft/screen/slot/Slot;DD)Z",
            at = @At("HEAD"), cancellable = true)
    private void athr$storageSlotHover(Slot slot, double pointX, double pointY, CallbackInfoReturnable<Boolean> cir) {
        StorageManager.overrideIsMouseOverSlot(slot, (int) pointX, (int) pointY, cir);
    }

    @Inject(method = "isClickOutsideBounds(DDIII)Z", at = @At("HEAD"), cancellable = true)
    private void athr$storageClickBounds(double mouseX, double mouseY, int left, int top, int button,
                                         CallbackInfoReturnable<Boolean> cir) {
        if (StorageManager.isOverlayActive() && StorageManager.isStorageChest()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "drawSlot(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/screen/slot/Slot;)V",
            at = @At("RETURN"))
    private void athr$searchHighlight(DrawContext context, Slot slot, CallbackInfo ci) {
        DrawContextUtils.setContext(context);
        try {
            GuiContainerHook.searchHighlight((HandledScreen) (Object) this, slot);
        } finally {
            DrawContextUtils.clearContext();
        }
    }

    @Inject(method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V",
            at = @At("HEAD"), cancellable = true)
    private void athr$protectItemClick(Slot slot, int slotId, int button, SlotActionType actionType, CallbackInfo ci) {
        if (GuiContainerHook.suppressBlankPaneHover(slot)) {
            ci.cancel();
            return;
        }
        if (GuiContainerHook.protectItemClick((HandledScreen) (Object) this, slot,
                slot == null ? slotId : slot.id, button, actionType.ordinal())) {
            ci.cancel();
        }
    }

    @Inject(method = "mouseClicked(DDI)Z", at = @At("HEAD"))
    private void athr$profileMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        GuiContainerHook.profileMouseClicked((HandledScreen) (Object) this, this.focusedSlot, button);
    }

    @Inject(method = "keyPressed(III)Z", at = @At("HEAD"))
    private void athr$nbtCopy(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        GuiContainerHook.nbtCopy(this.focusedSlot, keyCode);
    }

    @Inject(method = "keyPressed(III)Z", at = @At("HEAD"), cancellable = true)
    private void athr$protectItemKey(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (GuiContainerHook.protectItemKey(this.focusedSlot, keyCode)) {
            cir.setReturnValue(true);
        }
    }
}
