package io.hamlook.aetheria.mixins.gui;

import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.events.GuiContainerRenderBeforeTooltipEvent;
import io.hamlook.aetheria.events.SlotClickEvent;
import io.hamlook.aetheria.features.misc.protect.ProtectItemFeature;
import io.hamlook.aetheria.features.profile.ProfileParser;
import io.hamlook.aetheria.features.qol.BetterContainers;
import io.hamlook.aetheria.features.storage.StorageManager;
import io.hamlook.aetheria.utils.ColorUtils;
import io.hamlook.aetheria.utils.ContainerUtils;
import io.hamlook.aetheria.utils.data.SkyblockData;
import io.hamlook.aetheria.utils.item.NBTFormatter;
import io.hamlook.aetheria.utils.render.HighlightUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GuiContainer.class)
public abstract class MixinGuiContainer extends GuiScreen {

    @Shadow private Slot theSlot;
    @Shadow public int guiLeft;
    @Shadow public int guiTop;
    @Shadow public Container inventorySlots;

    @Unique private static final String GUI_TITLE = "Select Profile";
    @Unique private static final String ITEM_TITLE = "View player profile";
    @Unique public GuiButton aetheria$button;

    @Inject(
            method = "drawScreen",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/inventory/GuiContainer;drawGuiContainerForegroundLayer(II)V",
                    shift = At.Shift.AFTER
            )
    )
    public void ATHR$afterDrawForeground(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        new GuiContainerRenderBeforeTooltipEvent((GuiContainer)(Object)this, mouseX, mouseY).post();
    }

    @Inject(method = "onGuiClosed", at = @At("HEAD"))
    private void ATHR$onGuiClosed(CallbackInfo ci) {
        if (ContainerUtils.isChestOpen((GuiContainer) (Object) this)) {
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

    @Inject(method = "keyTyped", at = @At("HEAD"))
    private void ATHR$nbtCopy(char typedChar, int keyCode, CallbackInfo ci) {
        if (keyCode == ATHRConfig.feature.debug.copyNBTKey && ATHRConfig.feature.debug.copyNBTData) {
            if (this.theSlot != null && this.theSlot.getHasStack()) {
                ItemStack stack = this.theSlot.getStack();
                if (stack.hasTagCompound()) {
                    String prettyNbt = NBTFormatter.format(stack.getTagCompound());
                    GuiScreen.setClipboardString(prettyNbt);
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

    @Inject(method = "initGui", at = @At("RETURN"))
    public void ATHR$profileInitGui(CallbackInfo ci) {
        ContainerChest chest = ContainerUtils.getOpenChest((GuiScreen)(Object) this);
        if (chest != null) {
            Aetheria.logger.info(chest.getLowerChestInventory().getName());
            if (chest.getLowerChestInventory().getName().equals("View Profile")
                    && !SkyblockData.getEnvironment().isTest()) {
                this.aetheria$button = new GuiButton(1000,
                        this.guiLeft - 200,
                        this.guiTop,
                        80, 20,
                        "Parse Profile");
                this.buttonList.add(aetheria$button);
            }
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"))
    public void ATHR$profileMouseReleased(int mouseX, int mouseY, int state, CallbackInfo ci) {
        if (aetheria$button == null) return;
        if (mouseX > aetheria$button.xPosition && mouseX < aetheria$button.xPosition + aetheria$button.width
                && mouseY > aetheria$button.yPosition && mouseY < aetheria$button.yPosition + aetheria$button.height) {
            ProfileParser.parse("Diyansh", this.inventorySlots);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"))
    public void ATHR$profileMouseClicked(int mouseX, int mouseY, int mouseButton, CallbackInfo ci) {
        if (mouseButton != 0) return;
        ContainerChest chest = ContainerUtils.getOpenChest((GuiScreen)(Object) this);
        if (chest != null) {
            String title = ContainerUtils.getTitle(chest);
            if (theSlot == null || !theSlot.getHasStack()) return;
            Aetheria.logger.info("Slot: " + theSlot.slotNumber + " | Window: " + chest.windowId);
            if (!title.equals(GUI_TITLE)) return;
            ItemStack stack = theSlot.getStack();
            String itemName = ColorUtils.stripColor(stack.getDisplayName()).trim();
            if (!itemName.equals(ITEM_TITLE)) return;
            ProfileParser.parseName(stack);
        }
    }

    @Inject(
            method = "handleMouseClick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/PlayerControllerMP;windowClick(IIIILnet/minecraft/entity/player/EntityPlayer;)Lnet/minecraft/item/ItemStack;"
            ),
            cancellable = true
    )
    private void ATHR$protectItemClick(Slot slot, int slotId, int clickedButton, int clickType, CallbackInfo ci) {
        GuiContainer gui = (GuiContainer) (Object) this;
        SlotClickEvent event = new SlotClickEvent(gui, slot, slotId, clickedButton, clickType);
        event.post();
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "keyTyped", at = @At("HEAD"), cancellable = true)
    private void ATHR$protectItemKey(char typedChar, int keyCode, CallbackInfo ci) {
        if (ATHRConfig.feature == null) return;
        int protectionKey = ATHRConfig.feature.misc.protectItem.protectionKey;
        if (protectionKey == Keyboard.KEY_NONE || keyCode != protectionKey) return;
        Slot hovered = this.theSlot;
        if (hovered != null && hovered.getStack() != null) {
            ProtectItemFeature.toggleProtection(hovered.getStack());
        }
        ci.cancel();
    }

    @Inject(method = "drawSlot", at = @At("RETURN"))
    private void ATHR$searchHighlight(Slot slot, CallbackInfo ci) {
        if (slot != null) {
            HighlightUtils.renderAllHighlights((GuiContainer) (Object) this, slot);
        }
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
