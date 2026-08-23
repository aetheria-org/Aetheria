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


public class ItemRenderUtils {

    public static void renderItemIcon(Minecraft mc, ItemStack stack, int x, int y, int size) {
        if (stack == null) return;

        boolean depthWasOn = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);

        GlStateManager.pushAttrib();
        GlStateManager.enableDepth();
        GlStateManager.color(1f, 1f, 1f, 1f);
        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);
        GlStateManager.scale(size / 16f, size / 16f, 1f);
        mc.getRenderItem().renderItemIntoGUI(stack, 0, 0);
        GlStateManager.popMatrix();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.popAttrib();

        // Restore the depth state this helper used to leave disabled globally,
        // which broke depth-dependent rendering drawn afterwards (e.g. the
        // inventory player preview).
        if (depthWasOn) {
            GlStateManager.enableDepth();
        } else {
            GlStateManager.disableDepth();
        }
        GlStateManager.disableLighting();
        // Enchanted items run the glint pass, which leaves a multiplicative
        // blend function set; restore standard alpha blending or everything
        // drawn after us renders dark.
        GlStateManager.tryBlendFuncSeparate(
                GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GlStateManager.enableTexture2D();
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.enableBlend();
    }

    public static void renderItemIcon(Minecraft mc, ItemStack stack, int x, int y) {
        renderItemIcon(mc, stack, x, y, 16);
    }

    public static void drawItemStack(ItemStack stack, int x, int y) {
        if (stack == null) return;

        Minecraft mc = Minecraft.getMinecraft();
        RenderItem ri = mc.getRenderItem();
        FontRenderer fr = mc.fontRendererObj;

        RenderHelper.enableGUIStandardItemLighting();
        ri.zLevel = -145;
        ri.renderItemAndEffectIntoGUI(stack, x, y);
        ri.renderItemOverlayIntoGUI(fr, stack, x, y, null);
        ri.zLevel = 0;
        RenderHelper.disableStandardItemLighting();
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

        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.pushMatrix();
        GlStateManager.translate(0f, 0f, 300f);
        RenderItem ri = Minecraft.getMinecraft().getRenderItem();
        ri.renderItemAndEffectIntoGUI(held, cursorX - 8, cursorY - 8);
        ri.renderItemOverlayIntoGUI(
                Minecraft.getMinecraft().fontRendererObj, held, cursorX - 8, cursorY - 8, null);
        GlStateManager.disableLighting();
        GlStateManager.popMatrix();
    }

    public static void renderItemWithEffects(Minecraft mc, ItemStack stack, int x, int y) {
        if (stack == null) return;

        RenderHelper.enableGUIStandardItemLighting();
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        mc.getRenderItem().renderItemAndEffectIntoGUI(stack, x, y);
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableLighting();
    }
}