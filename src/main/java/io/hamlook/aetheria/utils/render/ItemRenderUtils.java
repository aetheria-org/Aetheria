package io.hamlook.aetheria.utils.render;

import io.hamlook.aetheria.utils.KeybindHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;


public class ItemRenderUtils {

    public static void renderItemIcon(Minecraft mc, ItemStack stack, int x, int y, int size) {
        if (stack == null) return;

        boolean depthWasOn = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        String label;
        try {
            label = io.hamlook.aetheria.utils.ColorUtils.stripColor(stack.getDisplayName()).trim();
        } catch (Exception e) {
            label = String.valueOf(stack.getItem());
        }

        beginGuiItemRender();
        try {
            GlStateManager.enableDepth();
            mc.getRenderItem().renderItemIntoGUI(stack, x, y);
        } catch (Exception e) {
            // A broken item model must not leak the pushed attrib/matrix
            // levels: exhausted GL stacks corrupt everything rendered after
            // us (Intel drivers report the exhaustion as GL_OUT_OF_MEMORY).
            io.hamlook.aetheria.utils.debug.GLDebugProbe.warnThrottled(
                    "itemicon." + label, 5_000L,
                    "[ItemRenderUtils] icon render failed for " + label + ": " + e);
        } finally {
            endGuiItemRender();
        }

        // Restore the depth state this helper used to leave disabled globally,
        // which broke depth-dependent rendering drawn afterwards (e.g. the
        // inventory player preview).
        if (depthWasOn) {
            GlStateManager.enableDepth();
        } else {
            GlStateManager.disableDepth();
        }
    }

    public static void renderItemIcon(Minecraft mc, ItemStack stack, int x, int y) {
        renderItemIcon(mc, stack, x, y, 16);
    }

    /**
     * Standard state for GUI item rendering: unlit-safe white color, standard
     * alpha blend function. Callers that draw text/rects right after an item
     * render must return to this state - vanilla item rendering leaves blend
     * disabled and a multiplicative glint function (enchanted stacks), which
     * darkens everything drawn after it.
     */
    private static void beginGuiItemRender() {
        GlStateManager.color(1f, 1f, 1f, 1f);
        RenderHelper.enableGUIStandardItemLighting();
    }

    /**
     * Counterpart to {@link #beginGuiItemRender()}: restores lighting off and
     * the standard alpha blend function. Must run even when the item render
     * throws, or every later draw inherits the polluted state.
     */
    private static void endGuiItemRender() {
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.tryBlendFuncSeparate(
                GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GlStateManager.enableTexture2D();
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.enableBlend();
    }

    public static void drawItemStack(ItemStack stack, int x, int y) {
        if (stack == null) return;

        Minecraft mc = Minecraft.getMinecraft();
        RenderItem ri = mc.getRenderItem();
        FontRenderer fr = mc.fontRendererObj;

        beginGuiItemRender();
        try {
            ri.zLevel = -145;
            ri.renderItemAndEffectIntoGUI(stack, x, y);
            ri.renderItemOverlayIntoGUI(fr, stack, x, y, null);
            ri.zLevel = 0;
        } finally {
            endGuiItemRender();
        }
    }

    public static void drawItemStackOverlay(ItemStack stack, int x, int y) {
        if (stack == null) return;
        GlStateManager.pushMatrix();
        GlStateManager.translate(0, 0, 100);
        GlStateManager.enableDepth();
        drawItemStack(stack, x, y);
        GlStateManager.disableDepth();
        GlStateManager.popMatrix();
    }

    public static void renderHeldCursorItem() {
        ItemStack held = Minecraft.getMinecraft().thePlayer.inventory.getItemStack();
        if (held == null) return;

        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        int[] cursor = KeybindHelper.getMouseCoords(sr);
        int cursorX = cursor[0], cursorY = cursor[1];

        beginGuiItemRender();
        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(0f, 0f, 300f);
            RenderItem ri = Minecraft.getMinecraft().getRenderItem();
            ri.renderItemAndEffectIntoGUI(held, cursorX - 8, cursorY - 8);
            ri.renderItemOverlayIntoGUI(
                    Minecraft.getMinecraft().fontRendererObj, held, cursorX - 8, cursorY - 8, null);
        } catch (Exception e) {
            io.hamlook.aetheria.utils.debug.GLDebugProbe.warnThrottled(
                    "itemrender.cursor", 5_000L,
                    "[ItemRenderUtils] cursor item render failed: " + e);
        } finally {
            GlStateManager.popMatrix();
            endGuiItemRender();
        }
    }

    public static void renderItemWithEffects(Minecraft mc, ItemStack stack, int x, int y) {
        beginGuiItemRender();
        try {
            GL11.glEnable(GL12.GL_RESCALE_NORMAL);
            mc.getRenderItem().renderItemAndEffectIntoGUI(stack, x, y);
        } finally {
            endGuiItemRender();
        }
    }
}
