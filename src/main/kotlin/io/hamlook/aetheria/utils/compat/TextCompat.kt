package io.hamlook.aetheria.utils.compat

import io.hamlook.aetheria.utils.ColorUtils
import io.hamlook.aetheria.utils.render.ChromaTextRenderer
import net.minecraft.client.gui.ChatLine
//#if MC < 1.16
import net.minecraft.event.ClickEvent
import net.minecraft.event.HoverEvent
import net.minecraft.util.*
import net.minecraft.util.MathHelper
//#elseif MC < 1.21
//$$ import net.minecraft.ChatFormatting
//$$ import net.minecraft.network.chat.ClickEvent
//$$ import net.minecraft.network.chat.HoverEvent
//$$ import net.minecraft.network.chat.MutableComponent
//$$ import net.minecraft.network.chat.Style
//$$ import net.minecraft.network.chat.TextColor
//$$ import net.minecraft.network.chat.Component
//#else
//$$ import net.minecraft.text.ClickEvent
//$$ import net.minecraft.text.HoverEvent
//$$ import net.minecraft.text.MutableText
//$$ import net.minecraft.text.PlainTextContent
//$$ import net.minecraft.text.Style
//$$ import net.minecraft.text.Text
//$$ import net.minecraft.text.TextColor
//$$ import net.minecraft.util.Formatting
//$$ import net.minecraft.util.Identifier
//$$ import java.util.Optional
//#endif
//#if FORGE
import net.minecraftforge.client.ClientCommandHandler
//#endif

object TextCompat {

    @JvmStatic
    fun createText(text: String): IChatComponent =
        //#if MC < 1.16
        ChatComponentText(text)
        //#else
        //$$ Component.literal(text)
        //#endif

    @JvmStatic
    fun fromFormatted(text: String): IChatComponent =
        //#if MC < 1.16
        ChatComponentText(text)
        //#else
        //$$ parseFormattingCodes(text)
        //#endif

    //#if MC < 1.16
    //#else
    //$$ private fun parseFormattingCodes(text: String): MutableText {
    //$$     val out = Text.empty()
    //$$     parseInto(out, text, Style.EMPTY)
    //$$     return out
    //$$ }
    //$$
    //$$ private fun parseInto(out: MutableText, text: String, base: Style) {
    //$$     var style = base
    //$$     val cur = StringBuilder()
    //$$     fun flush() {
    //$$         if (cur.isNotEmpty()) {
    //$$             out.append(Text.literal(cur.toString()).setStyle(style))
    //$$             cur.setLength(0)
    //$$         }
    //$$     }
    //$$     var i = 0
    //$$     while (i < text.length) {
    //$$         val c = text[i]
    //$$         if (c == '§' && i + 1 < text.length) {
    //$$             flush()
    //$$             val code = text[i + 1].lowercaseChar()
    //$$             var step = 2
    //$$             val fmt = Formatting.byCode(code)
    //$$             if (code == 'z') {
    //$$                 var tier = 0
    //$$                 if (i + 2 < text.length && text[i + 2] in '0'..'4') {
    //$$                     tier = text[i + 2] - '0'
    //$$                     step = 3
    //$$                 }
    //$$                 style = style.withColor(ChromaTextRenderer.tierSentinelRgb(tier))
    //$$             } else if (fmt == null) {
    //$$                 cur.append(c).append(text[i + 1])
    //$$             } else if (fmt == Formatting.RESET) {
    //$$                 style = Style.EMPTY
    //$$             } else if ("0123456789abcdef".indexOf(code) != -1) {
    //$$                 style = Style.EMPTY.withColor(fmt).withClickEvent(style.clickEvent).withHoverEvent(style.hoverEvent)
    //$$             } else {
    //$$                 style = style.withFormatting(fmt)
    //$$             }
    //$$             i += step
    //$$         } else {
    //$$             cur.append(c)
    //$$             i++
    //$$         }
    //$$     }
    //$$     flush()
    //$$ }
    //$$
    //$$ /**
    //$$  * Converts legacy § codes inside literal nodes to real styles, in
    //$$  * place on a private deep copy. Servers bridging legacy plugins send
    //$$  * §-coded strings inside literal Texts, which modern renders
    //$$  * verbatim. Structure, node styles and child order are preserved;
    //$$  * children re-attach to the last run of their rebuilt parent.
    //$$  * Unknown codes pass through untouched; the §z chroma marker is consumed
//$$  * into a per-tier sentinel colour that the renderer swaps for the animated
//$$  * one.
    //$$  * Idempotent: parsed trees contain no literal § runs to convert.
    //$$  */
    //$$ @JvmStatic
    //$$ fun parseLegacyLiterals(text: Text): Text {
    //$$     if (!containsLegacyCode(text)) return text
    //$$     val copy = deepMutableCopy(text) as MutableText
    //$$     val rebuilt = parsedReplacement(copy)
    //$$     val out = Text.empty()
    //$$     for (n in rebuilt) out.append(n)
    //$$     return out
    //$$ }
    //$$
    //$$ private fun containsLegacyCode(node: Text): Boolean {
    //$$     val raw = (node.content as? PlainTextContent)?.string()
    //$$     if (raw != null && raw.contains('§')) return true
    //$$     for (s in node.siblings) if (containsLegacyCode(s)) return true
    //$$     return false
    //$$ }
    //$$
    //$$ private fun parsedReplacement(node: MutableText): List<Text> {
    //$$     val kids = node.siblings.flatMap { parsedReplacement(it as MutableText) }
    //$$     val raw = (node.content as? PlainTextContent)?.string()
    //$$     if (raw == null || !raw.contains('§')) {
    //$$         node.siblings.clear()
    //$$         for (k in kids) node.append(k)
    //$$         return listOf(node)
    //$$     }
    //$$     val tmp = Text.empty()
    //$$     parseInto(tmp, raw, node.style)
    //$$     val runs = tmp.siblings.toList()
    //$$     if (runs.isEmpty()) return kids
    //$$     for (k in kids) (runs.last() as MutableText).append(k)
    //$$     return runs
    //$$ }
    //#endif

