// SPDX-License-Identifier: LGPL-3.0-only
// Derived from MoulConfig (https://github.com/NotEnoughUpdates/MoulConfig)

package io.hamlook.aetheria.core.moulconfig.editors;

import io.hamlook.aetheria.Resources;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigProcessor;
import io.hamlook.aetheria.utils.KeybindHelper;
import io.hamlook.aetheria.utils.compat.GlStateManagerCompat;
import io.hamlook.aetheria.utils.compat.KeyboardCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.MouseCompat;
import io.hamlook.aetheria.utils.render.RenderUtils;
import io.hamlook.aetheria.utils.render.TextRenderUtils;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import static io.hamlook.aetheria.Resources.button_tex;

public class GuiOptionEditorKeybind extends GuiOptionEditor {

    private static final ResourceLocation RESET = Resources.RESET;

    private int keyCode;
    private final int defaultKeyCode;
    private boolean editingKeycode;

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
        MinecraftCompat.getMinecraft().getTextureManager().bindTexture(button_tex);
        RenderUtils.drawTexturedRect(x + width / 6 - 24, y + height - 7 - 14, 48, 16);

        String keyName = KeybindHelper.getKeyName(keyCode);
        String text = editingKeycode ? "> " + keyName + " <" : keyName;
        TextRenderUtils.drawStringCenteredScaledMaxWidth(text, MinecraftCompat.getMinecraft().fontRendererObj, x + width / 6, y + height - 7 - 6, false, 40, 0xFF303030);

        MinecraftCompat.getMinecraft().getTextureManager().bindTexture(RESET);
        GlStateManagerCompat.color(1, 1, 1, 1);
        RenderUtils.drawTexturedRect(x + width / 6 - 24 + 48 + 3, y + height - 7 - 14 + 3, 10, 11, GL11.GL_NEAREST);
    }

    @Override
    public boolean mouseInput(int x, int y, int width, int mouseX, int mouseY) {
        if (MouseCompat.getEventButtonState() && MouseCompat.getEventButton() != -1 && editingKeycode) {
            editingKeycode = false;
            keyCode = MouseCompat.getEventButton() - 100;
            option.set(keyCode);
            return true;
        }

        if (MouseCompat.getEventButtonState() && MouseCompat.getEventButton() == 0) {
            int height = getHeight();
            if (mouseX > x + width / 6 - 24 && mouseX < x + width / 6 + 24 && mouseY > y + height - 7 - 14 && mouseY < y + height - 7 + 2) {
                editingKeycode = true;
                return true;
            }
            if (mouseX > x + width / 6 - 24 + 48 + 3 && mouseX < x + width / 6 - 24 + 48 + 13 && mouseY > y + height - 7 - 14 + 3 && mouseY < y + height - 7 - 14 + 3 + 11) {
                keyCode = defaultKeyCode;
                option.set(keyCode);
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean keyboardInput() {
        if (editingKeycode) {
            editingKeycode = false;
            if (KeyboardCompat.getEventKey() == Keyboard.KEY_ESCAPE) {
                keyCode = 0;
            } else {
                keyCode = KeyboardCompat.getEventKey() == 0 ? KeyboardCompat.getEventCharacter() + 256 : KeyboardCompat.getEventKey();
            }
            option.set(keyCode);
            return true;
        }
        return false;
    }
}
