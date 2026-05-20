package com.jef.justenoughfakepixel.features.misc.slotbinds;

import com.jef.justenoughfakepixel.core.JefConfig;
import com.jef.justenoughfakepixel.core.config.editors.ChromaColour;
import com.jef.justenoughfakepixel.events.SlotClickEvent;
import com.jef.justenoughfakepixel.init.RegisterEvents;
import com.jef.justenoughfakepixel.utils.chat.ChatUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.inventory.Slot;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.util.Iterator;
import java.util.Map;

/**
 * Slot Binds — ported from Odin's SlotBinds.kt into JEF's 1.8.9 Forge environment.
 *
 * <p>Usage:
 * <ol>
 *   <li>Open your player inventory.</li>
 *   <li>Hover a slot and press the configured <em>Bind Set Key</em> to begin a bind.</li>
 *   <li>Hover the second slot (one must be hotbar 36–44) and press the key again to save the bind.</li>
 *   <li>Hovering a bound slot and pressing the key with no pending slot removes the bind.</li>
 *   <li>Shift-click a bound slot to instantly swap it with its paired hotbar slot.</li>
 *   <li>Hold Shift while hovering a bound slot to preview the line to its pair.</li>
 * </ol>
 */
@RegisterEvents
public class SlotBinds {

    private static final Minecraft MC = Minecraft.getMinecraft();

    /** The slot index first chosen when setting a new bind; null when idle. */
    private Integer previousSlot = null;

    // ── Shift-click swap ─────────────────────────────────────────────────────

    /**
     * When the player shift-clicks a slot in their inventory that has a bind,
     * intercept and perform the hotbar swap instead of the normal shift-click.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onSlotClick(SlotClickEvent event) {
        if (!isEnabled()) return;
        if (!(event.getGui() instanceof GuiInventory)) return;
        // clickType 1 == shift-click
        if (event.getClickType() != 1) return;

        Slot slot = event.getSlot();
        if (slot == null) return;

        int slotIdx = slot.slotNumber;
        // Only intercept slots 5–44 (player inv + hotbar, excluding armour 0–4)
        if (slotIdx < 5 || slotIdx > 44) return;

        Map<Integer, Integer> binds = SlotBindStorage.getInstance().getBinds();
        Integer boundSlot = binds.get(slotIdx);
        if (boundSlot == null) return;

        // Determine which is the hotbar slot
        int from, to;
        if (slotIdx >= 36) {
            from = boundSlot;
            to = slotIdx;
        } else if (boundSlot >= 36) {
            from = slotIdx;
            to = boundSlot;
        } else {
            return; // neither is hotbar — shouldn't happen, but guard anyway
        }

        // Perform the swap via hotbar-swap click type (2) using the hotbar key number
        int hotbarKey = to - 36; // 0-8
        MC.playerController.windowClick(
                event.getGui().inventorySlots.windowId,
                from,
                hotbarKey,
                2, // SWAP
                MC.thePlayer
        );
        event.setCanceled(true);
    }

    // ── Bind set / remove ────────────────────────────────────────────────────

    @SubscribeEvent
    public void onKeyTyped(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        if (!isEnabled()) return;
        if (!(event.gui instanceof GuiInventory)) return;
        if (!Keyboard.getEventKeyState()) return; // key-up, ignore

        int bindKey = JefConfig.feature.misc.slotBinds.bindKey;
        if (bindKey == Keyboard.KEY_NONE || Keyboard.getEventKey() != bindKey) return;

        Slot hovered = getHoveredSlot((net.minecraft.client.gui.inventory.GuiContainer) event.gui);
        if (hovered == null) return;

        int hoveredIdx = hovered.slotNumber;
        if (hoveredIdx < 5 || hoveredIdx > 44) return;

        event.setCanceled(true);

        Map<Integer, Integer> binds = SlotBindStorage.getInstance().getBinds();

        if (previousSlot != null) {
            int first = previousSlot;
            previousSlot = null;

            if (first == hoveredIdx) {
                ChatUtils.sendMessage("§c[SlotBinds] You can't bind a slot to itself.");
                return;
            }
            if (first < 36 && hoveredIdx < 36) {
                ChatUtils.sendMessage("§c[SlotBinds] One of the slots must be a hotbar slot (36–44).");
                return;
            }

            binds.put(first, hoveredIdx);
            binds.put(hoveredIdx, first); // bidirectional for lookup in both directions
            SlotBindStorage.getInstance().save();
            ChatUtils.sendMessage("§a[SlotBinds] Bound slot §b" + first + " §ato §d" + hoveredIdx + "§a.");
        } else {
            // If hovering a bound slot, remove it; otherwise start a new bind
            if (binds.containsKey(hoveredIdx)) {
                int partner = binds.get(hoveredIdx);
                binds.remove(hoveredIdx);
                binds.remove(partner);
                SlotBindStorage.getInstance().save();
                ChatUtils.sendMessage("§c[SlotBinds] Removed bind between slot §b" + hoveredIdx + " §cand §d" + partner + "§c.");
            } else {
                previousSlot = hoveredIdx;
                ChatUtils.sendMessage("§e[SlotBinds] Selected slot §b" + hoveredIdx + "§e. Now hover the second slot and press the key again.");
            }
        }
    }

    // ── Line preview ─────────────────────────────────────────────────────────

    /**
     * Draw a line from the hovered bound slot to its partner when the player
     * is holding Shift in the player inventory.
     * Also draws a line from {@code previousSlot} to the cursor while setting a bind.
     */
    @SubscribeEvent
    public void onDrawScreen(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!isEnabled()) return;
        if (!(event.gui instanceof GuiInventory)) return;