    //#if MC < 1.21
    @JvmStatic
    fun createResourceLocation(domain: String, path: String): ResourceLocation =
        ResourceLocation(domain, path)

    @JvmStatic
    fun createResourceLocation(path: String): ResourceLocation =
        ResourceLocation(path)
    //#else
    //$$ @JvmStatic
    //$$ fun createResourceLocation(domain: String, path: String): Identifier =
    //$$     Identifier.of(domain, path)
    //$$
    //$$ @JvmStatic
    //$$ fun createResourceLocation(path: String): Identifier =
    //$$     Identifier.of(path)
    //#endif

    @JvmStatic
    fun getFormattedText(component: IChatComponent): String =
        //#if MC < 1.16
        component.formattedText
        //#else
        //$$ component.toLegacyCodes()
        //#endif

    @JvmStatic
    fun getUnformattedText(component: IChatComponent): String =
        //#if MC < 1.16
        component.unformattedText
        //#else
        //$$ ColorUtils.stripColor(component.string)
        //#endif

    @JvmStatic
    fun getUnformattedText(text: String): String = ColorUtils.stripColor(text)

    @JvmStatic
    fun getFormattedText(text: String): String = text

    @JvmStatic
    fun codedString(text: String): String = text

    @JvmStatic
    fun codedString(component: IChatComponent): String =
        //#if MC < 1.16
        getFormattedText(component)
        //#else
        //$$ (component as Text).toLegacyCodes()
        //#endif

    @JvmStatic
    fun plainString(text: String): String = getUnformattedText(text)

    @JvmStatic
    fun plainString(component: IChatComponent): String = getUnformattedText(component)

    @JvmStatic
    fun matchesCoded(haystack: String, needle: String): Boolean {
        if (needle.indexOf('§') < 0) return haystack.contains(needle)
        return normalizeRuns(haystack).contains(normalizeRuns(needle))
    }

    @JvmStatic
    fun matchesCoded(haystack: IChatComponent, needle: String): Boolean =
        //#if MC < 1.16
        matchesCoded(getFormattedText(haystack), needle)
        //#else
        //$$ matchesCoded((haystack as Text).toLegacyCodes(), needle)
        //#endif

