package io.hamlook.aetheria.events;

import io.hamlook.aetheria.api.event.AetheriaEvent;
import net.minecraft.client.gui.inventory.GuiEditSign;

public class SignSubmitEvent extends AetheriaEvent {
    public final GuiEditSign sign;
    public final String[] lines;

    public SignSubmitEvent(GuiEditSign sign, String[] lines) {
        this.sign = sign;
        this.lines = lines;
    }
}
