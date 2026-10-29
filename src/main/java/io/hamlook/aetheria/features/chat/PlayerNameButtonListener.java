package io.hamlook.aetheria.features.chat;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.features.chat.PlayerButtonsConfig;
import io.hamlook.aetheria.events.ASMChatEvent;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.chat.ChatUtils.ChatTextMatch;
import io.hamlook.aetheria.utils.compat.TextCompat;
import io.hamlook.aetheria.utils.data.SkyblockData;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.IChatComponent;

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
        if (ATHRConfig.feature == null) return;
        PlayerButtonsConfig cfg = ATHRConfig.feature.chat.playerButtons;
        if (!cfg.enabled) return;
        if (cfg.onlyInSkyblock && !SkyblockData.isOnSkyblock()) return;

        ChatUtils.Message m = event.parsed();
        if (m == null || !cfg.allows(m.type)) return;

        event.message = ChatUtils.ensureSiblings(event.message);
        String sender = m.sender;
        ChatUtils.replaceFirst(event.message, sender, true, 0, match -> playerName(match, sender));
    }

    private static IChatComponent playerName(ChatTextMatch match, String ign) {
        IChatComponent name = TextCompat.fromFormatted(
                match.formatAtStart() + match.matchText(), match.baseStyle());
        applyClickableLeaves(name, ign);
        return name;
    }

    private static void applyClickableLeaves(IChatComponent component, String ign) {
        if (TextCompat.getSiblings(component).isEmpty()) {
            ChatStyle style = TextCompat.createDeepCopy(TextCompat.getChatStyle(component));
            style = TextCompat.setClickSuggestCommand(style, MARKER_PREFIX + ign);
            style = TextCompat.setHoverShowText(style, "§eClick for player options");
            TextCompat.setChatStyle(component, style);
            return;
        }
        for (IChatComponent child : TextCompat.getSiblings(component)) {
            applyClickableLeaves(child, ign);
        }
    }
}
