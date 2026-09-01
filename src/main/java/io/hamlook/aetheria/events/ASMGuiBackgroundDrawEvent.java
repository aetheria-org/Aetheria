package io.hamlook.aetheria.events;

import io.hamlook.aetheria.api.event.AetheriaEvent;
import net.minecraft.client.gui.GuiScreen;

public class ASMGuiBackgroundDrawEvent extends AetheriaEvent {

    public final GuiScreen gui;

    public ASMGuiBackgroundDrawEvent(GuiScreen gui) {
        this.gui = gui;
    }
}
