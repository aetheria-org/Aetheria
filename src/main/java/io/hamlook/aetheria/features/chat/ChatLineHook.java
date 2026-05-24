package io.hamlook.aetheria.features.chat;

import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.IChatComponent;

/**
 * Interface injected onto {@code net.minecraft.client.gui.ChatLine} by
 * {@link io.hamlook.aetheria.mixins.chat.MixinChatLine}.
 *
 * Provides access to per-line data that the vanilla ChatLine class does not expose:
 * the detected player, unique ordering ID, and full original message component.
 */
public interface ChatLineHook {

    /** Returns {@code true} if a player was detected in this chat line's text. */
    boolean chatutils$hasDetected();

    /**
     * Returns the {@link NetworkPlayerInfo} for the detected player, or {@code null} if:
     * <ul>
     *   <li>No player was detected, or</li>
     *   <li>This is a consecutive message from the same player and
     *       {@code hideHeadOnConsecutive} is enabled.</li>
     * </ul>
     */
    NetworkPlayerInfo chatutils$getPlayerInfo();

    /** Monotonically-increasing ID assigned at construction time; used for ordering. */
    long chatutils$getUniqueId();

    /** The full {@link IChatComponent} that spawned this wrapped line (may be multi-line). */
    IChatComponent chatutils$getFullMessage();
}
