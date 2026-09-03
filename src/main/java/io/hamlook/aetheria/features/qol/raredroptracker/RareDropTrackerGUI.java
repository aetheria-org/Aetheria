package io.hamlook.aetheria.features.qol.raredroptracker;

import io.hamlook.aetheria.Resources;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.features.qol.RareDropTrackerConfig;
import io.hamlook.aetheria.core.moulconfig.gui.GuiElement;
import io.hamlook.aetheria.features.misc.itemList.ItemRegistry;
import io.hamlook.aetheria.features.misc.itemList.SkyblockItem;
import io.hamlook.aetheria.utils.ColorUtils;
import io.hamlook.aetheria.utils.KeybindHelper;
import io.hamlook.aetheria.utils.compat.*;
import io.hamlook.aetheria.utils.render.NineSliceUtils;
import io.hamlook.aetheria.utils.render.TextRenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.input.Keyboard;

import java.util.*;

public class RareDropTrackerGUI extends GuiElement {

    private static final int PAD = 10;
    private static final int TITLE_H = 20;
    private static final int SF_H = 16;
    private static final int ROW_H = 14;
    private static final int BTN_W = 42;
    private static final int SUGGESTION_H = 12;
    private static final int MIN_QUERY_LENGTH = 3;
    private static final int MAX_SUGGESTIONS = 6;
    private static final int MAX_W = 320;
    private static final int MAX_H = 260;
    private static final int GOAL_STEP = 1;
    private static final int GOAL_STEP_SHIFT = 10;
    private static final int MIN_EDIT_FIELD_W = 34;

    private static List<String> allNamesCache = null;
    private static Map<String, SkyblockItem> nameToItemCache = null;

    private final GuiScreen parentScreen;
    private final List<String> filteredNames = new ArrayList<>();
    private GuiTextField searchField;
    private String message = "";
    private int messageColor = 0xAAAAAA;
    private int scrollOffset = 0;

    // Inline row editing (click the goal count to type it, or [CMD] to bind a command)
    private String editingItemId = null;
    private String editingField = null; // "goal" or "command"
    private GuiTextField editField;

    private int px, py, pw, ph;

    public RareDropTrackerGUI() {
        this.parentScreen = MinecraftCompat.getMinecraft().currentScreen;
        buildCacheIfNeeded();
        if (allNamesCache == null) {
            message = "Item database is still loading, hang on...";
            messageColor = 0xFFFF55;
        }
    }

    private static void buildCacheIfNeeded() {
        if (allNamesCache != null) return;
        if (!ItemRegistry.isLoaded) return;

        Map<String, SkyblockItem> byName = new LinkedHashMap<>();
        for (SkyblockItem item : ItemRegistry.getAllItems()) {
            if (item.displayName == null || item.displayName.trim().isEmpty()) continue;
            String clean = ColorUtils.stripColor(item.displayName).trim();
            if (clean.isEmpty()) continue;
            byName.putIfAbsent(clean.toLowerCase(), item);
        }

        List<String> names = new ArrayList<>();
        for (SkyblockItem item : byName.values()) {
            names.add(ColorUtils.stripColor(item.displayName).trim());
        }
        names.sort(Comparator.naturalOrder());

        nameToItemCache = byName;
        allNamesCache = names;
    }

    private void updatePanel(ScaledResolution sr) {
        pw = Math.min(MAX_W, sr.getScaledWidth() - PAD * 2);
        ph = Math.min(MAX_H, sr.getScaledHeight() - PAD * 2);
        px = (sr.getScaledWidth() - pw) / 2;
        py = (sr.getScaledHeight() - ph) / 2;
    }

    private void ensureSearchField(int x, int y, int w, FontRenderer fr) {
        if (searchField == null) {
            searchField = new GuiTextField(0, fr, x, y, w, SF_H);
            searchField.setMaxStringLength(64);
        }
    }

