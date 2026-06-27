package io.hamlook.aetheria.features.misc;

import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.repo.PlayerTagRepo;
import io.hamlook.aetheria.repo.data.PlayerTagData;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.List;

@RegisterEvents
public class PlayerTagListener {

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onChat(ClientChatReceivedEvent event) {
        String plain = StringUtils.stripControlCodes(event.message.getFormattedText());

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
        List<IChatComponent> siblings = root.getSiblings();
        for (int i = 0; i < siblings.size(); i++) {
            IChatComponent sib = siblings.get(i);
            String sibText = sib.getUnformattedTextForChat();

            int idx = ignEndIndex(sibText, ign);
            if (idx != -1) {
                String before = sibText.substring(0, idx);
                String after = sibText.substring(idx);

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

        ChatComponentText tagComp = new ChatComponentText(" §r" + entry.resolveUnicodeColor() + sym + "§r");

        String hoverText = entry.text != null ? entry.text : "";
        if (!hoverText.isEmpty()) {
            tagComp.getChatStyle().setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(hoverText)));
        }

        return tagComp;
    }
}