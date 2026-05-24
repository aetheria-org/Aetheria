package io.hamlook.aetheria.features.chat

import io.hamlook.aetheria.core.ATHRConfig
import net.minecraft.client.gui.ChatLine
import net.minecraft.event.HoverEvent
import net.minecraft.util.ChatComponentText
import net.minecraft.util.EnumChatFormatting
import net.minecraft.util.IChatComponent
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object ChatCompactHandler {

    // ── Internal state ────────────────────────────────────────────────────────

    private data class ChatEntry(var messageCount: Int, var lastSeenMessageMillis: Long)

    private val chatMessageMap = HashMap<Int, ChatEntry>()
    private val messagesForHash = HashMap<Int, HashSet<ChatLine>>()
    private val decimalFormat = DecimalFormat("#,###")

    @JvmField
    var currentMessageHash: Int = -1

    private val TIMESTAMP_REGEX = Regex("""^(?:\[\d\d:\d\d(:\d\d)?( AM| PM)?]|<\d\d:\d\d(:\d\d)?>)\s""")

    // ── Public API (called from Java Mixins via @JvmStatic) ──────────────────

    @JvmStatic
    fun applyTimestamp(original: IChatComponent): IChatComponent {
        val cfg = ATHRConfig.feature?.chat ?: return original
        if (!cfg.timestampsEnabled) return original
        if (original.unformattedText.trim().isEmpty()) return original

        val pattern = when {
            cfg.timestamp24Hour && cfg.timestampShowSeconds -> "HH:mm:ss"
            cfg.timestamp24Hour                            -> "HH:mm"
            cfg.timestampShowSeconds                       -> "hh:mm:ss a"
            else                                           -> "hh:mm a"
        }

        val time = LocalDateTime.now().format(DateTimeFormatter.ofPattern(pattern))
        val bracket = if (cfg.timestampStyle == 0) "[$time] " else "<$time> "

        val wrapper = ChatComponentIgnored(EnumChatFormatting.GRAY.toString() + bracket + EnumChatFormatting.RESET)
        wrapper.appendSibling(original)
        return wrapper
    }

    @JvmStatic
    fun handleChatMessage(
        component: IChatComponent,
        refresh: Boolean,
        chatLines: MutableList<ChatLine>,
        drawnChatLines: MutableList<ChatLine>
    ) {
        val cfg = ATHRConfig.feature?.chat ?: return
        if (!cfg.compactingEnabled || refresh) return

        val clear = cleanColor(component.formattedText).trim()
        if (clear.isEmpty() || isDivider(clear)) return

        val hash = getChatComponentHash(component)
        currentMessageHash = hash

        val now = System.currentTimeMillis()
        val entry = chatMessageMap[hash]

        if (entry == null) {
            chatMessageMap[hash] = ChatEntry(1, now)
            return
        }

        val expireSec = cfg.expireTimeSeconds
        if (expireSec != -1 && (now - entry.lastSeenMessageMillis) > expireSec * 1000L) {
            chatMessageMap[hash] = ChatEntry(1, now)
            return
        }

        val removed = deleteMessageByHash(hash, chatLines, drawnChatLines, cfg.consecutiveOnly)
        if (!removed) {
            chatMessageMap[hash] = ChatEntry(1, now)
            return
        }

        entry.messageCount++
        entry.lastSeenMessageMillis = now

        component.appendSibling(
            ChatComponentText(EnumChatFormatting.GRAY.toString() + " (${decimalFormat.format(entry.messageCount)})")
        )
    }

    @JvmStatic
    fun trackChatLine(line: ChatLine) {
        if (currentMessageHash == -1) return
        messagesForHash.getOrPut(currentMessageHash) { HashSet() }.add(line)
    }

    @JvmStatic
    fun resetMessageHash() {
        currentMessageHash = -1
    }

    @JvmStatic
    fun cleanupExpired() {
        val expireSec = ATHRConfig.feature?.chat?.expireTimeSeconds ?: return
        if (expireSec == -1) return

        val now = System.currentTimeMillis()
        val iter = chatMessageMap.entries.iterator()
        while (iter.hasNext()) {
            val e = iter.next()
            if ((now - e.value.lastSeenMessageMillis) > expireSec * 1000L) {
                messagesForHash.remove(e.key)
                iter.remove()
            }
        }
    }

    @JvmStatic
    fun reset() {
        chatMessageMap.clear()
        messagesForHash.clear()
        currentMessageHash = -1
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun deleteMessageByHash(
        hash: Int,
        chatLines: MutableList<ChatLine>,
        drawnChatLines: MutableList<ChatLine>,
        consecutiveOnly: Boolean
    ): Boolean {
        val tracked = messagesForHash.remove(hash) ?: return false
        if (tracked.isEmpty()) return false

        var removed = false

        var i = 0
        while (i < chatLines.size && i < 100) {
            if (tracked.contains(chatLines[i])) {
                chatLines.removeAt(i)
                removed = true
                // don't increment i — the next element has shifted down
            } else {
                if (consecutiveOnly) break
                i++
            }
        }

        i = 0
        while (i < drawnChatLines.size && i < 300) {
            if (tracked.contains(drawnChatLines[i])) {
                drawnChatLines.removeAt(i)
                removed = true
            } else {
                if (consecutiveOnly) break
                i++
            }
        }

        return removed
    }

    private fun getChatStyleHash(style: net.minecraft.util.ChatStyle): Int {
        val hover = style.chatHoverEvent
        val action = hover?.action
        val hoverHash = if (hover != null) getChatComponentHash(hover.value) else 0
        return listOf(
            style.color,
            style.bold,
            style.italic,
            style.underlined,
            style.strikethrough,
            style.obfuscated,
            action,
            hoverHash,
            style.chatClickEvent,
            style.insertion
        ).hashCode()
    }

    private fun getChatComponentHash(component: IChatComponent): Int {
        val siblingHashes = component.siblings
            .filterNot { it is ChatComponentIgnored }
            .map { getChatComponentHash(it) }

        if (component is ChatComponentIgnored) return siblingHashes.hashCode()

        val cleaned = component.unformattedText
            .replace(TIMESTAMP_REGEX, "")
            .trim()

        return listOf(cleaned, siblingHashes, getChatStyleHash(component.chatStyle)).hashCode()
    }

    private fun isDivider(text: String): Boolean {
        val stripped = text.replace(TIMESTAMP_REGEX, "").trim()
        if (stripped.length < 5) return false
        return stripped.all { it == '-' || it == '=' || it == '\u25AC' }
    }

    private fun cleanColor(text: String) = text.replace(Regex("§."), "")
}
