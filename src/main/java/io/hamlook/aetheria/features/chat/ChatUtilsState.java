package io.hamlook.aetheria.features.chat;

import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.IChatComponent;

/**
 * Shared mutable state threaded through the Mixin pipeline so that
 * MixinGuiNewChat, MixinChatLine, and MixinGuiChat can communicate
 * without coupling to each other directly.
 */
public final class ChatUtilsState {

    private ChatUtilsState() {}

    /** The full IChatComponent currently being processed by setChatLine. */
    public static IChatComponent currentFullMessage = null;

    /**
     * The last full message we assigned a head to, used to suppress
     * re-detection when the same original message creates multiple
     * wrapped ChatLines (e.g. word-wrap splits).
     */
    public static IChatComponent lastFullMessage = null;

    /** The last player whose head we drew — used for hideHeadOnConsecutive. */
    public static NetworkPlayerInfo lastDetectedPlayer = null;
}
