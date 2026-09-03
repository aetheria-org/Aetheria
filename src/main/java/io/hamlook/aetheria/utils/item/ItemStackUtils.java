// Credit: Skytils (https://github.com/Skytils/SkytilsMod) (AGPLv3)

package io.hamlook.aetheria.utils.item;

import io.hamlook.aetheria.utils.compat.GlStateManagerCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

public class ItemStackUtils {

    private static final Minecraft mc = MinecraftCompat.getMinecraft();

    private ItemStackUtils() {
    }

    public static void drawTip(String tip, int x, int y) {
        drawTip(tip, x, y, 0xFFFFFF);
    }

    public static void drawTip(String tip, int x, int y, int color) {
        FontRenderer fr = mc.fontRendererObj;
        GlStateManagerCompat.disableDepth();
        GlStateManagerCompat.disableBlend();
        fr.drawStringWithShadow(tip, x + 17 - fr.getStringWidth(tip), y + 9, color);
        GlStateManagerCompat.enableDepth();
    }
}
