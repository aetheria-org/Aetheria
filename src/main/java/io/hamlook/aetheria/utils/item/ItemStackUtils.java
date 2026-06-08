// Credit: Skytils (https://github.com/Skytils/SkytilsMod) (AGPLv3)

package io.hamlook.aetheria.utils.item;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;

public class ItemStackUtils {

    private static final Minecraft mc = Minecraft.getMinecraft();

    private ItemStackUtils() {
    }

    public static void drawTip(String tip, int x, int y) {
        FontRenderer fr = mc.fontRendererObj;
        GlStateManager.disableDepth();
        GlStateManager.disableBlend();
        fr.drawStringWithShadow(tip, x + 17 - fr.getStringWidth(tip), y + 9, 0xFFFFFF);
        GlStateManager.enableDepth();
    }
}