    private void updateSuggestions() {
        buildCacheIfNeeded();
        filteredNames.clear();
        if (allNamesCache == null || searchField == null) return;

        String query = searchField.getText().trim().toLowerCase();
        if (query.length() < MIN_QUERY_LENGTH) return;

        for (String name : allNamesCache) {
            if (name.toLowerCase().contains(query)) {
                filteredNames.add(name);
                if (filteredNames.size() >= 200) break;
            }
        }
    }

    private void addTrackedItem(String name) {
        buildCacheIfNeeded();
        if (nameToItemCache == null) {
            message = "Item database is still loading, hang on...";
            messageColor = 0xFFFF55;
            return;
        }

        SkyblockItem item = nameToItemCache.get(name.trim().toLowerCase());
        if (item == null) {
            message = "No exact match, pick a suggestion from the list";
            messageColor = 0xFF5555;
            return;
        }

        RareDropTrackerConfig config = ATHRConfig.feature.qol.rareDropTracker;
        String id = item.skyblockID.toLowerCase();
        if (config.trackedItems.containsKey(id)) {
            message = "Already tracking " + ColorUtils.stripColor(item.displayName);
            messageColor = 0xFFFF55;
            return;
        }

        String displayName = item.displayName != null ? item.displayName : item.skyblockID;
        config.trackedItems.put(id, new RareDropTrackerConfig.TrackedItem(displayName));
        ATHRConfig.saveConfig();
        message = "Now tracking " + ColorUtils.stripColor(item.displayName);
        messageColor = 0x55FF55;
        searchField.setText("");
        filteredNames.clear();
    }

    private void removeTrackedItem(String id, String displayName) {
        RareDropTrackerConfig config = ATHRConfig.feature.qol.rareDropTracker;
        config.trackedItems.remove(id);
        ATHRConfig.saveConfig();
        message = "Stopped tracking " + ColorUtils.stripColor(displayName);
        messageColor = 0xAAAAAA;
    }

    private void adjustGoal(RareDropTrackerConfig.TrackedItem item, int delta) {
        item.goal = Math.max(0, item.goal + delta);
        ATHRConfig.markConfigDirty();
    }

    private void resetCount(RareDropTrackerConfig.TrackedItem item) {
        item.count = 0;
        ATHRConfig.markConfigDirty();
        message = "Reset progress for " + ColorUtils.stripColor(item.displayName);
        messageColor = 0xAAAAAA;
    }

    private void startEditGoal(String id, RareDropTrackerConfig.TrackedItem item) {
        if (editingItemId != null) commitEdit();
        editingItemId = id;
        editingField = "goal";
        editField = new GuiTextField(1, MinecraftCompat.getMinecraft().fontRendererObj, 0, 0, 0, 0);
        editField.setMaxStringLength(10);
        editField.setText(String.valueOf(item.goal));
        selectAllInEditField();
        editField.setFocused(true);
        if (searchField != null) searchField.setFocused(false);
    }

    private void startEditCommand(String id, RareDropTrackerConfig.TrackedItem item) {
        if (editingItemId != null) commitEdit();
        editingItemId = id;
        editingField = "command";
        editField = new GuiTextField(2, MinecraftCompat.getMinecraft().fontRendererObj, 0, 0, 0, 0);
        editField.setMaxStringLength(64);
        editField.setText(item.command != null ? item.command : "");
        selectAllInEditField();
        editField.setFocused(true);
        if (searchField != null) searchField.setFocused(false);
    }

    // Selects the whole pre-filled value so the first keystroke overwrites it
    // instead of just being inserted at the end (e.g. typing "13" over an
    // existing "12" replaces it -> "13", instead of appending -> "1213").
    private void selectAllInEditField() {
        editField.setCursorPosition(0);
        editField.setSelectionPos(editField.getText().length());
    }

