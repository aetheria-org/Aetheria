package io.hamlook.aetheria.features.misc;

import io.hamlook.aetheria.Resources;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.storage.StorageManager;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.CalculatorUtils;
import io.hamlook.aetheria.utils.ContainerUtils;
import io.hamlook.aetheria.utils.KeybindHelper;
import io.hamlook.aetheria.utils.Position;
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
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@RegisterEvents
public class SearchBar {

    private static final Minecraft MC = Minecraft.getMinecraft();

    private static final Set<Character> CALC_SYMBOLS = new HashSet<>(Arrays.asList('+', '-', '*', '/', 'x', '(', ')'));

    private static final SearchBar INSTANCE = new SearchBar();
    private static final int BAR_WIDTH = 170;
    private static final int BAR_HEIGHT = 20;
    private static final int TOGGLE_BTN_W = 22;
    private static final int TOGGLE_BTN_GAP = 3;

    private static GuiTextField searchBar;
    private static String searchText = "";
    private static String lastCalcInput = "";
    private static String lastCalcResult = null;

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
        if (sendToItemList || isCalcMode()) return "";
        return searchText;
    }

    public static String getItemListSearchText() {
        if (!sendToItemList || isCalcMode()) return "";
        return searchText;
    }

    public static boolean isCalcMode() {
        if (sendToItemList) return false;
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
        if (sendToItemList || text == null || text.isEmpty() || CalculatorUtils.isPlainNumber(text)) return null;
        if (!text.equals(lastCalcInput)) {
            lastCalcInput = text;
            lastCalcResult = CalculatorUtils.calculateAndFormat(text);
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
            MC.fontRendererObj.drawStringWithShadow(icon, toggleBtnX + TOGGLE_BTN_W / 2f - MC.fontRendererObj.getStringWidth(icon) / 2f, toggleBtnY + BAR_HEIGHT / 2f - 4, 0xFFFFFF);
        }
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
        if (isEnabled() && event.gui instanceof GuiContainer && searchBar != null && searchBar.isFocused() && KeybindHelper.getEventKeyState()) {
            char typedChar = KeybindHelper.getEventCharacter();
            int keyCode = KeybindHelper.getEventKeyCode();

            if ((keyCode == KeybindHelper.KEY_RETURN || keyCode == KeybindHelper.KEY_NUMPADENTER) && isCalcMode() && lastCalcResult != null) {
                if (ATHRConfig.feature.misc.searchBarConfig.calcEnterCopyResult)
                    GuiScreen.setClipboardString(lastCalcResult);
                if (ATHRConfig.feature.misc.searchBarConfig.calcEnterClearText) {
                    searchText = lastCalcResult;
                    searchBar.setText(lastCalcResult);
                }
                event.setCanceled(true);
                return;
            }

            if (keyCode != KeybindHelper.KEY_ESCAPE && searchBar.textboxKeyTyped(typedChar, keyCode)) {
                searchText = searchBar.getText();
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onMouseInput(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (!isEnabled() || !(event.gui instanceof GuiContainer)) return;
        if (searchBar == null || !KeybindHelper.getEventButtonState()) return;

        int mouseX = KeybindHelper.getScaledEventX(event.gui.width);
        int mouseY = KeybindHelper.getScaledEventY(event.gui.height);

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
    public void onDrawGui(GuiContainerRenderBeforeTooltipEvent event) {
        if (isEnabled() && isSupportedGui(event.gui) && searchBar != null && !StorageManager.isOverlayActive()) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(-event.gui.guiLeft, -event.gui.guiTop, 50);
            searchBar.updateCursorCounter();
            drawSearchBar(searchBar);
            if (isItemListActive()) drawToggleButton(searchBar.xPosition, searchBar.yPosition);
            GlStateManager.popMatrix();
        }
    }
}
