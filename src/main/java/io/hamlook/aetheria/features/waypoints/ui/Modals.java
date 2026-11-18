package io.hamlook.aetheria.features.waypoints.ui;

import io.hamlook.aetheria.ThemeContext;

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

        public abstract void render(WaypointGroupGui gui, FontRenderer fr, ThemeContext.HitList hits);

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
            ThemeContext.rect(b[0] - 1, b[1] - 1, b[2] + 2, b[3] + 2, ThemeContext.withAlpha(ThemeContext.EDGE, alpha));
            ThemeContext.rect(b[0], b[1], b[2], b[3], ThemeContext.withAlpha(ThemeContext.PANEL, alpha));
            ThemeContext.border(b[0], b[1], b[2], b[3], ThemeContext.LINE);
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
        public void render(WaypointGroupGui gui, FontRenderer fr, ThemeContext.HitList hits) {
            List<String> lines = lines(fr);
            int h = 12 + 12 + lines.size() * ThemeContext.LINE_H + 10 + ThemeContext.BTN_H + 12;
            int[] b = box(300, h);
            frame(b, gui.modalAlpha());

            int x = b[0] + 20;
            int y = b[1] + 12;
            ThemeContext.text(fr, title, x, y, ThemeContext.TEXT);
            y += 12;
            for (String line : lines) {
                ThemeContext.text(fr, line, x, y, ThemeContext.MUTED);
                y += ThemeContext.LINE_H;
            }
            int btnY = b[1] + b[3] - 12 - ThemeContext.BTN_H;
            int confirmW = ThemeContext.buttonWidth(fr, confirmLabel);
            int cancelW = ThemeContext.buttonWidth(fr, Strings.BTN_CANCEL);
            int right = b[0] + b[2] - 20;
            ThemeContext.button(hits, fr, confirmLabel, right - confirmW, btnY, confirmW, ThemeContext.BTN_H, !danger, danger, () -> {
                gui.closeModal();
                if (onConfirm != null) onConfirm.run();
            }, null);
            ThemeContext.button(hits, fr, Strings.BTN_CANCEL, right - confirmW - 6 - cancelW, btnY, cancelW, ThemeContext.BTN_H, false, false, gui::closeModal, null);
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
        public void render(WaypointGroupGui gui, FontRenderer fr, ThemeContext.HitList hits) {
            if (entries.isEmpty()) return;
            int w = 120;
            for (Entry e : entries) w = Math.max(w, ThemeContext.width(fr, e.label) + 24);
            int h = entries.size() * 18 + 4;
            int sw = GuiScreenUtils.getScaledWindowWidth();
            int sh = GuiScreenUtils.getScaledWindowHeight();
            int bx = Math.min(x, sw - w - 4);
            int by = Math.min(y, sh - h - 4);

            ThemeContext.rect(bx - 1, by - 1, w + 2, h + 2, ThemeContext.withAlpha(ThemeContext.EDGE, gui.modalAlpha()));
            ThemeContext.rect(bx, by, w, h, ThemeContext.withAlpha(ThemeContext.INSET, gui.modalAlpha()));
            ThemeContext.border(bx, by, w, h, ThemeContext.LINE);

            int rowY = by + 2;
            for (Entry e : entries) {
                boolean hover = hits.hover(bx + 2, rowY, w - 4, 18);
                if (hover) ThemeContext.rect(bx + 2, rowY, w - 4, 18, ThemeContext.BTN_HOVER);
                ThemeContext.text(fr, e.label, bx + 8, rowY + 5, e.danger ? ThemeContext.DANGER : ThemeContext.TEXT);
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
        public void render(WaypointGroupGui gui, FontRenderer fr, ThemeContext.HitList hits) {
            int w = 200;
            for (String n : names) w = Math.max(w, ThemeContext.width(fr, n) + 24);
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
                if (hover) ThemeContext.rect(b[0] + 4, rowY, w - 8, 18, ThemeContext.BTN_HOVER);
                boolean active = gui.isRouteActive(WaypointStorage.getInstance().getGroup(name));
                ThemeContext.text(fr, name, b[0] + 10, rowY + 5, active ? ThemeContext.ACCENT : ThemeContext.TEXT);
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
        public void render(WaypointGroupGui gui, FontRenderer fr, ThemeContext.HitList hits) {
            int w = 320;
            boolean ok = valid();
            int h = ok ? 164 : 150;
            int[] b = box(w, h);
            frame(b, gui.modalAlpha());
            int mx = hits.mouseX(), my = hits.mouseY();

            int x = b[0] + 16;
            int y = b[1] + 12;
            ThemeContext.text(fr, Strings.IMPORT_TITLE, x, y, ThemeContext.TEXT);
            y += 18;

            if (!ok) {
                String msg = parsed == null || parsed.getError() == null ? Strings.IMPORT_EMPTY : parsed.getError();
                for (String line : wrap(fr, msg, w - 32)) {
                    ThemeContext.text(fr, line, x, y, ThemeContext.DANGER);
                    y += ThemeContext.LINE_H;
                }
                y += 4;
                ThemeContext.text(fr, Strings.IMPORT_READS, x, y, ThemeContext.MUTED);
                y += ThemeContext.LINE_H;
                ThemeContext.text(fr, Strings.IMPORT_TRY, x, y, ThemeContext.MUTED);
            } else {
                ThemeContext.text(fr, Strings.IMPORT_FOUND + parsed.getGroup().waypoints.size() + " waypoints", x, y, ThemeContext.TEXT);
                y += ThemeContext.LINE_H;
                ThemeContext.text(fr, Strings.IMPORT_FORMAT + formatLabel(), x, y, ThemeContext.MUTED);
                y += ThemeContext.LINE_H;
                if (parsed.getSkipped() > 0) {
                    ThemeContext.text(fr, parsed.getSkipped() + Strings.IMPORT_SKIPPED, x, y, ThemeContext.MUTED);
                    y += ThemeContext.LINE_H;
                }
                y += 6;
                ThemeContext.text(fr, Strings.IMPORT_NAME, x, y, ThemeContext.MUTED);
                y += 11;

                int fieldY = y, fieldW = w - 32;
                ThemeContext.rect(x, fieldY, fieldW, ThemeContext.FIELD_H, ThemeContext.INSET);
                ThemeContext.border(x, fieldY, fieldW, ThemeContext.FIELD_H, error ? ThemeContext.DANGER : ThemeContext.LINE);
                GuiScreenUtils.setX(nameField, x + 4);
                GuiScreenUtils.setY(nameField, fieldY + 4);
                GuiScreenUtils.setW(nameField, fieldW - 8);
                GuiScreenUtils.setH(nameField, 10);
                GuiScreenUtils.tickTextField(nameField);
                GuiScreenUtils.renderTextField(nameField, mx, my);
                y += ThemeContext.FIELD_H + 6;

                boolean nameTaken = !WaypointActions.normalizeName(typedName()).isEmpty() && WaypointStorage.getInstance().getGroup(WaypointActions.normalizeName(typedName())) != null;
                if (nameTaken) {
                    String free = WaypointActions.uniqueName(WaypointActions.normalizeName(typedName()));
                    int bw1 = ThemeContext.buttonWidth(fr, "Replace " + WaypointActions.normalizeName(typedName()));
                    int bw2 = ThemeContext.buttonWidth(fr, "Save as " + free);
                    ThemeContext.button(hits, fr, "Replace " + WaypointActions.normalizeName(typedName()), x, y, bw1, ThemeContext.SMALL_H, replaceChosen, true, () -> replaceChosen = true, null);
                    ThemeContext.button(hits, fr, "Save as " + free, x + bw1 + 6, y, bw2, ThemeContext.SMALL_H, !replaceChosen, false, () -> replaceChosen = false, null);
                }
            }

            int btnY = b[1] + b[3] - 12 - ThemeContext.BTN_H;
            int importW = ThemeContext.buttonWidth(fr, Strings.BTN_IMPORT);
            int cancelW = ThemeContext.buttonWidth(fr, Strings.BTN_CANCEL);
            int right = b[0] + b[2] - 16;
            if (valid()) {
                ThemeContext.button(hits, fr, Strings.BTN_IMPORT, right - importW, btnY, importW, ThemeContext.BTN_H, true, false, () -> doImport(gui), null);
            }
            ThemeContext.button(hits, fr, Strings.BTN_CANCEL, right - importW - 6 - cancelW, btnY, cancelW, ThemeContext.BTN_H, false, false, gui::closeModal, null);
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
