// SPDX-License-Identifier: LGPL-3.0-only
// Derived from MoulConfig (https://github.com/NotEnoughUpdates/MoulConfig)

package io.hamlook.aetheria.core.moulconfig.editors;

import io.hamlook.aetheria.core.moulconfig.gui.GuiElementColour;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigProcessor;
import io.hamlook.aetheria.utils.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Mouse;

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
        GlStateManager.color(r / 255f, g / 255f, b / 255f, 1);
        Minecraft.getMinecraft().getTextureManager().bindTexture(button_white);
        RenderUtils.drawTexturedRect(x + width / 6 - 24, y + height - 7 - 14, 48, 16);
    }

    @Override
    public void renderOverlay(int x, int y, int width) {
        if (colourElement != null) {
            colourElement.render();
        }
    }

    @Override
    public boolean mouseInputOverlay(int x, int y, int width, int mouseX, int mouseY) {
        return colourElement != null && colourElement.mouseInput(mouseX, mouseY);
    }

    @Override
    public boolean mouseInput(int x, int y, int width, int mouseX, int mouseY) {
        int height = getHeight();

        if (Mouse.getEventButtonState() && Mouse.getEventButton() == 0 && mouseX > x + width / 6 - 24 && mouseX < x + width / 6 + 24 && mouseY > y + height - 7 - 14 && mouseY < y + height - 7 + 2) {
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
}