    private void commitEdit() {
        if (editingItemId == null) return;
        RareDropTrackerConfig config = ATHRConfig.feature.qol.rareDropTracker;
        RareDropTrackerConfig.TrackedItem item = config.trackedItems.get(editingItemId);
        if (item != null && editField != null) {
            if ("goal".equals(editingField)) {
                try {
                    item.goal = Math.max(0, Integer.parseInt(editField.getText().trim()));
                } catch (NumberFormatException ignored) {
                    // keep the previous goal if the typed value isn't a valid number
                }
            } else if ("command".equals(editingField)) {
                String text = editField.getText().trim();
                if (text.isEmpty()) {
                    item.command = null;
                } else {
                    item.command = text.startsWith("/") ? text : "/" + text;
                }
            }
            ATHRConfig.markConfigDirty();
        }
        cancelEdit();
    }

    private void cancelEdit() {
        editingItemId = null;
        editingField = null;
        editField = null;
    }

    private int listTop(int addY) {
        return addY + SF_H + 6 + 12 + 6 + 12;
    }

    @Override
    public void render() {
        Minecraft mc = MinecraftCompat.getMinecraft();
        ScaledResolution sr = GuiScreenUtils.getScaledResolution();
        FontRenderer fr = mc.fontRendererObj;
        updatePanel(sr);

        Gui.drawRect(0, 0, sr.getScaledWidth(), sr.getScaledHeight(), 0xaa050508);

        GlStateManagerCompat.color(0.18f, 0.18f, 0.18f, 1f);
        NineSliceUtils.draw(Resources.storageBackground(1), px, py, pw, ph, 6, 18);
        GlStateManagerCompat.color(1f, 1f, 1f, 1f);

        int curY = py + PAD;
        fr.drawStringWithShadow(EnumChatFormatting.LIGHT_PURPLE + "" + EnumChatFormatting.BOLD + "Rare Drop Tracker", px + PAD, curY + 5, -1);
        String escHint = EnumChatFormatting.DARK_GRAY + "ESC";
        fr.drawStringWithShadow(escHint, px + pw - fr.getStringWidth(escHint) - PAD, curY + 5, -1);
        Gui.drawRect(px + PAD, curY + TITLE_H, px + pw - PAD, curY + TITLE_H + 1, 0xff252535);
        curY += TITLE_H + PAD;

        fr.drawStringWithShadow(EnumChatFormatting.GRAY + "Type " + MIN_QUERY_LENGTH + "+ letters to search:", px + PAD, curY, -1);
        curY += 10;

        int sfW = pw - PAD * 2 - BTN_W - 6;
        ensureSearchField(px + PAD, curY, sfW, fr);
        searchField.xPosition = px + PAD;
        searchField.yPosition = curY;
        searchField.width = sfW;
        searchField.height = SF_H;
        searchField.drawTextBox();

        int addX = px + pw - PAD - BTN_W;
        drawBtn(addX, curY, EnumChatFormatting.WHITE + "Add", fr, isHovered(addX, curY));
        curY += SF_H + 6;

        if (!message.isEmpty()) {
            fr.drawStringWithShadow(message, px + PAD, curY, messageColor);
        }
        curY += 12;

        Gui.drawRect(px + PAD, curY, px + pw - PAD, curY + 1, 0xff252535);
        curY += 6;

        fr.drawStringWithShadow(EnumChatFormatting.GRAY + "Tracked items: §8(click count to type a goal, CMD to set a command, e.g. /echest 5 or /storage 3)", px + PAD, curY, -1);
        curY += 12;

        RareDropTrackerConfig config = ATHRConfig.feature.qol.rareDropTracker;
        List<Map.Entry<String, RareDropTrackerConfig.TrackedItem>> entries = new ArrayList<>(config.trackedItems.entrySet());
        int listTop = curY;
        int listBottom = py + ph - PAD;

        if (entries.isEmpty()) {
            fr.drawStringWithShadow(EnumChatFormatting.DARK_GRAY + "Nothing tracked yet.", px + PAD, listTop, -1);
        } else {
            int[] mouse = KeybindHelper.getMouseCoords(sr);
            int rowY = listTop - scrollOffset;
            for (Map.Entry<String, RareDropTrackerConfig.TrackedItem> e : entries) {
                if (rowY + ROW_H > listTop && rowY < listBottom) {
                    drawTrackedRow(fr, rowY, e.getKey(), e.getValue(), mouse);
                }
                rowY += ROW_H;
            }
        }

        if (searchField.isFocused() && !filteredNames.isEmpty()) {
            drawSuggestions(fr);
        }
    }

