// SPDX-License-Identifier: LGPL-3.0-only
// Derived from MoulConfig (https://github.com/NotEnoughUpdates/MoulConfig)

package io.hamlook.aetheria.core.moulconfig.editors;

import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.ThemeContext;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigProcessor;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.MouseCompat;

import java.lang.reflect.Method;

public class GuiOptionEditorButton extends GuiOptionEditor {

    private final String runnableId;
    private String buttonText;
    private final Object config;

    public GuiOptionEditorButton(ConfigProcessor.ProcessedOption option, String runnableId, String buttonText, Object config) {
        super(option);
        this.runnableId = runnableId;
        this.config = config;

        this.buttonText = buttonText;
        if (this.buttonText != null && this.buttonText.isEmpty()) this.buttonText = null;
    }

    @Override
    public void render(int x, int y, int width) {
        super.render(x, y, width);

        int height = getHeight();

        int buttonWidth = Math.max(1, Math.min(120, width / 3 - 10));
        int buttonX = controlCenterX(x, width) - buttonWidth / 2;
        int buttonY = controlY(y, height, 16);
        ThemeContext.buttonVisual(MinecraftCompat.getFontRenderer(), buttonText == null ? "" : buttonText,
                buttonX, buttonY, buttonWidth, 16, true, false, false);
    }

    @Override
    public boolean mouseInput(int x, int y, int width, int mouseX, int mouseY) {
        return mouseInput(x, y, width, mouseX, mouseY, MouseCompat.getEventButton(), MouseCompat.getEventButtonState());
    }

    @Override
    public boolean mouseInput(int x, int y, int width, int mouseX, int mouseY, int button, boolean pressed) {
        if (pressed) {
            int height = getHeight();
            int buttonWidth = Math.max(1, Math.min(120, width / 3 - 10));
            int buttonLeft = controlCenterX(x, width) - buttonWidth / 2;
            int buttonY = controlY(y, height, 16);
            if (mouseX > buttonLeft && mouseX < buttonLeft + buttonWidth && mouseY > buttonY && mouseY < buttonY + 16) {
                invokeRunnable(runnableId);
                return true;
            }
        }

        return false;
    }

    private void invokeRunnable(String id) {
        try {
            Method method = config.getClass().getMethod("executeRunnable", String.class);
            method.invoke(config, id);
        } catch (Exception e) {
            Aetheria.logger.warning("Config button '" + id + "' failed: " + e.getMessage());
        }
    }

    @Override
    public boolean keyboardInput() {
        return false;
    }
}