        GuiInventory gui = (GuiInventory) event.gui;
        Slot hovered = getHoveredSlot(gui);

        int startSlotIdx = -1;
        int endSlotIdx = -1;
        boolean useMouse = false;

        Map<Integer, Integer> binds = SlotBindStorage.getInstance().getBinds();

        if (previousSlot != null) {
            // Mid-bind: draw from first selected slot to the cursor
            startSlotIdx = previousSlot;
            useMouse = true;
        } else if (hovered != null && (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT))) {
            // Preview: draw from hovered bound slot to its partner
            int hovIdx = hovered.slotNumber;
            Integer partner = binds.get(hovIdx);
            if (partner == null) return;
            startSlotIdx = hovIdx;
            endSlotIdx = partner;
        } else {
            return;
        }

        // Resolve start coordinates (centre of slot)
        Slot startSlot = getSlotByIndex(gui, startSlotIdx);
        if (startSlot == null) return;

        int gx = gui.guiLeft;
        int gy = gui.guiTop;

        float x1 = gx + startSlot.xDisplayPosition + 8;
        float y1 = gy + startSlot.yDisplayPosition + 8;

        float x2, y2;
        if (useMouse) {
            x2 = event.mouseX;
            y2 = event.mouseY;
        } else {
            Slot endSlot = getSlotByIndex(gui, endSlotIdx);
            if (endSlot == null) return;
            x2 = gx + endSlot.xDisplayPosition + 8;
            y2 = gy + endSlot.yDisplayPosition + 8;
        }

        // Parse the configured colour
        String colorStr = JefConfig.feature.misc.slotBinds.lineColor;
        int argb = ChromaColour.specialToChromaRGB(colorStr);
        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;

        drawLine(x1, y1, x2, y2, r, g, b, a);
    }

    // ── Cleanup ───────────────────────────────────────────────────────────────

    @SubscribeEvent
    public void onGuiClose(GuiScreenEvent.InitGuiEvent event) {
        // Reset pending bind whenever a GUI is (re-)opened or closed
        previousSlot = null;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static boolean isEnabled() {
        return JefConfig.feature != null && JefConfig.feature.misc.slotBinds.enabled;
    }

    /**
     * Retrieves the slot currently under the mouse cursor using the public
     * {@code GuiContainer#getSlotUnderMouse()} method — works in both dev and
     * production (no obfuscation issues).
     */
    private static Slot getHoveredSlot(net.minecraft.client.gui.inventory.GuiContainer gui) {
        return gui.getSlotUnderMouse();
    }

    /**
     * Finds a {@link Slot} by its {@code slotNumber} inside the GUI's container.
     */
    private static Slot getSlotByIndex(net.minecraft.client.gui.inventory.GuiContainer gui, int slotNumber) {
        for (Slot s : gui.inventorySlots.inventorySlots) {
            if (s.slotNumber == slotNumber) return s;
        }
        return null;
    }

    /**
     * Draws a 2-pixel-wide 2-D line between two screen points using the Tessellator.
     */
    private static void drawLine(float x1, float y1, float x2, float y2, int r, int g, int b, int a) {
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableLighting();
        GlStateManager.disableAlpha();
        GL11.glLineWidth(2f);

        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(x1, y1, 0).color(r, g, b, a).endVertex();
        wr.pos(x2, y2, 0).color(r, g, b, a).endVertex();
        tess.draw();

        GL11.glLineWidth(1f);
        GlStateManager.enableAlpha();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }
}
