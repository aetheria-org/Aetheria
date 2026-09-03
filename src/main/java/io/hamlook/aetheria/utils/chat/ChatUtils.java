package io.hamlook.aetheria.utils.chat;

import io.hamlook.aetheria.events.ASMChatEvent;
import io.hamlook.aetheria.utils.ColorUtils;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.TextCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.StringUtils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ChatUtils {

    private static final ScheduledExecutorService PARTY_MSG_EXECUTOR = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "ATHR-PartyMsg");
        t.setDaemon(true);
        return t;
    });

    public static final Pattern PLAYER_MSG_STRIPPED = Pattern.compile("^(?:\\[\\d+\\]\\s*)?" + "(?:\\S\\s+)?" + "(?:\\[[^\\]]*\\]\\s*)?" + "(\\w{1,16})" + "[^\\w:]*" + ":\\s*" + "(.+)$");
    private static final Pattern PARTY_MSG = Pattern.compile("^Party > (?:\\[[^]]*])?\\s*(\\w{1,16}):\\s*(.+)$");
    private static final Pattern GUILD_MSG = Pattern.compile("^Guild > (?:\\[[^]]*])?\\s*(\\w{1,16}):\\s*(.+)$");
    private static final Pattern MSG_RECEIVED = Pattern.compile("^§.From (?:§.\\[[^\\]]*\\] )?§.(\\w{1,16}) §.to Me§.: §.(.+)§r$");
    private static final Pattern MSG_SENT = Pattern.compile("^§.From Me §.to (?:§.\\[[^\\]]*\\] )?§.(\\w{1,16})§.: §.(.+)§r$");
    private static final Pattern MSG_RECEIVED_STRIPPED = Pattern.compile("^From (?:\\[[^\\]]*\\] )?(\\w{1,16}) to Me: (.+)$");
    private static final Pattern MSG_SENT_STRIPPED = Pattern.compile("^From Me to (?:\\[[^\\]]*\\] )?(\\w{1,16}): (.+)$");
    private static final Pattern DONATE_MSG = Pattern.compile("^Donate Chat \\(/dc\\) ▶ (?:\\[[^]]*] )?(\\w{1,16}): (.+)$");

    private ChatUtils() {
    }

    public static String clean(ASMChatEvent event) {
        return StringUtils.stripControlCodes(TextCompat.getFormattedText(event.message)).trim();
    }

    public static boolean isChatOpen() {
        Minecraft mc = MinecraftCompat.getMinecraft();
        return mc != null && mc.currentScreen instanceof GuiChat;
    }

    public static boolean isFromServer(ASMChatEvent event) {
        return event.type == 0 || event.type == 1;
    }

    public static boolean isPartyMessage(String msg) {
        return PARTY_MSG.matcher(msg).matches();
    }

    public static boolean isPlayerMessage(String msg) {
        if (isNpcDialogue(msg)) return false;
        return PLAYER_MSG_STRIPPED.matcher(msg).matches();
    }

    public static boolean isPlayerChat(String msg) {
        return isPartyMessage(msg) || isPlayerMessage(msg) || isMsgReceived(msg) || isMsgSent(msg) || isDonateMessage(msg);
    }

    public static boolean isMsgReceived(String msg) {
        return MSG_RECEIVED.matcher(msg).matches() || MSG_RECEIVED_STRIPPED.matcher(msg).matches();
    }

    public static boolean isMsgSent(String msg) {
        return MSG_SENT.matcher(msg).matches() || MSG_SENT_STRIPPED.matcher(msg).matches();
    }

    public static String getPartyBody(String msg) {
        Matcher m = PARTY_MSG.matcher(msg);
        return m.matches() ? m.group(2).trim() : null;
    }

    public static String getPartySender(String msg) {
        Matcher m = PARTY_MSG.matcher(msg);
        return m.matches() ? m.group(1) : null;
    }

    public static String getPlayerMessageSender(String msg) {
        if (isNpcDialogue(msg)) return null;
        Matcher m = PLAYER_MSG_STRIPPED.matcher(msg);
        return m.matches() ? m.group(1) : null;
    }

    public static String getPlayerMessageBody(String msg) {
        if (isNpcDialogue(msg)) return null;
        Matcher m = PLAYER_MSG_STRIPPED.matcher(msg);
        return m.matches() ? m.group(2).trim() : null;
    }

    /**
     * SkyBlock NPC dialogue ("[NPC] Trevor: ...") otherwise matches
     * {@link #PLAYER_MSG_STRIPPED}, which made every player-chat filter swallow
     * NPC lines and forced NPC-sensitive features to check above the filters.
     */
    private static boolean isNpcDialogue(String msg) {
        return msg != null && ColorUtils.stripColor(msg).trim().startsWith("[NPC] ");
    }

    public static String getGuildSender(String msg) {
        Matcher m = GUILD_MSG.matcher(msg);
        return m.matches() ? m.group(1) : null;
    }

    public static String getGuildBody(String msg) {
        Matcher m = GUILD_MSG.matcher(msg);
        return m.matches() ? m.group(2).trim() : null;
    }

    public static String getMsgReceivedBody(String msg) {
        Matcher m = MSG_RECEIVED_STRIPPED.matcher(msg);
        return m.matches() ? m.group(2).trim() : null;
    }

    public static String getMsgSentBody(String msg) {
        Matcher m = MSG_SENT_STRIPPED.matcher(msg);
        return m.matches() ? m.group(2).trim() : null;
    }

    public static String getMsgReceivedSender(String msg) {
        Matcher m = MSG_RECEIVED_STRIPPED.matcher(msg);
        return m.matches() ? m.group(1) : null;
    }

    public static String getMsgSentRecipient(String msg) {
        Matcher m = MSG_SENT_STRIPPED.matcher(msg);
        return m.matches() ? m.group(1) : null;
    }

    public static boolean isDonateMessage(String msg) {
        return DONATE_MSG.matcher(msg).matches();
    }

    public static String getDonateSender(String msg) {
        Matcher m = DONATE_MSG.matcher(msg);
        return m.matches() ? m.group(1) : null;
    }

    public static String getDonateBody(String msg) {
        Matcher m = DONATE_MSG.matcher(msg);
        return m.matches() ? m.group(2).trim() : null;
    }

    public static IChatComponent ensureSiblings(IChatComponent component) {
        if (TextCompat.getSiblings(component).isEmpty()) {
            IChatComponent root = TextCompat.createText("");
            IChatComponent child = TextCompat.createText(component.getUnformattedTextForChat());
            child.setChatStyle(TextCompat.createDeepCopy(TextCompat.getChatStyle(component)));
            TextCompat.appendSibling(root, child);
            return root;
        }
        return component;
    }

    public static void sendMessage(String message) {
        Minecraft mc = MinecraftCompat.getMinecraft();
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(TextCompat.createText(message));
        }
    }

    public static void sendMultilineMessage(String message) {
        Minecraft mc = MinecraftCompat.getMinecraft();
        if (mc.thePlayer != null) {
            for (String line : message.split("\n")) {
                mc.thePlayer.addChatMessage(TextCompat.createText(line));
            }
        }
    }

    public static void sendChatCommand(String message) {
        Minecraft mc = MinecraftCompat.getMinecraft();
        if (mc.thePlayer != null) {
            mc.thePlayer.sendChatMessage(message);
        }
    }

    public static void sendPartyMessage(String message, long delayMs) {
        Minecraft mc = MinecraftCompat.getMinecraft();
        if (mc.thePlayer == null) return;

        PARTY_MSG_EXECUTOR.schedule(() -> {
            if (mc.thePlayer != null) {
                mc.thePlayer.sendChatMessage("/pc " + message);
            }
        }, delayMs, TimeUnit.MILLISECONDS);
    }

    public static void sendPartyMessage(String message) {
        sendPartyMessage(message, 1500);
    }

    public static void sendDonateMessage(String message) {
        sendChatCommand("/dc " + message);
    }
}
