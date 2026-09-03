package io.hamlook.aetheria.features.misc;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.events.ASMChatEvent;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.repo.PlayerTagRepo;
import io.hamlook.aetheria.repo.data.PlayerTagData;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.compat.TextCompat;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.StringUtils;

import java.util.List;

@RegisterEvents
public class PlayerTagListener {

    @HandleEvent(priority = HandleEvent.LOWEST, receiveCancelled = true)
    public void onChat(ASMChatEvent event) {
        String plain = StringUtils.stripControlCodes(TextCompat.getFormattedText(event.message));

        String ign = ChatUtils.getPlayerMessageSender(plain);
        if (ign == null) ign = ChatUtils.getPartySender(plain);
        if (ign == null) ign = ChatUtils.getGuildSender(plain);
        if (ign == null) ign = ChatUtils.getMsgReceivedSender(plain);
        if (ign == null) ign = ChatUtils.getMsgSentRecipient(plain);
        if (ign == null) ign = ChatUtils.getDonateSender(plain);
        if (ign == null) return;

        PlayerTagData.Entry entry = PlayerTagRepo.getTag(ign);
        if (entry == null) return;

        IChatComponent tagComp = buildTagComponent(entry);
        if (tagComp == null) return;

        // Inject into the existing component tree — preserves all ClickEvents
        event.message = ChatUtils.ensureSiblings(event.message);
        injectAfterIgn(event.message, ign, tagComp);
    }

    /**
     * Walks the component tree and inserts tagComp right after the sibling
     * whose unformatted text contains the IGN. Mutates in-place so the
     * original ClickEvents on all other siblings are untouched.
     */
    private boolean injectAfterIgn(IChatComponent root, String ign, IChatComponent tagComp) {
        List<IChatComponent> siblings = TextCompat.getSiblings(root);
        for (int i = 0; i < siblings.size(); i++) {
            IChatComponent sib = siblings.get(i);
            String sibText = sib.getUnformattedTextForChat();

            int idx = ignEndIndex(sibText, ign);
            if (idx != -1) {
                String before = sibText.substring(0, idx);
                String after = sibText.substring(idx);

                IChatComponent beforeComp = TextCompat.createText(before);
                beforeComp.setChatStyle(TextCompat.createDeepCopy(TextCompat.getChatStyle(sib)));

                IChatComponent afterComp = TextCompat.createText(after);
                afterComp.setChatStyle(TextCompat.createDeepCopy(TextCompat.getChatStyle(sib)));

                // Re-attach sib's own children to afterComp
                for (IChatComponent child : TextCompat.getSiblings(sib)) {
                    TextCompat.appendSibling(afterComp, child);
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

    /**
     * Returns the index just after ign inside text (case-insensitive), or -1.
     */
    private int ignEndIndex(String text, String ign) {
        int idx = text.toLowerCase().indexOf(ign.toLowerCase());
        return idx == -1 ? -1 : idx + ign.length();
    }

    /**
     * Builds the tag component:
     * Inline: colored unicode icon only  (e.g. §9✦)
     * Hover:  text field                 (e.g. §9[Developer])
     */
    private IChatComponent buildTagComponent(PlayerTagData.Entry entry) {
        char sym = entry.resolveSymbol();
        if (sym == 0) return null;

        IChatComponent tagComp = TextCompat.createText(" §r" + entry.resolveUnicodeColor() + sym + "§r");

        String hoverText = entry.text != null ? entry.text : "";
        if (!hoverText.isEmpty()) {
            TextCompat.setHoverShowText(TextCompat.getChatStyle(tagComp), hoverText);
        }

        return tagComp;
    }
}