    private fun normalizeRuns(s: String): String {
        if (s.indexOf('§') < 0) return s
        val sb = StringBuilder(s.length)
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '§' && i + 1 < s.length && s[i + 1] != 'r' && s[i + 1] != 'R') {
                val run = StringBuilder()
                while (i < s.length && s[i] == '§' && i + 1 < s.length) {
                    val code = Character.toLowerCase(s[i + 1])
                    if (code == 'r') break
                    run.append(code)
                    i += 2
                }
                val chars = run.toString().toCharArray()
                chars.sort()
                for (ch in chars) sb.append('§').append(ch)
            } else if (c == '§' && i + 1 < s.length) {
                sb.append("§r")
                i += 2
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }

    //#if MC < 1.16
    //#else
    //$$ private val codeLut: Map<Int, Char> by lazy {
    //$$     Formatting.values().mapNotNull { f -> f.colorValue?.let { it to f.code } }.toMap()
    //$$ }
    //$$
    //$$ private val legacyColors = intArrayOf(
    //$$     0x000000, 0x0000AA, 0x00AA00, 0x00AAAA,
    //$$     0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
    //$$     0x555555, 0x5555FF, 0x55FF55, 0x55FFFF,
    //$$     0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF
    //$$ )
    //$$ private val legacyCodes = charArrayOf(
    //$$     '0', '1', '2', '3', '4', '5', '6', '7',
    //$$     '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    //$$ )
    //$$
    //$$ private fun nearestLegacy(rgb: Int): Char {
    //$$     var best = 'f'
    //$$     var bestDist = Int.MAX_VALUE
    //$$     for (i in legacyColors.indices) {
    //$$         val c = legacyColors[i]
    //$$         val dr = ((rgb shr 16) and 255) - ((c shr 16) and 255)
    //$$         val dg = ((rgb shr 8) and 255) - ((c shr 8) and 255)
    //$$         val db = (rgb and 255) - (c and 255)
    //$$         val dist = dr * dr + dg * dg + db * db
    //$$         if (dist < bestDist) {
    //$$             bestDist = dist
    //$$             best = legacyCodes[i]
    //$$         }
    //$$     }
    //$$     return best
    //$$ }
    //$$
    //$$ private fun codesFor(style: Style): String {
    //$$     if (style.isEmpty) return ""
    //$$     return buildString {
    //$$     val rgb = style.color?.rgb
    //$$     val exact = if (rgb == null) null else codeLut[rgb]
    //$$     append('§').append(exact ?: if (rgb == null) 'r' else nearestLegacy(rgb))
    //$$     if (style.isBold) append("§l")
    //$$     if (style.isItalic) append("§o")
    //$$     if (style.isUnderlined) append("§n")
    //$$     if (style.isStrikethrough) append("§m")
    //$$     if (style.isObfuscated) append("§k")
    //$$     }
    //$$ }
    //$$
    //$$ private fun Text.toLegacyCodes(): String {
    //$$     val sb = StringBuilder()
    //$$     var lastCodes: String? = null
    //$$     visit({ style, str ->
    //$$         if (str.isNotEmpty()) {
    //$$             val codes = codesFor(style)
    //$$             if (codes != lastCodes) {
    //$$                 sb.append(codes)
    //$$                 lastCodes = codes
    //$$             }
    //$$             sb.append(str)
    //$$         }
    //$$         Optional.empty<String>()
    //$$     }, Style.EMPTY)
    //$$     return sb.toString()
    //$$ }
    //$$
    //$$ class StyledText internal constructor(
    //$$     val plain: String,
    //$$     private val starts: IntArray,
    //$$     private val styles: Array<Style>,
    //$$ ) {
//$$     fun colorRgbAt(offset: Int): Int {
//$$         val run = runIndexAt(offset)
//$$         return if (run >= 0) styles[run].color?.rgb ?: -1 else -1
//$$     }
//$$
//$$     private fun runIndexAt(offset: Int): Int {
//$$         var candidate = -1
//$$         for (i in starts.indices) {
//$$             if (starts[i] <= offset) candidate = i else break
//$$         }
//$$         return candidate
//$$     }
//$$ }
    //$$
    //$$ /**
    //$$  * Flattens a component into code-free text plus per-run styles.
    //$$  *
    //$$  * Plain text and style offsets come from the same walk, so offsets always
    //$$  * line up. Unlike [toLegacyCodes] this performs no code deduplication and no
    //$$  * colour quantisation, so it is lossless.
    //$$  */
    //$$ @JvmStatic
    //$$ fun flattenStyled(component: Text): StyledText {
    //$$     val starts = ArrayList<Int>()
    //$$     val styles = ArrayList<Style>()
    //$$     val sb = StringBuilder()
    //$$     component.visit({ style, str ->
    //$$         if (str.isNotEmpty()) {
    //$$             starts.add(sb.length)
    //$$             styles.add(style)
    //$$             sb.append(str)
    //$$         }
    //$$         Optional.empty<String>()
    //$$     }, Style.EMPTY)
    //$$     return StyledText(
    //$$         sb.toString(),
    //$$         starts.toIntArray(),
    //$$         styles.toTypedArray(),
    //$$     )
    //$$ }
    //$$
//$$ @JvmStatic
//$$ fun styledColorRgb(styled: StyledText, offset: Int): Int = styled.colorRgbAt(offset)
//$$
//$$ @JvmStatic
    //$$ fun appendStyledText(base: Text, text: String, rgb: Int): Text {
    //$$     val part = Text.literal(text)
    //$$     if (rgb >= 0) (part as MutableText).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb)))
    //$$     (base as MutableText).append(part)
    //$$     return base
    //$$ }
    //$$
    //$$ @JvmStatic
    //$$ fun appendPlainText(base: Text, text: String): Text {
    //$$     (base as MutableText).append(Text.literal(text))
    //$$     return base
    //$$ }
    //#endif

