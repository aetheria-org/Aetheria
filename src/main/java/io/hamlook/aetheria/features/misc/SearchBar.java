package io.hamlook.aetheria.features.misc;

import io.hamlook.aetheria.Resources;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.storage.StorageManager;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.CalculatorUtils;
import io.hamlook.aetheria.utils.ColorUtils;
import io.hamlook.aetheria.utils.ContainerUtils;
import io.hamlook.aetheria.utils.KeybindHelper;
import io.hamlook.aetheria.utils.Position;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.render.NineSliceUtils;
import io.hamlook.aetheria.utils.render.RenderUtils;
import io.hamlook.aetheria.utils.render.TextRenderUtils;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import io.hamlook.aetheria.events.GuiContainerRenderBeforeTooltipEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@RegisterEvents
public class SearchBar {

    private static final Minecraft MC = Minecraft.getMinecraft();

    private static final Set<Character> CALC_SYMBOLS = new HashSet<>(Arrays.asList('+', '-', '*', '/', 'x', '(', ')'));

    private static final SearchBar INSTANCE = new SearchBar();
    private static final int BAR_WIDTH = 170;
    private static final int BAR_HEIGHT = 20;
    private static final int TOGGLE_BTN_W = BAR_HEIGHT;
    private static final int TOGGLE_BTN_GAP = 3;
    private static final int CLEAR_BTN_W = BAR_HEIGHT;
    private static final float CLEAR_ICON_SCALE = 1.5f;
    // Hand-tuned against a screenshot: getStringWidth()/FONT_HEIGHT overestimate this glyph's
    // real rendered size, so the formula-centered position sits too far up-left. These nudge it
    // back toward true center; adjust and re-screenshot to refine.
    private static final float CLEAR_ICON_OFFSET_X = 3f;
    private static final float CLEAR_ICON_OFFSET_Y = 1f;

    private static final int RECENT_MAX_VISIBLE = 3;
    private static final int RECENT_MAX_STORED = 20;
    private static final int RECENT_ROW_HEIGHT = 14;

    private static GuiTextField searchBar;
    private static String searchText = "";
    private static String lastCalcInput = "";
    private static String lastCalcResult = null;
    /** Same value as {@link #lastCalcResult} but without the thousands separator, so applying it
     *  on Enter can't have its "," re-read as digit-grouping by the calculator. */
    private static String lastCalcResultPlain = null;

    private static int clearBtnX, clearBtnY;

    /** Session-only search/calculation history, most-recently-used first internally (so capacity
     *  eviction drops the oldest one) — never persisted across restarts. Displayed filtered by the
     *  current search text and alphabetically sorted; see {@link #filteredSortedRecent()}. */
    private static final List<String> recentSearches = new ArrayList<>();
    private static int recentScrollOffset = 0;
    private static String lastRecentFilterText = null;
    private static int recentPanelX, recentPanelY, recentPanelW, recentPanelH;

    /** Stripped display name of whatever item tooltip was drawn last frame, or null if none. */
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

