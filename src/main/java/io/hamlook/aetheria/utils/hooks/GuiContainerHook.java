package io.hamlook.aetheria.utils.hooks;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.Config;
import io.hamlook.aetheria.events.GuiContainerRenderBeforeTooltipEvent;
import io.hamlook.aetheria.events.SlotClickEvent;
import io.hamlook.aetheria.features.misc.protect.ProtectItemFeature;
import io.hamlook.aetheria.features.qol.BetterContainers;
import io.hamlook.aetheria.utils.ColorUtils;
import io.hamlook.aetheria.utils.ContainerUtils;
import io.hamlook.aetheria.utils.compat.ClipboardCompat;
import io.hamlook.aetheria.utils.compat.DrawContext;
import io.hamlook.aetheria.utils.compat.NbtCompat;
import io.hamlook.aetheria.utils.modcompat.NefSlotClickCompat;
import io.hamlook.aetheria.utils.compat.TextCompat;
import io.hamlook.aetheria.utils.item.NBTFormatter;
import io.hamlook.aetheria.utils.compat.GuiScreenUtils;
import io.hamlook.aetheria.utils.render.HighlightUtils;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.ContainerChest;
//#if MC < 1.21
import net.minecraft.client.gui.GuiButton;
//#else
//$$ import net.minecraft.client.gui.screen.ingame.HandledScreen;
//$$ import net.minecraft.client.gui.widget.ButtonWidget;
//$$ import net.minecraft.client.gui.widget.ClickableWidget;
//$$ import net.minecraft.text.Text;
//#endif
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

    //#if MC < 1.21
    public static void afterDrawForeground(DrawContext context, GuiContainer gui, int mouseX, int mouseY) {
        new GuiContainerRenderBeforeTooltipEvent(context, gui, mouseX, mouseY).post();
    }
    //#else
    //$$ public static void afterDrawForeground(DrawContext context, HandledScreen<?> gui, int mouseX, int mouseY) {
    //$$     new GuiContainerRenderBeforeTooltipEvent(context, gui, mouseX, mouseY).post();
    //$$ }
    //#endif

    //#if MC < 1.21
    public static void onGuiClosed(GuiContainer gui) {
        if (ContainerUtils.isChestOpen(gui)) {
            BetterContainers.getInstance().reset();
        }
    }
    //#else
    //$$ public static void onGuiClosed(HandledScreen<?> gui) {
    //$$     if (ContainerUtils.isChestOpen(gui)) {
    //$$         BetterContainers.getInstance().reset();
    //$$     }
    //$$ }
    //#endif

    public static boolean cancelBlankPaneRender(Slot slot) {
        ItemStack stack = slot.getStack();
        return BetterContainers.isEnabled() && BetterContainers.getInstance().isLoaded() && !BetterContainers.shouldRenderStack(slot.slotNumber, stack);
    }

    public static boolean suppressBlankPaneHover(Slot slot) {
        if (slot == null) return false;
        if (ATHRConfig.feature == null) return false;
        if (!ATHRConfig.feature.qol.betterContainers.hideEmptyPaneHover) return false;
        return cancelBlankPaneRender(slot);
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

    //#if MC < 1.21
    public static boolean protectItemClick(GuiContainer gui, Slot slot, int slotId, int clickedButton, int clickType) {
        SlotClickEvent event = new SlotClickEvent(gui, slot, slotId, clickedButton, SlotClickEvent.ClickType.fromId(clickType));
        event.post();
        if (event.isCancelled()) return true;
        NefSlotClickCompat.recordGuiPosted(slotId, clickedButton, clickType);
        return false;
    }
    //#else
    //$$ public static boolean protectItemClick(HandledScreen<?> gui, Slot slot, int slotId, int clickedButton, int clickType) {
    //$$     SlotClickEvent event = new SlotClickEvent(gui, slot, slotId, clickedButton, SlotClickEvent.ClickType.fromId(clickType));
    //$$     event.post();
    //$$     if (event.isCancelled()) return true;
    //$$     NefSlotClickCompat.recordGuiPosted(slotId, clickedButton, clickType);
    //$$     return false;
    //$$ }
    //#endif

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

    //#if MC < 1.21
    public static void searchHighlight(GuiContainer gui, Slot slot) {
        if (slot != null) {
            HighlightUtils.renderAllHighlights(gui, slot);
        }
    }
    //#else
    //$$ public static void searchHighlight(HandledScreen<?> gui, Slot slot) {
    //$$     if (slot != null) {
    //$$         HighlightUtils.renderAllHandledHighlights(gui, slot);
    //$$     }
    //$$ }
    //#endif
}
