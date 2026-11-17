package io.hamlook.aetheria.utils.overlay;

import io.hamlook.aetheria.features.storage.StorageManager;
import io.hamlook.aetheria.utils.compat.KeybindCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import net.minecraft.client.gui.GuiChat;

public class OverlayUtils {

    private static Boolean tabHeldCache;
    private static int tabHeldTick = -1;

    public static boolean isChatOpen()       { return MinecraftCompat.getCurrentScreen() instanceof GuiChat; }
    public static boolean isDebugActive()    { return MinecraftCompat.getShowDebugHud(); }
    public static boolean isTabHeld() {
        int tick = MinecraftCompat.getLocalPlayer() != null ? MinecraftCompat.getLocalPlayer().ticksExisted : -1;
        if (tick != tabHeldTick) {
            tabHeldCache = MinecraftCompat.getCurrentScreen() == null && KeybindCompat.isPlayerListKeyDown();
            tabHeldTick = tick;
        }
        return tabHeldCache;
    }
    public static boolean isStorageActive()  { return StorageManager.isOverlayActive(); }

    public static boolean shouldHide() {
        return isChatOpen() || isDebugActive() || isTabHeld() || isStorageActive();
    }
}