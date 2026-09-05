package io.hamlook.aetheria.features.profile.viewer.ui.tabs;

import io.hamlook.aetheria.Resources;
import io.hamlook.aetheria.features.misc.itemList.ItemRegistry;
import io.hamlook.aetheria.features.misc.itemList.SkyblockItem;
import io.hamlook.aetheria.features.profile.data.ItemData;
import io.hamlook.aetheria.features.profile.data.ProfileData;
import io.hamlook.aetheria.features.profile.data.pets.Pet;
import io.hamlook.aetheria.features.profile.data.wardrobe.WardrobeSet;
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

public class ExtraWearableInfoTab extends Tab {

    public static float scrollSpeed = 0.15f;
    private float petScrollX = 0, petScrollTarget = 0;
    private float wardScrollX = 0, wardScrollTarget = 0;

    public ExtraWearableInfoTab() {
        super(6, "Pets & Wardrobe");
    }

    private static class RenderablePet {
        Pet raw;
        String debugRegistryID;
        boolean active;
        int rarityScore;
        ItemStack stack;

        RenderablePet(Pet raw) {
            this.raw = raw;
            this.active = raw.equipped;

            SkyblockItem sbItem = ItemRegistry.getWithItemData(raw.data);
            this.rarityScore = parseRarity(sbItem != null ? sbItem.rarity : null, raw.data.displayName);
            this.debugRegistryID = sbItem != null ? sbItem.skyblockID : "N/A";

            if (sbItem != null && sbItem.itemStack != null) {
                this.stack = sbItem.itemStack;
            } else {
                this.stack = new ItemStack(Items.skull, 1, 3);
            }
        }

        private int parseRarity(String registryRarity, String displayName) {
            if (registryRarity != null) {
                switch(registryRarity.toUpperCase()) {
                    case "MYTHIC": return 6;
                    case "LEGENDARY": return 5;
                    case "EPIC": return 4;
                    case "RARE": return 3;
                    case "UNCOMMON": return 2;
                    case "COMMON": return 1;
                }
            }

            // Fallback to parsing color codes if registry is missing rarity
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
    }

    @Override
    public void draw(float xPos, float yPos, int width, int height, ProfileData data, Minecraft mc) {
        float pad = ProfileViewerGUI.getScaledF(4);
        float topBarH = ProfileViewerGUI.getScaledF(24);
        float sectionH = (height - topBarH - (pad * 6)) / 2f;

        // --- 1. PARSE PETS VIA REGISTRY ---
        List<RenderablePet> parsedPets = new ArrayList<>();
        if (data != null && data.petsData != null && data.petsData.pets != null) {
            for (Pet p : data.petsData.pets.values()) {
                parsedPets.add(new RenderablePet(p));
            }
        }

        parsedPets.sort((a, b) -> {
            if (a.rarityScore != b.rarityScore) {
                return Integer.compare(b.rarityScore, a.rarityScore);
            }
            return Boolean.compare(b.active, a.active);
        });


        // --- 2. DRAW SECTIONS ---
        float petGridY = yPos + topBarH + pad * 4;
        RenderablePet hoveredPet = drawPetSection(mc, xPos, petGridY, width, sectionH, parsedPets, pad);

        float wardGridY = petGridY + sectionH + (pad * 6);
        ItemData hoveredWardrobe = drawWardrobeSection(mc, xPos, wardGridY, width, sectionH, data, pad);

        // --- 3. CLEANUP STATE ---
        GlStateManagerCompat.color(1.0F, 1.0F, 1.0F, 1.0F);
        RenderHelperCompat.disableStandardItemLighting();

        if (hoveredPet != null) {
            int[] m1 = KeybindHelper.getMouseCoords(mc.currentScreen.width, mc.currentScreen.height);
            drawPetTooltip(mc, hoveredPet, m1[0], m1[1]);
        } else if (hoveredWardrobe != null) {
            int[] m2 = KeybindHelper.getMouseCoords(mc.currentScreen.width, mc.currentScreen.height);
            drawItemTooltip(mc, hoveredWardrobe, m2[0], m2[1]);
        }
    }

