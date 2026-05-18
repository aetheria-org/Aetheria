package com.jef.justenoughfakepixel.features.profile.viewer.ui.tabs;

import com.jef.justenoughfakepixel.core.config.utils.StringUtils;
import com.jef.justenoughfakepixel.features.profile.data.ProfileData;
import com.jef.justenoughfakepixel.features.profile.data.skills.Skill;
import com.jef.justenoughfakepixel.features.profile.data.skills.SkillData;
import com.jef.justenoughfakepixel.features.profile.viewer.ui.ProfileViewerGUI;
import com.jef.justenoughfakepixel.features.profile.viewer.ui.util.StringDrawer;
import com.jef.justenoughfakepixel.utils.render.ItemRenderUtils;
import com.jef.justenoughfakepixel.utils.render.NineSliceUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

public class SkillInfoTab extends Tab {

    public SkillInfoTab() {
        super(1, "Skills");
    }

    @Override
    public void draw(float xPos, float yPos, int width, int height, ProfileData data, Minecraft mc) {
        float pad = ProfileViewerGUI.getScaledF(8);

        int cols = 2;
        int rows = 6;

        float cardW = (width - (pad * (cols + 1))) / cols;
        float cardH = (height - (pad * (rows + 1))) / rows;

        int index = 0;
        for (Skill skill : Skill.values()) {
            int col = index % cols;
            int row = index / cols;

            float cX = xPos + pad + col * (cardW + pad);
            float cY = yPos + pad + row * (cardH + pad);

            SkillData sData = null;
            if (data != null && data.skillData != null && data.skillData.skills != null) {
                sData = data.skillData.skills.get(skill);
            }

            int level = sData != null ? sData.currentLevel : 0;
            long curXp = sData != null ? sData.currentXp : 0;
            long reqXp = sData != null ? sData.requiredXp : 50;

            drawSkillCard(mc, skill.name, level, curXp, reqXp, skill.skillColor.getRGB(), cX, cY, cardW, cardH);
            index++;
        }

        int col = index % cols;
        int row = index / cols;
        float cX = xPos + pad + col * (cardW + pad);
        float cY = yPos + pad + row * (cardH + pad);

        int cataLevel = 0;
        long cataCurXp = 0;
        long cataReqXp = 50;

        if (data != null && data.dungeonData != null) {
            cataLevel = data.dungeonData.cataLevel;
            cataCurXp = data.dungeonData.curProgress;
            cataReqXp = data.dungeonData.reqProgress;
        }

        drawSkillCard(mc, "Catacombs", cataLevel, cataCurXp, cataReqXp, Skill.COMBAT.skillColor.getRGB(), cX, cY, cardW, cardH);
    }

    private void drawSkillCard(Minecraft mc, String skillName, int currentLevel, long currentXp, long requiredXp, int ringColor, float x, float y, float w, float h) {
        NineSliceUtils.draw(ProfileViewerGUI.CONTAINER_BG, (int) x, (int) y, (int) w, (int) h, 6, 18);

        float textScale = ProfileViewerGUI.getScaleText();
        float pad = ProfileViewerGUI.getScaledF(6);

        boolean isMaxed = (requiredXp == -1L);
        float progress = isMaxed ? 1.0f : (float) ((double) currentXp / requiredXp);

        if (progress > 1.0f) progress = 1.0f;
        if (progress < 0.0f) progress = 0.0f;

        float radius = (h / 2f) - pad;
        float centerX = x + pad + radius;
        float centerY = y + (h / 2f);
        float thickness = ProfileViewerGUI.getScaledF(5);

        // Draw Rings
        drawRing(centerX, centerY, radius, thickness, 1.0f, 0x40FFFFFF);
        drawRing(centerX, centerY, radius, thickness, progress, ringColor);

        // NEW: Draw Item Icon exactly in the center of the ring
        ItemStack icon = getSkillItem(skillName);
        int iconSize = (int) (radius * 1.3f);
        ItemRenderUtils.renderItemIcon(mc, icon, (int) (centerX - iconSize / 2f), (int) (centerY - iconSize / 2f), iconSize);

        // Draw Texts
        float textStartX = centerX + radius + pad + ProfileViewerGUI.getScaledF(4);
        float textYTop = y + pad + ProfileViewerGUI.getScaledF(2);
        float textYBottom = y + (h / 2f) + ProfileViewerGUI.getScaledF(1);

        StringDrawer.drawString("§e§l" + skillName.toUpperCase(), textStartX, textYTop, textScale, false);

        if (isMaxed) {
            String overflow = currentXp > 0 ? " §7(+" + StringUtils.formatNumber(currentXp) + ")" : "";
            StringDrawer.drawString("§dMAXED" + overflow, textStartX, textYBottom, textScale * 0.85f, false);
        } else {
            String xpText = "§b" + StringUtils.formatNumber(currentXp) + " §7/ §3" + StringUtils.formatNumber(requiredXp);
            StringDrawer.drawString(xpText, textStartX, textYBottom, textScale * 0.85f, false);
        }

        String lvlText = (isMaxed ? "§d" : "§a") + "LVL " + currentLevel;
        float lvlWidth = mc.fontRendererObj.getStringWidth(lvlText) * textScale;
        StringDrawer.drawString(lvlText, x + w - pad - lvlWidth, textYTop, textScale, false);
    }

    private ItemStack getSkillItem(String skillName) {
        switch (skillName.toLowerCase()) {
            case "farming": return new ItemStack(Items.golden_hoe);
            case "mining": return new ItemStack(Items.diamond_pickaxe);
            case "combat": return new ItemStack(Items.iron_sword);
            case "foraging": return new ItemStack(Item.getItemFromBlock(Blocks.sapling), 1, 3);
            case "fishing": return new ItemStack(Items.fishing_rod);
            case "enchanting": return new ItemStack(Item.getItemFromBlock(Blocks.enchanting_table));
            case "alchemy": return new ItemStack(Items.brewing_stand);
            case "runecrafting": return new ItemStack(Items.magma_cream);
            case "social": return new ItemStack(Items.emerald);
            case "taming": return new ItemStack(Items.bone);
            case "carpentry": return new ItemStack(Item.getItemFromBlock(Blocks.crafting_table));
            case "catacombs": return new ItemStack(Items.skull, 1, 1);
            default: return new ItemStack(Items.paper);
        }
    }

    private void drawRing(float x, float y, float radius, float thickness, float progress, int hexColor) {
        float alpha = (float)(hexColor >> 24 & 255) / 255.0F;
        float red = (float)(hexColor >> 16 & 255) / 255.0F;
        float green = (float)(hexColor >> 8 & 255) / 255.0F;
        float blue = (float)(hexColor & 255) / 255.0F;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(thickness);
        GlStateManager.color(red, green, blue, alpha);

        GL11.glBegin(GL11.GL_LINE_STRIP);

        int segments = 100;
        int maxSegments = (int)(segments * progress);

        for (int i = 0; i <= maxSegments; i++) {
            double angle = (Math.PI * 2 * i / segments) - (Math.PI / 2);
            GL11.glVertex2d(x + Math.cos(angle) * radius, y + Math.sin(angle) * radius);
        }

        GL11.glEnd();

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }
}