package io.hamlook.aetheria.mixins.chat;

import io.hamlook.aetheria.features.chat.ChatLineHook;
import io.hamlook.aetheria.features.chat.ChatUtilsState;
import io.hamlook.aetheria.utils.hooks.ChatLineInitHook;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.IChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatLine.class)
public class MixinChatLine implements ChatLineHook {

    @Unique private boolean athr$detected = false;
    @Unique private NetworkPlayerInfo athr$playerInfo = null;
    @Unique private long athr$uniqueId = 0L;
    @Unique private IChatComponent athr$fullMsg = null;

    @Unique private static long athr$lastUniqueId = 0L;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(int updateCounter, IChatComponent lineString, int chatLineID, CallbackInfo ci) {
        athr$uniqueId = ++athr$lastUniqueId;
        athr$fullMsg  = ChatUtilsState.currentFullMessage;

        ChatLineInitHook.PlayerDetectionResult result = ChatLineInitHook.detectPlayer(
                lineString, athr$playerInfo, ChatUtilsState.lastFullMessage);

        ChatUtilsState.lastFullMessage = result.getCurrentFullMessage();

        if (result.getDetected()) {
            athr$detected = true;
            athr$playerInfo = result.getPlayerInfo();
        }
    }

    @Override public boolean athr$hasDetected()            { return athr$detected; }
    @Override public NetworkPlayerInfo athr$getPlayerInfo() { return athr$playerInfo; }
    @Override public long athr$getUniqueId()               { return athr$uniqueId; }
    @Override public IChatComponent athr$getFullMessage()   { return athr$fullMsg; }
}