    private RenderablePet drawPetSection(Minecraft mc, float x, float y, float w, float h, List<RenderablePet> pets, float pad) {
        int rows = 4;
        float slotSize = (h - pad * (rows - 1)) / rows;
        int cols = Math.max(1, (int) Math.ceil(pets.size() / (double)rows));
        float totalGridW = cols * (slotSize + pad);

        float offsetX = totalGridW < w ? (w - totalGridW) / 2f : 0;
        
        float headerY = y - ProfileViewerGUI.getScaledF(16);
        TextRenderUtils.drawStringScaleAware("§ePets", x, headerY, ProfileViewerGUI.getScaleText(), false);
        Gui.drawRect((int)x, (int)(headerY + ProfileViewerGUI.getScaledF(12)), (int)(x + w), (int)(headerY + ProfileViewerGUI.getScaledF(13)), 0xFF555555);

        handleHorizontalScroll(x, y, w, h, totalGridW, true);
        petScrollX += (petScrollTarget - petScrollX) * scrollSpeed;

        applyScissor(x, y, w, h);
        GlStateManagerCompat.pushMatrix();
        GlStateManagerCompat.translate(x + offsetX - petScrollX, 0, 0);

        int totalSlots = cols * rows;
        int[] mouse = KeybindHelper.getMouseCoords(mc.currentScreen.width, mc.currentScreen.height);
        int mouseX = mouse[0], mouseY = mouse[1];
        RenderablePet hoveredPet = null;

        for (int i = 0; i < totalSlots; i++) {
            int col = i % cols;
            int row = i / cols;
            
            float cX = col * (slotSize + pad);
            float cY = y + row * (slotSize + pad);

            if (i < pets.size()) {
                RenderablePet p = pets.get(i);
                drawSlot(mc, p.stack, p.active, p.rarityScore, cX, cY, slotSize, true);

                float absX = x + offsetX - petScrollX + cX;
                if (mouseX >= absX && mouseX <= absX + slotSize && mouseY >= cY && mouseY <= cY + slotSize) {
                    if (mouseX >= x && mouseX <= x + w) {
                        hoveredPet = p;
                    }
                }
            } else {
                drawSlot(mc, null, false, 1, cX, cY, slotSize, true);
            }
        }
        GlStateManagerCompat.popMatrix();
        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        return hoveredPet;
    }

    private ItemData drawWardrobeSection(Minecraft mc, float x, float y, float w, float h, ProfileData data, float pad) {
        if (data == null || data.wardrobeData == null || data.wardrobeData.wardrobe == null) return null;

        float slotSize = h / 4.5f;
        int cols = data.wardrobeData.wardrobe.size();
        float totalGridW = cols * (slotSize + pad);

        float offsetX = totalGridW < w ? (w - totalGridW) / 2f : 0;
        
        float headerY = y - ProfileViewerGUI.getScaledF(16);
        TextRenderUtils.drawStringScaleAware("§eWardrobe", x, headerY, ProfileViewerGUI.getScaleText(), false);
        Gui.drawRect((int)x, (int)(headerY + ProfileViewerGUI.getScaledF(12)), (int)(x + w), (int)(headerY + ProfileViewerGUI.getScaledF(13)), 0xFF555555);

        handleHorizontalScroll(x, y, w, h, totalGridW, false);
        wardScrollX += (wardScrollTarget - wardScrollX) * scrollSpeed;

        applyScissor(x, y, w, h);
        GlStateManagerCompat.pushMatrix();
        GlStateManagerCompat.translate(x + offsetX - wardScrollX, 0, 0);

        int index = 0;
        int[] mouse = KeybindHelper.getMouseCoords(mc.currentScreen.width, mc.currentScreen.height);
        int mouseX = mouse[0], mouseY = mouse[1];
        ItemData hoveredWardrobe = null;

        for (WardrobeSet set : data.wardrobeData.wardrobe.values()) {
            float cX = index * (slotSize + pad);
            drawSlot(mc, itemToStack(set.helmet), set.equipped, 1, cX, y, slotSize, false);
            drawSlot(mc, itemToStack(set.chestplate), set.equipped, 1, cX, y + slotSize + pad/2, slotSize, false);
            drawSlot(mc, itemToStack(set.leggings), set.equipped, 1, cX, y + (slotSize + pad/2)*2, slotSize, false);
            drawSlot(mc, itemToStack(set.boots), set.equipped, 1, cX, y + (slotSize + pad/2)*3, slotSize, false);

            if (mouseX >= x && mouseX <= x + w) {
                float absX = x + offsetX - wardScrollX + cX;
                if (mouseX >= absX && mouseX <= absX + slotSize) {
                    if (mouseY >= y && mouseY <= y + slotSize && set.helmet != null) hoveredWardrobe = set.helmet;
                    if (mouseY >= y + slotSize + pad/2 && mouseY <= y + slotSize*2 + pad/2 && set.chestplate != null) hoveredWardrobe = set.chestplate;
                    if (mouseY >= y + (slotSize + pad/2)*2 && mouseY <= y + slotSize*3 + pad && set.leggings != null) hoveredWardrobe = set.leggings;
                    if (mouseY >= y + (slotSize + pad/2)*3 && mouseY <= y + slotSize*4 + pad*1.5f && set.boots != null) hoveredWardrobe = set.boots;
                }
            }
            index++;
        }
        GlStateManagerCompat.popMatrix();
        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        return hoveredWardrobe;
    }

