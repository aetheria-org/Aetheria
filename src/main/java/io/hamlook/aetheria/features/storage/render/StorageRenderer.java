package io.hamlook.aetheria.features.storage.render;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.Resources;
import io.hamlook.aetheria.utils.render.TextRenderUtils;
import io.hamlook.aetheria.features.misc.SearchBar;
import io.hamlook.aetheria.features.storage.StorageManager;
import io.hamlook.aetheria.features.storage.utils.SContainer;
import io.hamlook.aetheria.features.storage.utils.Type;
import io.hamlook.aetheria.utils.render.ItemRenderUtils;
import io.hamlook.aetheria.utils.render.NineSliceUtils;
import io.hamlook.aetheria.utils.render.ResolutionUtils;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.LinkedHashMap;

public class StorageRenderer extends Gui {

    private static final int PADDING = 5;
    private static final int ROW_SPACING = 16;
    private static final int INVENTORY_HEIGHT = 76;
    private static final float SCROLL_LENGTH = 0.2f;
    private static final int SEARCH_BAR_WIDTH = 200;
    private static final int SEARCH_BAR_HEIGHT = 20;
    private static final int NINE_SLICE_CORNER = 6;
    private static final int NINE_SLICE_SIZE = 18;
    private static final int SLOT_SIZE = 18;
    private static final int SLOTS_PER_ROW = 9;

    private ResourceLocation getContainerBg() {
        return Resources.storageBackground(ATHRConfig.feature.storage.overlayStyle);
    }

    private ResourceLocation getSlotTexture() {
        return Resources.storageSlot(ATHRConfig.feature.storage.overlayStyle);
    }

    private final LinkedHashMap<String, SContainer> containers;
    private final java.util.HashMap<String, Boolean> searchCache = new java.util.HashMap<>();
    private final java.util.HashMap<String, Integer> containerHeightCache = new java.util.HashMap<>();
    private final java.util.HashMap<Integer, Integer> rowHeightCache = new java.util.HashMap<>();
    private int boxX, boxY, boxW, boxH;
    private int containerW, containerH;
    private int containersPerRow = 3;
    private int inventoryX, inventoryY;
    private int storageAreaH;
    private int lastWidth, lastHeight;
    private float scrollOffset;
    private float scrollTarget;
    private final float scrollSpeed;
    private final java.util.List<SContainer> filteredContainers = new java.util.ArrayList<>();
    @Getter
    private ItemStack hoveredItem;
    private int hoveredX = -1;
    private int hoveredY = -1;
    private boolean hoveredItemIsFromInventory = false;
    private boolean isDraggingScrollbar = false;
    private int dragStartY = 0;
    private float dragStartScroll = 0;
    private GuiTextField searchField;
    private String searchText = "";
    private String lastSearchText = "";
    private boolean needsScrollToActive = false;
    private String lastActiveContainerId = null;

    // Scrollbar geometry — cached on layout change
    private int sbX, sbTrackY, sbTrackHeight;

    // Pre-computed per frame
    private ContainerLayout[] cachedLayouts = new ContainerLayout[0];
    private int[] rowYOffsets = new int[0];

    boolean containersDirty = false;

    public StorageRenderer(LinkedHashMap<String, SContainer> containers) {
        this.containers = containers;
        this.scrollSpeed = ATHRConfig.feature.storage.scrollSpeed;
        this.containersDirty = true;
        initLayout();
        initSearchBar();
        updateScrollbarGeometry();
    }

    public void markDirty() {
        containersDirty = true;
    }

    public boolean isHoveredItemFromInventory() {
        return hoveredItemIsFromInventory;
    }

    private void drawBackground() {
        int width = ResolutionUtils.getWidth();
        int height = ResolutionUtils.getHeight();
        GlStateManager.disableLighting();
        GlStateManager.disableFog();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        drawGradientRect(0, 0, width, height, -1072689136, -804253680);
        GlStateManager.disableBlend();
    }

