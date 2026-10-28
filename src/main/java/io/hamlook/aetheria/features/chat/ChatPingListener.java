package io.hamlook.aetheria.features.chat;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.features.chat.ChatPingConfig;
import io.hamlook.aetheria.events.ASMChatEvent;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.SoundUtils;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.chat.ChatUtils.ChatTextMatch;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.TextCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.StringUtils;

@RegisterEvents
public class ChatPingListener {

    private static final String[] SOUNDS = {"note.pling", "random.orb", "random.levelup", "mob.endermen.portal", "random.pop"};

    private static final EnumChatFormatting[] COLORS = {null, EnumChatFormatting.YELLOW, EnumChatFormatting.RED, EnumChatFormatting.AQUA, EnumChatFormatting.GREEN, EnumChatFormatting.LIGHT_PURPLE, EnumChatFormatting.GOLD, EnumChatFormatting.WHITE};

    @HandleEvent(priority = HandleEvent.LOWEST, receiveCancelled = true)
    public void onChat(ASMChatEvent event) {
        if (ATHRConfig.feature == null) return;
        ChatPingConfig cfg = ATHRConfig.feature.chat.chatPingConfig;
        if (!cfg.chatPing) return;

        Minecraft mc = MinecraftCompat.getMinecraft();
        if (MinecraftCompat.getLocalPlayer() == null) return;

        String name = TextCompat.getUnformattedText(MinecraftCompat.getLocalPlayer().getName()).toLowerCase();
        String stripped = StringUtils.stripControlCodes(TextCompat.getFormattedTextWithResets(event.message));

        int colonIdx = stripped.indexOf(':');
        if (colonIdx != -1) {
            String beforeColon = stripped.substring(0, colonIdx);
            if (beforeColon.toLowerCase().contains(name + "'s parrot")) {
                return;
            }
        }

        String body = ChatUtils.getPlayerMessageBody(stripped);

        if (body == null && cfg.chatPingAllMessages) {
            body = ChatUtils.getPartyBody(stripped);
        }
        if (body == null && cfg.chatPingAllMessages) {
            body = ChatUtils.getGuildBody(stripped);
        }
        if (body == null && cfg.chatPingAllMessages) {
            body = ChatUtils.getMsgReceivedBody(stripped);
        }
        if (body == null && cfg.chatPingAllMessages) {
            body = ChatUtils.getMsgSentBody(stripped);
        }
        if (body == null && cfg.chatPingAllMessages) {
            body = ChatUtils.getDonateBody(stripped);
        }

        if (body == null || !body.toLowerCase().contains(name)) return;

        SoundUtils.playSound(SOUNDS[cfg.chatPingSound], cfg.chatPingVolume, cfg.chatPingPitch);

        EnumChatFormatting color = COLORS[cfg.chatPingColor];
        if (color == null) return;

        String flat = ChatUtils.flattenText(event.message);
        int colon = flat.indexOf(':');
        if (colon < 0) return;

        event.message = ChatUtils.ensureSiblings(event.message);
        ChatUtils.replaceFirst(event.message, name, true, colon + 1,
                match -> pingName(match, color));
    }

    private static IChatComponent pingName(ChatTextMatch match, EnumChatFormatting color) {
        IChatComponent name =
                //#if MC < 1.16
                TextCompat.fromFormatted(match.matchText(), match.baseStyle())
                //#else
                //$$ TextCompat.fromFormatted(match.formatAtStart() + match.matchText(), match.baseStyle())
                //#endif
                ;
        applyColorLeaves(name, color);
        return name;
    }

    private static void applyColorLeaves(IChatComponent component, EnumChatFormatting color) {
        if (TextCompat.getSiblings(component).isEmpty()) {
            TextCompat.setChatStyle(component,
                    TextCompat.setColor(TextCompat.createDeepCopy(TextCompat.getChatStyle(component)), color));
            return;
        }
        for (IChatComponent child : TextCompat.getSiblings(component)) {
            applyColorLeaves(child, color);
        }
    }
}
