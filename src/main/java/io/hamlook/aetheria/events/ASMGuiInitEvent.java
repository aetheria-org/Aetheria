package io.hamlook.aetheria.events;

import io.hamlook.aetheria.api.event.AetheriaEvent;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

import java.util.List;

public class ASMGuiInitEvent extends AetheriaEvent {

    public final GuiScreen gui;
    public final List<GuiButton> buttonList;

    public ASMGuiInitEvent(GuiScreen gui, List<GuiButton> buttonList) {
        this.gui = gui;
        this.buttonList = buttonList;
    }
}
