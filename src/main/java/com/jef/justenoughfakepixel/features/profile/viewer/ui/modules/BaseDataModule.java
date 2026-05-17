package com.jef.justenoughfakepixel.features.profile.viewer.ui.modules;

import com.jef.justenoughfakepixel.core.config.utils.StringUtils;
import com.jef.justenoughfakepixel.features.profile.data.base.BaseData;
import com.jef.justenoughfakepixel.features.profile.vars.ProfileMode;
import com.jef.justenoughfakepixel.features.profile.viewer.ui.ProfileViewerGUI;
import com.jef.justenoughfakepixel.features.profile.viewer.ui.util.StringDrawer;
import com.jef.justenoughfakepixel.utils.render.NineSliceUtils;
import com.jef.justenoughfakepixel.utils.render.ResolutionUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;

import java.time.Duration;

public class BaseDataModule {

    public static void draw(BaseData data,int xPos, int yPos,int width,int height,float scale) {
        float scaleDisplay = ResolutionUtils.getXStatic(1);
        float textScale = scale * 2.0f;
        float headerScale = scale * 3.0f;

        float finalHeaderScale = Math.max(0.25f, headerScale * scaleDisplay);
        float finalTextScale = Math.max(0.25f, textScale * scaleDisplay);
        int bgY = (int) (yPos + (-10 - 8) * finalHeaderScale);
        int bottomY = (int) (yPos + (155 + 9 + 8) * finalTextScale);

        int bgHeight = bottomY - bgY;
        NineSliceUtils.draw(ProfileViewerGUI.CONTAINER_BG, xPos, bgY, width, bgHeight, 6, 18);GlStateManager.pushMatrix();
        GlStateManager.translate(xPos,yPos,0);

        String headerText = "§a§lOVERVIEW";
        int headerWidth = Minecraft.getMinecraft().fontRendererObj.getStringWidth(headerText);
        float centeredX = (width / finalHeaderScale - headerWidth) / 2.0f;
        StringDrawer.drawString(headerText, centeredX, -10, headerScale);
        StringDrawer.drawString("§8§m---------------------------",5,25,textScale);
        StringDrawer.drawString("§aProfile: §f" + data.playerProfile,5,5,textScale);
        StringDrawer.drawString("§aProfile Type: " + (data.currentMode == ProfileMode.IRONMAN ? "§fIronMan" : "§fNormal"),5,15,textScale);
        StringDrawer.drawString("§8§m---------------------------",5,25,textScale);
        StringDrawer.drawString("§bPlaytime: §f" + getTime(data.stats.playtime),5,35,textScale);
        StringDrawer.drawString("§bProfile Age: §f" + getTime(data.profileAge),5,45,textScale);
        StringDrawer.drawString("§bSkyblock Level: §8[" + getColor(data.currentLevel) + data.currentLevel + "§8]",5,55,textScale);
        StringDrawer.drawString("§8§m---------------------------",5,65,textScale);
        StringDrawer.drawString("§5Bank: §d" + StringUtils.formatNumber(data.bankBalance),5,75,textScale);
        StringDrawer.drawString("§5Purse: §d" + StringUtils.formatNumber(data.currentPurse),5,85,textScale);
        StringDrawer.drawString("§5Bits: §d" + StringUtils.formatNumber(data.bitCount),5,95,textScale);
        StringDrawer.drawString("§8§m---------------------------",5,105,textScale);
        StringDrawer.drawString("§6Networth: §e" + StringUtils.formatNumber(data.networth.totalNetWorth),5,115,textScale);
        StringDrawer.drawString("§6Item Networth: §e" + StringUtils.formatNumber(data.networth.itemNetWorth),5,125,textScale);
        StringDrawer.drawString("§6Armor Networth: §e" + StringUtils.formatNumber(data.networth.armorNetWorth),5,135,textScale);
        StringDrawer.drawString("§6Accessory Networth: §e" + StringUtils.formatNumber(data.networth.accessoriesNetWorth),5,145,textScale);
        StringDrawer.drawString("§6Pets Networth: §e" + StringUtils.formatNumber(data.networth.petNetWorth),5,155,textScale);
        GlStateManager.popMatrix();
    }

    public static String getColor(int level){
        if(level > 480) return "§4";
        if(level > 440) return "§c";
        if(level > 400) return "§6";
        if(level > 360) return "§5";
        if(level > 320) return "§d";
        if(level > 280) return "§9";
        if(level > 240) return "§3";
        if(level > 200) return "§b";
        if(level > 160) return "§2";
        if(level > 120) return "§a";
        if(level > 80) return "§e";
        return "§f";
    }
    public static String getTime(long time){
        return String.format("%dh", Duration.ofSeconds(time).toHours());
    }

}
