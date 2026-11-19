// SPDX-License-Identifier: LGPL-3.0-only
// Derived from MoulConfig (https://github.com/NotEnoughUpdates/MoulConfig)

package io.hamlook.aetheria.core.moulconfig.editors;

import io.hamlook.aetheria.ThemeContext;
import io.hamlook.aetheria.core.moulconfig.gui.GuiElementTextField;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigProcessor;
import io.hamlook.aetheria.utils.compat.KeyboardCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.MouseCompat;

public class GuiOptionEditorText extends GuiOptionEditor {

    private final GuiElementTextField textField;

    public GuiOptionEditorText(ConfigProcessor.ProcessedOption option) {
        super(option);
        Object value = option.get();
        textField = new GuiElementTextField(value instanceof String ? (String) value : "", GuiElementTextField.SCISSOR_TEXT);
    }

    @Override
    public void render(int x, int y, int width) {
        super.render(x, y, width);
        int height = getHeight();

        int maxWidth = Math.max(1, width / 3 - 10);
        int fullWidth = Math.max(1, Math.min(maxWidth, 80));
        if (textField.getFocus()) {
            fullWidth = Math.min(maxWidth, Math.max(fullWidth, MinecraftCompat.getFontRenderer().getStringWidth(textField.getText()) + 10));
        }

        int textFieldX = controlCenterX(x, width) - fullWidth / 2;
        textField.setSize(fullWidth, 16);

        textField.render(textFieldX, controlY(y, height, 16));
    }

    @Override
    public boolean mouseInput(int x, int y, int width, int mouseX, int mouseY) {
        return mouseInput(x, y, width, mouseX, mouseY, MouseCompat.getEventButton(), MouseCompat.getEventButtonState());
    }

    @Override
    public boolean mouseInput(int x, int y, int width, int mouseX, int mouseY, int button, boolean pressed) {
        int height = getHeight();

        int maxWidth = Math.max(1, width / 3 - 10);
        int fullWidth = Math.max(1, Math.min(maxWidth, 80));

        if (textField.getFocus()) {
            fullWidth = Math.min(maxWidth, Math.max(fullWidth, MinecraftCompat.getFontRenderer().getStringWidth(textField.getText()) + 10));
        }

        int textFieldX = controlCenterX(x, width) - fullWidth / 2;
        int textFieldY = controlY(y, height, 16);
        textField.setSize(fullWidth, 16);

        if (pressed && (button == 0 || button == 1)) {
            if (mouseX > textFieldX && mouseX < textFieldX + fullWidth && mouseY > textFieldY && mouseY < textFieldY + 16) {
                textField.mouseClicked(mouseX, mouseY, button);
                return true;
            }
            textField.unfocus();
        }

        return false;
    }

    @Override
    public boolean capturesEscape() {
        return textField.getFocus();
    }

    @Override
    public boolean keyboardInput() {
        return keyboardInput(KeyboardCompat.getEventCharacter(), KeyboardCompat.getEventKey());
    }

    @Override
    public boolean keyboardInput(char typedChar, int keyCode) {
        if (textField.getFocus()) {
            KeyboardCompat.enableRepeatEvents(true);
            textField.keyTyped(typedChar, keyCode);

            try {
                textField.setCustomBorderColour(ThemeContext.ACCENT);
                option.set(textField.getText());
            } catch (Exception e) {
                textField.setCustomBorderColour(ThemeContext.DANGER);
            }

            return true;
        }
        return false;
    }

    @Override
    public void resetTransientState() {
        textField.unfocus();
    }
}
