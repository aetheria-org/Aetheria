package io.hamlook.aetheria.features.chat;

import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.IChatComponent;

import io.hamlook.aetheria.utils.chat.ChatUtils;

//#if MC < 1.21
import java.util.Map;
//#endif

/**
 * Shared mutable state threaded through the Mixin pipeline so that
 * MixinGuiNewChat, MixinChatLine, and MixinGuiChat can communicate
 * without coupling to each other directly.
 */
public final class ChatUtilsState {

    public static IChatComponent currentFullMessage = null;
    public static IChatComponent lastFullMessage = null;
    public static NetworkPlayerInfo lastDetectedPlayer = null;

    public static ChatUtils.MessageType currentLineMessage = null;
    public static ChatUtils.MessageType pendingLineMessage = null;
//#if MC < 1.21
    public static Map<IChatComponent, ChatUtils.MessageType> refreshTags = null;
//#endif

    private ChatUtilsState() {
    }
}
