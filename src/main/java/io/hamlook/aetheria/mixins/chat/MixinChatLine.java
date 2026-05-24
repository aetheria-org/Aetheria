package io.hamlook.aetheria.mixins.chat;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.chat.ChatLineHook;
import io.hamlook.aetheria.features.chat.ChatUtilsState;
import net.minecraft.client.Minecraft;
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
import java.util.regex.Pattern;

/**
 * Implements {@link ChatLineHook} on every {@link ChatLine} instance.
 *
 * During construction the mixin scans text before the first colon to detect a
 * player name, then stores the matching {@link NetworkPlayerInfo} for the chat-head
 * renderer in {@link MixinGuiNewChat}.
 */
@Mixin(ChatLine.class)
public class MixinChatLine implements ChatLineHook {

    @Unique private boolean chatutils$detected = false;
    @Unique private NetworkPlayerInfo chatutils$playerInfo = null;
    @Unique private long chatutils$uniqueId = 0L;
    @Unique private IChatComponent chatutils$fullMsg = null;

    @Unique private static long chatutils$lastUniqueId = 0L;
    @Unique private static final Pattern SPLIT_PATTERN = Pattern.compile("(§.)|\\W");

    // ── Constructor injection ─────────────────────────────────────────────────

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(int updateCounter, IChatComponent lineString, int chatLineID, CallbackInfo ci) {
        chatutils$uniqueId = ++chatutils$lastUniqueId;
        chatutils$fullMsg  = ChatUtilsState.currentFullMessage;

        // Only run head-detection once per original message (not for every wrapped line).
        if (chatutils$fullMsg != null && chatutils$fullMsg == ChatUtilsState.lastFullMessage) return;
        ChatUtilsState.lastFullMessage = chatutils$fullMsg;

        // Chat-heads require the feature to be enabled.
        if (ATHRConfig.feature == null || !ATHRConfig.feature.chat.chatHeads) return;

        NetHandlerPlayClient netHandler = Minecraft.getMinecraft().getNetHandler();
        if (netHandler == null) return;

        String beforeColon = StringUtils.substringBefore(lineString.getFormattedText(), ":");
        Map<String, NetworkPlayerInfo> nicknameCache = new HashMap<>();

        try {
            for (String word : SPLIT_PATTERN.split(beforeColon)) {
                if (word.isEmpty()) continue;

                NetworkPlayerInfo info = netHandler.getPlayerInfo(word);
                if (info == null) {
                    info = chatutils$resolveNickname(word, netHandler, nicknameCache);
                }

                if (info != null) {
                    chatutils$detected = true;

                    boolean sameAsLast = ChatUtilsState.lastDetectedPlayer != null
                            && info.getGameProfile() == ChatUtilsState.lastDetectedPlayer.getGameProfile()
                            && ATHRConfig.feature.chat.hideHeadOnConsecutive;

                    chatutils$playerInfo = sameAsLast ? null : info;
                    ChatUtilsState.lastDetectedPlayer = info;
                    return;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Nickname lookup helper ────────────────────────────────────────────────

    @Unique
    @Nullable
    private static NetworkPlayerInfo chatutils$resolveNickname(
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

    // ── ChatLineHook implementation ───────────────────────────────────────────

    @Override public boolean chatutils$hasDetected()         { return chatutils$detected; }
    @Override public NetworkPlayerInfo chatutils$getPlayerInfo() { return chatutils$playerInfo; }
    @Override public long chatutils$getUniqueId()            { return chatutils$uniqueId; }
    @Override public IChatComponent chatutils$getFullMessage()   { return chatutils$fullMsg; }
}
