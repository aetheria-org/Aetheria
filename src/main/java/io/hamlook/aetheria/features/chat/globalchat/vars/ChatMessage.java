package io.hamlook.aetheria.features.chat.globalchat.vars;

import io.hamlook.aetheria.features.chat.globalchat.GlobalChat;
import io.hamlook.aetheria.repo.CapeAPI;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatMessage {

    public String messageID,discordID;
    public String channelId,author,replyingMessage,avatar;
    public String content;
    /** "discord" or "minecraft" — which client sent this message. */
    public String client;
    /** True once the message has been edited after being sent. */
    public boolean edited;
    public boolean replying;
    public HashMap<String,EmojiRef> emojiRefs;
    public HashMap<String,Sticker> stickers;
    public long timestamp;
    public List<Attachment> attachments;
    public List<Embed> embeds;
    /** Bumped whenever this message's content changes; layout caches key off it. */
    public transient int contentVersion;

    public ChatMessage(String content,String channelID,ChatMessage repliedMessage) {
        this.content = content;
        this.channelId = channelID;
        this.replying = repliedMessage != null;
        this.replyingMessage = replying ? repliedMessage.discordID : null;
        this.discordID = null;
        this.author = Minecraft.getMinecraft().getSession().getUsername();
        this.client = "minecraft";
        this.avatar = CapeAPI.getAPIUrl("avatar") + "/" + author + ".png";
        this.messageID = author + "-" + System.nanoTime() + "-" + channelID;
        this.timestamp = System.currentTimeMillis();
        emojiRefs = new HashMap<>();
        stickers = new HashMap<>();
        attachments = new ArrayList<>();
    }

    public ChatMessage addEmojiRefs(HashMap<String,EmojiRef> emojiRefs) {
        this.emojiRefs.putAll(emojiRefs);
        return this;
    }

    private static final Pattern EMOJI_TOKEN = Pattern.compile(":([a-zA-Z0-9_~]+):");

    /** Scans the raw text for ":shortcode:" tokens and maps them to emoji refs, skipping escaped, fenced-code and inline-code tokens. */
    public void populateEmojiRefs(String rawText) {
        if (rawText == null) return;
        boolean inCode = false;
        String[] lines = rawText.split("\n", -1);
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("```")) {
                inCode = !inCode;
                continue;
            }
            if (inCode) continue;
            boolean inlineCode = false;
            int last = 0;
            Matcher matcher = EMOJI_TOKEN.matcher(line);
            while (matcher.find()) {
                for (int k = last; k < matcher.start(); k++) {
                    if (line.charAt(k) == '`') inlineCode = !inlineCode;
                }
                last = matcher.end();
                if (inlineCode) continue;
                if (matcher.start() > 0 && line.charAt(matcher.start() - 1) == '\\') continue;
                String name = matcher.group(1);
                if (emojiRefs.containsKey(name)) continue;
                IEmoji emoji = GlobalChat.usableEmojis.get(name);
                if (emoji != null) emojiRefs.put(name, emoji.toEmoji());
            }
        }
    }
    public ChatMessage addStickers(HashMap<String,Sticker> stickers) {
        this.stickers.putAll(stickers);
        return this;
    }
    public ChatMessage addAttachments(List<Attachment> attachments) {
        this.attachments.addAll(attachments);
        return this;
    }

    public void sendMessage() {
        GlobalChat.sendMessage(this);
    }

    public List<ChatLine> getLines() {
        return Collections.singletonList(new ChatLine(this));
    }
}