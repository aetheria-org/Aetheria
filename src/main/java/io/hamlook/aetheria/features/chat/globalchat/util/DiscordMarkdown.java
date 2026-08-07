package io.hamlook.aetheria.features.chat.globalchat.util;

import io.hamlook.aetheria.features.chat.globalchat.GlobalChat;
import io.hamlook.aetheria.features.chat.globalchat.vars.Channel;
import io.hamlook.aetheria.features.chat.globalchat.vars.ChatMessage;
import io.hamlook.aetheria.features.chat.globalchat.vars.EmojiRef;
import net.minecraft.client.gui.FontRenderer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the subset of Discord's markdown flavour that actually shows up in chat -
 * bold, italic, bold+italic, underline, strikethrough, spoilers, inline code, fenced
 * code blocks, block quotes, headers (#, ##, ###), and subheaders (-#) - and converts custom emoji
 * shortcodes into inline image spans. The result is a list of {@link RenderLine}s
 * that are already word-wrapped to a given pixel width, ready to be drawn with
 * vanilla's {@link FontRenderer}.
 * <p>
 * This is intentionally not a full CommonMark implementation - only what Discord's
 * own client renders is handled.
 */
public class DiscordMarkdown {

    public static final int EMOJI_SIZE = 16;
    public static final int LINE_SPACING = 3;

    public enum LineType { TEXT, HEADER1, HEADER2, HEADER3, SUBHEADER, QUOTE, CODE_BLOCK, LIST }

    public static class Span {
        public String text = "";
        public boolean bold, italic, underline, strikethrough, spoiler, code;
        public String imageUrl;      // non-null => render as an inline image (custom emoji) instead of text
        public String emojiName;     // shortcode (no colons) of the emoji, for spritesheet rendering; null for other images
        public String linkUrl;       // non-null => render as a clickable hyperlink [text](url)
        public boolean plainLink;    // clickable link that still renders in the normal text style
        public boolean bareLink;     // link created from a raw URL in the text (not a [text](url) token)
        public boolean spaceBefore;  // whether a single space should be drawn before this token
        /** True when this span is a "@name" mention — rendered as a Discord-style mention pill, immune to markdown. */
        public boolean mention;
        /** Display name to show in the mention pill (null/empty => the raw token). */
        public String mentionDisplay;
        /** Non-null => a discord.com/channels/{guild}/{channel}[/{message}] link, rendered as "#channel" (or "#channel → bubble"). */
        public String discordChannelId;
        /** Non-null (with discordChannelId) => the linked message; the span renders a bubble and jumps to the message on click. */
        public String discordMessageId;
        /** Full original URL for message links (used to jump into local history). */
        public String discordUrl;
        /** Raw unicode of the emoji when it is a default (spritesheet-renderable) emoji; empty for remote custom emojis. */
        public String emojiSurrogates;

        public Span() {}
        public Span(String text) { this.text = text; }

        public Span copy() {
            Span s = new Span(text);
            s.bold = bold; s.italic = italic; s.underline = underline;
            s.strikethrough = strikethrough; s.spoiler = spoiler; s.code = code;
            s.imageUrl = imageUrl; s.spaceBefore = spaceBefore;
            s.emojiName = emojiName;
            s.linkUrl = linkUrl; s.plainLink = plainLink; s.bareLink = bareLink;
            s.mention = mention; s.mentionDisplay = mentionDisplay;
            s.discordChannelId = discordChannelId; s.discordMessageId = discordMessageId;
            s.discordUrl = discordUrl;
            s.emojiSurrogates = emojiSurrogates;
            return s;
        }
    }

    public static class RenderLine {
        public final LineType type;
        public final List<Span> spans = new ArrayList<>();
        public List<CodeParser.HighlightedToken> codeTokens; // non-null => syntax-highlighted code line
        public int listIndent; // horizontal shift for list items (continuation lines stay indented)
        public RenderLine(LineType type) { this.type = type; }
    }

    private static final Pattern EMOJI_PATTERN = Pattern.compile(":([a-zA-Z0-9_~+-]+):");
    private static final Pattern NUMBERED_PATTERN = Pattern.compile("^\\d{1,3}\\. ");
    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s<>\"']+");
    private static final String ESCAPABLE = "\\*_~>#`|:[]()";
    private static final int LIST_INDENT = 12;
    /** Horizontal padding (both sides) applied to mention pills when measuring width. */
    public static final int MENTION_PILL_PADDING = 6;
    /** The font used to render a resolved mention's display name in the pill. */
    public static final String MENTION_FONT = "\u00a7r\u00a7f";

    private DiscordMarkdown() {}

    /** Parses + word-wraps a raw message body to fit within maxWidth pixels. */
    public static List<RenderLine> parse(String content, Map<String, EmojiRef> emojis, FontRenderer fr, int maxWidth) {
        return parse(content, emojis, null, fr, maxWidth);
    }

    /**
     * Parses + word-wraps a raw message body to fit within maxWidth pixels.
     *
     * @param mentionNames lowercase mention key ("@gina") -> display name shown in the mention pill; null to
     *                     render the raw token. Keys are lowercase usernames AND display names.
     */
    public static List<RenderLine> parse(String content, Map<String, EmojiRef> emojis, Map<String, String> mentionNames,
                                         FontRenderer fr, int maxWidth) {
        List<RenderLine> out = new ArrayList<>();
        if (content == null || content.isEmpty()) return out;

        String[] rawLines = content.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);

        boolean inCodeBlock = false;
        List<String> codeBuffer = null;
        String codeLanguage = null;

        for (String raw : rawLines) {
            String trimmedCheck = raw.trim();
            if (trimmedCheck.startsWith("```")) {
                if (!inCodeBlock) {
                    inCodeBlock = true;
                    codeBuffer = new ArrayList<>();
                    codeLanguage = trimmedCheck.substring(3).trim();
                    if (codeLanguage.isEmpty()) codeLanguage = "txt";
                } else {
                    inCodeBlock = false;
                    emitCodeBlock(codeBuffer, codeLanguage, out, fr, Math.max(20, maxWidth - 8));
                    codeBuffer = null;
                    codeLanguage = null;
                }
                continue;
            }
            if (inCodeBlock) {
                codeBuffer.add(raw);
                continue;
            }

            LineType type = LineType.TEXT;
            String text = raw;
            int wrapWidth = maxWidth;

            if (text.startsWith("### ")) { type = LineType.HEADER3; text = text.substring(4); wrapWidth = (int) (maxWidth / 1.15f); }
            else if (text.startsWith("## ")) { type = LineType.HEADER2; text = text.substring(3); wrapWidth = (int) (maxWidth / 1.3f); }
            else if (text.startsWith("# ")) { type = LineType.HEADER1; text = text.substring(2); wrapWidth = (int) (maxWidth / 1.5f); }
            else if (text.startsWith("-# ")) { type = LineType.SUBHEADER; text = text.substring(3); wrapWidth = (int) (maxWidth / 0.8f); }
            else if (isListStart(text)) { type = LineType.LIST; wrapWidth = maxWidth - LIST_INDENT; text = bulletize(text); }
            else if (text.startsWith("> ")) { type = LineType.QUOTE; text = text.substring(2); wrapWidth = maxWidth - 10; }

            List<Span> runs = parseInline(text, emojis, mentionNames);
            List<Span> tokens = toWordTokens(runs);
            List<RenderLine> packed = packWords(tokens, type, fr, Math.max(20, wrapWidth));
            if (type == LineType.LIST) {
                for (RenderLine rl : packed) rl.listIndent = LIST_INDENT;
            }
            out.addAll(packed);
        }

        if (inCodeBlock) {
            emitCodeBlock(codeBuffer, codeLanguage, out, fr, Math.max(20, maxWidth - 8));
        }

        return out;
    }

    public static int lineHeight(LineType type, FontRenderer fr) {
        int base = fr.FONT_HEIGHT;
        switch (type) {
            case HEADER1: return Math.round(base * 1.5f) + 5;
            case HEADER2: return Math.round(base * 1.3f) + 4;
            case HEADER3: return Math.round(base * 1.15f) + 3;
            case SUBHEADER: return Math.round(base * 0.8f) + LINE_SPACING;
            default: return base + LINE_SPACING;
        }
    }
    public static float lineScale(LineType type) {
        switch (type) {
            case HEADER1: return 1.5f;
            case HEADER2: return 1.3f;
            case HEADER3: return 1.15f;
            case SUBHEADER: return 0.8f;
            default: return 1f;
        }
    }

    private static boolean isListStart(String text) {
        return text.startsWith("- ") || text.startsWith("* ") || text.startsWith("+ ")
                || NUMBERED_PATTERN.matcher(text).find();
    }

    private static String bulletize(String text) {
        if (text.startsWith("- ") || text.startsWith("* ") || text.startsWith("+ ")) {
            return "• " + text.substring(2);
        }
        return text;
    }

    /** Strips trailing punctuation Discord-style so "see https://x.com)." links to https://x.com. */
    private static String trimUrlPunctuation(String url) {
        int end = url.length();
        while (end > 0 && ".,;:!?)'\"".indexOf(url.charAt(end - 1)) >= 0) end--;
        return url.substring(0, end);
    }

    /** Display text for a bare URL: the file name for image links (or "image"), the URL otherwise. */
    private static String linkLabel(String url) {
        if (!isImageUrl(url)) return url;
        String base = url;
        int q = base.indexOf('?');
        if (q >= 0) base = base.substring(0, q);
        int hash = base.indexOf('#');
        if (hash >= 0) base = base.substring(0, hash);
        int slash = base.lastIndexOf('/');
        if (slash >= 0 && slash < base.length() - 1) return base.substring(slash + 1);
        return "image";
    }

    /** Lowercased file extension of a URL (query/hash stripped), or null if it has no extension. */
    public static String extensionOf(String url) {
        if (url == null) return null;
        String base = url;
        int q = base.indexOf('?');
        if (q >= 0) base = base.substring(0, q);
        int hash = base.indexOf('#');
        if (hash >= 0) base = base.substring(0, hash);
        int dot = base.lastIndexOf('.');
        if (dot < 0 || dot >= base.length() - 1) return null;
        String ext = base.substring(dot + 1).toLowerCase();
        if (ext.length() > 5) return null;
        for (int i = 0; i < ext.length(); i++) {
            char c = ext.charAt(i);
            if (!((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9'))) return null;
        }
        return ext;
    }

    /** True if the URL points at a decodable image (by extension). */
    public static boolean isImageUrl(String url) {
        String ext = extensionOf(url);
        if (ext == null) return false;
        switch (ext) {
            case "png": case "jpg": case "jpeg": case "gif": case "webp": case "bmp":
                return true;
            default: return false;
        }
    }

    /** True if the URL points at a playable video file (by extension). */
    public static boolean isVideoUrl(String url) {
        String ext = extensionOf(url);
        if (ext == null) return false;
        switch (ext) {
            case "mp4": case "webm": case "mkv": case "mov": case "avi":
            case "m4v": case "wmv": case "flv": case "ts":
                return true;
            default: return false;
        }
    }

    /** True if the URL points at an image, animated image or video (the only media types the client renders/downloads). */
    public static boolean isMediaUrl(String url) {
        return isImageUrl(url) || isVideoUrl(url);
    }

    // ---------------------------------------------------------------- inline

    private static List<Span> parseInline(String text, Map<String, EmojiRef> emojis, Map<String, String> mentionNames) {
        List<Span> spans = new ArrayList<>();
        StringBuilder buf = new StringBuilder();
        boolean bold = false, italic = false, underline = false, strike = false, spoiler = false, code = false;
        int i = 0, n = text.length();
        Matcher urlMatcher = URL_PATTERN.matcher(text);

        while (i < n) {
            char c = text.charAt(i);

            if (!code) {
                if (c == '\\' && i + 1 < n && ESCAPABLE.indexOf(text.charAt(i + 1)) >= 0) {
                    buf.append(text.charAt(i + 1));
                    i += 2;
                    continue;
                }

                if (c == '@' && (i == 0 || !isMentionWordChar(text.charAt(i - 1)))) {
                    int j = i + 1;
                    while (j < n && isMentionWordChar(text.charAt(j))) j++;
                    if (j > i + 1) {
                        // "@" followed by token chars => a mention, immune to markdown (so names like
                        // "@_.whispering" never toggle italics) and rendered as a Discord-style pill.
                        String token = text.substring(i + 1, j);
                        while (token.length() > 1 && token.charAt(token.length() - 1) == '.') {
                            token = token.substring(0, token.length() - 1);
                        }
                        flush(buf, spans, bold, italic, underline, strike, spoiler, code);
                        Span m = new Span(token);
                        m.mention = true;
                        if (mentionNames != null) m.mentionDisplay = mentionNames.get(token.toLowerCase());
                        spans.add(m);
                        i = j;
                        continue;
                    }
                }

                if (c == ':' && emojis != null && !emojis.isEmpty()) {
                    Matcher em = EMOJI_PATTERN.matcher(text);
                    em.region(i, n);
                    if (em.lookingAt()) {
                        EmojiRef ref = emojis.get(em.group(1));
                        if (ref != null) {
                            flush(buf, spans, bold, italic, underline, strike, spoiler, code);
                            Span img = new Span("");
                            img.imageUrl = ref.url;
                            img.emojiName = em.group(1);
                            img.emojiSurrogates = ref.surrogates;
                            spans.add(img);
                            i = em.end();
                            continue;
                        }
                    }
                }

                if (c == '[') {
                    int closeBracket = text.indexOf(']', i + 1);
                    int openParen = closeBracket + 1;
                    if (closeBracket > i + 1 && openParen < n && text.charAt(openParen) == '(') {
                        int closeParen = text.indexOf(')', openParen + 1);
                        if (closeParen > openParen + 1) {
                            flush(buf, spans, bold, italic, underline, strike, spoiler, code);
                            Span link = new Span(text.substring(i + 1, closeBracket));
                            link.linkUrl = text.substring(openParen + 1, closeParen);
                            spans.add(link);
                            i = closeParen + 1;
                            continue;
                        }
                    }
                }

                urlMatcher.region(i, n);
                if (urlMatcher.lookingAt()) {
                    String raw = urlMatcher.group();
                    boolean angleWrapped = i > 0 && text.charAt(i - 1) == '<'
                            && i + raw.length() < n && text.charAt(i + raw.length()) == '>';
                    boolean discordMessageLink = raw.startsWith("https://discord.com/channels/");
                    if (discordMessageLink) {
                        flush(buf, spans, bold, italic, underline, strike, spoiler, code);
                        Span link = buildDiscordChannelLink(raw);
                        spans.add(link);
                        i += raw.length();
                        continue;
                    }
                    if (angleWrapped) {
                        buf.append(raw);
                        i += raw.length();
                        continue;
                    }
                    String url = trimUrlPunctuation(raw);
                    flush(buf, spans, bold, italic, underline, strike, spoiler, code);
                    Span link = new Span(linkLabel(url));
                    link.linkUrl = url;
                    link.bareLink = true;
                    spans.add(link);
                    i += raw.length();
                    continue;
                }
                if (c == '`') {
                    flush(buf, spans, bold, italic, underline, strike, spoiler, code);
                    code = true; i++; continue;
                }
                if (text.regionMatches(i, "**", 0, 2)) {
                    flush(buf, spans, bold, italic, underline, strike, spoiler, code);
                    bold = !bold; i += 2; continue;
                }
                if (text.regionMatches(i, "__", 0, 2)) {
                    flush(buf, spans, bold, italic, underline, strike, spoiler, code);
                    underline = !underline; i += 2; continue;
                }
                if (text.regionMatches(i, "~~", 0, 2)) {
                    flush(buf, spans, bold, italic, underline, strike, spoiler, code);
                    strike = !strike; i += 2; continue;
                }
                if (text.regionMatches(i, "||", 0, 2)) {
                    flush(buf, spans, bold, italic, underline, strike, spoiler, code);
                    spoiler = !spoiler; i += 2; continue;
                }
                if (c == '*' || c == '_') {
                    flush(buf, spans, bold, italic, underline, strike, spoiler, code);
                    italic = !italic; i++; continue;
                }
            } else if (c == '`') {
                flush(buf, spans, bold, italic, underline, strike, spoiler, code);
                code = false; i++; continue;
            }

            buf.append(c);
            i++;
        }
        flush(buf, spans, bold, italic, underline, strike, spoiler, code);
        return spans;
    }

    private static boolean isMentionWordChar(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_' || c == '.';
    }

    /**
     * Turns a discord.com/channels/{guild}/{channel}[/{message}] URL into a
     * "#channel" span (plus a message bubble when a message id is present), or
     * a plain link when the channel isn't a Global Chat channel the user can
     * open. The label is resolved here so word-wrapping measures the right text.
     */
    private static Span buildDiscordChannelLink(String raw) {
        String[] parts = raw.split("/");
        String channelId = parts.length >= 6 ? parts[5] : null;
        String messageId = parts.length >= 7 ? parts[6] : null;
        Channel channel = channelId != null ? GlobalChat.channels.get(channelId) : null;
        if (channel == null || channel.channelName == null) {
            Span fallback = new Span(raw);
            fallback.linkUrl = raw;
            fallback.plainLink = true;
            fallback.bareLink = true;
            return fallback;
        }
        Span link = new Span("#" + channel.channelName);
        if (messageId != null) {
            ChatMessage target = channel.byDiscordID.get(messageId);
            String who = target != null
                    ? (target.authorDisplay != null && !target.authorDisplay.isEmpty() ? target.authorDisplay : target.author)
                    : null;
            if (who != null && !who.isEmpty()) link.text = link.text + " \u2192 " + who;
        }
        link.discordChannelId = channelId;
        link.discordMessageId = messageId;
        link.discordUrl = raw;
        return link;
    }

    private static void flush(StringBuilder buf, List<Span> spans, boolean bold, boolean italic,
                              boolean underline, boolean strike, boolean spoiler, boolean code) {
        if (buf.length() == 0) return;
        Span s = new Span(buf.toString());
        s.bold = bold; s.italic = italic; s.underline = underline;
        s.strikethrough = strike; s.spoiler = spoiler; s.code = code;
        spans.add(s);
        buf.setLength(0);
    }

    // ------------------------------------------------------------ wrapping

    private static List<Span> toWordTokens(List<Span> runs) {
        List<Span> tokens = new ArrayList<>();
        boolean pendingSpace = false;
        for (Span run : runs) {
            if (run.imageUrl != null || run.mention || run.discordChannelId != null) {
                Span t = run.copy();
                t.spaceBefore = pendingSpace;
                tokens.add(t);
                pendingSpace = false;
                continue;
            }
            String text = run.text;
            int start = 0, len = text.length();
            for (int idx = 0; idx <= len; idx++) {
                boolean atEnd = idx == len;
                boolean isSpace = !atEnd && text.charAt(idx) == ' ';
                if (atEnd || isSpace) {
                    if (idx > start) {
                        Span t = run.copy();
                        t.text = text.substring(start, idx);
                        t.spaceBefore = pendingSpace;
                        tokens.add(t);
                        pendingSpace = false;
                    }
                    if (isSpace) pendingSpace = true;
                    start = idx + 1;
                }
            }
        }
        return tokens;
    }

    private static Span tokDisplay(Span tok) {
        Span copy = tok.copy();
        copy.text = tok.mentionDisplay;
        return copy;
    }

    private static List<RenderLine> packWords(List<Span> tokens, LineType type, FontRenderer fr, int maxWidth) {
        List<RenderLine> result = new ArrayList<>();
        RenderLine current = new RenderLine(type);
        int width = 0;
        int spaceWidth = fr.getStringWidth(" ");

        for (Span tok : tokens) {
            int tokWidth = tok.imageUrl != null ? EMOJI_SIZE
                    : fr.getStringWidth(toFormatted(tok.mention && tok.mentionDisplay != null ? tokDisplay(tok) : tok))
                        + (tok.mention ? MENTION_PILL_PADDING * 2 : 0);

            if (tokWidth > maxWidth && tok.imageUrl == null && !tok.mention && tok.discordChannelId == null) {
                if (!current.spans.isEmpty()) { result.add(current); current = new RenderLine(type); width = 0; }
                splitOversized(tok, result, type, fr, maxWidth);
                continue;
            }

            int addWidth = (current.spans.isEmpty() ? 0 : spaceWidth) + tokWidth;
            if (!current.spans.isEmpty() && width + addWidth > maxWidth) {
                result.add(current);
                current = new RenderLine(type);
                Span fresh = tok.copy();
                fresh.spaceBefore = false;
                current.spans.add(fresh);
                width = tokWidth;
                continue;
            }
            if (!current.spans.isEmpty()) width += spaceWidth;
            current.spans.add(tok);
            width += tokWidth;
        }
        if (!current.spans.isEmpty() || result.isEmpty()) result.add(current);
        return result;
    }

    private static void splitOversized(Span tok, List<RenderLine> result, LineType type, FontRenderer fr, int maxWidth) {
        String remaining = tok.text;
        while (!remaining.isEmpty()) {
            String fit = fr.trimStringToWidth(remaining, maxWidth);
            if (fit.isEmpty()) fit = remaining.substring(0, 1);
            RenderLine line = new RenderLine(type);
            Span piece = tok.copy();
            piece.text = fit;
            piece.spaceBefore = false;
            line.spans.add(piece);
            result.add(line);
            remaining = remaining.substring(fit.length());
        }
    }

    private static void emitCodeBlock(List<String> codeBuffer, String language, List<RenderLine> out, FontRenderer fr, int maxWidth) {
        if (codeBuffer == null || codeBuffer.isEmpty()) {
            out.add(new RenderLine(LineType.CODE_BLOCK));
            return;
        }

        CodeParser parser = new CodeParser(language);
        List<List<CodeParser.HighlightedToken>> tokenLines = parser.parseDocument(codeBuffer);
        for (List<CodeParser.HighlightedToken> tokenLine : tokenLines) {
            if (tokenLine.isEmpty()) {
                out.add(new RenderLine(LineType.CODE_BLOCK));
                continue;
            }
            out.addAll(wrapTokens(tokenLine, fr, maxWidth));
        }
    }

    private static List<RenderLine> wrapTokens(List<CodeParser.HighlightedToken> tokens, FontRenderer fr, int maxWidth) {
        List<RenderLine> result = new ArrayList<>();
        RenderLine current = new RenderLine(LineType.CODE_BLOCK);
        current.codeTokens = new ArrayList<>();
        int width = 0;

        for (CodeParser.HighlightedToken token : tokens) {
            String rest = token.text;
            while (!rest.isEmpty()) {
                String fit = fr.trimStringToWidth(rest, maxWidth - width);
                if (fit.isEmpty()) {
                    if (width > 0) {
                        result.add(current);
                        current = new RenderLine(LineType.CODE_BLOCK);
                        current.codeTokens = new ArrayList<>();
                        width = 0;
                        continue;
                    }
                    fit = rest.substring(0, 1);
                }
                current.codeTokens.add(new CodeParser.HighlightedToken(fit, token.mcColor));
                width += fr.getStringWidth(fit);
                rest = rest.substring(fit.length());
                if (width >= maxWidth && !rest.isEmpty()) {
                    result.add(current);
                    current = new RenderLine(LineType.CODE_BLOCK);
                    current.codeTokens = new ArrayList<>();
                    width = 0;
                }
            }
        }
        if (!current.codeTokens.isEmpty() || result.isEmpty()) result.add(current);
        return result;
    }

    public static String toFormatted(Span s) {
        StringBuilder sb = new StringBuilder();
        if (s.bold) sb.append('§').append('l');
        if (s.italic) sb.append('§').append('o');
        if (s.underline) sb.append('§').append('n');
        if (s.strikethrough) sb.append('§').append('m');
        sb.append(s.text);
        return sb.toString();
    }
}