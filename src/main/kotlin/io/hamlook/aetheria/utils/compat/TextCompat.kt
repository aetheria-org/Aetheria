package io.hamlook.aetheria.utils.compat

import io.hamlook.aetheria.utils.ColorUtils
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
//$$ import net.minecraft.text.Style
//$$ import net.minecraft.text.Text
//$$ import net.minecraft.util.Formatting
//$$ import net.minecraft.util.Identifier
//#endif

object TextCompat {

    @JvmStatic
    fun createText(text: String): IChatComponent =
        //#if MC < 1.16
        ChatComponentText(text)
        //#else
        //$$ Component.literal(text)
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
        //$$ component.string
        //#endif

    @JvmStatic
    fun getUnformattedText(component: IChatComponent): String =
        //#if MC < 1.16
        component.unformattedText
        //#else
        //$$ ColorUtils.stripColor(component.string)
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
        //$$ (base as MutableText).append(text)
        //#endif

    @JvmStatic
    fun appendSibling(base: IChatComponent, sibling: IChatComponent): IChatComponent =
        //#if MC < 1.16
        base.appendSibling(sibling)
        //#else
        //$$ (base as MutableText).append(sibling)
        //#endif

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
        //$$ style.withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(text)))
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
        //$$ (component as MutableText).setStyle(component.style.withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(text))))
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
        MinecraftCompat.getLocalPlayer()?.sendChatMessage(message)
        //#else
        //$$ MinecraftCompat.getMinecraft().player?.sendMessage(Text.literal(message), false)
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