    @JvmStatic
    fun getUnformattedTextForChat(component: IChatComponent): String =
        //#if MC < 1.16
        component.unformattedTextForChat
        //#elseif MC < 1.21
        //$$ component.contents
        //#else
        //$$ component.string
        //#endif

    @JvmStatic
    fun appendString(base: IChatComponent, text: String): IChatComponent =
        //#if MC < 1.16
        base.appendText(text)
        //#else
        //$$ mutableAppend(base, createText(text))
        //#endif

    @JvmStatic
    fun appendSibling(base: IChatComponent, sibling: IChatComponent): IChatComponent =
        //#if MC < 1.16
        base.appendSibling(sibling)
        //#else
        //$$ mutableAppend(base, sibling)
        //#endif

    /**
     * Deep-mutable copy: fresh root with a fresh siblings list at EVERY level.
     * Packet-deserialized texts can carry immutable lists at any depth
     * (vanilla codec output), so a top-level copy is not sufficient.
     * Built with of()+append (never copy(), which the toolchain remaps).
     */
    @JvmStatic
    fun deepMutableCopy(base: IChatComponent): IChatComponent =
        //#if MC < 1.16
        base
        //#else
        //$$ deepCopy(base)
        //#endif

    //$$ private fun deepCopy(text: Text): MutableText {
    //$$     val out = MutableText.of(text.content).setStyle(text.style)
    //$$     for (s in text.siblings) out.append(deepCopy(s))
    //$$     return out
    //$$ }
    //$$
    //$$ private fun mutableAppend(base: Text, sibling: Text): Text {
    //$$     val target = (base as? MutableText)?.takeIf { it.siblings is ArrayList<*> } ?: deepCopy(base)
    //$$     target.append(sibling)
    //$$     return target
    //$$ }

    @JvmStatic
    fun getChatStyle(component: IChatComponent): ChatStyle =
        //#if MC < 1.16
        component.chatStyle
        //#else
        //$$ component.style
        //#endif

    @JvmStatic
    fun setChatStyle(component: IChatComponent, style: ChatStyle): IChatComponent =
        //#if MC < 1.16
        component.setChatStyle(style)
        //#else
        //$$ (component as MutableText).setStyle(style)
        //#endif

    @JvmStatic
    fun createStyle(): ChatStyle =
        //#if MC < 1.16
        ChatStyle()
        //#else
        //$$ Style.EMPTY
        //#endif

    @JvmStatic
    fun createDeepCopy(style: ChatStyle): ChatStyle =
        //#if MC < 1.16
        style.createDeepCopy()
        //#else
        //$$ style
        //#endif

    @JvmStatic
    fun createCopy(component: IChatComponent): IChatComponent =
        //#if MC < 1.16
        component.createCopy()
        //#else
        //$$ component.copy()
        //#endif