    private void drawPanelBackground(int x, int y, int width, int height) {
        GlStateManager.disableBlend();
        GlStateManager.color(1f, 1f, 1f, 1f);
        drawRect(x, y, x + width, y + height, 0xFF000000);
        GlStateManager.enableBlend();
        NineSliceUtils.draw(getContainerBg(), x, y, width, height, NINE_SLICE_CORNER, NINE_SLICE_SIZE);
    }

    private void initSearchBar() {
        int searchBarY = boxY - SEARCH_BAR_HEIGHT - 4;
        if (searchBarY < 4) searchBarY = 4;
        int searchBarX = boxX + (boxW - SEARCH_BAR_WIDTH) / 2;
        searchField = SearchBar.createStorageSearchBar(searchBarX, searchBarY, SEARCH_BAR_WIDTH);
    }

    private void updateScrollbarGeometry() {
        int scrollbarWidth = 4;
        sbX = boxX + boxW - NINE_SLICE_CORNER - scrollbarWidth + 2;
        sbTrackY = boxY + NINE_SLICE_CORNER + 2;
        sbTrackHeight = storageAreaH - NINE_SLICE_CORNER * 2 - 4;
    }

    private void initLayout() {
        int width = ResolutionUtils.getWidth();
        int height = ResolutionUtils.getHeight();

        containerW = 170;

        int minContainerWidth = containerW + PADDING;
        int maxContainersPerRow = Math.max(3, (width - 40) / minContainerWidth);
        containersPerRow = maxContainersPerRow;

        int maxContainerH = 120;
        for (SContainer container : containers.values()) {
            int h = getContainerDisplayHeight(container);
            if (h > maxContainerH) maxContainerH = h;
        }
        containerH = maxContainerH;

        inventoryX = (width - 162) / 2;
        inventoryY = height - INVENTORY_HEIGHT - 10;

        int searchBarReserved = SEARCH_BAR_HEIGHT + 8;
        int topMargin = 10;

        int maxStorageH = inventoryY - searchBarReserved - topMargin;
        int rowHeight = containerH + PADDING;
        int padding = PADDING * 2 + 20;
        int maxRows = Math.max(3, (maxStorageH - padding) / rowHeight);
        int rows = Math.min(maxRows, 10);
        storageAreaH = Math.min(rowHeight * rows + padding, maxStorageH);
        if (storageAreaH < 40) storageAreaH = 40;

        boxY = inventoryY - storageAreaH;
        if (boxY < topMargin + searchBarReserved) {
            boxY = topMargin + searchBarReserved;
            storageAreaH = inventoryY - boxY;
        }

        boxH = storageAreaH;

        boxW = (containerW + PADDING) * containersPerRow + PADDING * 2;
        int maxBoxW = width - 20;
        if (boxW > maxBoxW) {
            boxW = maxBoxW;
            containersPerRow = Math.max(1, (boxW - PADDING * 2) / (containerW + PADDING));
        }

        boxX = (width - boxW) / 2;
        if (boxX < 10) boxX = 10;
        if (boxX + boxW > width - 10) boxX = width - boxW - 10;

        updateScrollbarGeometry();
    }

    private boolean containerMatchesSearch(SContainer container) {
        if (searchText == null || searchText.isEmpty()) return true;

        String cacheKey = container.id + ":" + searchText;
        if (searchCache.containsKey(cacheKey)) {
            return searchCache.get(cacheKey);
        }

        for (int i = 0; i < container.slotCount; i++) {
            String displayName = container.getDisplayName(i);
            if (displayName != null && !displayName.isEmpty() && displayName.toLowerCase().contains(searchText)) {
                searchCache.put(cacheKey, true);
                return true;
            }
        }
        searchCache.put(cacheKey, false);
        return false;
    }

    private boolean itemMatchesSearch(ItemStack stack) {
        if (searchText == null || searchText.isEmpty()) return false;
        if (stack == null) return false;
        return stack.getDisplayName().toLowerCase().contains(searchText);
    }

