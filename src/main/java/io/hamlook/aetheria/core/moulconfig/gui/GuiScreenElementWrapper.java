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

    // 1.8.9 input is polled per-event below; event-driven hooks would double-dispatch.
    //#if MC >= 1.21
    //$$ @Override
    //$$ public void onMouseClicked(int mouseX, int mouseY, int mouseButton) {
    //$$     element.mouseInput(mouseX, mouseY, mouseButton, true);
    //$$ }
    //$$
    //$$ @Override
    //$$ public void onMouseReleased(int mouseX, int mouseY, int state) {
    //$$     element.mouseInput(mouseX, mouseY, state, false);
    //$$ }
    //$$
    //$$ @Override
    //$$ public void onMouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
    //$$     element.mouseInput(mouseX, mouseY, -1, false);
    //$$ }
    //#endif

    //#if MC < 1.21
    @Override
    public void onHandleMouseInput() {
        int i = KeybindHelper.getScaledEventX(this.width);
        int j = KeybindHelper.getScaledEventY(this.height);
        element.mouseInput(i, j);
    }
    //#else
    //$$ @Override
    //$$ public void onMouseScrolled(int mouseX, int mouseY, double verticalAmount) {
    //$$     element.mouseInput(mouseX, mouseY);
    //$$ }
    //#endif

    //#if MC < 1.21
    @Override
    public void handleKeyboardInput() throws IOException {
        super.handleKeyboardInput();
        element.keyboardInput();
    }
    //#else
    //$$ @Override
    //$$ protected void onKeyTyped(char typedChar, int keyCode) {
    //$$     element.keyboardInput(typedChar, keyCode);
    //$$ }
    //$$
    //$$ @Override
    //$$ protected void onCharTyped(char typedChar, int modifiers) {
    //$$     element.keyboardInput(typedChar, -1);
    //$$ }
    //#endif

    @Override
    public void guiClosed() {
        ATHRConfig.saveConfig();
    }
}