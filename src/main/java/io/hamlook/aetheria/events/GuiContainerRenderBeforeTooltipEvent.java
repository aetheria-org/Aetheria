package io.hamlook.aetheria.events;

import io.hamlook.aetheria.api.event.AetheriaEvent;
import net.minecraft.client.gui.inventory.GuiContainer;

public class GuiContainerRenderBeforeTooltipEvent extends AetheriaEvent {

    public final GuiContainer gui;
    public final int mouseX;
    public final int mouseY;

    public GuiContainerRenderBeforeTooltipEvent(GuiContainer gui, int mouseX, int mouseY) {
        this.gui = gui;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
    }
}
