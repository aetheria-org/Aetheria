package io.hamlook.aetheria.features.waypoints.ui;

import io.hamlook.aetheria.features.waypoints.*;
import io.hamlook.aetheria.features.waypoints.text.WaypointText;
import io.hamlook.aetheria.utils.LerpUtils;
import io.hamlook.aetheria.utils.compat.ClipboardCompat;
import io.hamlook.aetheria.utils.compat.DrawContextUtils;
import net.minecraft.client.gui.FontRenderer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class EditorTab implements MenuTab {

    private static final String TAG_RENAME = "editor-rename";
    private static final String TAG_DESC = "editor-desc";
    private static final String TAG_CELL = "editor-cell";

    private static final int NUM_W = 26;
    private static final int COORD_W = 52;
    private static final int BTN_W = 18;
    private final List<Row> rows = new ArrayList<>();
    private int scroll;
    private LerpUtils.LerpingFloat shownScroll;
    private int selectedIndex = -1;
    private long lastClickMs;
    private int lastClickRow = -1;
    private int cellIndex = -1;
    private int cellCol = -1;
    private boolean fieldsReady;
    private WaypointField renameField;
    private WaypointField descField;
    private WaypointField cellField;
    private boolean renameRequested;

    private static WaypointGroupGui gui() {
        return WaypointGroupGui.current();
    }

    private static WaypointGroup selected(WaypointGroupGui gui) {
        return gui == null ? null : gui.selectedRoute();
    }

    private static String cellText(WaypointPoint p, int col) {
        if (col == 0) return p.name == null ? "" : p.name;
        if (col == 1) return num(p.x);
        if (col == 2) return num(p.y);
        return num(p.z);
    }

    private static String num(double v) {
        if (v == Math.floor(v) && !Double.isInfinite(v)) return String.valueOf((long) v);
        return String.valueOf(Math.round(v * 10.0) / 10.0);
    }

    @Override
    public String title() {
        return Strings.TAB_EDITOR;
    }

    public void requestRename() {
        renameRequested = true;
    }

    @Override
    public void onShow(WaypointGroupGui gui) {
        scroll = 0;
        shownScroll = null;
        selectedIndex = -1;
    }

    private void ensureFields(WaypointGroupGui gui) {
        if (fieldsReady) return;
        fieldsReady = true;
        renameField = gui.field(this, TAG_RENAME).maxLength(WaypointText.MAX_GROUP_NAME)
                .placeholder(Strings.NEW_ROUTE_PLACEHOLDER)
                .onSubmit(this::submitRename);
        descField = gui.field(this, TAG_DESC).maxLength(WaypointText.MAX_DESCRIPTION)
                .placeholder("Description")
                .onSubmit(this::submitDescription);
        cellField = gui.field(this, TAG_CELL).placeholder("")
                .onSubmit(text -> commitCell())
                .onTab(text -> commitCell() && editNextCell());
    }

    private boolean submitRename(String text) {
        WaypointGroup g = selected(gui());
        if (g == null) return false;
        WaypointResult result = WaypointActions.renameRoute(g.name, text);
        gui().toast(result);
        if (result.ok) gui().selectRoute(WaypointActions.normalizeName(text));
        return result.ok;
    }

    private boolean submitDescription(String text) {
        WaypointGroup g = selected(gui());
        gui().toast(WaypointActions.setDescription(g, text));
        return true;
    }

    @Override
    public void render(WaypointGroupGui gui, FontRenderer fr, WaypointUi.HitList hits, int mouseX, int mouseY) {
        ensureFields(gui);
        rows.clear();

        WaypointGroup g = gui.selectedRoute();
        int x = gui.contentX();
        int w = gui.contentW();

        if (g == null) {
            int cy = gui.contentY() + gui.contentH() / 2 - 20;
            WaypointUi.textCenter(fr, Strings.EDITOR_PICK, x + w / 2, cy, WaypointUi.MUTED);
            int bw = WaypointUi.buttonWidth(fr, Strings.BTN_GO_TO_ROUTES);
            WaypointUi.button(hits, fr, Strings.BTN_GO_TO_ROUTES, x + w / 2 - bw / 2, cy + 18, bw,
                    WaypointUi.BTN_H, false, false, () -> gui.openTab(WaypointGroupGui.TAB_ROUTES), null);
            return;
        }

        if (renameRequested) {
            renameRequested = false;
            openRename(gui, g);
        }

        int y = gui.contentY() + 4;
        int addW = WaypointUi.buttonWidth(fr, Strings.BTN_ADD_HERE);
        int exportW = WaypointUi.buttonWidth(fr, Strings.BTN_EXPORT);
        int renameW = WaypointUi.buttonWidth(fr, Strings.BTN_RENAME);
        int pickerW = Math.max(70, Math.min(w / 3, WaypointUi.width(fr, g.name) + 22));
        int btnH = WaypointUi.BTN_H;

        int moreX = x + w - addW - 6 - BTN_W;
        int exportX = moreX - 6 - exportW;
        int renameX = exportX - 6 - renameW;
        int menuY = y + btnH;

        WaypointUi.button(hits, fr, Strings.BTN_ADD_HERE, x + w - addW, y, addW, btnH, true, false,
                () -> gui.toast(WaypointActions.addHere(g, null)), Strings.TOOLTIP_ADD_HERE);
        WaypointUi.button(hits, fr, "...", moreX, y, BTN_W, btnH, false, false,
                () -> openMoreMenu(gui, g, moreX - 110, menuY), Strings.TOOLTIP_MORE);
        WaypointUi.button(hits, fr, Strings.BTN_EXPORT, exportX, y, exportW, btnH, false, false,
                () -> gui.toast(WaypointActions.exportRoute(g.name)), Strings.TOOLTIP_EXPORT);
        WaypointUi.button(hits, fr, Strings.BTN_RENAME, renameX, y, renameW, btnH, false, false,
                () -> openRename(gui, g), null);

        if (renameField.isActive()) {
            renameField.bounds(x, y, pickerW, btnH);
        } else {
            boolean hover = hits.hover(x, y, pickerW, btnH);
            WaypointUi.rect(x, y, pickerW, btnH, hover ? WaypointUi.BTN_HOVER : WaypointUi.BTN);
            WaypointUi.border(x, y, pickerW, btnH, WaypointUi.BTN_BORDER);
            WaypointUi.text(fr, WaypointUi.fit(fr, g.name, pickerW - 18), x + 6, y + 4, WaypointUi.TEXT);
            WaypointUi.textRight(fr, "v", x + pickerW - 8, y + 4, WaypointUi.MUTED);
            hits.add(x, y, pickerW, btnH, Strings.TOOLTIP_PICKER, () -> openPicker(gui));
        }
        WaypointUi.text(fr, g.waypoints.size() + (g.waypoints.size() == 1 ? " waypoint" : " waypoints"),
                x + pickerW + 8, y + 4, WaypointUi.MUTED);
        y += 22;

        if (descField.isActive()) {
            descField.bounds(x, y, w, WaypointUi.FIELD_H);
        } else {
            boolean empty = g.description == null || g.description.isEmpty();
            String desc = empty ? Strings.DESC_PLACEHOLDER : g.description;
            boolean hover = hits.hover(x, y, w, WaypointUi.FIELD_H);
            if (hover) {
                WaypointUi.rect(x, y, w, WaypointUi.FIELD_H, WaypointUi.INSET);
                WaypointUi.border(x, y, w, WaypointUi.FIELD_H, WaypointUi.LINE);
            }
            WaypointUi.text(fr, WaypointUi.fit(fr, desc, w - 8), x + 4, y + 8,
                    empty ? WaypointUi.MUTED : WaypointUi.TEXT);
            hits.add(x, y, w, WaypointUi.FIELD_H, null, () -> {
                descField.text(g.description == null ? "" : g.description);
                gui.activateField(descField);
            });
        }
        y += WaypointUi.FIELD_H + 6;

        boolean active = gui.isRouteActive(g);
        if (!active) {
            WaypointUi.text(fr, Strings.EDITOR_NOT_LOADED, x, y + 3, WaypointUi.MUTED);
            int loadW = WaypointUi.buttonWidth(fr, Strings.BTN_LOAD);
            WaypointUi.button(hits, fr, Strings.BTN_LOAD, x + w - loadW, y, loadW, WaypointUi.SMALL_H, false, false,
                    () -> gui.toast(WaypointActions.loadRoute(g.name)), Strings.TOOLTIP_LOAD);
            y += WaypointUi.SMALL_H + 6;
        }

        int nameW = nameColumnWidth(w);
        int colX = x + NUM_W + nameW;
        WaypointUi.text(fr, Strings.COL_NUM, x + 4, y, WaypointUi.MUTED);
        WaypointUi.text(fr, Strings.COL_NAME, x + NUM_W + 4, y, WaypointUi.MUTED);
        WaypointUi.textRight(fr, Strings.COL_X, colX + COORD_W - 6, y, WaypointUi.MUTED);
        WaypointUi.textRight(fr, Strings.COL_Y, colX + COORD_W * 2 - 6, y, WaypointUi.MUTED);
        WaypointUi.textRight(fr, Strings.COL_Z, colX + COORD_W * 3 - 6, y, WaypointUi.MUTED);
        y += 12;
        WaypointUi.divider(x, y, w);
        y += 2;

        int listTop = y;
        int listH = Math.max(0, gui.contentY() + gui.contentH() - 12 - listTop);
        int total = g.waypoints.size() * WaypointUi.TABLE_H;
        int maxScroll = Math.max(0, total - listH);
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        if (shownScroll == null) {
            shownScroll = new LerpUtils.LerpingFloat(scroll, 120);
            shownScroll.resetTimer();
        }
        if ((int) shownScroll.getTarget() != scroll) {
            shownScroll.setTarget(scroll);
            shownScroll.resetTimer();
        }
        shownScroll.tick();
        if (Math.abs(shownScroll.getValue() - scroll) > listH) shownScroll.setValue(scroll);
        int shown = Math.round(shownScroll.getValue());

        if (g.waypoints.isEmpty()) {
            WaypointUi.textCenter(fr, Strings.EDITOR_EMPTY, x + w / 2, listTop + 16, WaypointUi.MUTED);
            return;
        }

        int current = gui.activeIndex();
        hits.clip(x, listTop, w, listH);
        DrawContextUtils.enableScissor(x, listTop, w, listH);
        int rowY = listTop - shown;
        for (int i = 0; i < g.waypoints.size(); i++) {
            Row r = new Row();
            r.index = i;
            r.x = x;
            r.y = rowY;
            r.w = w;
            r.h = WaypointUi.TABLE_H;
            rows.add(r);
            if (rowY + WaypointUi.TABLE_H > listTop && rowY < listTop + listH) {
                drawRow(gui, fr, hits, g, i, rowY, x, w, active, current);
            }
            rowY += WaypointUi.TABLE_H;
        }
        DrawContextUtils.disableScissor();
        hits.unclip();

        if (maxScroll > 0) {
            int barH = Math.max(16, (int) ((long) listH * listH / Math.max(1, total)));
            int barY = listTop + (int) ((long) (listH - barH) * shown / maxScroll);
            WaypointUi.rect(x + w - 3, listTop, 3, listH, WaypointUi.LINE);
            WaypointUi.rect(x + w - 3, barY, 3, barH, WaypointUi.MUTED);
        }
    }

    private int nameColumnWidth(int w) {
        int buttons = (BTN_W + 4) * 3 + 8;
        return Math.max(60, w - NUM_W - COORD_W * 3 - buttons);
    }

    private void drawRow(WaypointGroupGui gui, FontRenderer fr, WaypointUi.HitList hits, WaypointGroup g,
                         int index, int rowY, int x, int w, boolean active, int current) {
        WaypointPoint p = g.waypoints.get(index);
        boolean isCurrent = active && index == current;
        boolean selected = index == selectedIndex;
        boolean hover = hits.hover(x, rowY, w, WaypointUi.TABLE_H);
        if (isCurrent || selected) WaypointUi.rect(x, rowY, w, WaypointUi.TABLE_H, WaypointUi.INSET);

        WaypointUi.textRight(fr, String.valueOf(index + 1), x + NUM_W - 8, rowY + 7,
                isCurrent ? WaypointUi.ACCENT : WaypointUi.MUTED);

        int nameW = nameColumnWidth(w);
        int nameX = x + NUM_W;
        int colX = nameX + nameW;

        hits.add(x, rowY, w, WaypointUi.TABLE_H, null, () -> selectedIndex = index);

        drawCell(hits, gui, g, index, 0, nameX, rowY, nameW, p.name, WaypointUi.TEXT);
        drawCell(hits, gui, g, index, 1, colX, rowY, COORD_W, num(p.x), WaypointUi.MUTED);
        drawCell(hits, gui, g, index, 2, colX + COORD_W, rowY, COORD_W, num(p.y), WaypointUi.MUTED);
        drawCell(hits, gui, g, index, 3, colX + COORD_W * 2, rowY, COORD_W, num(p.z), WaypointUi.MUTED);

        if (hover || selected) {
            int bx = x + w - 6 - BTN_W;
            int by = rowY + (WaypointUi.TABLE_H - WaypointUi.SMALL_H) / 2;
            WaypointUi.button(hits, fr, "x", bx, by, BTN_W, WaypointUi.SMALL_H, false, true,
                    () -> remove(gui, g, index), Strings.TOOLTIP_REMOVE);
            if (index < g.waypoints.size() - 1) {
                WaypointUi.button(hits, fr, "v", bx - BTN_W - 4, by, BTN_W, WaypointUi.SMALL_H, false, false,
                        () -> gui.toast(WaypointActions.movePoint(g, index, 1)), Strings.TOOLTIP_MOVE_DOWN);
            } else {
                WaypointUi.buttonDisabled(fr, "v", bx - BTN_W - 4, by, BTN_W, WaypointUi.SMALL_H);
            }
            if (index > 0) {
                WaypointUi.button(hits, fr, "^", bx - (BTN_W + 4) * 2, by, BTN_W, WaypointUi.SMALL_H, false, false,
                        () -> gui.toast(WaypointActions.movePoint(g, index, -1)), Strings.TOOLTIP_MOVE_UP);
            } else {
                WaypointUi.buttonDisabled(fr, "^", bx - (BTN_W + 4) * 2, by, BTN_W, WaypointUi.SMALL_H);
            }
            int hereW = WaypointUi.width(fr, Strings.BTN_HERE) + 10;
            WaypointUi.button(hits, fr, Strings.BTN_HERE, bx - (BTN_W + 4) * 2 - hereW - 4, by, hereW,
                    WaypointUi.SMALL_H, false, false, () -> gui.toast(WaypointActions.setHere(g, index)),
                    Strings.TOOLTIP_HERE);
        }

        WaypointUi.divider(x, rowY + WaypointUi.TABLE_H - 1, w);
    }

    private void drawCell(WaypointUi.HitList hits, WaypointGroupGui gui, WaypointGroup g, int index, int col,
                          int x, int rowY, int w, String text, int color) {
        FontRenderer fr = gui.font();
        if (col == 0) {
            WaypointUi.text(fr, WaypointUi.fit(fr, text == null ? "" : text, w - 8), x + 4, rowY + 7, color);
        } else {
            WaypointUi.textRight(fr, text, x + w - 6, rowY + 7, color);
        }
        hits.add(x, rowY + 1, w, WaypointUi.TABLE_H - 2, Strings.TOOLTIP_EDIT_CELL,
                () -> openCell(gui, g, index, col, x, rowY, w));
    }

    private void openCell(WaypointGroupGui gui, WaypointGroup g, int index, int col,
                          int x, int rowY, int w) {
        if (index < 0 || index >= g.waypoints.size()) return;
        cellIndex = index;
        cellCol = col;
        cellField.maxLength(col == 0 ? WaypointText.MAX_POINT_NAME : 20);
        cellField.setError(false);
        cellField.bounds(x + 1, rowY + 3, w - 2, 16);
        cellField.text(cellText(g.waypoints.get(index), col));
        gui.activateField(cellField);
    }

    private boolean commitCell() {
        WaypointGroupGui gui = gui();
        WaypointGroup g = selected(gui);
        if (gui == null || g == null) return false;
        if (cellIndex < 0 || cellIndex >= g.waypoints.size()) return true;
        String text = cellField.value();
        WaypointResult result;
        if (cellCol == 0) {
            result = WaypointActions.editPoint(g, cellIndex, text, null, null, null);
        } else {
            double value;
            try {
                value = Double.parseDouble(text.trim());
            } catch (NumberFormatException e) {
                gui.toast("Enter a number.", false);
                cellField.setError(true);
                return false;
            }
            result = WaypointActions.editPoint(g, cellIndex, null,
                    cellCol == 1 ? value : null,
                    cellCol == 2 ? value : null,
                    cellCol == 3 ? value : null);
        }
        if (!result.ok) {
            gui.toast(result);
            cellField.setError(true);
            return false;
        }
        cellField.setError(false);
        return true;
    }

    private boolean editNextCell() {
        WaypointGroupGui gui = gui();
        WaypointGroup g = selected(gui);
        if (gui == null || g == null) return false;
        int nextCol = cellCol + 1;
        int nextRow = cellIndex;
        if (nextCol > 3) {
            nextCol = 0;
            nextRow++;
        }
        if (nextRow >= g.waypoints.size()) return false;
        int rowY = -1;
        for (Row r : rows) if (r.index == nextRow) rowY = r.y;
        if (rowY < 0) return false;
        int nameW = nameColumnWidth(gui.contentW());
        int cellX = nextCol == 0
                ? gui.contentX() + NUM_W
                : gui.contentX() + NUM_W + nameW + COORD_W * (nextCol - 1);
        int cellW = nextCol == 0 ? nameW : COORD_W;
        openCell(gui, g, nextRow, nextCol, cellX, rowY, cellW);
        return true;
    }

    private void remove(WaypointGroupGui gui, WaypointGroup g, int index) {
        if (index < 0 || index >= g.waypoints.size()) return;
        WaypointResult result = WaypointActions.removePoint(g, index);
        gui.toast(result);
    }

    private void openPicker(WaypointGroupGui gui) {
        List<String> names = new ArrayList<>(WaypointStorage.getInstance().getGroups().keySet());
        Collections.sort(names);
        gui.openModal(new Modals.Picker(names, gui::selectRoute));
    }

    private void openMoreMenu(WaypointGroupGui gui, WaypointGroup g, int x, int y) {
        Modals.Menu menu = new Modals.Menu(x, y);
        menu.add(Strings.MENU_DUPLICATE_ROUTE, () -> gui.toast(WaypointActions.duplicateRoute(g.name)));
        menu.add(Strings.MENU_REVERSE, () -> gui.toast(WaypointActions.reverse(g)));
        menu.add(Strings.MENU_CLEAR_ALL, () -> gui.openModal(new Modals.Confirm(
                "Clear " + g.name + "?",
                "This removes all " + g.waypoints.size() + " waypoints of the route.",
                "Clear", true, () -> gui.toast(WaypointActions.clear(g)))));
        menu.addDanger(Strings.MENU_DELETE_ROUTE, () -> gui.openModal(new Modals.Confirm(
                "Delete " + g.name + "?",
                "This removes the route and its " + g.waypoints.size() + " waypoints. You cannot undo this.",
                "Delete", true, () -> {
            gui.toast(WaypointActions.deleteRoute(g.name));
            gui.selectRoute(null);
        })));
        gui.openModal(menu);
    }

    private void openRename(WaypointGroupGui gui, WaypointGroup g) {
        int pickerW = Math.max(70, Math.min(gui.contentW() / 3, WaypointUi.width(gui.font(), g.name) + 22));
        renameField.bounds(gui.contentX(), gui.contentY() + 4, pickerW, WaypointUi.BTN_H);
        renameField.text(g.name);
        gui.activateField(renameField);
    }

    @Override
    public boolean scroll(WaypointGroupGui gui, int amount) {
        scroll = Math.max(0, scroll - (amount > 0 ? 36 : -36));
        return true;
    }

    @Override
    public boolean mouse(WaypointGroupGui gui, int mouseX, int mouseY, int button, boolean pressed) {
        if (!pressed) return false;
        WaypointGroup g = gui.selectedRoute();
        if (g == null) return false;
        for (Row r : rows) {
            if (mouseX < r.x || mouseX >= r.x + r.w || mouseY < r.y || mouseY >= r.y + r.h) continue;
            if (button == 1) {
                openRowMenu(gui, g, r.index, mouseX, mouseY);
                return true;
            }
            if (button == 0 && gui.isRouteActive(g)) {
                long now = System.currentTimeMillis();
                if (now - lastClickMs < 320 && lastClickRow == r.index) {
                    gui.toast(WaypointActions.skipTo(r.index));
                    lastClickMs = 0;
                    lastClickRow = -1;
                    return true;
                }
                lastClickMs = now;
                lastClickRow = r.index;
            }
            return false;
        }
        return false;
    }

    private void openRowMenu(WaypointGroupGui gui, WaypointGroup g, int index, int mouseX, int mouseY) {
        Modals.Menu menu = new Modals.Menu(mouseX, mouseY);
        menu.add(Strings.ROW_INSERT_ABOVE, () -> gui.toast(WaypointActions.insertHere(g, index, null)));
        menu.add(Strings.ROW_INSERT_BELOW, () -> gui.toast(WaypointActions.insertHere(g, index + 1, null)));
        menu.add(Strings.MENU_DUPLICATE, () -> gui.toast(WaypointActions.duplicatePoint(g, index)));
        if (gui.isRouteActive(g)) menu.add(Strings.ROW_GOTO, () -> gui.toast(WaypointActions.skipTo(index)));
        menu.add(Strings.ROW_COPY_COORDS, () -> {
            if (index < 0 || index >= g.waypoints.size()) return;
            WaypointPoint p = g.waypoints.get(index);
            ClipboardCompat.setClipboard(p.x + ", " + p.y + ", " + p.z);
            gui.toast(Strings.TOAST_COORDS, true);
        });
        gui.openModal(menu);
    }

    private static final class Row {
        int index;
        int x, y, w, h;
    }
}