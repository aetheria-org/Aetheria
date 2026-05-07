package com.jef.justenoughfakepixel.features.misc;

import com.jef.justenoughfakepixel.core.JefConfig;
import com.jef.justenoughfakepixel.features.storage.StorageManager;
import com.jef.justenoughfakepixel.init.RegisterEvents;
import com.jef.justenoughfakepixel.utils.CalculatorUtils;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.*;

@RegisterEvents
public class SearchBar {

    private static final Minecraft MC = Minecraft.getMinecraft();

    private static final Set<Character> CALC_SYMBOLS = new HashSet<>(Arrays.asList('+', '-', '*', '/', 'x', '(', ')'));

    private static final SearchBar INSTANCE = new SearchBar();
    private static final int BAR_WIDTH = 170;
    private static final int BAR_HEIGHT = 20;
    private static GuiTextField searchBar;
    private static String searchText = "";
    private static String lastCalcInput = "";
    private static String lastCalcResult = null;

    private static GuiTextField storageSearchBar;
    @Getter
    private static String storageSearchText = "";

    public static SearchBar getInstance() {
        return INSTANCE;
    }

    public static String getSearchText() {
        return isCalcMode() ? "" : searchText;
    }

    public static boolean isCalcMode() {
        for (char c : searchText.toCharArray())
            if (CALC_SYMBOLS.contains(c)) return true;
        return false;
    }

    public static GuiTextField createStorageSearchBar(int x, int y, int width) {
        storageSearchBar = new GuiTextField(1, MC.fontRendererObj, x, y, width, BAR_HEIGHT);
        storageSearchBar.setCanLoseFocus(true);
        storageSearchBar.setMaxStringLength(50);
        storageSearchBar.setEnableBackgroundDrawing(false);
        storageSearchBar.setFocused(false);
        storageSearchBar.setText(storageSearchText);
        return storageSearchBar;
    }

    public static void drawStorageSearchBar(GuiTextField field) {
        if (field == null) return;
        com.jef.justenoughfakepixel.core.config.utils.RenderUtils.drawSearchBar(field, true);
        storageSearchText = field.getText();
    }

    public static boolean handleStorageKeyTyped(GuiTextField field, char typedChar, int keyCode) {
        if (field == null || !field.isFocused()) return false;
        boolean consumed = field.textboxKeyTyped(typedChar, keyCode);
        storageSearchText = field.getText();
        return consumed;
    }

    public static boolean handleStorageMouseClick(GuiTextField field, int mouseX, int mouseY) {
        if (field == null) return false;

        boolean inside = mouseX >= field.xPosition && mouseX <= field.xPosition + field.width &&
                mouseY >= field.yPosition && mouseY <= field.yPosition + field.height;

        field.setFocused(inside);
        if (inside) {
            field.mouseClicked(mouseX, mouseY, 0);
        }

        return inside;
    }

    private static boolean isEnabled() {
        return JefConfig.feature != null && JefConfig.feature.misc.searchBarConfig.searchBar;
    }

    private static boolean isSupportedGui(Object gui) {
        return gui instanceof GuiInventory || gui instanceof GuiChest;
    }

