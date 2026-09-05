package io.hamlook.aetheria.features.misc;

import io.hamlook.aetheria.Resources;
import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.events.*;
import io.hamlook.aetheria.features.storage.StorageManager;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.*;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.compat.ClipboardCompat;
import io.hamlook.aetheria.utils.compat.GlStateManagerCompat;
import io.hamlook.aetheria.utils.compat.GuiScreenUtils;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.render.RenderUtils;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;

import java.util.*;

@RegisterEvents
public class SearchBar {

    private static final Minecraft MC = MinecraftCompat.getMinecraft();

    private static final Set<Character> CALC_SYMBOLS = new HashSet<>(Arrays.asList('+', '-', '*', '/', 'x', '(', ')'));

    private static final SearchBar INSTANCE = new SearchBar();
    private static final int BAR_WIDTH = 170;
    private static final int BAR_HEIGHT = 20;
    private static final int TOGGLE_BTN_W = BAR_HEIGHT;
    private static final int TOGGLE_BTN_GAP = 3;
    private static final int CLEAR_BTN_W = BAR_HEIGHT;

    private static final int RECENT_MAX_VISIBLE = 3;
    private static final int RECENT_MAX_STORED = 20;
    private static final int RECENT_ROW_HEIGHT = 14;

    private static GuiTextField searchBar;
    private static String searchText = "";
    private static String lastCalcInput = "";
    private static String lastCalcResult = null;
    private static String lastCalcResultPlain = null;

    private static int clearBtnX, clearBtnY;

    private static final List<String> recentSearches = new ArrayList<>();
    private static int recentScrollOffset = 0;
    private static String lastRecentFilterText = null;
    private static int recentPanelX, recentPanelY, recentPanelW, recentPanelH;

    private static String hoveredItemName = null;

    private static GuiTextField storageSearchBar;
    @Getter
    private static String storageSearchText = "";

    private static int toggleBtnX, toggleBtnY;

    @Getter
    private static boolean sendToItemList = false;

    public static SearchBar getInstance() {
        return INSTANCE;
    }

    public static String getSearchText() {
        if (sendToItemList || isCalcMode() || isCommandMode()) return "";
        return searchText;
    }

    public static String getItemListSearchText() {
        if (!sendToItemList || isCalcMode() || isCommandMode()) return "";
        return searchText;
    }

    public static boolean isCommandMode() {
        return !sendToItemList && !searchText.isEmpty() && searchText.charAt(0) == '/';
    }

    public static boolean isCalcMode() {
        if (sendToItemList || isCommandMode()) return false;
        for (int i = 0; i < searchText.length(); i++)
            if (CALC_SYMBOLS.contains(searchText.charAt(i))) return true;
        return false;
    }

    public static GuiTextField createStorageSearchBar(int x, int y, int width) {
        storageSearchBar = new GuiTextField(1, MC.fontRendererObj, x, y, width, BAR_HEIGHT);
        storageSearchBar.setCanLoseFocus(true);
        storageSearchBar.setMaxStringLength(50);
        storageSearchBar.setEnableBackgroundDrawing(false);
        storageSearchBar.setFocused(false);
        if (ATHRConfig.feature != null && !ATHRConfig.feature.misc.searchBarConfig.persistStorageSearch)
            storageSearchText = "";
        storageSearchBar.setText(storageSearchText);
        return storageSearchBar;
    }

    public static void drawStorageSearchBar(GuiTextField field) {
        if (field == null) return;
        RenderUtils.drawSearchBar(field, true);
        storageSearchText = field.getText();
    }

    public static void drawStorageSearchBar(GuiTextField field, String[] textHolder) {
        if (field == null) return;
        RenderUtils.drawSearchBar(field, true);
        textHolder[0] = field.getText();
    }

    public static boolean handleStorageKeyTyped(GuiTextField field, char typedChar, int keyCode) {
        if (field == null || !field.isFocused()) return false;
        boolean consumed = field.textboxKeyTyped(typedChar, keyCode);
        storageSearchText = field.getText();
        return consumed;
    }