    private void drawSlot(Minecraft mc, ItemStack icon, boolean active, int rarityScore, float x, float y, float size, boolean isPet) {
        GlStateManagerCompat.color(1, 1, 1, 1);
        mc.getTextureManager().bindTexture(Resources.storageSlot(1));
        Gui.drawModalRectWithCustomSizedTexture((int)x, (int)y, 0, 0, (int)size, (int)size, (int)size, (int)size);

        if (active) {
            int color = getRarityHex(rarityScore);
            int b = 1;
            Gui.drawRect((int)x + 1, (int)y + 1, (int)(x + size) - 1, (int)y + 1 + b, color);
            Gui.drawRect((int)x + 1, (int)(y + size) - 1 - b, (int)(x + size) - 1, (int)(y + size) - 1, color);
            Gui.drawRect((int)x + 1, (int)y + 1, (int)x + 1 + b, (int)(y + size) - 1, color);
            Gui.drawRect((int)(x + size) - 1 - b, (int)y + 1, (int)(x + size) - 1, (int)(y + size) - 1, color);
        }

        if (icon != null) {
            if (isPet) {
                ItemRenderUtils.renderItemIcon(mc, icon, (int)(x + size*0.1f), (int)(y + size*0.1f), (int)(size*0.8f));
                int squareSize = (int) Math.max(3, size * 0.18f);
                Gui.drawRect((int)(x + size - squareSize - 1), (int)(y + size - squareSize - 1), (int)(x + size - 1), (int)(y + size - 1), getRarityHex(rarityScore));
                if (active) {
                    Gui.drawRect((int)x + 1, (int)y + 1, (int)(x + size) - 1, (int)(y + size) - 1, 0x4055FF55);
                }
            } else {
                ItemRenderUtils.renderItemIcon(mc, icon, (int)(x + size*0.15f), (int)(y + size*0.15f), (int)(size*0.7f));
            }
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

    private void drawPetTooltip(Minecraft mc, RenderablePet pet, int mouseX, int mouseY) {
        if (pet == null || pet.raw == null || pet.raw.data == null) return;
        List<String> lines = new ArrayList<>();
        lines.add(pet.raw.data.displayName);
        if (pet.raw.data.lore != null) lines.addAll(pet.raw.data.lore);
        
        lines.add("");
        lines.add("§8Item SB_ID: §7" + pet.raw.data.skyblockID);
        lines.add("§8ItemRegistry SB_ID: §7" + pet.debugRegistryID);

        if (mc.currentScreen instanceof ProfileViewerGUI) {
            ((ProfileViewerGUI) mc.currentScreen).drawTooltip(lines, mouseX, mouseY);
        }
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

    private void handleHorizontalScroll(float x, float y, float w, float h, float contentW, boolean isPet) {
        int[] mouse = KeybindHelper.getMouseCoords(MinecraftCompat.getMinecraft().currentScreen.width, MinecraftCompat.getMinecraft().currentScreen.height);
        int mx = mouse[0], my = mouse[1];

        if (mx >= x && mx <= x + w && my >= y && my <= y + h) {
            int dWheel = MouseCompat.getDWheel();
            if (dWheel != 0) {
                float step = 50f;
                if (isPet) {
                    petScrollTarget = Math.max(0, Math.min(petScrollTarget + (dWheel > 0 ? -step : step), Math.max(0, contentW - w)));
                } else {
                    wardScrollTarget = Math.max(0, Math.min(wardScrollTarget + (dWheel > 0 ? -step : step), Math.max(0, contentW - w)));
                }
            }
        }
    }

    private void applyScissor(float x, float y, float w, float h) {
        ScaledResolution res = GuiScreenUtils.getScaledResolution();
        int f = res.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((int)(x * f), (int)(GuiScreenUtils.getDisplayHeight() - (y + h) * f), (int)(w * f), (int)(h * f));
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