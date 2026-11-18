package io.hamlook.aetheria.features.waypoints.ui;

import io.hamlook.aetheria.ThemeContext;

import io.hamlook.aetheria.features.waypoints.*;
import io.hamlook.aetheria.features.waypoints.text.WaypointText;
import io.hamlook.aetheria.utils.LerpUtils;
import io.hamlook.aetheria.utils.compat.ClipboardCompat;
import io.hamlook.aetheria.utils.compat.DrawContextUtils;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import net.minecraft.client.gui.FontRenderer;

import java.util.ArrayList;
import java.util.List;

public final class RoutesTab implements MenuTab {

    private static final String TAG_SEARCH = "routes-search";
    private static final String TAG_NEW = "routes-new";
    private static final int SEARCH_PILL_W = 90;
    private final List<Row> rows = new ArrayList<>();
    private int scroll;
    private LerpUtils.LerpingFloat shownScroll;
    private String query = "";
    private String pendingName = "";
    private String newError;
    private long lastClickMs;
    private String lastClickName;
    private boolean fieldsReady;
    private WaypointField searchField;
    private WaypointField newField;

    @Override
    public String title() {
        return Strings.TAB_ROUTES;
    }

    @Override
    public void onShow(WaypointGroupGui gui) {
        scroll = 0;
        shownScroll = null;
    }

    private void ensureFields(WaypointGroupGui gui) {
        if (fieldsReady) return;
        fieldsReady = true;
        searchField = gui.field(this, TAG_SEARCH).collapsedWidth(SEARCH_PILL_W).maxLength(64)
                .onChange(text -> query = text)
                .onSubmit(text -> true);
        newField = gui.field(this, TAG_NEW).maxLength(WaypointText.MAX_GROUP_NAME)
                .onSubmit(this::submitNew);
    }

    private boolean submitNew(String text) {
        WaypointGroupGui gui = WaypointGroupGui.current();
        if (gui == null) return false;
        WaypointResult result = WaypointActions.createRoute(text, "");
        if (!result.ok) {
            newError = result.message;
            pendingName = text;
            gui.toast(result);
            return false;
        }
        newError = null;
        pendingName = "";
        gui.toast(result);
        gui.openEditorTab(WaypointActions.normalizeName(text), false);
        return true;
    }

    private List<WaypointGroup> visible() {
        List<WaypointGroup> out = new ArrayList<>(WaypointStorage.getInstance().getGroups().values());
        String q = query.trim().toLowerCase();
        if (!q.isEmpty()) {
            List<WaypointGroup> filtered = new ArrayList<>();
            for (WaypointGroup g : out) {
                String desc = g.description == null ? "" : g.description.toLowerCase();
                if (g.name.toLowerCase().contains(q) || desc.contains(q)) filtered.add(g);
            }
            out = filtered;
        }
        out.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
        return out;
    }

