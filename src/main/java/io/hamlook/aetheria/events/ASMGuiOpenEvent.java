package io.hamlook.aetheria.events;

import io.hamlook.aetheria.api.event.AetheriaEvent;
import net.minecraft.client.gui.GuiScreen;

public class ASMGuiOpenEvent extends AetheriaEvent implements AetheriaEvent.Cancellable {

    public GuiScreen gui;

    public ASMGuiOpenEvent(GuiScreen gui) {
        this.gui = gui;
    }
}
