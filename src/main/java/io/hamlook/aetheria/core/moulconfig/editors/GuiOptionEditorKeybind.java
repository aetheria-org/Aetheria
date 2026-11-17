// SPDX-License-Identifier: LGPL-3.0-only
// Derived from MoulConfig (https://github.com/NotEnoughUpdates/MoulConfig)

package io.hamlook.aetheria.core.moulconfig.editors;

import io.hamlook.aetheria.Resources;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigProcessor;
import io.hamlook.aetheria.utils.KeybindHelper;
import io.hamlook.aetheria.utils.compat.GlStateManagerCompat;
import io.hamlook.aetheria.utils.compat.KeyboardCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.TextureCompat;
import io.hamlook.aetheria.utils.compat.MouseCompat;
import io.hamlook.aetheria.utils.render.TextRenderUtils;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;

import static io.hamlook.aetheria.Resources.button_tex;

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

        GlStateManagerCompat.color(1, 1, 1, 1);
        TextureCompat.drawBoundTextured(button_tex, x + width / 6 - 24, y + height - 7 - 14, 48, 16, 0, 0, 1, 1);

        String text = editingKeycode ? CAPTURING_LABEL : KeybindHelper.getKeyName(keyCode);
        TextRenderUtils.drawStringCenteredScaledMaxWidth(text, MinecraftCompat.getFontRenderer(), x + width / 6, y + height - 7 - 6, false, 40, 0xFF303030);

        GlStateManagerCompat.color(1, 1, 1, 1);
        TextureCompat.drawBoundTextured(RESET, x + width / 6 - 24 + 48 + 3, y + height - 7 - 14 + 3, 10, 11, 0, 0, 1, 1);
    }

    @Override
    public boolean mouseInput(int x, int y, int width, int mouseX, int mouseY) {
        return mouseInput(x, y, width, mouseX, mouseY, MouseCompat.getEventButton(), MouseCompat.getEventButtonState());
    }

    private boolean isOverButton(int x, int y, int width, int mouseX, int mouseY) {
        int height = getHeight();
        return mouseX > x + width / 6 - 24 && mouseX < x + width / 6 + 24
                && mouseY > y + height - 7 - 14 && mouseY < y + height - 7 + 2;
    }

    private boolean isOverReset(int x, int y, int width, int mouseX, int mouseY) {
        int height = getHeight();
        return mouseX > x + width / 6 - 24 + 48 + 3 && mouseX < x + width / 6 - 24 + 48 + 13
                && mouseY > y + height - 7 - 14 + 3 && mouseY < y + height - 7 - 14 + 3 + 11;
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
