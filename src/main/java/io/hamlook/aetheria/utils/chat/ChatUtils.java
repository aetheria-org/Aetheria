package io.hamlook.aetheria.utils.chat;

import io.hamlook.aetheria.events.ASMChatEvent;
import io.hamlook.aetheria.utils.ColorUtils;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.TextCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ChatUtils {

    public static final String COLOR_CODES = "0123456789abcdef";
    public static final String FORMATTING_CODES = COLOR_CODES + "klmnor";
    public static final Pattern PLAYER_MSG_STRIPPED = Pattern.compile("^(?:\\[\\d+\\]\\s*)?" + "(?:\\S\\s+)?" + "(?:\\[[^\\]]*\\]\\s*)?" + "(\\w{1,16})" + "[^\\w:]*" + ":\\s*" + "(.+)$");
    private static final ScheduledExecutorService PARTY_MSG_EXECUTOR = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "ATHR-PartyMsg");
        t.setDaemon(true);
        return t;
    });
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
        return StringUtils.stripControlCodes(TextCompat.getFormattedTextWithResets(event.message)).trim();
    }

    public static boolean isChatOpen() {
        Minecraft mc = MinecraftCompat.getMinecraft();
        return mc != null && MinecraftCompat.getCurrentScreen() instanceof GuiChat;
    }

    public static boolean isFromServer(ASMChatEvent event) {
        return event.type == ASMChatEvent.Type.CHAT || event.type == ASMChatEvent.Type.SYSTEM;
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

    public enum MessageType {
        PLAYER, PARTY, GUILD, MSG_RECEIVED, MSG_SENT, DONATE;

        static final MessageType[] VALUES = values();
    }

    public static final class Message {
        public final MessageType type;
        /** author, except MSG_SENT where it is the recipient. */
        public final String sender;
        public final String body;

        private Message(MessageType type, String sender, String body) {
            this.type = type;
            this.sender = sender;
            this.body = body;
        }
    }

    // first type that matches, in the order every chat feature used before
    public static Message findMessage(String plain) {
        for (MessageType type : MessageType.VALUES) {
            String sender = senderOf(type, plain);
            if (sender != null) return new Message(type, sender, bodyOf(type, plain));
        }
        return null;
    }

    private static String senderOf(MessageType type, String plain) {
        switch (type) {
            case PLAYER: return getPlayerMessageSender(plain);
            case PARTY: return getPartySender(plain);
            case GUILD: return getGuildSender(plain);
            case MSG_RECEIVED: return getMsgReceivedSender(plain);
            case MSG_SENT: return getMsgSentRecipient(plain);
            case DONATE: return getDonateSender(plain);
            default: return null;
        }
    }

    private static String bodyOf(MessageType type, String plain) {
        switch (type) {
            case PLAYER: return getPlayerMessageBody(plain);
            case PARTY: return getPartyBody(plain);
            case GUILD: return getGuildBody(plain);
            case MSG_RECEIVED: return getMsgReceivedBody(plain);
            case MSG_SENT: return getMsgSentBody(plain);
            case DONATE: return getDonateBody(plain);
            default: return null;
        }
    }

    public static boolean isFormattingCode(char c) {
        return FORMATTING_CODES.indexOf(Character.toLowerCase(c)) != -1;
    }

    public static Match findFirst(String text, String needle, boolean ignoreCase, int startIndex) {
        if (text == null || needle == null || needle.isEmpty()) return null;
        String haystack = ignoreCase ? text.toLowerCase(Locale.ROOT) : text;
        String target = ignoreCase ? needle.toLowerCase(Locale.ROOT) : needle;
        int start = Math.max(0, Math.min(startIndex, haystack.length()));
        int index = haystack.indexOf(target, start);
        return index < 0 ? null : split(text, index, index + needle.length());
    }

    public static Match split(String text, int start, int end) {
        String before = text.substring(0, start);
        String match = text.substring(start, end);
        String after = text.substring(end);
        return new Match(before, match, after, formatStateAt(text, start), formatStateAt(text, end));
    }

    public static String formatStateAt(String text, int endExclusive) {
        Character color = null;
        boolean bold = false, italic = false, underline = false, strike = false, obfuscated = false;

        int limit = Math.min(endExclusive, text.length()) - 1;
        for (int i = 0; i < limit; i++) {
            if (text.charAt(i) != '§') continue;
            char code = Character.toLowerCase(text.charAt(i + 1));
            if (code == 'r') {
                color = null;
                bold = italic = underline = strike = obfuscated = false;
            } else if (COLOR_CODES.indexOf(code) != -1) {
                color = code;
                bold = italic = underline = strike = obfuscated = false;
            } else if (code == 'l') bold = true;
            else if (code == 'o') italic = true;
            else if (code == 'n') underline = true;
            else if (code == 'm') strike = true;
            else if (code == 'k') obfuscated = true;
            i++;
        }

        StringBuilder out = new StringBuilder();
        if (color != null) out.append('§').append(color);
        if (bold) out.append("§l");
        if (italic) out.append("§o");
        if (underline) out.append("§n");
        if (obfuscated) out.append("§k");
        if (strike) out.append("§m");
        return out.toString();
    }

    public static boolean replaceFirst(IChatComponent root, String needle, boolean ignoreCase, int minFlatIndex, Function<ChatTextMatch, IChatComponent> replacement) {
        if (root == null || needle == null || needle.isEmpty()) return false;
        return replaceFirstIn(TextCompat.getSiblings(root), needle, ignoreCase, Math.max(0, minFlatIndex), new int[]{0}, replacement);
    }

    private static boolean replaceFirstIn(List<IChatComponent> siblings, String needle, boolean ignoreCase, int minFlatIndex, int[] flatOffset, Function<ChatTextMatch, IChatComponent> replacement) {
        for (int i = 0; i < siblings.size(); i++) {
            IChatComponent sibling = siblings.get(i);
            String own = TextCompat.getUnformattedTextForChat(sibling);
            int localStart = Math.max(0, minFlatIndex - flatOffset[0]);
            Match match = findFirst(own, needle, ignoreCase, localStart);
            if (match != null) {
                splitSibling(siblings, i, sibling, match, replacement);
                return true;
            }
            flatOffset[0] += own.length();
            if (replaceFirstIn(TextCompat.getSiblings(sibling), needle, ignoreCase, minFlatIndex, flatOffset, replacement)) {
                return true;
            }
        }
        return false;
    }

    private static void splitSibling(List<IChatComponent> siblings, int index, IChatComponent original, Match match, Function<ChatTextMatch, IChatComponent> replacement) {
        ChatStyle base = TextCompat.createDeepCopy(TextCompat.getChatStyle(original));

        IChatComponent before = TextCompat.fromFormatted(match.before, base);
        IChatComponent middle = replacement.apply(new ChatTextMatch(match, original));
        IChatComponent after = TextCompat.fromFormatted(match.formatAtEnd + match.after, base);

        for (IChatComponent child : TextCompat.getSiblings(original)) {
            TextCompat.appendSibling(after, child);
        }

        siblings.set(index, before);
        siblings.add(index + 1, middle);
        siblings.add(index + 2, after);
    }

    public static String flattenText(IChatComponent root) {
        StringBuilder out = new StringBuilder();
        out.append(TextCompat.getUnformattedTextForChat(root));
        for (IChatComponent child : TextCompat.getSiblings(root)) {
            appendFlat(child, out);
        }
        return out.toString();
    }

    private static void appendFlat(IChatComponent component, StringBuilder out) {
        out.append(TextCompat.getUnformattedTextForChat(component));
        for (IChatComponent child : TextCompat.getSiblings(component)) {
            appendFlat(child, out);
        }
    }

    public static IChatComponent ensureSiblings(IChatComponent component) {
        List<IChatComponent> siblings = TextCompat.getSiblings(component);
        String own = TextCompat.getUnformattedTextForChat(component);
        if (siblings.isEmpty()) {
            IChatComponent root = TextCompat.createText("");
            IChatComponent child = TextCompat.createText(own);
            TextCompat.setChatStyle(child, TextCompat.createDeepCopy(TextCompat.getChatStyle(component)));
            TextCompat.appendSibling(root, child);
            return root;
        }
        if (!own.isEmpty()) {
            IChatComponent root = TextCompat.createText("");
            IChatComponent child = TextCompat.createText(own);
            TextCompat.setChatStyle(child, TextCompat.createDeepCopy(TextCompat.getChatStyle(component)));
            TextCompat.appendSibling(root, child);
            for (IChatComponent sibling : siblings) {
                TextCompat.appendSibling(root, sibling);
            }
            return root;
        }
        //#if MC < 1.21
        return component;
        //#else
        //$$ return TextCompat.deepMutableCopy(component);
        //#endif
    }

    public static void sendMessage(String message) {
        TextCompat.addChatMessage(TextCompat.fromFormatted(message));
    }

    public static void sendMessage(IChatComponent component) {
        TextCompat.addChatMessage(component);
    }

    public static void simulateIncomingChat(IChatComponent message) {
        ASMChatEvent event = new ASMChatEvent(message, ASMChatEvent.Type.CHAT);
        event.post();
        if (!event.isCancelled()) sendMessage(event.message);
    }

    public static void sendMultilineMessage(String message) {
        for (String line : message.split("\n")) {
            TextCompat.addChatMessage(TextCompat.fromFormatted(line));
        }
    }

    public static void sendChatCommand(String message) {
        TextCompat.sendChatMessage(message);
    }

    public static void sendPartyMessage(String message, long delayMs) {
        PARTY_MSG_EXECUTOR.schedule(() -> TextCompat.sendChatMessage("/pc " + message), delayMs, TimeUnit.MILLISECONDS);
    }

    public static void sendPartyMessage(String message) {
        sendPartyMessage(message, 1500);
    }

    public static void sendDonateMessage(String message) {
        sendChatCommand("/dc " + message);
    }

    public static final class Match {
        public final String before;
        public final String match;
        public final String after;
        public final String formatAtStart;
        public final String formatAtEnd;

        private Match(String before, String match, String after, String formatAtStart, String formatAtEnd) {
            this.before = before;
            this.match = match;
            this.after = after;
            this.formatAtStart = formatAtStart;
            this.formatAtEnd = formatAtEnd;
        }
    }

    public static final class ChatTextMatch {
        private final Match split;
        private final IChatComponent source;

        ChatTextMatch(Match split, IChatComponent source) {
            this.split = split;
            this.source = source;
        }

        public String matchText() {
            return split.match;
        }

        public String formatAtStart() {
            return split.formatAtStart;
        }

        public ChatStyle baseStyle() {
            return TextCompat.createDeepCopy(TextCompat.getChatStyle(source));
        }
    }
}
