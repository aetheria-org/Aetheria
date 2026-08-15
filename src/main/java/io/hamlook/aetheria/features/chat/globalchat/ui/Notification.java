package io.hamlook.aetheria.features.chat.globalchat.ui;

import io.hamlook.aetheria.features.chat.globalchat.GlobalChat;
import io.hamlook.aetheria.features.chat.globalchat.vars.Channel;
import io.hamlook.aetheria.features.chat.globalchat.vars.ChatMessage;
import io.hamlook.aetheria.utils.chat.ExpiringArrayList;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class Notification implements ExpiringArrayList.Trackable {

    public Long startTime;
    public Long timeFrame;
    public String header;
    public String message;

    public static Notification createPunishmentNotif(String msg) {
        String header = "";
        String message = msg.toLowerCase();
        if(message.contains("muted")){
            if(message.contains("unmuted")){
                header = "Unmuted";
            }else {
                header = "Mute";
            }
        }else if(message.contains("banned")){
            if(message.contains("unbanned")){
                header = "Unbanned";
            }else {
                header = "Banned";
            }
        }
        return new Notification(System.currentTimeMillis(),5000L,header,msg);
    }

    /**
     * Builds a toast notification for an incoming G-Chat message, or null when
     * it should not toast: own messages never ping, @everyone only pings when
     * the server flagged {@code pingEveryone}, @here only when it flagged
     * {@code pingHere} (the server only sets these for senders holding the
     * canPingEveryone permission), individual mentions come from the
     * server-provided {@code mentions} list with a literal {@code @username}
     * fallback, and replies to the local user toast as well.
     */
    public static Notification createFromMessage(ChatMessage message, Channel channel) {
        if (message == null || message.content == null) return null;
        String ownName = GlobalChat.getUsername();
        if (ownName == null || ownName.isEmpty()) return null;
        if (!message.pingsMe(ownName, channel)) return null;

        String header;
        if (message.pingEveryone) {
            header = "@everyone";
        } else if (message.pingHere) {
            header = "@here";
        } else if (message.isReplyTo(ownName, channel)) {
            header = "Reply";
        } else {
            header = "Mention";
        }
        return new Notification(System.currentTimeMillis(), 5000L, header, preview(message));
    }

    /** Plain text toast (e.g. the offline "Unread Messages" summary on login). */
    public static Notification createTextNotif(String header, String text, long timeFrame) {
        return new Notification(System.currentTimeMillis(), timeFrame, header, text);
    }

    private static String preview(ChatMessage message) {
        Channel channel = GlobalChat.channels.get(message.channelId);
        String channelName = channel != null && channel.channelName != null ? channel.channelName : "unknown";
        String author = message.authorDisplay != null && !message.authorDisplay.isEmpty()
                ? message.authorDisplay : message.author;
        String text = message.content.replaceAll("\\s+", " ").trim();
        if (text.length() > 60) text = text.substring(0, 60).trim() + "…";
        return "#" + channelName + " · " + author + ": " + text;
    }


    @Override
    public boolean isExpired() {
        return (System.currentTimeMillis() - startTime) > timeFrame;
    }
}
