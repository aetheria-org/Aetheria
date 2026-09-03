package io.hamlook.aetheria.utils.debug;

import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Fail-safe GL state diagnostics for render debugging. Captures a compact
 * snapshot of the attributes that commonly break GUI rendering (lighting,
 * depth test, blend mode and function, alpha test, current color, color write
 * mask) and logs it at a throttled rate.
 *
 * Never throws: any internal failure is logged once and the probe disables
 * itself permanently, so it can safely be called from render paths.
 */
public final class GLDebugProbe {

    // LWJGL2's buffered glGet* calls require at least 16 remaining elements,
    // regardless of how many values the queried pname actually returns.
    private static final FloatBuffer COLOR = BufferUtils.createFloatBuffer(16);
    private static final ByteBuffer MASK = BufferUtils.createByteBuffer(16);

    private static final Map<String, Long> LAST_LOG = new HashMap<>();
    private static boolean disabled = false;

    private GLDebugProbe() {
    }

    /** Compact snapshot of commonly-polluted GL state. */
    public static String state() {
        if (disabled) return "GLDebugProbe disabled (earlier failure)";
        try {
            COLOR.rewind();
            GL11.glGetFloat(GL11.GL_CURRENT_COLOR, COLOR);
            MASK.rewind();
            GL11.glGetBoolean(GL11.GL_COLOR_WRITEMASK, MASK);
            GuiScreen screen = MinecraftCompat.getMinecraft().currentScreen;
            String screenName = screen == null ? "null" : screen.getClass().getSimpleName();
            return "[" + screenName + "]"
                    + " light=" + GL11.glIsEnabled(GL11.GL_LIGHTING)
                    + " depth=" + GL11.glIsEnabled(GL11.GL_DEPTH_TEST)
                    + " blend=" + GL11.glIsEnabled(GL11.GL_BLEND)
                    + " alphaT=" + GL11.glIsEnabled(GL11.GL_ALPHA_TEST)
                    + " src=" + GL11.glGetInteger(GL11.GL_BLEND_SRC)
                    + " dst=" + GL11.glGetInteger(GL11.GL_BLEND_DST)
                    + String.format(" col=%.2f/%.2f/%.2f/%.2f",
                            COLOR.get(0), COLOR.get(1), COLOR.get(2), COLOR.get(3))
                    + " mask=" + MASK.get(0) + MASK.get(1) + MASK.get(2) + MASK.get(3);
        } catch (Exception e) {
            disable(e);
            return "GLDebugProbe failed: " + e;
        }
    }

    /**
     * True at most once per {@code intervalMs} for the given feature key; use
     * as the gate for logging one or more probe lines within the same cycle.
     */
    public static boolean throttle(String key, long intervalMs) {
        if (disabled) return false;
        long now = System.currentTimeMillis();
        Long last = LAST_LOG.get(key);
        if (last != null && now - last < intervalMs) return false;
        LAST_LOG.put(key, now);
        return true;
    }

    /** Logs {@code message} as a warning, throttled per key; fail-safe. */
    public static void warnThrottled(String key, long intervalMs, String message) {
        if (disabled) return;
        try {
            if (throttle(key, intervalMs)) {
                Aetheria.logger.warning(message);
            }
        } catch (Exception e) {
            disable(e);
        }
    }

    private static void disable(Exception e) {
        disabled = true;
        Aetheria.logger.warning("[GLDebugProbe] disabled after failure: " + e);
    }
}