    public static boolean handleStorageKeyTyped(GuiTextField field, char typedChar, int keyCode, String[] textHolder) {
        if (field == null || !field.isFocused()) return false;
        boolean consumed = field.textboxKeyTyped(typedChar, keyCode);
        textHolder[0] = field.getText();
        return consumed;
    }

    public static boolean handleStorageMouseClick(GuiTextField field, int mouseX, int mouseY) {
        if (field == null) return false;
        boolean inside = mouseX >= field.xPosition && mouseX <= field.xPosition + field.width && mouseY >= field.yPosition && mouseY <= field.yPosition + field.height;
        field.setFocused(inside);
        if (inside) field.mouseClicked(mouseX, mouseY, 0);
        return inside;
    }

    private static boolean isEnabled() {
        return ATHRConfig.feature != null && ATHRConfig.feature.misc.searchBarConfig.searchBar;
    }

    private static boolean isSupportedGui(Object gui) {
        return gui instanceof GuiInventory || ContainerUtils.isChestOpen((net.minecraft.client.gui.GuiScreen) gui);
    }

    private static void drawSearchBar(GuiTextField field) {
        String text = field.getText();
        String suffix = calcSuffix(text);
        if (suffix != null) {
            RenderUtils.drawSearchBar(createTempFieldWithText(field, text + " " + suffix), true, true);
        } else {
            RenderUtils.drawSearchBar(field, true, false);
        }
    }

    private static GuiTextField createTempFieldWithText(GuiTextField original, String text) {
        GuiTextField temp = new GuiTextField(original.getId(), MC.fontRendererObj, original.xPosition, original.yPosition, original.width, original.height);
        temp.setText(text);
        temp.setFocused(original.isFocused());
        temp.setCursorPosition(original.getCursorPosition());
        return temp;
    }

    private static String calcSuffix(String text) {
        if (sendToItemList || isCommandMode() || text == null || text.isEmpty() || CalculatorUtils.isPlainNumber(text)) return null;
        if (!text.equals(lastCalcInput)) {
            lastCalcInput = text;
            lastCalcResult = CalculatorUtils.calculateAndFormat(text);
            lastCalcResultPlain = CalculatorUtils.calculateAndFormatPlain(text);
        }
        return lastCalcResult == null ? null : "§e= §a" + lastCalcResult;
    }

    private static boolean isItemListActive() {
        return ATHRConfig.feature != null && ATHRConfig.feature.misc.itemList.enabled && ATHRConfig.feature.misc.itemList.searchItemList;
    }

    private static int[] getMouseCoords() {
        return KeybindHelper.getMouseCoords(GuiScreenUtils.getScaledResolution());
    }

    private static void drawToggleButton(int barX, int barY) {
        toggleBtnX = barX + BAR_WIDTH + TOGGLE_BTN_GAP;
        toggleBtnY = barY;

        String tooltip = sendToItemList ? "§aSearch Item List" : "§aSearch Inventory & Calculator";
        RenderUtils.drawButton(toggleBtnX, toggleBtnY, TOGGLE_BTN_W, BAR_HEIGHT, tooltip, () -> {
            if (sendToItemList) {
                MC.getTextureManager().bindTexture(Resources.SEARCH_ICON);
                GlStateManagerCompat.color(1f, 1f, 1f, 1f);
                int size = 12;
                Gui.drawModalRectWithCustomSizedTexture(toggleBtnX + (TOGGLE_BTN_W - size) / 2, toggleBtnY + (BAR_HEIGHT - size) / 2, 0, 0, size, size, size, size);
            } else {
                String icon = "≡";
                MC.fontRendererObj.drawStringWithShadow(icon, toggleBtnX + TOGGLE_BTN_W / 2f - MC.fontRendererObj.getStringWidth(icon) / 2f, toggleBtnY + BAR_HEIGHT / 2f - 4, 0xFFFFFF);
            }
        });
    }