    private static void drawSearchBar(GuiTextField field, String text) {
        String suffix = calcSuffix(text);
        boolean useGoldTexture = suffix != null;

        if (useGoldTexture) {
            int x = field.xPosition, y = field.yPosition;
            int w = field.width, h = field.height;

            GlStateManager.color(1f, 1f, 1f, 1f);
            com.jef.justenoughfakepixel.core.config.utils.RenderUtils.drawSearchBar(createTempFieldWithText(field, text + " " + suffix), true, true);
        } else {
            com.jef.justenoughfakepixel.core.config.utils.RenderUtils.drawSearchBar(field, true, false);
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
        if (text == null || text.isEmpty()) return null;
        if (!text.equals(lastCalcInput)) {
            lastCalcInput = text;
            lastCalcResult = CalculatorUtils.calculateAndFormat(text);
        }
        return lastCalcResult == null ? null : "§e= §a" + lastCalcResult;
    }

    public int getOverlayWidth() {
        return BAR_WIDTH;
    }

    public int getOverlayHeight() {
        return BAR_HEIGHT;
    }

    public void render(boolean preview) {
        ScaledResolution sr = new ScaledResolution(MC);
        com.jef.justenoughfakepixel.core.config.utils.Position pos = JefConfig.feature.misc.searchBarConfig.searchBarPos;
        int x = pos.getAbsX(sr, BAR_WIDTH);
        int y = pos.getAbsY(sr, BAR_HEIGHT);
        if (pos.isCenterX()) x -= BAR_WIDTH / 2;
        if (pos.isCenterY()) y -= BAR_HEIGHT / 2;

        Gui.drawRect(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, 0xFF2C2C2C);
        Gui.drawRect(x + 1, y + 1, x + BAR_WIDTH - 1, y + BAR_HEIGHT - 1, 0xFF111111);
        MC.fontRendererObj.drawStringWithShadow("Search...", x + 5, y + (float) BAR_HEIGHT / 2 - 4, 0x8F8F8F);
    }

    @SubscribeEvent
    public void onGuiInit(GuiScreenEvent.InitGuiEvent.Post event) {
        if (!isEnabled() || !isSupportedGui(event.gui)) return;

        // Enable keyboard repeat for text field
        Keyboard.enableRepeatEvents(true);

        int w = BAR_WIDTH, h = BAR_HEIGHT;

        ScaledResolution sr = new ScaledResolution(MC);
        com.jef.justenoughfakepixel.core.config.utils.Position pos = JefConfig.feature.misc.searchBarConfig.searchBarPos;
        int x = pos.getAbsX(sr, w);
        int y = pos.getAbsY(sr, h);
        if (pos.isCenterX()) x -= w / 2;
        if (pos.isCenterY()) y -= h / 2;

        searchBar = new GuiTextField(0, MC.fontRendererObj, x, y, w, h);
        searchBar.setCanLoseFocus(false);
        searchBar.setMaxStringLength(100);
        searchBar.setEnableBackgroundDrawing(false);
        searchBar.setFocused(false);
        searchBar.setText(searchText);
    }

    @SubscribeEvent
    public void onKeyboardInput(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        if (!isEnabled() || !(event.gui instanceof GuiContainer)) return;
        if (searchBar == null || !searchBar.isFocused()) return;

        // Only process key press and repeat events, not release
        if (!Keyboard.getEventKeyState()) return;

        char typedChar = Keyboard.getEventCharacter();
        int keyCode = Keyboard.getEventKey();

        if (keyCode == Keyboard.KEY_ESCAPE) return;

        if (searchBar.textboxKeyTyped(typedChar, keyCode)) {
            searchText = searchBar.getText();
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onMouseInput(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (!isEnabled() || !(event.gui instanceof GuiContainer)) return;
        if (searchBar == null || !Mouse.getEventButtonState()) return;

        int mouseX = Mouse.getEventX() * event.gui.width / MC.displayWidth;
        int mouseY = event.gui.height - Mouse.getEventY() * event.gui.height / MC.displayHeight - 1;

        boolean inside = mouseX >= searchBar.xPosition && mouseX <= searchBar.xPosition + searchBar.width && mouseY >= searchBar.yPosition && mouseY <= searchBar.yPosition + searchBar.height;

        searchBar.setFocused(inside);
        if (inside) searchBar.mouseClicked(mouseX, mouseY, Mouse.getEventButton());
    }

    @SubscribeEvent
    public void onDrawGui(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!isEnabled() || !isSupportedGui(event.gui) || searchBar == null) return;

        if (StorageManager.isOverlayActive()) {
            return;
        }

        searchBar.updateCursorCounter();
        drawSearchBar(searchBar, searchBar.getText());
    }
}
