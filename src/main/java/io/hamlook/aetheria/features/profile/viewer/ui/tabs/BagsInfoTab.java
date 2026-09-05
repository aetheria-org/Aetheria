package io.hamlook.aetheria.features.profile.viewer.ui.tabs;

import io.hamlook.aetheria.Resources;
import io.hamlook.aetheria.features.misc.itemList.ItemRegistry;
import io.hamlook.aetheria.features.misc.itemList.SkyblockItem;
import io.hamlook.aetheria.features.profile.data.ItemData;
import io.hamlook.aetheria.features.profile.data.ProfileData;
import io.hamlook.aetheria.features.profile.data.bags.AccessoryData;
import io.hamlook.aetheria.features.profile.data.bags.FishingData;
import io.hamlook.aetheria.features.profile.data.bags.QuiverData;
import io.hamlook.aetheria.features.profile.data.bags.vars.Arrow;
import io.hamlook.aetheria.features.profile.data.bags.vars.Bait;
import io.hamlook.aetheria.features.profile.viewer.ui.ProfileViewerGUI;
import io.hamlook.aetheria.utils.KeybindHelper;
import io.hamlook.aetheria.utils.compat.GlStateManagerCompat;
import io.hamlook.aetheria.utils.compat.GuiScreenUtils;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.MouseCompat;
import io.hamlook.aetheria.utils.render.ItemRenderUtils;
import io.hamlook.aetheria.utils.render.TextRenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import io.hamlook.aetheria.utils.compat.RenderHelperCompat;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BagsInfoTab extends Tab {

    public static float scrollSpeed = 0.15f;
    private float accScrollY = 0, accScrollTarget = 0;

    public BagsInfoTab() {
        super(8, "Bags");
    }

    @Override
    public void draw(float xPos, float yPos, int width, int height, ProfileData data, Minecraft mc) {
        float pad = ProfileViewerGUI.getScaledF(4);
        float topBarH = ProfileViewerGUI.getScaledF(24);
        float sideWidth = width * 0.45f;
        float mainWidth = width * 0.5f;

        ItemData hoveredItem = null;

        if (data != null && data.bagsData != null) {
            hoveredItem = drawAccessoryBag(mc, xPos, yPos + topBarH, mainWidth, height - topBarH, data.bagsData.accessoryData, pad);
            drawSideBags(mc, xPos + mainWidth + pad * 4, yPos + topBarH, sideWidth, height - topBarH, data.bagsData.fishingData, data.bagsData.quiverData, pad);
        } else {
            TextRenderUtils.drawCenteredStringScaleAware("§cNo Bags Data Found!", xPos + (width / 2f), yPos + (height / 2f), ProfileViewerGUI.getScaleText(), false);
        }

        GlStateManagerCompat.color(1.0F, 1.0F, 1.0F, 1.0F);
        RenderHelperCompat.disableStandardItemLighting();

        if (hoveredItem != null) {
            int[] mouse = KeybindHelper.getMouseCoords(MinecraftCompat.getCurrentScreen().width, MinecraftCompat.getCurrentScreen().height);
            int mouseX = mouse[0], mouseY = mouse[1];
            drawItemTooltip(mc, hoveredItem, mouseX, mouseY);
        }
    }

    private ItemData drawAccessoryBag(Minecraft mc, float x, float y, float w, float h, AccessoryData accData, float pad) {
        String mpText = "§eAccessories";
        if (accData != null) mpText += " §7(MP: §b" + accData.magicalPower + "§7)";
        
        float titleY = y - ProfileViewerGUI.getScaledF(16);
        TextRenderUtils.drawStringScaleAware(mpText, x, titleY, ProfileViewerGUI.getScaleText(), false);
        Gui.drawRect((int)x, (int)(titleY + ProfileViewerGUI.getScaledF(12)), (int)(x + w), (int)(titleY + ProfileViewerGUI.getScaledF(13)), 0xFF555555);

        float gridY = y + ProfileViewerGUI.getScaledF(4);
        float gridH = h - ProfileViewerGUI.getScaledF(4);

        if (accData == null || accData.accessories == null) return null;

        int cols = 7;
        float slotSize = (w - pad * (cols - 1)) / cols;
        int rows = (int) Math.ceil(accData.accessories.size() / (double)cols);
        float totalGridH = rows * (slotSize + pad);

        handleVerticalScroll(x, gridY, w, gridH, totalGridH);
        accScrollY += (accScrollTarget - accScrollY) * scrollSpeed;

        applyScissor(x, gridY, w, gridH);
        GlStateManagerCompat.pushMatrix();
        GlStateManagerCompat.translate(0, -accScrollY, 0);

        int index = 0;
        int[] mouse = KeybindHelper.getMouseCoords(MinecraftCompat.getCurrentScreen().width, MinecraftCompat.getCurrentScreen().height);
        int mouseX = mouse[0], mouseY = mouse[1];
        ItemData hoveredAcc = null;

        List<ItemData> sortedAccs = new ArrayList<>(accData.accessories);
        sortedAccs.sort((a, b) -> Integer.compare(parseRarity(b.displayName), parseRarity(a.displayName)));

        for (ItemData acc : sortedAccs) {
            float cX = x + (index % cols) * (slotSize + pad);
            float cY = gridY + (int)(index / cols) * (slotSize + pad);

            ItemStack stack = itemToStack(acc);
            int rarityScore = parseRarity(acc.displayName);
            
            drawSlot(mc, stack, false, rarityScore, cX, cY, slotSize);

            float absY = cY - accScrollY;
            if (mouseX >= cX && mouseX <= cX + slotSize && mouseY >= absY && mouseY <= absY + slotSize) {
                if (mouseY >= gridY && mouseY <= gridY + gridH) {
                    hoveredAcc = acc;
                }
            }
            index++;
        }
        GlStateManagerCompat.popMatrix();
        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        return hoveredAcc;
    }

    private void drawSideBags(Minecraft mc, float x, float y, float w, float h, FishingData fishData, QuiverData quiverData, float pad) {
        float halfH = (h - pad * 2) / 2f;
        
        float fishTitleY = y - ProfileViewerGUI.getScaledF(16);
        TextRenderUtils.drawStringScaleAware("§eFishing Bag", x, fishTitleY, ProfileViewerGUI.getScaleText(), false);
        Gui.drawRect((int)x, (int)(fishTitleY + ProfileViewerGUI.getScaledF(12)), (int)(x + w), (int)(fishTitleY + ProfileViewerGUI.getScaledF(13)), 0xFF555555);

        if (fishData != null && fishData.baits != null) {
            float curY = y + ProfileViewerGUI.getScaledF(4);
            for (Map.Entry<Bait, Integer> entry : fishData.baits.entrySet()) {
                if (entry.getValue() > 0 && !entry.getKey().itemName.isEmpty()) {
                    float iconSize = ProfileViewerGUI.getScaledF(24);
                    String id = entry.getKey().itemName.replace(" ", "_").toUpperCase();
                    SkyblockItem sbItem = ItemRegistry.getItem(id);
                    if (sbItem != null && sbItem.itemStack != null) {
                        ItemRenderUtils.renderItemIcon(mc, sbItem.itemStack, (int)x, (int)curY, (int)iconSize);
                    } else {
                        ItemRenderUtils.renderItemIcon(mc, new ItemStack(Items.dye, 1, 0), (int)x, (int)curY, (int)iconSize);
                    }
                    TextRenderUtils.drawStringScaleAware("§f" + entry.getValue() + "x §7" + entry.getKey().itemName, x + iconSize + pad, curY + (iconSize / 2f) - ProfileViewerGUI.getScaledF(4), ProfileViewerGUI.getScaleText() * 0.8f, false);
                    curY += iconSize + ProfileViewerGUI.getScaledF(4);
                }
            }
        }

        float quiverY = y + halfH + pad;
        float quiverTitleY = quiverY - ProfileViewerGUI.getScaledF(16);
        TextRenderUtils.drawStringScaleAware("§eQuiver", x, quiverTitleY, ProfileViewerGUI.getScaleText(), false);
        Gui.drawRect((int)x, (int)(quiverTitleY + ProfileViewerGUI.getScaledF(12)), (int)(x + w), (int)(quiverTitleY + ProfileViewerGUI.getScaledF(13)), 0xFF555555);

        if (quiverData != null && quiverData.arrows != null) {
            float curY = quiverY + ProfileViewerGUI.getScaledF(4);
            for (Map.Entry<Arrow, Integer> entry : quiverData.arrows.entrySet()) {
                if (entry.getValue() > 0) {
                    float iconSize = ProfileViewerGUI.getScaledF(24);
                    String id = entry.getKey().itemName.replace(" ", "_").toUpperCase();
                    SkyblockItem sbItem = ItemRegistry.getItem(id);
                    if (sbItem != null && sbItem.itemStack != null) {
                        ItemRenderUtils.renderItemIcon(mc, sbItem.itemStack, (int)x, (int)curY, (int)iconSize);
                    } else {
                        ItemRenderUtils.renderItemIcon(mc, new ItemStack(Items.arrow), (int)x, (int)curY, (int)iconSize);
                    }
                    TextRenderUtils.drawStringScaleAware("§f" + entry.getValue() + "x §7" + entry.getKey().itemName, x + iconSize + pad, curY + (iconSize / 2f) - ProfileViewerGUI.getScaledF(4), ProfileViewerGUI.getScaleText() * 0.8f, false);
                    curY += iconSize + ProfileViewerGUI.getScaledF(4);
                }
            }
        }
    }

    private void drawSlot(Minecraft mc, ItemStack icon, boolean active, int rarityScore, float x, float y, float size) {
        GlStateManagerCompat.color(1, 1, 1, 1);
        mc.getTextureManager().bindTexture(Resources.storageSlot(1));
        Gui.drawModalRectWithCustomSizedTexture((int)x, (int)y, 0, 0, (int)size, (int)size, (int)size, (int)size);

        if (icon != null) {
            ItemRenderUtils.renderItemIcon(mc, icon, (int)(x + size*0.1f), (int)(y + size*0.1f), (int)(size*0.8f));
            int squareSize = (int) Math.max(3, size * 0.18f);
            Gui.drawRect((int)(x + size - squareSize - 1), (int)(y + size - squareSize - 1), (int)(x + size - 1), (int)(y + size - 1), getRarityHex(rarityScore));
        }
    }

    private ItemStack itemToStack(ItemData data) {
        if (data == null || data.skyblockID == null) return null;
        SkyblockItem sbItem = ItemRegistry.getWithItemData(data);
        if (sbItem != null && sbItem.itemStack != null) {
            return sbItem.itemStack;
        }
        return new ItemStack(Items.skull, 1, 3);
    }

    private void handleVerticalScroll(float x, float y, float w, float h, float contentH) {
        int[] mouse = KeybindHelper.getMouseCoords(MinecraftCompat.getCurrentScreen().width, MinecraftCompat.getCurrentScreen().height);
        int mx = mouse[0], my = mouse[1];

        if (mx >= x && mx <= x + w && my >= y && my <= y + h) {
            int dWheel = MouseCompat.getDWheel();
            if (dWheel != 0) {
                float step = 50f;
                accScrollTarget = Math.max(0, Math.min(accScrollTarget + (dWheel > 0 ? -step : step), Math.max(0, contentH - h)));
            }
        }
    }

    private void applyScissor(float x, float y, float w, float h) {
        ScaledResolution res = GuiScreenUtils.getScaledResolution();
        int f = res.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((int)(x * f), (int)(GuiScreenUtils.getDisplayHeight() - (y + h) * f), (int)(w * f), (int)(h * f));
    }

    private void drawItemTooltip(Minecraft mc, ItemData data, int mouseX, int mouseY) {
        if (data == null) return;
        List<String> lines = new ArrayList<>();
        lines.add(data.displayName);
        if (data.lore != null) lines.addAll(data.lore);

        lines.add("");
        lines.add("§8Item SB_ID: §7" + data.skyblockID);
        lines.add("§8ItemRegistry SB_ID: §7" + io.hamlook.aetheria.features.misc.itemList.ItemResolver.resolveId(data.skyblockID, data.displayName));

        if (MinecraftCompat.getCurrentScreen() instanceof ProfileViewerGUI) {
            ((ProfileViewerGUI) MinecraftCompat.getCurrentScreen()).drawTooltip(lines, mouseX, mouseY);
        }
    }

    private int parseRarity(String displayName) {
        if (displayName == null) return 1;
        String lastColor = "";
        for (int i = 0; i < displayName.length() - 1; i++) {
            if (displayName.charAt(i) == '§' || displayName.charAt(i) == '&') {
                char c = Character.toLowerCase(displayName.charAt(i + 1));
                if ("0123456789abcdef".indexOf(c) != -1) lastColor = String.valueOf(c);
            }
        }
        switch (lastColor) {
            case "c": return 8;
            case "b": return 7;
            case "d": return 6;
            case "6": return 5;
            case "5": return 4;
            case "9": return 3;
            case "a": return 2;
            default: return 1;
        }
    }

    private int getRarityHex(int score) {
        switch (score) {
            case 8: return 0xFFFF5555; case 7: return 0xFF55FFFF;
            case 6: return 0xFFFF55FF; case 5: return 0xFFFFAA00;
            case 4: return 0xFFAA00AA; case 3: return 0xFF5555FF;
            case 2: return 0xFF55FF55; default: return 0xFFFFFFFF;
        }
    }
}
