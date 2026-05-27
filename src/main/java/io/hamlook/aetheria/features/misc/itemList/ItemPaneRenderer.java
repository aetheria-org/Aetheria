package io.hamlook.aetheria.features.misc.itemList;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.config.gui.GuiTextures;
import io.hamlook.aetheria.features.misc.SearchBar;
import io.hamlook.aetheria.features.storage.StorageManager;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.render.ItemRenderUtils;
import io.hamlook.aetheria.utils.render.NineSliceUtils;
import io.hamlook.aetheria.utils.render.TextRenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RegisterEvents
public class ItemPaneRenderer {

    private static final int COLUMNS = 8;
    private static final int PAD = 4;
    private static final int NAV_H = 22;
    private static final int FILTER_H = 20;
    private static final String[] RARITIES = {"Any Rarity", "Common", "Uncommon", "Rare", "Epic", "Legendary", "Mythic", "Divine", "Special"};
    private static final String[] TYPES = {"Any Type", "Sword", "Bow", "Armor", "Helmet", "Chestplate", "Leggings", "Boots", "Accessory", "Pet", "Pickaxe", "Drill"};
    public static ItemPaneRenderer INSTANCE;
    private List<ItemFamily> filteredFamilies = new ArrayList<>();
    private GuiTextField searchField;
    private String lastSearchText = "";
    private int currentPage = 0;
    private boolean wasLoaded = false;
    private String hoverFamilyId = null;
    private int hoverSlotX, hoverSlotY;
    private int dropDx, dropDy, dropDw, dropDh;
    private int rarityFilterIdx = 0;
    private int typeFilterIdx = 0;
    private int paneX, paneY, paneW, paneH, itemsPerPage;
    private int cachedTotalPages = 1;
    public ItemPaneRenderer() {
        INSTANCE = this;
    }

    private static boolean isGlobalSearch() {
        return ATHRConfig.feature != null && ATHRConfig.feature.misc.itemList.searchItemList;
    }

    private static String currentSearchQuery() {
        return isGlobalSearch() ? SearchBar.getItemListSearchText() : SearchBar.getStorageSearchText();
    }

