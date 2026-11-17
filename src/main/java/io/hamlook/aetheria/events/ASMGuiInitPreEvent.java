package io.hamlook.aetheria.events;

import io.hamlook.aetheria.api.event.AetheriaEvent;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

import java.util.List;

public class ASMGuiInitPreEvent extends AetheriaEvent {

    public final GuiScreen gui;
    public final List<GuiButton> buttonList;

    public ASMGuiInitPreEvent(GuiScreen gui, List<GuiButton> buttonList) {
        this.gui = gui;
        this.buttonList = buttonList;
    }
}
