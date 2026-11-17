package io.hamlook.aetheria.utils.render;

import io.hamlook.aetheria.utils.KeybindHelper;
import io.hamlook.aetheria.utils.compat.GlStateManagerCompat;
import io.hamlook.aetheria.utils.debug.GLDebugProbe;
import io.hamlook.aetheria.utils.compat.DrawContextUtils;
import io.hamlook.aetheria.utils.compat.InventoryCompat;
import io.hamlook.aetheria.utils.compat.GuiScreenUtils;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.RenderHelperCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import io.hamlook.aetheria.utils.compat.TextCompat;


public class ItemRenderUtils {

    public static void renderItemIcon(Minecraft mc, ItemStack stack, int x, int y, int size) {
        if (stack == null) return;

        boolean depthWasOn = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        String label;
        try {
            label = io.hamlook.aetheria.utils.ColorUtils.stripColor(TextCompat.getUnformattedText(stack.getDisplayName())).trim();
        } catch (Exception e) {
            label = String.valueOf(stack.getItem());
        }

        beginGuiItemRender();
        try {
            GlStateManagerCompat.pushMatrix();
            GlStateManagerCompat.translate(x, y, 0);
            GlStateManagerCompat.scale(size / 16f, size / 16f, 1f);
            GlStateManagerCompat.enableDepth();
            DrawContextUtils.drawItem(stack, 0, 0);
        } catch (Exception e) {
            // A broken item model must not leak the pushed attrib/matrix.
            GLDebugProbe.warnThrottled(
                    "itemicon." + label, 5_000L,
                    "[ItemRenderUtils] icon render failed for " + label + ": " + e);
        } finally {
            GlStateManagerCompat.popMatrix();
            endGuiItemRender();
        }

        // Restore the depth state this helper used to leave disabled globally,
        // which broke depth-dependent rendering drawn afterwards (e.g. the
        // inventory player preview).
        if (depthWasOn) {
            GlStateManagerCompat.enableDepth();
        } else {
            GlStateManagerCompat.disableDepth();
        }
    }

    public static void renderItemIcon(Minecraft mc, ItemStack stack, int x, int y) {
        renderItemIcon(mc, stack, x, y, 16);
    }

    private static void beginGuiItemRender() {
        GlStateManagerCompat.color(1f, 1f, 1f, 1f);
        //#if MC < 1.21
        RenderHelperCompat.enableGUIStandardItemLighting();
        //#endif
    }

    private static void endGuiItemRender() {
        //#if MC < 1.21
        RenderHelperCompat.disableStandardItemLighting();
        GlStateManagerCompat.disableLighting();
        GlStateManagerCompat.disableRescaleNormal();
        GlStateManagerCompat.tryBlendFuncSeparate(
                GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GlStateManagerCompat.enableTexture2D();
        //#endif
        GlStateManagerCompat.color(1f, 1f, 1f, 1f);
        GlStateManagerCompat.enableBlend();
    }

    public static void drawItemStack(ItemStack stack, int x, int y) {
        if (stack == null) return;

        FontRenderer fr = MinecraftCompat.getFontRenderer();

        beginGuiItemRender();
        try {
            DrawContextUtils.drawItem(stack, x, y);
            DrawContextUtils.drawItemInSlot(fr, stack, x, y);
        } catch (Exception e) {
            GLDebugProbe.warnThrottled(
                    "itemstack." + stack.getItem(), 5_000L,
                    "[ItemRenderUtils] item stack render failed: " + e);
        } finally {
            endGuiItemRender();
        }
    }

    public static void drawItemStackOverlay(ItemStack stack, int x, int y) {
        if (stack == null) return;
        GlStateManagerCompat.pushMatrix();
        GlStateManagerCompat.translate(0, 0, 100);
        GlStateManagerCompat.enableDepth();
        drawItemStack(stack, x, y);
        GlStateManagerCompat.disableDepth();
        GlStateManagerCompat.popMatrix();
    }

    public static void renderHeldCursorItem() {
        ItemStack held = InventoryCompat.getCursorStack(MinecraftCompat.getLocalPlayer());
        if (held == null) return;

        int[] cursor = KeybindHelper.getMouseCoords(GuiScreenUtils.getScaledWindowWidth(), GuiScreenUtils.getScaledWindowHeight());
        int cursorX = cursor[0], cursorY = cursor[1];

        beginGuiItemRender();
        GlStateManagerCompat.pushMatrix();
        try {
            GlStateManagerCompat.translate(0f, 0f, 300f);
            DrawContextUtils.drawItem(held, cursorX - 8, cursorY - 8);
            DrawContextUtils.drawItemInSlot(
                    MinecraftCompat.getFontRenderer(), held, cursorX - 8, cursorY - 8);
        } catch (Exception e) {
            GLDebugProbe.warnThrottled(
                    "itemrender.cursor", 5_000L,
                    "[ItemRenderUtils] cursor item render failed: " + e);
        } finally {
            GlStateManagerCompat.popMatrix();
            endGuiItemRender();
        }
    }

    public static void renderItemWithEffects(Minecraft mc, ItemStack stack, int x, int y) {
        //#if MC < 1.21
        beginGuiItemRender();
        try {
            GL11.glEnable(GL12.GL_RESCALE_NORMAL);
            DrawContextUtils.drawItem(stack, x, y);
        } finally {
            endGuiItemRender();
        }
        //#else
        //$$ GlStateManagerCompat.color(1f, 1f, 1f, 1f);
        //$$ try {
        //$$     DrawContextUtils.drawItem(stack, x, y);
        //$$ } finally {
        //$$     GlStateManagerCompat.color(1f, 1f, 1f, 1f);
        //$$ }
        //#endif
    }

}