    @Override
    public void render(WaypointGroupGui gui, FontRenderer fr, ThemeContext.HitList hits, int mouseX, int mouseY) {
        ensureFields(gui);
        rows.clear();

        int x = gui.contentX();
        int w = gui.contentW();
        int toolbarY = gui.contentY() + 4;

        int importW = ThemeContext.buttonWidth(fr, Strings.BTN_IMPORT);
        int newW = ThemeContext.buttonWidth(fr, Strings.BTN_NEW_ROUTE);
        int fieldW = w - importW - newW - 16;

        if (searchField.isOpen()) {
            searchField.bounds(x, toolbarY, fieldW, ThemeContext.FIELD_H);
            if (searchField.isClosing()) searchField.render();
        } else {
            boolean hover = hits.hover(x, toolbarY, SEARCH_PILL_W, ThemeContext.FIELD_H);
            ThemeContext.rect(x, toolbarY, SEARCH_PILL_W, ThemeContext.FIELD_H, ThemeContext.INSET);
            ThemeContext.border(x, toolbarY, SEARCH_PILL_W, ThemeContext.FIELD_H,
                    hover ? ThemeContext.BTN_BORDER : ThemeContext.LINE);
            String shown = query.isEmpty() ? Strings.SEARCH_PLACEHOLDER
                    : ThemeContext.fit(fr, query, SEARCH_PILL_W - 10);
            ThemeContext.text(fr, shown, x + 5, toolbarY + 8,
                    query.isEmpty() ? ThemeContext.MUTED : ThemeContext.TEXT);
            hits.add(x, toolbarY, SEARCH_PILL_W, ThemeContext.FIELD_H,
                    "Search routes by name or description.",
                    () -> gui.activateField(searchField));
        }

        ThemeContext.button(hits, fr, Strings.BTN_NEW_ROUTE, x + w - importW - newW - 8, toolbarY, newW,
                ThemeContext.FIELD_H, false, false, () -> {
                    if (newField.isActive()) gui.closeField();
                    else openNewField(gui);
                }, Strings.TOOLTIP_NEW_ROUTE);
        ThemeContext.button(hits, fr, Strings.BTN_IMPORT, x + w - importW, toolbarY, importW,
                ThemeContext.FIELD_H, true, false, () -> openImport(gui), Strings.TOOLTIP_PASTE);

        int rowTop = gui.contentY() + ThemeContext.TOOLBAR_H + 2;

        if (newField.isActive()) {
            int nameW = w - 150;
            newField.bounds(x, rowTop + 3, nameW, ThemeContext.FIELD_H);
            String hint = newError != null ? newError : Strings.NEW_ROUTE_HINT;
            ThemeContext.text(fr, ThemeContext.fit(fr, hint, w - nameW - 8), x + nameW + 8, rowTop + 12,
                    newError != null ? ThemeContext.DANGER : ThemeContext.MUTED);
            rowTop += 24;
        }

        List<WaypointGroup> groups = visible();
        int listH = gui.contentY() + gui.contentH() - rowTop;
        if (listH <= 0) return;
        int total = groups.size() * ThemeContext.ROW_H;
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

        if (groups.isEmpty()) {
            drawEmpty(gui, fr, hits, x, rowTop, w, listH, query.trim().isEmpty());
            return;
        }

        hits.clip(x, rowTop, w, listH);
        DrawContextUtils.enableScissor(x, rowTop, w, listH);
        int y = rowTop - shown;
        for (WaypointGroup g : groups) {
            if (y + ThemeContext.ROW_H > rowTop && y < rowTop + listH) drawRow(gui, fr, hits, g, x, y, w);
            Row r = new Row();
            r.name = g.name;
            r.x = x;
            r.y = y;
            r.w = w;
            r.h = ThemeContext.ROW_H;
            rows.add(r);
            y += ThemeContext.ROW_H;
        }
        DrawContextUtils.disableScissor();
        hits.unclip();

        if (maxScroll > 0) {
            int barH = Math.max(16, (int) ((long) listH * listH / Math.max(1, total)));
            int barY = rowTop + (int) ((long) (listH - barH) * shown / maxScroll);
            ThemeContext.rect(x + w - 3, rowTop, 3, listH, ThemeContext.LINE);
            ThemeContext.rect(x + w - 3, barY, 3, barH, ThemeContext.MUTED);
        }
    }

