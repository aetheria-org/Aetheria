// SPDX-License-Identifier: LGPL-3.0-only
// Derived from MoulConfig (https://github.com/NotEnoughUpdates/MoulConfig)

package io.hamlook.aetheria.core.moulconfig.gui;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.utils.KeybindHelper;
import io.hamlook.aetheria.utils.compat.AetheriaBaseScreen;

import java.io.IOException;

public class GuiScreenElementWrapper extends AetheriaBaseScreen {

    public final GuiElement element;

    public GuiScreenElementWrapper(GuiElement element) {
        this.element = element;
    }

    @Override
    public void onDrawScreen(int mouseX, int mouseY, float partialTicks) {
        element.render();
    }

    @Override
    public void onHandleMouseInput() {
        int i = KeybindHelper.getScaledEventX(this.width);
        int j = KeybindHelper.getScaledEventY(this.height);
        element.mouseInput(i, j);
    }

    @Override
    public void handleKeyboardInput() throws IOException {
        super.handleKeyboardInput();
        element.keyboardInput();
    }

    @Override
    public void guiClosed() {
        ATHRConfig.saveConfig();
    }
}