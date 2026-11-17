package io.hamlook.aetheria.utils.hooks

import io.hamlook.aetheria.Aetheria
import io.hamlook.aetheria.core.ATHRConfig
import io.hamlook.aetheria.features.chat.ChatLineHook
import io.hamlook.aetheria.features.chat.ChatUtilsState
import io.hamlook.aetheria.utils.compat.MinecraftCompat
import io.hamlook.aetheria.utils.compat.TextCompat
import net.minecraft.client.network.NetHandlerPlayClient
import net.minecraft.client.network.NetworkPlayerInfo
import net.minecraft.util.IChatComponent
//#if MC < 1.16
//#else
//$$ import net.minecraft.client.gui.hud.ChatHudLine
//#endif
import org.apache.commons.lang3.StringUtils
import java.util.logging.Level
import java.util.regex.Pattern

object ChatLineInitHook {

    private val SPLIT_PATTERN = Pattern.compile("(§.)|\\W")

    @JvmStatic
    fun resolveNickname(
        word: String, connection: NetHandlerPlayClient, cache: MutableMap<String, NetworkPlayerInfo>
    ): NetworkPlayerInfo? {
        //#if MC < 1.16
        if (cache.isEmpty()) {
            for (p in connection.playerInfoMap) {
                val displayName: IChatComponent = p.displayName ?: continue
                val nickname = displayName.unformattedTextForChat
                if (word == nickname) return p
                cache[nickname] = p
            }
            return null
        }
        return cache[word]
        //#else
        //$$ if (cache.isEmpty()) {
        //$$     for (p in connection.playerList) {
        //$$         p.displayName?.let { name -> cache[TextCompat.getUnformattedText(name)] = p }
        //$$     }
        //$$ }
        //$$ return cache[word]
        //#endif
    }

    @JvmStatic
    fun resolveUsername(
        word: String, connection: NetHandlerPlayClient
    ): NetworkPlayerInfo? {
        //#if MC < 1.16
        for (p in connection.playerInfoMap) {
            if (p.gameProfile != null && word == p.gameProfile.name) {
                return p
            }
        }
        return null
        //#else
        //$$ for (p in connection.playerList) {
        //$$     if (word.equals(p.getProfile()?.name, ignoreCase = true)) return p
        //$$ }
        //$$ return null
        //#endif
    }

    @JvmStatic
    fun detectPlayer(
        lineString: IChatComponent, existingPlayer: NetworkPlayerInfo?, lastFullMsg: IChatComponent?
    ): PlayerDetectionResult {
        val config = ATHRConfig.feature
        if (config == null || !config.chat.heads.chatHeads) {
            return PlayerDetectionResult(false, null, null)
        }

        val currentFullMsg = ChatUtilsState.currentFullMessage
        if (currentFullMsg != null && currentFullMsg == lastFullMsg) {
            return PlayerDetectionResult(false, null, currentFullMsg)
        }

        val netHandler =
            MinecraftCompat.getMinecraft().netHandler ?: return PlayerDetectionResult(false, null, currentFullMsg)

        val text = StringUtils.substringAfter(TextCompat.getFormattedText(lineString), "]")
        val beforeColon = StringUtils.substringBefore(text, ":")
        val nicknameCache = HashMap<String, NetworkPlayerInfo>()

        try {
            for (word in SPLIT_PATTERN.split(beforeColon)) {
                if (word.isEmpty()) continue

                var info: NetworkPlayerInfo? =
                    //#if MC < 1.16
                    netHandler.getPlayerInfo(word)
                    //#else
                    //$$ netHandler.getPlayerListEntry(word)
                    //#endif
                if (info == null) {
                    info = resolveNickname(word, netHandler, nicknameCache)
                }
                if (info == null) {
                    info = resolveUsername(word, netHandler)
                }

                if (info != null) {
                    //#if MC < 1.16
                    val sameAsLast =
                        ChatUtilsState.lastDetectedPlayer != null && info.gameProfile == ChatUtilsState.lastDetectedPlayer!!.gameProfile && config.chat.heads.hideHeadOnConsecutive

                    val playerInfo = if (sameAsLast) null else info
                    ChatUtilsState.lastDetectedPlayer = info
                    return PlayerDetectionResult(true, playerInfo, currentFullMsg)
                    //#else
                    //$$ return PlayerDetectionResult(true, info, currentFullMsg)
                    //#endif
                }
            }
        } catch (e: Exception) {
            Aetheria.logger.log(Level.WARNING, "Chat head detection failed", e)
        }

        return PlayerDetectionResult(false, null, currentFullMsg)
    }

    data class PlayerDetectionResult(
        val detected: Boolean, val playerInfo: NetworkPlayerInfo?, val currentFullMessage: IChatComponent?
    )

    //#if MC < 1.16
    //#else
    //$$ @JvmStatic
    //$$ fun isConsecutiveHidden(info: PlayerListEntry?): Boolean {
    //$$     val config = ATHRConfig.feature ?: return false
    //$$     val last = ChatUtilsState.lastDetectedPlayer
    //$$     val hidden = config.chat.heads.hideHeadOnConsecutive && info != null && last != null &&
    //$$         info.getProfile()?.id == last.getProfile()?.id
    //$$     ChatUtilsState.lastDetectedPlayer = info
    //$$     return hidden
    //$$ }
    //#endif

    //#if MC < 1.16
    //#else
    //$$ @JvmStatic
    //$$ fun tagHead(line: ChatHudLine) {
    //$$     val result = detectPlayer(line.content(), null, ChatUtilsState.lastFullMessage)
    //$$     ChatUtilsState.lastFullMessage = result.currentFullMessage
    //$$     val hook = line as? ChatLineHook ?: return
    //$$     if (!result.detected) {
    //$$         return
    //$$     }
    //$$     val info = result.playerInfo
    //$$     hook.`athr$setPlayerInfo`(info)
    //$$     hook.`athr$setHeadHidden`(isConsecutiveHidden(info))
    //$$ }
    //#endif
}
