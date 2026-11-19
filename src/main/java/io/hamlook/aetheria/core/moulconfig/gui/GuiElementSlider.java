// SPDX-License-Identifier: LGPL-3.0-only
// Derived from MoulConfig (https://github.com/NotEnoughUpdates/MoulConfig)

package io.hamlook.aetheria.core.moulconfig.gui;

import io.hamlook.aetheria.ThemeContext;
import io.hamlook.aetheria.utils.KeybindHelper;
import io.hamlook.aetheria.utils.compat.GuiScreenUtils;
import io.hamlook.aetheria.utils.compat.MouseCompat;
import lombok.Setter;

import java.util.function.Consumer;


public class GuiElementSlider extends GuiElement {

    public int x, y, width;
    private static final int HEIGHT = 16;

    private final float minValue, maxValue, minStep;
    @Setter
    private float value;
    private final Consumer<Float> setCallback;
    private boolean clicked = false;

    public GuiElementSlider(int x, int y, int width,
                            float minValue, float maxValue, float minStep,
                            float value, Consumer<Float> setCallback) {
        if (minStep < 0) minStep = 0.01f;
        this.x = x; this.y = y; this.width = width;
        this.minValue = minValue; this.maxValue = maxValue; this.minStep = minStep;
        this.value = value;
        this.setCallback = setCallback;
    }

    @Override
    public void render() {
        int screenW = GuiScreenUtils.getScaledWindowWidth();
        int mouseX = KeybindHelper.getMouseCoords(screenW, GuiScreenUtils.getScaledWindowHeight())[0];

        float v = value;
        if (clicked) {
            v = snap((mouseX - x) * (maxValue - minValue) / width + minValue);
        }

        ThemeContext.slider(x, y, width, v, minValue, maxValue);
    }

    @Override
    public boolean mouseInput(int mouseX, int mouseY) {
        return mouseInput(mouseX, mouseY, MouseCompat.getEventButton(), MouseCompat.getEventButtonState());
    }

    @Override
    public boolean mouseInput(int mouseX, int mouseY, int button, boolean pressed) {
        if (button == 0) {
            if (pressed) {
                clicked = isInside(mouseX, mouseY);
                if (!clicked) return false;
                snapTo(mouseX);
                return true;
            }
            if (!clicked) return false;
            clicked = false;
            snapTo(mouseX);
            return true;
        }
        if (button == -1 && !pressed && clicked && MouseCompat.isButtonDown(0)) {
            snapTo(mouseX);
            return true;
        }
        return false;
    }

    private boolean isInside(int mouseX, int mouseY) {
        return mouseX > x && mouseX < x + width && mouseY > y && mouseY < y + HEIGHT;
    }

    private void snapTo(int mouseX) {
        value = snap((mouseX - x) * (maxValue - minValue) / width + minValue);
        setCallback.accept(value);
    }

    @Override
    public boolean keyboardInput() { return false; }

    private float snap(float raw) {
        raw = Math.max(minValue, Math.min(maxValue, raw));
        return Math.round(raw / minStep) * minStep;
    }

    public void resetDragState() {
        clicked = false;
    }
}
