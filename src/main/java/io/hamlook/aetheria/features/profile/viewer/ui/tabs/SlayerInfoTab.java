package io.hamlook.aetheria.features.profile.viewer.ui.tabs;

import io.hamlook.aetheria.features.profile.data.ProfileData;
import io.hamlook.aetheria.features.profile.data.slayer.Slayer;
import io.hamlook.aetheria.features.profile.data.slayer.SlayerData;
import io.hamlook.aetheria.features.profile.viewer.ui.ProfileViewerGUI;
import io.hamlook.aetheria.utils.StringUtils;
import io.hamlook.aetheria.utils.compat.GlStateManagerCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.render.ItemRenderUtils;
import io.hamlook.aetheria.utils.render.NineSliceUtils;
import io.hamlook.aetheria.utils.render.TextRenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

public class SlayerInfoTab extends Tab {

    public SlayerInfoTab() {
        super(4, "Slayers");
    }

    @Override
    public void draw(float xPos, float yPos, int width, int height, ProfileData data, Minecraft mc) {
        float pad = ProfileViewerGUI.getScaledF(8);

        int cols = 2;
        int rows = 3;

        float cardW = (width - (pad * (cols + 1))) / cols;
        float cardH = (height - (pad * (rows + 1))) / rows;

        int index = 0;
        int totalItems = Slayer.values().length;
        for (Slayer slayer : Slayer.values()) {
            int col = index % cols;
            int row = index / cols;

            float cX = xPos + pad + col * (cardW + pad);
            float cY = yPos + pad + row * (cardH + pad);

            boolean isLastAndOdd = (index == totalItems - 1) && (totalItems % 2 != 0);
            if (isLastAndOdd) {
                cX += (cardW / 2f) + (pad / 2f);
            }

            SlayerData sData = null;
            if (data != null && data.slayersData != null && data.slayersData.slayerData != null) {
                sData = data.slayersData.slayerData.get(slayer);
            }

            drawSlayerCard(mc, slayer, sData, cX, cY, cardW, cardH);
            index++;
        }
    }

    private void drawSlayerCard(Minecraft mc, Slayer slayer, SlayerData sData, float x, float y, float w, float h) {
        NineSliceUtils.draw(ProfileViewerGUI.CONTAINER_BG, (int) x, (int) y, (int) w, (int) h, 6, 18);

        float textScale = ProfileViewerGUI.getScaleText();
        float pad = ProfileViewerGUI.getScaledF(8);

        boolean hasData = (sData != null);
        boolean isMaxed = hasData && (sData.reqExp <= 0 || sData.curLevel >= 9);
        float progress = 0.0f;

        if (hasData) {
            if (isMaxed) progress = 1.0f;
            else progress = (float) ((double) sData.curExp / sData.reqExp);
        }

        if (progress > 1.0f) progress = 1.0f;
        if (progress < 0.0f) progress = 0.0f;

        float radius = (h / 2.2f) - pad;
        float centerX = x + pad + radius;
        float centerY = y + (h / 2f);
        float thickness = ProfileViewerGUI.getScaledF(5);

        // Draw Rings
        drawRing(centerX, centerY, radius, thickness, 1.0f, 0x40FFFFFF);
        int ringColor = slayer.guiColor.getRGB();
        drawRing(centerX, centerY, radius, thickness, progress, ringColor);

        // NEW: Draw Item Icon exactly in the center of the ring
        ItemStack icon = getSlayerItem(slayer);
        int iconSize = (int) (radius * 1.3f);
        ItemRenderUtils.renderItemIcon(mc, icon, (int) (centerX - iconSize / 2f), (int) (centerY - iconSize / 2f), iconSize);

        float textStartX = centerX + radius + pad + ProfileViewerGUI.getScaledF(6);
        float currentY = y + pad;
        float lineSpc = textScale * MinecraftCompat.getFontRenderer().FONT_HEIGHT + ProfileViewerGUI.getScaledF(3);

        String colorPrefix = getTitleColor(slayer);
        TextRenderUtils.drawStringScaleAware(colorPrefix + "§l" + slayer.itemName.toUpperCase(), textStartX, currentY, textScale * 1.1f, false);
        currentY += lineSpc + ProfileViewerGUI.getScaledF(2);

        if (!hasData || sData.curLevel == 0 && sData.curExp == 0) {
            TextRenderUtils.drawStringScaleAware("§8Not Started", textStartX, currentY, textScale, false);
            return;
        }

        String lvlText = (isMaxed ? "§d" : "§a") + "LVL " + sData.curLevel;
        String xpText = isMaxed ? "§dMAXED" : "§b" + StringUtils.formatNumber(sData.curExp) + " §7/ §3" + StringUtils.formatNumber(sData.reqExp);

        TextRenderUtils.drawStringScaleAware(lvlText + "  §8-  " + xpText, textStartX, currentY, textScale * 0.9f, false);
        currentY += lineSpc + ProfileViewerGUI.getScaledF(4);

        float statScale = textScale * 0.85f;
        float killSpc = statScale * MinecraftCompat.getFontRenderer().FONT_HEIGHT + ProfileViewerGUI.getScaledF(2);

        String killsRow1 = "§7T1: §a" + sData.t1Kills + "  §7T2: §a" + sData.t2Kills + "  §7T3: §a" + sData.t3Kills;
        TextRenderUtils.drawStringScaleAware(killsRow1, textStartX, currentY, statScale, false);
        currentY += killSpc;

        String killsRow2 = "§7T4: §a" + sData.t4Kills + "  §7T5: §a" + sData.t5Kills;
        TextRenderUtils.drawStringScaleAware(killsRow2, textStartX, currentY, statScale, false);
    }

    private ItemStack getSlayerItem(Slayer slayer) {
        switch (slayer.name()) {
            case "ZOMBIE": return new ItemStack(Items.rotten_flesh);
            case "SPIDER": return new ItemStack(Items.spider_eye);
            case "WOLF": return new ItemStack(Items.bone);
            case "ENDERMAN": return new ItemStack(Items.ender_pearl);
            case "BLAZE": return new ItemStack(Items.blaze_rod);
            default: return new ItemStack(Items.paper);
        }
    }

    private String getTitleColor(Slayer slayer) {
        switch(slayer) {
            case ZOMBIE: return "§2";
            case SPIDER: return "§4";
            case WOLF: return "§f";
            case ENDERMAN: return "§5";
            case BLAZE: return "§6";
            default: return "§c";
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