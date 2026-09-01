package io.hamlook.aetheria.events;

import io.hamlook.aetheria.api.event.AetheriaEvent;
import net.minecraft.client.gui.GuiScreen;

public class ASMGuiDrawPreEvent extends AetheriaEvent implements AetheriaEvent.Cancellable {

    public final GuiScreen gui;
    public final int mouseX;
    public final int mouseY;

    public ASMGuiDrawPreEvent(GuiScreen gui, int mouseX, int mouseY) {
        this.gui = gui;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
    }
}