    private void drawRow(WaypointGroupGui gui, FontRenderer fr, ThemeContext.HitList hits, WaypointGroup g,
                         int x, int y, int w) {
        boolean active = gui.isRouteActive(g);
        boolean selected = g.name.equalsIgnoreCase(gui.selectedRouteName());
        if (selected) ThemeContext.rect(x, y, w, ThemeContext.ROW_H, ThemeContext.INSET);
        if (active) ThemeContext.rect(x + 2, y + ThemeContext.ROW_H / 2 - 3, 6, 6, ThemeContext.ACCENT);

        int textX = x + 16;
        int menuW = 18;
        int loadW = ThemeContext.buttonWidth(fr, Strings.BTN_LOAD);
        int right = x + w - 6;
        int btnY = y + (ThemeContext.ROW_H - ThemeContext.SMALL_H) / 2;

        ThemeContext.button(hits, fr, "...", right - menuW, btnY, menuW, ThemeContext.SMALL_H, false, false,
                () -> openMenu(gui, g, right - 130, y + ThemeContext.ROW_H), Strings.TOOLTIP_MORE);

        int nameMax = w - 16 - menuW - loadW - 24;
        ThemeContext.text(fr, ThemeContext.fit(fr, g.name, nameMax), textX, y + 7,
                active ? ThemeContext.ACCENT : ThemeContext.TEXT);

        StringBuilder sub = new StringBuilder();
        sub.append(g.waypoints.size()).append(g.waypoints.size() == 1 ? " waypoint" : " waypoints");
        if (g.description != null && !g.description.isEmpty()) sub.append("   ").append(g.description);
        ThemeContext.text(fr, ThemeContext.fit(fr, sub.toString(), nameMax), textX, y + 19, ThemeContext.MUTED);

        if (active) {
            ThemeContext.textRight(fr, Strings.ACTIVE_BADGE, right - menuW - 8, y + 13, ThemeContext.ACCENT);
        } else {
            ThemeContext.button(hits, fr, Strings.BTN_LOAD, right - menuW - 6 - loadW, btnY, loadW,
                    ThemeContext.SMALL_H, false, false,
                    () -> gui.toast(WaypointActions.loadRoute(g.name)), Strings.TOOLTIP_LOAD);
        }

        ThemeContext.divider(x, y + ThemeContext.ROW_H - 1, w);

        hits.add(x, y, w - menuW - loadW - 10, ThemeContext.ROW_H, null, () -> {
            if (g.name.equalsIgnoreCase(gui.selectedRouteName())) {
                long now = System.currentTimeMillis();
                if (now - lastClickMs < 320 && g.name.equals(lastClickName)) {
                    gui.openEditorTab(g.name, false);
                    lastClickMs = 0;
                    return;
                }
                lastClickMs = now;
                lastClickName = g.name;
            } else {
                gui.selectRoute(g.name);
                lastClickMs = 0;
            }
        });
    }

    private void drawEmpty(WaypointGroupGui gui, FontRenderer fr, ThemeContext.HitList hits,
                           int x, int top, int w, int h, boolean noRoutes) {
        int cx = x + w / 2;
        int y = top + Math.max(0, h / 2 - 70);
        if (!noRoutes) {
            ThemeContext.textCenter(fr, Strings.NO_MATCH_SEARCH, cx, y + 24, ThemeContext.MUTED);
            return;
        }

        ThemeContext.textCenter(fr, Strings.EMPTY_TITLE, cx, y, ThemeContext.TEXT);
        y += 22;

        String[] steps = {Strings.EMPTY_STEP_1, Strings.EMPTY_STEP_2, Strings.EMPTY_STEP_3};
        int stepX = Math.max(x + 10, cx - 170);
        for (int i = 0; i < steps.length; i++) {
            ThemeContext.text(fr, String.valueOf(i + 1), stepX, y, ThemeContext.ACCENT);
            ThemeContext.text(fr, steps[i], stepX + 14, y, ThemeContext.MUTED);
            if (i == 0) {
                int newW = ThemeContext.buttonWidth(fr, Strings.BTN_NEW_ROUTE);
                ThemeContext.button(hits, fr, Strings.BTN_NEW_ROUTE,
                        stepX + 14 + ThemeContext.width(fr, steps[0]) + 10, y - 4, newW, ThemeContext.SMALL_H,
                        false, false, () -> openNewField(gui), null);
            }
            y += 16;
        }

        y += 16;
        drawEmptyActions(gui, fr, hits, cx, y);
    }

