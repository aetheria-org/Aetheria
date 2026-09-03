package io.hamlook.aetheria.features.qol

import io.hamlook.aetheria.api.event.HandleEvent
import io.hamlook.aetheria.core.ATHRConfig
import io.hamlook.aetheria.core.moulconfig.editors.ChromaColour
import io.hamlook.aetheria.events.ASMGuiDrawEvent
import io.hamlook.aetheria.events.ASMGuiInitPreEvent
import io.hamlook.aetheria.events.ASMKeyEvent
import io.hamlook.aetheria.events.ASMMouseEvent
import io.hamlook.aetheria.init.RegisterEvents
import io.hamlook.aetheria.utils.chat.ChatUtils
import io.hamlook.aetheria.utils.compat.*
import io.hamlook.aetheria.utils.data.SkyblockData
import io.hamlook.aetheria.utils.render.RenderUtils
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.inventory.Container
import org.lwjgl.input.Keyboard

@RegisterEvents
class SlotBinds {

    private var pendingSlot: Int? = null

    private fun cfg() = ATHRConfig.feature?.qol?.slotBinds

    private fun isEnabled(): Boolean {
        val c = cfg() ?: return false
        return c.enabled && (!c.skyblockOnly || SkyblockData.isOnSkyblock())
    }

    private fun binds() = cfg()?.binds

    private fun Int.isHotbar() = this in 36..44
    private fun Int.isArmor() = this in 5..8
    private fun Int.isValidSlot() = this in 5 until 45
    private fun isShiftDown() =
        KeyboardCompat.isKeyDown(Keyboard.KEY_LSHIFT) || KeyboardCompat.isKeyDown(Keyboard.KEY_RSHIFT)

    private fun canBind(a: Int, b: Int) = !(a.isArmor() && !b.isHotbar()) && !(b.isArmor() && !a.isHotbar())

    private fun slotCenter(container: Container, slotIndex: Int, gui: GuiInventory): Pair<Int, Int>? {
        val slot = container.getSlot(slotIndex) ?: return null
        return (slot.xDisplayPosition + gui.guiLeft + 8) to (slot.yDisplayPosition + gui.guiTop + 8)
    }

    private fun MutableMap<Int, Int>.removeBind(slot: Int) {
        val partner = remove(slot)
        if (partner != null) remove(partner)
    }

    private fun MutableMap<Int, Int>.addBind(a: Int, b: Int) {
        removeBind(a)
        removeBind(b)

        this[a] = b
        this[b] = a
    }

    @HandleEvent(priority = HandleEvent.HIGHEST)
    fun onMouseClick(event: ASMMouseEvent) {
        if (!isEnabled() || !isShiftDown()) return
        val gui = event.gui as? GuiInventory ?: return
        if (!MouseCompat.getEventButtonState() || MouseCompat.getEventButton() != 0) return
        val clicked = InventoryCompat.getSlotUnderMouse(gui)?.slotNumber?.takeIf { it.isValidSlot() } ?: return
        val bound = binds()?.get(clicked) ?: return
        if (!bound.isValidSlot() || bound == clicked) return

        val (from, to) = if (clicked.isHotbar()) bound to clicked else if (bound.isHotbar()) clicked to bound else return

        InventoryCompat.windowClick(
            InventoryCompat.getContainer(gui).windowId, from, to - 36, 2, MinecraftCompat.getLocalPlayer() ?: return
        )
        event.cancel()
    }

    @HandleEvent
    fun onKeyPress(event: ASMKeyEvent) {
        if (!isEnabled()) return
        val gui = event.gui as? GuiInventory ?: return
        val c = cfg() ?: return
        if (c.bindKey == Keyboard.KEY_NONE || KeyboardCompat.getEventKey() != c.bindKey || !KeyboardCompat.getEventKeyState()) return

        val clicked = InventoryCompat.getSlotUnderMouse(gui)?.slotNumber?.takeIf { it.isValidSlot() } ?: return
        event.cancel()

        val pending = pendingSlot
        if (pending != null) {
            pendingSlot = null
            when {
                pending == clicked -> ChatUtils.sendMessage("§cCan't bind a slot to itself.")
                !pending.isHotbar() && !clicked.isHotbar() -> ChatUtils.sendMessage("§cOne slot must be in the hotbar.")
                !canBind(pending, clicked) -> ChatUtils.sendMessage("§cArmor slots can only be bound to hotbar slots.")
                else -> {
                    c.binds.addBind(pending, clicked)
                    ATHRConfig.saveConfig()
                }
            }
        } else {
            if (c.binds.containsKey(clicked)) {
                c.binds.removeBind(clicked)
                ATHRConfig.saveConfig()
            } else {
                pendingSlot = clicked
            }
        }
    }

    @HandleEvent
    fun onDraw(event: ASMGuiDrawEvent) {
        if (!isEnabled()) return
        val gui = event.gui as? GuiInventory ?: return
        val c = cfg() ?: return
        val container = InventoryCompat.getContainer(gui) ?: return
        val color = ChromaColour.specialToChromaRGB(c.lineColor)

        GlStateManagerCompat.pushMatrix()
        GlStateManagerCompat.translate(0.0, 0.0, 999.0)

        try {
            val pending = pendingSlot
            if (pending != null) {
                val (sx, sy) = slotCenter(container, pending, gui) ?: return
                RenderUtils.drawLine(sx, sy, event.mouseX, event.mouseY, color, 2f)
                return
            }

            if (c.alwaysShowLines) {
                // Draw all binds, iterate unique pairs (avoid drawing Aâ†’B and Bâ†’A twice)
                val drawn = mutableSetOf<Int>()
                for ((from, to) in c.binds) {
                    if (drawn.contains(from)) continue
                    drawn.add(from)
                    drawn.add(to)
                    val (sx, sy) = slotCenter(container, from, gui) ?: continue
                    val (ex, ey) = slotCenter(container, to, gui) ?: continue
                    RenderUtils.drawLine(sx, sy, ex, ey, color, 2f)
                }
            } else {
                if (!isShiftDown()) return
                val hovered = InventoryCompat.getSlotUnderMouse(gui)?.slotNumber?.takeIf { it.isValidSlot() } ?: return
                val bound = c.binds[hovered]?.takeIf { it.isValidSlot() && it != hovered } ?: return
                val (sx, sy) = slotCenter(container, hovered, gui) ?: return
                val (ex, ey) = slotCenter(container, bound, gui) ?: return
                RenderUtils.drawLine(sx, sy, ex, ey, color, 2f)
            }
        } finally {
            GlStateManagerCompat.popMatrix()
        }
    }

    @HandleEvent
    fun onGuiClose(event: ASMGuiInitPreEvent) {
        if (event.gui == null) pendingSlot = null
    }
}
