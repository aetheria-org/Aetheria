package io.hamlook.aetheria.features.chat

import io.hamlook.aetheria.core.ATHRConfig
import io.hamlook.aetheria.utils.compat.TextCompat
import net.minecraft.client.gui.ChatLine
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

    /**
     * Set when a dedup hit produces a copy that must replace the original
     * (frozen packet components can't be mutated in place). The mixin
     * re-adds it and cancels the original. Single-thread only (render thread).
     */
    @JvmField
    var replacement: IChatComponent? = null

    @JvmField
    var reentering: Boolean = false

    @JvmStatic
    fun takeReplacement(): IChatComponent? {
        val r = replacement
        replacement = null
        return r
    }

    private val TIMESTAMP_REGEX = Regex("""^(?:\[\d\d:\d\d(:\d\d)?( AM| PM)?]|<\d\d:\d\d(:\d\d)?>)\s""")

    // ── Public API (called from Java Mixins via @JvmStatic) ──────────────────

    @JvmStatic
    fun applyTimestamp(original: IChatComponent): IChatComponent {
        val cfg = ATHRConfig.feature?.chat ?: return original
        //#if MC < 1.16
        if (!cfg.timestampsEnabled) return original
        val src = original
        //#else
        //$$ val src = TextCompat.parseLegacyLiterals(original)
        //$$ if (!cfg.timestampsEnabled) return src
        //#endif
        val plain = TextCompat.getUnformattedText(src).trim()
        if (plain.isEmpty()) return src
        if (TIMESTAMP_REGEX.containsMatchIn(plain)) return src

        val pattern = when {
            cfg.timestamp24Hour && cfg.timestampShowSeconds -> "HH:mm:ss"
            cfg.timestamp24Hour                            -> "HH:mm"
            cfg.timestampShowSeconds                       -> "hh:mm:ss a"
            else                                           -> "hh:mm a"
        }

        val time = LocalDateTime.now().format(DateTimeFormatter.ofPattern(pattern))
        val bracket = if (cfg.timestampStyle == 0) "[$time] " else "<$time> "

        val wrapper = ChatComponentIgnored(EnumChatFormatting.GRAY.toString() + bracket + EnumChatFormatting.RESET)
        TextCompat.appendSibling(wrapper.wrapped, src)
        return wrapper.wrapped
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

        val clear = cleanColor(TextCompat.getFormattedTextWithResets(component)).trim()
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

        val countSuffix = "§7 (${decimalFormat.format(entry.messageCount)})"
        //#if MC < 1.16
        TextCompat.appendSibling(
            component,
            TextCompat.createText(countSuffix)
        )
        //#else
        //$$ val display = TextCompat.deepMutableCopy(component)
        //$$ TextCompat.appendSibling(display, TextCompat.fromFormatted(countSuffix))
        //$$ replacement = display
        //#endif
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
        //#if MC < 1.16
        while (i < drawnChatLines.size && i < 300) {
            if (tracked.contains(drawnChatLines[i])) {
                drawnChatLines.removeAt(i)
                removed = true
            } else {
                if (consecutiveOnly) break
                i++
            }
        }
        //#else
        //$$ val drawnRaw: MutableList<Any?> = drawnChatLines as MutableList<Any?>
        //$$ val liveParents = HashSet(chatLines)
        //$$ while (i < drawnRaw.size && i < 300) {
        //$$     val parent = (drawnRaw[i] as? ChatLineHook)?.`athr$getParentLine`()
        //$$     if (parent != null && (tracked.contains(parent) || !liveParents.contains(parent))) {
        //$$         drawnRaw.removeAt(i)
        //$$         removed = true
        //$$     } else {
        //$$         if (consecutiveOnly) break
        //$$         i++
        //$$     }
        //$$ }
        //#endif

        return removed
    }

    private fun getChatStyleHash(style: net.minecraft.util.ChatStyle): Int {
        val hoverHash = TextCompat.getHoverValue(style)?.let { getChatComponentHash(it) } ?: 0
        return listOf(
            TextCompat.getColorChatStyle(style),
            TextCompat.getBold(style),
            TextCompat.getItalic(style),
            TextCompat.getUnderlined(style),
            TextCompat.getStrikethrough(style),
            TextCompat.getObfuscated(style),
            TextCompat.getHoverAction(style),
            hoverHash,
            TextCompat.getChatClickEvent(style),
            TextCompat.getInsertion(style)
        ).hashCode()
    }

    private fun getChatComponentHash(component: IChatComponent): Int {
        val siblingHashes = component.siblings
            .filterNot { ChatComponentIgnored.isIgnored(it) }
            .map { getChatComponentHash(it) }

        if (ChatComponentIgnored.isIgnored(component)) return siblingHashes.hashCode()

        val cleaned = TextCompat.getUnformattedText(component)
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
