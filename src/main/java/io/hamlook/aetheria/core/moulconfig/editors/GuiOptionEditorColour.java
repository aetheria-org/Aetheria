// SPDX-License-Identifier: LGPL-3.0-only
// Derived from MoulConfig (https://github.com/NotEnoughUpdates/MoulConfig)

package io.hamlook.aetheria.core.moulconfig.editors;

import io.hamlook.aetheria.core.moulconfig.gui.GuiElementColour;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigProcessor;
import io.hamlook.aetheria.utils.compat.GlStateManagerCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.TextureCompat;
import io.hamlook.aetheria.utils.compat.MouseCompat;
import io.hamlook.aetheria.utils.render.RenderUtils;

import java.util.function.Consumer;

import static io.hamlook.aetheria.Resources.button_white;

public class GuiOptionEditorColour extends GuiOptionEditor {

    private final Consumer<String> setter;
    private String chromaColour;
    private GuiElementColour colourElement = null;

    public GuiOptionEditorColour(ConfigProcessor.ProcessedOption option) {
        this(option, (String) option.get(), option::set);
    }

    protected GuiOptionEditorColour(ConfigProcessor.ProcessedOption option, String initialColour, Consumer<String> setter) {
        super(option);
        this.chromaColour = initialColour;
        this.setter = setter;
    }

    @Override
    public void render(int x, int y, int width) {
        super.render(x, y, width);
        int height = getHeight();

        int argb = ChromaColour.specialToChromaRGB(chromaColour);
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        GlStateManagerCompat.color(r / 255f, g / 255f, b / 255f, 1);
        TextureCompat.drawBoundTextured(button_white, x + width / 6 - 24, y + height - 7 - 14, 48, 16, 0, 0, 1, 1);
        GlStateManagerCompat.resetColor();
    }

    @Override
    public void renderOverlay(int x, int y, int width) {
        if (colourElement != null) {
            colourElement.render();
        }
    }

    @Override
    public boolean mouseInputOverlay(int x, int y, int width, int mouseX, int mouseY) {
        return colourElement != null && colourElement.mouseInput(mouseX, mouseY, MouseCompat.getEventButton(), MouseCompat.getEventButtonState());
    }

    public boolean mouseInputOverlay(int x, int y, int width, int mouseX, int mouseY, int button, boolean pressed) {
        return colourElement != null && colourElement.mouseInput(mouseX, mouseY, button, pressed);
    }

    @Override
    public boolean mouseInput(int x, int y, int width, int mouseX, int mouseY) {
        return mouseInput(x, y, width, mouseX, mouseY, MouseCompat.getEventButton(), MouseCompat.getEventButtonState());
    }

    @Override
    public boolean mouseInput(int x, int y, int width, int mouseX, int mouseY, int button, boolean pressed) {
        int height = getHeight();

        if (pressed && button == 0 && mouseX > x + width / 6 - 24 && mouseX < x + width / 6 + 24 && mouseY > y + height - 7 - 14 && mouseY < y + height - 7 + 2) {
            colourElement = new GuiElementColour(mouseX, mouseY, chromaColour, val -> {
                setter.accept(val);
                chromaColour = val;
            }, () -> colourElement = null);
        }

        return false;
    }

    @Override
    public boolean keyboardInput() {
        return colourElement != null && colourElement.keyboardInput();
    }

    @Override
    public boolean keyboardInput(char typedChar, int keyCode) {
        return colourElement != null && colourElement.keyboardInput(typedChar, keyCode);
    }
}
