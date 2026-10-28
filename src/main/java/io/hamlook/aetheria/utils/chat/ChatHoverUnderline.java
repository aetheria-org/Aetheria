package io.hamlook.aetheria.utils.chat;

import io.hamlook.aetheria.utils.compat.TextCompat;
import io.hamlook.aetheria.utils.hooks.ChatHook;
//#if MC < 1.21
import net.minecraft.util.IChatComponent;
//#else
//$$ import net.minecraft.text.ClickEvent;
//$$ import net.minecraft.text.OrderedText;
//$$ import net.minecraft.text.Style;
//$$ import java.util.ArrayList;
//#endif

import java.util.List;

public final class ChatHoverUnderline {

    private ChatHoverUnderline() {}

    //#if MC < 1.21
    // legacy draws chat from a string, so rebuild it with the hovered name underlined
    public static String apply(String formatted, IChatComponent component, String clickValue) {
        if (clickValue == null) return formatted;
        IChatComponent copy = TextCompat.createCopy(component);
        return underline(copy, clickValue) ? TextCompat.getFormattedText(copy) : formatted;
    }

    private static boolean underline(IChatComponent node, String clickValue) {
        boolean changed = false;
        List<IChatComponent> children = TextCompat.getSiblings(node);
        for (int i = 0; i < children.size(); i++) {
            IChatComponent child = children.get(i);
            if (clickValue.equals(TextCompat.getClickValue(TextCompat.getChatStyle(child)))) {
                children.set(i, TextCompat.fromFormatted(
                        underlineRun(TextCompat.getUnformattedTextForChat(child)),
                        TextCompat.getChatStyle(child)));
                changed = true;
            } else if (underline(child, clickValue)) {
                changed = true;
            }
        }
        return changed;
    }

    // a style writes its codes before the text, so a colour code kept in the text would wipe the underline
    private static String underlineRun(String text) {
        if (text.isEmpty()) return text;
        int i = 0;
        while (i + 1 < text.length() && text.charAt(i) == '\u00a7') i += 2;
        return text.substring(0, i) + "\u00a7n" + text.substring(i);
    }
    //#else
    // modern draws chat from an ordered text, so mark the glyphs instead of rebuilding text
    //$$ public static OrderedText apply(OrderedText text) {
    //$$     Object hoveredVisible = ChatHook.getHoveredVisible();
    //$$     if (hoveredVisible == null || hoveredVisible != ChatHook.getRenderVisible()) return text;
    //$$     String hovered = ChatHook.getHoveredNameClickValue();
    //$$     if (hovered == null) return text;
    //$$     List<OrderedText> parts = new ArrayList<>();
    //$$     text.accept((index, style, codePoint) -> {
    //$$         Style out = style;
    //$$         ClickEvent click = style.getClickEvent();
    //$$         if (click != null && hovered.equals(click.getValue())) out = style.withUnderline(true);
    //$$         parts.add(OrderedText.styled(codePoint, out));
    //$$         return true;
    //$$     });
    //$$     return OrderedText.concat(parts);
    //$$ }
    //#endif
}