    private int getContainerDisplayHeight(SContainer container) {
        String cacheKey = container.id + ":" + container.slotCount;
        if (containerHeightCache.containsKey(cacheKey)) {
            return containerHeightCache.get(cacheKey);
        }

        int rows = (int) Math.ceil(container.slotCount / 9.0);
        int titleHeight = 18;
        int bottomPadding = 4;
        int height = titleHeight + (rows * 18) + bottomPadding + 8;

        containerHeightCache.put(cacheKey, height);
        return height;
    }

    private void rebuildFilteredContainers() {
        filteredContainers.clear();
        for (SContainer container : containers.values()) {
            if (containerMatchesSearch(container)) {
                filteredContainers.add(container);
            }
        }
    }

    private void computeCachedLayouts() {
        int count = filteredContainers.size();
        if (cachedLayouts.length != count) {
            cachedLayouts = new ContainerLayout[count];
        }

        int rowCount = (int) Math.ceil((double) count / containersPerRow);
        if (rowYOffsets.length != rowCount) {
            rowYOffsets = new int[rowCount];
        }

        // Pre-compute row Y offsets
        int runningOffset = 0;
        for (int r = 0; r < rowCount; r++) {
            rowYOffsets[r] = runningOffset;
            runningOffset += getRowHeight(r);
            if (r < rowCount - 1) {
                runningOffset += ROW_SPACING;
            }
        }

        // Grid geometry
        int totalGridW = containerW * containersPerRow + PADDING * (containersPerRow - 1);
        int gridStartX = boxX + (boxW - totalGridW) / 2;
        int gridStartY = boxY + 6 + PADDING;
        int scrollPixels = (int) scrollOffset;

        for (int i = 0; i < count; i++) {
            int col = i % containersPerRow;
            int row = i / containersPerRow;
            SContainer container = filteredContainers.get(i);

            int x = gridStartX + col * (containerW + PADDING);
            int y = gridStartY + rowYOffsets[row] - scrollPixels;
            int height = getContainerDisplayHeight(container);
            boolean isVisible = y + height > boxY + 10 && y < boxY + storageAreaH - 10;

            cachedLayouts[i] = new ContainerLayout(x, y, containerW, height, isVisible);
        }
    }

