// SPDX-License-Identifier: LGPL-3.0-only
// Derived from MoulConfig (https://github.com/NotEnoughUpdates/MoulConfig)

package io.hamlook.aetheria.core.moulconfig.gui;

import io.hamlook.aetheria.Resources;
import io.hamlook.aetheria.utils.KeybindHelper;
import io.hamlook.aetheria.utils.Utils;
import io.hamlook.aetheria.utils.compat.GlStateManagerCompat;
import io.hamlook.aetheria.utils.compat.TextureCompat;
import io.hamlook.aetheria.utils.compat.GuiScreenUtils;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.MouseCompat;

import org.lwjgl.opengl.GL11;

import java.util.function.Consumer;


public class GuiElementSlider extends GuiElement {

    public int x, y, width;
    private static final int HEIGHT = 16;

    private final float minValue, maxValue, minStep;
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

    public void setValue(float value) { this.value = value; }

    @Override
    public void render() {
        int screenW = GuiScreenUtils.getScaledWindowWidth();
        int screenH = GuiScreenUtils.getScaledWindowHeight();
        int mouseX = KeybindHelper.getMouseCoords(screenW, screenH)[0];

        float v = value;
        if (clicked) {
            v = snap((mouseX - x) * (maxValue - minValue) / width + minValue);
        }

        float sliderAmt = Math.max(0, Math.min(1, (v - minValue) / (maxValue - minValue)));
        int   sliderAmtI = (int) (width * sliderAmt);

        GlStateManagerCompat.color(1, 1, 1, 1);

        TextureCompat.drawBoundTextured(Resources.slider_on_cap, x, y, 4, HEIGHT, 0, 0, 1, 1);
        TextureCompat.drawBoundTextured(Resources.slider_off_cap, x + width - 4, y, 4, HEIGHT, 0, 0, 1, 1);

        if (sliderAmtI > 5) {
            TextureCompat.drawBoundTextured(Resources.slider_on_segment, x + 4, y, sliderAmtI - 4, HEIGHT, 0, 0, 1, 1);
        }
        if (sliderAmtI < width - 5) {
            TextureCompat.drawBoundTextured(Resources.slider_off_segment, x + sliderAmtI, y, width - 4 - sliderAmtI, HEIGHT, 0, 0, 1, 1);
        }

        for (int i = 1; i < 4; i++) {
            int notchX = x + width * i / 4 - 1;
            TextureCompat.drawBoundTextured(
                    notchX > x + sliderAmtI ? Resources.slider_off_notch : Resources.slider_on_notch,
                    notchX, y + (HEIGHT - 4f) / 2, 2, 4, 0, 0, 1, 1);
        }

        TextureCompat.drawBoundTextured(Resources.slider_button_new, x + sliderAmtI - 4, y, 8, HEIGHT, 0, 0, 1, 1);
    }

    @Override
    public boolean mouseInput(int mouseX, int mouseY) {
        return mouseInput(mouseX, mouseY, MouseCompat.getEventButton(), MouseCompat.getEventButtonState());
    }

    @Override
    public boolean mouseInput(int mouseX, int mouseY, int button, boolean pressed) {
        if (!MouseCompat.isButtonDown(0)) clicked = false;

        if (button == 0) {
            clicked = pressed
                    && mouseX > x && mouseX < x + width
                    && mouseY > y && mouseY < y + HEIGHT;
            if (clicked) {
                value = snap((mouseX - x) * (maxValue - minValue) / width + minValue);
                setCallback.accept(value);
                return true;
            }
        }

        if (!pressed && (button == -1 || button == 0) && clicked) {
            value = snap((mouseX - x) * (maxValue - minValue) / width + minValue);
            setCallback.accept(value);
            return true;
        }
        return false;
    }

    @Override
    public boolean keyboardInput() { return false; }

    private float snap(float raw) {
        raw = Math.max(minValue, Math.min(maxValue, raw));
        return Math.round(raw / minStep) * minStep;
    }
}