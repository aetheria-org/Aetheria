// SPDX-License-Identifier: LGPL-3.0-only
// Derived from MoulConfig (https://github.com/NotEnoughUpdates/MoulConfig)

package io.hamlook.aetheria.core.moulconfig.editors;

import io.hamlook.aetheria.core.moulconfig.gui.GuiElementSlider;
import io.hamlook.aetheria.core.moulconfig.gui.GuiElementTextField;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigProcessor;
import io.hamlook.aetheria.utils.compat.KeyboardCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.MouseCompat;

import java.util.Locale;

public class GuiOptionEditorSlider extends GuiOptionEditor {

    private final float minValue;
    private final float maxValue;
    private final GuiElementSlider slider;
    private final GuiElementTextField textField;

    public GuiOptionEditorSlider(ConfigProcessor.ProcessedOption option, float minValue, float maxValue, float minStep) {
        super(option);
        this.minValue = minValue;
        this.maxValue = maxValue;
        if (minStep < 0) minStep = 0.01f;

        float floatVal = ((Number) option.get()).floatValue();
        textField = new GuiElementTextField(displayValue(floatVal),
                GuiElementTextField.NO_SPACE | GuiElementTextField.NUM_ONLY | GuiElementTextField.SCALE_TEXT);

        slider = new GuiElementSlider(
                0,
                0,
                80,
                minValue,
                maxValue,
                minStep,
                floatVal,
                val -> {
                    option.set(val);
                    textField.setText(displayValue(val));
                }
        );
    }

    @Override
    public void render(int x, int y, int width) {
        super.render(x, y, width);
        int height = getHeight();

        int fullWidth = Math.min(width / 3 - 10, 80);
        int sliderWidth = (fullWidth - 5) * 3 / 4;
        int textFieldWidth = (fullWidth - 5) / 4;

        slider.x = x + width / 6 - fullWidth / 2;
        slider.y = y + height - 7 - 14;
        slider.width = sliderWidth;
        slider.render();

        if (textField.getFocus()) {
            textField.setOptions(GuiElementTextField.NO_SPACE | GuiElementTextField.NUM_ONLY);
            textField.setSize(MinecraftCompat.getFontRenderer().getStringWidth(textField.getText()) + 10, 16);
        } else {
            textField.setSize(textFieldWidth, 16);
            textField.setOptions(GuiElementTextField.NO_SPACE | GuiElementTextField.NUM_ONLY | GuiElementTextField.SCALE_TEXT);
        }

        textField.render(x + width / 6 - fullWidth / 2 + sliderWidth + 5, y + height - 7 - 14);
    }

    @Override
    public boolean mouseInput(int x, int y, int width, int mouseX, int mouseY) {
        return mouseInput(x, y, width, mouseX, mouseY, MouseCompat.getEventButton(), MouseCompat.getEventButtonState());
    }

    @Override
    public boolean mouseInput(int x, int y, int width, int mouseX, int mouseY, int button, boolean pressed) {
        int height = getHeight();

        int fullWidth = Math.min(width / 3 - 10, 80);
        int sliderWidth = (fullWidth - 5) * 3 / 4;
        int textFieldWidth = (fullWidth - 5) / 4;

        slider.x = x + width / 6 - fullWidth / 2;
        slider.y = y + height - 7 - 14;
        slider.width = sliderWidth;
        if (slider.mouseInput(mouseX, mouseY, button, pressed)) {
            textField.unfocus();
            return true;
        }

        if (textField.getFocus()) {
            textFieldWidth = MinecraftCompat.getFontRenderer().getStringWidth(textField.getText()) + 10;
        }

        int textFieldX = x + width / 6 - fullWidth / 2 + sliderWidth + 5;
        int textFieldY = y + height - 7 - 14;
        textField.setSize(textFieldWidth, 16);

        if (pressed && (button == 0 || button == 1)) {
            if (mouseX > textFieldX && mouseX < textFieldX + textFieldWidth && mouseY > textFieldY && mouseY < textFieldY + 16) {
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
        if (!textField.getFocus()) return false;

        textField.keyTyped(typedChar, keyCode);

        if (!textField.getFocus()) {
            return true;
        }

        Float parsed = parseValue(textField.getText());
        if (parsed == null) {
            textField.setCustomBorderColour(0xffff0000);
            return true;
        }

        float clamped = clamp(parsed);
        if (option.set(clamped)) {
            slider.setValue(clamped);
            textField.setCustomBorderColour(0xffffffff);
        } else {
            textField.setCustomBorderColour(0xff0000ff);
        }
        return true;
    }

    @Override
    public void resetTransientState() {
        textField.unfocus();
        slider.resetDragState();
    }

    private float clamp(float value) {
        return Math.max(minValue, Math.min(maxValue, value));
    }

    private Float parseValue(String text) {
        try {
            float parsed = Float.parseFloat(text);
            return Float.isFinite(parsed) ? parsed : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String displayValue(float value) {
        return String.format(Locale.ROOT, "%.3f", value).replaceAll("\\.?0+$", "");
    }
}
