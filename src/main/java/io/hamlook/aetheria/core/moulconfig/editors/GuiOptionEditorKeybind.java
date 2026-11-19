// SPDX-License-Identifier: LGPL-3.0-only
// Derived from MoulConfig (https://github.com/NotEnoughUpdates/MoulConfig)

package io.hamlook.aetheria.core.moulconfig.editors;

import io.hamlook.aetheria.Resources;
import io.hamlook.aetheria.ThemeContext;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigProcessor;
import io.hamlook.aetheria.utils.KeybindHelper;
import io.hamlook.aetheria.utils.compat.GlStateManagerCompat;
import io.hamlook.aetheria.utils.compat.KeyboardCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.MouseCompat;
import io.hamlook.aetheria.utils.compat.TextureCompat;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;

public class GuiOptionEditorKeybind extends GuiOptionEditor {

    private static final String CAPTURING_LABEL = "Press a key or mouse button";
    private static final ResourceLocation RESET = Resources.RESET;

    private int keyCode;
    private final int defaultKeyCode;
    private boolean editingKeycode;
    private long editStartMs = 0L;

    public GuiOptionEditorKeybind(ConfigProcessor.ProcessedOption option, int keyCode, int defaultKeyCode) {
        super(option);
        this.keyCode = keyCode;
        this.defaultKeyCode = defaultKeyCode;
    }

    @Override
    public void render(int x, int y, int width) {
        super.render(x, y, width);

        int height = getHeight();
        int buttonWidth = keybindButtonWidth(width);
        int groupWidth = buttonWidth + 13;
        int buttonX = controlCenterX(x, width) - groupWidth / 2;
        int buttonY = controlY(y, height, 16);

        ThemeContext.buttonVisual(MinecraftCompat.getFontRenderer(), editingKeycode ? CAPTURING_LABEL : KeybindHelper.getKeyName(keyCode),
                buttonX, buttonY, buttonWidth, 16, true, false, editingKeycode);

        GlStateManagerCompat.color(1, 1, 1, 1);
        TextureCompat.drawBoundTextured(RESET, buttonX + buttonWidth + 3, buttonY + 3, 10, 11, 0, 0, 1, 1);
    }

    @Override
    public boolean mouseInput(int x, int y, int width, int mouseX, int mouseY) {
        return mouseInput(x, y, width, mouseX, mouseY, MouseCompat.getEventButton(), MouseCompat.getEventButtonState());
    }

    private boolean isOverButton(int x, int y, int width, int mouseX, int mouseY) {
        int height = getHeight();
        int buttonWidth = keybindButtonWidth(width);
        int buttonX = controlCenterX(x, width) - (buttonWidth + 13) / 2;
        int buttonY = controlY(y, height, 16);
        return mouseX > buttonX && mouseX < buttonX + buttonWidth
                && mouseY > buttonY && mouseY < buttonY + 16;
    }

    private boolean isOverReset(int x, int y, int width, int mouseX, int mouseY) {
        int height = getHeight();
        int buttonWidth = keybindButtonWidth(width);
        int buttonX = controlCenterX(x, width) - (buttonWidth + 13) / 2;
        int buttonY = controlY(y, height, 16);
        return mouseX > buttonX + buttonWidth + 3 && mouseX < buttonX + buttonWidth + 13
                && mouseY > buttonY + 3 && mouseY < buttonY + 14;
    }

    private int keybindButtonWidth(int width) {
        return Math.max(1, Math.min(60, width / 3 - 23));
    }

    @Override
    public boolean mouseInput(int x, int y, int width, int mouseX, int mouseY, int button, boolean pressed) {
        if (editingKeycode) {
            if (!pressed) return true;
            if (button < 0) return false;
            if (isOverButton(x, y, width, mouseX, mouseY)) {
                editingKeycode = false;
                return true;
            }
            if (isOverReset(x, y, width, mouseX, mouseY)) {
                keyCode = defaultKeyCode;
                option.set(keyCode);
                editingKeycode = false;
                return true;
            }
            if (System.currentTimeMillis() - editStartMs > 200L) {
                keyCode = button - 100;
                option.set(keyCode);
                editingKeycode = false;
                return true;
            }
            return true;
        }

        if (pressed && button == 0) {
            if (isOverButton(x, y, width, mouseX, mouseY)) {
                editingKeycode = true;
                editStartMs = System.currentTimeMillis();
                return true;
            }
            if (isOverReset(x, y, width, mouseX, mouseY)) {
                keyCode = defaultKeyCode;
                option.set(keyCode);
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean capturesEscape() {
        return editingKeycode;
    }

    @Override
    public boolean keyboardInput() {
        return keyboardInput(KeyboardCompat.getEventCharacter(), KeyboardCompat.getEventKey());
    }

    @Override
    public boolean keyboardInput(char typedChar, int keyCode) {
        if (!editingKeycode) return false;
        if (keyCode == -1) return false;

        editingKeycode = false;
        if (KeybindHelper.isEscapeKey(keyCode)) {
            this.keyCode = Keyboard.KEY_NONE;
        } else if (keyCode == 0) {
            this.keyCode = typedChar + 256;
        } else {
            this.keyCode = keyCode;
        }
        option.set(this.keyCode);
        return true;
    }

    @Override
    public void resetTransientState() {
        editingKeycode = false;
    }
}
