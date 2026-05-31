package io.hamlook.aetheria.utils.chat;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.ClientChatReceivedEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ChatUtils {

    private static final Pattern PARTY_MSG = Pattern.compile("^Party > (?:\\[[^]]*])?\\s*(\\w{1,16}):\\s*(.+)$");
    private static final Pattern PLAYER_MSG = Pattern.compile("^(?:§.\\[[^\\]]*\\] )?§.(\\w{1,16})§.: (.+)§r$");
    private static final Pattern MSG_RECEIVED = Pattern.compile("^§.From (?:§.\\[[^\\]]*\\] )?§.(\\w{1,16}) §.to Me§.: §.(.+)§r$");
    private static final Pattern MSG_SENT = Pattern.compile("^§.From Me §.to (?:§.\\[[^\\]]*\\] )?§.(\\w{1,16})§.: §.(.+)§r$");

    private ChatUtils() {
    }

    public static String clean(ClientChatReceivedEvent event) {
        return StringUtils.stripControlCodes(event.message.getFormattedText()).trim();
    }

    public static boolean isFromServer(ClientChatReceivedEvent event) {
        return event.type == 0 || event.type == 1;
    }

    public static boolean isPartyMessage(String msg) {
        return PARTY_MSG.matcher(msg).matches();
    }

    public static boolean isPlayerMessage(String msg) {
        return PLAYER_MSG.matcher(msg).matches();
    }

    public static boolean isMsgReceived(String msg) {
        return MSG_RECEIVED.matcher(msg).matches();
    }

    public static boolean isMsgSent(String msg) {
        return MSG_SENT.matcher(msg).matches();
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
        Matcher m = PLAYER_MSG.matcher(msg);
        return m.matches() ? m.group(1) : null;
    }

    public static String getMsgReceivedSender(String msg) {
        Matcher m = MSG_RECEIVED.matcher(msg);
        return m.matches() ? m.group(1) : null;
    }

    public static String getMsgSentRecipient(String msg) {
        Matcher m = MSG_SENT.matcher(msg);
        return m.matches() ? m.group(1) : null;
    }

    public static void sendMessage(String message) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText(message));
        }
    }

    public static void sendMultilineMessage(String message) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null) {
            for (String line : message.split("\n")) {
                mc.thePlayer.addChatMessage(new ChatComponentText(line));
            }
        }
    }

    public static void sendChatCommand(String message) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null) {
            mc.thePlayer.sendChatMessage(message);
        }
    }

    public static void sendPartyMessage(String message, long delayMs) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;

        java.util.concurrent.Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            if (mc.thePlayer != null) {
                mc.thePlayer.sendChatMessage("/pc " + message);
            }
        }, delayMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    public static void sendPartyMessage(String message) {
        sendPartyMessage(message, 1500);
    }
}