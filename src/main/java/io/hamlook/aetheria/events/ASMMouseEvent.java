package io.hamlook.aetheria.events;

import io.hamlook.aetheria.api.event.AetheriaEvent;
import io.hamlook.aetheria.utils.KeybindHelper;
import io.hamlook.aetheria.utils.compat.MouseCompat;
import net.minecraft.client.gui.GuiScreen;

public class ASMMouseEvent extends AetheriaEvent implements AetheriaEvent.Cancellable {

    public final GuiScreen gui;
    public final int button;
    public final boolean buttonState;
    public final int dWheel;
    public final int mouseX;
    public final int mouseY;

    public ASMMouseEvent(GuiScreen gui) {
        this(gui, MouseCompat.getEventButton(), MouseCompat.getEventButtonState(),
                MouseCompat.getEventDWheel(), KeybindHelper.getScaledEventX(gui.width),
                KeybindHelper.getScaledEventY(gui.height));
    }

    public ASMMouseEvent(GuiScreen gui, int button, boolean buttonState, int dWheel, int mouseX, int mouseY) {
        this.gui = gui;
        this.button = button;
        this.buttonState = buttonState;
        this.dWheel = dWheel;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
    }
}