    @JvmStatic
    fun setBold(component: IChatComponent, bold: Boolean): IChatComponent {
        //#if MC < 1.16
        component.chatStyle.bold = bold
        //#else
        //$$ (component as MutableText).setStyle(component.style.withBold(bold))
        //#endif
        return component
    }

    @JvmStatic
    fun setItalic(component: IChatComponent, italic: Boolean): IChatComponent {
        //#if MC < 1.16
        component.chatStyle.italic = italic
        //#else
        //$$ (component as MutableText).setStyle(component.style.withItalic(italic))
        //#endif
        return component
    }

    @JvmStatic
    fun setUnderlined(component: IChatComponent, underlined: Boolean): IChatComponent {
        //#if MC < 1.16
        component.chatStyle.underlined = underlined
        //#else
        //$$ (component as MutableText).setStyle(component.style.withUnderline(underlined))
        //#endif
        return component
    }

    @JvmStatic
    fun setUnderlined(style: ChatStyle, underlined: Boolean): ChatStyle =
        //#if MC < 1.16
        style.setUnderlined(underlined)
        //#else
        //$$ style.withUnderline(underlined)
        //#endif

    @JvmStatic
    fun setStrikethrough(component: IChatComponent, strikethrough: Boolean): IChatComponent {
        //#if MC < 1.16
        component.chatStyle.strikethrough = strikethrough
        //#else
        //$$ (component as MutableText).setStyle(component.style.withStrikethrough(strikethrough))
        //#endif
        return component
    }

    @JvmStatic
    fun setObfuscated(component: IChatComponent, obfuscated: Boolean): IChatComponent {
        //#if MC < 1.16
        component.chatStyle.obfuscated = obfuscated
        //#else
        //$$ (component as MutableText).setStyle(component.style.withObfuscated(obfuscated))
        //#endif
        return component
    }

    @JvmStatic
    fun setColor(component: IChatComponent, color: EnumChatFormatting): IChatComponent {
        //#if MC < 1.16
        component.chatStyle.color = color
        //#else
        //$$ (component as MutableText).setStyle(component.style.withColor(Formatting.byColorIndex(color.ordinal)))
        //#endif
        return component
    }

    @JvmStatic
    fun setClickEvent(style: ChatStyle, action: ClickEvent.Action, value: String): ChatStyle =
        //#if MC < 1.16
        style.setChatClickEvent(ClickEvent(action, value))
        //#else
        //$$ style.withClickEvent(ClickEvent(action, value))
        //#endif

    @JvmStatic
    fun setClickRunCommand(style: ChatStyle, command: String): ChatStyle =
        //#if MC < 1.16
        style.setChatClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
        //#else
        //$$ style.withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
        //#endif

