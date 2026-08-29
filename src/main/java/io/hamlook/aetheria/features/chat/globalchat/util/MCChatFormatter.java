package io.hamlook.aetheria.features.chat.globalchat.util;

import io.hamlook.aetheria.features.chat.globalchat.GlobalChat;
import io.hamlook.aetheria.features.chat.globalchat.vars.Attachment;
import io.hamlook.aetheria.features.chat.globalchat.vars.ChatMessage;
import io.hamlook.aetheria.features.chat.globalchat.vars.Channel;
import io.hamlook.aetheria.features.chat.globalchat.vars.Embed;
import io.hamlook.aetheria.features.chat.globalchat.vars.EmojiRef;
import io.hamlook.aetheria.utils.EmojiParser;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MCChatFormatter {

    private static final Pattern EMOJI_SHORTCODE = Pattern.compile(":([a-zA-Z0-9_~+-]+):");
    private static final Pattern ESCAPABLE = Pattern.compile("\\\\([*_~>#`|:\\[\\]()])");

    public static String format(ChatMessage msg) {
        StringBuilder out = new StringBuilder();

        Channel channel = GlobalChat.channels.get(msg.channelId);
        String channelName = (channel != null && channel.channelName != null) ? channel.channelName : msg.channelId;

        String author = msg.authorDisplay != null && !msg.authorDisplay.isEmpty() ? msg.authorDisplay : msg.author;

        out.append("§7[§bGCHAT§7:§e").append(escapeForChat(channelName)).append("§7] §a").append(escapeForChat(author)).append("§7: §r");

        String content = msg.content;
        if (content == null || content.isEmpty()) {
            content = "";
        }

        String formatted = processMarkdown(content, msg.emojiRefs);
        out.append(formatted);

        appendAttachments(out, msg.attachments, msg.embeds);
        appendStickers(out, msg.stickers);

        return out.toString();
    }

    private static String escapeForChat(String s) {
        if (s == null) return "";
        return s.replace("§", "").replace("&", "");
    }

    private static String processMarkdown(String text, Map<String, EmojiRef> emojiRefs) {
        StringBuilder out = new StringBuilder();
        int i = 0, n = text.length();

        boolean bold = false, italic = false, underline = false, strike = false, spoiler = false, code = false;
        StringBuilder buf = new StringBuilder();

        while (i < n) {
            char c = text.charAt(i);

            if (!code) {
                if (c == '\\' && i + 1 < n && ESCAPABLE.matcher(text.substring(i, i + 2)).matches()) {
                    buf.append(text.charAt(i + 1));
                    i += 2;
                    continue;
                }

                if (c == ':' && emojiRefs != null && !emojiRefs.isEmpty()) {
                    Matcher em = EMOJI_SHORTCODE.matcher(text);
                    em.region(i, n);
                    if (em.lookingAt()) {
                        String name = em.group(1);
                        EmojiRef ref = emojiRefs.get(name);
                        flush(buf, out, bold, italic, underline, strike, spoiler, code);
                        String unicode;
                        if (ref != null && ref.surrogates != null && !ref.surrogates.isEmpty()) {
                            unicode = ref.surrogates;
                        } else {
                            unicode = EmojiParser.toUnicode(":" + name + ":");
                            if (unicode.equals(":" + name + ":")) unicode = null;
                        }
                        if (unicode != null) {
                            out.append(unicode);
                        } else {
                            out.append(":").append(name).append(":");
                        }
                        i = em.end();
                        continue;
                    }
                }

                if (text.regionMatches(i, "**", 0, 2)) {
                    flush(buf, out, bold, italic, underline, strike, spoiler, code);
                    bold = !bold;
                    i += 2;
                    continue;
                }
                if (text.regionMatches(i, "__", 0, 2)) {
                    flush(buf, out, bold, italic, underline, strike, spoiler, code);
                    underline = !underline;
                    i += 2;
                    continue;
                }
                if (text.regionMatches(i, "~~", 0, 2)) {
                    flush(buf, out, bold, italic, underline, strike, spoiler, code);
                    strike = !strike;
                    i += 2;
                    continue;
                }
                if (text.regionMatches(i, "||", 0, 2)) {
                    flush(buf, out, bold, italic, underline, strike, spoiler, code);
                    spoiler = !spoiler;
                    i += 2;
                    continue;
                }
                if (c == '`') {
                    flush(buf, out, bold, italic, underline, strike, spoiler, code);
                    code = !code;
                    i++;
                    continue;
                }
                if (c == '*' || c == '_') {
                    flush(buf, out, bold, italic, underline, strike, spoiler, code);
                    italic = !italic;
                    i++;
                    continue;
                }
                if (text.regionMatches(i, "# ", 0, 2)) {
                    flush(buf, out, bold, italic, underline, strike, spoiler, code);
                    out.append("§l");
                    i += 2;
                    continue;
                }
                if (text.regionMatches(i, "## ", 0, 3)) {
                    flush(buf, out, bold, italic, underline, strike, spoiler, code);
                    out.append("§l");
                    i += 3;
                    continue;
                }
                if (text.regionMatches(i, "### ", 0, 4)) {
                    flush(buf, out, bold, italic, underline, strike, spoiler, code);
                    out.append("§l");
                    i += 4;
                    continue;
                }
                if (text.regionMatches(i, "-# ", 0, 3)) {
                    flush(buf, out, bold, italic, underline, strike, spoiler, code);
                    out.append("§l");
                    i += 3;
                    continue;
                }
            } else {
                if (c == '`') {
                    flush(buf, out, bold, italic, underline, strike, spoiler, code);
                    code = false;
                    i++;
                    continue;
                }
            }

            buf.append(c);
            i++;
        }

        flush(buf, out, bold, italic, underline, strike, spoiler, code);

        return out.toString().replace("\n", " §7|§r ");
    }

    private static void flush(StringBuilder buf, StringBuilder out, boolean bold, boolean italic,
                              boolean underline, boolean strike, boolean spoiler, boolean code) {
        if (buf.length() == 0) return;

        String text = buf.toString();
        if (spoiler) {
            out.append("||").append(text).append("||");
        } else if (code) {
            out.append("`").append(text).append("`");
        } else {
            StringBuilder codes = new StringBuilder();
            if (bold) codes.append("§l");
            if (italic) codes.append("§o");
            if (underline) codes.append("§n");
            if (strike) codes.append("§m");
            if (codes.length() > 0) {
                out.append(codes).append(text).append("§r");
            } else {
                out.append(text);
            }
        }
        buf.setLength(0);
    }

    private static void appendAttachments(StringBuilder out, List<Attachment> attachments, List<Embed> embeds) {
        if ((attachments == null || attachments.isEmpty()) && (embeds == null || embeds.isEmpty())) return;

        int imageCount = 0;
        boolean hasGif = false;
        int fileCount = 0;

        if (attachments != null) {
            for (Attachment att : attachments) {
                String type = att.imageType;
                if (type == null) type = guessMimeFromUrl(att.url);
                if (type != null) {
                    if (type.startsWith("image/")) {
                        if (type.equals("image/gif") || (att.name != null && att.name.toLowerCase().endsWith(".gif"))) {
                            hasGif = true;
                        } else {
                            imageCount++;
                        }
                    } else if (type.startsWith("video/")) {
                        hasGif = true;
                    } else {
                        fileCount++;
                    }
                } else {
                    fileCount++;
                }
            }
        }

        if (embeds != null) {
            for (Embed embed : embeds) {
                if ("image".equals(embed.type)) {
                    imageCount++;
                } else if ("file".equals(embed.type) || "video".equals(embed.type)) {
                    fileCount++;
                }
            }
        }

        boolean first = true;
        if (imageCount > 0) {
            if (!first) out.append(" ");
            out.append("§8[§7Sent ").append(imageCount).append(" Image").append(imageCount == 1 ? "" : "s").append("§8]");
            first = false;
        }
        if (hasGif) {
            if (!first) out.append(" ");
            out.append("§8[§7Sent a GiF§8]");
            first = false;
        }
        if (fileCount > 0) {
            if (!first) out.append(" ");
            out.append("§8[§7Sent ").append(fileCount).append(" File").append(fileCount == 1 ? "" : "s").append("§8]");
        }
    }

    private static void appendStickers(StringBuilder out, Map<String, ?> stickers) {
        if (stickers == null || stickers.isEmpty()) return;
        out.append(" §8[§7Sent a Sticker§8]");
    }

    private static String guessMimeFromUrl(String url) {
        if (url == null) return null;
        String lower = url.toLowerCase();
        int q = lower.indexOf('?');
        if (q >= 0) lower = lower.substring(0, q);
        int h = lower.indexOf('#');
        if (h >= 0) lower = lower.substring(0, h);
        int dot = lower.lastIndexOf('.');
        if (dot < 0) return null;
        String ext = lower.substring(dot + 1);
        switch (ext) {
            case "png": return "image/png";
            case "jpg": case "jpeg": return "image/jpeg";
            case "gif": return "image/gif";
            case "webp": return "image/webp";
            case "bmp": return "image/bmp";
            case "mp4": case "webm": case "mov": case "mkv": case "avi":
            case "m4v": case "wmv": case "flv": case "ts": return "video/" + ext;
            default: return null;
        }
    }
}