package io.hamlook.aetheria.features.waypoints.ui;

import io.hamlook.aetheria.features.waypoints.*;
import io.hamlook.aetheria.features.waypoints.text.WaypointText;
import io.hamlook.aetheria.utils.compat.DrawContextUtils;
import io.hamlook.aetheria.utils.compat.GuiScreenUtils;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class Modals {

    private Modals() {
    }

    public abstract static class Modal {

        public abstract void render(WaypointGroupGui gui, FontRenderer fr, WaypointUi.HitList hits);

        public boolean key(WaypointGroupGui gui, char typedChar, int keyCode) {
            return false;
        }

        public boolean scroll(WaypointGroupGui gui, int amount) {
            return false;
        }

        public boolean escape(WaypointGroupGui gui) {
            gui.closeModal();
            return true;
        }

        int[] box(int w, int h) {
            int sw = GuiScreenUtils.getScaledWindowWidth();
            int sh = GuiScreenUtils.getScaledWindowHeight();
            return new int[]{Math.max(4, (sw - w) / 2), Math.max(4, (sh - h) / 2), w, h};
        }

        void frame(int[] b, float alpha) {
            WaypointUi.rect(b[0] - 1, b[1] - 1, b[2] + 2, b[3] + 2, WaypointUi.withAlpha(WaypointUi.EDGE, alpha));
            WaypointUi.rect(b[0], b[1], b[2], b[3], WaypointUi.withAlpha(WaypointUi.PANEL, alpha));
            WaypointUi.border(b[0], b[1], b[2], b[3], WaypointUi.LINE);
        }

        List<String> wrap(FontRenderer fr, String text, int width) {
            List<String> out = new ArrayList<>();
            if (text == null || text.isEmpty()) return out;
            for (String line : text.split("\n")) out.addAll(DrawContextUtils.wrapFormattedText(fr, line, width));
            return out;
        }
    }

    public static final class Confirm extends Modal {

        private final String title;
        private final String body;
        private final String confirmLabel;
        private final boolean danger;
        private final Runnable onConfirm;

        public Confirm(String title, String body, String confirmLabel, boolean danger, Runnable onConfirm) {
            this.title = title;
            this.body = body;
            this.confirmLabel = confirmLabel;
            this.danger = danger;
            this.onConfirm = onConfirm;
        }

        private List<String> lines(FontRenderer fr) {
            return wrap(fr, body, 260);
        }

        @Override
        public void render(WaypointGroupGui gui, FontRenderer fr, WaypointUi.HitList hits) {
            List<String> lines = lines(fr);
            int h = 12 + 12 + lines.size() * WaypointUi.LINE_H + 10 + WaypointUi.BTN_H + 12;
            int[] b = box(300, h);
            frame(b, gui.modalAlpha());

            int x = b[0] + 20;
            int y = b[1] + 12;
            WaypointUi.text(fr, title, x, y, WaypointUi.TEXT);
            y += 12;
            for (String line : lines) {
                WaypointUi.text(fr, line, x, y, WaypointUi.MUTED);
                y += WaypointUi.LINE_H;
            }
            int btnY = b[1] + b[3] - 12 - WaypointUi.BTN_H;
            int confirmW = WaypointUi.buttonWidth(fr, confirmLabel);
            int cancelW = WaypointUi.buttonWidth(fr, Strings.BTN_CANCEL);
            int right = b[0] + b[2] - 20;
            WaypointUi.button(hits, fr, confirmLabel, right - confirmW, btnY, confirmW, WaypointUi.BTN_H, !danger, danger, () -> {
                gui.closeModal();
                if (onConfirm != null) onConfirm.run();
            }, null);
            WaypointUi.button(hits, fr, Strings.BTN_CANCEL, right - confirmW - 6 - cancelW, btnY, cancelW, WaypointUi.BTN_H, false, false, gui::closeModal, null);
        }

        @Override
        public boolean key(WaypointGroupGui gui, char typedChar, int keyCode) {
            if (keyCode == Keyboard.KEY_RETURN && !danger) {
                gui.closeModal();
                if (onConfirm != null) onConfirm.run();
                return true;
            }
            return false;
        }
    }

    public static final class Entry {
        final String label;
        final boolean danger;
        final Runnable action;

        public Entry(String label, boolean danger, Runnable action) {
            this.label = label;
            this.danger = danger;
            this.action = action;
        }
    }

    public static final class Menu extends Modal {

        private final int x, y;
        private final List<Entry> entries = new ArrayList<>();

        public Menu(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public Menu add(String label, Runnable action) {
            entries.add(new Entry(label, false, action));
            return this;
        }

        public Menu addDanger(String label, Runnable action) {
            entries.add(new Entry(label, true, action));
            return this;
        }

        @Override
        public void render(WaypointGroupGui gui, FontRenderer fr, WaypointUi.HitList hits) {
            if (entries.isEmpty()) return;
            int w = 120;
            for (Entry e : entries) w = Math.max(w, WaypointUi.width(fr, e.label) + 24);
            int h = entries.size() * 18 + 4;
            int sw = GuiScreenUtils.getScaledWindowWidth();
            int sh = GuiScreenUtils.getScaledWindowHeight();
            int bx = Math.min(x, sw - w - 4);
            int by = Math.min(y, sh - h - 4);

            WaypointUi.rect(bx - 1, by - 1, w + 2, h + 2, WaypointUi.withAlpha(WaypointUi.EDGE, gui.modalAlpha()));
            WaypointUi.rect(bx, by, w, h, WaypointUi.withAlpha(WaypointUi.INSET, gui.modalAlpha()));
            WaypointUi.border(bx, by, w, h, WaypointUi.LINE);

            int rowY = by + 2;
            for (Entry e : entries) {
                boolean hover = hits.hover(bx + 2, rowY, w - 4, 18);
                if (hover) WaypointUi.rect(bx + 2, rowY, w - 4, 18, WaypointUi.BTN_HOVER);
                WaypointUi.text(fr, e.label, bx + 8, rowY + 5, e.danger ? WaypointUi.DANGER : WaypointUi.TEXT);
                Runnable action = e.action;
                hits.add(bx + 2, rowY, w - 4, 18, null, () -> {
                    gui.closeModal();
                    if (action != null) action.run();
                });
                rowY += 18;
            }
        }
    }

    public static final class Picker extends Modal {

        private final List<String> names;
        private final Consumer<String> onPick;
        private int scroll;

        public Picker(List<String> names, Consumer<String> onPick) {
            this.names = names;
            this.onPick = onPick;
        }

        private int visible() {
            return Math.min(10, names.size());
        }

        @Override
        public void render(WaypointGroupGui gui, FontRenderer fr, WaypointUi.HitList hits) {
            int w = 200;
            for (String n : names) w = Math.max(w, WaypointUi.width(fr, n) + 24);
            int rows = Math.max(1, visible());
            int h = rows * 18 + 8;
            int[] b = box(w, h);
            frame(b, gui.modalAlpha());

            int rowY = b[1] + 4;
            int maxScroll = Math.max(0, names.size() - rows);
            scroll = Math.min(scroll, maxScroll);
            for (int i = 0; i < rows; i++) {
                int index = i + scroll;
                if (index >= names.size()) break;
                String name = names.get(index);
                boolean hover = hits.hover(b[0] + 4, rowY, w - 8, 18);
                if (hover) WaypointUi.rect(b[0] + 4, rowY, w - 8, 18, WaypointUi.BTN_HOVER);
                boolean active = gui.isRouteActive(WaypointStorage.getInstance().getGroup(name));
                WaypointUi.text(fr, name, b[0] + 10, rowY + 5, active ? WaypointUi.ACCENT : WaypointUi.TEXT);
                hits.add(b[0] + 4, rowY, w - 8, 18, null, () -> {
                    gui.closeModal();
                    if (onPick != null) onPick.accept(name);
                });
                rowY += 18;
            }
        }

        @Override
        public boolean scroll(WaypointGroupGui gui, int amount) {
            int maxScroll = Math.max(0, names.size() - Math.max(1, visible()));
            scroll = Math.max(0, Math.min(maxScroll, scroll + (amount > 0 ? -1 : 1)));
            return true;
        }
    }

    public static final class Import extends Modal {

        private final WaypointText.Result parsed;
        private final GuiTextField nameField;
        private boolean replaceChosen;
        private boolean error;

        public Import(FontRenderer fr, WaypointText.Result parsed) {
            this.parsed = parsed;
            String base = parsed != null && !parsed.isFailed() ? WaypointActions.normalizeName(parsed.getGroup().name) : "";
            if (base.isEmpty()) base = "imported";
            this.nameField = GuiScreenUtils.createTextField(fr, 0, 0, 160, 12);
            GuiScreenUtils.setTextFieldMaxLength(nameField, WaypointText.MAX_GROUP_NAME);
            GuiScreenUtils.setTextFieldBackground(nameField, false);
            GuiScreenUtils.setTextFieldCanLoseFocus(nameField, true);
            nameField.setText(base);
            nameField.setFocused(true);
            GuiScreenUtils.cursorTextFieldToEnd(nameField);
        }

        private boolean valid() {
            return parsed != null && !parsed.isFailed();
        }

        private String typedName() {
            return nameField.getText() == null ? "" : nameField.getText().trim();
        }

        private String targetName() {
            String typed = typedName();
            if (typed.isEmpty()) return "";
            return replaceChosen ? typed : WaypointActions.uniqueName(WaypointActions.normalizeName(typed));
        }

        private void doImport(WaypointGroupGui gui) {
            String typed = typedName();
            if (WaypointActions.normalizeName(typed).isEmpty()) {
                error = true;
                gui.toast(Strings.NAME_EMPTY, false);
                return;
            }
            String target = targetName();
            WaypointResult result = WaypointActions.importRoute(parsed, target, replaceChosen);
            gui.toast(result);
            if (!result.ok) {
                error = true;
                return;
            }
            gui.closeModal();
            gui.selectRoute(target);
            gui.openTab(WaypointGroupGui.TAB_EDITOR);
        }

        private String formatLabel() {
            if (parsed == null || parsed.getFormat() == null) return "";
            if ("aetheria".equals(parsed.getFormat())) return "Aetheria";
            if ("soopy".equals(parsed.getFormat())) return "Soopy-style JSON";
            return "Plain rows";
        }

        @Override
        public void render(WaypointGroupGui gui, FontRenderer fr, WaypointUi.HitList hits) {
            int w = 320;
            boolean ok = valid();
            int h = ok ? 164 : 150;
            int[] b = box(w, h);
            frame(b, gui.modalAlpha());
            int mx = hits.mouseX(), my = hits.mouseY();

            int x = b[0] + 16;
            int y = b[1] + 12;
            WaypointUi.text(fr, Strings.IMPORT_TITLE, x, y, WaypointUi.TEXT);
            y += 18;

            if (!ok) {
                String msg = parsed == null || parsed.getError() == null ? Strings.IMPORT_EMPTY : parsed.getError();
                for (String line : wrap(fr, msg, w - 32)) {
                    WaypointUi.text(fr, line, x, y, WaypointUi.DANGER);
                    y += WaypointUi.LINE_H;
                }
                y += 4;
                WaypointUi.text(fr, Strings.IMPORT_READS, x, y, WaypointUi.MUTED);
                y += WaypointUi.LINE_H;
                WaypointUi.text(fr, Strings.IMPORT_TRY, x, y, WaypointUi.MUTED);
            } else {
                WaypointUi.text(fr, Strings.IMPORT_FOUND + parsed.getGroup().waypoints.size() + " waypoints", x, y, WaypointUi.TEXT);
                y += WaypointUi.LINE_H;
                WaypointUi.text(fr, Strings.IMPORT_FORMAT + formatLabel(), x, y, WaypointUi.MUTED);
                y += WaypointUi.LINE_H;
                if (parsed.getSkipped() > 0) {
                    WaypointUi.text(fr, parsed.getSkipped() + Strings.IMPORT_SKIPPED, x, y, WaypointUi.MUTED);
                    y += WaypointUi.LINE_H;
                }
                y += 6;
                WaypointUi.text(fr, Strings.IMPORT_NAME, x, y, WaypointUi.MUTED);
                y += 11;

                int fieldY = y, fieldW = w - 32;
                WaypointUi.rect(x, fieldY, fieldW, WaypointUi.FIELD_H, WaypointUi.INSET);
                WaypointUi.border(x, fieldY, fieldW, WaypointUi.FIELD_H, error ? WaypointUi.DANGER : WaypointUi.LINE);
                GuiScreenUtils.setX(nameField, x + 4);
                GuiScreenUtils.setY(nameField, fieldY + 4);
                GuiScreenUtils.setW(nameField, fieldW - 8);
                GuiScreenUtils.setH(nameField, 10);
                GuiScreenUtils.tickTextField(nameField);
                GuiScreenUtils.renderTextField(nameField, mx, my);
                y += WaypointUi.FIELD_H + 6;

                boolean nameTaken = !WaypointActions.normalizeName(typedName()).isEmpty() && WaypointStorage.getInstance().getGroup(WaypointActions.normalizeName(typedName())) != null;
                if (nameTaken) {
                    String free = WaypointActions.uniqueName(WaypointActions.normalizeName(typedName()));
                    int bw1 = WaypointUi.buttonWidth(fr, "Replace " + WaypointActions.normalizeName(typedName()));
                    int bw2 = WaypointUi.buttonWidth(fr, "Save as " + free);
                    WaypointUi.button(hits, fr, "Replace " + WaypointActions.normalizeName(typedName()), x, y, bw1, WaypointUi.SMALL_H, replaceChosen, true, () -> replaceChosen = true, null);
                    WaypointUi.button(hits, fr, "Save as " + free, x + bw1 + 6, y, bw2, WaypointUi.SMALL_H, !replaceChosen, false, () -> replaceChosen = false, null);
                }
            }

            int btnY = b[1] + b[3] - 12 - WaypointUi.BTN_H;
            int importW = WaypointUi.buttonWidth(fr, Strings.BTN_IMPORT);
            int cancelW = WaypointUi.buttonWidth(fr, Strings.BTN_CANCEL);
            int right = b[0] + b[2] - 16;
            if (valid()) {
                WaypointUi.button(hits, fr, Strings.BTN_IMPORT, right - importW, btnY, importW, WaypointUi.BTN_H, true, false, () -> doImport(gui), null);
            }
            WaypointUi.button(hits, fr, Strings.BTN_CANCEL, right - importW - 6 - cancelW, btnY, cancelW, WaypointUi.BTN_H, false, false, gui::closeModal, null);
        }

        @Override
        public boolean key(WaypointGroupGui gui, char typedChar, int keyCode) {
            if (keyCode == Keyboard.KEY_RETURN) {
                if (valid()) doImport(gui);
                return true;
            }
            if (keyCode == Keyboard.KEY_TAB) {
                return true;
            }
            if (GuiScreenUtils.typeTextField(nameField, typedChar, keyCode)) {
                error = false;
                return true;
            }
            return false;
        }
    }
}
