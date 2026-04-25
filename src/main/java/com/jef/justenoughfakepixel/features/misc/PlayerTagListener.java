package com.jef.justenoughfakepixel.features.misc;

import com.jef.justenoughfakepixel.init.RegisterEvents;
import com.jef.justenoughfakepixel.repo.PlayerTagRepo;
import com.jef.justenoughfakepixel.repo.data.PlayerTagData;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RegisterEvents
public class PlayerTagListener {

    private static final Pattern CHAT_PATTERN = Pattern.compile(
            "^(.*\\s)([A-Za-z0-9_]{1,16})[^A-Za-z0-9_:]*\\s*:\\s*(.+)$"
    );

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onChat(ClientChatReceivedEvent event) {
        String plain = StringUtils.stripControlCodes(event.message.getFormattedText());

        Matcher m = CHAT_PATTERN.matcher(plain);
        if (!m.matches()) return;

        String ign = m.group(2);
        PlayerTagData.Entry entry = PlayerTagRepo.getTag(ign);
        if (entry == null) return;

        IChatComponent tagComp = buildTagComponent(entry);
        if (tagComp == null) return;

        // Inject into the existing component tree — preserves all ClickEvents
        injectAfterIgn(event.message, ign, tagComp);
    }

    /**
     * Walks the component tree and inserts tagComp right after the sibling
     * whose unformatted text contains the IGN. Mutates in-place so the
     * original ClickEvents on all other siblings are untouched.
     */
    private boolean injectAfterIgn(IChatComponent root, String ign, IChatComponent tagComp) {
        List<IChatComponent> siblings = root.getSiblings();
        for (int i = 0; i < siblings.size(); i++) {
            IChatComponent sib = siblings.get(i);
            String sibText = sib.getUnformattedTextForChat();

            int idx = ignEndIndex(sibText, ign);
            if (idx != -1) {
                String before = sibText.substring(0, idx);
                String after  = sibText.substring(idx);

                ChatComponentText beforeComp = new ChatComponentText(before);
                beforeComp.setChatStyle(sib.getChatStyle().createDeepCopy());

                ChatComponentText afterComp = new ChatComponentText(after);
                afterComp.setChatStyle(sib.getChatStyle().createDeepCopy());

                // Re-attach sib's own children to afterComp
                for (IChatComponent child : sib.getSiblings()) {
                    afterComp.appendSibling(child);
                }

                siblings.set(i, beforeComp);
                siblings.add(i + 1, tagComp);
                siblings.add(i + 2, afterComp);
                return true;
            }

            if (injectAfterIgn(sib, ign, tagComp)) return true;
        }
        return false;
    }

    /** Returns the index just after ign inside text (case-insensitive), or -1. */
    private int ignEndIndex(String text, String ign) {
        int idx = text.toLowerCase().indexOf(ign.toLowerCase());
        return idx == -1 ? -1 : idx + ign.length();
    }

    /**
     * Builds the tag component:
     *   Inline: colored unicode icon only  (e.g. §9✦)
     *   Hover:  text field                 (e.g. §9[Developer])
     */
    private IChatComponent buildTagComponent(PlayerTagData.Entry entry) {
        char sym = entry.resolveSymbol();
        if (sym == 0) return null;

        ChatComponentText tagComp = new ChatComponentText(
                " §r" + entry.resolveUnicodeColor() + sym + "§r"
        );

        String hoverText = entry.text != null ? entry.text : "";
        if (!hoverText.isEmpty()) {
            tagComp.getChatStyle().setChatHoverEvent(
                    new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            new ChatComponentText(hoverText)
                    )
            );
        }

        return tagComp;
    }
}