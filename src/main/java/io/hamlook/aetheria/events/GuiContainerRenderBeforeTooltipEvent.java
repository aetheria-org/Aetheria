package io.hamlook.aetheria.events;

import io.hamlook.aetheria.api.event.RenderingAetheriaEvent;
import io.hamlook.aetheria.utils.compat.DrawContext;
import net.minecraft.client.gui.inventory.GuiContainer;

public class GuiContainerRenderBeforeTooltipEvent extends RenderingAetheriaEvent {

    public final GuiContainer gui;
    public final int mouseX;
    public final int mouseY;

    public GuiContainerRenderBeforeTooltipEvent(DrawContext context, GuiContainer gui, int mouseX, int mouseY) {
        super(context);
        this.gui = gui;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
    }
}
