package io.hamlook.aetheria.features.chat;

import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.IChatComponent;

/**
 * Shared mutable state threaded through the Mixin pipeline so that
 * MixinGuiNewChat, MixinChatLine, and MixinGuiChat can communicate
 * without coupling to each other directly.
 */
public final class ChatUtilsState {

    public static IChatComponent currentFullMessage = null;
    public static IChatComponent lastFullMessage = null;
    public static NetworkPlayerInfo lastDetectedPlayer = null;

    private ChatUtilsState() {
    }
}