    private static void drawClearButton(int barX, int barY) {
        clearBtnX = barX - CLEAR_BTN_W - TOGGLE_BTN_GAP;
        clearBtnY = barY;

        RenderUtils.drawButton(clearBtnX, clearBtnY, CLEAR_BTN_W, BAR_HEIGHT, "§cClear Search", () -> {
            MC.fontRendererObj.drawStringWithShadow("✕", clearBtnX + CLEAR_BTN_W / 2f - MC.fontRendererObj.getStringWidth("✕") / 2f, clearBtnY + BAR_HEIGHT / 2f - 4, 0xFF5555);
        });
    }

    private static boolean isInsideClearButton(int mouseX, int mouseY) {
        return mouseX >= clearBtnX && mouseX < clearBtnX + CLEAR_BTN_W && mouseY >= clearBtnY && mouseY < clearBtnY + BAR_HEIGHT;
    }

    private static boolean isRecentSearchesEnabled() {
        return ATHRConfig.feature != null && ATHRConfig.feature.misc.searchBarConfig.recentSearchesEnabled;
    }

    private static void recordRecentSearch(String value) {
        if (!isRecentSearchesEnabled() || value == null || value.isEmpty()) return;
        recentSearches.remove(value);
        recentSearches.add(0, value);
        while (recentSearches.size() > RECENT_MAX_STORED) recentSearches.remove(recentSearches.size() - 1);
        recentScrollOffset = 0;
    }