    public void render(int mouseX, int mouseY) {
        if (containers.isEmpty()) return;

        drawBackground();

        hoveredItem = null;
        hoveredX = -1;
        hoveredY = -1;

        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;

        int curWidth = ResolutionUtils.getWidth();
        int curHeight = ResolutionUtils.getHeight();
        if (curWidth != lastWidth || curHeight != lastHeight) {
            lastWidth = curWidth;
            lastHeight = curHeight;
            initLayout();
            initSearchBar();
        }

        // Update search text — only rebuild filtered list when needed
        String newSearchText = SearchBar.getStorageSearchText().toLowerCase();
        boolean searchChanged = !newSearchText.equals(lastSearchText);
        boolean needsRebuild = searchChanged || containersDirty;

        if (searchChanged) {
            searchCache.clear();
            rowHeightCache.clear();
            containerHeightCache.clear();
            lastSearchText = newSearchText;
        }
        if (containersDirty) {
            searchCache.clear();
            containersDirty = false;
        }

        searchText = newSearchText;

        if (needsRebuild) {
            rebuildFilteredContainers();
        }

        if (searchField != null) {
            searchField.updateCursorCounter();
        }

        int maxScroll = getMaxScroll();
        scrollTarget = Math.max(0, Math.min(scrollTarget, maxScroll));
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        handleScrollbarDrag(mouseX, mouseY, org.lwjgl.input.Mouse.isButtonDown(0));

        SearchBar.drawStorageSearchBar(searchField);

        drawPanelBackground(boxX, boxY, boxW, boxH);

        scrollToActiveContainerIfNeeded();

        scrollOffset += (scrollTarget - scrollOffset) * SCROLL_LENGTH;

        int scaleFactor = ResolutionUtils.getFactor();
        int inset = NINE_SLICE_CORNER;

        int scissorScreenTop = boxY + inset;
        int scissorScreenBottom = boxY + storageAreaH - inset;
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((boxX + inset) * scaleFactor, (Minecraft.getMinecraft().displayHeight - scissorScreenBottom * scaleFactor), (boxW - inset * 2) * scaleFactor, (scissorScreenBottom - scissorScreenTop) * scaleFactor);

        String activeId = StorageManager.getActiveContainerId();
        boolean dimMode = ATHRConfig.feature.storage.activeContainerStyle == 0 && activeId != null;

        // Pre-compute layouts and row Y offsets for this frame
        computeCachedLayouts();

        // Single pass: draw containers + dim overlay in one loop
        for (int i = 0; i < filteredContainers.size(); i++) {
            SContainer container = filteredContainers.get(i);
            boolean isActive = container.id.equals(activeId);
            drawContainer(i, mouseX, mouseY, fr, isActive, dimMode);
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        renderScrollbar();
        renderPlayerInventory(mouseX, mouseY);

        if (hoveredItem != null) {
            TextRenderUtils.drawItemTooltip(hoveredItem, hoveredX, hoveredY, fr);
        }
    }

    private void drawSlotBackground(int x, int y, float color) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(getSlotTexture());
        GlStateManager.color(color, 1f, 1f, 1f);
        drawModalRectWithCustomSizedTexture(x, y, 0, 0, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    private void drawSlotItem(int x, int y, ItemStack stack, int mouseX, int mouseY, boolean isFromInventory) {
        ItemRenderUtils.drawItemStackOverlay(stack, x, y);

        if (isHovering(mouseX, mouseY, x, y, SLOT_SIZE, SLOT_SIZE)) {
            if (stack != null) {
                hoveredItem = stack;
                hoveredX = mouseX;
                hoveredY = mouseY;
                hoveredItemIsFromInventory = isFromInventory;
            }
            drawSlotHighlight(x, y);
        }
    }

    private void drawSlotHighlight(int x, int y) {
        GlStateManager.disableDepth();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.colorMask(true, true, true, false);
        drawRect(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0x80FFFFFF);
        GlStateManager.colorMask(true, true, true, true);
        GlStateManager.disableBlend();
    }

    private void renderPlayerInventory(int mouseX, int mouseY) {
        ItemStack[] playerItems = Minecraft.getMinecraft().thePlayer.inventory.mainInventory;

        drawInventoryBackground();
        renderInventorySlots(playerItems, mouseX, mouseY);

        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.disableBlend();
    }

    private void drawInventoryBackground() {
        int invPanelX = inventoryX - 5;
        int invPanelY = inventoryY - 4;
        int invPanelW = 172;
        int invPanelH = INVENTORY_HEIGHT + 4 + 5;

        drawPanelBackground(invPanelX, invPanelY, invPanelW, invPanelH);
    }

    private void renderInventorySlots(ItemStack[] playerItems, int mouseX, int mouseY) {
        float fullBright = 1f;
        for (int i = 0; i < 27; i++) {
            int x = inventoryX + (i % 9) * 18;
            int y = inventoryY + (i / 9) * 18;
            drawSlotBackground(x, y, fullBright);
            drawSlotItem(x, y, playerItems[i + 9], mouseX, mouseY, true);
        }

        for (int i = 0; i < 9; i++) {
            int x = inventoryX + i * 18;
            int y = inventoryY + 58;
            drawSlotBackground(x, y, fullBright);
            drawSlotItem(x, y, playerItems[i], mouseX, mouseY, true);
        }
    }

    private void renderScrollbar() {
        int maxScroll = getMaxScroll();
        if (maxScroll <= 0) return;

        int scrollbarWidth = 4;

        drawRect(sbX, sbTrackY, sbX + scrollbarWidth, sbTrackY + sbTrackHeight, 0x80000000);

        int visibleHeight = storageAreaH - NINE_SLICE_CORNER * 2;
        int totalHeight = visibleHeight + maxScroll;
        float thumbHeightRatio = (float) visibleHeight / totalHeight;
        int thumbHeight = Math.max(20, (int) (sbTrackHeight * thumbHeightRatio));

        float scrollRatio = scrollOffset / maxScroll;
        int thumbY = sbTrackY + (int) ((sbTrackHeight - thumbHeight) * scrollRatio);

        drawRect(sbX, thumbY, sbX + scrollbarWidth, thumbY + thumbHeight, 0xFFAAAAAA);
    }

    public boolean isMouseOverScrollbar(int mouseX, int mouseY) {
        if (getMaxScroll() <= 0) return false;
        int scrollbarWidth = 4;
        return mouseX >= sbX && mouseX <= sbX + scrollbarWidth &&
                mouseY >= sbTrackY && mouseY <= sbTrackY + sbTrackHeight;
    }

    public void handleScrollbarDrag(int mouseX, int mouseY, boolean isPressed) {
        int maxScroll = getMaxScroll();
        if (maxScroll <= 0) return;

        if (isPressed && !isDraggingScrollbar && isMouseOverScrollbar(mouseX, mouseY)) {
            isDraggingScrollbar = true;
            dragStartY = mouseY;
            dragStartScroll = scrollOffset;
        } else if (!isPressed) {
            isDraggingScrollbar = false;
        }

        if (isDraggingScrollbar) {
            int visibleHeight = storageAreaH - NINE_SLICE_CORNER * 2;
            int totalHeight = visibleHeight + maxScroll;
            float thumbHeightRatio = (float) visibleHeight / totalHeight;
            int thumbHeight = Math.max(20, (int) (sbTrackHeight * thumbHeightRatio));

            int deltaY = mouseY - dragStartY;
            float scrollableTrackHeight = sbTrackHeight - thumbHeight;
            float scrollDelta = (deltaY / scrollableTrackHeight) * maxScroll;

            scrollTarget = Math.max(0, Math.min(maxScroll, dragStartScroll + scrollDelta));
            scrollOffset = scrollTarget;
        }
    }

    public void handleScroll(int dWheel) {
        int maxScroll = getMaxScroll();
        float step = (containerH + PADDING) * scrollSpeed;
        scrollTarget -= dWheel > 0 ? step : -step;
        scrollTarget = Math.max(0, Math.min(scrollTarget, maxScroll));
    }

    private void scrollToActiveContainerIfNeeded() {
        String activeId = StorageManager.getActiveContainerId();

        if (activeId != null && (!activeId.equals(lastActiveContainerId) || needsScrollToActive)) {
            scrollToContainer(activeId);
            lastActiveContainerId = activeId;
            needsScrollToActive = false;
        } else if (activeId == null) {
            lastActiveContainerId = null;
        }
    }

    private void scrollToContainer(String containerId) {
        if (containerId == null) return;

        SContainer container = containers.get(containerId);
        if (container == null) return;

        int visibleIndex = filteredContainers.indexOf(container);
        if (visibleIndex < 0) return;

        int row = visibleIndex / containersPerRow;
        int containerY = getRowYOffset(row);
        int containerHeight = getContainerDisplayHeight(container);

        int visibleHeight = storageAreaH - 20;

        int currentScrollPixels = (int) scrollOffset;
        int containerTop = containerY - currentScrollPixels;
        int containerBottom = containerTop + containerHeight;

        if (containerTop >= 0 && containerBottom <= visibleHeight) {
            return;
        }

        int targetScroll = containerY - (visibleHeight - containerHeight) / 2;
        int maxScroll = getMaxScroll();
        targetScroll = Math.max(0, Math.min(targetScroll, maxScroll));

        scrollTarget = targetScroll;
    }

    public void requestScrollToActive() {
        needsScrollToActive = true;
    }

    private int getMaxScroll() {
        int visibleCount = filteredContainers.size();
        if (visibleCount == 0) return 0;
        int rowCount = (int) Math.ceil((double) visibleCount / containersPerRow);

        int totalHeight = 0;
        for (int i = 0; i < rowCount; i++) {
            totalHeight += getRowHeight(i);
            if (i < rowCount - 1) {
                totalHeight += ROW_SPACING;
            }
        }

        int visibleHeight = storageAreaH - 20;
        return Math.max(0, totalHeight - visibleHeight);
    }

    public boolean isMouseOverStorageArea(int mouseX, int mouseY) {
        return mouseX >= boxX && mouseX <= boxX + boxW && mouseY >= boxY && mouseY <= boxY + boxH;
    }

    public void handleClick(int mouseX, int mouseY) {
        if (isMouseOverScrollbar(mouseX, mouseY)) {
            return;
        }

        if (SearchBar.handleStorageMouseClick(searchField, mouseX, mouseY)) {
            return;
        }

        for (int i = 0; i < filteredContainers.size(); i++) {
            SContainer container = filteredContainers.get(i);
            ContainerLayout layout = getContainerLayout(i);
            if (!layout.isVisible) continue;

            if (isHovering(mouseX, mouseY, layout.x, layout.y, layout.width, layout.height)) {
                handleContainerClick(container);
                return;
            }
        }
    }

    public boolean handleKeyTyped(char typedChar, int keyCode) {
        return SearchBar.handleStorageKeyTyped(searchField, typedChar, keyCode);
    }

    private void handleContainerClick(SContainer container) {
        if (container.id.equals(StorageManager.getActiveContainerId())) {
            return;
        }

        StorageManager.switchToContainer(container.id);
    }

    private int getRowHeight(int rowIndex) {
        if (rowHeightCache.containsKey(rowIndex)) {
            return rowHeightCache.get(rowIndex);
        }

        int maxHeight = 0;
        int startIndex = rowIndex * containersPerRow;
        int endIndex = Math.min(startIndex + containersPerRow, filteredContainers.size());

        for (int i = startIndex; i < endIndex; i++) {
            SContainer container = filteredContainers.get(i);
            int height = getContainerDisplayHeight(container);
            if (height > maxHeight) {
                maxHeight = height;
            }
        }

        rowHeightCache.put(rowIndex, maxHeight);
        return maxHeight;
    }

    private int getRowYOffset(int rowIndex) {
        if (rowIndex < rowYOffsets.length) {
            return rowYOffsets[rowIndex];
        }
        int offset = 0;
        for (int i = 0; i < rowIndex; i++) {
            offset += getRowHeight(i) + ROW_SPACING;
        }
        return offset;
    }

    private int[] getGridStart() {
        int totalGridW = (containerW * containersPerRow) + (PADDING * (containersPerRow - 1));
        int gridStartX = boxX + (boxW - totalGridW) / 2;
        int gridStartY = boxY + 6 + PADDING;
        return new int[]{gridStartX, gridStartY};
    }

    private int[] getGridPosition(int index) {
        int xGrid = index % containersPerRow;
        int yGrid = index / containersPerRow;
        return new int[]{xGrid, yGrid};
    }

    private void drawContainer(int index, int mouseX, int mouseY, FontRenderer fr, boolean isActive, boolean dimMode) {
        ContainerLayout layout = cachedLayouts[index];
        SContainer container = filteredContainers.get(index);

        drawContainerBackground(layout, isActive, mouseX, mouseY, dimMode);
        drawContainerTitle(container, layout, fr, isActive, dimMode);
        drawContainerSlots(container, layout, mouseX, mouseY);

        // Draw dim overlay on inactive containers (merged into first pass)
        if (dimMode && !isActive && layout.isVisible) {
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GlStateManager.disableTexture2D();
            drawRect(layout.x, layout.y, layout.x + layout.width, layout.y + layout.height, 0x55000000);
            GlStateManager.enableTexture2D();
            GlStateManager.disableBlend();
        }
    }

    private ContainerLayout getContainerLayout(int index) {
        SContainer container = filteredContainers.get(index);
        int[] gridPos = getGridPosition(index);
        int[] gridStart = getGridStart();

        int scrollPixels = (int) scrollOffset;
        int yOffset = getRowYOffset(gridPos[1]);

        int x = gridStart[0] + (gridPos[0] * (containerW + PADDING));
        int y = gridStart[1] + yOffset - scrollPixels;
        int width = containerW;
        int height = getContainerDisplayHeight(container);

        boolean isVisible = y + height > boxY + 10 && y < boxY + storageAreaH - 10;

        return new ContainerLayout(x, y, width, height, isVisible);
    }

    private ContainerLayout getContainerLayout(SContainer container) {
        int index = filteredContainers.indexOf(container);
        if (index < 0) return null;
        return getContainerLayout(index);
    }

    private void drawContainerBackground(ContainerLayout info, boolean isActive, int mouseX, int mouseY, boolean dimMode) {
        boolean hovering = isHovering(mouseX, mouseY, info.x, info.y, info.width, info.height);

        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableLighting();

        if (isActive) {
            if (dimMode) {
                GlStateManager.color(1.4f, 1.4f, 0.7f, 1f);
            } else {
                GlStateManager.color(1.2f, 1.2f, 0.8f, 1f);
            }
        } else if (hovering && !dimMode) {
            GlStateManager.color(1.3f, 1.3f, 1.3f, 1f);
        } else if (hovering) {
            GlStateManager.color(1.1f, 1.1f, 1.1f, 1f);
        }

        NineSliceUtils.draw(getContainerBg(), info.x, info.y, info.width, info.height, NINE_SLICE_CORNER, NINE_SLICE_SIZE);
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    private void drawContainerTitle(SContainer container, ContainerLayout info, FontRenderer fr, boolean isActive, boolean dimMode) {
        String title = buildContainerTitle(container, isActive, dimMode);
        drawCenteredString(fr, title, info.x + info.width / 2, info.y + 4, Color.WHITE.getRGB());
    }

    private String buildContainerTitle(SContainer container, boolean isActive, boolean dimMode) {
        String baseTitle = container.type == Type.ECHEST ? "§6Ender Chest " + container.page : "§aBackpack " + container.page;

        if (isActive) {
            if (!dimMode) {
                baseTitle = "§e§l» §r" + baseTitle + " §e§l«";
            } else {
                baseTitle = "§e§l" + baseTitle;
            }
        }

        if (container.locked) {
            baseTitle += " §c(Locked)";
        }

        if (container.empty) {
            baseTitle += " §7(Empty)";
        }

        return baseTitle;
    }

    private void drawContainerSlots(SContainer container, ContainerLayout info, int mouseX, int mouseY) {
        int gridWidth = SLOT_SIZE * SLOTS_PER_ROW;
        int startX = info.x + (info.width - gridWidth) / 2;
        int startY = info.y + 18;

        GlStateManager.enableBlend();
        GlStateManager.disableLighting();

        for (int i = 0; i < container.slotCount; i++) {
            int col = i % SLOTS_PER_ROW;
            int row = i / SLOTS_PER_ROW;
            int xPos = startX + (col * SLOT_SIZE);
            int yPos = startY + (row * SLOT_SIZE);
            if (!isSlotVisible(xPos, yPos)) continue;
            ItemStack stack = container.getStack(i);
            float color = itemMatchesSearch(stack) && !searchText.isEmpty() ? 0.5f : 1f;
            drawSlotBackground(xPos, yPos, color);
            drawSlotItem(xPos, yPos, stack, mouseX, mouseY, false);
        }

        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.disableBlend();
        GlStateManager.disableLighting();
    }

    private boolean isHovering(int mouseX, int mouseY, int xStart, int yStart, int width, int height) {
        return mouseX > xStart && mouseX < xStart + width && mouseY > yStart && mouseY < yStart + height;
    }

    private boolean isSlotVisible(int slotX, int slotY) {
        int slotEndX = slotX + SLOT_SIZE;
        int slotEndY = slotY + SLOT_SIZE;

        int inset = NINE_SLICE_CORNER;
        int storageLeft = boxX + inset;
        int storageRight = boxX + boxW - inset;
        int storageTop = boxY + inset;
        int storageBottom = boxY + storageAreaH - inset;

        int invLeft = inventoryX;
        int invRight = inventoryX + 162;
        int invTop = inventoryY;
        int invBottom = inventoryY + INVENTORY_HEIGHT;

        boolean inStorageArea = slotX < storageRight && slotEndX > storageLeft &&
                slotY < storageBottom && slotEndY > storageTop;

        boolean inInventoryArea = slotX < invRight && slotEndX > invLeft &&
                slotY < invBottom && slotEndY > invTop;

        return inStorageArea || inInventoryArea;
    }

    public boolean isClickingPlayerInventory(int mouseX, int mouseY) {
        return mouseX >= inventoryX && mouseX < inventoryX + 162 && mouseY >= inventoryY && mouseY < inventoryY + INVENTORY_HEIGHT;
    }

    public boolean isMouseOverPlayerInventorySlot(net.minecraft.inventory.Slot slot, int mouseX, int mouseY) {
        int slotIndex = slot.getSlotIndex();

        if (slotIndex >= 0 && slotIndex < 9) {
            return checkSlotHover(mouseX, mouseY, inventoryX + slotIndex * 18, inventoryY + 58);
        }

        if (slotIndex >= 9 && slotIndex < 36) {
            int adjustedIndex = slotIndex - 9;
            return checkSlotHover(mouseX, mouseY, inventoryX + (adjustedIndex % 9) * 18, inventoryY + (adjustedIndex / 9) * 18);
        }

        return false;
    }

    public boolean isMouseOverActiveContainerSlot(net.minecraft.inventory.Slot slot, int mouseX, int mouseY) {
        String activeId = StorageManager.getActiveContainerId();
        if (activeId == null) return false;

        SContainer activeContainer = containers.get(activeId);
        if (activeContainer == null) return false;
        if (!containerMatchesSearch(activeContainer)) return false;

        ContainerLayout layout = getContainerLayout(activeContainer);
        if (layout == null || !layout.isVisible) return false;

        return checkActiveContainerSlotHover(slot, mouseX, mouseY, layout);
    }

    private boolean checkSlotHover(int mouseX, int mouseY, int slotX, int slotY) {
        return isHovering(mouseX, mouseY, slotX, slotY, SLOT_SIZE, SLOT_SIZE);
    }

    private boolean checkActiveContainerSlotHover(net.minecraft.inventory.Slot slot, int mouseX, int mouseY, ContainerLayout pos) {
        int gridWidth = SLOT_SIZE * SLOTS_PER_ROW;
        int startX = pos.x + (containerW - gridWidth) / 2;
        int startY = pos.y + 18;

        int slotIndex = slot.getSlotIndex();
        int storageSlotIndex = slotIndex - 9;

        SContainer activeContainer = containers.get(StorageManager.getActiveContainerId());
        if (storageSlotIndex < 0 || storageSlotIndex >= activeContainer.slotCount) {
            return false;
        }

        int col = storageSlotIndex % SLOTS_PER_ROW;
        int row = storageSlotIndex / SLOTS_PER_ROW;
        int xPos = startX + (col * SLOT_SIZE);
        int yPos = startY + (row * SLOT_SIZE);

        return isHovering(mouseX, mouseY, xPos, yPos, SLOT_SIZE, SLOT_SIZE);
    }

    private static class ContainerLayout {
        final int x, y, width, height;
        final boolean isVisible;

        ContainerLayout(int x, int y, int width, int height, boolean isVisible) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.isVisible = isVisible;
        }
    }
}
