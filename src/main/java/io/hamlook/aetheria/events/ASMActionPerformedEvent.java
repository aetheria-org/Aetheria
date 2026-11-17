package io.hamlook.aetheria.events;

import io.hamlook.aetheria.api.event.AetheriaEvent;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

public class ASMActionPerformedEvent extends AetheriaEvent implements AetheriaEvent.Cancellable {

    public final GuiScreen gui;
    public final GuiButton button;

    public ASMActionPerformedEvent(GuiScreen gui, GuiButton button) {
        this.gui = gui;
        this.button = button;
    }
}
