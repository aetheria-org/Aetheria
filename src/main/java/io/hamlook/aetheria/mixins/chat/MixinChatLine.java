package io.hamlook.aetheria.mixins.chat;

import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.chat.ChatLineHook;
import io.hamlook.aetheria.features.chat.ChatUtilsState;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.TextCompat;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.IChatComponent;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Pattern;

@Mixin(ChatLine.class)
public class MixinChatLine implements ChatLineHook {

    @Unique private boolean athr$detected = false;
    @Unique private NetworkPlayerInfo athr$playerInfo = null;
    @Unique private long athr$uniqueId = 0L;
    @Unique private IChatComponent athr$fullMsg = null;

    @Unique private static long athr$lastUniqueId = 0L;
    @Unique private static final Pattern SPLIT_PATTERN = Pattern.compile("(§.)|\\W");

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(int updateCounter, IChatComponent lineString, int chatLineID, CallbackInfo ci) {
        athr$uniqueId = ++athr$lastUniqueId;
        athr$fullMsg  = ChatUtilsState.currentFullMessage;

        if (athr$fullMsg != null && athr$fullMsg == ChatUtilsState.lastFullMessage) return;
        ChatUtilsState.lastFullMessage = athr$fullMsg;

        if (ATHRConfig.feature == null || !ATHRConfig.feature.chat.chatHeads) return;

        NetHandlerPlayClient netHandler = MinecraftCompat.getMinecraft().getNetHandler();
        if (netHandler == null) return;

        String text = StringUtils.substringAfter(TextCompat.getFormattedText((lineString)), "]");
        String beforeColon = StringUtils.substringBefore(text, ":");
        Map<String, NetworkPlayerInfo> nicknameCache = new HashMap<>();

        try {
            for (String word : SPLIT_PATTERN.split(beforeColon)) {
                if (word.isEmpty()) continue;

                NetworkPlayerInfo info = netHandler.getPlayerInfo(word);
                if (info == null) {
                    info = athr$resolveNickname(word, netHandler, nicknameCache);
                }
                if (info == null) {
                    info = athr$resolveUsername(word, netHandler);
                }

                if (info != null) {
                    athr$detected = true;

                    boolean sameAsLast = ChatUtilsState.lastDetectedPlayer != null
                            && info.getGameProfile() == ChatUtilsState.lastDetectedPlayer.getGameProfile()
                            && ATHRConfig.feature.chat.hideHeadOnConsecutive;

                    athr$playerInfo = sameAsLast ? null : info;
                    ChatUtilsState.lastDetectedPlayer = info;
                    return;
                }
            }
        } catch (Exception e) {
            Aetheria.logger.log(Level.WARNING, "Chat head detection failed", e);
        }
    }

    @Unique
    @Nullable
    private static NetworkPlayerInfo athr$resolveNickname(
            String word,
            NetHandlerPlayClient connection,
            Map<String, NetworkPlayerInfo> cache) {

        if (cache.isEmpty()) {
            for (NetworkPlayerInfo p : connection.getPlayerInfoMap()) {
                IChatComponent displayName = p.getDisplayName();
                if (displayName == null) continue;
                String nickname = displayName.getUnformattedTextForChat();
                if (word.equals(nickname)) return p;
                cache.put(nickname, p);
            }
            return null;
        }
        return cache.get(word);
    }

    @Unique
    @Nullable
    private static NetworkPlayerInfo athr$resolveUsername(
            String word,
            NetHandlerPlayClient connection) {

        for (NetworkPlayerInfo p : connection.getPlayerInfoMap()) {
            if (p.getGameProfile() != null && word.equals(p.getGameProfile().getName())) {
                return p;
            }
        }
        return null;
    }

    @Override public boolean athr$hasDetected()         { return athr$detected; }
    @Override public NetworkPlayerInfo athr$getPlayerInfo() { return athr$playerInfo; }
    @Override public long athr$getUniqueId()            { return athr$uniqueId; }
    @Override public IChatComponent athr$getFullMessage()   { return athr$fullMsg; }
}
