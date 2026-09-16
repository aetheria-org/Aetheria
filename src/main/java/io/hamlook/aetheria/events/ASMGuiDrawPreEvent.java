package io.hamlook.aetheria.events;

import io.hamlook.aetheria.api.event.AetheriaEvent;
import io.hamlook.aetheria.api.event.RenderingAetheriaEvent;
import io.hamlook.aetheria.utils.compat.DrawContext;
import net.minecraft.client.gui.GuiScreen;

public class ASMGuiDrawPreEvent extends RenderingAetheriaEvent implements AetheriaEvent.Cancellable {

    public final GuiScreen gui;
    public final int mouseX;
    public final int mouseY;

    public ASMGuiDrawPreEvent(DrawContext context, GuiScreen gui, int mouseX, int mouseY) {
        super(context);
        this.gui = gui;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
    }
}
