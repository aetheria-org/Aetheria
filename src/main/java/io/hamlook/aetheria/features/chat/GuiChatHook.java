package io.hamlook.aetheria.features.chat;

/**
 * Interface injected onto {@code net.minecraft.client.gui.GuiChat} by
 * {@link io.hamlook.aetheria.mixins.chat.MixinGuiChat}.
 */
public interface GuiChatHook {

    /**
     * Returns {@code true} only when the chat input field is focused (the player
     * opened chat with T/Enter in typing mode).  Returns {@code false} in scroll /
     * view-only mode (e.g. opened with Z).
     */
    boolean chatutils$isTypingMode();
}
