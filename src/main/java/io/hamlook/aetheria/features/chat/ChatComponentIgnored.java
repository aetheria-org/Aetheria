package io.hamlook.aetheria.features.chat;

import io.hamlook.aetheria.utils.compat.TextCompat;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.IChatComponent;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * A wrapper component that marks timestamp-prefixed chat lines so
 * {@link ChatCompactHandler} can skip them during hash computation.
 * <p>
 * On 1.21+, {@code LiteralText} was removed so we can no longer extend
 * {@code ChatComponentText}. Instead this class implements {@link IChatComponent}
 * by delegation and tracks all instances in a static {@link WeakHashMap}-backed set.
 */
public class ChatComponentIgnored {

    private static final Set<IChatComponent> TRACKED =
            Collections.newSetFromMap(new WeakHashMap<>());

    private final IChatComponent wrapped;

    public ChatComponentIgnored(String msg) {
        this.wrapped = TextCompat.createText(msg);
        TRACKED.add(this.wrapped);
    }

    public static boolean isIgnored(IChatComponent component) {
        return TRACKED.contains(component);
    }

    public IChatComponent getWrapped() {
        return wrapped;
    }
}
