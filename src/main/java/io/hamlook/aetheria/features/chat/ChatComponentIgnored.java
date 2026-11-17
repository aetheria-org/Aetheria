package io.hamlook.aetheria.features.chat;

import io.hamlook.aetheria.utils.compat.TextCompat;
import net.minecraft.util.IChatComponent;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Marks timestamp-prefixed chat lines so {@link ChatCompactHandler} can skip them
 * during hash computation. Tracked by identity: 1.8.9 {@code ChatStyle.hashCode()}
 * throws on unset fields, and a 1.21 {@code Style} would match equal components.
 */
public class ChatComponentIgnored {

    private static final List<WeakReference<IChatComponent>> TRACKED = new ArrayList<>();

    private final IChatComponent wrapped;

    public ChatComponentIgnored(String msg) {
        this.wrapped = TextCompat.fromFormatted(msg);
        synchronized (TRACKED) {
            TRACKED.add(new WeakReference<>(this.wrapped));
        }
    }

    public static boolean isIgnored(IChatComponent component) {
        if (component == null) return false;
        synchronized (TRACKED) {
            for (Iterator<WeakReference<IChatComponent>> it = TRACKED.iterator(); it.hasNext(); ) {
                IChatComponent tracked = it.next().get();
                if (tracked == null) {
                    it.remove();
                } else if (tracked == component) {
                    return true;
                }
            }
        }
        return false;
    }

    public IChatComponent getWrapped() {
        return wrapped;
    }
}