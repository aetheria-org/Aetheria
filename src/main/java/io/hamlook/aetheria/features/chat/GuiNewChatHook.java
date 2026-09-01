package io.hamlook.aetheria.features.chat;

import net.minecraft.client.gui.ChatLine;

/**
 * Interface injected onto {@code net.minecraft.client.gui.GuiNewChat} by
 * {@link io.hamlook.aetheria.mixins.chat.MixinGuiNewChat}.
 */
public interface GuiNewChatHook {

    ChatLine athr$getHoveredChatLine(int rawMouseX, int rawMouseY);

    ChatLine athr$getCurrentHoveredLine();
}
