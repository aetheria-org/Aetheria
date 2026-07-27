package io.hamlook.aetheria.features.chat.globalchat.ui.util;

import io.hamlook.aetheria.features.chat.globalchat.GlobalChat;
import io.hamlook.aetheria.features.chat.globalchat.image.GCImage;
import io.hamlook.aetheria.features.chat.globalchat.image.ImageManager;
import io.hamlook.aetheria.features.chat.globalchat.vars.Attachment;
import io.hamlook.aetheria.features.chat.globalchat.vars.GEmoji;
import io.hamlook.aetheria.features.chat.globalchat.vars.Message;
import io.hamlook.aetheria.features.chat.globalchat.vars.MessageData;
import io.hamlook.aetheria.features.chat.globalchat.vars.Reaction;
import io.hamlook.aetheria.features.chat.globalchat.vars.Sticker;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatHelper {

    public static HashMap<String, String> imageCache = new HashMap<>();
    private static final Pattern EMOJI_PATTERN = Pattern.compile(":([a-zA-Z0-9_]{2,}):");

    private static final Pattern URL_PATTERN = Pattern.compile(
            "https?://[\\w./?=&%\\-#]+", Pattern.CASE_INSENSITIVE);

    private static final List<String> EMBEDDABLE_PAGE_DOMAINS = Arrays.asList("tenor.com", "giphy.com");

    public static ChatLine getContent(Message message, long timestamp){
        ChatLine chatLine = new ChatLine();
        chatLine.player = message.player;
        chatLine.timestamp = getTimeStamp(timestamp);
        chatLine.skin = getSkinGC(message.skin, message.player);
        chatLine.messageID = message.messageID;

        if (message.replyTo != null) {
            chatLine.replyTo = message.replyTo;
            chatLine.replyAuthor = message.replyAuthor;
            chatLine.replyPreview = getReplyPreview(message.replyTo);
        }

        if (message.reactions != null) {
            chatLine.reactions.putAll(message.reactions);
        }

        Message.Content msg = message.message;
        chatLine.rawContent = msg.content;

        if (msg.stickers != null && !msg.stickers.isEmpty()) {
            for (Map.Entry<String, Sticker> sticker : msg.stickers.entrySet()) {
                GCImage image = getStickerGC(sticker.getValue(), sticker.getKey());
                if (image == null) continue;
                chatLine.objects.add(new ChatObject(image, null, ChatObject.ObjectType.STICKER));
            }
        }

        if (msg.attachments != null && !msg.attachments.isEmpty()) {
            for (Map.Entry<String, Attachment> attachment : msg.attachments.entrySet()) {
                Attachment att = attachment.getValue();
                if (att == null || !GCImage.looksLikeImageUrl(att.url)) continue;
                GCImage image = getAttachmentGC(att, attachment.getKey());
                if (image == null) continue;
                chatLine.objects.add(new ChatObject(image, null, ChatObject.ObjectType.ATTACHMENT));
            }
        }

        if (msg.content != null && !msg.content.isEmpty()) {
            String content = msg.content;
            List<ResolvedEmbed> resolvedEmbeds = extractLinkEmbedsWithUrls(content);
            for (ResolvedEmbed re : resolvedEmbeds) {
                content = content.replace(re.url, "");
            }
            content = content.replaceAll("\\s+", " ").trim();
            chatLine.objects.addAll(replaceEmojiRefs(content, msg.emojiRefs));
            for (ResolvedEmbed re : resolvedEmbeds) {
                chatLine.objects.add(re.embed);
            }
        }

        return chatLine;
    }

    private static class ResolvedEmbed {
        ChatObject embed;
        String url;
    }

    private static List<ResolvedEmbed> extractLinkEmbedsWithUrls(String content) {
        List<ResolvedEmbed> results = new ArrayList<>();
        Matcher matcher = URL_PATTERN.matcher(content);
        while (matcher.find()) {
            String rawUrl = matcher.group();
            GCImage image = resolveEmbedImage(rawUrl);
            if (image != null) {
                ResolvedEmbed re = new ResolvedEmbed();
                re.embed = new ChatObject(image, null, ChatObject.ObjectType.EMBED, labelForUrl(rawUrl));
                re.url = rawUrl;
                results.add(re);
            }
        }
        return results;
    }

    private static String getTimeStamp(long timestamp) {
        ZonedDateTime targetTime = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault());
        LocalDate targetDate = targetTime.toLocalDate();
        LocalDate today = LocalDate.now(ZoneId.systemDefault());

        if (targetDate.equals(today)) {
            return targetTime.format(DateTimeFormatter.ofPattern("h:mm a"));
        } else {
            return targetTime.format(DateTimeFormatter.ofPattern("M/d/yy, h:mm a"));
        }
    }

    public static GCImage getSkinGC(String skinUrl, String player){
        String cacheKey = "skin_" + player;
        if(imageCache.containsKey(cacheKey)){
            GCImage img = ImageManager.images.get(imageCache.get(cacheKey));
            if(img == null){ imageCache.remove(cacheKey); } else { return img; }
        }
        GCImage image = ImageManager.images.get(GCImage.createGCImage(skinUrl,true));
        if(image != null){ imageCache.put(cacheKey, image.id); }
        return image;
    }

    public static GCImage getAttachmentGC(Attachment attachment, String id){
        if(imageCache.containsKey(id)){
            GCImage img = ImageManager.images.get(imageCache.get(id));
            if(img == null){ imageCache.remove(id); } else { return img; }
        }
        GCImage image = ImageManager.images.get(GCImage.createGCImage(attachment.url));
        if(image != null){ imageCache.put(id, image.id); }
        return image;
    }

    private static List<ChatObject> replaceEmojiRefs(String content, HashMap<String, GEmoji> emojiRefs) {
        if(emojiRefs == null || emojiRefs.isEmpty()) return Collections.singletonList(new ChatObject(null, content));
        List<ChatObject> chat = new ArrayList<>();
        Matcher matcher = EMOJI_PATTERN.matcher(content);
        if(!matcher.find()) {
            chat.add(new ChatObject(null, content));
            return chat;
        }
        matcher.reset();
        int lastEnd = 0;
        while(matcher.find()) {
            int start = matcher.start();
            if(start != lastEnd){
                chat.add(new ChatObject(null, content.substring(lastEnd, start)));
            }
            String key = matcher.group(1);
            GEmoji emoji = emojiRefs.get(key);
            GCImage img = (emoji != null) ? getEmojiGC(emoji) : null;
            if(img != null){
                chat.add(new ChatObject(img, null, ChatObject.ObjectType.EMOJI));
            } else {
                chat.add(new ChatObject(null, matcher.group()));
            }
            lastEnd = matcher.end();
        }
        if (lastEnd < content.length()) {
            chat.add(new ChatObject(null, content.substring(lastEnd)));
        }
        return chat;
    }

    public static GCImage getEmojiGC(GEmoji emoji){
        if(imageCache.containsKey(emoji.id)){
            GCImage img = ImageManager.images.get(imageCache.get(emoji.id));
            if(img == null){ imageCache.remove(emoji.id); } else { return img; }
        }
        GCImage image = ImageManager.images.get(GCImage.createGCImage(createEmojiURL(emoji.id,emoji.animated)));
        if(image != null){ imageCache.put(emoji.id,image.id); }
        return image;
    }

    public static String createEmojiURL(String id,boolean animated){
        return "https://cdn.discordapp.com/emojis/" + id + (animated ? ".gif" : ".png");
    }

    public static GCImage getStickerGC(Sticker sticker,String id){
        if(imageCache.containsKey(id)){
            GCImage img = ImageManager.images.get(imageCache.get(id));
            if(img == null){ imageCache.remove(id); } else { return img; }
        }
        GCImage image = ImageManager.images.get(GCImage.createGCImage(sticker.url));
        if(image != null){ imageCache.put(id,image.id); }
        return image;
    }

    private static String labelForUrl(String url) {
        try {
            String host = new java.net.URL(url).getHost();
            if (host == null) return "Link";
            if (host.startsWith("www.")) host = host.substring(4);
            return host;
        } catch (Exception e) {
            return "Link";
        }
    }

    private static GCImage resolveEmbedImage(String rawUrl) {
        String cacheKey = "embed_" + rawUrl;
        if (imageCache.containsKey(cacheKey)) {
            GCImage img = ImageManager.images.get(imageCache.get(cacheKey));
            if (img == null) { imageCache.remove(cacheKey); } else { return img; }
        }

        String gcId;
        if (GCImage.looksLikeImageUrl(rawUrl)) {
            gcId = GCImage.createGCImage(rawUrl);
        } else if (isEmbeddablePage(rawUrl)) {
            gcId = GCImage.createGCImageFromPage(rawUrl);
        } else {
            return null;
        }

        GCImage image = ImageManager.images.get(gcId);
        if (image != null) imageCache.put(cacheKey, image.id);
        return image;
    }

    private static boolean isEmbeddablePage(String url) {
        try {
            String host = new java.net.URL(url).getHost().toLowerCase();
            for (String domain : EMBEDDABLE_PAGE_DOMAINS) {
                if (host.equals(domain) || host.endsWith("." + domain)) return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    public static String getReplyPreview(String messageID) {
        for (MessageData md : GlobalChat.messages) {
            if (md.message != null && messageID.equals(md.message.messageID)) {
                String c = md.message.message != null ? md.message.message.content : null;
                if (c != null && c.length() > 50) return c.substring(0, 50) + "...";
                return c != null ? c : "";
            }
        }
        return "";
    }

    public static String getReactionKey(String name, String id, boolean animated) {
        if (id == null || id.isEmpty()) return name;
        return (animated ? "a:" : "") + name + ":" + id;
    }
}