    /** A leading "/" means this is a chat command, not a search — checked ahead of
     *  {@link #isCalcMode()} since "/" is also a calculator operator (division), and a command
     *  like "/warp home" would otherwise be misread as a broken calc expression. */
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
        return KeybindHelper.getMouseCoords(new ScaledResolution(MC));
    }

    private static void drawToggleButton(int barX, int barY) {
        toggleBtnX = barX + BAR_WIDTH + TOGGLE_BTN_GAP;
        toggleBtnY = barY;

        NineSliceUtils.draw(Resources.storageBackground(1), toggleBtnX, toggleBtnY, TOGGLE_BTN_W, BAR_HEIGHT, 6, 18);

        int[] mouse = getMouseCoords();
        boolean hovered = mouse[0] >= toggleBtnX && mouse[0] < toggleBtnX + TOGGLE_BTN_W && mouse[1] >= toggleBtnY && mouse[1] < toggleBtnY + BAR_HEIGHT;

        if (hovered) {
            Gui.drawRect(toggleBtnX, toggleBtnY, toggleBtnX + TOGGLE_BTN_W, toggleBtnY + BAR_HEIGHT, 0x33FFFFFF);
            if (sendToItemList) {
                TextRenderUtils.drawHoveringText("§aSearch Item List", mouse[0], mouse[1], MC.fontRendererObj);
            } else {
                TextRenderUtils.drawHoveringText("§aSearch Inventory & Calculator", mouse[0], mouse[1], MC.fontRendererObj);
            }
        }
        if (sendToItemList) {
            MC.getTextureManager().bindTexture(Resources.SEARCH_ICON);
            GlStateManager.color(1f, 1f, 1f, 1f);
            int size = 12;
            Gui.drawModalRectWithCustomSizedTexture(toggleBtnX + (TOGGLE_BTN_W - size) / 2, toggleBtnY + (BAR_HEIGHT - size) / 2, 0, 0, size, size, size, size);
        } else {
            String icon = "≡";
            MC.fontRendererObj.drawStringWithShadow(icon, toggleBtnX + TOGGLE_BTN_W / 2f - MC.fontRendererObj.getStringWidth(icon) / 2f + 0.5f, toggleBtnY + BAR_HEIGHT / 2f - 4, 0xFFFFFF);
        }
    }

    private static void drawClearButton(int barX, int barY) {
        clearBtnX = barX - CLEAR_BTN_W - TOGGLE_BTN_GAP;
        clearBtnY = barY;

        NineSliceUtils.draw(Resources.storageBackground(1), clearBtnX, clearBtnY, CLEAR_BTN_W, BAR_HEIGHT, 6, 18);

        int[] mouse = getMouseCoords();
        boolean hovered = mouse[0] >= clearBtnX && mouse[0] < clearBtnX + CLEAR_BTN_W && mouse[1] >= clearBtnY && mouse[1] < clearBtnY + BAR_HEIGHT;

        if (hovered) {
            Gui.drawRect(clearBtnX, clearBtnY, clearBtnX + CLEAR_BTN_W, clearBtnY + BAR_HEIGHT, 0x33FFFFFF);
            TextRenderUtils.drawHoveringText("§cClear Search", mouse[0], mouse[1], MC.fontRendererObj);
        }

        String icon = "✕";
        float iconW = MC.fontRendererObj.getStringWidth(icon) * CLEAR_ICON_SCALE;
        float iconH = MC.fontRendererObj.FONT_HEIGHT * CLEAR_ICON_SCALE;
        float px = clearBtnX + CLEAR_BTN_W / 2f - iconW / 2f + CLEAR_ICON_OFFSET_X;
        float py = clearBtnY + BAR_HEIGHT / 2f - iconH / 2f + CLEAR_ICON_OFFSET_Y;

        GlStateManager.pushMatrix();
        GlStateManager.translate(px, py, 0);
        GlStateManager.scale(CLEAR_ICON_SCALE, CLEAR_ICON_SCALE, 1f);
        MC.fontRendererObj.drawStringWithShadow(icon, 0, 0, 0xFF5555);
        GlStateManager.popMatrix();
    }

    private static boolean isInsideClearButton(int mouseX, int mouseY) {
        return mouseX >= clearBtnX && mouseX < clearBtnX + CLEAR_BTN_W && mouseY >= clearBtnY && mouseY < clearBtnY + BAR_HEIGHT;
    }

    private static boolean isRecentSearchesEnabled() {
        return ATHRConfig.feature != null && ATHRConfig.feature.misc.searchBarConfig.recentSearchesEnabled;
    }

    /** Records a completed search/calc-result, bumping an existing identical entry to the top
     *  instead of duplicating it, and capping the stored history so it can't grow unbounded. */
    private static void recordRecentSearch(String value) {
        if (!isRecentSearchesEnabled() || value == null || value.isEmpty()) return;
        recentSearches.remove(value);
        recentSearches.add(0, value);
        while (recentSearches.size() > RECENT_MAX_STORED) recentSearches.remove(recentSearches.size() - 1);
        recentScrollOffset = 0;
    }

    /** History entries whose start matches the current search text (case-insensitive), alphabetically
     *  sorted — typing "k" shows every entry starting with "k", "ka" narrows that down further. */
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
        for (int row = 0; row < visibleCount; row++) {
            int index = recentScrollOffset + row;
            if (index >= matches.size()) break;

            int rowY = recentPanelY + row * RECENT_ROW_HEIGHT;
            boolean hovered = mouse[0] >= recentPanelX && mouse[0] < recentPanelX + recentPanelW && mouse[1] >= rowY && mouse[1] < rowY + RECENT_ROW_HEIGHT;
            if (hovered) {
                Gui.drawRect(recentPanelX + 1, rowY, recentPanelX + recentPanelW - 1, rowY + RECENT_ROW_HEIGHT, 0x33FFFFFF);
            }

            String display = MC.fontRendererObj.trimStringToWidth(matches.get(index), recentPanelW - 10);
            MC.fontRendererObj.drawStringWithShadow(display, recentPanelX + 5, rowY + (RECENT_ROW_HEIGHT - 8) / 2f, 0xCCCCCC);
        }
    }

    private static boolean isInsideRecentPanel(int mouseX, int mouseY) {
        return recentPanelH > 0 && mouseX >= recentPanelX && mouseX < recentPanelX + recentPanelW && mouseY >= recentPanelY && mouseY < recentPanelY + recentPanelH;
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
        ScaledResolution sr = new ScaledResolution(MC);
        int[] pos = calculateBarPosition(sr);
        int x = pos[0], y = pos[1];

        Gui.drawRect(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, 0xFF2C2C2C);
        Gui.drawRect(x + 1, y + 1, x + BAR_WIDTH - 1, y + BAR_HEIGHT - 1, 0xFF111111);
        MC.fontRendererObj.drawStringWithShadow("Search...", x + 5, y + (float) BAR_HEIGHT / 2 - 4, 0x8F8F8F);
    }

    @SubscribeEvent
    public void onGuiInit(GuiScreenEvent.InitGuiEvent.Post event) {
        if (!isEnabled() || !isSupportedGui(event.gui)) return;

        KeybindHelper.enableRepeatEvents(true);

        ScaledResolution sr = new ScaledResolution(MC);
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

    @SubscribeEvent
    public void onKeyboardInput(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        if (!isEnabled() || !(event.gui instanceof GuiContainer) || searchBar == null || !KeybindHelper.getEventKeyState()) return;

        int keyCode = KeybindHelper.getEventKeyCode();

        if (!searchBar.isFocused()) {
            int hoverKey = ATHRConfig.feature.misc.searchBarConfig.hoverPasteKey;
            if (hoverKey != Keyboard.KEY_NONE && keyCode == hoverKey && hoveredItemName != null) {
                searchText = hoveredItemName;
                searchBar.setText(hoveredItemName);
                searchBar.setFocused(true);
                searchBar.setCursorPositionEnd();
                event.setCanceled(true);
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
                event.setCanceled(true);
                return;
            }
            if (isCalcMode() && lastCalcResult != null) {
                if (ATHRConfig.feature.misc.searchBarConfig.calcEnterCopyResult)
                    GuiScreen.setClipboardString(lastCalcResult);
                if (ATHRConfig.feature.misc.searchBarConfig.calcEnterClearText) {
                    searchText = lastCalcResultPlain;
                    searchBar.setText(lastCalcResultPlain);
                }
                recordRecentSearch(lastCalcResultPlain);
                event.setCanceled(true);
                return;
            }
            if (!isCalcMode() && !searchText.isEmpty()) {
                recordRecentSearch(searchText);
                event.setCanceled(true);
                return;
            }
        }

        if (keyCode != KeybindHelper.KEY_ESCAPE && searchBar.textboxKeyTyped(typedChar, keyCode)) {
            searchText = searchBar.getText();
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onMouseInput(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (!isEnabled() || !(event.gui instanceof GuiContainer) || searchBar == null) return;

        int mouseX = KeybindHelper.getScaledEventX(event.gui.width);
        int mouseY = KeybindHelper.getScaledEventY(event.gui.height);

        int wheel = KeybindHelper.getEventDWheel();
        if (wheel != 0 && isInsideRecentPanel(mouseX, mouseY)) {
            int maxOffset = Math.max(0, filteredSortedRecent().size() - RECENT_MAX_VISIBLE);
            recentScrollOffset = Math.max(0, Math.min(recentScrollOffset - Integer.signum(wheel), maxOffset));
            event.setCanceled(true);
            return;
        }

        if (!KeybindHelper.getEventButtonState()) return;

        if (KeybindHelper.getEventButton() == 0 && isInsideClearButton(mouseX, mouseY)) {
            searchText = "";
            searchBar.setText("");
            event.setCanceled(true);
            return;
        }

        if (KeybindHelper.getEventButton() == 0 && isInsideRecentPanel(mouseX, mouseY)) {
            List<String> matches = filteredSortedRecent();
            int index = recentScrollOffset + (mouseY - recentPanelY) / RECENT_ROW_HEIGHT;
            if (index >= 0 && index < matches.size()) {
                String entry = matches.get(index);
                searchText = entry;
                searchBar.setText(entry);
                searchBar.setCursorPositionEnd();
            }
            event.setCanceled(true);
            return;
        }

        boolean inside = mouseX >= searchBar.xPosition && mouseX <= searchBar.xPosition + searchBar.width && mouseY >= searchBar.yPosition && mouseY <= searchBar.yPosition + searchBar.height;

        searchBar.setFocused(inside);
        if (inside) {
            searchBar.mouseClicked(mouseX, mouseY, KeybindHelper.getEventButton());
        } else if (isItemListActive() && KeybindHelper.getEventButton() == 0 && mouseX >= toggleBtnX && mouseX < toggleBtnX + TOGGLE_BTN_W && mouseY >= toggleBtnY && mouseY < toggleBtnY + BAR_HEIGHT) {
            sendToItemList = !sendToItemList;
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onItemTooltip(ItemTooltipEvent event) {
        if (!isEnabled() || event.itemStack == null) return;
        hoveredItemName = ColorUtils.stripColor(event.itemStack.getDisplayName()).trim();
    }

    @SubscribeEvent
    public void onDrawGui(GuiContainerRenderBeforeTooltipEvent event) {
        // Re-armed each frame; ItemTooltipEvent (fired later this same frame, per this event's
        // name) repopulates it only while an item is actually being hovered right now.
        hoveredItemName = null;

        if (isEnabled() && isSupportedGui(event.gui) && searchBar != null && !StorageManager.isOverlayActive()) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(-event.gui.guiLeft, -event.gui.guiTop, 50);
            searchBar.updateCursorCounter();
            drawSearchBar(searchBar);
            drawClearButton(searchBar.xPosition, searchBar.yPosition);
            if (isItemListActive()) drawToggleButton(searchBar.xPosition, searchBar.yPosition);
            drawRecentSearches(searchBar.xPosition, searchBar.yPosition);
            GlStateManager.popMatrix();
        }
    }
}
