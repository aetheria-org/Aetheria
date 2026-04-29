package com.jef.justenoughfakepixel.features.misc

import com.jef.justenoughfakepixel.core.JefConfig
import com.jef.justenoughfakepixel.core.config.gui.GuiTextures
import com.jef.justenoughfakepixel.events.ItemTossEvent
import com.jef.justenoughfakepixel.events.RenderItemOverlayEvent
import com.jef.justenoughfakepixel.events.SlotClickEvent
import com.jef.justenoughfakepixel.init.RegisterEvents
import com.jef.justenoughfakepixel.utils.item.ItemUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.inventory.ContainerChest
import net.minecraft.item.ItemStack
import net.minecraft.util.ChatComponentText
import net.minecraft.util.EnumChatFormatting
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import org.lwjgl.input.Keyboard


@RegisterEvents
class ProtectItemFeature {

    companion object {
        private val mc = Minecraft.getMinecraft()
        private const val STAR_SIZE = 16
        private const val CLICK_OUTSIDE_WINDOW = -999
        private const val DROP_KEY_CLICK_TYPE = 4
        private const val SWAP_OFFHAND_CLICK_TYPE = 5

        @JvmStatic
        fun toggleProtection(stack: ItemStack) {
            val uuid = ItemUtils.getItemUuid(stack)
            if (uuid == null) {
                notifyRaw("${EnumChatFormatting.RED}This item has no SkyBlock UUID and cannot be protected.")
                return
            }
            if (ProtectedItemStorage.contains(uuid)) {
                ProtectedItemStorage.remove(uuid)
                notifyRaw("${EnumChatFormatting.YELLOW}${stack.displayName} ${EnumChatFormatting.GRAY}is no longer protected.")
            } else {
                ProtectedItemStorage.add(uuid)
                notifyRaw("${EnumChatFormatting.GREEN}${stack.displayName} ${EnumChatFormatting.GRAY}is now protected!")
            }
        }

        private fun notifyRaw(message: String) {
            if (JefConfig.feature?.misc?.protectItem?.showChatNotifications != true) return
            mc.thePlayer?.addChatMessage(ChatComponentText("${EnumChatFormatting.RED}[JEF] §r$message"))
        }
    }

    private var keyWasDown = false

    @SubscribeEvent
    fun onClientTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.END) return
        if (mc.thePlayer == null || mc.currentScreen != null) return

        val key = JefConfig.feature?.misc?.protectItem?.protectionKey ?: return
        if (key == Keyboard.KEY_NONE) return

        val keyDown = Keyboard.isKeyDown(key)
        if (keyDown && !keyWasDown) {
            val held = mc.thePlayer.heldItem
            if (held == null) {
                notifyRaw("${EnumChatFormatting.RED}You are not holding an item!")
            } else {
                toggleProtection(held)
            }
        }
        keyWasDown = keyDown
    }


    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onItemDrop(event: ItemTossEvent) {
        val stack = event.item ?: return
        if (!isProtected(stack)) return

        event.isCanceled = true
        notifyBlocked(stack.displayName, "dropped")
    }


    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onSlotClick(event: SlotClickEvent) {
        // Case 1: Click outside window (throws cursor item)
        if (isClickOutsideWindow(event)) {
            handleClickOutsideWindow(event)
            return
        }

        // Case 2: Drop key (Q) pressed on a slot
        if (isDropKeyPress(event)) {
            handleDropKeyPress(event)
            return
        }

        // Case 3: Moving items in dangerous GUIs
        if (isDangerousGuiInteraction(event)) {
            handleDangerousGuiInteraction(event)
        }
    }


    private fun isClickOutsideWindow(event: SlotClickEvent): Boolean {
        return event.slotId == CLICK_OUTSIDE_WINDOW && event.clickType != SWAP_OFFHAND_CLICK_TYPE
    }

    private fun handleClickOutsideWindow(event: SlotClickEvent) {
        val cursorItem = mc.thePlayer?.inventory?.itemStack ?: return
        if (!isProtected(cursorItem)) return

        event.isCanceled = true
        notifyBlocked(cursorItem.displayName, "thrown away")
    }


    private fun isDropKeyPress(event: SlotClickEvent): Boolean {
        return event.clickType == DROP_KEY_CLICK_TYPE && event.slotId != CLICK_OUTSIDE_WINDOW
    }

    private fun handleDropKeyPress(event: SlotClickEvent) {
        val slot = event.slot ?: return
        if (!slot.hasStack) return

        val stack = slot.stack ?: return
        if (!isProtected(stack)) return

        event.isCanceled = true
        notifyBlocked(stack.displayName, "dropped")
    }

    private fun isDangerousGuiInteraction(event: SlotClickEvent): Boolean {
        val gui = mc.currentScreen as? GuiChest ?: return false
        val container = gui.inventorySlots as? ContainerChest ?: return false

        return ProtectionChecks.shouldBlockMovement(gui, container)
    }

    private fun handleDangerousGuiInteraction(event: SlotClickEvent) {
        // Check if the clicked slot has a protected item
        val slot = event.slot
        if (slot != null && slot.hasStack) {
            val stack = slot.stack
            if (stack != null && isProtected(stack)) {
                event.isCanceled = true
                notifyBlocked(stack.displayName, "moved in this menu")
                return
            }
        }

        // Check if the cursor has a protected item
        val cursorItem = mc.thePlayer?.inventory?.itemStack
        if (cursorItem != null && isProtected(cursorItem)) {
            event.isCanceled = true
            notifyBlocked(cursorItem.displayName, "moved in this menu")
        }
    }


    @SubscribeEvent
    fun onItemOverlay(event: RenderItemOverlayEvent) {
        if (!shouldShowStar()) return

        val stack = event.stack ?: return
        if (!isProtected(stack)) return

        renderProtectionStar(event.x, event.y)
    }

    private fun shouldShowStar(): Boolean {
        return JefConfig.feature?.misc?.protectItem?.showProtectedStar == true
    }

    private fun renderProtectionStar(x: Int, y: Int) {
        val opacity = getStarOpacity()

        GlStateManager.pushMatrix()
        GlStateManager.enableBlend()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        GlStateManager.color(1f, 1f, 1f, opacity)
        GlStateManager.disableDepth()

        mc.textureManager.bindTexture(GuiTextures.PROTECT_ITEM_STAR)
        Gui.drawModalRectWithCustomSizedTexture(
            x, y, 0f, 0f, STAR_SIZE, STAR_SIZE, STAR_SIZE.toFloat(), STAR_SIZE.toFloat()
        )

        GlStateManager.enableDepth()
        GlStateManager.disableBlend()
        GlStateManager.color(1f, 1f, 1f, 1f)
        GlStateManager.popMatrix()
    }

    private fun getStarOpacity(): Float {
        val opacityPercent = JefConfig.feature?.misc?.protectItem?.starOpacity ?: 100
        return opacityPercent / 100f
    }


    private fun isProtected(stack: ItemStack?): Boolean {
        val uuid = ItemUtils.getItemUuid(stack) ?: return false
        return ProtectedItemStorage.contains(uuid)
    }

    private fun notifyBlocked(displayName: String, action: String) {
        notifyRaw("${EnumChatFormatting.YELLOW}$displayName ${EnumChatFormatting.RED}is protected and cannot be $action!")
    }
}