    private void drawTrackedRow(FontRenderer fr, int rowY, String id, RareDropTrackerConfig.TrackedItem item, int[] mouse) {
        int rowRight = px + pw - PAD;

        String delStr = "§c[X]";
        int delW = fr.getStringWidth(delStr);
        int delX = rowRight - delW;

        String plusStr = "§a[+]";
        int plusW = fr.getStringWidth(plusStr);
        int plusX = delX - 4 - plusW;

        String minusStr = "§c[-]";
        int minusW = fr.getStringWidth(minusStr);
        int minusX = plusX - 3 - minusW;

        String resetStr = "§7[R]";
        int resetW = fr.getStringWidth(resetStr);
        int resetX = minusX - 3 - resetW;

        boolean hasCommand = item.command != null && !item.command.isEmpty();
        String cmdStr = hasCommand ? "§a[CMD]" : "§8[CMD]";
        int cmdW = fr.getStringWidth(cmdStr);
        int cmdX = resetX - 4 - cmdW;

        String progress = item.goal > 0 ? (item.count + "§8/§7" + item.goal) : String.valueOf(item.count);
        String progressStr = "§b" + progress;
        int progW = Math.max(fr.getStringWidth(progressStr), MIN_EDIT_FIELD_W);
        int progX = cmdX - 6 - progW;

        int nameMaxWidth = progX - 4 - (px + PAD);

        boolean editingGoal = id.equals(editingItemId) && "goal".equals(editingField);
        boolean editingCmd = id.equals(editingItemId) && "command".equals(editingField);

        if (editingCmd) {
            positionEditField(px + PAD, rowY - 1, nameMaxWidth);
            editField.drawTextBox();
        } else {
            String rawName = item.displayName != null ? item.displayName : "";
            String name = fr.trimStringToWidth(rawName, Math.max(10, nameMaxWidth));
            fr.drawStringWithShadow(EnumChatFormatting.WHITE + name, px + PAD, rowY, -1);
        }

        if (editingGoal) {
            positionEditField(progX, rowY - 1, progW);
            editField.drawTextBox();
        } else {
            fr.drawStringWithShadow(progressStr, progX, rowY, -1);
        }

        drawBracketBtn(cmdStr, cmdX, rowY, cmdW, mouse, fr);
        drawBracketBtn(resetStr, resetX, rowY, resetW, mouse, fr);
        drawBracketBtn(minusStr, minusX, rowY, minusW, mouse, fr);
        drawBracketBtn(plusStr, plusX, rowY, plusW, mouse, fr);
        drawBracketBtn(delStr, delX, rowY, delW, mouse, fr);
    }

    private void positionEditField(int x, int y, int w) {
        editField.xPosition = x;
        editField.yPosition = y;
        editField.width = Math.max(MIN_EDIT_FIELD_W, w);
        editField.height = ROW_H - 1;
    }

    private void drawBracketBtn(String label, int x, int y, int w, int[] mouse, FontRenderer fr) {
        boolean hovered = inBounds(mouse[0], mouse[1], x, y - 1, w, ROW_H);
        if (hovered) {
            Gui.drawRect(x - 1, y - 1, x + w + 1, y + ROW_H - 2, 0x33FFFFFF);
        }
        fr.drawStringWithShadow(label, x, y, -1);
    }

