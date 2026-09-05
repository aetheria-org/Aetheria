package io.hamlook.aetheria.utils.overlay;

import io.hamlook.aetheria.features.storage.StorageManager;
import io.hamlook.aetheria.utils.compat.KeyboardCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;

public class OverlayUtils {

    private static final Minecraft mc = MinecraftCompat.getMinecraft();
    private static Boolean tabHeldCache;
    private static int tabHeldTick = -1;

    public static boolean isChatOpen()       { return MinecraftCompat.getCurrentScreen() instanceof GuiChat; }
    public static boolean isDebugActive()    { return mc.gameSettings.showDebugInfo; }
    public static boolean isTabHeld() {
        int tick = MinecraftCompat.getLocalPlayer() != null ? MinecraftCompat.getLocalPlayer().ticksExisted : -1;
        if (tick != tabHeldTick) {
            tabHeldCache = MinecraftCompat.getCurrentScreen() == null && KeyboardCompat.isKeyDown(mc.gameSettings.keyBindPlayerList.getKeyCode());
            tabHeldTick = tick;
        }
        return tabHeldCache;
    }
    public static boolean isStorageActive()  { return StorageManager.isOverlayActive(); }

    public static boolean shouldHide() {
        return isChatOpen() || isDebugActive() || isTabHeld() || isStorageActive();
    }
}