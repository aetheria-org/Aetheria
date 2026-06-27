package io.hamlook.aetheria.features.chat;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.features.chat.ChatPingConfig;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.SoundUtils;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.List;

@RegisterEvents
public class ChatPingListener {

    private static final String[] SOUNDS = {"note.pling", "random.orb", "random.levelup", "mob.endermen.portal", "random.pop"};

    private static final EnumChatFormatting[] COLORS = {null, EnumChatFormatting.YELLOW, EnumChatFormatting.RED, EnumChatFormatting.AQUA, EnumChatFormatting.GREEN, EnumChatFormatting.LIGHT_PURPLE, EnumChatFormatting.GOLD, EnumChatFormatting.WHITE};

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onChat(ClientChatReceivedEvent event) {
        if (ATHRConfig.feature == null) return;
        ChatPingConfig cfg = ATHRConfig.feature.chat.chatPingConfig;
        if (!cfg.chatPing) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;

        String name = mc.thePlayer.getName().toLowerCase();
        String stripped = StringUtils.stripControlCodes(event.message.getFormattedText());

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
        if (color != null) {
            event.message = ChatUtils.ensureSiblings(event.message);
            highlightName(event.message, name, color);
        }
    }

    private static String getLastFormatCode(String text) {
        for (int i = text.length() - 2; i >= 0; i--) {
            if (text.charAt(i) == '§') {
                return text.substring(i, i + 2);
            }
        }
        return "";
    }

    private void highlightName(IChatComponent component, String name, EnumChatFormatting color) {
        highlightName(component, name, color, new boolean[]{false});
    }

    private void highlightName(IChatComponent component, String name, EnumChatFormatting color, boolean[] pastColon) {
        List<IChatComponent> siblings = component.getSiblings();
        int nameLen = name.length();
        for (int i = 0; i < siblings.size(); i++) {
            IChatComponent sib = siblings.get(i);
            String text = sib.getUnformattedTextForChat();
            String lower = text.toLowerCase();

            int colonPos = lower.indexOf(':');
            int namePos = lower.indexOf(name);

            if (namePos == -1) {
                if (colonPos != -1) pastColon[0] = true;
                highlightName(sib, name, color, pastColon);
                continue;
            }

            boolean colonBeforeName = colonPos != -1 && colonPos < namePos;

            if (!pastColon[0] && !colonBeforeName) {
                if (colonPos != -1 && namePos < colonPos) {
                    int after = lower.indexOf(name, colonPos + 1);
                    if (after != -1) {
                        namePos = after;
                        pastColon[0] = true;
                    } else {
                        pastColon[0] = true;
                        highlightName(sib, name, color, pastColon);
                        continue;
                    }
                } else {
                    if (colonPos != -1) pastColon[0] = true;
                    highlightName(sib, name, color, pastColon);
                    continue;
                }
            }

            if (colonPos != -1) pastColon[0] = true;

            String before = text.substring(0, namePos);
            String match = text.substring(namePos, namePos + nameLen);
            String after = text.substring(namePos + nameLen);

            if (!before.isEmpty()) {
                ChatComponentText beforeComp = new ChatComponentText(before);
                beforeComp.setChatStyle(sib.getChatStyle().createDeepCopy());
                siblings.set(i, beforeComp);
                i++;
            }

            ChatComponentText nameComp = new ChatComponentText(match);
            nameComp.getChatStyle().setColor(color);
            if (before.isEmpty()) {
                siblings.set(i, nameComp);
            } else {
                siblings.add(i, nameComp);
            }
            i++;

            if (!after.isEmpty()) {
                if (!after.startsWith("§")) {
                    after = getLastFormatCode(before) + after;
                }
                ChatComponentText afterComp = new ChatComponentText(after);
                afterComp.setChatStyle(sib.getChatStyle().createDeepCopy());
                for (IChatComponent child : sib.getSiblings()) {
                    afterComp.appendSibling(child);
                }
                siblings.add(i, afterComp);
                i--;
            }
        }
    }
}