    private static boolean isInBounds(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    private float getScale() {
        return ATHRConfig.feature != null ? ATHRConfig.feature.misc.itemList.itemListScale : 1.0f;
    }

    private int S() {
        return Math.max(16, (int) (24 * getScale()));
    }

    private void updateSearch(String q) {
        String[] terms = q.toLowerCase().trim().split("\\s+");
        String currentRarity = RARITIES[rarityFilterIdx].toLowerCase();
        String currentType = TYPES[typeFilterIdx].toLowerCase();

        filteredFamilies = ItemRegistry.familyRegistry.values().stream().filter(fam -> {
            if (!currentRarity.equals("any rarity")) {
                boolean matchRarity = fam.members.stream().anyMatch(i -> (i.itemRarity != null && i.itemRarity.toLowerCase().contains(currentRarity)) || (i.rarity != null && i.rarity.toLowerCase().contains(currentRarity)));
                if (!matchRarity) return false;
            }
            if (!currentType.equals("any type")) {
                boolean matchType = fam.members.stream().anyMatch(i -> i.itemType != null && i.itemType.toLowerCase().contains(currentType));
                if (!matchType) return false;
            }
            if (q.trim().isEmpty()) return true;
            for (String term : terms) {
                if (term.isEmpty()) continue;
                boolean matchTerm;
                if (term.startsWith("type:")) {
                    String t = term.substring(5);
                    matchTerm = fam.members.stream().anyMatch(i -> i.itemType != null && i.itemType.toLowerCase().contains(t));
                } else if (term.startsWith("rarity:")) {
                    String r = term.substring(7);
                    matchTerm = fam.members.stream().anyMatch(i -> (i.itemRarity != null && i.itemRarity.toLowerCase().contains(r)) || (i.rarity != null && i.rarity.toLowerCase().contains(r)));
                } else {
                    if (fam.cleanDisplayNameLower.contains(term)) matchTerm = true;
                    else
                        matchTerm = fam.members.stream().anyMatch(i -> (i.idLower != null && i.idLower.contains(term)) || (i.cleanNameLower != null && i.cleanNameLower.contains(term)));
                }
                if (!matchTerm) return false;
            }
            return true;
        }).collect(Collectors.toList());

        filteredFamilies.sort((f1, f2) -> f1.cleanDisplayName.compareToIgnoreCase(f2.cleanDisplayName));
        currentPage = 0;
        cachedTotalPages = totalPages();
    }

    private boolean shouldntShow() {
        if (ATHRConfig.feature == null) return true;
        if (!ATHRConfig.feature.misc.itemList.enabled) return true;
        if (StorageManager.isOverlayActive()) return true;
        return !ItemRegistry.isLoaded || ItemRegistry.familyRegistry.isEmpty();
    }

    private int totalPages() {
        if (itemsPerPage <= 0) return 1;
        return Math.max(1, (int) Math.ceil((double) filteredFamilies.size() / itemsPerPage));
    }

    private void computeGeometry(int screenW, int screenH) {
        paneW = COLUMNS * S() + (PAD * 2);
        paneX = screenW - paneW;
        paneY = 0;
        paneH = screenH;

        int searchH = isGlobalSearch() ? 0 : 20;
        int searchPad = isGlobalSearch() ? 0 : PAD;

        int filterY = paneH - searchH - PAD - FILTER_H - searchPad;
        int gridStartY = paneY + PAD + NAV_H + PAD;
        int gridMaxH = (filterY - PAD) - gridStartY;

        int rows = Math.max(1, gridMaxH / S());
        int newItemsPerPage = COLUMNS * rows;
        if (newItemsPerPage != itemsPerPage) {
            itemsPerPage = newItemsPerPage;
            cachedTotalPages = totalPages();
        }
    }

    private void renderItemInSlot(ItemStack stack, int sx, int sy) {
        if (stack == null) return;
        GlStateManager.pushMatrix();
        float itemScale = (S() - PAD) / 16.0f;
        GlStateManager.translate(sx + PAD / 2f, sy + PAD / 2f, 0);
        GlStateManager.scale(itemScale, itemScale, 1.0f);
        ItemRenderUtils.drawItemStack(stack, 0, 0);
        GlStateManager.popMatrix();
    }

    @SubscribeEvent
    public void onDraw(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!(event.gui instanceof GuiContainer)) return;
        drawPane(event.gui.width, event.gui.height, event.mouseX, event.mouseY);
    }

