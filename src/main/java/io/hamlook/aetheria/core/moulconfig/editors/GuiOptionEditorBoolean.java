// SPDX-License-Identifier: LGPL-3.0-only
// Derived from MoulConfig (https://github.com/NotEnoughUpdates/MoulConfig)

package io.hamlook.aetheria.core.moulconfig.editors;

import io.hamlook.aetheria.core.moulconfig.gui.GuiElementBoolean;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigProcessor;

public class GuiOptionEditorBoolean extends GuiOptionEditor {

    private final GuiElementBoolean bool;

    public GuiOptionEditorBoolean(ConfigProcessor.ProcessedOption option) {
        super(option);
        bool = new GuiElementBoolean(0, 0, (boolean) option.get(), 10, option::set);
    }

    @Override
    public void render(int x, int y, int width) {
        super.render(x, y, width);
        int height = getHeight();

        bool.x = x + width / 6 - 24;
        bool.y = y + height - 7 - 14;
        bool.render();
    }

    @Override
    public boolean mouseInput(int x, int y, int width, int mouseX, int mouseY) {
        int height = getHeight();
        bool.x = x + width / 6 - 24;
        bool.y = y + height - 7 - 14;
        return bool.mouseInput(mouseX, mouseY);
    }

    @Override
    public boolean mouseInput(int x, int y, int width, int mouseX, int mouseY, int button, boolean pressed) {
        int height = getHeight();
        bool.x = x + width / 6 - 24;
        bool.y = y + height - 7 - 14;
        return bool.mouseInput(mouseX, mouseY, button, pressed);
    }

    @Override
    public boolean keyboardInput() {
        return false;
    }
}
