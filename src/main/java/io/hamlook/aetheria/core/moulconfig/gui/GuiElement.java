// SPDX-License-Identifier: LGPL-3.0-only
// Derived from MoulConfig (https://github.com/NotEnoughUpdates/MoulConfig)

package io.hamlook.aetheria.core.moulconfig.gui;

public abstract class GuiElement {

    public abstract void render();

    public abstract boolean mouseInput(int mouseX, int mouseY);

    /**
     * Button-aware variant. Default keeps legacy latch behavior; override
     * where press/release must come from the event (modern).
     */
    public boolean mouseInput(int mouseX, int mouseY, int button, boolean pressed) {
        return mouseInput(mouseX, mouseY);
    }

    public abstract boolean keyboardInput();

    /**
     * Key-aware variant. Default keeps legacy latch behavior; override where
     * the typed char and key code must come from the event (modern).
     * A keyCode of -1 marks a char-only event with no key code.
     */
    public boolean keyboardInput(char typedChar, int keyCode) {
        return keyboardInput();
    }
}