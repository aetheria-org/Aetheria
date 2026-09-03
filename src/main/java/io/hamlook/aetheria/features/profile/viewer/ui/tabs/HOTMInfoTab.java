package io.hamlook.aetheria.features.profile.viewer.ui.tabs;

import io.hamlook.aetheria.features.profile.data.HOTMData;
import io.hamlook.aetheria.features.profile.data.ProfileData;
import io.hamlook.aetheria.features.profile.viewer.ui.ProfileViewerGUI;
import io.hamlook.aetheria.utils.StringUtils;
import io.hamlook.aetheria.utils.compat.GlStateManagerCompat;
import io.hamlook.aetheria.utils.render.ItemRenderUtils;
import io.hamlook.aetheria.utils.render.NineSliceUtils;
import io.hamlook.aetheria.utils.render.TextRenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

public class HOTMInfoTab extends Tab {

    public HOTMInfoTab() {
        super(7, "HOTM");
    }

    @Override
    public void draw(float xPos, float yPos, int width, int height, ProfileData data, Minecraft mc) {
        float pad = ProfileViewerGUI.getScaledF(8);
        
        NineSliceUtils.draw(ProfileViewerGUI.CONTAINER_BG, (int)xPos, (int)yPos, width, height, 6, 18);

        float centerX = xPos + (width / 2f);
        
        TextRenderUtils.drawCenteredStringScaleAware("§dHeart of the Mountain", centerX, yPos + ProfileViewerGUI.getScaledF(20), ProfileViewerGUI.getScaleHeader() * 1.5f, false);

        if (data == null || data.hotmData == null) {
            TextRenderUtils.drawCenteredStringScaleAware("§cNo HOTM Data Found!", centerX, yPos + (height / 2f), ProfileViewerGUI.getScaleText(), false);
            return;
        }

        HOTMData hData = data.hotmData;

        float radius = ProfileViewerGUI.getScaledF(40);
        float ringY = yPos + ProfileViewerGUI.getScaledF(80);
        
        float progress = 1.0f;
        if (hData.requiredProgress > 0) {
            progress = (float)((double)hData.currentProgress / hData.requiredProgress);
            if (progress > 1) progress = 1;
            if (progress < 0) progress = 0;
        }

        drawRing(centerX, ringY, radius, ProfileViewerGUI.getScaledF(8), 1.0f, 0x40FFFFFF);
        drawRing(centerX, ringY, radius, ProfileViewerGUI.getScaledF(8), progress, 0xFFFF55FF);
        
        int iconSize = (int)(radius * 1.5f);
        ItemStack icon = new ItemStack(Blocks.emerald_ore);
        ItemRenderUtils.renderItemIcon(mc, icon, (int)(centerX - iconSize / 2f), (int)(ringY - iconSize / 2f), iconSize);

        TextRenderUtils.drawCenteredStringScaleAware("§dTier " + hData.hotmTier, centerX, ringY + radius + ProfileViewerGUI.getScaledF(15), ProfileViewerGUI.getScaleText() * 1.2f, false);

        String xpText = "§b" + StringUtils.formatNumber(hData.currentProgress) + " §7/ §3" + (hData.requiredProgress > 0 ? StringUtils.formatNumber(hData.requiredProgress) : "MAX");
        TextRenderUtils.drawCenteredStringScaleAware(xpText, centerX, ringY + radius + ProfileViewerGUI.getScaledF(30), ProfileViewerGUI.getScaleText(), false);

        float statsY = ringY + radius + ProfileViewerGUI.getScaledF(60);
        float lineSpace = ProfileViewerGUI.getScaledF(27);
        
        float boxW = width * 0.8f;
        float boxX = centerX - (boxW / 2f);
        float boxH = ProfileViewerGUI.getScaledF(22);

        String[] labels = {"§aTokens: ", "§2Mithril Powder: ", "§dGemstone Powder: ", "§eCommissions: "};
        String[] values = {StringUtils.formatNumber(hData.tokens), StringUtils.formatNumber(hData.mithrilPowder), StringUtils.formatNumber(hData.gemstonePowder), StringUtils.formatNumber(hData.commissions)};

        for (int i = 0; i < 4; i++) {
            float y = statsY + (lineSpace * i);
            NineSliceUtils.draw(ProfileViewerGUI.CONTAINER_BG, (int)boxX, (int)y, (int)boxW, (int)boxH, 6, 18);
            TextRenderUtils.drawStringScaleAware(labels[i] + "§f" + values[i], boxX + ProfileViewerGUI.getScaledF(8), y + ProfileViewerGUI.getScaledF(4), ProfileViewerGUI.getScaleText(), false);
        }

    }

    private void drawRing(float x, float y, float radius, float thickness, float progress, int hexColor) {
        float alpha = (float)(hexColor >> 24 & 255) / 255.0F;
        float red = (float)(hexColor >> 16 & 255) / 255.0F;
        float green = (float)(hexColor >> 8 & 255) / 255.0F;
        float blue = (float)(hexColor & 255) / 255.0F;

        GlStateManagerCompat.pushMatrix();
        GlStateManagerCompat.enableBlend();
        GlStateManagerCompat.disableTexture2D();
        GlStateManagerCompat.tryBlendFuncSeparate(770, 771, 1, 0);

        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(thickness);
        GlStateManagerCompat.color(red, green, blue, alpha);

        GL11.glBegin(GL11.GL_LINE_STRIP);

        int segments = 100;
        int maxSegments = (int)(segments * progress);

        for (int i = 0; i <= maxSegments; i++) {
            double angle = (Math.PI * 2 * i / segments) - (Math.PI / 2);
            GL11.glVertex2d(x + Math.cos(angle) * radius, y + Math.sin(angle) * radius);
        }

        GL11.glEnd();

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManagerCompat.enableTexture2D();
        GlStateManagerCompat.disableBlend();
        GlStateManagerCompat.popMatrix();
    }
}
