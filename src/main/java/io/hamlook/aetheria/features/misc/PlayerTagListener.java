package io.hamlook.aetheria.features.misc;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.events.ASMChatEvent;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.repo.PlayerTagRepo;
import io.hamlook.aetheria.repo.data.PlayerTagData;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.chat.ChatUtils.ChatTextMatch;
import io.hamlook.aetheria.utils.compat.TextCompat;
import net.minecraft.util.IChatComponent;

@RegisterEvents
public class PlayerTagListener {

    @HandleEvent(priority = HandleEvent.LOWEST, receiveCancelled = true)
    public void onChat(ASMChatEvent event) {
        ChatUtils.Message m = event.parsed();
        if (m == null) return;

        String ign = m.sender;

        PlayerTagData.Entry entry = PlayerTagRepo.getTag(ign);
        if (entry == null) return;

        IChatComponent tagComp = buildTagComponent(entry);
        if (tagComp == null) return;

        event.message = ChatUtils.ensureSiblings(event.message);
        ChatUtils.replaceFirst(event.message, ign, true, 0,
                match -> tagReplacement(match, tagComp));
    }

    private static IChatComponent tagReplacement(ChatTextMatch match, IChatComponent tagComp) {
        IChatComponent name = TextCompat.fromFormatted(
                match.formatAtStart() + match.matchText(), match.baseStyle());
        TextCompat.appendSibling(name, tagComp);
        return name;
    }

    /**
     * Builds the tag component:
     * Inline: colored unicode icon only  (e.g. §9✦)
     * Hover:  text field                 (e.g. §9[Developer])
     */
    private IChatComponent buildTagComponent(PlayerTagData.Entry entry) {
        char sym = entry.resolveSymbol();
        if (sym == 0) return null;

        IChatComponent tagComp = TextCompat.fromFormatted(" §r" + entry.resolveUnicodeColor() + sym + "§r");

        String hoverText = entry.text != null ? entry.text : "";
        if (!hoverText.isEmpty()) {
            TextCompat.setChatStyle(tagComp, TextCompat.setHoverShowText(TextCompat.getChatStyle(tagComp), hoverText));
        }

        return tagComp;
    }
}