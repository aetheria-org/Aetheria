package io.hamlook.aetheria.mixins;

import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.features.profile.ProfileParser;
import io.hamlook.aetheria.utils.ColorUtils;
import io.hamlook.aetheria.utils.ContainerUtils;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(GuiContainer.class)
public class MixinGuiContainer_ProfileViewer extends GuiScreen {

    @Unique
    private static final String GUI_TITLE  = "Select Profile";
    @Unique
    private static final String ITEM_TITLE = "View player profile";

    @Unique
    public GuiButton justEnoughfakepixel$button;
    @Shadow
    public int guiLeft;

    @Shadow
    public int guiTop;

    @Shadow
    public Container inventorySlots;

    @Shadow
    private Slot theSlot;

    @Inject(method = "initGui",at = @At("RETURN"))
    public void initGui(CallbackInfo ci) {
        ContainerChest chest = ContainerUtils.getOpenChest((GuiScreen)(Object) this);
        if (chest != null) {
            Aetheria.logger.info(chest.getLowerChestInventory().getName());
            if (chest.getLowerChestInventory().getName().equals("View Profile")) {
                this.justEnoughfakepixel$button = new GuiButton(1000,
                        this.guiLeft - 200,
                        this.guiTop,
                        80,20,
                        "Parse Profile");

                this.buttonList.add(justEnoughfakepixel$button);
            }
        }
    }


    @Inject(method = "mouseReleased",at = @At("HEAD"))
    public void mouseReleased(int mouseX, int mouseY, int state, CallbackInfo ci) {
        if(justEnoughfakepixel$button == null) return;
        if(mouseX > justEnoughfakepixel$button.xPosition && mouseX < justEnoughfakepixel$button.xPosition + justEnoughfakepixel$button.width
                && mouseY > justEnoughfakepixel$button.yPosition && mouseY < justEnoughfakepixel$button.yPosition + justEnoughfakepixel$button.height) {
            ProfileParser.parse("Diyansh",this.inventorySlots);
        }
    }
    @Inject(method = "mouseClicked", at = @At("HEAD"))
    public void mouseClicked(int mouseX, int mouseY, int mouseButton, CallbackInfo ci) {
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
}