    public void drawPane(int screenW, int screenH, int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getMinecraft();

        if (!ItemRegistry.preloadQueue.isEmpty()) {
            long start = System.currentTimeMillis();
            while (!ItemRegistry.preloadQueue.isEmpty() && System.currentTimeMillis() - start < 3) {
                ItemStack stack = ItemRegistry.preloadQueue.poll();
                if (stack != null) mc.getRenderItem().getItemModelMesher().getItemModel(stack);
            }
        }

        if (shouldntShow()) return;
        if (ItemRegistry.isLoaded && !wasLoaded) {
            wasLoaded = true;
            updateSearch(lastSearchText);
        }

        computeGeometry(screenW, screenH);
        currentPage = Math.max(0, Math.min(currentPage, cachedTotalPages - 1));

        boolean globalSearch = isGlobalSearch();
        int searchH = globalSearch ? 0 : 20;
        int searchPad = globalSearch ? 0 : PAD;

        if (!globalSearch) {
            int sbY = paneH - 20 - PAD;
            if (searchField == null) {
                searchField = SearchBar.createStorageSearchBar(paneX + PAD, sbY, paneW - PAD * 2);
            } else {
                searchField.xPosition = paneX + PAD;
                searchField.yPosition = sbY;
                searchField.width = paneW - PAD * 2;
            }
        }

        String cur = currentSearchQuery();
        if (cur == null) cur = "";
        if (!cur.equals(lastSearchText)) {
            lastSearchText = cur;
            updateSearch(cur);
        }

        int filterY = paneH - searchH - PAD - FILTER_H - searchPad;
        int btnWidth = (paneW - (PAD * 3)) / 2;

        int rarityX = paneX + PAD;
        boolean hoverRar = isInBounds(mouseX, mouseY, rarityX, filterY, btnWidth, FILTER_H);
        NineSliceUtils.draw(GuiTextures.storageBackground(1), rarityX, filterY, btnWidth, FILTER_H, 6, 18);
        if (hoverRar) Gui.drawRect(rarityX, filterY, rarityX + btnWidth, filterY + FILTER_H, 0x33FFFFFF);
        drawCenteredText(mc, RARITIES[rarityFilterIdx], rarityX + btnWidth / 2, filterY + 6, 0xAAAAAA);

        int typeX = rarityX + btnWidth + PAD;
        boolean hoverType = isInBounds(mouseX, mouseY, typeX, filterY, btnWidth, FILTER_H);
        NineSliceUtils.draw(GuiTextures.storageBackground(1), typeX, filterY, btnWidth, FILTER_H, 6, 18);
        if (hoverType) Gui.drawRect(typeX, filterY, typeX + btnWidth, filterY + FILTER_H, 0x33FFFFFF);
        drawCenteredText(mc, TYPES[typeFilterIdx], typeX + btnWidth / 2, filterY + 6, 0xAAAAAA);

        int navY = paneY + PAD;
        int navBtnW = 40;
        int prevX = paneX + PAD;
        int nextX = paneX + paneW - PAD - navBtnW;

        boolean hP = isInBounds(mouseX, mouseY, prevX, navY, navBtnW, NAV_H);
        boolean hN = isInBounds(mouseX, mouseY, nextX, navY, navBtnW, NAV_H);

        NineSliceUtils.draw(GuiTextures.storageBackground(1), prevX, navY, navBtnW, NAV_H, 6, 18);
        if (hP) Gui.drawRect(prevX, navY, prevX + navBtnW, navY + NAV_H, 0x33FFFFFF);
        drawCenteredText(mc, "◄", prevX + navBtnW / 2, navY + 6, hP ? 0xFFFFAA : 0xFFFFFF);

        NineSliceUtils.draw(GuiTextures.storageBackground(1), nextX, navY, navBtnW, NAV_H, 6, 18);
        if (hN) Gui.drawRect(nextX, navY, nextX + navBtnW, navY + NAV_H, 0x33FFFFFF);
        drawCenteredText(mc, "►", nextX + navBtnW / 2, navY + 6, hN ? 0xFFFFAA : 0xFFFFFF);

        drawCenteredText(mc, "Page: " + (currentPage + 1) + "/" + cachedTotalPages, paneX + paneW / 2, navY + 6, 0xCCCCCC);

        boolean overDropdown = false;
        if (hoverFamilyId != null) {
            overDropdown = isInBounds(mouseX, mouseY, dropDx, dropDy, dropDw, dropDh);
            boolean overParent = isInBounds(mouseX, mouseY, hoverSlotX, hoverSlotY, S(), S());
            if (!overDropdown && !overParent) {
                hoverFamilyId = null;
                dropDw = 0;
                dropDh = 0;
            }
        }

        int gridX = paneX + PAD;
        int gridY = paneY + PAD + NAV_H + PAD;
        int start = currentPage * itemsPerPage;

        String nowHovered = null;
        int nowHovX = 0, nowHovY = 0;
        ItemFamily tooltipFamily = null;
        SkyblockItem tooltipItem = null;

        for (int i = 0; i < itemsPerPage; i++) {
            int idx = start + i;
            if (idx >= filteredFamilies.size()) break;

            ItemFamily fam = filteredFamilies.get(idx);
            SkyblockItem rep = fam.representative();
            int col = i % COLUMNS, row = i / COLUMNS;
            int sx = gridX + col * S(), sy = gridY + row * S();

            GlStateManager.color(1f, 1f, 1f, 1f);
            NineSliceUtils.draw(GuiTextures.storageSlot(1), sx, sy, S(), S(), 6, 18);

            if (rep != null) renderItemInSlot(rep.getStack(), sx, sy);

            if (fam.hasDropdown()) {
                int indS = (int) (4 * getScale());
                Gui.drawRect(sx + S() - indS - 2, sy + S() - indS - 2, sx + S() - 2, sy + S() - 2, 0xFFFFDD44);
            }

            boolean hovered = isInBounds(mouseX, mouseY, sx, sy, S(), S());
            if (hovered && !overDropdown) {
                GlStateManager.enableBlend();
                GlStateManager.disableDepth();
                Gui.drawRect(sx, sy, sx + S(), sy + S(), 0x80FFFFFF);
                GlStateManager.enableDepth();

                if (fam.hasDropdown()) {
                    nowHovered = fam.familyId;
                    nowHovX = sx;
                    nowHovY = sy;
                    tooltipFamily = fam;
                } else {
                    tooltipItem = rep;
                    hoverFamilyId = null;
                    dropDw = 0;
                    dropDh = 0;
                }
            }
        }

        if (nowHovered != null) {
            hoverFamilyId = nowHovered;
            hoverSlotX = nowHovX;
            hoverSlotY = nowHovY;
        }

        if (!globalSearch && searchField != null) {
            searchField.updateCursorCounter();
            SearchBar.drawStorageSearchBar(searchField);
        }

        SkyblockItem dropdownTooltipItem = null;
        if (hoverFamilyId != null) {
            ItemFamily dropFam = ItemRegistry.familyRegistry.get(hoverFamilyId);
            if (dropFam != null && dropFam.hasDropdown()) {
                dropdownTooltipItem = drawFamilyHover(mc, dropFam, mouseX, mouseY);
            }
        }

        SkyblockItem tipItem = dropdownTooltipItem != null ? dropdownTooltipItem : tooltipItem;
        if (tipItem != null) {
            List<String> tip = new ArrayList<>();
            tip.add(tipItem.displayName);
            if (tipItem.baseLore != null) tip.addAll(tipItem.baseLore);
            TextRenderUtils.drawHoveringText(tip, mouseX, mouseY, mc.fontRendererObj);
        } else if (tooltipFamily != null) {
            List<String> tip = new ArrayList<>();
            tip.add(tooltipFamily.displayName);
            tip.add("§7" + tooltipFamily.members.size() + " variants – click/hover to expand");
            TextRenderUtils.drawHoveringText(tip, mouseX, mouseY, mc.fontRendererObj);
        }
    }

