package io.hamlook.aetheria.features.chat;

import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.IChatComponent;
//#if MC < 1.16
//#else
//$$ import net.minecraft.client.gui.hud.ChatHudLine;
//#endif

/**
 * Interface injected onto {@code net.minecraft.client.gui.ChatLine} by
 * {@link io.hamlook.aetheria.mixins.chat.MixinChatLine}.
 */
public interface ChatLineHook {

    //#if MC < 1.16
    boolean athr$hasDetected();

    NetworkPlayerInfo athr$getPlayerInfo();

    long athr$getUniqueId();

    IChatComponent athr$getFullMessage();
    //#else
    //$$ PlayerListEntry athr$getPlayerInfo();
    //$$
    //$$ void athr$setPlayerInfo(PlayerListEntry info);
    //$$
    //$$ boolean athr$isHeadHidden();
    //$$
    //$$ void athr$setHeadHidden(boolean hidden);
    //$$
    //$$ ChatHudLine athr$getParentLine();
    //$$
    //$$ void athr$setParentLine(ChatHudLine parent);
    //#endif
}
