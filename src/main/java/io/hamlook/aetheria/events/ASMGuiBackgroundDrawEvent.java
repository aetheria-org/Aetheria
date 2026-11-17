package io.hamlook.aetheria.events;

import io.hamlook.aetheria.api.event.RenderingAetheriaEvent;
import io.hamlook.aetheria.utils.compat.DrawContext;
import net.minecraft.client.gui.GuiScreen;

public class ASMGuiBackgroundDrawEvent extends RenderingAetheriaEvent {

    public final GuiScreen gui;

    public ASMGuiBackgroundDrawEvent(DrawContext context, GuiScreen gui) {
        super(context);
        this.gui = gui;
    }
}