    private SkyblockItem drawFamilyHover(Minecraft mc, ItemFamily fam, int mouseX, int mouseY) {
        int members = fam.members.size();
        int cols = Math.min(members, 5);
        int dropRows = (int) Math.ceil((double) members / cols);

        dropDw = cols * (S() + 2) + PAD * 2 - 2;
        dropDh = dropRows * (S() + 2) + PAD * 2 - 2;

        dropDx = hoverSlotX - PAD;
        dropDy = hoverSlotY + S();

        if (dropDx + dropDw > paneX + paneW) dropDx = paneX + paneW - dropDw - PAD;
        if (dropDx < paneX) dropDx = paneX + PAD;
        if (dropDy + dropDh > paneY + paneH) dropDy = hoverSlotY - dropDh;

        GlStateManager.pushMatrix();
        GlStateManager.translate(0, 0, 300);

        Gui.drawRect(dropDx - 1, dropDy - 1, dropDx + dropDw + 1, dropDy, 0xFFFFAA00);
        Gui.drawRect(dropDx - 1, dropDy + dropDh, dropDx + dropDw + 1, dropDy + dropDh + 1, 0xFFFFAA00);
        Gui.drawRect(dropDx - 1, dropDy, dropDx, dropDy + dropDh, 0xFFFFAA00);
        Gui.drawRect(dropDx + dropDw, dropDy, dropDx + dropDw + 1, dropDy + dropDh, 0xFFFFAA00);

        GlStateManager.color(1f, 1f, 1f, 1f);
        NineSliceUtils.draw(GuiTextures.storageBackground(1), dropDx, dropDy, dropDw, dropDh, 6, 18);

        SkyblockItem hovered = null;
        for (int i = 0; i < members; i++) {
            SkyblockItem mem = fam.members.get(i);
            int r = i / cols;
            int c = i % cols;
            int sx = dropDx + PAD + c * (S() + 2);
            int sy = dropDy + PAD + r * (S() + 2);

            NineSliceUtils.draw(GuiTextures.storageSlot(1), sx, sy, S(), S(), 6, 18);
            renderItemInSlot(mem.getStack(), sx, sy);

            boolean h = isInBounds(mouseX, mouseY, sx, sy, S(), S());
            if (h) {
                GlStateManager.enableBlend();
                GlStateManager.disableDepth();
                Gui.drawRect(sx, sy, sx + S(), sy + S(), 0x80FFFFFF);
                GlStateManager.enableDepth();
                hovered = mem;
            }
        }

        GlStateManager.popMatrix();
        return hovered;
    }

