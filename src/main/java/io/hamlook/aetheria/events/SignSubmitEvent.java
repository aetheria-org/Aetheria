package io.hamlook.aetheria.events;

import io.hamlook.aetheria.api.event.AetheriaEvent;
//#if MC < 1.21
import net.minecraft.client.gui.inventory.GuiEditSign;
//#else
//$$ import net.minecraft.client.gui.screen.ingame.AbstractSignEditScreen;
//#endif

public class SignSubmitEvent extends AetheriaEvent {
    //#if MC < 1.21
    public final GuiEditSign sign;
    //#else
    //$$ public final AbstractSignEditScreen sign;
    //#endif
    public final String[] lines;
    //#if MC < 1.21
    public SignSubmitEvent(GuiEditSign sign, String[] lines) {
    //#else
    //$$ public SignSubmitEvent(AbstractSignEditScreen sign, String[] lines) {
    //#endif
        this.sign = sign;
        this.lines = lines;
    }
}
