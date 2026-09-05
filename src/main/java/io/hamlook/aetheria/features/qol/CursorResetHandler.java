package io.hamlook.aetheria.features.qol;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.events.ASMGuiOpenEvent;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.MouseCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

@RegisterEvents
public class CursorResetHandler {

    private static final Minecraft mc = MinecraftCompat.getMinecraft();

    // Cached mouse coordinates
    public static int cachedX;
    public static int cachedY;


    public static void cacheMouse() {
        cachedX = MouseCompat.getX();
        cachedY = MouseCompat.getY();
    }


    @HandleEvent
    public void onGuiOpen(ASMGuiOpenEvent event) {
        GuiScreen oldGui = MinecraftCompat.getCurrentScreen();
        if (oldGui != null) {
            cacheMouse();
        }
    }
}