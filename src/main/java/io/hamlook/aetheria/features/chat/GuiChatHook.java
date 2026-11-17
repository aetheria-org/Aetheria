package io.hamlook.aetheria.features.chat;

/**
 * Interface injected onto {@code net.minecraft.client.gui.GuiChat} by
 * {@link io.hamlook.aetheria.mixins.chat.MixinGuiChat}.
 */
public interface GuiChatHook {

    boolean athr$isTypingMode();
}
