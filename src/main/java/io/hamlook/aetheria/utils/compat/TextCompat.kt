package io.hamlook.aetheria.utils.compat

import net.minecraft.client.Minecraft
import net.minecraft.event.ClickEvent
import net.minecraft.event.HoverEvent
import net.minecraft.util.ChatComponentText
import net.minecraft.util.ChatStyle
import net.minecraft.util.EnumChatFormatting
import net.minecraft.util.IChatComponent
import net.minecraft.util.ResourceLocation

object TextCompat {

    @JvmStatic
    fun createText(text: String): IChatComponent = ChatComponentText(text)

    @JvmStatic
    fun createResourceLocation(domain: String, path: String): ResourceLocation =
        ResourceLocation(domain, path)

    @JvmStatic
    fun createResourceLocation(path: String): ResourceLocation =
        ResourceLocation(path)

    @JvmStatic
    fun getFormattedText(component: IChatComponent): String =
        component.formattedText

    @JvmStatic
    fun getUnformattedText(component: IChatComponent): String =
        component.unformattedText

    @JvmStatic
    fun getUnformattedTextForChat(component: IChatComponent): String =
        component.unformattedTextForChat

    @JvmStatic
    fun appendString(base: IChatComponent, text: String): IChatComponent =
        base.appendText(text)

    @JvmStatic
    fun appendSibling(base: IChatComponent, sibling: IChatComponent): IChatComponent =
        base.appendSibling(sibling)

    @JvmStatic
    fun getChatStyle(component: IChatComponent): ChatStyle =
        component.chatStyle

    @JvmStatic
    fun setChatStyle(component: IChatComponent, style: ChatStyle): IChatComponent =
        component.setChatStyle(style)

    @JvmStatic
    fun createStyle(): ChatStyle = ChatStyle()

    @JvmStatic
    fun createDeepCopy(style: ChatStyle): ChatStyle = style.createDeepCopy()

    @JvmStatic
    fun createCopy(component: IChatComponent): IChatComponent = component.createCopy()

    @JvmStatic
    fun setBold(component: IChatComponent, bold: Boolean): IChatComponent {
        component.chatStyle.bold = bold
        return component
    }

    @JvmStatic
    fun setItalic(component: IChatComponent, italic: Boolean): IChatComponent {
        component.chatStyle.italic = italic
        return component
    }

    @JvmStatic
    fun setUnderlined(component: IChatComponent, underlined: Boolean): IChatComponent {
        component.chatStyle.underlined = underlined
        return component
    }

    @JvmStatic
    fun setStrikethrough(component: IChatComponent, strikethrough: Boolean): IChatComponent {
        component.chatStyle.strikethrough = strikethrough
        return component
    }

    @JvmStatic
    fun setObfuscated(component: IChatComponent, obfuscated: Boolean): IChatComponent {
        component.chatStyle.obfuscated = obfuscated
        return component
    }

    @JvmStatic
    fun setColor(component: IChatComponent, color: EnumChatFormatting): IChatComponent {
        component.chatStyle.color = color
        return component
    }

    @JvmStatic
    fun setClickEvent(style: ChatStyle, action: ClickEvent.Action, value: String): ChatStyle =
        style.setChatClickEvent(ClickEvent(action, value))

    @JvmStatic
    fun setClickRunCommand(style: ChatStyle, command: String): ChatStyle =
        style.setChatClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, command))

    @JvmStatic
    fun setClickSuggestCommand(style: ChatStyle, command: String): ChatStyle =
        style.setChatClickEvent(ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command))

    @JvmStatic
    fun setClickOpenUrl(style: ChatStyle, url: String): ChatStyle =
        style.setChatClickEvent(ClickEvent(ClickEvent.Action.OPEN_URL, url))

    @JvmStatic
    fun getClickAction(style: ChatStyle): ClickEvent.Action? =
        style.chatClickEvent?.action

    @JvmStatic
    fun getClickValue(style: ChatStyle): String? =
        style.chatClickEvent?.value

    @JvmStatic
    fun setHoverEvent(style: ChatStyle, action: HoverEvent.Action, value: IChatComponent): ChatStyle =
        style.setChatHoverEvent(HoverEvent(action, value))

    @JvmStatic
    fun setHoverShowText(style: ChatStyle, text: String): ChatStyle =
        style.setChatHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, ChatComponentText(text)))

    @JvmStatic
    fun setHoverShowText(style: ChatStyle, component: IChatComponent): ChatStyle =
        style.setChatHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, component))

    @JvmStatic
    fun getHoverAction(style: ChatStyle): HoverEvent.Action? =
        style.chatHoverEvent?.action

    @JvmStatic
    fun getHoverValue(style: ChatStyle): IChatComponent? =
        style.chatHoverEvent?.value

    @JvmStatic
    fun withClickRunCommand(component: IChatComponent, command: String): IChatComponent {
        component.chatStyle.setChatClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
        return component
    }

    @JvmStatic
    fun withClickSuggestCommand(component: IChatComponent, command: String): IChatComponent {
        component.chatStyle.setChatClickEvent(ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command))
        return component
    }

    @JvmStatic
    fun withHoverShowText(component: IChatComponent, text: String): IChatComponent {
        component.chatStyle.setChatHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, ChatComponentText(text)))
        return component
    }

    @JvmStatic
    fun withStyle(component: IChatComponent, style: ChatStyle): IChatComponent {
        component.setChatStyle(style)
        return component
    }

    @JvmStatic
    fun getSiblings(component: IChatComponent): List<IChatComponent> =
        component.siblings

    @JvmStatic
    fun setColor(style: ChatStyle, color: EnumChatFormatting): ChatStyle {
        style.color = color
        return style
    }

    @JvmStatic
    fun getClickEvent(style: ChatStyle): ClickEvent? =
        style.chatClickEvent

    @JvmStatic
    fun getHoverEvent(style: ChatStyle): HoverEvent? =
        style.chatHoverEvent

    @JvmStatic
    fun createClickableStyle(
        command: String? = null,
        hoverText: String? = null,
        suggestCommand: String? = null,
        url: String? = null
    ): ChatStyle {
        val style = ChatStyle()
        if (command != null) {
            style.chatClickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, command)
        }
        if (suggestCommand != null) {
            style.chatClickEvent = ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, suggestCommand)
        }
        if (url != null) {
            style.chatClickEvent = ClickEvent(ClickEvent.Action.OPEN_URL, url)
        }
        if (hoverText != null) {
            style.chatHoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, ChatComponentText(hoverText))
        }
        return style
    }

    @JvmStatic
    fun addChatMessage(message: IChatComponent) {
        Minecraft.getMinecraft().thePlayer?.addChatMessage(message)
    }

    @JvmStatic
    fun addChatMessageWithId(message: IChatComponent, id: Int) {
        Minecraft.getMinecraft().ingameGUI.chatGUI.printChatMessageWithOptionalDeletion(message, id)
    }

    @JvmStatic
    fun convertToJson(component: IChatComponent): String =
        IChatComponent.Serializer.componentToJson(component)

    @JvmStatic
    fun convertFromJson(json: String): IChatComponent =
        IChatComponent.Serializer.jsonToComponent(json)
}
