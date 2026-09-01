package io.hamlook.aetheria.events;

import io.hamlook.aetheria.api.event.AetheriaEvent;
import net.minecraft.client.gui.GuiScreen;

public class ASMMouseEvent extends AetheriaEvent implements AetheriaEvent.Cancellable {

    public final GuiScreen gui;

    public ASMMouseEvent(GuiScreen gui) {
        this.gui = gui;
    }
}
