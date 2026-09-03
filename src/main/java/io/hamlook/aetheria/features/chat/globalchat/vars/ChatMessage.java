package io.hamlook.aetheria.features.chat.globalchat.vars;

import io.hamlook.aetheria.features.chat.globalchat.GlobalChat;
import io.hamlook.aetheria.features.chat.globalchat.util.MCChatFormatter;
import io.hamlook.aetheria.repo.CapeAPI;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatMessage {

    public String messageID,discordID;
    public String channelId,author,authorDisplay,replyingMessage,avatar;
    public String content;
    /** "discord" or "minecraft" — which client sent this message. */
    public String client;
    /** Server-set: the sender was allowed to (and did) ping @everyone. Never true otherwise. */
    public boolean pingEveryone;
    /** Server-set: the sender was allowed to (and did) ping @here. Never true otherwise. */
    public boolean pingHere;
    /** Server-provided list of mentioned usernames (lower-case); null when the server didn't send them. */
    public List<String> mentions;
    /** Server-provided @token -> display name for mention pills; null/absent on old messages. */
    public HashMap<String, String> mentionDisplays;
    /** Server-set: display name of the author of the message this one replies to, when resolvable. */
    public String replyingAuthor;
    /** Server-set: lower-case MC identity of the message this one replies to, when the target is an MC user. */
    public String replyingTo;
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

    /** Timestamp render caches (transient — never serialized to the server). */
    public transient long tsDayNum = Long.MIN_VALUE;
    public transient long tsHeaderKey = Long.MIN_VALUE;
    public transient String tsHeaderText;
    public transient String tsTimeText;
    public transient String tsDateText;
    /** Computed at receive time: this message pings the local user (mention, @everyone/@here, or a reply to them). */
    public transient boolean highlighted;
    public transient volatile boolean sendFailed;

    public ChatMessage(String content,String channelID,ChatMessage repliedMessage) {
        this.content = content;
        this.channelId = channelID;
        this.replying = repliedMessage != null;
        this.replyingMessage = replying ? repliedMessage.discordID : null;
        this.replyingTo = replying ? repliedMessage.author : null;
        this.discordID = null;
        this.author = MinecraftCompat.getMinecraft().getSession().getUsername();
        // Display name keeps the original capitalization (the server lowercases "author" for identity).
        this.authorDisplay = this.author;
        this.client = "minecraft";
        this.avatar = CapeAPI.getAPIUrl("avatar") + "/" + author + ".png";
        this.messageID = author + "-" + System.nanoTime() + "-" + channelID;
        this.timestamp = System.currentTimeMillis();
        emojiRefs = new HashMap<>();
        stickers = new HashMap<>();
        attachments = new ArrayList<>();
        embeds = new ArrayList<>();
    }

    public ChatMessage addEmojiRefs(HashMap<String,EmojiRef> emojiRefs) {
        this.emojiRefs.putAll(emojiRefs);
        return this;
    }

    private static final Pattern EMOJI_TOKEN = Pattern.compile(":([a-zA-Z0-9_~+-]+):");

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

    public boolean sendMessage() {
        return GlobalChat.sendMessage(this);
    }

    public List<ChatLine> getLines() {
        return Collections.singletonList(new ChatLine(this));
    }

    /** True when this message replies to the given (lower-case) username, per the server-resolved target. */
    public boolean isReplyTo(String ownName) {
        if (ownName == null || ownName.isEmpty()) return false;
        if (author != null && author.toLowerCase().equals(ownName)) return false;
        if (replyingTo != null && !replyingTo.isEmpty()) {
            return replyingTo.equalsIgnoreCase(ownName);
        }
        return replyingAuthor != null && !replyingAuthor.isEmpty() && replyingAuthor.equalsIgnoreCase(ownName);
    }

    /** Reply check with a local fallback for messages lacking a server-side {@code replyingAuthor} (e.g. old history). */
    public boolean isReplyTo(String ownName, Channel channel) {
        if (isReplyTo(ownName)) return true;
        if (ownName == null || ownName.isEmpty() || !replying || replyingMessage == null || replyingMessage.isEmpty()) return false;
        if (author != null && author.toLowerCase().equals(ownName)) return false;
        if (channel == null) return false;
        ChatMessage original = channel.byDiscordID.get(replyingMessage);
        return original != null && original.author != null && original.author.toLowerCase().equals(ownName);
    }

    /** True when this message pings the local user: a @mention, a (server-gated) @everyone/@here, or a reply to them. */
    public boolean pingsMe(String ownName, Channel channel) {
        if (ownName == null || ownName.isEmpty()) return false;
        if (author != null && author.toLowerCase().equals(ownName)) return false;
        if (pingEveryone || pingHere) return true;
        if (mentions != null) {
            for (String name : mentions) {
                if (name != null && name.equalsIgnoreCase(ownName)) return true;
            }
        }
        if (content != null) {
            Pattern own = Pattern.compile("(?i)(?<![\\w])@" + Pattern.quote(ownName) + "\\b");
            if (own.matcher(content).find()) return true;
        }
        return isReplyTo(ownName, channel);
    }

    public String getMCChatMessage() {
        return MCChatFormatter.format(this);
    }
}