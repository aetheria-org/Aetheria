package io.hamlook.aetheria.features.chat;

import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.IChatComponent;

/**
 * Interface injected onto {@code net.minecraft.client.gui.ChatLine} by
 * {@link io.hamlook.aetheria.mixins.chat.MixinChatLine}.
 */
public interface ChatLineHook {

    boolean athr$hasDetected();

    NetworkPlayerInfo athr$getPlayerInfo();

    long athr$getUniqueId();

    IChatComponent athr$getFullMessage();
}
