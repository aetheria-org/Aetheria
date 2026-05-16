package com.jef.justenoughfakepixel.features.profile.viewer.ui.modules;

import com.jef.justenoughfakepixel.core.config.utils.StringUtils;
import com.jef.justenoughfakepixel.features.profile.data.base.BaseData;
import com.jef.justenoughfakepixel.features.profile.vars.ProfileMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;

import java.time.Duration;

public class BaseDataModule {

    public static void draw(BaseData data,int xPos, int yPos,int width,int height,float scale) {
        Gui.drawRect(xPos,yPos,xPos + width,yPos + height,0xFF1A1A1A);
        GlStateManager.pushMatrix();
        GlStateManager.translate(xPos,yPos,0);
        GlStateManager.scale(scale,scale,scale);
        Minecraft.getMinecraft().fontRendererObj.drawString("§aProfile: §f" + data.playerProfile,5,5,-1);
        Minecraft.getMinecraft().fontRendererObj.drawString("§aProfile Type: " + (data.currentMode == ProfileMode.IRONMAN ? "§fIronMan" : "§fNormal"),5,15,-1);
        Minecraft.getMinecraft().fontRendererObj.drawString("§8§m------------------------------------",5,25,-1);
        Minecraft.getMinecraft().fontRendererObj.drawString("§bPlaytime: §f" + getTime(data.stats.playtime),5,35,-1);
        Minecraft.getMinecraft().fontRendererObj.drawString("§bProfile Age: §f" + getTime(data.profileAge),5,45,-1);
        Minecraft.getMinecraft().fontRendererObj.drawString("§bSkyblock Level: §8[" + getColor(data.currentLevel) + data.currentLevel + "§8]",5,55,-1);
        Minecraft.getMinecraft().fontRendererObj.drawString("§8§m------------------------------------",5,65,-1);
        Minecraft.getMinecraft().fontRendererObj.drawString("§6Networth: §e" + StringUtils.formatNumber(data.networth.totalNetWorth),5,75,-1);
        Minecraft.getMinecraft().fontRendererObj.drawString("§6Bank: §e" + StringUtils.formatNumber(data.bankBalance),5,85,-1);
        Minecraft.getMinecraft().fontRendererObj.drawString("§6Purse: §e" + StringUtils.formatNumber(data.currentPurse),5,95,-1);
        Minecraft.getMinecraft().fontRendererObj.drawString("§6Bits: §e" + StringUtils.formatNumber(data.bitCount),5,105,-1);
        Minecraft.getMinecraft().fontRendererObj.drawString("§8§m------------------------------------",5,115,-1);
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
        Duration duration = Duration.ofSeconds(time);
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;
        return String.format("%dh %dm and %ds", hours, minutes, seconds);
    }

}
