package io.hamlook.aetheria.events;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraftforge.fml.common.eventhandler.Event;

public class GuiContainerRenderBeforeTooltipEvent extends Event {

    public final GuiContainer gui;
    public final int mouseX;
    public final int mouseY;

    public GuiContainerRenderBeforeTooltipEvent(GuiContainer gui, int mouseX, int mouseY) {
        this.gui = gui;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
    }
}