    @JvmStatic
    fun setClickSuggestCommand(style: ChatStyle, command: String): ChatStyle =
        //#if MC < 1.16
        style.setChatClickEvent(ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command))
        //#else
        //$$ style.withClickEvent(ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command))
        //#endif

    @JvmStatic
    fun setClickOpenUrl(style: ChatStyle, url: String): ChatStyle =
        //#if MC < 1.16
        style.setChatClickEvent(ClickEvent(ClickEvent.Action.OPEN_URL, url))
        //#else
        //$$ style.withClickEvent(ClickEvent(ClickEvent.Action.OPEN_URL, url))
        //#endif

    @JvmStatic
    fun getClickAction(style: ChatStyle): ClickEvent.Action? =
        //#if MC < 1.16
        style.chatClickEvent?.action
        //#else
        //$$ style.clickEvent?.action
        //#endif

    @JvmStatic
    fun getClickValue(style: ChatStyle): String? =
        //#if MC < 1.16
        style.chatClickEvent?.value
        //#else
        //$$ style.clickEvent?.value
        //#endif

    @JvmStatic
    fun getClickActionName(style: ChatStyle): String? =
        getClickAction(style)?.name

    @JvmStatic
    fun isClickAction(style: ChatStyle, actionName: String): Boolean =
        getClickAction(style)?.name == actionName

    @JvmStatic
    fun setHoverEvent(
        style: ChatStyle,
        //#if MC < 1.16
        action: HoverEvent.Action,
        //#else
        //$$ action: HoverEvent.Action<*>,
        //#endif
        value: IChatComponent
    ): ChatStyle =
        //#if MC < 1.16
        style.setChatHoverEvent(HoverEvent(action, value))
        //#else
        //$$ style.withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, value))
        //#endif

    @JvmStatic
    fun setHoverShowText(style: ChatStyle, text: String): ChatStyle =
        //#if MC < 1.16
        style.setChatHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, ChatComponentText(text)))
        //#else
        //$$ style.withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, parseFormattingCodes(text)))
        //#endif

    @JvmStatic
    fun setHoverShowText(style: ChatStyle, component: IChatComponent): ChatStyle =
        //#if MC < 1.16
        style.setChatHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, component))
        //#else
        //$$ style.withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, component))
        //#endif

    //#if MC < 1.16
    @JvmStatic
    fun getHoverAction(style: ChatStyle): HoverEvent.Action? =
        style.chatHoverEvent?.action
    //#else
    //$$ @JvmStatic
    //$$ fun getHoverAction(style: Style): HoverEvent.Action<*>? =
    //$$     style.hoverEvent?.action
    //#endif

    @JvmStatic
    fun getHoverValue(style: ChatStyle): IChatComponent? {
        //#if MC < 1.16
        return style.chatHoverEvent?.value
        //#else
        //$$ val event = style.hoverEvent ?: return null
        //$$ if (event.action != HoverEvent.Action.SHOW_TEXT) return null
        //$$ return event.getValue(HoverEvent.Action.SHOW_TEXT)
        //#endif
    }

    @JvmStatic
    fun withClickRunCommand(component: IChatComponent, command: String): IChatComponent {
        //#if MC < 1.16
        component.chatStyle.setChatClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
        //#else
        //$$ (component as MutableText).setStyle(component.style.withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, command)))
        //#endif
        return component
    }

    @JvmStatic
    fun withClickSuggestCommand(component: IChatComponent, command: String): IChatComponent {
        //#if MC < 1.16
        component.chatStyle.setChatClickEvent(ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command))
        //#else
        //$$ (component as MutableText).setStyle(component.style.withClickEvent(ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command)))
        //#endif
        return component
    }

    @JvmStatic
    fun withHoverShowText(component: IChatComponent, text: String): IChatComponent {
        //#if MC < 1.16
        component.chatStyle.setChatHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, ChatComponentText(text)))
        //#else
        //$$ (component as MutableText).setStyle(component.style.withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, parseFormattingCodes(text))))
        //#endif
        return component
    }

    @JvmStatic
    fun withStyle(component: IChatComponent, style: ChatStyle): IChatComponent {
        //#if MC < 1.16
        component.setChatStyle(style)
        //#else
        //$$ (component as MutableText).setStyle(style)
        //#endif
        return component
    }

    @JvmStatic
    fun getSiblings(component: IChatComponent): List<IChatComponent> =
        //#if MC < 1.16
        component.siblings
        //#else
        //$$ component.siblings
        //#endif

    @JvmStatic
    fun setColor(style: ChatStyle, color: EnumChatFormatting): ChatStyle {
        //#if MC < 1.16
        style.color = color
        //#else
        //$$ style.withColor(Formatting.byColorIndex(color.ordinal))
        //#endif
        return style
    }

    @JvmStatic
    fun getClickEvent(style: ChatStyle): ClickEvent? =
        //#if MC < 1.16
        style.chatClickEvent
        //#else
        //$$ style.clickEvent
        //#endif

    @JvmStatic
    fun getHoverEvent(style: ChatStyle): HoverEvent? =
        //#if MC < 1.16
        style.chatHoverEvent
        //#else
        //$$ style.hoverEvent
        //#endif

    @JvmStatic
    fun createClickableStyle(
        command: String? = null,
        hoverText: String? = null,
        suggestCommand: String? = null,
        url: String? = null
    ): ChatStyle {
        val style = createStyle()
        if (command != null) {
            setClickRunCommand(style, command)
        }
        if (suggestCommand != null) {
            setClickSuggestCommand(style, suggestCommand)
        }
        if (url != null) {
            setClickOpenUrl(style, url)
        }
        if (hoverText != null) {
            setHoverShowText(style, hoverText)
        }
        return style
    }

    @JvmStatic
    fun addChatMessage(message: IChatComponent) {
        //#if FORGE
        MinecraftCompat.getLocalPlayer()?.addChatMessage(message)
        //#else
        //$$ MinecraftCompat.getMinecraft().player?.sendMessage(message, false)
        //#endif
    }

    @JvmStatic
    fun sendChatMessage(message: String) {
        //#if FORGE
        val player = MinecraftCompat.getLocalPlayer() ?: return
        if (!message.startsWith("/") || ClientCommandHandler.instance.executeCommand(player, message) == 0) {
            player.sendChatMessage(message)
        }
        //#else
        //$$ val handler = MinecraftCompat.getMinecraft().networkHandler ?: return
        //$$ if (message.startsWith("/")) handler.sendChatCommand(message.substring(1))
        //$$ else handler.sendChatMessage(message)
        //#endif
    }

    @JvmStatic
    fun addChatMessageWithId(message: IChatComponent, id: Int) {
        //#if MC < 1.16
        MinecraftCompat.getMinecraft().ingameGUI.chatGUI.printChatMessageWithOptionalDeletion(message, id)
        //#else
        //$$ MinecraftCompat.getMinecraft().gui.chat.addMessage(message)
        //#endif
    }

    @JvmStatic
    fun convertToJson(component: IChatComponent): String =
        //#if MC < 1.16
        IChatComponent.Serializer.componentToJson(component)
        //#elseif MC < 1.21
        //$$ Component.Serializer.toJson(component)
        //#else
        //$$ TODO("1.21 component JSON needs registry ops")
        //#endif

    @JvmStatic
    fun convertFromJson(json: String): IChatComponent =
        //#if MC < 1.16
        IChatComponent.Serializer.jsonToComponent(json)
        //#elseif MC < 1.21
        //$$ Component.Serializer.fromJson(json) ?: Component.empty()
        //#else
        //$$ TODO("1.21 component JSON needs registry ops")
        //#endif

    @JvmStatic
    fun getBold(style: ChatStyle): Boolean =
        //#if MC < 1.16
        style.bold ?: false
        //#else
        //$$ style.isBold
        //#endif

    @JvmStatic
    fun getItalic(style: ChatStyle): Boolean =
        //#if MC < 1.16
        style.italic ?: false
        //#else
        //$$ style.isItalic
        //#endif

    @JvmStatic
    fun getUnderlined(style: ChatStyle): Boolean =
        //#if MC < 1.16
        style.underlined ?: false
        //#else
        //$$ style.isUnderlined
        //#endif

    @JvmStatic
    fun getStrikethrough(style: ChatStyle): Boolean =
        //#if MC < 1.16
        style.strikethrough ?: false
        //#else
        //$$ style.isStrikethrough
        //#endif

    @JvmStatic
    fun getObfuscated(style: ChatStyle): Boolean =
        //#if MC < 1.16
        style.obfuscated ?: false
        //#else
        //$$ style.isObfuscated
        //#endif

    @JvmStatic
    fun getColorChatStyle(style: ChatStyle): Any? =
        //#if MC < 1.16
        style.color
        //#else
        //$$ style.color
        //#endif

    @JvmStatic
    fun getInsertion(style: ChatStyle): String? =
        //#if MC < 1.16
        style.insertion
        //#else
        //$$ style.insertion
        //#endif

    @JvmStatic
    fun getChatClickEvent(style: ChatStyle): Any? =
        //#if MC < 1.16
        style.chatClickEvent
        //#else
        //$$ style.clickEvent
        //#endif

    @JvmStatic
    fun getChatHoverEvent(style: ChatStyle): Any? =
        //#if MC < 1.16
        style.chatHoverEvent
        //#else
        //$$ style.hoverEvent
        //#endif

    @JvmStatic
    fun getTextWithoutFormattingCodes(text: String): String =
        //#if MC < 1.16
        EnumChatFormatting.getTextWithoutFormattingCodes(text)
        //#else
        //$$ ChatFormatting.stripFormatting(text) ?: text
        //#endif

    @JvmStatic
    fun floor(value: Float): Int =
        //#if MC < 1.16
        MathHelper.floor_float(value)
        //#else
        //$$ kotlin.math.floor(value).toInt()
        //#endif

    @JvmStatic
    fun getChatLineContent(line: ChatLine): IChatComponent =
        //#if MC < 1.16
        line.chatComponent
        //#else
        //$$ line.content()
        //#endif

}
