package io.hamlook.aetheria.features.chat;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.events.ASMChatEvent;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.StringUtils;

import java.util.List;
import java.util.Locale;

/**
 * Turns the sender's name in player / party / guild / whisper chat lines into
 * a clickable span. The click itself does nothing on its own — it just carries
 * a recognizable {@link ClickEvent} marker that {@code mixins/chat/MixinGuiChat}
 * intercepts to open the player-action popup menu instead of letting vanilla
 * handle the click.
 */
@RegisterEvents
public class PlayerNameButtonListener {

    public static final String MARKER_PREFIX = "/athr-player-menu ";

    @HandleEvent(priority = HandleEvent.LOWEST, receiveCancelled = true)
    public void onChat(ASMChatEvent event) {
        if (ATHRConfig.feature == null || !ATHRConfig.feature.chat.playerButtons.enabled) return;

        String plain = StringUtils.stripControlCodes(event.message.getFormattedText());

        String ign = ChatUtils.getPlayerMessageSender(plain);
        if (ign == null) ign = ChatUtils.getPartySender(plain);
        if (ign == null) ign = ChatUtils.getGuildSender(plain);
        if (ign == null) ign = ChatUtils.getMsgReceivedSender(plain);
        if (ign == null) ign = ChatUtils.getMsgSentRecipient(plain);
        if (ign == null) ign = ChatUtils.getDonateSender(plain);
        if (ign == null) return;

        event.message = ChatUtils.ensureSiblings(event.message);
        wrapIgn(event.message, ign);
    }

    /**
     * Walks the component tree and replaces the first occurrence of ign's text
     * with three siblings: the text before it, the name itself (now clickable),
     * and the text after it. Mutates in-place so any existing ClickEvents /
     * HoverEvents on other siblings (or on ign's own trailing children) survive.
     */
    private boolean wrapIgn(IChatComponent root, String ign) {
        List<IChatComponent> siblings = root.getSiblings();
        for (int i = 0; i < siblings.size(); i++) {
            IChatComponent sib = siblings.get(i);
            String sibText = sib.getUnformattedTextForChat();

            int idx = sibText.toLowerCase(Locale.ROOT).indexOf(ign.toLowerCase(Locale.ROOT));
            if (idx != -1) {
                // Every split-off sibling gets an implicit §r appended after its text by
                // getFormattedText(), so any inline colour code sitting in "before" would
                // otherwise be wiped out before "name" ever renders. Re-assert whatever
                // colour/format was active at each cut point explicitly.
                String namePrefix = activeFormatState(sibText, idx);
                String afterPrefix = activeFormatState(sibText, idx + ign.length());

                String before = sibText.substring(0, idx);
                String name = namePrefix + sibText.substring(idx, idx + ign.length());
                String after = afterPrefix + sibText.substring(idx + ign.length());

                ChatStyle baseStyle = sib.getChatStyle();

                ChatComponentText beforeComp = new ChatComponentText(before);
                beforeComp.setChatStyle(baseStyle.createDeepCopy());

                ChatComponentText nameComp = new ChatComponentText(name);
                ChatStyle nameStyle = baseStyle.createDeepCopy();
                nameStyle.setUnderlined(true);
                nameStyle.setChatClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, MARKER_PREFIX + ign));
                nameStyle.setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new ChatComponentText("§eClick for player options")));
                nameComp.setChatStyle(nameStyle);

                ChatComponentText afterComp = new ChatComponentText(after);
                afterComp.setChatStyle(baseStyle.createDeepCopy());
                for (IChatComponent child : sib.getSiblings()) {
                    afterComp.appendSibling(child);
                }

                siblings.set(i, beforeComp);
                siblings.add(i + 1, nameComp);
                siblings.add(i + 2, afterComp);
                return true;
            }

            if (wrapIgn(sib, ign)) return true;
        }
        return false;
    }

    /**
     * Scans {@code text[0, endExclusive)} for legacy §-codes and returns the
     * resolved colour/format state as a literal code prefix (e.g. "§7" or
     * "§7§l"), mirroring vanilla's rule that a colour code resets bold /
     * italic / underline / strikethrough / obfuscated.
     */
    private static String activeFormatState(String text, int endExclusive) {
        Character color = null;
        boolean bold = false, italic = false, underline = false, strikethrough = false, obfuscated = false;

        int limit = Math.min(endExclusive, text.length()) - 1;
        for (int i = 0; i < limit; i++) {
            if (text.charAt(i) != '§') continue;
            char code = Character.toLowerCase(text.charAt(i + 1));
            if (code == 'r') {
                color = null;
                bold = italic = underline = strikethrough = obfuscated = false;
            } else if ("0123456789abcdef".indexOf(code) != -1) {
                color = code;
                bold = italic = underline = strikethrough = obfuscated = false;
            } else if (code == 'l') bold = true;
            else if (code == 'o') italic = true;
            else if (code == 'n') underline = true;
            else if (code == 'm') strikethrough = true;
            else if (code == 'k') obfuscated = true;
            i++;
        }

        StringBuilder sb = new StringBuilder();
        if (color != null) sb.append('§').append(color);
        if (bold) sb.append("§l");
        if (strikethrough) sb.append("§m");
        if (underline) sb.append("§n");
        if (italic) sb.append("§o");
        if (obfuscated) sb.append("§k");
        return sb.toString();
    }
}
