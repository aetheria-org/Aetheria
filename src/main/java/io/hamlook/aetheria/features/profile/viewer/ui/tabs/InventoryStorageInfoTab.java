package io.hamlook.aetheria.features.profile.viewer.ui.tabs;

import io.hamlook.aetheria.Resources;
import io.hamlook.aetheria.features.misc.itemList.ItemRegistry;
import io.hamlook.aetheria.features.misc.itemList.SkyblockItem;
import io.hamlook.aetheria.features.profile.data.ItemData;
import io.hamlook.aetheria.features.profile.data.ProfileData;
import io.hamlook.aetheria.features.profile.data.storage.ContainerData;
import io.hamlook.aetheria.features.profile.vars.EquipmentSlot;
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
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class InventoryStorageInfoTab extends Tab {

    private int selectedContainerIndex = -1;

    private float storageScrollTarget = 0;

    private float backBtnX, backBtnY, backBtnW, backBtnH;
    private float prevBtnX, prevBtnY, prevBtnW, prevBtnH;
    private float nextBtnX, nextBtnY, nextBtnW, nextBtnH;

    private final List<ContainerClickArea> containerClickAreas = new ArrayList<>();

    public InventoryStorageInfoTab() {
        super(1, "Inv & Storage");
    }

    @Override
    public void draw(float xPos, float yPos, int width, int height, ProfileData data, Minecraft mc) {
        float pad = ProfileViewerGUI.getScaledF(6);
        float gap = width * 0.04f;
        float leftWidth = width * 0.43f;
        float rightWidth = width - leftWidth - gap;

        ItemData hoveredItem = null;
        containerClickAreas.clear();

        // 1. Draw Inventory Section (Left)
        ItemData invHovered = drawInventory(mc, xPos, yPos, leftWidth, height, data, pad);
        if (invHovered != null) hoveredItem = invHovered;

        // Vertical Separator
        float sepX = xPos + leftWidth + gap / 2;
        float titleTop = yPos - ProfileViewerGUI.getScaledF(13);
        Gui.drawRect((int)sepX, (int)titleTop, (int)sepX + 1, (int)(yPos + height - pad), 0xFF555555);

        // 2. Draw Storage Section (Right)
        ItemData storageHovered = drawStorage(mc, xPos + leftWidth + gap, yPos, rightWidth, height, data, pad);
        if (storageHovered != null) hoveredItem = storageHovered;

        // Cleanup GL state just in case
        GlStateManagerCompat.color(1.0F, 1.0F, 1.0F, 1.0F);
        RenderHelperCompat.disableStandardItemLighting();

        // 3. Draw Tooltip at very top
        if (hoveredItem != null) {
            int[] mouse = KeybindHelper.getMouseCoords(mc.currentScreen.width, mc.currentScreen.height);
            int mouseX = mouse[0], mouseY = mouse[1];
            drawItemTooltip(mc, hoveredItem, mouseX, mouseY);
        }
    }

    private ItemData drawInventory(Minecraft mc, float x, float y, float w, float h, ProfileData data, float pad) {
        ItemData hoveredItem = null;
        float titleY = y - ProfileViewerGUI.getScaledF(16);
        TextRenderUtils.drawStringScaleAware("§eInventory", x + ProfileViewerGUI.getScaledF(6), titleY, ProfileViewerGUI.getScaleText(), false);
        Gui.drawRect((int)x, (int)(titleY + ProfileViewerGUI.getScaledF(12)), (int)(x + w), (int)(titleY + ProfileViewerGUI.getScaledF(13)), 0xFF555555);

        if (data == null || data.inventoryData == null) return null;
        float gridY = y + ProfileViewerGUI.getScaledF(8);
        float invSlotSize = w / 9;
        float hotbarGap = ProfileViewerGUI.getScaledF(4);

        float totalInvH = 4 * invSlotSize + hotbarGap * 2 + 4 * invSlotSize + hotbarGap;
        float availableHeight = h - ProfileViewerGUI.getScaledF(8);
        float startY = gridY + Math.max(0, (availableHeight - totalInvH) / 2);

        // Armor and Equipment 4x2 grid above inventory
        EquipmentSlot[] equipSlots = {EquipmentSlot.NECKLACE, EquipmentSlot.CLOAK, EquipmentSlot.BELT, EquipmentSlot.GLOVES};
        EquipmentSlot[] armorSlots = {EquipmentSlot.HELMET, EquipmentSlot.CHESTPLATE, EquipmentSlot.LEGGINGS, EquipmentSlot.BOOTS};

        float equipStartX = x + (w / 2f) - invSlotSize;
        float armorStartX = x + (w / 2f);

        for (int i = 0; i < 4; i++) {
            // Equipment (left column)
            float eY = startY + i * invSlotSize;
            ItemData eData = data.inventoryData.armorData != null ? data.inventoryData.armorData.get(equipSlots[i]) : null;
            drawSlot(mc, itemToStack(eData), eData != null ? parseRarity(eData.displayName) : 1, equipStartX, eY, invSlotSize);
            if (isMouseOver(equipStartX, eY, invSlotSize, invSlotSize) && eData != null) hoveredItem = eData;

            // Armor (right column)
            float aY = startY + i * invSlotSize;
            ItemData aData = data.inventoryData.armorData != null ? data.inventoryData.armorData.get(armorSlots[i]) : null;
            drawSlot(mc, itemToStack(aData), aData != null ? parseRarity(aData.displayName) : 1, armorStartX, aY, invSlotSize);
            if (isMouseOver(armorStartX, aY, invSlotSize, invSlotSize) && aData != null) hoveredItem = aData;
        }

        float invStartY = startY + 4 * invSlotSize + hotbarGap * 2;

        // Draw 36 inventory slots (cols 0-8)
        for (int i = 0; i < 36; i++) {
            int renderRow = (i < 9) ? 3 : ((i - 9) / 9);
            int renderCol = (i < 9) ? i : ((i - 9) % 9);

            float cX = x + renderCol * invSlotSize;
            float cY = invStartY + renderRow * invSlotSize;

            if (renderRow == 3) {
                cY += hotbarGap; // Extra separation for hotbar
            }

            ItemData iData = data.inventoryData.invData != null ? data.inventoryData.invData.get(i) : null;
            drawSlot(mc, itemToStack(iData), iData != null ? parseRarity(iData.displayName) : 1, cX, cY, invSlotSize);

            if (isMouseOver(cX, cY, invSlotSize, invSlotSize) && iData != null) {
                hoveredItem = iData;
            }
        }

        return hoveredItem;
    }

    private ItemData drawStorage(Minecraft mc, float x, float y, float w, float h, ProfileData data, float pad) {
        String title = selectedContainerIndex == -1 ? "§eStorage" : "§eContainer Viewer";
        float titleY = y - ProfileViewerGUI.getScaledF(16);
        TextRenderUtils.drawStringScaleAware(title, x, titleY, ProfileViewerGUI.getScaleText(), false);
        Gui.drawRect((int)x, (int)(titleY + ProfileViewerGUI.getScaledF(12)), (int)(x + w), (int)(titleY + ProfileViewerGUI.getScaledF(13)), 0xFF555555);

        float contentY = y + ProfileViewerGUI.getScaledF(8);
        float contentH = h - ProfileViewerGUI.getScaledF(8);

        if (data == null || data.storageData == null || data.storageData.containers == null || data.storageData.containers.isEmpty()) {
            TextRenderUtils.drawCenteredStringScaleAware("§cNo Storage Data Found", x + w/2, y + h/2, ProfileViewerGUI.getScaleText(), false);
            return null;
        }

        if (selectedContainerIndex == -1) {
            // Draw Grid View (9x4 Hypixel Storage Layout)
            int cols = 9;
            float slotSize = w / cols;

            ItemData hovered = null;
            for (int i = 0; i < 36; i++) {
                int col = i % cols;
                int row = i / cols;

                float cX = x + col * slotSize;
                float cY = contentY + row * slotSize;

                ContainerData found = null;
                int foundIndex = -1;
                for (int c = 0; c < data.storageData.containers.size(); c++) {
                    ContainerData container = data.storageData.containers.get(c);
                    if (i <= 8 && container.containerID.equals("echest-" + i)) {
                        found = container;
                        foundIndex = c;
                        break;
                    } else if (i >= 18 && container.containerID.equals("bag-" + (i - 18))) {
                        found = container;
                        foundIndex = c;
                        break;
                    }
                }

                if (found != null) {
                    ItemStack icon = getContainerIcon(found.containerID);
                    drawSlot(mc, icon, 1, cX, cY, slotSize);

                    containerClickAreas.add(new ContainerClickArea(cX, cY, slotSize, foundIndex, contentY, contentY + contentH));

                    if (isMouseOver(cX, cY, slotSize, slotSize)) {
                        ItemData dummy = new ItemData();
                        dummy.displayName = "§a" + formatContainerName(found.containerID);
                        List<String> lore = new ArrayList<>();
                        lore.add("§7Click to view contents!");
                        dummy.lore = lore;
                        hovered = dummy;
                    }
                } else {
                    drawSlot(mc, null, 1, cX, cY, slotSize);
                }
            }
            return hovered;

        } else {
            // Draw Container View
            selectedContainerIndex = Math.min(selectedContainerIndex,data.storageData.containers.size() - 1);
            ContainerData container = data.storageData.containers.get(selectedContainerIndex);
            float btnH = ProfileViewerGUI.getScaledF(16);
            float btnW = ProfileViewerGUI.getScaledF(40);
            
            backBtnX = x; backBtnY = contentY; backBtnW = btnW; backBtnH = btnH;
            prevBtnX = x + w - btnW*2 - pad*2; prevBtnY = contentY; prevBtnW = btnW; prevBtnH = btnH;
            nextBtnX = x + w - btnW; nextBtnY = contentY; nextBtnW = btnW; nextBtnH = btnH;

            drawButton("§c<- Back", backBtnX, backBtnY, backBtnW, backBtnH);
            
            if (selectedContainerIndex > 0) {
                drawButton("§ePrev", prevBtnX, prevBtnY, prevBtnW, prevBtnH);
            }
            if (selectedContainerIndex < data.storageData.containers.size() - 1) {
                drawButton("§eNext", nextBtnX, nextBtnY, nextBtnW, nextBtnH);
            }

            TextRenderUtils.drawCenteredStringScaleAware("§6" + formatContainerName(container.containerID), x + w/2, contentY + btnH/2, ProfileViewerGUI.getScaleText(), false);

            float gridY = contentY + btnH + pad * 2;
            int cols = 9;
            float slotSize = w / cols;
            ItemData hovered = null;

            for (int i = 0; i < 54; i++) {
                int col = i % cols;
                int row = i / cols;
                float cX = x + col * slotSize;
                float cY = gridY + row * slotSize;

                ItemData iData = container.data != null ? container.data.get(i) : null;
                drawSlot(mc, itemToStack(iData), iData != null ? parseRarity(iData.displayName) : 1, cX, cY, slotSize);

                if (isMouseOver(cX, cY, slotSize, slotSize) && iData != null) {
                    hovered = iData;
                }
            }
            return hovered;
        }
    }

    private void drawButton(String text, float x, float y, float w, float h) {
        boolean hover = isMouseOver(x, y, w, h);
        int color = hover ? 0xFF666666 : 0xFF444444;
        Gui.drawRect((int)x, (int)y, (int)(x + w), (int)(y + h), color);
        TextRenderUtils.drawCenteredStringScaleAware(text, x + w/2, y + h/2, ProfileViewerGUI.getScaleText() * 0.8f, false);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) return;

        if (selectedContainerIndex == -1) {
            for (ContainerClickArea area : containerClickAreas) {
                if (mouseX >= area.x && mouseX <= area.x + area.size && mouseY >= area.y && mouseY <= area.y + area.size) {
                    if (mouseY >= area.minY && mouseY <= area.maxY) {
                        selectedContainerIndex = area.index;
                        break;
                    }
                }
            }
        } else {
            if (mouseX >= backBtnX && mouseX <= backBtnX + backBtnW && mouseY >= backBtnY && mouseY <= backBtnY + backBtnH) {
                selectedContainerIndex = -1;
            } else if (selectedContainerIndex > 0 && mouseX >= prevBtnX && mouseX <= prevBtnX + prevBtnW && mouseY >= prevBtnY && mouseY <= prevBtnY + prevBtnH ) {
                selectedContainerIndex--;
            } else if (mouseX >= nextBtnX && mouseX <= nextBtnX + nextBtnW && mouseY >= nextBtnY && mouseY <= nextBtnY + nextBtnH) {
                // assume caller checked bounds
                // But we must enforce it here just in case
                // We'll trust the button bound logic
                selectedContainerIndex++;
            }
        }
    }

    private int getMouseY() {
        return KeybindHelper.getMouseCoords(MinecraftCompat.getMinecraft().currentScreen.width, MinecraftCompat.getMinecraft().currentScreen.height)[1];
    }

    private boolean isMouseOver(float x, float y, float w, float h) {
        int[] mouse = KeybindHelper.getMouseCoords(MinecraftCompat.getMinecraft().currentScreen.width, MinecraftCompat.getMinecraft().currentScreen.height);
        int mouseX = mouse[0], mouseY = mouse[1];
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    private void handleVerticalScroll(float x, float y, float w, float h, float contentH) {
        int[] mouse = KeybindHelper.getMouseCoords(MinecraftCompat.getMinecraft().currentScreen.width, MinecraftCompat.getMinecraft().currentScreen.height);
        int mx = mouse[0], my = mouse[1];

        if (mx >= x && mx <= x + w && my >= y && my <= y + h) {
            int dWheel = MouseCompat.getDWheel();
            if (dWheel != 0) {
                float step = 30f;
                storageScrollTarget = Math.max(0, Math.min(storageScrollTarget + (dWheel > 0 ? -step : step), Math.max(0, contentH - h)));
            }
        }
    }

    private void applyScissor(float x, float y, float w, float h) {
        ScaledResolution res = GuiScreenUtils.getScaledResolution();
        int f = res.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((int)(x * f), (int)(MinecraftCompat.getMinecraft().displayHeight - (y + h) * f), (int)(w * f), (int)(h * f));
    }

    private void drawSlot(Minecraft mc, ItemStack icon, int rarityScore, float x, float y, float size) {
        GlStateManagerCompat.color(1, 1, 1, 1);
        mc.getTextureManager().bindTexture(Resources.storageSlot(1));
        Gui.drawModalRectWithCustomSizedTexture((int)x, (int)y, 0, 0, (int)size, (int)size, (int)size, (int)size);

        if (icon != null) {
            int color = getRarityHex(rarityScore);
            int b = 1;
            Gui.drawRect((int)x + 1, (int)y + 1, (int)(x + size) - 1, (int)y + 1 + b, color);
            Gui.drawRect((int)x + 1, (int)(y + size) - 1 - b, (int)(x + size) - 1, (int)(y + size) - 1, color);
            Gui.drawRect((int)x + 1, (int)y + 1, (int)x + 1 + b, (int)(y + size) - 1, color);
            Gui.drawRect((int)(x + size) - 1 - b, (int)y + 1, (int)(x + size) - 1, (int)(y + size) - 1, color);

            ItemRenderUtils.renderItemIcon(mc, icon, (int)(x + size*0.15f), (int)(y + size*0.15f), (int)(size*0.7f));
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

    private int parseRarity(String displayName) {
        if (displayName == null) return 1;
        String lastColor = "f";
        for (int i = 0; i < displayName.length() - 1; i++) {
            if (displayName.charAt(i) == '§' || displayName.charAt(i) == '&') {
                char c = Character.toLowerCase(displayName.charAt(i + 1));
                if ("0123456789abcdef".indexOf(c) != -1) lastColor = String.valueOf(c);
            }
        }
        switch (lastColor) {
            case "c": return 8; case "b": return 7; case "d": return 6;
            case "6": return 5; case "5": return 4; case "9": return 3;
            case "a": return 2; default: return 1;
        }
    }

    private ItemStack itemToStack(ItemData data) {
        if (data == null || data.skyblockID == null) return null;
        SkyblockItem sbItem = ItemRegistry.getWithItemData(data);
        if (sbItem != null && sbItem.itemStack != null) {
            return sbItem.itemStack;
        }
        // If ID contains backpack/enderchest, try that. But usually items have valid IDs.
        return new ItemStack(Items.skull, 1, 3);
    }

    private ItemStack getContainerIcon(String id) {
        if (id == null) return new ItemStack(Blocks.chest);
        if (id.toLowerCase().contains("echest")) {
            return new ItemStack(Blocks.ender_chest);
        } else {
            return new ItemStack(Blocks.chest); // Use chest icon for backpacks
        }
    }

    private String formatContainerName(String id) {
        if (id == null) return "Unknown";
        if (id.startsWith("echest-")) {
            int num = Integer.parseInt(id.replace("echest-", "")) + 1;
            return "Ender Chest " + num;
        } else if (id.startsWith("bag-")) {
            int num = Integer.parseInt(id.replace("bag-", "")) + 1;
            return "Backpack " + num;
        }
        
        String[] words = id.split("_");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.isEmpty()) continue;
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }

    private void drawItemTooltip(Minecraft mc, ItemData data, int mouseX, int mouseY) {
        if (data == null) return;
        List<String> lines = new ArrayList<>();
        lines.add(data.displayName);
        if (data.lore != null) lines.addAll(data.lore);
        
        lines.add("");
        lines.add("§8Item SB_ID: §7" + data.skyblockID);
        lines.add("§8ItemRegistry SB_ID: §7" + io.hamlook.aetheria.features.misc.itemList.ItemResolver.resolveId(data.skyblockID, data.displayName));

        if (mc.currentScreen instanceof ProfileViewerGUI) {
            ((ProfileViewerGUI) mc.currentScreen).drawTooltip(lines, mouseX, mouseY);
        }
    }

    private static class ContainerClickArea {
        float x, y, size, minY, maxY;
        int index;
        ContainerClickArea(float x, float y, float size, int index, float minY, float maxY) {
            this.x = x; this.y = y; this.size = size; this.index = index;
            this.minY = minY; this.maxY = maxY;
        }
    }
}
