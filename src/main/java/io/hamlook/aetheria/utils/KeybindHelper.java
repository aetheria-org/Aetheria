package io.hamlook.aetheria.utils;

import io.hamlook.aetheria.utils.compat.KeyboardCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.MouseCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public final class KeybindHelper {

    private static final Minecraft MC = MinecraftCompat.getMinecraft();
    private static final Map<Integer, Boolean> PREV_DOWN = new ConcurrentHashMap<>();
    private static final Map<Integer, Boolean> CURR_DOWN = new ConcurrentHashMap<>();
    private static int cacheTick = -1;
    private static EntityPlayer cacheAnchor;

    private KeybindHelper() {
    }

    // Key constants
    public static final int KEY_ESCAPE = 1;
    public static final int KEY_RETURN = 28;
    public static final int KEY_NUMPAD_ENTER = 156;

    // Version-split key predicates (LWJGL2 codes vs GLFW codes)
    public static boolean isEscapeKey(int keyCode) {
        //#if MC < 1.16
        return keyCode == KEY_ESCAPE;
        //#else
        //$$ return keyCode == 256;
        //#endif
    }

    public static boolean isEnterKey(int keyCode) {
        //#if MC < 1.16
        return keyCode == KEY_RETURN || keyCode == KEY_NUMPAD_ENTER;
        //#else
        //$$ return keyCode == 257 || keyCode == 335;
        //#endif
    }

    public static boolean isBackspaceKey(int keyCode) {
        //#if MC < 1.16
        return keyCode == 14;
        //#else
        //$$ return keyCode == 259;
        //#endif
    }

    public static String getKeyName(int keyCode) {
        if (keyCode == 0 || keyCode == -1) return "NONE";
        if (keyCode < 0) return "Button " + (keyCode + 101);
        try {
            String name = KeyboardCompat.getKeyName(keyCode);
            if (name == null) return "???";
            if (name.equalsIgnoreCase("LMENU")) return "LALT";
            if (name.equalsIgnoreCase("RMENU")) return "RALT";
            return name;
        } catch (Exception e) {
            return "???";
        }
    }

    public static boolean isKeyValid(int keyCode) {
        return !isKeyInvalid(keyCode);
    }

    private static boolean isKeyInvalid(int keyCode) {
        return keyCode == 0 || keyCode == -1;
    }

    public static boolean isKeyDown(int keyCode) {
        if (isKeyInvalid(keyCode)) return false;
        return keyCode < 0 ? MouseCompat.isButtonDown(keyCode + 100) : KeyboardCompat.isKeyDown(keyCode);
    }

    /**
     * Event-context read: returns {@code true} when the current keyboard event
     * (as dispatched by the surrounding {@code InputEvent.KeyInputEvent} handler)
     * is a press of {@code keyCode}. Only meaningful inside such a callback
     * polled from elsewhere use {@link #isKeyTapped(int)} instead.
     */
    public static boolean isKeyPressed(int keyCode) {
        if (isKeyInvalid(keyCode)) return false;
        return keyCode < 0 ? MouseCompat.getEventButtonState() && MouseCompat.getEventButton() == keyCode + 100 : KeyboardCompat.getEventKeyState() && KeyboardCompat.getEventKey() == keyCode;
    }

    public static boolean isKeyTapped(int keyCode) {
        if (isKeyInvalid(keyCode)) return false;
        rollTickCache();
        boolean cur = CURR_DOWN.computeIfAbsent(keyCode, KeybindHelper::safeIsKeyDown);
        Boolean prev = PREV_DOWN.get(keyCode);
        return cur && !(prev != null && prev);
    }

    /** Re-seeds the tap state for a key with its current held-state, so the next {@link #isKeyTapped(int)} requires a fresh release+press to fire. */
    public static void resetKeyTap(int keyCode) {
        if (isKeyInvalid(keyCode)) return;
        boolean down = safeIsKeyDown(keyCode);
        PREV_DOWN.put(keyCode, down);
        CURR_DOWN.put(keyCode, down);
    }

    private static void rollTickCache() {
        EntityPlayer player = MinecraftCompat.getLocalPlayer();
        int tick = player == null ? -1 : player.ticksExisted;
        if (player != cacheAnchor || tick != cacheTick) {
            PREV_DOWN.clear();
            PREV_DOWN.putAll(CURR_DOWN);
            CURR_DOWN.clear();
            cacheTick = tick;
            cacheAnchor = player;
        }
    }

    private static boolean safeIsKeyDown(int keyCode) {
        try {
            return isKeyDown(keyCode);
        } catch (Exception e) {
            return false;
        }
    }

    // Keyboard event accessors
    public static char getEventCharacter() {
        return KeyboardCompat.getEventCharacter();
    }

    public static boolean getEventKeyState() {
        return KeyboardCompat.getEventKeyState();
    }

    public static int getEventKeyCode() {
        return KeyboardCompat.getEventKey();
    }

    public static void enableRepeatEvents(boolean repeat) {
        KeyboardCompat.enableRepeatEvents(repeat);
    }

    // Mouse event accessors
    public static boolean getEventButtonState() {
        return MouseCompat.getEventButtonState();
    }

    public static int getEventButton() {
        return MouseCompat.getEventButton();
    }

    public static int getEventDWheel() {
        return MouseCompat.getEventDWheel();
    }

    // Coordinate helpers, poll-based (for draw/render)
    public static int[] getMouseCoords(int guiWidth, int guiHeight) {
        //#if MC < 1.21
        int mouseX = MouseCompat.getX() * guiWidth / MinecraftCompat.getDisplayWidth();
        int mouseY = guiHeight - MouseCompat.getY() * guiHeight / MinecraftCompat.getDisplayHeight() - 1;
        //#else
        //$$ int mouseX = (int) (MouseCompat.getX() * guiWidth / (float) MinecraftCompat.getDisplayWidth());
        //$$ int mouseY = (int) (MouseCompat.getY() * guiHeight / (float) MinecraftCompat.getDisplayHeight());
        //#endif
        return new int[]{mouseX, mouseY};
    }

    public static float[] getMouseCoordsFloat(double guiWidth, double guiHeight) {
        //#if MC < 1.21
        float mouseX = (float) (MouseCompat.getX() * guiWidth / MinecraftCompat.getDisplayWidth());
        float mouseY = (float) (guiHeight - MouseCompat.getY() * guiHeight / MinecraftCompat.getDisplayHeight() - 1);
        //#else
        //$$ float mouseX = (float) (MouseCompat.getX() * guiWidth / MinecraftCompat.getDisplayWidth());
        //$$ float mouseY = (float) (MouseCompat.getY() * guiHeight / MinecraftCompat.getDisplayHeight());
        //#endif
        return new float[]{mouseX, mouseY};
    }

    // Coordinate helpers, event-based (for mouse input events)
    public static int getScaledEventX(int guiWidth) {
        //#if MC < 1.21
        return MouseCompat.getEventX() * guiWidth / MinecraftCompat.getDisplayWidth();
        //#else
        //$$ return (int) (MouseCompat.getEventX() * guiWidth / (float) MinecraftCompat.getDisplayWidth());
        //#endif
    }

    public static int getScaledEventY(int guiHeight) {
        //#if MC < 1.21
        return guiHeight - MouseCompat.getEventY() * guiHeight / MinecraftCompat.getDisplayHeight() - 1;
        //#else
        //$$ return (int) (MouseCompat.getEventY() * guiHeight / (float) MinecraftCompat.getDisplayHeight());
        //#endif
    }
}