    private void drawEmptyActions(WaypointGroupGui gui, FontRenderer fr, ThemeContext.HitList hits,
                                  int cx, int y) {
        int importW = ThemeContext.buttonWidth(fr, Strings.BTN_IMPORT_CLIPBOARD);
        if (!WaypointLinks.hasSite9()) {
            ThemeContext.button(hits, fr, Strings.BTN_IMPORT_CLIPBOARD, cx - importW / 2, y, importW,
                    ThemeContext.BTN_H, true, false, () -> openImport(gui), Strings.TOOLTIP_PASTE);
            return;
        }
        String or = Strings.EMPTY_OR;
        int orW = ThemeContext.width(fr, or);
        int siteW = ThemeContext.buttonWidth(fr, Strings.SITE_9_TEXT);
        int bx = cx - (orW + 8 + importW + 8 + siteW) / 2;
        ThemeContext.text(fr, or, bx, y + 3, ThemeContext.MUTED);
        bx += orW + 8;
        ThemeContext.button(hits, fr, Strings.BTN_IMPORT_CLIPBOARD, bx, y, importW, ThemeContext.BTN_H,
                true, false, () -> openImport(gui), Strings.TOOLTIP_PASTE);
        bx += importW + 8;
        ThemeContext.button(hits, fr, Strings.SITE_9_TEXT, bx, y, siteW, ThemeContext.BTN_H,
                false, false, () -> gui.toast(WaypointLinks.openSite9()), Strings.TOOLTIP_OPEN_SITE9);
    }

    private void openMenu(WaypointGroupGui gui, WaypointGroup g, int x, int y) {
        Modals.Menu menu = new Modals.Menu(x, y);
        menu.add(Strings.MENU_EDIT, () -> gui.openEditorTab(g.name, false));
        menu.add(Strings.MENU_RENAME, () -> gui.openEditorTab(g.name, true));
        menu.add(Strings.MENU_DUPLICATE, () -> gui.toast(WaypointActions.duplicateRoute(g.name)));
        menu.add(Strings.MENU_COPY_EXPORT, () -> gui.toast(WaypointActions.exportRoute(g.name)));
        menu.addDanger(Strings.MENU_DELETE, () -> gui.openModal(new Modals.Confirm(
                "Delete " + g.name + "?",
                "This removes the route and its " + g.waypoints.size() + " waypoints. You cannot undo this.",
                "Delete", true, () -> gui.toast(WaypointActions.deleteRoute(g.name)))));
        gui.openModal(menu);
    }

    private void openImport(WaypointGroupGui gui) {
        WaypointText.Result parsed = WaypointActions.readImport(ClipboardCompat.getClipboard());
        gui.openModal(new Modals.Import(MinecraftCompat.getFontRenderer(), parsed));
    }

    public void requestNewRoute(WaypointGroupGui gui) {
        ensureFields(gui);
        openNewField(gui);
    }

    private void openNewField(WaypointGroupGui gui) {
        newError = null;
        newField.placeholder(Strings.NEW_ROUTE_PLACEHOLDER).text(pendingName);
        gui.activateField(newField);
    }

    @Override
    public boolean scroll(WaypointGroupGui gui, int amount) {
        scroll = Math.max(0, scroll - (amount > 0 ? 36 : -36));
        return true;
    }

    @Override
    public boolean mouse(WaypointGroupGui gui, int mouseX, int mouseY, int button, boolean pressed) {
        if (button != 1 || !pressed) return false;
        for (Row r : rows) {
            if (mouseX < r.x || mouseX >= r.x + r.w || mouseY < r.y || mouseY >= r.y + r.h) continue;
            WaypointGroup g = WaypointStorage.getInstance().getGroup(r.name);
            if (g != null) openMenu(gui, g, mouseX, mouseY);
            return true;
        }
        return false;
    }

    private static final class Row {
        String name;
        int x, y, w, h;
    }
}