package io.hamlook.aetheria.features.profile.viewer.ui.tabs;

import io.hamlook.aetheria.features.profile.data.ProfileData;
import io.hamlook.aetheria.features.profile.viewer.ui.ProfileViewerGUI;
import io.hamlook.aetheria.utils.StringUtils;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.render.NineSliceUtils;
import io.hamlook.aetheria.utils.render.TextRenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;

import java.awt.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class BasicInfoTab extends Tab {

    public BasicInfoTab() {
        super(0, "Overview");
    }

    @Override
    public void draw(float xPos, float yPos, int width, int height, ProfileData data, Minecraft mc) {
        float textScale = ProfileViewerGUI.getScaleText();
        float pad = ProfileViewerGUI.getScaledF(12);

        float cardW = (width - (pad * 3)) / 2f;
        float cardH = (height - pad) / 2f;

        float col1X = xPos + pad;
        float col2X = col1X + cardW + pad;
        float row2Y = yPos + cardH + pad;

        drawCard(mc, "§bProfile & Time", getProfileLines(data), col1X, yPos, cardW, cardH, textScale);
        drawCard(mc, "§6Economy", getEconomyLines(data), col2X, yPos, cardW, cardH, textScale);
        drawCard(mc, "§cCombat Stats", getCombatLines(data), col1X, row2Y, cardW, cardH, textScale);
        drawCard(mc, "§aNetworth", getNetworthLines(data), col2X, row2Y, cardW, cardH, textScale);
    }

    private void drawCard(Minecraft mc, String title, List<String> lines, float x, float y, float w, float h, float textScale) {
        NineSliceUtils.draw(ProfileViewerGUI.CONTAINER_BG, (int) x, (int) y, (int) w, (int) h, 6, 18);

        float pad = ProfileViewerGUI.getScaledF(10);
        float titleScale = textScale * 1.15f;

        TextRenderUtils.drawStringScaleAware(title, x + pad, y + pad, titleScale, false);

        float lineY = y + pad + (titleScale * MinecraftCompat.getFontRenderer().FONT_HEIGHT) + ProfileViewerGUI.getScaledF(4);
        Gui.drawRect((int)(x + pad), (int)lineY, (int)(x + w - pad), (int)(lineY + Math.max(1, ProfileViewerGUI.getScaled(1))), new Color(255, 255, 255, 25).getRGB());

        float currentY = lineY + ProfileViewerGUI.getScaledF(8);
        for (String line : lines) {
            TextRenderUtils.drawStringScaleAware(line, x + pad, currentY, textScale, false);
            currentY += (textScale * MinecraftCompat.getFontRenderer().FONT_HEIGHT) + ProfileViewerGUI.getScaledF(4);
        }
    }

    private List<String> getProfileLines(ProfileData data) {
        List<String> lines = new ArrayList<>();
        if (data == null || data.baseData == null) return lines;

        lines.add("§7Level: §8[" + getColor(data.baseData.currentLevel) + data.baseData.currentLevel + "§8]");
        lines.add("§7Mode: §f" + (data.baseData.currentMode != null ? data.baseData.currentMode.getName() : "§aNormal"));
        if (data.baseData.stats != null) {
            lines.add("§7Playtime: §f" + formatTime(data.baseData.stats.playtime,false));
        }
        lines.add("§7Profile Age: §f" + formatTime(data.baseData.profileAge,true));

        return lines;
    }

    private List<String> getEconomyLines(ProfileData data) {
        List<String> lines = new ArrayList<>();
        if (data == null || data.baseData == null) return lines;

        lines.add("§7Purse: §6" + StringUtils.formatNumber(data.baseData.currentPurse));
        lines.add("§7Bank: §e" + StringUtils.formatNumber(data.baseData.bankBalance));
        lines.add("§7Bits: §b" + StringUtils.formatNumber(data.baseData.bitCount));

        return lines;
    }

    private List<String> getCombatLines(ProfileData data) {
        List<String> lines = new ArrayList<>();
        if (data == null || data.baseData == null || data.baseData.stats == null) return lines;

        long k = data.baseData.stats.kills;
        long d = data.baseData.stats.deaths;
        float kd = (d == 0) ? k : ((float) k / d);

        lines.add("§7Kills: §a" + StringUtils.formatNumber(k));
        lines.add("§7Deaths: §c" + StringUtils.formatNumber(d));
        lines.add("§7K/D Ratio: §e" + String.format("%.2f", kd));
        lines.add("§7Highest Crit: §d" + StringUtils.formatNumber(data.baseData.stats.highCritDamage));

        return lines;
    }

    private List<String> getNetworthLines(ProfileData data) {
        List<String> lines = new ArrayList<>();
        if (data == null || data.baseData == null || data.baseData.networth == null) return lines;

        lines.add("§7Total: §a" + StringUtils.formatNumber(data.baseData.networth.totalNetWorth));
        lines.add("§7Items: §e" + StringUtils.formatNumber(data.baseData.networth.itemNetWorth));
        lines.add("§7Armor: §9" + StringUtils.formatNumber(data.baseData.networth.armorNetWorth));
        lines.add("§7Accessories: §5" + StringUtils.formatNumber(data.baseData.networth.accessoriesNetWorth));
        lines.add("§7Pets: §d" + StringUtils.formatNumber(data.baseData.networth.petNetWorth));

        return lines;
    }

    private String getColor(int level) {
        if (level > 480) return "§4";
        if (level > 440) return "§c";
        if (level > 400) return "§6";
        if (level > 360) return "§5";
        if (level > 320) return "§d";
        if (level > 280) return "§9";
        if (level > 240) return "§3";
        if (level > 200) return "§b";
        if (level > 160) return "§2";
        if (level > 120) return "§a";
        if (level > 80) return "§e";
        return "§f";
    }

    private String formatTime(long seconds,boolean useDays) {
        Duration duration = Duration.ofSeconds(seconds);
        long hours = duration.toHours();
        long days = hours / 24;
        if(useDays) {
            if (days > 0) {
                return days + "d " + (hours%24 > 0 ? (hours % 24) + "h" : "");
            }
        }
        return hours + "h";
    }
}