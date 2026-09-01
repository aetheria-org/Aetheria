package io.hamlook.aetheria.features.qol;

import io.hamlook.aetheria.init.RegisterEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import io.hamlook.aetheria.api.event.HandleEvent;
import org.lwjgl.input.Mouse;
import io.hamlook.aetheria.events.ASMGuiOpenEvent;

@RegisterEvents
public class CursorResetHandler {

    private static final Minecraft mc = Minecraft.getMinecraft();

    // Cached mouse coordinates
    public static int cachedX;
    public static int cachedY;


    public static void cacheMouse() {
        cachedX = Mouse.getX();
        cachedY = Mouse.getY();
    }


    @HandleEvent
    public void onGuiOpen(ASMGuiOpenEvent event) {
        GuiScreen oldGui = mc.currentScreen;
        if (oldGui != null) {
            cacheMouse();
        }
    }
}