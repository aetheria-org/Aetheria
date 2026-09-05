// SPDX-License-Identifier: LGPL-3.0-only
// Derived from MoulConfig (https://github.com/NotEnoughUpdates/MoulConfig)

package io.hamlook.aetheria.core.moulconfig.editors;

import io.hamlook.aetheria.core.moulconfig.gui.GuiElementTextField;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigProcessor;
import io.hamlook.aetheria.utils.compat.KeyboardCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.MouseCompat;

public class GuiOptionEditorText extends GuiOptionEditor {

    private final GuiElementTextField textField;

    public GuiOptionEditorText(ConfigProcessor.ProcessedOption option) {
        super(option);
        textField = new GuiElementTextField((String) option.get(), 0);
    }

    @Override
    public void render(int x, int y, int width) {
        super.render(x, y, width);
        int height = getHeight();

        int fullWidth = Math.min(width / 3 - 10, 80);

        int textFieldX = x + width / 6 - fullWidth / 2;
        if (textField.getFocus()) {
            fullWidth = Math.max(fullWidth, MinecraftCompat.getFontRenderer().getStringWidth(textField.getText()) + 10);
        }

        textField.setSize(fullWidth, 16);

        textField.render(textFieldX, y + height - 7 - 14);
    }

    @Override
    public boolean mouseInput(int x, int y, int width, int mouseX, int mouseY) {
        int height = getHeight();

        int fullWidth = Math.min(width / 3 - 10, 80);

        int textFieldX = x + width / 6 - fullWidth / 2;

        if (textField.getFocus()) {
            fullWidth = Math.max(fullWidth, MinecraftCompat.getFontRenderer().getStringWidth(textField.getText()) + 10);
        }

        int textFieldY = y + height - 7 - 14;
        textField.setSize(fullWidth, 16);

        if (MouseCompat.getEventButtonState() && (MouseCompat.getEventButton() == 0 || MouseCompat.getEventButton() == 1)) {
            if (mouseX > textFieldX && mouseX < textFieldX + fullWidth && mouseY > textFieldY && mouseY < textFieldY + 16) {
                textField.mouseClicked(mouseX, mouseY, MouseCompat.getEventButton());
                return true;
            }
            textField.unfocus();
        }

        return false;
    }

    @Override
    public boolean keyboardInput() {
        if (KeyboardCompat.getEventKeyState() && textField.getFocus()) {
            KeyboardCompat.enableRepeatEvents(true);
            textField.keyTyped(KeyboardCompat.getEventCharacter(), KeyboardCompat.getEventKey());

            try {
                textField.setCustomBorderColour(0xffffffff);
                option.set(textField.getText());
            } catch (Exception e) {
                textField.setCustomBorderColour(0xffff0000);
            }

            return true;
        }
        return false;
    }
}
