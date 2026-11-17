package io.hamlook.aetheria.features.waypoints.ui;

import io.hamlook.aetheria.features.waypoints.Strings;
import io.hamlook.aetheria.features.waypoints.WaypointGroupGui;
import io.hamlook.aetheria.features.waypoints.WaypointLinks;
import io.hamlook.aetheria.utils.LerpUtils;
import io.hamlook.aetheria.utils.compat.ClipboardCompat;
import io.hamlook.aetheria.utils.compat.DrawContextUtils;
import net.minecraft.client.gui.FontRenderer;

import java.util.ArrayList;
import java.util.List;

public final class HelpTab implements MenuTab {

    private static final int TITLE_H = 18;
    private static final int TEXT_GAP = 6;
    private static final int CMD_H = 14;
    private static final int BUTTON_SPACE = 60;

    private static final int KIND_TITLE = 0;
    private static final int KIND_TEXT = 1;
    private static final int KIND_STEP = 2;
    private static final int KIND_COMMAND = 3;
    private final List<Item> items = new ArrayList<>();
    private int scroll;
    private LerpUtils.LerpingFloat shownScroll;
    private int builtWidth = -1;
    private boolean builtSite;

    @Override
    public String title() {
        return Strings.TAB_HELP;
    }

    @Override
    public void onShow(WaypointGroupGui gui) {
        scroll = 0;
        shownScroll = null;
    }

    private Item base(int kind, int height) {
        Item item = new Item();
        item.kind = kind;
        item.height = height;
        items.add(item);
        return item;
    }

    private void build(WaypointGroupGui gui, FontRenderer fr, int w) {
        boolean partner = WaypointLinks.hasSite9();
        if (builtWidth == w && builtSite == partner) return;
        builtWidth = w;
        builtSite = partner;
        items.clear();

        if (partner) {
            title(Strings.HELP_SITE_TITLE);
            Item link = base(KIND_TEXT, 0);
            link.lines = DrawContextUtils.wrapFormattedText(fr, Strings.HELP_SITE_TEXT, w - BUTTON_SPACE);
            link.button = Strings.BTN_OPEN;
            link.run = () -> gui.toast(WaypointLinks.openSite9());
            link.height = link.lines.size() * WaypointUi.LINE_H + TEXT_GAP;
        }

        title(Strings.HELP_QUICK);
        step(fr, "1", Strings.HELP_STEP_1, w, Strings.BTN_NEW_ROUTE,
                gui::requestNewRoute);
        step(fr, "2", Strings.HELP_STEP_2, w, Strings.BTN_OPEN_EDITOR,
                () -> gui.openTab(WaypointGroupGui.TAB_EDITOR));
        step(fr, "3", Strings.HELP_STEP_3, w, Strings.BTN_GO_ROUTES_SHORT,
                () -> gui.openTab(WaypointGroupGui.TAB_ROUTES));

        title(Strings.HELP_HOW);
        text(fr, Strings.HELP_HOW_1, w);
        text(fr, Strings.HELP_HOW_2, w);
        text(fr, Strings.HELP_HOW_3, w);
        text(fr, Strings.HELP_HOW_4, w);

        title(Strings.HELP_IMPORT);
        text(fr, Strings.HELP_IMPORT_TEXT, w);

        title(Strings.HELP_COMMANDS);
        text(fr, Strings.ALIASES, w);
        for (String[] row : Strings.COMMAND_ROWS) {
            Item item = base(KIND_COMMAND, CMD_H);
            item.command = row[0];
            item.action = row[1];
            item.button = Strings.BTN_COPY;
        }
    }

    private void title(String text) {
        Item item = base(KIND_TITLE, TITLE_H);
        item.lines = new ArrayList<>(1);
        item.lines.add(text);
    }

    private void text(FontRenderer fr, String text, int w) {
        Item item = base(KIND_TEXT, 0);
        item.lines = DrawContextUtils.wrapFormattedText(fr, text, w);
        item.height = item.lines.size() * WaypointUi.LINE_H + TEXT_GAP;
    }