    private void drawCenteredText(Minecraft mc, String text, int x, int y, int color) {
        mc.fontRendererObj.drawStringWithShadow(text, x - mc.fontRendererObj.getStringWidth(text) / 2f, y, color);
    }

    @SubscribeEvent
    public void onMouse(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (!(event.gui instanceof GuiContainer)) return;
        if (shouldntShow()) return;

        Minecraft mc = Minecraft.getMinecraft();
        int mouseX = Mouse.getEventX() * event.gui.width / mc.displayWidth;
        int mouseY = event.gui.height - Mouse.getEventY() * event.gui.height / mc.displayHeight - 1;
        handleMouseInput(event.gui.width, event.gui.height, mouseX, mouseY, event);
    }

    public void handleMouseInput(int screenW, int screenH, int mouseX, int mouseY, GuiScreenEvent.MouseInputEvent.Pre event) {
        if (shouldntShow()) return;

        int dw = Mouse.getEventDWheel();
        if (dw != 0) {
            computeGeometry(screenW, screenH);
            if (isInBounds(mouseX, mouseY, paneX, paneY, paneW, paneH)) {
                currentPage = dw > 0 ? Math.max(0, currentPage - 1) : Math.min(cachedTotalPages - 1, currentPage + 1);
                if (event != null) event.setCanceled(true);
            }
            return;
        }

        if (!Mouse.getEventButtonState()) return;
        if (Mouse.getEventButton() != 0 && Mouse.getEventButton() != 1) return;

        computeGeometry(screenW, screenH);
        handleClick(screenW, screenH, mouseX, mouseY, Mouse.getEventButton(), event);
    }

