package io.hamlook.aetheria.features.waypoints.ui;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.moulconfig.gui.GuiElementColour;
import io.hamlook.aetheria.features.waypoints.Strings;
import io.hamlook.aetheria.features.waypoints.WaypointActions;
import io.hamlook.aetheria.features.waypoints.WaypointGroupGui;
import net.minecraft.client.gui.FontRenderer;

import java.util.Map;

public final class SettingsTab implements MenuTab {

    private GuiElementColour colourEditor;

    @Override
    public String title() {
        return Strings.TAB_SETTINGS;
    }

    @Override
    public boolean capturesEscape() {
        return colourEditor != null;
    }

    private void closeColourEditor() {
        colourEditor = null;
    }

    @Override
    public void render(WaypointGroupGui gui, FontRenderer fr, WaypointUi.HitList hits, int mouseX, int mouseY) {
        boolean editorOpen = colourEditor != null;
        if (editorOpen) hits.setMouse(-1, -1);
        int x = gui.contentX();
        int w = gui.contentW();
        int y = gui.contentY() + 6;

        y = section(fr, x, y, w, Strings.SETTINGS_AUTO);
        y = sliderRow(fr, hits, gui, x, y, w, Strings.RANGE,
                Strings.RANGE_DESC,
                WaypointActions.getAdvanceRange(),
                WaypointActions.MIN_RANGE, WaypointActions.MAX_RANGE, 0.5, " blocks",
                value -> gui.toast(WaypointActions.setAdvanceRange(value)));
        y = sliderRow(fr, hits, gui, x, y, w, Strings.DELAY,
                Strings.DELAY_DESC,
                WaypointActions.getAdvanceDelayMs(),
                WaypointActions.MIN_DELAY_MS, WaypointActions.MAX_DELAY_MS, 250, " ms",
                value -> gui.toast(WaypointActions.setAdvanceDelayMs(Math.round(value))));

        y += 4;
        y = section(fr, x, y, w, Strings.SETTINGS_COLORS);
        Map<String, String> defaults = WaypointActions.colorDefaults();
        for (String key : WaypointActions.COLOR_KEYS) {
            y = colourRow(fr, hits, gui, x, y, w, key, defaults);
        }

        y += 6;
        int resetW = WaypointUi.buttonWidth(fr, Strings.SETTINGS_RESET);
        WaypointUi.button(hits, fr, Strings.SETTINGS_RESET, x, y, resetW, WaypointUi.BTN_H, false, true, () ->
                        gui.openModal(new Modals.Confirm(Strings.SETTINGS_RESET_TITLE,
                                Strings.SETTINGS_RESET_BODY,
                                "Reset", true, () -> {
                            WaypointActions.resetAdvanceDefaults();
                            gui.toast("Reset settings to defaults.", true);
                        })),
                Strings.TOOLTIP_SETTINGS_RESET);
        int openW = WaypointUi.buttonWidth(fr, Strings.OPEN_CONFIG);
        WaypointUi.button(hits, fr, Strings.OPEN_CONFIG, x + resetW + 6, y, openW, WaypointUi.BTN_H, false, false,
                () -> ATHRConfig.openCategory("Waypoints"), Strings.TOOLTIP_OPEN_CONFIG);

        if (editorOpen) hits.setMouse(mouseX, mouseY);
    }

    @Override
    public boolean hasOverlay() {
        return colourEditor != null;
    }

    @Override
    public void renderOverlay(WaypointGroupGui gui, FontRenderer fr) {
        if (colourEditor != null) colourEditor.render();
    }

    private int section(FontRenderer fr, int x, int y, int w, String title) {
        WaypointUi.text(fr, title, x, y, WaypointUi.ACCENT);
        y += 11;
        WaypointUi.divider(x, y, w);
        return y + 4;
    }

    private int sliderRow(FontRenderer fr, WaypointUi.HitList hits, WaypointGroupGui gui, int x, int y, int w,
                          String label, String description, double value,
                          double min, double max, double step, String unit,
                          java.util.function.Consumer<Double> apply) {
        int sliderW = 150;
        int sliderX = x + w - sliderW - 54;
        WaypointUi.text(fr, label, x, y + 2, WaypointUi.TEXT);
        WaypointUi.text(fr, WaypointUi.fit(fr, description, sliderX - x - 8), x, y + 13, WaypointUi.MUTED);
        WaypointUi.textRight(fr, WaypointUi.valueLabel(value) + unit, x + w, y + 6, WaypointUi.MUTED);
        WaypointUi.slider(sliderX, y + 4, sliderW, value, min, max);
        WaypointUi.SliderDrag drag = new WaypointUi.SliderDrag(sliderX, sliderW, min, max, step, apply::accept);
        hits.add(sliderX, y + 4, sliderW, 16, null, () -> gui.startSliderDrag(drag, hits.mouseX()));
        return y + 26;
    }

    private int colourRow(FontRenderer fr, WaypointUi.HitList hits, WaypointGroupGui gui, int x, int y, int w,
                          String key, Map<String, String> defaults) {
        String value = WaypointActions.getColor(key);
        String name = WaypointActions.colorName(key);
        WaypointUi.text(fr, name, x, y + 4, WaypointUi.TEXT);
        int swX = x + w - 40;
        WaypointUi.rect(swX, y + 1, 40, 14, WaypointUi.argbOf(value));
        WaypointUi.border(swX, y + 1, 40, 14, WaypointUi.LINE);
        boolean changed = !value.equals(defaults.get(key));
        if (changed) WaypointUi.text(fr, Strings.COLOR_RESET, swX - 34, y + 4, WaypointUi.ACCENT);
        hits.add(swX, y + 1, 40, 14, "Change the " + name.toLowerCase() + " color.", () -> {
            if (colourEditor != null) return;
            colourEditor = new GuiElementColour(swX - 80, y + 18, WaypointActions.getColor(key),
                    colour -> WaypointActions.setColor(key, colour), this::closeColourEditor);
        });
        if (changed) {
            hits.add(swX - 34, y + 2, 30, 12, Strings.TOOLTIP_COLOR_RESET, () -> {
                WaypointActions.setColor(key, defaults.get(key));
                gui.toast(name + " color reset.", true);
            });
        }
        return y + 18;
    }

    @Override
    public boolean mouse(WaypointGroupGui gui, int mouseX, int mouseY, int button, boolean pressed) {
        if (colourEditor == null) return false;
        colourEditor.mouseInput(mouseX, mouseY, button, pressed);
        return true;
    }

    @Override
    public boolean key(WaypointGroupGui gui, char typedChar, int keyCode) {
        if (colourEditor == null) return false;
        return colourEditor.keyboardInput(typedChar, keyCode);
    }
}