package io.hamlook.aetheria.events;

import io.hamlook.aetheria.api.event.AetheriaEvent;
import net.minecraft.client.gui.GuiScreen;

public class ASMKeyEvent extends AetheriaEvent implements AetheriaEvent.Cancellable {

    public final GuiScreen gui;

    public ASMKeyEvent(GuiScreen gui) {
        this.gui = gui;
    }
}
