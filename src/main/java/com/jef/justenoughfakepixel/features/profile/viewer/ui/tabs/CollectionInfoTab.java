package com.jef.justenoughfakepixel.features.profile.viewer.ui.tabs;

import com.jef.justenoughfakepixel.core.config.utils.StringUtils;
import com.jef.justenoughfakepixel.features.profile.data.ProfileData;
import com.jef.justenoughfakepixel.features.profile.data.collection.CollectionBase;
import com.jef.justenoughfakepixel.features.profile.data.collection.CollectionData;
import com.jef.justenoughfakepixel.features.profile.data.collection.CollectionType;
import com.jef.justenoughfakepixel.features.profile.viewer.ui.ProfileViewerGUI;
import com.jef.justenoughfakepixel.features.profile.viewer.ui.util.StringDrawer;
import com.jef.justenoughfakepixel.utils.render.ItemRenderUtils;
import com.jef.justenoughfakepixel.utils.render.NineSliceUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class CollectionInfoTab extends Tab {

    public static float scrollSpeed = 0.15f;

    private CollectionBase currentCategory = CollectionBase.FARMING;
    private boolean isDropdownOpen = false;

    private float scrollY = 0;
    private float scrollTarget = 0;

    private boolean wasMouseDown = false;
    private float dropX, dropY, dropW, dropH, itemHeight;

    public CollectionInfoTab() {
        super(4, "Collections");
    }

    @Override
    public void draw(float xPos, float yPos, int width, int height, ProfileData data, Minecraft mc) {
        float textScale = ProfileViewerGUI.getScaleText();
        float pad = ProfileViewerGUI.getScaledF(8);

        float topBarH = ProfileViewerGUI.getScaledF(24);
        float btnW = ProfileViewerGUI.getScaledF(140);
        float btnX = xPos + pad;
        float btnY = yPos + pad;

        NineSliceUtils.draw(ProfileViewerGUI.CONTAINER_BG, (int) btnX, (int) btnY, (int) btnW, (int) topBarH, 6, 18);
        String triggerText = "§a" + formatEnumName(currentCategory.name()) + " §7▼";
        StringDrawer.drawCenteredString(triggerText, btnX + (btnW / 2f), btnY + (topBarH / 2f), textScale * 0.9f, false);

        float gridY = btnY + topBarH + pad;
        float gridH = height - (gridY - yPos) - pad;
        int cols = 2;
        float cardW = (width - (pad * (cols + 1))) / cols;

        int visibleRows = 6;
        float cardH = (gridH - (pad * (visibleRows - 1))) / visibleRows;

        List<CollectionType> filteredTypes = new ArrayList<>();
        for (CollectionType type : CollectionType.values()) {
            if (type.base == currentCategory) {
                filteredTypes.add(type);
            }
        }

        int rows = (int) Math.ceil(filteredTypes.size() / (double) cols);
        float totalContentH = rows > 0 ? (rows * cardH) + ((rows - 1) * pad) : 0;
        float maxScroll = Math.max(0, totalContentH - gridH);

        handleInputEvents(mc, xPos, yPos, width, height, maxScroll);

        scrollY += (scrollTarget - scrollY) * scrollSpeed;
        if (Math.abs(scrollTarget - scrollY) < 0.5f) scrollY = scrollTarget;

        ScaledResolution res = new ScaledResolution(mc);
        int scaleFactor = res.getScaleFactor();

        int scissorX = (int) (xPos * scaleFactor);
        int scissorY = (int) ((mc.displayHeight - (gridY + gridH) * scaleFactor));
        int scissorW = width * scaleFactor;
        int scissorH = (int) (gridH * scaleFactor);

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(scissorX, scissorY, scissorW, scissorH);

        GlStateManager.pushMatrix();
        GlStateManager.translate(0, -scrollY, 0);

        int index = 0;
        int totalItems = filteredTypes.size();

        for (CollectionType type : filteredTypes) {
            int col = index % cols;
            int row = index / cols;

            float cX = xPos + pad + col * (cardW + pad);
            float cY = gridY + row * (cardH + pad);

            boolean isLastAndOdd = (index == totalItems - 1) && (totalItems % 2 != 0);
            if (isLastAndOdd) {
                cX += (cardW / 2f) + (pad / 2f);
            }

            CollectionData cData = null;
            if (data != null && data.collectionData != null && data.collectionData.collections != null) {
                cData = data.collectionData.collections.get(type);
            }
            if (cData == null) cData = new CollectionData(0, 0, 100);

            drawCollectionCard(mc, type, cData, cX, cY, cardW, cardH, textScale);
            index++;
        }

        GlStateManager.popMatrix();
        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        if (isDropdownOpen) {
            drawCategoryDropdown(mc, xPos, btnY + topBarH, btnW, textScale);
        }
    }

    private void drawCollectionCard(Minecraft mc, CollectionType type, CollectionData cData, float x, float y, float w, float h, float textScale) {
        NineSliceUtils.draw(ProfileViewerGUI.CONTAINER_BG, (int) x, (int) y, (int) w, (int) h, 6, 18);

        float pad = ProfileViewerGUI.getScaledF(6);

        boolean notUnlocked = cData.level == 0 && cData.curProgress <= 0;
        boolean isMaxed = cData.level > 0 && (cData.maxProgress <= 0 || (cData.curProgress >= cData.maxProgress && cData.maxProgress > 0));

        float progress = 0.0f;
        if (isMaxed) {
            progress = 1.0f;
        } else if (cData.maxProgress > 0 && !notUnlocked) {
            progress = (float) ((double) cData.curProgress / cData.maxProgress);
        }

        if (progress > 1.0f) progress = 1.0f;
        if (progress < 0.0f) progress = 0.0f;

        float radius = (h / 2f) - pad;
        float centerX = x + pad + radius;
        float centerY = y + (h / 2f);
        float thickness = ProfileViewerGUI.getScaledF(4.5f);

        int ringColor = getCategoryColor(type.base);

        drawRing(centerX, centerY, radius, thickness, 1.0f, 0x40FFFFFF);

        if (!notUnlocked && progress > 0) {
            drawRing(centerX, centerY, radius, thickness, progress, isMaxed ? 0xFFFF55FF : ringColor);
        }

        ItemStack icon = getCollectionItem(type);
        int iconSize = (int) (radius * 1.3f);
        ItemRenderUtils.renderItemIcon(mc, icon, (int) (centerX - iconSize / 2f), (int) (centerY - iconSize / 2f), iconSize);

        float textStartX = centerX + radius + pad + ProfileViewerGUI.getScaledF(6);
        float textYTop = y + pad + ProfileViewerGUI.getScaledF(2);
        float textYBottom = y + (h / 2f) + ProfileViewerGUI.getScaledF(1);

        StringDrawer.drawString("§e" + type.itemName, textStartX, textYTop, textScale * 0.95f, false);

        if (notUnlocked) {
            StringDrawer.drawString("§8Not Unlocked", textStartX, textYBottom, textScale * 0.8f, false);
        } else if (isMaxed) {
            StringDrawer.drawString("§dMAXED §7" +(cData.curProgress > 0 ? "(" + StringUtils.formatNumber(cData.curProgress) + ")" : ""), textStartX, textYBottom, textScale * 0.8f, false);
        } else {
            String progressText = "§b" + StringUtils.formatNumber(cData.curProgress) + " §7/ §3" + StringUtils.formatNumber(cData.maxProgress);
            StringDrawer.drawString(progressText, textStartX, textYBottom, textScale * 0.8f, false);
        }

        if (!notUnlocked) {
            String lvlText = (isMaxed ? "§d" : "§a") + "LVL " + cData.level;
            float lvlWidth = mc.fontRendererObj.getStringWidth(lvlText) * textScale * 0.95f;
            StringDrawer.drawString(lvlText, x + w - pad - lvlWidth, textYTop, textScale * 0.95f, false);
        }
    }

    private ItemStack getSkull(String base64) {
        ItemStack skull = new ItemStack(Items.skull, 1, 3);
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagCompound skullOwner = new NBTTagCompound();
        skullOwner.setString("Id", java.util.UUID.randomUUID().toString());
        NBTTagCompound properties = new NBTTagCompound();
        NBTTagList textures = new NBTTagList();
        NBTTagCompound texture = new NBTTagCompound();
        texture.setString("Value", base64);
        textures.appendTag(texture);
        properties.setTag("textures", textures);
        skullOwner.setTag("Properties", properties);
        tag.setTag("SkullOwner", skullOwner);
        skull.setTagCompound(tag);
        return skull;
    }

    private ItemStack getCollectionItem(CollectionType type) {
        switch (type.name()) {
            case "CACTUS": return new ItemStack(Blocks.cactus);
            case "CARROT": return new ItemStack(Items.carrot);
            case "COCOA_BEANS": return new ItemStack(Items.dye, 1, 3);
            case "FEATHER": return new ItemStack(Items.feather);
            case "LEATHER": return new ItemStack(Items.leather);
            case "MELON": return new ItemStack(Items.melon);
            case "MUSHROOM": return new ItemStack(Blocks.red_mushroom);
            case "MUTTON": return new ItemStack(Items.mutton);
            case "NETHER_WART": return new ItemStack(Items.nether_wart);
            case "POTATO": return new ItemStack(Items.potato);
            case "PUMPKIN": return new ItemStack(Blocks.pumpkin);
            case "RAW_CHICKEN": return new ItemStack(Items.chicken);
            case "RAW_PORKCHOP": return new ItemStack(Items.porkchop);
            case "RAW_RABBIT": return new ItemStack(Items.rabbit);
            case "SEEDS": return new ItemStack(Items.wheat_seeds);
            case "SUGAR_CANE": return new ItemStack(Items.reeds);
            case "WHEAT": return new ItemStack(Items.wheat);
            case "COAL": return new ItemStack(Items.coal);
            case "COBBLESTONE": return new ItemStack(Blocks.cobblestone);
            case "DIAMOND": return new ItemStack(Items.diamond);
            case "EMERALD": return new ItemStack(Items.emerald);
            case "END_STONE": return new ItemStack(Blocks.end_stone);
            case "GLOWSTONE_DUST": return new ItemStack(Items.glowstone_dust);
            case "GOLD_INGOT": return new ItemStack(Items.gold_ingot);
            case "GRAVEL": return new ItemStack(Blocks.gravel);
            case "ICE": return new ItemStack(Blocks.ice);
            case "IRON_INGOT": return new ItemStack(Items.iron_ingot);
            case "LAPIS_LAZULI": return new ItemStack(Items.dye, 1, 4);
            case "MITHRIL": return new ItemStack(Items.prismarine_crystals);
            case "NETHERRACK": return new ItemStack(Blocks.netherrack);
            case "OBSIDIAN": return new ItemStack(Blocks.obsidian);
            case "QUARTZ": return new ItemStack(Items.quartz);
            case "REDSTONE": return new ItemStack(Items.redstone);
            case "SAND": return new ItemStack(Blocks.sand);
            case "HARD_STONE": return new ItemStack(Blocks.stone);
            case "BONE": return new ItemStack(Items.bone);
            case "ENDER_PEARL": return new ItemStack(Items.ender_pearl);
            case "GHAST_TEAR": return new ItemStack(Items.ghast_tear);
            case "GUNPOWDER": return new ItemStack(Items.gunpowder);
            case "MAGMA_CREAM": return new ItemStack(Items.magma_cream);
            case "ROTTEN_FLESH": return new ItemStack(Items.rotten_flesh);
            case "SLIME_BALL": return new ItemStack(Items.slime_ball);
            case "SPIDER_EYE": return new ItemStack(Items.spider_eye);
            case "STRING": return new ItemStack(Items.string);
            case "ACACIA_WOOD": return new ItemStack(Blocks.log2, 1, 0);
            case "BIRCH_WOOD": return new ItemStack(Blocks.log, 1, 2);
            case "DARK_OAK_WOOD": return new ItemStack(Blocks.log2, 1, 1);
            case "JUNGLE_WOOD": return new ItemStack(Blocks.log, 1, 3);
            case "OAK_WOOD": return new ItemStack(Blocks.log, 1, 0);
            case "SPRUCE_WOOD": return new ItemStack(Blocks.log, 1, 1);
            case "CLAY": return new ItemStack(Items.clay_ball);
            case "CLOWNFISH": return new ItemStack(Items.fish, 1, 2);
            case "INK_SACK": return new ItemStack(Items.dye, 1, 0);
            case "LILY_PAD": return new ItemStack(Blocks.waterlily);
            case "PRISMARINE_CRYSTALS": return new ItemStack(Items.prismarine_crystals);
            case "PRISMARINE_SHARD": return new ItemStack(Items.prismarine_shard);
            case "PUFFERFISH": return new ItemStack(Items.fish, 1, 3);
            case "RAW_FISH": return new ItemStack(Items.fish, 1, 0);
            case "RAW_SALMON": return new ItemStack(Items.fish, 1, 1);
            case "SPONGE": return new ItemStack(Blocks.sponge);
            case "MAGMAFISH": return new ItemStack(Items.magma_cream);
            case "BONZO": return getSkull("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzhhYjVmZjU1NDk4NWExNzc2MzhiZTY1MTFlOTU1YWJjYzVmMzg1NDUxMjNiYWY5NDNlZmRlZmE3ODNmMzVlZiJ9fX0=");
            case "SCARF": return getSkull("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGJmZjM2YTk1YThmMjQ2OGE5ZWVmZDA2OTY0NGZjODVkNzg0ODNlNGFhM2Y1MDhlODc3NTM5NDdmMzZmNmU4OCJ9fX0=");
            case "PROFESSOR": return new ItemStack(Items.skull, 1, 3);
            case "THORN": return new ItemStack(Items.skull, 1, 3);
            case "LIVID": return new ItemStack(Items.skull, 1, 3);
            case "SADAN": return new ItemStack(Items.skull, 1, 3);
            case "NECRON": return new ItemStack(Items.skull, 1, 3);
            default: return new ItemStack(Items.paper);
        }
    }

    private void drawCategoryDropdown(Minecraft mc, float x, float y, float w, float textScale) {
        CollectionBase[] categories = CollectionBase.values();
        itemHeight = ProfileViewerGUI.getScaledF(20);
        dropX = x + ProfileViewerGUI.getScaledF(8);
        dropY = y;
        dropW = w;
        dropH = itemHeight * categories.length;

        NineSliceUtils.draw(ProfileViewerGUI.CONTAINER_BG, (int) dropX, (int) dropY, (int) dropW, (int) dropH, 6, 18);

        int mouseX = Mouse.getX() * mc.currentScreen.width / mc.displayWidth;
        int mouseY = mc.currentScreen.height - (Mouse.getY() * mc.currentScreen.height / mc.displayHeight) - 1;

        for (int i = 0; i < categories.length; i++) {
            CollectionBase base = categories[i];
            float itemY = dropY + (i * itemHeight);

            boolean isHovered = mouseX >= dropX && mouseX <= dropX + dropW &&
                    mouseY >= itemY && mouseY <= itemY + itemHeight;

            if (isHovered) {
                Gui.drawRect((int) dropX + 4, (int) itemY, (int) (dropX + dropW - 4), (int) (itemY + itemHeight), 0x30FFFFFF);
            }

            float centerX = dropX + (dropW / 2.0f);
            float centerY = itemY + (itemHeight / 2.0f);
            String prefix = (base == currentCategory) ? "§a> §f" : "§7";
            StringDrawer.drawCenteredString(prefix + formatEnumName(base.name()), centerX, centerY, textScale * 0.85f, false);
        }
    }

    private void handleInputEvents(Minecraft mc, float xPos, float yPos, int width, int height, float maxScroll) {
        int mouseX = Mouse.getX() * mc.currentScreen.width / mc.displayWidth;
        int mouseY = mc.currentScreen.height - (Mouse.getY() * mc.currentScreen.height / mc.displayHeight) - 1;

        boolean isMouseDown = Mouse.isButtonDown(0);
        boolean isLeftClick = isMouseDown && !wasMouseDown;
        wasMouseDown = isMouseDown;

        if (isLeftClick) {
            if (isDropdownOpen) {
                if (mouseX >= dropX && mouseX <= dropX + dropW && mouseY >= dropY && mouseY <= dropY + dropH) {
                    int rowClicked = (int) ((mouseY - dropY) / itemHeight);
                    if (rowClicked >= 0 && rowClicked < CollectionBase.values().length) {
                        currentCategory = CollectionBase.values()[rowClicked];
                        scrollTarget = 0;
                        scrollY = 0;
                    }
                }
                isDropdownOpen = false;
            } else {
                float pad = ProfileViewerGUI.getScaledF(8);
                float btnW = ProfileViewerGUI.getScaledF(140);
                float btnH = ProfileViewerGUI.getScaledF(24);
                if (mouseX >= xPos + pad && mouseX <= xPos + pad + btnW && mouseY >= yPos + pad && mouseY <= yPos + pad + btnH) {
                    isDropdownOpen = true;
                }
            }
        }

        int dWheel = Mouse.getDWheel();
        if (dWheel != 0 && !isDropdownOpen) {
            if (mouseX >= xPos && mouseX <= xPos + width && mouseY >= yPos && mouseY <= yPos + height) {
                float scrollStep = ProfileViewerGUI.getScaledF(45);
                if (dWheel > 0) scrollTarget -= scrollStep;
                else scrollTarget += scrollStep;
            }
        }

        if (scrollTarget < 0) scrollTarget = 0;
        if (scrollTarget > maxScroll) scrollTarget = maxScroll;
    }

    private int getCategoryColor(CollectionBase base) {
        switch (base) {
            case FARMING: return new Color(255, 215, 0).getRGB();
            case MINING: return new Color(0, 191, 255).getRGB();
            case COMBAT: return new Color(220, 20, 60).getRGB();
            case FORAGING: return new Color(34, 139, 34).getRGB();
            case FISHING: return new Color(0, 250, 154).getRGB();
            case BOSS: return new Color(148, 0, 211).getRGB();
            default: return -1;
        }
    }

    private String formatEnumName(String name) {
        if (name == null || name.isEmpty()) return "";
        return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
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
        int segments = 60;
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