package io.hamlook.aetheria.utils.hooks

import io.hamlook.aetheria.Aetheria
import io.hamlook.aetheria.core.ATHRConfig
import io.hamlook.aetheria.features.chat.ChatUtilsState
import io.hamlook.aetheria.utils.compat.MinecraftCompat
import net.minecraft.client.network.NetHandlerPlayClient
import net.minecraft.client.network.NetworkPlayerInfo
import net.minecraft.util.IChatComponent
import org.apache.commons.lang3.StringUtils
import java.util.logging.Level
import java.util.regex.Pattern

object ChatLineInitHook {

    private val SPLIT_PATTERN = Pattern.compile("(§.)|\\W")

    @JvmStatic
    fun resolveNickname(
        word: String, connection: NetHandlerPlayClient, cache: MutableMap<String, NetworkPlayerInfo>
    ): NetworkPlayerInfo? {
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
    }

    @JvmStatic
    fun resolveUsername(
        word: String, connection: NetHandlerPlayClient
    ): NetworkPlayerInfo? {
        for (p in connection.playerInfoMap) {
            if (p.gameProfile != null && word == p.gameProfile.name) {
                return p
            }
        }
        return null
    }

    @JvmStatic
    fun detectPlayer(
        lineString: IChatComponent, existingPlayer: NetworkPlayerInfo?, lastFullMsg: IChatComponent?
    ): PlayerDetectionResult {
        val config = ATHRConfig.feature
        if (config == null || !config.chat.chatHeads) {
            return PlayerDetectionResult(false, null, null)
        }

        val currentFullMsg = ChatUtilsState.currentFullMessage
        if (currentFullMsg != null && currentFullMsg == lastFullMsg) {
            return PlayerDetectionResult(false, null, currentFullMsg)
        }

        val netHandler =
            MinecraftCompat.getMinecraft().netHandler ?: return PlayerDetectionResult(false, null, currentFullMsg)

        val text = StringUtils.substringAfter(lineString.formattedText, "]")
        val beforeColon = StringUtils.substringBefore(text, ":")
        val nicknameCache = HashMap<String, NetworkPlayerInfo>()

        try {
            for (word in SPLIT_PATTERN.split(beforeColon)) {
                if (word.isEmpty()) continue

                var info: NetworkPlayerInfo? = netHandler.getPlayerInfo(word)
                if (info == null) {
                    info = resolveNickname(word, netHandler, nicknameCache)
                }
                if (info == null) {
                    info = resolveUsername(word, netHandler)
                }

                if (info != null) {
                    val sameAsLast =
                        ChatUtilsState.lastDetectedPlayer != null && info.gameProfile == ChatUtilsState.lastDetectedPlayer!!.gameProfile && config.chat.hideHeadOnConsecutive

                    val playerInfo = if (sameAsLast) null else info
                    ChatUtilsState.lastDetectedPlayer = info
                    return PlayerDetectionResult(true, playerInfo, currentFullMsg)
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
}
