package io.hamlook.aetheria.mixins.chat;

import io.hamlook.aetheria.features.chat.ChatLineHook;
import io.hamlook.aetheria.features.chat.ChatUtilsState;
import io.hamlook.aetheria.utils.hooks.ChatLineInitHook;
//#if MC < 1.16
import net.minecraft.client.gui.ChatLine;
//#else
//$$ import net.minecraft.client.gui.hud.ChatHudLine;
//$$ import net.minecraft.client.gui.hud.ChatHudLine.Visible;
//#endif
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.IChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC < 1.16
@Mixin(ChatLine.class)
//#else
//$$ @Mixin({ChatHudLine.class, Visible.class})
//#endif
public class MixinChatLine implements ChatLineHook {

    //#if MC < 1.16
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
    //#else
    //$$ @Unique private PlayerListEntry athr$playerInfo = null;
    //$$ @Unique private boolean athr$headHidden = false;
    //$$ @Unique private ChatHudLine athr$parentLine = null;
    //$$ @Unique private boolean athr$headRow = false;
    //$$
    //$$ @Override public PlayerListEntry athr$getPlayerInfo() { return athr$playerInfo; }
    //$$ @Override public void athr$setPlayerInfo(PlayerListEntry info) { athr$playerInfo = info; }
    //$$ @Override public boolean athr$isHeadHidden() { return athr$headHidden; }
    //$$ @Override public void athr$setHeadHidden(boolean hidden) { athr$headHidden = hidden; }
    //$$ @Override public ChatHudLine athr$getParentLine() { return athr$parentLine; }
    //$$ @Override public void athr$setParentLine(ChatHudLine parent) { athr$parentLine = parent; }
    //$$ @Override public boolean athr$isHeadRow() { return athr$headRow; }
    //$$ @Override public void athr$setHeadRow(boolean headRow) { athr$headRow = headRow; }
    //#endif
}
