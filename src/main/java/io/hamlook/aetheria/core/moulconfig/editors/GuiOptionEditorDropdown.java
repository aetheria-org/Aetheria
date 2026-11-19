// SPDX-License-Identifier: LGPL-3.0-only
// Derived from MoulConfig (https://github.com/NotEnoughUpdates/MoulConfig)

package io.hamlook.aetheria.core.moulconfig.editors;

import io.hamlook.aetheria.ThemeContext;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigProcessor;
import io.hamlook.aetheria.utils.compat.DrawContextUtils;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.MouseCompat;
import io.hamlook.aetheria.utils.render.TextRenderUtils;
import net.minecraft.client.gui.FontRenderer;

public class GuiOptionEditorDropdown extends GuiOptionEditor {

    protected final String[] values;
    private final boolean useOrdinal;
    protected int selected;
    protected boolean open = false;

    public GuiOptionEditorDropdown(ConfigProcessor.ProcessedOption option, String[] values, int selected, boolean useOrdinal) {
        super(option);
        this.values = values;
        this.selected = Math.max(0, Math.min(values.length - 1, selected));
        this.useOrdinal = useOrdinal;
    }

    private int dropdownWidth(int width) {
        return Math.min(width / 3 - 10, 80);
    }

    private int dropdownLeft(int x, int width) {
        return controlCenterX(x, width) - dropdownWidth(width) / 2;
    }

    private int dropdownTop(int y, int height) {
        return controlY(y, height, 14);
    }

    @Override
    public void render(int x, int y, int width) {
        super.render(x, y, width);

        if (!open) {
            int height = getHeight();

            FontRenderer fr = MinecraftCompat.getFontRenderer();
            int dropdownWidth = dropdownWidth(width);
            int left = dropdownLeft(x, width);
            int top = dropdownTop(y, height);

            String selectedString = " - Select - ";
            if (selected >= 0 && selected < values.length) {
                selectedString = values[selected];
            }

            ThemeContext.inset(left, top, dropdownWidth, 14);
            TextRenderUtils.drawStringScaled("\u25BC", fr, left + dropdownWidth - 10, top - 1, false, ThemeContext.ACCENT, 2);

            TextRenderUtils.drawStringScaledMaxWidth(selectedString, fr, left + 3, top + 3, false, dropdownWidth - 16, ThemeContext.TEXT);
        }
    }

    @Override
    public void renderOverlay(int x, int y, int width) {
        if (open) {
            String selectedString = " - Select - ";
            if (selected >= 0 && selected < values.length) {
                selectedString = values[selected];
            }

            int height = getHeight();

            FontRenderer fr = MinecraftCompat.getFontRenderer();
            int dropdownWidth = dropdownWidth(width);
            int left = dropdownLeft(x, width);
            int top = dropdownTop(y, height);

            int dropdownHeight = 13 + 12 * values.length;

            int main = ThemeContext.PANEL;
            int blue = ThemeContext.ACCENT;
            DrawContextUtils.fill(left, top, left + 1, top + dropdownHeight, blue); //Left
            DrawContextUtils.fill(left + 1, top, left + dropdownWidth, top + 1, blue); //Top
            DrawContextUtils.fill(left + dropdownWidth - 1, top + 1, left + dropdownWidth, top + dropdownHeight, blue); //Right
            DrawContextUtils.fill(left + 1, top + dropdownHeight - 1, left + dropdownWidth - 1, top + dropdownHeight, blue); //Bottom
            DrawContextUtils.fill(left + 1, top + 1, left + dropdownWidth - 1, top + dropdownHeight - 1, main); //Middle

            DrawContextUtils.fill(left + 1, top + 14 - 1, left + dropdownWidth - 1, top + 14, blue); //Bar

            int dropdownY = 13;
            for (String option : values) {
                if (option.isEmpty()) {
                    option = "<NONE>";
                }
                TextRenderUtils.drawStringScaledMaxWidth(option, fr, left + 3, top + 3 + dropdownY, false, dropdownWidth - 6, ThemeContext.TEXT);
                dropdownY += 12;
            }

            TextRenderUtils.drawStringScaled("\u25B2", fr, left + dropdownWidth - 10, top - 1, false, ThemeContext.ACCENT, 2);

            TextRenderUtils.drawStringScaledMaxWidth(selectedString, fr, left + 3, top + 3, false, dropdownWidth - 16, ThemeContext.TEXT);
        }
    }

    @Override
    public boolean mouseInput(int x, int y, int width, int mouseX, int mouseY) {
        return mouseInput(x, y, width, mouseX, mouseY, MouseCompat.getEventButton(), MouseCompat.getEventButtonState());
    }

    @Override
    public boolean mouseInput(int x, int y, int width, int mouseX, int mouseY, int button, boolean pressed) {
        int height = getHeight();

        int left = dropdownLeft(x, width);
        int dropdownWidth = dropdownWidth(width);
        int top = dropdownTop(y, height);

        if (pressed && button == 0) {
            if (mouseX >= left && mouseX <= left + dropdownWidth && mouseY >= top && mouseY <= top + 14) {
                open = !open;
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseInputOverlay(int x, int y, int width, int mouseX, int mouseY) {
        return mouseInputOverlay(x, y, width, mouseX, mouseY, MouseCompat.getEventButton(), MouseCompat.getEventButtonState());
    }

    @Override
    public boolean mouseInputOverlay(int x, int y, int width, int mouseX, int mouseY, int button, boolean pressed) {
        int height = getHeight();

        int left = dropdownLeft(x, width);
        int dropdownWidth = dropdownWidth(width);
        int top = dropdownTop(y, height);

        if (pressed && button == 0) {
            if (!(mouseX >= left && mouseX <= left + dropdownWidth && mouseY >= top && mouseY <= top + 14) && open) {
                open = false;
                if (mouseX >= left && mouseX <= left + dropdownWidth) {
                    int dropdownY = 13;
                    for (int ordinal = 0; ordinal < values.length; ordinal++) {
                        if (mouseY >= top + 3 + dropdownY && mouseY <= top + 3 + dropdownY + 12) {
                            selected = ordinal;
                            if (useOrdinal) {
                                option.set(selected);
                            } else {
                                option.set(values[selected]);
                            }
                            return true;
                        }
                        dropdownY += 12;
                    }
                }
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean keyboardInput() {
        return false;
    }
}