    public void handleClick(int screenW, int screenH, int mouseX, int mouseY, int btn, GuiScreenEvent.MouseInputEvent.Pre event) {
        if (shouldntShow()) return;

        if (mouseX < paneX || mouseX >= paneX + paneW || mouseY < paneY || mouseY >= paneY + paneH) return;

        Minecraft mc = Minecraft.getMinecraft();

        int navY = paneY + PAD, navBtnW = 40;
        int prevX = paneX + PAD, nextX = paneX + paneW - PAD - navBtnW;
        if (isInBounds(mouseX, mouseY, prevX, navY, navBtnW, NAV_H)) {
            currentPage = Math.max(0, currentPage - 1);
            if (event != null) event.setCanceled(true);
            return;
        }
        if (isInBounds(mouseX, mouseY, nextX, navY, navBtnW, NAV_H)) {
            currentPage = Math.min(cachedTotalPages - 1, currentPage + 1);
            if (event != null) event.setCanceled(true);
            return;
        }

        boolean globalSearch = isGlobalSearch();
        int searchH = globalSearch ? 0 : 20;
        int searchPad = globalSearch ? 0 : PAD;
        int filterY = paneH - searchH - PAD - FILTER_H - searchPad;
        int btnWidth = (paneW - (PAD * 3)) / 2;
        int rarityX = paneX + PAD;
        int typeX = rarityX + btnWidth + PAD;

        if (mouseY >= filterY && mouseY < filterY + FILTER_H) {
            if (mouseX >= rarityX && mouseX < rarityX + btnWidth) {
                rarityFilterIdx = (rarityFilterIdx + 1) % RARITIES.length;
                updateSearch(currentSearchQuery());
                if (event != null) event.setCanceled(true);
                return;
            }
            if (mouseX >= typeX && mouseX < typeX + btnWidth) {
                typeFilterIdx = (typeFilterIdx + 1) % TYPES.length;
                updateSearch(currentSearchQuery());
                if (event != null) event.setCanceled(true);
                return;
            }
        }

        if (!globalSearch && SearchBar.handleStorageMouseClick(searchField, mouseX, mouseY)) {
            if (event != null) event.setCanceled(true);
            return;
        }

        if (hoverFamilyId != null) {
            ItemFamily fam = ItemRegistry.familyRegistry.get(hoverFamilyId);
            if (fam != null && fam.hasDropdown()) {
                int members = fam.members.size();
                int cols = Math.min(members, 5);
                for (int i = 0; i < members; i++) {
                    int sx = dropDx + PAD + (i % cols) * (S() + 2);
                    int sy = dropDy + PAD + (i / cols) * (S() + 2);
                    if (isInBounds(mouseX, mouseY, sx, sy, S(), S())) {
                        if (btn == 1) WikiPane.open(fam.members.get(i));
                        else mc.displayGuiScreen(new RecipeViewerGUI(fam.members.get(i)));
                        if (event != null) event.setCanceled(true);
                        return;
                    }
                }
            }
        }

        int gridX = paneX + PAD, gridY = paneY + PAD + NAV_H + PAD;
        for (int i = 0; i < itemsPerPage; i++) {
            int idx = currentPage * itemsPerPage + i;
            if (idx >= filteredFamilies.size()) break;
            ItemFamily fam = filteredFamilies.get(idx);
            int sx = gridX + (i % COLUMNS) * S(), sy = gridY + (i / COLUMNS) * S();
            if (isInBounds(mouseX, mouseY, sx, sy, S(), S())) {
                if (btn == 1) WikiPane.open(fam.representative());
                else if (!fam.hasDropdown() && fam.representative() != null)
                    mc.displayGuiScreen(new RecipeViewerGUI(fam.representative()));
                if (event != null) event.setCanceled(true);
                return;
            }
        }
    }

    private boolean processKeyInput() {
        if (!isGlobalSearch() && searchField != null && searchField.isFocused() && Keyboard.getEventKeyState()) {
            if (SearchBar.handleStorageKeyTyped(searchField, Keyboard.getEventCharacter(), Keyboard.getEventKey())) {
                updateSearch(SearchBar.getStorageSearchText());
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public void onKey(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        if (!(event.gui instanceof GuiContainer)) return;
        if (shouldntShow()) return;
        if (processKeyInput()) event.setCanceled(true);
    }

    public void handleKeyInput() {
        if (shouldntShow()) return;
        processKeyInput();
    }
}