    private void step(FontRenderer fr, String number, String text, int w, String button, Runnable run) {
        Item item = base(KIND_STEP, 0);
        item.number = number;
        item.lines = DrawContextUtils.wrapFormattedText(fr, text, w - BUTTON_SPACE - 16);
        item.button = button;
        item.run = run;
        item.height = Math.max(WaypointUi.BTN_H, item.lines.size() * WaypointUi.LINE_H) + TEXT_GAP;
    }

    @Override
    public void render(WaypointGroupGui gui, FontRenderer fr, WaypointUi.HitList hits, int mouseX, int mouseY) {
        int x = gui.contentX();
        int w = gui.contentW();
        int top = gui.contentY();
        int height = gui.contentH();

        build(gui, fr, w);
        int total = 0;
        for (Item item : items) total += item.height;
        int maxScroll = Math.max(0, total - height);
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
        if (Math.abs(shownScroll.getValue() - scroll) > height) shownScroll.setValue(scroll);
        int shown = Math.round(shownScroll.getValue());

        hits.clip(x, top, w, height);
        DrawContextUtils.enableScissor(x, top, w, height);
        int y = top - shown;
        for (Item item : items) {
            if (y + item.height > top && y < top + height) drawItem(gui, fr, hits, item, x, y, w);
            y += item.height;
        }
        DrawContextUtils.disableScissor();
        hits.unclip();

        if (maxScroll > 0) {
            int barH = Math.max(16, (int) ((long) height * height / Math.max(1, total)));
            int barY = top + (int) ((long) (height - barH) * shown / maxScroll);
            WaypointUi.rect(x + w - 3, top, 3, height, WaypointUi.LINE);
            WaypointUi.rect(x + w - 3, barY, 3, barH, WaypointUi.MUTED);
        }
    }

    private void drawItem(WaypointGroupGui gui, FontRenderer fr, WaypointUi.HitList hits, Item item,
                          int x, int y, int w) {
        if (item.kind == KIND_TITLE) {
            WaypointUi.text(fr, item.lines.get(0), x, y + 8, WaypointUi.ACCENT);
            return;
        }

        int btnW = item.button == null ? 0 : WaypointUi.buttonWidth(fr, item.button);
        int btnX = x + w - btnW;
        int btnY = item.kind == KIND_COMMAND ? y + 1 : y + 2;

        if (item.kind == KIND_COMMAND) {
            WaypointUi.text(fr, item.command, x, y + 4, WaypointUi.TEXT);
            int actionX = x + Math.max(WaypointUi.width(fr, item.command) + 12, w / 3);
            WaypointUi.text(fr, WaypointUi.fit(fr, item.action, btnX - 8 - actionX), actionX, y + 4,
                    WaypointUi.MUTED);
        } else {
            int textX = x;
            if (item.number != null) {
                WaypointUi.text(fr, item.number, x, y + 3, WaypointUi.ACCENT);
                textX = x + 14;
            }
            int lineY = y + 3;
            for (String line : item.lines) {
                WaypointUi.text(fr, line, textX, lineY, WaypointUi.MUTED);
                lineY += WaypointUi.LINE_H;
            }
        }

        if (item.button == null) return;

        boolean primary = item.kind != KIND_COMMAND;
        WaypointUi.button(hits, fr, item.button, btnX, btnY, btnW, WaypointUi.SMALL_H, primary, false,
                () -> run(gui, item), Strings.TOOLTIP_COPY);
    }

    private void run(WaypointGroupGui gui, Item item) {
        if (item.command != null) {
            ClipboardCompat.setClipboard(item.command);
            gui.toast(Strings.TOAST_COPIED_PASTE, true);
            return;
        }
        if (item.run != null) item.run.run();
    }

    @Override
    public boolean scroll(WaypointGroupGui gui, int amount) {
        scroll = Math.max(0, scroll - (amount > 0 ? 32 : -32));
        return true;
    }

    private static final class Item {
        int kind;
        String number;
        List<String> lines;
        String command;
        String action;
        String button;
        Runnable run;
        int height;
    }
}