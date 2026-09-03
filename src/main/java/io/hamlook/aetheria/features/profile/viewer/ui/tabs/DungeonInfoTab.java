package io.hamlook.aetheria.features.profile.viewer.ui.tabs;

import io.hamlook.aetheria.features.profile.data.ProfileData;
import io.hamlook.aetheria.features.profile.data.dungeon.Floor;
import io.hamlook.aetheria.features.profile.data.dungeon.FloorData;
import io.hamlook.aetheria.features.profile.viewer.ui.ProfileViewerGUI;
import io.hamlook.aetheria.utils.StringUtils;
import io.hamlook.aetheria.utils.render.NineSliceUtils;
import io.hamlook.aetheria.utils.render.TextRenderUtils;
import net.minecraft.client.Minecraft;

public class DungeonInfoTab extends Tab {

    public DungeonInfoTab() {
        super(3, "Dungeons");
    }

    @Override
    public void draw(float xPos, float yPos, int width, int height, ProfileData data, Minecraft mc) {
        float pad = ProfileViewerGUI.getScaledF(8);
        float textScale = ProfileViewerGUI.getScaleText();

        float classSectionH = ProfileViewerGUI.getScaledF(35);
        NineSliceUtils.draw(ProfileViewerGUI.CONTAINER_BG, (int) xPos, (int) yPos, width, (int) classSectionH, 6, 18);

        float spacing = width / 5f;
        String[] classNames = {"Healer", "Mage", "Archer", "Berserk", "Tank"};
        int[] classLevels = {0, 0, 0, 0, 0};

        if (data != null && data.dungeonData != null) {
            classLevels[0] = data.dungeonData.healerLevel;
            classLevels[1] = data.dungeonData.mageLevel;
            classLevels[2] = data.dungeonData.archerLevel;
            classLevels[3] = data.dungeonData.bersLevel;
            classLevels[4] = data.dungeonData.tankLevel;
        }

        for (int i = 0; i < 5; i++) {
            float cX = xPos + (i * spacing) + (spacing / 2f);
            float cY = yPos + (classSectionH / 2f);
            String text = "§e" + classNames[i] + ": §a" + classLevels[i];
            TextRenderUtils.drawCenteredStringScaleAware(text, cX, cY, textScale, false);
        }

        float gridY = yPos + classSectionH + pad;
        float gridH = height - classSectionH - pad;

        int cols = 4;
        int rows = 2;
        float cardW = (width - (pad * (cols - 1))) / cols;
        float cardH = (gridH - (pad * (rows - 1))) / rows;

        int index = 0;
        for (Floor f : Floor.values()) {
            int col = index % cols;
            int row = index / cols;
            float cX = xPos + col * (cardW + pad);
            float cY = gridY + row * (cardH + pad);

            FloorData fData = null;
            if (data != null && data.dungeonData != null && data.dungeonData.floorData != null) {
                fData = data.dungeonData.floorData.get(f);
            }

            drawFloorCard(mc, f, fData, cX, cY, cardW, cardH, textScale);
            index++;
        }
    }

    private void drawFloorCard(Minecraft mc, Floor floor, FloorData fData, float x, float y, float w, float h, float textScale) {
        NineSliceUtils.draw(ProfileViewerGUI.CONTAINER_BG, (int) x, (int) y, (int) w, (int) h, 6, 18);

        float pad = ProfileViewerGUI.getScaledF(6);

        float titleScale = textScale * 1.2f;
        float statScale = textScale * 0.95f;

        String title = "§c§l" + floor.floorName;
        TextRenderUtils.drawStringScaleAware(title, x + pad, y + pad, titleScale, false);

        float currentY = y + pad + (titleScale * mc.fontRendererObj.FONT_HEIGHT) + ProfileViewerGUI.getScaledF(4);

        float lineSpc = statScale * mc.fontRendererObj.FONT_HEIGHT + ProfileViewerGUI.getScaledF(1.5f);

        if (fData == null || fData.bossKills == 0) {
            TextRenderUtils.drawStringScaleAware("§8Not Completed", x + pad, currentY, statScale, false);
            return;
        }

        TextRenderUtils.drawStringScaleAware("§7Kills: §a" + StringUtils.formatNumber(fData.bossKills), x + pad, currentY, statScale, false);
        currentY += lineSpc;

        TextRenderUtils.drawStringScaleAware("§7Best Score: §6" + fData.bestScore, x + pad, currentY, statScale, false);
        currentY += lineSpc;

        long maxDmg = getMaxDamage(fData);
        if (maxDmg > 0) {
            TextRenderUtils.drawStringScaleAware("§7Max Dmg: §d" + StringUtils.formatNumber(maxDmg), x + pad, currentY, statScale, false);
            currentY += lineSpc;
        }

        TextRenderUtils.drawStringScaleAware("§7Enemies Killed: §c" + StringUtils.formatNumber(fData.totalEnemiesKilled), x + pad, currentY, statScale, false);
        currentY += lineSpc;

        TextRenderUtils.drawStringScaleAware("§7Fastest: §b" + formatTime(fData.fastestTime), x + pad, currentY, statScale, false);
        currentY += lineSpc;

        TextRenderUtils.drawStringScaleAware("§7S: §b" + formatTime(fData.fastestSTime) + "  §7S+: §b" + formatTime(fData.fastestSPlusTime), x + pad, currentY, statScale, false);
    }

    private long getMaxDamage(FloorData data) {
        if (data == null) return 0;
        long m1 = Math.max(data.mostHealerDmg, data.mostMageDamage);
        long m2 = Math.max(data.mostArcherDamage, data.mostBersDamage);
        return Math.max(Math.max(m1, m2), data.mostTankDamage);
    }

    private String formatTime(long seconds) {
        if (seconds <= 0) return "N/A";
        long mins = seconds / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d", mins, secs);
    }
}