    private void drawSuggestions(FontRenderer fr) {
        int dropY = searchField.yPosition + searchField.height;
        int count = Math.min(filteredNames.size(), MAX_SUGGESTIONS);
        int[] mouse = KeybindHelper.getMouseCoords(GuiScreenUtils.getScaledResolution());

        Gui.drawRect(searchField.xPosition - 1, dropY, searchField.xPosition + searchField.width + 1, dropY + count * SUGGESTION_H + 1, 0xFFAAAAAA);
        Gui.drawRect(searchField.xPosition, dropY, searchField.xPosition + searchField.width, dropY + count * SUGGESTION_H, 0xE6000000);

        for (int i = 0; i < count; i++) {
            int itemTop = dropY + i * SUGGESTION_H;
            boolean hovered = inBounds(mouse[0], mouse[1], searchField.xPosition, itemTop, searchField.width, SUGGESTION_H);
            if (hovered) {
                Gui.drawRect(searchField.xPosition, itemTop, searchField.xPosition + searchField.width, itemTop + SUGGESTION_H, 0x44FFFFFF);
            }
            fr.drawStringWithShadow(filteredNames.get(i), searchField.xPosition + 3, itemTop + 2, hovered ? 0xFFFFA0 : 0xFFFFFF);
        }
    }

    private void drawBtn(int x, int y, String label, FontRenderer fr, boolean hov) {
        Gui.drawRect(x, y, x + BTN_W, y + SF_H, hov ? 0xff282830 : 0xff1a1a22);
        Gui.drawRect(x, y, x + BTN_W, y + 1, hov ? 0xff505060 : 0xff303038);
        Gui.drawRect(x, y + SF_H - 1, x + BTN_W, y + SF_H, 0xff0a0a0e);
        TextRenderUtils.drawStringCenteredScaledMaxWidth(label, fr, x + BTN_W / 2f, y + SF_H / 2f + 1, false, BTN_W - 4, -1);
    }