    private static List<String> filteredSortedRecent() {
        String filter = searchText == null ? "" : searchText.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String entry : recentSearches) {
            if (filter.isEmpty() || entry.toLowerCase(Locale.ROOT).startsWith(filter)) matches.add(entry);
        }
        matches.sort(String.CASE_INSENSITIVE_ORDER);
        return matches;
    }

    private static void drawRecentSearches(int barX, int barY) {
        if (!isRecentSearchesEnabled() || searchBar == null || !searchBar.isFocused()) {
            recentPanelH = 0;
            return;
        }
        if (!searchText.equals(lastRecentFilterText)) {
            lastRecentFilterText = searchText;
            recentScrollOffset = 0;
        }

        List<String> matches = filteredSortedRecent();
        if (matches.isEmpty()) {
            recentPanelH = 0;
            return;
        }

        int visibleCount = Math.min(RECENT_MAX_VISIBLE, matches.size());
        recentScrollOffset = Math.max(0, Math.min(recentScrollOffset, matches.size() - visibleCount));
        recentPanelX = barX;
        recentPanelY = barY + BAR_HEIGHT;
        recentPanelW = BAR_WIDTH;
        recentPanelH = visibleCount * RECENT_ROW_HEIGHT;

        Gui.drawRect(recentPanelX, recentPanelY, recentPanelX + recentPanelW, recentPanelY + recentPanelH, 0xFF2C2C2C);
        Gui.drawRect(recentPanelX + 1, recentPanelY + 1, recentPanelX + recentPanelW - 1, recentPanelY + recentPanelH - 1, 0xFF111111);

        int[] mouse = getMouseCoords();
        int hoveredRow = hoveredRecentRow(mouse[0], mouse[1]);
        for (int row = 0; row < visibleCount; row++) {
            int index = recentScrollOffset + row;
            if (index >= matches.size()) break;

            int rowY = recentPanelY + row * RECENT_ROW_HEIGHT;
            if (row == hoveredRow) {
                Gui.drawRect(recentPanelX + 1, rowY, recentPanelX + recentPanelW - 1, rowY + RECENT_ROW_HEIGHT, 0x33FFFFFF);
            }

            String display = MC.fontRendererObj.trimStringToWidth(matches.get(index), recentPanelW - 10);
            MC.fontRendererObj.drawStringWithShadow(display, recentPanelX + 5, rowY + (RECENT_ROW_HEIGHT - 8) / 2f, 0xCCCCCC);
        }
    }

    private static boolean isInsideRecentPanel(int mouseX, int mouseY) {
        return recentPanelH > 0 && mouseX >= recentPanelX && mouseX < recentPanelX + recentPanelW && mouseY >= recentPanelY && mouseY < recentPanelY + recentPanelH;
    }

    private static int hoveredRecentRow(int mouseX, int mouseY) {
        if (!isInsideRecentPanel(mouseX, mouseY)) return -1;
        return (mouseY - recentPanelY) / RECENT_ROW_HEIGHT;
    }

    private static int[] calculateBarPosition(ScaledResolution sr) {
        Position pos = ATHRConfig.feature.misc.searchBarConfig.searchBarPos;
        int x = pos.getAbsX(sr, BAR_WIDTH);
        int y = pos.getAbsY(sr, BAR_HEIGHT);
        if (pos.isCenterX()) x -= BAR_WIDTH / 2;
        if (pos.isCenterY()) y -= BAR_HEIGHT / 2;
        return new int[]{x, y};
    }

    public int getOverlayWidth() {
        return BAR_WIDTH;
    }

    public int getOverlayHeight() {
        return BAR_HEIGHT;
    }

    public void render(boolean preview) {
        ScaledResolution sr = GuiScreenUtils.getScaledResolution();
        int[] pos = calculateBarPosition(sr);
        int x = pos[0], y = pos[1];

        Gui.drawRect(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, 0xFF2C2C2C);
        Gui.drawRect(x + 1, y + 1, x + BAR_WIDTH - 1, y + BAR_HEIGHT - 1, 0xFF111111);
        MC.fontRendererObj.drawStringWithShadow("Search...", x + 5, y + (float) BAR_HEIGHT / 2 - 4, 0x8F8F8F);
    }

    @HandleEvent
    public void onGuiInit(ASMGuiInitEvent event) {
        if (!isEnabled() || !isSupportedGui(event.gui)) return;

        KeybindHelper.enableRepeatEvents(true);

        ScaledResolution sr = GuiScreenUtils.getScaledResolution();
        int[] pos = calculateBarPosition(sr);

        searchBar = new GuiTextField(0, MC.fontRendererObj, pos[0], pos[1], BAR_WIDTH, BAR_HEIGHT);
        searchBar.setCanLoseFocus(false);
        searchBar.setMaxStringLength(100);
        searchBar.setEnableBackgroundDrawing(false);
        searchBar.setFocused(false);
        if (!ATHRConfig.feature.misc.searchBarConfig.persistSearchText) searchText = "";
        if (!isItemListActive()) sendToItemList = false;
        searchBar.setText(searchText);
    }

    @HandleEvent
    public void onKeyboardInput(ASMKeyEvent event) {
        if (!isEnabled() || !(event.gui instanceof GuiContainer) || searchBar == null || !KeybindHelper.getEventKeyState()) return;

        int keyCode = KeybindHelper.getEventKeyCode();

        if (!searchBar.isFocused()) {
            int hoverKey = ATHRConfig.feature.misc.searchBarConfig.hoverPasteKey;
            if (KeybindHelper.isKeyValid(hoverKey) && keyCode == hoverKey && hoveredItemName != null) {
                searchText = hoveredItemName;
                searchBar.setText(hoveredItemName);
                searchBar.setFocused(true);
                searchBar.setCursorPositionEnd();
                event.cancel();
            }
            return;
        }

        char typedChar = KeybindHelper.getEventCharacter();

        if (keyCode == ATHRConfig.feature.misc.searchBarConfig.submitKey) {
            if (isCommandMode()) {
                ChatUtils.sendChatCommand(searchText);
                recordRecentSearch(searchText);
                searchText = "";
                searchBar.setText("");
                event.cancel();
                return;
            }
            if (isCalcMode() && lastCalcResult != null) {
                if (ATHRConfig.feature.misc.searchBarConfig.calcEnterCopyResult)
                    ClipboardCompat.setClipboard(lastCalcResult);
                if (ATHRConfig.feature.misc.searchBarConfig.calcEnterClearText) {
                    searchText = lastCalcResultPlain;
                    searchBar.setText(lastCalcResultPlain);
                }
                recordRecentSearch(lastCalcResultPlain);
                event.cancel();
                return;
            }
            if (!isCalcMode() && !searchText.isEmpty()) {
                recordRecentSearch(searchText);
                event.cancel();
                return;
            }
        }

        if (keyCode != KeybindHelper.KEY_ESCAPE && searchBar.textboxKeyTyped(typedChar, keyCode)) {
            searchText = searchBar.getText();
            event.cancel();
        }
    }

    @HandleEvent
    public void onMouseInput(ASMMouseEvent event) {
        if (!isEnabled() || !(event.gui instanceof GuiContainer) || searchBar == null) return;

        int mouseX = KeybindHelper.getScaledEventX(event.gui.width);
        int mouseY = KeybindHelper.getScaledEventY(event.gui.height);

        int wheel = KeybindHelper.getEventDWheel();
        boolean insideRecentPanel = isInsideRecentPanel(mouseX, mouseY);
        List<String> recentMatches = insideRecentPanel ? filteredSortedRecent() : null;

        if (wheel != 0 && insideRecentPanel) {
            int maxOffset = Math.max(0, recentMatches.size() - RECENT_MAX_VISIBLE);
            recentScrollOffset = Math.max(0, Math.min(recentScrollOffset - Integer.signum(wheel), maxOffset));
            event.cancel();
            return;
        }

        if (!KeybindHelper.getEventButtonState()) return;

        if (KeybindHelper.getEventButton() == 0 && isInsideClearButton(mouseX, mouseY)) {
            searchText = "";
            searchBar.setText("");
            event.cancel();
            return;
        }

        if (KeybindHelper.getEventButton() == 0 && insideRecentPanel) {
            int index = recentScrollOffset + hoveredRecentRow(mouseX, mouseY);
            if (index >= 0 && index < recentMatches.size()) {
                String entry = recentMatches.get(index);
                searchText = entry;
                searchBar.setText(entry);
                searchBar.setCursorPositionEnd();
            }
            event.cancel();
            return;
        }

        boolean inside = mouseX >= searchBar.xPosition && mouseX <= searchBar.xPosition + searchBar.width && mouseY >= searchBar.yPosition && mouseY <= searchBar.yPosition + searchBar.height;

        searchBar.setFocused(inside);
        if (inside) {
            searchBar.mouseClicked(mouseX, mouseY, KeybindHelper.getEventButton());
        } else if (isItemListActive() && KeybindHelper.getEventButton() == 0 && mouseX >= toggleBtnX && mouseX < toggleBtnX + TOGGLE_BTN_W && mouseY >= toggleBtnY && mouseY < toggleBtnY + BAR_HEIGHT) {
            sendToItemList = !sendToItemList;
            event.cancel();
        }
    }

    @HandleEvent
    public void onItemTooltip(ASMTooltipEvent event) {
        if (!isEnabled() || event.itemStack == null) return;
        hoveredItemName = ColorUtils.stripColor(event.itemStack.getDisplayName()).trim();
    }

    @HandleEvent
    public void onDrawGui(GuiContainerRenderBeforeTooltipEvent event) {
        // Re-armed each frame; ItemTooltipEvent (fired later this same frame, per this event's
        // name) repopulates it only while an item is actually being hovered right now.
        hoveredItemName = null;

        if (isEnabled() && isSupportedGui(event.gui) && searchBar != null && !StorageManager.isOverlayActive()) {
            GlStateManagerCompat.pushMatrix();
            GlStateManagerCompat.translate(-event.gui.guiLeft, -event.gui.guiTop, 50);
            searchBar.updateCursorCounter();
            drawSearchBar(searchBar);
            drawClearButton(searchBar.xPosition, searchBar.yPosition);
            if (isItemListActive()) drawToggleButton(searchBar.xPosition, searchBar.yPosition);
            drawRecentSearches(searchBar.xPosition, searchBar.yPosition);
            GlStateManagerCompat.popMatrix();
        }
    }
}