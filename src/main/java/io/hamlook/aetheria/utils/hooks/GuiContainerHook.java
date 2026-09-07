package io.hamlook.aetheria.utils.hooks;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.Config;
import io.hamlook.aetheria.events.GuiContainerRenderBeforeTooltipEvent;
import io.hamlook.aetheria.events.SlotClickEvent;
import io.hamlook.aetheria.features.misc.protect.ProtectItemFeature;
import io.hamlook.aetheria.features.profile.ProfileParser;
import io.hamlook.aetheria.features.qol.BetterContainers;
import io.hamlook.aetheria.utils.ColorUtils;
import io.hamlook.aetheria.utils.ContainerUtils;
import io.hamlook.aetheria.utils.compat.ClipboardCompat;
import io.hamlook.aetheria.utils.compat.NbtCompat;
import io.hamlook.aetheria.utils.compat.NefSlotClickCompat;
import io.hamlook.aetheria.utils.compat.TextCompat;
import io.hamlook.aetheria.utils.data.SkyblockData;
import io.hamlook.aetheria.utils.item.NBTFormatter;
import io.hamlook.aetheria.utils.render.HighlightUtils;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.input.Keyboard;

import java.util.List;

public final class GuiContainerHook {

    private static final String GUI_TITLE = "Select Profile";
    private static final String ITEM_TITLE = "View player profile";

    private GuiContainerHook() {
    }

    public static void afterDrawForeground(GuiContainer gui, int mouseX, int mouseY) {
        new GuiContainerRenderBeforeTooltipEvent(gui, mouseX, mouseY).post();
    }

    public static void onGuiClosed(GuiContainer gui) {
        if (ContainerUtils.isChestOpen(gui)) {
            BetterContainers.getInstance().reset();
        }
    }

    public static boolean cancelBlankPaneRender(Slot slot) {
        ItemStack stack = slot.getStack();
        return BetterContainers.isEnabled() && BetterContainers.getInstance().isLoaded() && !BetterContainers.shouldRenderStack(slot.slotNumber, stack);
    }

    public static boolean nbtCopy(Slot theSlot, int keyCode) {
        Config config = ATHRConfig.feature;
        if (config == null) return false;
        if (keyCode != config.debug.copyNBTKey || !config.debug.copyNBTData) return false;
        if (theSlot == null || !theSlot.getHasStack()) return false;

        net.minecraft.nbt.NBTTagCompound tag = NbtCompat.getTagCompound(theSlot.getStack());
        if (tag != null) {
            ClipboardCompat.setClipboard(NBTFormatter.format(tag));
            TextCompat.addChatMessage(TextCompat.createText(EnumChatFormatting.GREEN + "Copied NBT to clipboard!"));
        } else {
            TextCompat.addChatMessage(TextCompat.createText(EnumChatFormatting.RED + "This item has no NBT data."));
        }
        return true;
    }

    public static void profileInitGui(GuiContainer gui, List<GuiButton> buttonList) {
        ContainerChest chest = ContainerUtils.getOpenChest(gui);
        if (chest != null && chest.getLowerChestInventory().getName().equals("View Profile") && !SkyblockData.getEnvironment().isTest()) {
            buttonList.add(new GuiButton(1000, gui.guiLeft - 200, gui.guiTop, 80, 20, "Parse Profile"));
        }
    }

    public static boolean profileMouseReleased(GuiContainer gui, int mouseX, int mouseY, GuiButton button) {
        if (button == null) return false;
        if (mouseX > button.xPosition && mouseX < button.xPosition + button.width && mouseY > button.yPosition && mouseY < button.yPosition + button.height) {
            ProfileParser.parse("Diyansh", gui.inventorySlots);
            return true;
        }
        return false;
    }

    public static boolean profileMouseClicked(GuiContainer gui, Slot theSlot, int mouseButton) {
        if (mouseButton != 0) return false;
        ContainerChest chest = ContainerUtils.getOpenChest(gui);
        if (chest == null) return false;
        String title = ContainerUtils.getTitle(chest);
        if (theSlot == null || !theSlot.getHasStack()) return false;
        if (!title.equals(GUI_TITLE)) return false;
        ItemStack stack = theSlot.getStack();
        String itemName = ColorUtils.stripColor(stack.getDisplayName()).trim();
        if (!itemName.equals(ITEM_TITLE)) return false;
        ProfileParser.parseName(stack);
        return true;
    }

    public static boolean protectItemClick(GuiContainer gui, Slot slot, int slotId, int clickedButton, int clickType) {
        SlotClickEvent event = new SlotClickEvent(gui, slot, slotId, clickedButton, SlotClickEvent.ClickType.fromId(clickType));
        event.post();
        if (event.isCancelled()) return true;
        NefSlotClickCompat.recordGuiPosted(slotId, clickedButton, clickType);
        return false;
    }

    public static boolean protectItemKey(Slot theSlot, int keyCode) {
        Config config = ATHRConfig.feature;
        if (config == null) return false;
        int protectionKey = config.misc.protectItem.protectionKey;
        if (protectionKey == Keyboard.KEY_NONE || keyCode != protectionKey) return false;
        if (theSlot != null && theSlot.getStack() != null) {
            ProtectItemFeature.toggleProtection(theSlot.getStack());
        }
        return true;
    }

    public static void searchHighlight(GuiContainer gui, Slot slot) {
        if (slot != null) {
            HighlightUtils.renderAllHighlights(gui, slot);
        }
    }
}