    @Override
    public boolean mouseInput(int mouseX, int mouseY) {
        int dWheel = MouseCompat.getEventDWheel();
        if (dWheel != 0) {
            RareDropTrackerConfig config = ATHRConfig.feature.qol.rareDropTracker;
            int maxScroll = Math.max(0, config.trackedItems.size() * ROW_H - (ph - PAD * 2 - 90));
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (dWheel > 0 ? 20 : -20)));
            return false;
        }
        if (!MouseCompat.getEventButtonState() || MouseCompat.getEventButton() != 0) return false;

        if (editingItemId != null && editField != null) {
            if (inBounds(mouseX, mouseY, editField.xPosition, editField.yPosition, editField.width, editField.height)) {
                editField.mouseClicked(mouseX, mouseY, 0);
                return true;
            }
            // clicking away from the field saves it, then falls through so the click can still hit another button
            commitEdit();
        }

        if (searchField == null) return false;

        if (searchField.isFocused() && !filteredNames.isEmpty()) {
            int dropY = searchField.yPosition + searchField.height;
            int count = Math.min(filteredNames.size(), MAX_SUGGESTIONS);
            if (inBounds(mouseX, mouseY, searchField.xPosition, dropY, searchField.width, count * SUGGESTION_H)) {
                int index = (mouseY - dropY) / SUGGESTION_H;
                searchField.setText(filteredNames.get(index));
                filteredNames.clear();
                return true;
            }
        }

        searchField.mouseClicked(mouseX, mouseY, 0);

        int addX = px + pw - PAD - BTN_W;
        int addY = searchField.yPosition;
        if (inBounds(mouseX, mouseY, addX, addY, BTN_W, SF_H)) {
            addTrackedItem(searchField.getText());
            return true;
        }

        RareDropTrackerConfig config = ATHRConfig.feature.qol.rareDropTracker;
        List<Map.Entry<String, RareDropTrackerConfig.TrackedItem>> entries = new ArrayList<>(config.trackedItems.entrySet());
        int listTop = listTop(addY);
        int listBottom = py + ph - PAD;

        boolean shift = KeyboardCompat.isKeyDown(Keyboard.KEY_LSHIFT) || KeyboardCompat.isKeyDown(Keyboard.KEY_RSHIFT);
        int step = shift ? GOAL_STEP_SHIFT : GOAL_STEP;

        FontRenderer fr = MinecraftCompat.getMinecraft().fontRendererObj;
        int rowY = listTop - scrollOffset;
        for (Map.Entry<String, RareDropTrackerConfig.TrackedItem> e : entries) {
            if (rowY + ROW_H > listTop && rowY < listBottom) {
                RareDropTrackerConfig.TrackedItem item = e.getValue();
                int rowRight = px + pw - PAD;

                int delW = fr.getStringWidth("[X]");
                int delX = rowRight - delW;

                int plusW = fr.getStringWidth("[+]");
                int plusX = delX - 4 - plusW;

                int minusW = fr.getStringWidth("[-]");
                int minusX = plusX - 3 - minusW;

                int resetW = fr.getStringWidth("[R]");
                int resetX = minusX - 3 - resetW;

                int cmdW = fr.getStringWidth("[CMD]");
                int cmdX = resetX - 4 - cmdW;

                String progress = item.goal > 0 ? (item.count + "/" + item.goal) : String.valueOf(item.count);
                int progW = Math.max(fr.getStringWidth(progress), MIN_EDIT_FIELD_W);
                int progX = cmdX - 6 - progW;

                if (inBounds(mouseX, mouseY, delX, rowY - 1, delW, ROW_H)) {
                    removeTrackedItem(e.getKey(), item.displayName);
                    return true;
                }
                if (inBounds(mouseX, mouseY, plusX, rowY - 1, plusW, ROW_H)) {
                    adjustGoal(item, step);
                    return true;
                }
                if (inBounds(mouseX, mouseY, minusX, rowY - 1, minusW, ROW_H)) {
                    adjustGoal(item, -step);
                    return true;
                }
                if (inBounds(mouseX, mouseY, resetX, rowY - 1, resetW, ROW_H)) {
                    resetCount(item);
                    return true;
                }
                if (inBounds(mouseX, mouseY, cmdX, rowY - 1, cmdW, ROW_H)) {
                    startEditCommand(e.getKey(), item);
                    return true;
                }
                if (inBounds(mouseX, mouseY, progX, rowY - 1, progW, ROW_H)) {
                    startEditGoal(e.getKey(), item);
                    return true;
                }
            }
            rowY += ROW_H;
        }

        return false;
    }

    @Override
    public boolean keyboardInput() {
        if (!KeyboardCompat.getEventKeyState()) return false;
        int key = KeyboardCompat.getEventKey();
        char c = KeyboardCompat.getEventCharacter();

        if (editingItemId != null) {
            if (key == Keyboard.KEY_ESCAPE) {
                cancelEdit();
                return true;
            }
            if (key == Keyboard.KEY_RETURN) {
                commitEdit();
                return true;
            }
            if (editField != null) editField.textboxKeyTyped(c, key);
            return true;
        }

        if (key == Keyboard.KEY_ESCAPE) {
            MinecraftCompat.getMinecraft().displayGuiScreen(parentScreen);
            return true;
        }

        if (searchField != null && searchField.isFocused()) {
            if (key == Keyboard.KEY_RETURN) {
                if (!filteredNames.isEmpty()) {
                    searchField.setText(filteredNames.get(0));
                    filteredNames.clear();
                } else {
                    addTrackedItem(searchField.getText());
                }
                return true;
            }
            boolean handled = searchField.textboxKeyTyped(c, key);
            if (handled) updateSuggestions();
            return true;
        }
        return false;
    }

    private boolean inBounds(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private boolean isHovered(int x, int y) {
        int[] mouse = KeybindHelper.getMouseCoords(GuiScreenUtils.getScaledResolution());
        return inBounds(mouse[0], mouse[1], x, y, BTN_W, SF_H);
    }
}
