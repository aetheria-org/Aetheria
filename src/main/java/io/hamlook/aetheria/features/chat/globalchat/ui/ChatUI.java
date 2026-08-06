package io.hamlook.aetheria.features.chat.globalchat.ui;

import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.WebSocketClient;
import io.hamlook.aetheria.features.chat.globalchat.GlobalChat;
import io.hamlook.aetheria.features.chat.globalchat.image.GCImage;
import io.hamlook.aetheria.features.chat.globalchat.image.ImageManager;
import io.hamlook.aetheria.features.chat.globalchat.util.CodeParser;
import io.hamlook.aetheria.features.chat.globalchat.util.DiscordMarkdown;
import io.hamlook.aetheria.features.chat.globalchat.util.DiscordMarkdown.LineType;
import io.hamlook.aetheria.features.chat.globalchat.util.DiscordMarkdown.RenderLine;
import io.hamlook.aetheria.features.chat.globalchat.util.DiscordMarkdown.Span;
import io.hamlook.aetheria.features.chat.globalchat.vars.Attachment;
import io.hamlook.aetheria.features.chat.globalchat.vars.Channel;
import io.hamlook.aetheria.features.chat.globalchat.vars.ChatLine;
import io.hamlook.aetheria.features.chat.globalchat.vars.ChatMessage;
import io.hamlook.aetheria.features.chat.globalchat.vars.Embed;
import io.hamlook.aetheria.features.chat.globalchat.vars.EmojiRef;
import io.hamlook.aetheria.features.chat.globalchat.vars.IEmoji;
import io.hamlook.aetheria.features.chat.globalchat.vars.Sticker;
import io.hamlook.aetheria.repo.CapeAPI;
import io.hamlook.aetheria.utils.MediaSaver;
import io.hamlook.aetheria.utils.MediaUploader;
import io.hamlook.aetheria.utils.ElectionUtils;
import io.hamlook.aetheria.utils.EmojiParser;
import io.hamlook.aetheria.utils.render.RenderUtils;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A Discord-style GUI for the global chat feature: a channel list down the left,
 * a scrollable, avatar-grouped message list in the middle rendering markdown +
 * attachments + stickers + custom emoji, and a send box along the bottom.
 * <p>
 * Written against the vanilla 1.8.9 GuiScreen/FontRenderer/GlStateManager API
 * (matches the fields already used elsewhere in this codebase, e.g. GCImage's
 * use of {@code Minecraft.getMinecraft().getTextureManager()}). If this mod
 * targets a different mapping set (e.g. {@code fontRenderer} instead of
 * {@code fontRendererObj} on newer versions), rename accordingly.
 * <p>
 * Open with {@code Minecraft.getMinecraft().displayGuiScreen(new ChatUI())}, or
 * {@link #open()}.
 */
public class ChatUI extends GuiScreen {

    private static final int SIDEBAR_WIDTH = 240;
    private static final int HEADER_HEIGHT = 30;
    private static final int INPUT_HEIGHT = 39;
    private static final int AVATAR_SIZE = 34;
    private static final int PADDING = 10;
    private static final long GROUP_GAP_MS = 5 * 60 * 1000L;
    private static final int MESSAGE_GAP = 18;
    private static final int CONTINUATION_GAP = 4;
    private static final int MESSAGE_BOTTOM_PAD = 20;
    private static final long JUMP_HIGHLIGHT_MS = 2200L;
    private static final int MARKDOWN_BAR_H = 108;
    private static final int ATTACHMENT_BOX_H = 120;
    private static final int MAX_IMAGE_DRAW_W = 260;
    private static final int MAX_IMAGE_DRAW_H = 260;
    private static final int EMBED_BOX_H = 56;
    private static final int WEBSITE_EMBED_H = 128;
    private static final int STICKER_BOX_H = 90;
    private static final int IMAGE_GRID_COLS = 2;
    private static final int IMAGE_GRID_GAP = 3;
    private static final int EMOJI_BUTTON_W = 26;
    private static final int EMOJI_PANEL_COLS = 10;
    private static final int EMOJI_PANEL_ROWS = 5;
    private static final int EMOJI_PANEL_CELL = 22;
    private static final int EMOJI_SEARCH_H = 22;
    private static final int EMOJI_PANEL_W = EMOJI_PANEL_COLS * EMOJI_PANEL_CELL + 8;
    private static final int EMOJI_PANEL_H = EMOJI_PANEL_ROWS * EMOJI_PANEL_CELL + 8;
    private static final int EMOJI_PANEL_TOTAL_H = EMOJI_PANEL_H + EMOJI_SEARCH_H + 4;

    private static final int STICKER_PANEL_COLS = 3;
    private static final int STICKER_PANEL_ROWS = 4;
    private static final int STICKER_PANEL_CELL = 56;
    private static final int STICKER_PANEL_W = STICKER_PANEL_COLS * STICKER_PANEL_CELL + 8;
    private static final int STICKER_PANEL_H = STICKER_PANEL_ROWS * STICKER_PANEL_CELL + 8;

    private static final int PANEL_EMOJI = 0;
    private static final int PANEL_STICKER = 1;

    private static final String[] CODE_LANGUAGES = {
            "txt", "java", "json", "js", "python", "c", "cpp", "csharp",
            "html", "css", "xml", "sql", "sh", "yaml", "md", "kotlin",
            "lua", "go", "ts", "php", "ruby"
    };

    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("h:mm a");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy h:mm a");

    private ChatInputField inputField;
    private Channel selectedChannel;
    private int scrollPixels = 0;
    private String pendingLinkUrl = null;
    private boolean langDropdownOpen = false;
    private String selectedCodeLang = "txt";
    private long lastCopyTime = 0;

    private final Map<ChatMessage, LayoutCache> layoutCache = new IdentityHashMap<>();
    private final Set<Span> revealedSpoilers = Collections.newSetFromMap(new IdentityHashMap<>());
    private final List<ClickRect> clickRects = new ArrayList<>();
    private final Map<String, Embed> embedCache = new ConcurrentHashMap<>();
    private List<Channel> cachedChannelList = null;
    private int cachedChannelListVersion = -1;
    private List<EmojiRef> sortedEmojiCache = null;
    private int sortedEmojiCacheSize = -1;
    private String emojiFilterKey = null;
    private List<EmojiRef> emojiFilterCache = null;
    private List<Sticker> sortedStickerCache = null;
    private int sortedStickerCacheSize = -1;
    private String jumpToMessageId = null;
    private int jumpTargetTop = Integer.MIN_VALUE;
    private String highlightMessageId = null;
    private long jumpHighlightStart = 0L;
    private boolean emojiPanelOpen = false;
    private int emojiScroll = 0;
    private GuiTextField emojiSearchField;
    private boolean stickerPanelOpen = false;
    private int stickerScroll = 0;
    private int dragPanel = -1;
    private int dragGrabY = 0;
    private volatile boolean attachUploading = false;
    /** File embeds waiting to be attached to the next sent message (from "+" uploads). */
    private final List<Embed> pendingFileEmbeds = new ArrayList<>();
    private String downloadMsg = null;
    private long downloadMsgUntil = 0;
    private ChatMessage pendingReply = null;
    private ChatMessage editingMessage = null;
    private ChatInputField editField;
    private int editBoxX = 0;
    private int editBoxY = 0;
    private int editBoxWidth = 300;
    private int editBoxHeight = 35;
    private boolean editBoxVisible = false;

    public static void open() {
        net.minecraft.client.Minecraft.getMinecraft().displayGuiScreen(new ChatUI());
    }

    // ------------------------------------------------------------- lifecycle

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        if (selectedChannel == null && !GlobalChat.channels.isEmpty()) {
            selectedChannel = GlobalChat.channels.values().iterator().next();
        }

        int inputX = SIDEBAR_WIDTH + PADDING;
        int boxY = height - INPUT_HEIGHT - PADDING;
        int inputWidth = width - SIDEBAR_WIDTH - PADDING * 2;
        inputField = new ChatInputField(inputX + 8 + EMOJI_BUTTON_W + 6, boxY + 2, inputWidth - 16 - EMOJI_BUTTON_W * 4 - 24, INPUT_HEIGHT - 4);
        editField = new ChatInputField(editBoxX, editBoxY, editBoxWidth, INPUT_HEIGHT);
        emojiSearchField = new GuiTextField(0, fontRendererObj, 0, 0, 0, 16);
        emojiSearchField.setEnableBackgroundDrawing(false);
        emojiSearchField.setMaxStringLength(32);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        if (emojiSearchField != null) emojiSearchField.setFocused(false);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void updateScreen() {
        inputField.updateDrag();
        if (editField != null) editField.updateDrag();
        if (emojiSearchField != null) emojiSearchField.updateCursorCounter();
        if (pendingLinkUrl != null) {
            String url = pendingLinkUrl;
            pendingLinkUrl = null;
            mc.displayGuiScreen(new LinkConfirmScreen(url));
        }
        updateConnection();
    }

    private boolean wasConnected = true;
    private long lastReconnectAttempt = 0;

    /** Handles a websocket drop while this screen is open: toasts the loss, throttled reconnects, refreshes channels on recovery. */
    private void updateConnection() {
        boolean connected = Aetheria.webSocketClient != null && WebSocketClient.isConnected;
        if (connected) {
            if (!wasConnected) {
                wasConnected = true;
                GlobalChat.refreshChannels(false);
            }
            return;
        }
        if (wasConnected) {
            wasConnected = false;
            downloadMsg = "Global Chat disconnected - reconnecting...";
            downloadMsgUntil = System.currentTimeMillis() + 3500;
        }
        long now = System.currentTimeMillis();
        if (now - lastReconnectAttempt > 3000) {
            lastReconnectAttempt = now;
            WebSocketClient.reconnectIfNeeded();
        }
    }

    // ---------------------------------------------------------------- input

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (editingMessage != null) {
            if (editBoxVisible && editField.mouseClicked(mouseX, mouseY, mouseButton)) return;
            editingMessage = null;
            return;
        }
        if (emojiPanelOpen && emojiSearchField != null && inEmojiSearchBar(mouseX, mouseY)) {
            emojiSearchField.mouseClicked(mouseX, mouseY, mouseButton);
            return;
        }
        if (inputField.mouseClicked(mouseX, mouseY, mouseButton)) return;
        if (mouseButton == 0) {
            if (handlePanelSliderClick(mouseX, mouseY)) return;
            if ((emojiPanelOpen || stickerPanelOpen) && !inAnyPanel(mouseX, mouseY) && !inAnyButton(mouseX, mouseY)) {
                closeAllPanels();
            }
            for (int i = clickRects.size() - 1; i >= 0; i--) {
                ClickRect cr = clickRects.get(i);
                if (cr.contains(mouseX, mouseY)) {
                    cr.action.run();
                    break;
                }
            }
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (clickedMouseButton == 0 && dragPanel >= 0 && panelOpen(dragPanel)) {
            int[] tr = panelTrackRect(dragPanel);
            int maxScroll = panelMaxScroll(dragPanel);
            int thumbH = panelThumbHeight(dragPanel, tr[3]);
            int travel = Math.max(1, tr[3] - thumbH);
            int target = Math.round((mouseY - tr[1] - dragGrabY) / (float) travel * maxScroll);
            setPanelScroll(dragPanel, target);
            return;
        }
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (state == 0) dragPanel = -1;
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            if (editingMessage != null) {
                editingMessage = null;
                return;
            }
            if (pendingReply != null) {
                pendingReply = null;
                return;
            }
            if (emojiPanelOpen || stickerPanelOpen) {
                closeAllPanels();
                return;
            }
            mc.displayGuiScreen(null);
            return;
        }
        if (editingMessage != null) {
            if (editField.keyTyped(typedChar, keyCode)) return;
            if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
                submitEdit();
            }
            return;
        }
        if (emojiPanelOpen && emojiSearchField != null && emojiSearchField.isFocused()) {
            emojiSearchField.textboxKeyTyped(typedChar, keyCode);
            emojiScroll = 0;
            return;
        }
        if (selectedChannel != null && !selectedChannel.canSend) {
            if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
                downloadMsg = "This channel is read only.";
                downloadMsgUntil = System.currentTimeMillis() + 3500;
            }
            return;
        }
        if (inputField.keyTyped(typedChar, keyCode)) return;
        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            sendCurrentMessage();
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) return;
        int mx = Mouse.getEventX() * width / mc.displayWidth;
        int my = Mouse.getEventY() * height / mc.displayHeight;
        if (editingMessage != null && editBoxVisible && editField.isHovered(mx, my)) {
            editField.mouseWheel(wheel);
            return;
        }
        if (inEmojiPanel(mx, my)) {
            int rowsTotal = (filteredEmojis().size() + EMOJI_PANEL_COLS - 1) / EMOJI_PANEL_COLS;
            int maxScroll = Math.max(0, rowsTotal - EMOJI_PANEL_ROWS);
            emojiScroll = Math.max(0, Math.min(maxScroll, emojiScroll + (wheel > 0 ? -1 : 1)));
            return;
        }
        if (inStickerPanel(mx, my)) {
            int rowsTotal = (sortedStickers().size() + STICKER_PANEL_COLS - 1) / STICKER_PANEL_COLS;
            int maxScroll = Math.max(0, rowsTotal - STICKER_PANEL_ROWS);
            stickerScroll = Math.max(0, Math.min(maxScroll, stickerScroll + (wheel > 0 ? -1 : 1)));
            return;
        }
        if (inputField.isHovered(mx, my)) {
            inputField.mouseWheel(wheel);
            return;
        }
        if (mx <= SIDEBAR_WIDTH) return;
        scrollPixels += wheel > 0 ? 20 : -20;
        if (scrollPixels < 0) scrollPixels = 0;
    }

    private void sendCurrentMessage() {
        if (selectedChannel == null) return;
        if (!selectedChannel.canSend) {
            downloadMsg = "This channel is read only.";
            downloadMsgUntil = System.currentTimeMillis() + 3500;
            return;
        }
        String raw = inputField.getText();
        String converted = EmojiParser.toShortcode(raw);
        String text = converted.trim();
        if (text.isEmpty()) return;
        ChatMessage reply = (pendingReply != null && pendingReply.discordID != null && !pendingReply.discordID.isEmpty())
                ? pendingReply : null;
        ChatMessage message = new ChatMessage(text, selectedChannel.channelID, reply);
        message.populateEmojiRefs(converted);
        if (!pendingFileEmbeds.isEmpty()) {
            List<Embed> attach = new ArrayList<>();
            for (Embed e : pendingFileEmbeds) {
                if (e != null && e.url != null && text.contains("](" + e.url + ")")) attach.add(e);
            }
            if (!attach.isEmpty()) {
                message.embeds = attach;
                pendingFileEmbeds.removeAll(attach);
            }
        }
        if (!message.sendMessage()) {
            downloadMsg = "Not connected to Global Chat. Reconnecting...";
            downloadMsgUntil = System.currentTimeMillis() + 3500;
            return;
        }
        inputField.setText("");
        pendingReply = null;
    }

    private void submitEdit() {
        if (editingMessage == null) return;
        if (selectedChannel != null && !selectedChannel.canSend) {
            downloadMsg = "This channel is read only.";
            downloadMsgUntil = System.currentTimeMillis() + 3500;
            editingMessage = null;
            return;
        }
        String newContent = editField.getText().trim();
        ChatMessage msg = editingMessage;
        editingMessage = null;
        if (newContent.isEmpty()) return;
        GlobalChat.editMessage(msg, newContent);
        msg.content = newContent;
        msg.edited = true;
        msg.contentVersion++;
    }

    private void deleteMessageAction(ChatMessage msg) {
        Channel channel = GlobalChat.channels.get(msg.channelId);
        if (channel != null) channel.removeMessage(msg.messageID, msg.discordID);
        GlobalChat.deleteMessage(msg);
        if (editingMessage == msg) editingMessage = null;
    }

    private void startEdit(ChatMessage msg) {
        if (editingMessage != null) return;
        editingMessage = msg;
        editField.setText(msg.content == null ? "" : msg.content);
    }

    private static boolean isOwnMessage(ChatMessage msg) {
        return msg.author != null && msg.author.equalsIgnoreCase(
                net.minecraft.client.Minecraft.getMinecraft().getSession().getUsername());
    }

    // --------------------------------------------------------------- render

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawRect(0, 0, width, height, 0xFF313338);

        if (selectedChannel != null && GlobalChat.channels.get(selectedChannel.channelID) != selectedChannel) {
            Channel replacement = null;
            for (Channel c : GlobalChat.channels.values()) {
                if (c.channelName != null && c.channelName.equals(selectedChannel.channelName)) {
                    replacement = c;
                    break;
                }
            }
            if (replacement == null && !GlobalChat.channels.isEmpty()) {
                replacement = GlobalChat.channels.values().iterator().next();
            }
            selectedChannel = replacement;
        }

        clickRects.clear();
        drawSidebar(mouseX, mouseY);
        drawHeader(mouseX, mouseY);
        drawMessages(mouseX, mouseY);
        drawSystemNotices(mouseX, mouseY);
        drawInputArea(mouseX, mouseY);
        drawReplyBanner(mouseX, mouseY);

        if (selectedChannel == null || selectedChannel.canSend) {
            inputField.draw();
        }
        if (editingMessage != null && editField != null && editBoxVisible) {
            editField.x = editBoxX;
            editField.y = editBoxY;
            editField.width = editBoxWidth;
            editField.height = editBoxHeight;
            drawRect(editBoxX, editBoxY, editBoxX + editBoxWidth, editBoxY + editBoxHeight, 0xFF383A40);
            editField.draw();
        }
        drawEmojiPanel(mouseX, mouseY);
        drawStickerPanel(mouseX, mouseY);
        drawEmojiAutocomplete(mouseX, mouseY);
        if (downloadMsg != null && System.currentTimeMillis() < downloadMsgUntil) {
            int toastBoxY = height - INPUT_HEIGHT - PADDING;
            int toastY = toastBoxY - 30;
            drawRect(SIDEBAR_WIDTH, toastY, width, toastY + 20, 0xE62B2D31);
            fontRendererObj.drawStringWithShadow(downloadMsg, SIDEBAR_WIDTH + PADDING, toastY + 6, 0xFFDCDDDE);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawReplyBanner(int mouseX, int mouseY) {
        if (pendingReply == null) return;
        int boxY = height - INPUT_HEIGHT - PADDING;
        int bx = SIDEBAR_WIDTH + PADDING;
        int by = boxY - 26;
        drawRect(bx, by, width - PADDING, boxY, 0xFF2B2D31);
        drawRect(bx, by, bx + 3, boxY, 0xFF5865F2);
        String label = "Replying to " + (pendingReply.authorDisplay != null ? pendingReply.authorDisplay : (pendingReply.author == null ? "a message" : pendingReply.author));
        fontRendererObj.drawStringWithShadow(label, bx + 10, by + 9, 0xFFB5BAC1);
        int cx = width - PADDING - 26;
        boolean hover = mouseX >= cx && mouseX <= cx + 18 && mouseY >= by + 4 && mouseY <= by + 20;
        fontRendererObj.drawStringWithShadow("X", cx + 6, by + 8, hover ? 0xFFFFFFFF : 0xFF949BA4);
        clickRects.add(new ClickRect(cx, by + 4, 18, 16, () -> pendingReply = null));
    }

    private void drawSidebar(int mouseX, int mouseY) {
        drawRect(0, 0, SIDEBAR_WIDTH, height, 0xFF2B2D31);
        fontRendererObj.drawStringWithShadow("CHANNELS", 10, 12, 0xFF949BA4);

        if (cachedChannelList == null || GlobalChat.channelsVersion != cachedChannelListVersion) {
            cachedChannelList = new ArrayList<>(GlobalChat.channels.values());
            cachedChannelList.sort(Comparator.comparing(a -> a.channelID));
            cachedChannelListVersion = GlobalChat.channelsVersion;
        }
        List<Channel> channelList = cachedChannelList;

        int y = 30;
        int bottomLimit = height - MARKDOWN_BAR_H;
        for (final Channel channel : channelList) {
            if (y + 26 > bottomLimit) break;
            boolean selected = channel == selectedChannel;
            boolean hovered = mouseX >= 4 && mouseX <= SIDEBAR_WIDTH - 4 && mouseY >= y && mouseY <= y + 26;

            if (selected) {
                drawRect(4, y, SIDEBAR_WIDTH - 4, y + 26, 0xFF404249);
                drawRect(0, y + 3, 4, y + 23, 0xFF5865F2);
            } else if (hovered) {
                drawRect(4, y, SIDEBAR_WIDTH - 4, y + 26, 0xFF35373C);
            }

            String label = fontRendererObj.trimStringToWidth("# " + channel.channelName, SIDEBAR_WIDTH - 22);
            fontRendererObj.drawStringWithShadow(label, 14, y + 9, selected ? 0xFFFFFFFF : 0xFFB5BAC1);

            clickRects.add(new ClickRect(4, y, SIDEBAR_WIDTH - 8, 26, () -> { selectedChannel = channel; scrollPixels = 0; }));

            y += 30;
        }

        drawMarkdownBar(mouseX, mouseY);
    }

    private void drawMarkdownBar(int mouseX, int mouseY) {
        int top = height - MARKDOWN_BAR_H;
        drawRect(0, top, SIDEBAR_WIDTH, height, 0xFF1E1F22);

        int gap = 6;
        int btnH = 20;
        String[][] rows = {
                {"Bold", "**"}, {"Italic", "*"}, {"Underline", "__"},
                {"Strikethrough", "~~"}, {"Spoiler", "||"}, {"Code", "`"}
        };
        int cols = 3;
        int bw = (SIDEBAR_WIDTH - 12 - gap * (cols - 1)) / cols;
        int y0 = top + 6;
        for (int i = 0; i < rows.length; i++) {
            int col = i % cols;
            int row = i / cols;
            int bx = 6 + col * (bw + gap);
            final String open = rows[i][1];
            drawMarkdownButton(bx, y0 + row * (btnH + gap), bw, btnH, rows[i][0], mouseX, mouseY,
                    () -> applyFormatting(open, open));
        }

        int codeY = y0 + 2 * (btnH + gap);
        drawMarkdownButton(6, codeY, SIDEBAR_WIDTH - 12, btnH, "Code Block", mouseX, mouseY,
                () -> applyCodeBlock(selectedCodeLang));

        int dby = codeY + btnH + gap;
        int dbw = SIDEBAR_WIDTH - 12;
        int dbh = 18;
        boolean dbHover = mouseX >= 6 && mouseX <= 6 + dbw && mouseY >= dby && mouseY <= dby + dbh;
        drawRect(6, dby, 6 + dbw, dby + dbh, dbHover ? 0xFF404249 : 0xFF2B2D31);
        String label = "``` " + selectedCodeLang + (langDropdownOpen ? " ▲" : " ▼");
        fontRendererObj.drawStringWithShadow(label, 11, dby + 5, 0xFFB5BAC1);
        clickRects.add(new ClickRect(6, dby, dbw, dbh, () -> langDropdownOpen = !langDropdownOpen));

        if (langDropdownOpen) {
            int rowsCount = (CODE_LANGUAGES.length + 1) / 2;
            int itemH = 11;
            int panelH = rowsCount * itemH + 4;
            int px = 4, py = dby - panelH - 4;
            drawRect(px - 2, py - 2, px + SIDEBAR_WIDTH - 4, dby - 2, 0xFF17181C);
            for (int i = 0; i < CODE_LANGUAGES.length; i++) {
                final String lang = CODE_LANGUAGES[i];
                int col = i / rowsCount;
                int row = i % rowsCount;
                int ix = px + 2 + col * ((SIDEBAR_WIDTH - 8) / 2);
                int iy = py + 2 + row * itemH;
                int iw = (SIDEBAR_WIDTH - 8) / 2 - 2;
                boolean hov = mouseX >= ix && mouseX <= ix + iw && mouseY >= iy && mouseY <= iy + itemH;
                if (hov) drawRect(ix, iy, ix + iw, iy + itemH, 0xFF35373C);
                fontRendererObj.drawStringWithShadow(lang, ix + 2, iy + 1,
                        lang.equals(selectedCodeLang) ? 0xFFFFFFFF : (hov ? 0xFFFFFFFF : 0xFFB5BAC1));
                clickRects.add(new ClickRect(ix, iy, iw, itemH, () -> {
                    selectedCodeLang = lang;
                    langDropdownOpen = false;
                    applyCodeBlock(lang);
                }));
            }
        }
    }

    private void drawMarkdownButton(int x, int y, int w, int h, String label, int mouseX, int mouseY, Runnable action) {
        boolean hover = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        drawRect(x, y, x + w, y + h, hover ? 0xFF404249 : 0xFF2B2D31);
        fontRendererObj.drawStringWithShadow(label,
                x + (w - fontRendererObj.getStringWidth(label)) / 2,
                y + (h - fontRendererObj.FONT_HEIGHT) / 2,
                hover ? 0xFFFFFFFF : 0xFFB5BAC1);
        clickRects.add(new ClickRect(x, y, w, h, action));
    }

    // -------------------------------------------------------------- emojis

    private int[] emojiButtonRect() {
        int boxY = height - INPUT_HEIGHT - PADDING;
        int ex = width - PADDING - EMOJI_BUTTON_W - 4;
        int ey = boxY + (INPUT_HEIGHT - EMOJI_BUTTON_W) / 2 + 1;
        return new int[]{ex, ey};
    }

    private boolean inEmojiButton(int mouseX, int mouseY) {
        int[] b = emojiButtonRect();
        return mouseX >= b[0] && mouseX <= b[0] + EMOJI_BUTTON_W && mouseY >= b[1] && mouseY <= b[1] + EMOJI_BUTTON_W;
    }

    private boolean inStickerButton(int mouseX, int mouseY) {
        int[] e = emojiButtonRect();
        int sx = e[0] - EMOJI_BUTTON_W - 2;
        return mouseX >= sx && mouseX <= sx + EMOJI_BUTTON_W && mouseY >= e[1] && mouseY <= e[1] + EMOJI_BUTTON_W;
    }

    private boolean inAttachButton(int mouseX, int mouseY) {
        int boxY = height - INPUT_HEIGHT - PADDING;
        int by = boxY + (INPUT_HEIGHT - EMOJI_BUTTON_W) / 2 + 1;
        int abx = SIDEBAR_WIDTH + PADDING + 4;
        return mouseX >= abx && mouseX <= abx + EMOJI_BUTTON_W && mouseY >= by && mouseY <= by + EMOJI_BUTTON_W;
    }

    private boolean inAnyButton(int mouseX, int mouseY) {
        return inEmojiButton(mouseX, mouseY) || inStickerButton(mouseX, mouseY) || inAttachButton(mouseX, mouseY);
    }

    private void closeAllPanels() {
        emojiPanelOpen = false;
        stickerPanelOpen = false;
        if (emojiSearchField != null) emojiSearchField.setFocused(false);
    }

    private void toggleEmojiPanel() {
        boolean open = !emojiPanelOpen;
        closeAllPanels();
        emojiPanelOpen = open;
    }

    private void toggleStickerPanel() {
        boolean open = !stickerPanelOpen;
        closeAllPanels();
        stickerPanelOpen = open;
        if (open && stickerScroll > 0) stickerScroll = 0;
    }

    private boolean inEmojiPanel(int mouseX, int mouseY) {
        if (!emojiPanelOpen) return false;
        int boxY = height - INPUT_HEIGHT - PADDING;
        int px = Math.max(SIDEBAR_WIDTH, width - PADDING - EMOJI_PANEL_W);
        int py = boxY - EMOJI_PANEL_TOTAL_H - 6;
        return mouseX >= px && mouseX <= px + EMOJI_PANEL_W && mouseY >= py && mouseY <= py + EMOJI_PANEL_TOTAL_H;
    }

    private boolean inStickerPanel(int mouseX, int mouseY) {
        if (!stickerPanelOpen) return false;
        int boxY = height - INPUT_HEIGHT - PADDING;
        int px = Math.max(SIDEBAR_WIDTH, width - PADDING - STICKER_PANEL_W);
        int py = boxY - STICKER_PANEL_H - 6;
        return mouseX >= px && mouseX <= px + STICKER_PANEL_W && mouseY >= py && mouseY <= py + STICKER_PANEL_H;
    }

    private boolean inAnyPanel(int mouseX, int mouseY) {
        return inEmojiPanel(mouseX, mouseY) || inStickerPanel(mouseX, mouseY);
    }

    private boolean inEmojiSearchBar(int mouseX, int mouseY) {
        if (!emojiPanelOpen) return false;
        int[] r = emojiSearchRect();
        return mouseX >= r[0] && mouseX <= r[0] + r[2] && mouseY >= r[1] && mouseY <= r[1] + r[3];
    }

    private int[] emojiSearchRect() {
        int boxY = height - INPUT_HEIGHT - PADDING;
        int px = Math.max(SIDEBAR_WIDTH, width - PADDING - EMOJI_PANEL_W);
        int py = boxY - EMOJI_PANEL_TOTAL_H - 6;
        return new int[]{px + 4, py + 4, EMOJI_PANEL_W - 8, 16};
    }

    // ------------------------------------------------------- panel sliders

    private boolean panelOpen(int panel) {
        return panel == PANEL_EMOJI ? emojiPanelOpen : stickerPanelOpen;
    }

    private int panelRowsTotal(int panel) {
        if (panel == PANEL_EMOJI) return (filteredEmojis().size() + EMOJI_PANEL_COLS - 1) / EMOJI_PANEL_COLS;
        return (sortedStickers().size() + STICKER_PANEL_COLS - 1) / STICKER_PANEL_COLS;
    }

    private int panelRowsVisible(int panel) {
        if (panel == PANEL_EMOJI) return EMOJI_PANEL_ROWS;
        return STICKER_PANEL_ROWS;
    }

    private int panelCellH(int panel) {
        if (panel == PANEL_EMOJI) return EMOJI_PANEL_CELL;
        return STICKER_PANEL_CELL;
    }

    private int panelMaxScroll(int panel) {
        return Math.max(0, panelRowsTotal(panel) - panelRowsVisible(panel));
    }

    private int panelScroll(int panel) {
        if (panel == PANEL_EMOJI) return emojiScroll;
        return stickerScroll;
    }

    private void setPanelScroll(int panel, int scroll) {
        int v = Math.max(0, Math.min(panelMaxScroll(panel), scroll));
        if (panel == PANEL_EMOJI) emojiScroll = v;
        else stickerScroll = v;
    }

    /** [x, y, w, h] of the scrollbar track inside a panel, or null-sized values when the panel is closed. */
    private int[] panelTrackRect(int panel) {
        int boxY = height - INPUT_HEIGHT - PADDING;
        if (panel == PANEL_EMOJI) {
            int px = Math.max(SIDEBAR_WIDTH, width - PADDING - EMOJI_PANEL_W);
            int py = boxY - EMOJI_PANEL_TOTAL_H - 6;
            int gridTop = py + EMOJI_SEARCH_H + 4;
            return new int[]{px + EMOJI_PANEL_W - 7, gridTop + 2, 3, (py + EMOJI_PANEL_TOTAL_H - 6) - (gridTop + 2)};
        }
        int px = Math.max(SIDEBAR_WIDTH, width - PADDING - STICKER_PANEL_W);
        int py = boxY - STICKER_PANEL_H - 6;
        return new int[]{px + STICKER_PANEL_W - 7, py + 4, 3, STICKER_PANEL_H - 8};
    }

    private int panelThumbHeight(int panel, int trackH) {
        int rowsVisible = panelRowsVisible(panel);
        int rowsTotal = Math.max(rowsVisible, panelRowsTotal(panel));
        float frac = rowsVisible / (float) rowsTotal;
        return Math.max(18, Math.min(trackH, Math.round(trackH * frac)));
    }

    private boolean panelSliderVisible(int panel) {
        return panelOpen(panel) && panelMaxScroll(panel) > 0;
    }

    /** Click on a panel scrollbar: drag the thumb or jump the list to the clicked position. Returns true if handled. */
    private boolean handlePanelSliderClick(int mouseX, int mouseY) {
        for (int p = PANEL_EMOJI; p <= PANEL_STICKER; p++) {
            if (!panelSliderVisible(p)) continue;
            int[] tr = panelTrackRect(p);
            if (mouseX < tr[0] || mouseX > tr[0] + tr[2] || mouseY < tr[1] || mouseY > tr[1] + tr[3]) continue;
            int maxScroll = panelMaxScroll(p);
            int thumbH = panelThumbHeight(p, tr[3]);
            int thumbY = tr[1] + Math.round((tr[3] - thumbH) * (panelScroll(p) / (float) maxScroll));
            if (mouseY >= thumbY && mouseY <= thumbY + thumbH) {
                dragPanel = p;
                dragGrabY = mouseY - thumbY;
            } else {
                setPanelScroll(p, Math.round((mouseY - tr[1]) / (float) tr[3] * maxScroll));
            }
            return true;
        }
        return false;
    }

    private void drawPanelSlider(int panel, int mouseX, int mouseY) {
        if (!panelSliderVisible(panel)) return;
        int[] tr = panelTrackRect(panel);
        int maxScroll = panelMaxScroll(panel);
        int thumbH = panelThumbHeight(panel, tr[3]);
        int thumbY = tr[1] + Math.round((tr[3] - thumbH) * (panelScroll(panel) / (float) maxScroll));
        drawRect(tr[0], tr[1], tr[0] + tr[2], tr[1] + tr[3], 0x14FFFFFF);
        boolean hover = mouseX >= tr[0] && mouseX <= tr[0] + tr[2] && mouseY >= thumbY && mouseY <= thumbY + thumbH;
        drawRect(tr[0], thumbY, tr[0] + tr[2], thumbY + thumbH, hover || dragPanel == panel ? 0xFF5865F2 : 0xFF5E6673);
    }

    // ------------------------------------------------------------ emojis

    private void drawEmojiPanel(int mouseX, int mouseY) {
        if (!emojiPanelOpen) return;
        List<EmojiRef> emojis = filteredEmojis();
        if (emojis.isEmpty()) return;

        int boxY = height - INPUT_HEIGHT - PADDING;
        int px = Math.max(SIDEBAR_WIDTH, width - PADDING - EMOJI_PANEL_W);
        int py = boxY - EMOJI_PANEL_TOTAL_H - 6;
        drawRect(px, py, px + EMOJI_PANEL_W, py + EMOJI_PANEL_TOTAL_H, 0xFF17181C);

        int[] sr = emojiSearchRect();
        drawRect(sr[0] - 2, sr[1] - 2, sr[0] + sr[2] + 2, sr[1] + sr[3] + 2, 0xFF232428);
        emojiSearchField.xPosition = sr[0];
        emojiSearchField.yPosition = sr[1];
        emojiSearchField.width = sr[2];
        emojiSearchField.height = sr[3];
        emojiSearchField.drawTextBox();
        if (emojiSearchField.getText().isEmpty() && !emojiSearchField.isFocused()) {
            fontRendererObj.drawStringWithShadow("Search emojis...", sr[0] + 2, sr[1] + 4, 0xFF6D6F78);
        }

        int gridTop = py + EMOJI_SEARCH_H + 4;
        int rowsTotal = (emojis.size() + EMOJI_PANEL_COLS - 1) / EMOJI_PANEL_COLS;
        int maxScroll = Math.max(0, rowsTotal - EMOJI_PANEL_ROWS);
        if (emojiScroll > maxScroll) emojiScroll = maxScroll;

        for (int r = 0; r < EMOJI_PANEL_ROWS; r++) {
            for (int c = 0; c < EMOJI_PANEL_COLS; c++) {
                int idx = (r + emojiScroll) * EMOJI_PANEL_COLS + c;
                if (idx >= emojis.size()) break;
                EmojiRef ref = emojis.get(idx);
                int cx = px + 4 + c * EMOJI_PANEL_CELL;
                int cy = gridTop + r * EMOJI_PANEL_CELL;
                boolean hover = mouseX >= cx && mouseX <= cx + EMOJI_PANEL_CELL && mouseY >= cy && mouseY <= cy + EMOJI_PANEL_CELL;
                if (hover) drawRect(cx, cy, cx + EMOJI_PANEL_CELL, cy + EMOJI_PANEL_CELL, 0xFF35373C);
                drawInlineEmoji(ref.url, ref.name, cx + (EMOJI_PANEL_CELL - 16) / 2, cy + (EMOJI_PANEL_CELL - 16) / 2);
                clickRects.add(new ClickRect(cx, cy, EMOJI_PANEL_CELL, EMOJI_PANEL_CELL,
                        () -> {
                            inputField.replaceSelection(":" + ref.name + ":");
                            if (emojiSearchField != null) emojiSearchField.setFocused(false);
                        }));
            }
        }
        drawPanelSlider(PANEL_EMOJI, mouseX, mouseY);
    }

    private List<EmojiRef> filteredEmojis() {
        List<EmojiRef> list = sortedEmojis();
        String query = emojiSearchField == null ? "" : emojiSearchField.getText().trim().toLowerCase();
        if (query.isEmpty()) {
            emojiFilterKey = query;
            emojiFilterCache = null;
            return list;
        }
        if (query.equals(emojiFilterKey)) return emojiFilterCache;
        List<EmojiRef> filtered = new ArrayList<>();
        for (EmojiRef ref : list) {
            if (ref.name != null && ref.name.toLowerCase().contains(query)) filtered.add(ref);
        }
        emojiFilterKey = query;
        emojiFilterCache = filtered;
        return filtered;
    }

    // ------------------------------------------------------------ stickers

    private List<Sticker> sortedStickers() {
        if (sortedStickerCache == null || GlobalChat.usableStickers.size() != sortedStickerCacheSize) {
            List<Sticker> list = new ArrayList<>(GlobalChat.usableStickers.values());
            list.sort(Comparator.comparing(a -> a.name == null ? "" : a.name));
            sortedStickerCache = list;
            sortedStickerCacheSize = list.size();
        }
        return sortedStickerCache;
    }

    /** Scrollable grid of all server stickers; clicking one sends it as a message. */
    private void drawStickerPanel(int mouseX, int mouseY) {
        if (!stickerPanelOpen) return;
        List<Sticker> stickers = sortedStickers();

        int boxY = height - INPUT_HEIGHT - PADDING;
        int px = Math.max(SIDEBAR_WIDTH, width - PADDING - STICKER_PANEL_W);
        int py = boxY - STICKER_PANEL_H - 6;
        drawRect(px, py, px + STICKER_PANEL_W, py + STICKER_PANEL_H, 0xFF17181C);

        if (stickers.isEmpty()) {
            fontRendererObj.drawStringWithShadow("No stickers available.", px + 8, py + 12, 0xFF6D6F78);
            return;
        }

        int rowsTotal = (stickers.size() + STICKER_PANEL_COLS - 1) / STICKER_PANEL_COLS;
        int maxScroll = Math.max(0, rowsTotal - STICKER_PANEL_ROWS);
        if (stickerScroll > maxScroll) stickerScroll = maxScroll;

        for (int r = 0; r < STICKER_PANEL_ROWS; r++) {
            for (int c = 0; c < STICKER_PANEL_COLS; c++) {
                int idx = (r + stickerScroll) * STICKER_PANEL_COLS + c;
                if (idx >= stickers.size()) break;
                Sticker st = stickers.get(idx);
                int cx = px + 4 + c * STICKER_PANEL_CELL;
                int cy = py + 4 + r * STICKER_PANEL_CELL;
                boolean hover = mouseX >= cx && mouseX <= cx + STICKER_PANEL_CELL && mouseY >= cy && mouseY <= cy + STICKER_PANEL_CELL;
                if (hover) drawRect(cx, cy, cx + STICKER_PANEL_CELL, cy + STICKER_PANEL_CELL, 0xFF35373C);
                drawImageBlock(st.name, st.url, cx + 3, cy + 3, STICKER_PANEL_CELL - 6, STICKER_PANEL_CELL - 6, false, mouseX, mouseY);
                clickRects.add(new ClickRect(cx, cy, STICKER_PANEL_CELL, STICKER_PANEL_CELL, () -> sendSticker(st)));
            }
        }
        drawPanelSlider(PANEL_STICKER, mouseX, mouseY);
    }

    private void sendSticker(Sticker st) {
        if (selectedChannel == null) return;
        ChatMessage message = new ChatMessage(st.url, selectedChannel.channelID, null);
        message.stickers.put(st.id, st);
        message.sendMessage();
        closeAllPanels();
    }

    // ---------------------------------------------------------- attachments

    /** Opens a file picker, uploads the chosen image/video/gif through the API, then inserts [file_name](file_url) into the input. */
    private void openAttachmentPicker() {
        if (attachUploading) return;
        attachUploading = true;
        CompletableFuture.runAsync(() -> {
            String url = null;
            String fileName = null;
            Embed fileEmbed = null;
            try {
                File file = pickAttachmentFile();
                if (file != null) {
                    MediaUploader.UploadResult result = MediaUploader.upload(file);
                    fileName = result.name;
                    url = result.url;
                    fileEmbed = new Embed("file", result.url);
                    fileEmbed.name = result.name;
                    fileEmbed.fileType = result.type;
                    fileEmbed.fileSize = result.size;
                }
            } catch (Exception e) {
                Aetheria.logger.warning("[G-Chat] Attachment upload failed: " + e.getMessage());
            }
            final String fUrl = url;
            final String fName = fileName;
            final Embed fEmbed = fileEmbed;
            mc.addScheduledTask(() -> {
                attachUploading = false;
                if (fUrl != null && !fUrl.isEmpty() && fName != null) {
                    inputField.replaceSelection("[" + fName + "](" + fUrl + ")");
                    if (fEmbed != null) pendingFileEmbeds.add(fEmbed);
                }
            });
        });
    }

    private File pickAttachmentFile() {
        final File[] chosen = new File[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle("Attach an image, video or GIF");
                chooser.setFileFilter(new FileNameExtensionFilter("Images & videos (PNG, JPG, GIF, WEBP, MP4, WEBM...)",
                        "png", "jpg", "jpeg", "gif", "webp", "bmp",
                        "mp4", "webm", "mkv", "mov", "avi", "m4v", "wmv", "flv", "ts"));
                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    chosen[0] = chooser.getSelectedFile();
                }
            });
        } catch (Exception e) {
            Aetheria.logger.warning("[G-Chat] File picker failed: " + e.getMessage());
        }
        return chosen[0];
    }

    /** Discord-style suggestion dropdown while typing ":prefix" (no closing colon yet). */
    private void drawEmojiAutocomplete(int mouseX, int mouseY) {
        String text = inputField.getText();
        int caret = inputField.getCaret();
        int start = caret;
        while (start > 0 && isEmojiWordChar(text.charAt(start - 1))) start--;
        if (start == caret || start <= 0 || text.charAt(start - 1) != ':') return;
        String prefix = text.substring(start, caret);

        List<EmojiRef> matches = new ArrayList<>();
        for (Map.Entry<String, IEmoji> entry : GlobalChat.usableEmojis.entrySet()) {
            if (entry.getKey().startsWith(prefix)) matches.add(entry.getValue().toEmoji());
        }
        if (matches.isEmpty()) return;
        matches.sort(Comparator.comparing(a -> a.name));
        if (matches.size() > 8) matches = new ArrayList<>(matches.subList(0, 8));

        int dw = 200;
        int boxY = height - INPUT_HEIGHT - PADDING;
        int dx = Math.max(SIDEBAR_WIDTH, Math.min(inputField.x, width - PADDING - dw));
        int rowH = 18;
        int dh = matches.size() * rowH + 6;
        int dy = boxY - dh - 6;
        drawRect(dx, dy, dx + dw, dy + dh, 0xFF17181C);
        for (int i = 0; i < matches.size(); i++) {
            EmojiRef ref = matches.get(i);
            int ry = dy + 3 + i * rowH;
            boolean hover = mouseX >= dx && mouseX <= dx + dw && mouseY >= ry && mouseY <= ry + rowH;
            if (hover) drawRect(dx, ry, dx + dw, ry + rowH, 0xFF35373C);
            drawInlineEmoji(ref.url, ref.name, dx + 4, ry + 1);
            fontRendererObj.drawStringWithShadow(":" + ref.name + ":", dx + 24, ry + 5, hover ? 0xFFFFFFFF : 0xFFB5BAC1);
            final int selStart = start - 1;
            final int selEnd = caret;
            clickRects.add(new ClickRect(dx, ry, dw, rowH, () -> {
                inputField.select(selStart, selEnd);
                inputField.replaceSelection(":" + ref.name + ":");
            }));
        }
    }

    private List<EmojiRef> sortedEmojis() {
        if (sortedEmojiCache == null || GlobalChat.usableEmojis.size() != sortedEmojiCacheSize) {
            List<EmojiRef> list = new ArrayList<>();
            for (IEmoji emoji : GlobalChat.usableEmojis.values()) {
                list.add(emoji.toEmoji());
            }
            list.sort(Comparator.comparing(a -> a.name));
            sortedEmojiCache = list;
            sortedEmojiCacheSize = list.size();
        }
        return sortedEmojiCache;
    }

    private static boolean isEmojiWordChar(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_' || c == '~';
    }

    private void applyFormatting(String open, String close) {
        String sel = inputField.hasSelection() ? inputField.getSelectedText() : "";
        if (sel.isEmpty()) {
            inputField.replaceSelection(open + close);
            inputField.setCaret(inputField.getCaret() - close.length());
        } else {
            inputField.replaceSelection(open + sel + close);
        }
    }

    private void applyCodeBlock(String lang) {
        String sel = inputField.hasSelection() ? inputField.getSelectedText() : "";
        if (sel.isEmpty()) {
            inputField.replaceSelection("```" + lang + "\n\n```");
            inputField.setCaret(inputField.getCaret() - 4);
        } else {
            inputField.replaceSelection("```" + lang + "\n" + sel + "\n```");
        }
    }

    private void drawHeader(int mouseX, int mouseY) {
        drawRect(SIDEBAR_WIDTH, 0, width, HEADER_HEIGHT, 0xFF2B2D31);
        drawRect(SIDEBAR_WIDTH, HEADER_HEIGHT - 1, width, HEADER_HEIGHT, 0xFF1E1F22);

        String title = selectedChannel != null ? ("# " + selectedChannel.channelName) : "No channel selected";
        fontRendererObj.drawStringWithShadow(title, SIDEBAR_WIDTH + PADDING, (HEADER_HEIGHT - fontRendererObj.FONT_HEIGHT) / 2f, 0xFFFFFFFF);
        if (selectedChannel != null && !selectedChannel.canSend) {
            String badge = "READ ONLY";
            int bx = SIDEBAR_WIDTH + PADDING + fontRendererObj.getStringWidth(title) + 10;
            drawRect(bx, 7, bx + fontRendererObj.getStringWidth(badge) + 10, HEADER_HEIGHT - 7, 0xFF4E5058);
            fontRendererObj.drawStringWithShadow(badge, bx + 5, (HEADER_HEIGHT - fontRendererObj.FONT_HEIGHT) / 2f, 0xFFB5BAC1);
        }

        int closeX = width - 24;
        boolean closeHover = mouseX >= closeX && mouseX < closeX + 16 && mouseY >= 7 && mouseY < 23;
        fontRendererObj.drawStringWithShadow("X", closeX + 4, 10, closeHover ? 0xFFFFFFFF : 0xFF949BA4);
        clickRects.add(new ClickRect(closeX, 7, 16, 16, () -> mc.displayGuiScreen(null)));
    }

    /** Renders server/system notices (mute/ban/permission errors) in the message area so they are visible
     *  even when the user has no channels. Shows up to the 3 most recent. */
    private void drawSystemNotices(int mouseX, int mouseY) {
        List<String> notices = GlobalChat.systemNotices;
        if (notices.isEmpty()) return;

        int areaX = SIDEBAR_WIDTH + PADDING;
        int boxW = width - SIDEBAR_WIDTH - PADDING * 2;
        int lines = Math.min(3, notices.size());
        int lineH = 18;
        int boxH = lines * lineH + 12;
        int boxY = HEADER_HEIGHT + 6;

        drawRect(areaX - 4, boxY, areaX - 4 + boxW + 8, boxY + boxH, 0xFF232428);
        drawRect(areaX - 4, boxY, areaX - 2, boxY + boxH, 0xFF5865F2);
        fontRendererObj.drawStringWithShadow("SYSTEM", areaX, boxY + 4, 0xFF949BA4);

        int start = Math.max(0, notices.size() - lines);
        int y = boxY + 4 + lineH;
        for (int i = start; i < notices.size(); i++) {
            String text = notices.get(i);
            String trimmed = fontRendererObj.trimStringToWidth(text, boxW - 8);
            fontRendererObj.drawStringWithShadow(trimmed, areaX, y, 0xFFDCDDDE);
            y += lineH;
        }

        int closeX = areaX - 4 + boxW + 4;
        boolean hover = mouseX >= closeX && mouseX <= closeX + 16 && mouseY >= boxY && mouseY <= boxY + 16;
        fontRendererObj.drawStringWithShadow("X", closeX + 4, boxY + 4, hover ? 0xFFFFFFFF : 0xFF949BA4);
        clickRects.add(new ClickRect(closeX - 12, boxY + 2, 28, 16, () -> dismissSystemNotices()));
    }

    private void dismissSystemNotices() {
        if (!GlobalChat.systemNotices.isEmpty()) {
            GlobalChat.systemNotices.remove(GlobalChat.systemNotices.size() - 1);
            GlobalChat.systemNoticesVersion++;
        }
    }

    private void drawInputArea(int mouseX, int mouseY) {
        int boxY = height - INPUT_HEIGHT - PADDING;
        drawRect(SIDEBAR_WIDTH, boxY - PADDING, width, height, 0xFF313338);
        drawRect(SIDEBAR_WIDTH + PADDING, boxY, width - PADDING, boxY + INPUT_HEIGHT, 0xFF383A40);

        if (selectedChannel != null && !selectedChannel.canSend) {
            fontRendererObj.drawStringWithShadow("Read only channel - you cannot send messages here.", SIDEBAR_WIDTH + PADDING + 10, boxY + (INPUT_HEIGHT - fontRendererObj.FONT_HEIGHT) / 2f + 1, 0xFF949BA4);
            return;
        }

        int ex = width - PADDING - EMOJI_BUTTON_W - 4;
        int by = boxY + (INPUT_HEIGHT - EMOJI_BUTTON_W) / 2 + 1;
        int sx = ex - EMOJI_BUTTON_W - 2;

        int abx = SIDEBAR_WIDTH + PADDING + 4;
        boolean attachHover = mouseX >= abx && mouseX <= abx + EMOJI_BUTTON_W && mouseY >= by && mouseY <= by + EMOJI_BUTTON_W;
        drawRect(abx, by, abx + EMOJI_BUTTON_W, by + EMOJI_BUTTON_W, attachHover ? 0xFF404249 : 0xFF2B2D31);
        int ax = abx + (EMOJI_BUTTON_W - 14) / 2;
        int ay = by + (EMOJI_BUTTON_W - 14) / 2;
        drawRect(ax + 4, ay + 6, ax + 10, ay + 8, 0xFFF2C94C);
        drawRect(ax + 6, ay + 4, ax + 8, ay + 10, 0xFFF2C94C);
        clickRects.add(new ClickRect(abx, by, EMOJI_BUTTON_W, EMOJI_BUTTON_W, this::openAttachmentPicker));
        if (attachUploading) {
            fontRendererObj.drawStringWithShadow("Uploading...", abx + EMOJI_BUTTON_W + 8, by + (EMOJI_BUTTON_W - fontRendererObj.FONT_HEIGHT) / 2f + 1, 0xFFF2C94C);
        }

        boolean stickerHover = mouseX >= sx && mouseX <= sx + EMOJI_BUTTON_W && mouseY >= by && mouseY <= by + EMOJI_BUTTON_W;

        drawRect(sx, by, sx + EMOJI_BUTTON_W, by + EMOJI_BUTTON_W, stickerHover || stickerPanelOpen ? 0xFF404249 : 0xFF2B2D31);
        int sfx = sx + (EMOJI_BUTTON_W - 14) / 2;
        int sfy = by + (EMOJI_BUTTON_W - 14) / 2;
        drawRect(sfx, sfy, sfx + 11, sfy + 12, 0xFFF2C94C);
        drawRect(sfx + 7, sfy, sfx + 11, sfy + 4, 0xFFF2C94C);
        drawRect(sfx + 7, sfy, sfx + 9, sfy + 2, 0xFF2B2D31);
        drawRect(sfx + 3, sfy + 6, sfx + 4, sfy + 7, 0xFF1E1F22);
        drawRect(sfx + 6, sfy + 6, sfx + 7, sfy + 7, 0xFF1E1F22);

        boolean emojiHover = mouseX >= ex && mouseX <= ex + EMOJI_BUTTON_W && mouseY >= by && mouseY <= by + EMOJI_BUTTON_W;
        drawRect(ex, by, ex + EMOJI_BUTTON_W, by + EMOJI_BUTTON_W, emojiHover || emojiPanelOpen ? 0xFF404249 : 0xFF2B2D31);
        int fx = ex + (EMOJI_BUTTON_W - 14) / 2;
        int fy = by + (EMOJI_BUTTON_W - 14) / 2;
        drawRect(fx, fy, fx + 14, fy + 14, 0xFFF2C94C);
        drawRect(fx + 3, fy + 4, fx + 5, fy + 6, 0xFF1E1F22);
        drawRect(fx + 9, fy + 4, fx + 11, fy + 6, 0xFF1E1F22);
        drawRect(fx + 4, fy + 9, fx + 10, fy + 10, 0xFF1E1F22);
        clickRects.add(new ClickRect(sx, by, EMOJI_BUTTON_W, EMOJI_BUTTON_W, () -> toggleStickerPanel()));
        clickRects.add(new ClickRect(ex, by, EMOJI_BUTTON_W, EMOJI_BUTTON_W, () -> toggleEmojiPanel()));
    }

    private void drawMessages(int mouseX, int mouseY) {
        editBoxVisible = false;
        if (layoutCache.size() > 2048) {
            layoutCache.clear();
            revealedSpoilers.clear();
        }
        int areaX = SIDEBAR_WIDTH;
        int areaY = HEADER_HEIGHT;
        int areaW = width - SIDEBAR_WIDTH;
        int areaBottom = height - INPUT_HEIGHT - PADDING * 2 - (pendingReply != null ? 26 : 0);
        int contentBottom = areaBottom - MESSAGE_BOTTOM_PAD;
        int areaH = areaBottom - areaY;
        if (areaH <= 0) return;

        if (selectedChannel == null) {
            fontRendererObj.drawStringWithShadow("No channels available.", areaX + PADDING, areaY + PADDING, 0xFF949BA4);
            return;
        }

        List<ChatLine> lines = selectedChannel.messageHistory;
        int contentWidth = Math.max(60, areaW - PADDING * 2 - AVATAR_SIZE - 8);

        enableScissor(areaX, areaY, areaW, areaH);

        int cursorBottom = contentBottom + scrollPixels;
        int total = 0;
        jumpTargetTop = Integer.MIN_VALUE;

        for (int i = lines.size() - 1; i >= 0; i--) {
            ChatMessage msg = lines.get(i).message;
            if (msg == null) continue;

            ChatMessage older = (i > 0) ? lines.get(i - 1).message : null;
            boolean groupStart = older == null || older.author == null || msg.author == null
                    || !older.author.equals(msg.author) || (msg.timestamp - older.timestamp) > GROUP_GAP_MS;

            LayoutCache layout = getLayout(msg, contentWidth);
            int bh = blockHeight(msg, layout, groupStart, contentWidth);
            int top = cursorBottom - bh;

            if (top < areaBottom && cursorBottom > areaY) {
                renderMessage(msg, layout, areaX + PADDING, top, contentWidth, groupStart, mouseX, mouseY);
            }

            if (jumpToMessageId != null && msg.discordID != null && msg.discordID.equals(jumpToMessageId)) {
                jumpTargetTop = top - scrollPixels;
            }

            int gap = groupStart ? MESSAGE_GAP : CONTINUATION_GAP;
            total += bh + gap;
            cursorBottom = top - gap;
        }

        disableScissor();

        int maxScroll = Math.max(0, total + MESSAGE_BOTTOM_PAD - areaH);
        if (jumpToMessageId != null) {
            if (jumpTargetTop != Integer.MIN_VALUE) {
                scrollPixels = areaY - jumpTargetTop;
                highlightMessageId = jumpToMessageId;
                jumpHighlightStart = System.currentTimeMillis();
            }
            jumpToMessageId = null;
        }
        if (scrollPixels > maxScroll) scrollPixels = maxScroll;
        if (scrollPixels < 0) scrollPixels = 0;

        if (total > areaH) {
            drawScrollbar(areaX, areaY, areaW, areaH, total);
        }
    }

    private void drawScrollbar(int areaX, int areaY, int areaW, int areaH, int totalHeight) {
        int trackX = areaX + areaW - 8;
        drawRect(trackX, areaY, trackX + 6, areaY + areaH, 0x22000000);
        float ratio = areaH / (float) totalHeight;
        int thumbH = Math.max(20, Math.round(areaH * ratio));
        int maxScroll = Math.max(1, totalHeight - areaH);
        float scrollRatio = scrollPixels / (float) maxScroll;
        int thumbY = areaY + Math.round((areaH - thumbH) * (1f - scrollRatio));
        drawRect(trackX, thumbY, trackX + 6, thumbY + thumbH, 0x66FFFFFF);
    }

    // ------------------------------------------------------------ messages

    private int replyAllowance(ChatMessage msg) {
        return msg.replying ? fontRendererObj.FONT_HEIGHT + 3 : 0;
    }

    /** Content to render: for replies, drops the leading "https://discord.com/channels/..." hyperlink prefix the server prepends on Discord. */
    private String displayContent(ChatMessage msg) {
        String content = msg.content;
        if (content != null && msg.replying && msg.replyingMessage != null && !msg.replyingMessage.isEmpty()
                && msg.channelId != null) {
            String prefix = "https://discord.com/channels/1479556885769093192/" + msg.channelId + "/" + msg.replyingMessage;
            if (content.startsWith(prefix)) {
                int end = content.indexOf('\n', prefix.length());
                if (end < 0) end = content.length();
                content = content.substring(end).trim();
            }
        }
        return content;
    }

    /**
     * Click on a link: plain click on a Discord message link jumps to that message in the UI when it is
     * in a global channel's local history (switching channels if needed); shift+click (or any non-message link)
     * opens the link-confirm screen for opening it in Discord.
     */
    private void handleLinkClick(String url) {
        if (url == null) return;
        boolean shift = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
        if (!shift && url.startsWith("https://discord.com/channels/")) {
            String[] parts = url.split("/");
            if (parts.length >= 2) {
                String channelId = parts[parts.length - 2];
                String messageId = parts[parts.length - 1];
                Channel channel = GlobalChat.channels.get(channelId);
                if (channel != null) {
                    for (ChatLine line : channel.messageHistory) {
                        if (line.message != null && messageId.equals(line.message.discordID)) {
                            if (channel != selectedChannel) selectedChannel = channel;
                            jumpToMessageId = messageId;
                            return;
                        }
                    }
                }
            }
        }
        pendingLinkUrl = url;
    }

    private int headerAllowance(boolean groupStart) {
        if (!groupStart) return 2;
        return 2 + fontRendererObj.FONT_HEIGHT + DiscordMarkdown.LINE_SPACING;
    }

    private int blockHeight(ChatMessage msg, LayoutCache layout, boolean groupStart, int contentWidth) {
        int contentH = layout.totalHeight;
        if (singleMediaLink(msg, layout)) contentH -= layout.textHeight;
        if (!layout.imgs.isEmpty()) contentH += imageGridHeight(layout.imgs, contentWidth) + 6;
        return replyAllowance(msg) + headerAllowance(groupStart) + contentH;
    }

    /** Height of the image area (grid or single image), not including the 6px trailing gap. Computed live so it
     *  updates as images load; grid cells are fixed-size so multi-image layouts are stable while loading. */
    private int imageGridHeight(List<ImageRef> imgs, int maxWidth) {
        if (imgs.size() == 1) {
            GCImage img = getImage(imgs.get(0).url, false);
            if (img != null && img.isLoaded && img.width > 0 && img.height > 0) {
                return naturalImageHeight(img, maxWidth);
            }
            return ATTACHMENT_BOX_H;
        }
        int cell = imageGridCell(maxWidth);
        int rows = (imgs.size() + IMAGE_GRID_COLS - 1) / IMAGE_GRID_COLS;
        return rows * cell + (rows - 1) * IMAGE_GRID_GAP;
    }

    private static int imageGridCell(int maxWidth) {
        return Math.min((maxWidth - IMAGE_GRID_GAP) / 2, 220);
    }

    /** Natural drawn height of a loaded image (width-capped, aspect preserved, height capped too). */
    private int naturalImageHeight(GCImage img, int maxWidth) {
        float ratio = img.width / (float) img.height;
        int drawW = Math.min(img.width, Math.min(maxWidth, MAX_IMAGE_DRAW_W));
        int drawH = Math.round(drawW / ratio);
        if (drawH > MAX_IMAGE_DRAW_H) {
            drawH = MAX_IMAGE_DRAW_H;
            drawW = Math.round(drawH * ratio);
        }
        return drawH;
    }

    /** True when the message is a single bare link or hyperlink to an image/video/GIF (hide the link text, show only the media). */
    private boolean singleMediaLink(ChatMessage msg, LayoutCache layout) {
        if (layout.lines.size() != 1) return false;
        List<Span> spans = layout.lines.get(0).spans;
        if (spans == null || spans.size() != 1) return false;
        Span span = spans.get(0);
        if (span.linkUrl == null) return false;
        if (layout.embeds.size() != 1) return false;
        String type = layout.embeds.get(0).type;
        return "image".equals(type) || "video".equals(type) || "file".equals(type);
    }

    private void renderMessage(ChatMessage msg, LayoutCache layout, int x, int y, int contentWidth, boolean groupStart, int mouseX, int mouseY) {
        int textX = x + AVATAR_SIZE + 8;
        int replyAlw = replyAllowance(msg);
        int headerAlw = headerAllowance(groupStart);
        boolean hideContent = singleMediaLink(msg, layout);
        int rowHeight = replyAlw + headerAlw + layout.totalHeight - (hideContent ? layout.textHeight : 0);

        boolean hovered = mouseX >= x - 4 && mouseX < x + AVATAR_SIZE + 8 + contentWidth + PADDING
                && mouseY >= y - 2 && mouseY < y + rowHeight;
        if (hovered) {
            drawRect(x - 4, y - 2, x + AVATAR_SIZE + 8 + contentWidth + PADDING, y + rowHeight, 0x14FFFFFF);
            drawMessageActions(msg, x + AVATAR_SIZE + 8 + contentWidth, y, mouseX, mouseY);
        }

        if (highlightMessageId != null && highlightMessageId.equals(msg.discordID)) {
            long elapsed = System.currentTimeMillis() - jumpHighlightStart;
            if (elapsed >= JUMP_HIGHLIGHT_MS) {
                highlightMessageId = null;
            } else {
                float fade = 1f - elapsed / (float) JUMP_HIGHLIGHT_MS;
                int alpha = (int) (fade * (0.35f + 0.65f * Math.abs(Math.sin(elapsed / 150.0))) * 36);
                if (alpha > 2) {
                    drawRect(x - 4, y - 2, x + AVATAR_SIZE + 8 + contentWidth + PADDING, y + rowHeight,
                            (alpha << 24) | 0xFFFFFF);
                }
            }
        }

        if (msg.replying) {
            ChatMessage original = findByDiscordId(msg.replyingMessage);
            String replyName = original != null && original.authorDisplay != null ? original.authorDisplay : (original != null && original.author != null ? original.author : "a message");
            String replyLabel = "\u21B0 " + replyName;
            fontRendererObj.drawStringWithShadow(replyLabel, textX, y + 1, 0xFF949BA4);
            if (msg.replyingMessage != null && !msg.replyingMessage.isEmpty()) {
                final String replyUrl = "https://discord.com/channels/1479556885769093192/"
                        + msg.channelId + "/" + msg.replyingMessage;
                clickRects.add(new ClickRect(textX, y + 1, fontRendererObj.getStringWidth(replyLabel),
                        fontRendererObj.FONT_HEIGHT, () -> handleLinkClick(replyUrl)));
            }
        }

        int afterReplyY = y + replyAlw;

        if (groupStart) {
            drawAvatar(msg, x, afterReplyY);
            String author = msg.authorDisplay != null ? msg.authorDisplay : (msg.author == null ? "Unknown" : msg.author);
            int nameColor = userColor(msg.author == null ? "?" : msg.author);
            fontRendererObj.drawStringWithShadow(author, textX, afterReplyY + 2, nameColor);
            int nameWidth = fontRendererObj.getStringWidth(author);
            String headerTime = formatTimestamp(msg);
            if (msg.client != null && !msg.client.isEmpty()) headerTime += " (" + msg.client + ")";
            if (msg.edited) headerTime += " (edited)";
            fontRendererObj.drawStringWithShadow(headerTime, textX + nameWidth + 8, afterReplyY + 3, 0xFF949BA4);
        } else if (hovered) {
            String time = formatTimeShort(msg);
            if (msg.edited) time += " (edited)";
            fontRendererObj.drawStringWithShadow(time, x, afterReplyY + 2, 0xFF6D6F78);
        }

        int cursorY = afterReplyY + headerAlw;

        if (!hideContent) {
            for (int li = 0; li < layout.lines.size(); li++) {
                RenderLine line = layout.lines.get(li);
                if (line.codeTokens != null) {
                    drawCodeLine(line, textX, cursorY, contentWidth);
                } else {
                    drawRenderLine(line, textX, cursorY, contentWidth);
                }
                if (line.type == LineType.CODE_BLOCK
                        && (li == 0 || layout.lines.get(li - 1).type != LineType.CODE_BLOCK)) {
                    drawCodeCopyButton(layout.lines, li, textX, cursorY, contentWidth, mouseX, mouseY);
                }
                cursorY += DiscordMarkdown.lineHeight(line.type, fontRendererObj);
            }
        }

        List<ImageRef> imgs = layout.imgs;
        if (!imgs.isEmpty()) {
            if (!layout.lines.isEmpty()) cursorY += 6;
            drawImageGrid(imgs, textX, cursorY, contentWidth, mouseX, mouseY);
            cursorY += imageGridHeight(imgs, contentWidth) + 6;
        }

        if (msg.attachments != null && !msg.attachments.isEmpty()) {
            for (Attachment att : msg.attachments) {
                if (!isImageAttachment(att)) {
                    drawEmbedBlock(att, msg, textX, cursorY, Math.min(contentWidth, 360), mouseX, mouseY);
                    cursorY += EMBED_BOX_H + 6;
                }
            }
        }

        if (!msg.stickers.isEmpty()) {
            for (Sticker st : msg.stickers.values()) {
                drawImageBlock(st.name, st.url, textX, cursorY, Math.min(contentWidth, 96), STICKER_BOX_H, false, mouseX, mouseY);
                cursorY += STICKER_BOX_H + 6;
            }
        }

        for (Embed embed : layout.embeds) {
            if ("image".equals(embed.type)) {
                // already drawn as part of the image grid above
            } else if ("file".equals(embed.type) || "video".equals(embed.type)) {
                drawFileEmbed(embed, textX, cursorY, Math.min(contentWidth, 360), mouseX, mouseY);
                cursorY += EMBED_BOX_H + 6;
            } else if ("website".equals(embed.type) || "rich".equals(embed.type)) {
                int ew = Math.min(contentWidth, 360);
                int eh = embedHeight(embed, ew);
                drawWebEmbed(embed, textX, cursorY, ew, eh, mouseX, mouseY);
                cursorY += eh + 6;
            }
        }

        if (msg == editingMessage) {
            editBoxX = x - 4;
            editBoxY = y - 2;
            editBoxWidth = AVATAR_SIZE + 8 + contentWidth + PADDING + 8;
            editBoxHeight = rowHeight + 4;
            editBoxVisible = true;
        }
    }

    /** Reply/Edit/Delete buttons shown on hover at the top-right of a message block. */
    private void drawMessageActions(ChatMessage msg, int rightEdge, int blockTop, int mouseX, int mouseY) {
        List<String> labels = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        if (isOwnMessage(msg)) {
            labels.add("Edit");
            actions.add(() -> startEdit(msg));
            labels.add("Delete");
            actions.add(() -> deleteMessageAction(msg));
        }
        labels.add("Reply");
        actions.add(() -> pendingReply = msg);

        int gap = 4;
        int bh = 15;
        int totalW = 0;
        List<Integer> widths = new ArrayList<>();
        for (String label : labels) {
            int w = fontRendererObj.getStringWidth(label) + 12;
            widths.add(w);
            totalW += w + gap;
        }
        int bx = rightEdge - totalW + gap;
        int by = blockTop + 1;
        for (int i = 0; i < labels.size(); i++) {
            int w = widths.get(i);
            boolean hover = mouseX >= bx && mouseX <= bx + w && mouseY >= by && mouseY <= by + bh;
            drawRect(bx, by, bx + w, by + bh, hover ? 0xFF404249 : 0xFF2B2D31);
            fontRendererObj.drawStringWithShadow(labels.get(i), bx + 6, by + 3,
                    hover ? 0xFFFFFFFF : 0xFFB5BAC1);
            clickRects.add(new ClickRect(bx, by, w, bh, actions.get(i)));
            bx += w + gap;
        }
    }

    private void drawRenderLine(RenderLine line, int x, int y, int maxWidth) {
        int lh = DiscordMarkdown.lineHeight(line.type, fontRendererObj);
        x += line.listIndent;
        maxWidth -= line.listIndent;

        if (line.type == LineType.QUOTE) {
            drawRect(x - 8, y, x - 6, y + lh - 2, 0xFF4E5058);
        } else if (line.type == LineType.CODE_BLOCK) {
            drawRect(x - 4, y - 1, x + maxWidth + 4, y + lh - 1, 0xFF232428);
        }

        int color = line.type == LineType.CODE_BLOCK ? 0xFFDCDDDE
                : line.type == LineType.QUOTE ? 0xFFB5BAC1
                  : line.type == LineType.SUBHEADER ? 0xFF949BA4
                    : 0xFFF2F3F5;

        float scale = DiscordMarkdown.lineScale(line.type);
        if (scale != 1f) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(x, y, 0f);
            GlStateManager.scale(scale, scale, 1f);
            drawSpans(line.spans, 0, 0, color, false);
            GlStateManager.popMatrix();
        } else {
            drawSpans(line.spans, x, y, color, true);
        }
    }

    /** Draws a syntax-highlighted code line from pre-tokenized {@link CodeParser.HighlightedToken}s. */
    private void drawCodeLine(RenderLine line, int x, int y, int maxWidth) {
        int lh = DiscordMarkdown.lineHeight(line.type, fontRendererObj);
        drawRect(x - 4, y - 1, x + maxWidth + 4, y + lh - 1, 0xFF232428);
        int cx = x;
        for (CodeParser.HighlightedToken token : line.codeTokens) {
            fontRendererObj.drawStringWithShadow(token.text, cx, y, 0xFF000000 | token.mcColor);
            cx += fontRendererObj.getStringWidth(token.text);
        }
    }

    /** Draws a Discord-style "Copy" button on the top-right of a fenced code block; clicking copies the code to the clipboard. */
    private void drawCodeCopyButton(List<RenderLine> lines, int startIdx, int x, int y, int maxWidth, int mouseX, int mouseY) {
        StringBuilder sb = new StringBuilder();
        for (int i = startIdx; i < lines.size() && lines.get(i).type == LineType.CODE_BLOCK; i++) {
            RenderLine rl = lines.get(i);
            if (rl.codeTokens != null) {
                for (CodeParser.HighlightedToken token : rl.codeTokens) {
                    sb.append(token.text);
                }
            } else {
                for (Span span : rl.spans) {
                    if (span.spaceBefore) sb.append(' ');
                    sb.append(span.text);
                }
            }
            sb.append('\n');
        }
        while (sb.length() > 0 && sb.charAt(sb.length() - 1) == '\n') {
            sb.setLength(sb.length() - 1);
        }
        final String codeText = sb.toString();

        boolean copied = System.currentTimeMillis() - lastCopyTime < 1000;
        String label = copied ? "Copied" : "Copy";
        int labelWidth = fontRendererObj.getStringWidth(label);
        int bw = labelWidth + 12;
        int bh = fontRendererObj.FONT_HEIGHT + 4;
        int bx = x + maxWidth - bw;
        int by = y - 1;
        boolean hover = mouseX >= bx && mouseX <= bx + bw && mouseY >= by && mouseY <= by + bh;

        drawRect(bx, by, bx + bw, by + bh, hover ? 0x66FFFFFF : 0x30FFFFFF);
        fontRendererObj.drawStringWithShadow(label, bx + 6, by + 2, hover ? 0xFFFFFFFF : 0xFF949BA4);
        if (!copied) {
            clickRects.add(new ClickRect(bx, by, bw, bh, () -> {
                lastCopyTime = System.currentTimeMillis();
                GuiScreen.setClipboardString(codeText);
            }));
        }
    }

    /** registerClicks is disabled for scaled (header) text since click rects would need extra transform math. */
    private void drawSpans(List<Span> spans, int x, int y, int baseColor, boolean registerClicks) {
        int cursorX = x;
        int spaceWidth = fontRendererObj.getStringWidth(" ");
        for (final Span span : spans) {
            if (span.spaceBefore) cursorX += spaceWidth;

            if (span.imageUrl != null) {
                drawInlineEmoji(span.imageUrl, span.emojiName, cursorX, y);
                cursorX += DiscordMarkdown.EMOJI_SIZE;
                continue;
            }

            String formatted = DiscordMarkdown.toFormatted(span);
            int w = fontRendererObj.getStringWidth(formatted);

            if (span.linkUrl != null) {
                if (span.plainLink) {
                    fontRendererObj.drawStringWithShadow(formatted, cursorX, y, baseColor);
                } else {
                    fontRendererObj.drawStringWithShadow(formatted, cursorX, y, 0xFF00A8FC);
                    drawRect(cursorX, y + fontRendererObj.FONT_HEIGHT - 1, cursorX + w, y + fontRendererObj.FONT_HEIGHT, 0xFF00A8FC);
                }
                if (registerClicks) {
                    int rx = cursorX, ry = y, rw = w, rh = fontRendererObj.FONT_HEIGHT;
                    clickRects.add(new ClickRect(rx, ry, rw, rh, () -> handleLinkClick(span.linkUrl)));
                }
                cursorX += w;
                continue;
            }

            if (span.spoiler && !revealedSpoilers.contains(span)) {
                drawRect(cursorX, y, cursorX + Math.max(w, 4), y + fontRendererObj.FONT_HEIGHT, 0xFF1E1F22);
                if (registerClicks) {
                    int rx = cursorX, ry = y, rw = Math.max(w, 4), rh = fontRendererObj.FONT_HEIGHT;
                    clickRects.add(new ClickRect(rx, ry, rw, rh, () -> revealedSpoilers.add(span)));
                }
            } else {
                int color = span.code ? 0xFF95D8A6 : baseColor;
                if (span.code) drawRect(cursorX - 1, y - 1, cursorX + w + 1, y + fontRendererObj.FONT_HEIGHT, 0xFF2B2D31);
                fontRendererObj.drawStringWithShadow(formatted, cursorX, y, color);
            }
            cursorX += w;
        }
    }

    // -------------------------------------------------------------- images

    private static class LayoutCache {
        int width;
        int contentVersion;
        List<RenderLine> lines;
        List<ImageRef> imgs = Collections.emptyList();
        List<Embed> embeds = Collections.emptyList();
        int textHeight;
        int totalHeight;
    }

    private LayoutCache getLayout(ChatMessage msg, int width) {
        LayoutCache cache = layoutCache.get(msg);
        if (cache != null && cache.width == width && cache.contentVersion == msg.contentVersion) return cache;

        cache = new LayoutCache();
        cache.width = width;
        cache.contentVersion = msg.contentVersion;
        cache.lines = DiscordMarkdown.parse(displayContent(msg), msg.emojiRefs, fontRendererObj, width);
        cache.embeds = computeEmbeds(msg, cache);
        cache.imgs = computeImages(msg, cache);

        int textHeight = 0;
        for (RenderLine line : cache.lines) {
            textHeight += DiscordMarkdown.lineHeight(line.type, fontRendererObj);
        }

        int extra = 0;
        if (!cache.imgs.isEmpty()) {
            extra += textHeight > 0 ? 6 : 0;
        }
        if (msg.attachments != null && !msg.attachments.isEmpty()) {
            for (Attachment att : msg.attachments) {
                if (!isImageAttachment(att)) extra += EMBED_BOX_H + 6;
            }
        }
        if (msg.stickers != null && !msg.stickers.isEmpty()) {
            extra += msg.stickers.size() * (STICKER_BOX_H + 6);
        }
        for (Embed embed : cache.embeds) {
            if ("image".equals(embed.type)) {
                // image attachments and image embeds share the grid, sized live in blockHeight
            } else if ("file".equals(embed.type) || "video".equals(embed.type)) {
                extra += EMBED_BOX_H + 6;
            } else if ("website".equals(embed.type) || "rich".equals(embed.type)) extra += embedHeight(embed, Math.min(width, 360)) + 6;
        }

        cache.totalHeight = textHeight + extra;
        cache.textHeight = textHeight;
        layoutCache.put(msg, cache);
        return cache;
    }

    private GCImage getImage(String url, boolean circular) {
        if (url == null || url.isEmpty()) return null;
        return ImageManager.images.get(ImageManager.getOrCreateImage(url, circular));
    }

    private void drawAvatar(ChatMessage msg, int x, int y) {
        GCImage img = getImage(msg.avatar, true);
        if (img != null && img.isLoaded) {
            ResourceLocation tex = img.getTextureToRender(false);
            if (tex != null) {
                mc.getTextureManager().bindTexture(tex);
                GlStateManager.color(1f, 1f, 1f, 1f);
                GlStateManager.enableBlend();
                drawScaledCustomSizeModalRect(x, y, 0, 0, img.width, img.height, AVATAR_SIZE, AVATAR_SIZE, img.width, img.height);
                GlStateManager.disableBlend();
                return;
            }
        }
        drawRect(x, y, x + AVATAR_SIZE, y + AVATAR_SIZE, (userColor(msg.author == null ? "?" : msg.author) & 0x00FFFFFF) | 0x66000000);
    }

    private void drawInlineEmoji(String url, String name, int x, int y) {
        if (name != null && RenderUtils.drawEmoji(name, x, y - 2, DiscordMarkdown.EMOJI_SIZE)) return;
        GCImage img = getImage(url, false);
        if (img == null || !img.isLoaded || img.width == 0) {
            drawRect(x, y, x + DiscordMarkdown.EMOJI_SIZE, y + DiscordMarkdown.EMOJI_SIZE, 0x22FFFFFF);
            return;
        }
        ResourceLocation tex = img.getTextureToRender(false);
        if (tex == null) return;
        mc.getTextureManager().bindTexture(tex);
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.enableBlend();
        int size = DiscordMarkdown.EMOJI_SIZE;
        drawScaledCustomSizeModalRect(x, y - 2, 0, 0, img.width, img.height, size, size, img.width, img.height);
        GlStateManager.disableBlend();
    }

    /** Always reserves exactly boxHeight vertically, regardless of the image's real aspect ratio, so layout stays stable while media loads async. */
    private void drawImageBlock(String name, String url, int x, int y, int maxWidth, int boxHeight, boolean circular, int mouseX, int mouseY) {
        drawImageBlock(name, url, x, y, maxWidth, boxHeight, circular, false, mouseX, mouseY);
    }

    /** With naturalSize the loaded image is drawn at its own aspect ratio (width-capped), not forced into a fixed-height box. */
    private void drawImageBlock(String name, String url, int x, int y, int maxWidth, int boxHeight, boolean circular, boolean naturalSize, int mouseX, int mouseY) {
        GCImage img = getImage(url, circular);
        int cap = Math.min(maxWidth, MAX_IMAGE_DRAW_W);

        if (img != null && img.loadFailed) {
            drawRect(x, y, x + cap, y + boxHeight, 0xFF232428);
            String label = (name == null || name.isEmpty()) ? url : name;
            fontRendererObj.drawStringWithShadow("Could not load Image: " + label, x + 8, y + boxHeight / 2f - 4, 0xFF6D6F78);
            return;
        }

        if (img == null || !img.isLoaded || img.width == 0 || img.height == 0) {
            drawRect(x, y, x + cap, y + boxHeight, 0xFF232428);
            fontRendererObj.drawStringWithShadow("Loading...", x + 8, y + boxHeight / 2f - 4, 0xFF6D6F78);
            return;
        }

        float ratio = img.width / (float) img.height;
        int drawH;
        int drawW;
        if (naturalSize) {
            drawH = naturalImageHeight(img, maxWidth);
            drawW = Math.round(drawH * ratio);
        } else {
            drawH = boxHeight;
            drawW = Math.round(drawH * ratio);
            if (drawW > cap) { drawW = cap; drawH = Math.round(drawW / ratio); }
        }

        ResourceLocation tex = img.getTextureToRender(mouseX >= x && mouseX <= x + drawW && mouseY >= y && mouseY <= y + drawH);
        if (tex != null) {
            mc.getTextureManager().bindTexture(tex);
            GlStateManager.color(1f, 1f, 1f, 1f);
            GlStateManager.enableBlend();
            drawScaledCustomSizeModalRect(x, y, 0, 0, img.width, img.height, drawW, drawH, img.width, img.height);
            GlStateManager.disableBlend();
        }
    }

    /** All renderable images of a message, in order: image attachments first, then image embeds. Cached in {@link LayoutCache}. */
    private List<ImageRef> computeImages(ChatMessage msg, LayoutCache layout) {
        List<ImageRef> imgs = new ArrayList<>();
        if (msg.attachments != null) {
            for (Attachment att : msg.attachments) {
                if (isImageAttachment(att)) imgs.add(new ImageRef(att.name, att.url));
            }
        }
        for (Embed embed : layout.embeds) {
            if ("image".equals(embed.type) && embed.url != null) imgs.add(new ImageRef(embed.name, embed.url));
        }
        return imgs;
    }

    /** Draws all images of a message as a cover-cropped grid (2 columns); a single image is drawn natural-size. */
    private void drawImageGrid(List<ImageRef> imgs, int x, int y, int maxWidth, int mouseX, int mouseY) {
        if (imgs.size() == 1) {
            ImageRef ref = imgs.get(0);
            int h = imageGridHeight(imgs, maxWidth);
            drawImageBlock(ref.name, ref.url, x, y, maxWidth, h, false, true, mouseX, mouseY);
            clickRects.add(new ClickRect(x, y, Math.min(maxWidth, MAX_IMAGE_DRAW_W), h, () -> openImageViewer(imgs, 0)));
            return;
        }

        int cell = imageGridCell(maxWidth);
        for (int i = 0; i < imgs.size(); i++) {
            ImageRef ref = imgs.get(i);
            int cx = x + (i % IMAGE_GRID_COLS) * (cell + IMAGE_GRID_GAP);
            int cy = y + (i / IMAGE_GRID_COLS) * (cell + IMAGE_GRID_GAP);

            GCImage img = getImage(ref.url, false);
            if (img == null || img.loadFailed || !img.isLoaded || img.width == 0 || img.height == 0) {
                drawRect(cx, cy, cx + cell, cy + cell, 0xFF232428);
                String label = img != null && img.loadFailed ? "!" : "Loading...";
                fontRendererObj.drawStringWithShadow(label, cx + (cell - fontRendererObj.getStringWidth(label)) / 2f,
                        cy + cell / 2f - 4, 0xFF6D6F78);
            } else {
                boolean cellHover = mouseX >= cx && mouseX <= cx + cell && mouseY >= cy && mouseY <= cy + cell;
                ResourceLocation tex = img.getTextureToRender(cellHover);
                if (tex != null) {
                    float ratio = img.width / (float) img.height;
                    int srcW, srcH, srcX = 0, srcY = 0;
                    if (ratio > 1f) { srcW = srcH = img.height; srcX = (img.width - srcW) / 2; }
                    else { srcW = srcH = img.width; srcY = (img.height - srcH) / 2; }
                    mc.getTextureManager().bindTexture(tex);
                    GlStateManager.color(1f, 1f, 1f, 1f);
                    GlStateManager.enableBlend();
                    drawScaledCustomSizeModalRect(cx, cy, srcX, srcY, srcW, srcH, cell, cell, img.width, img.height);
                    GlStateManager.disableBlend();
                }
            }

            final int index = i;
            clickRects.add(new ClickRect(cx, cy, cell, cell, () -> openImageViewer(imgs, index)));
        }
    }

    private void openImageViewer(List<ImageRef> imgs, int index) {
        mc.displayGuiScreen(new ImageViewerScreen(imgs, index));
    }

    /** True if the attachment is a decodable image (by content type or URL extension) and should use the image renderer. */
    private static boolean isImageAttachment(Attachment att) {
        if (att == null) return false;
        if (att.imageType != null && att.imageType.startsWith("image/")) return true;
        if (att.url != null) {
            String url = att.url;
            int q = url.indexOf('?');
            if (q >= 0) url = url.substring(0, q);
            int dot = url.lastIndexOf('.');
            if (dot >= 0 && dot < url.length() - 1) {
                switch (url.substring(dot + 1).toLowerCase()) {
                    case "png": case "jpg": case "jpeg": case "gif": case "webp": case "bmp":
                        return true;
                }
            }
        }
        return false;
    }

    /** Shared file-embed banner: background, accent strip, file-type square, name + meta. Buttons are drawn by the caller. */
    private void drawFileBanner(int x, int y, int maxWidth, int accent, String extLabel, String name, String meta) {
        drawRect(x, y, x + maxWidth, y + EMBED_BOX_H, 0xFF232428);
        drawRect(x, y, x + 3, y + EMBED_BOX_H, accent);

        String label = extLabel == null || extLabel.isEmpty() ? "FILE" : extLabel;
        if (label.length() > 4) label = label.substring(0, 4);

        drawRect(x + 10, y + 9, x + 36, y + 35, 0xFF2B2D31);
        fontRendererObj.drawStringWithShadow(label,
                x + 10 + (26 - fontRendererObj.getStringWidth(label)) / 2f,
                y + 9 + (26 - fontRendererObj.FONT_HEIGHT) / 2f, 0xFFB5BAC1);

        int tx = x + 46;
        fontRendererObj.drawStringWithShadow(fontRendererObj.trimStringToWidth(name, maxWidth - 60), tx, y + 7, 0xFFDCDDDE);
        fontRendererObj.drawStringWithShadow(meta, tx, y + 18, 0xFF949BA4);
    }

    /** Renders a Discord-style embed for non-image attachments: file name, type, size, a Download button and an Open in Discord button. */
    private void drawEmbedBlock(Attachment att, ChatMessage msg, int x, int y, int maxWidth, int mouseX, int mouseY) {
        String name = (att.name == null || att.name.isEmpty()) ? "attachment" : att.name;
        String ext = "";
        int dot = name.lastIndexOf('.');
        if (dot >= 0 && dot < name.length() - 1) ext = name.substring(dot + 1);
        String meta = (att.imageType == null || att.imageType.isEmpty() ? ext : att.imageType);
        if (att.size > 0) meta += " • " + formatFileSize(att.size);
        drawFileBanner(x, y, maxWidth, 0xFF4E5058, ext.toUpperCase(), name, meta);

        boolean media = MediaSaver.isMediaUrl(att.url);
        int by = y + 35;
        drawEmbedButton(x + 10, by, media ? "Download" : "Open", mouseX, mouseY, () -> {
            if (media) downloadMedia(att.url, att.name);
            else pendingLinkUrl = att.url;
        });
        int bw = fontRendererObj.getStringWidth(media ? "Download" : "Open") + 14;
        if (msg.discordID != null && !msg.discordID.isEmpty()) {
            drawEmbedButton(x + 10 + bw + 6, by, "Open in Discord", mouseX, mouseY, () ->
                    pendingLinkUrl = "https://discord.com/channels/1479556885769093192/" + msg.channelId + "/" + msg.discordID);
        }
    }

    /** Renders a file/video embed for a link: file name, type, size, Download (media only) and Open buttons. */
    private void drawFileEmbed(Embed e, int x, int y, int maxWidth, int mouseX, int mouseY) {
        String name = (e.name == null || e.name.isEmpty()) ? MediaSaver.fileNameFromUrl(e.url) : e.name;
        String ext = e.fileType == null || e.fileType.isEmpty() ? "FILE" : e.fileType;
        String meta = e.loading ? "Loading info..." : (ext + (e.fileSize > 0 ? " • " + formatFileSize(e.fileSize) : ""));
        drawFileBanner(x, y, maxWidth, "video".equals(e.type) ? 0xFF5865F2 : 0xFF4E5058, ext, name, meta);

        boolean media = MediaSaver.isMediaUrl(e.url);
        int by = y + 35;
        if (media) {
            drawEmbedButton(x + 10, by, "Download", mouseX, mouseY, () -> downloadMedia(e.url, e.name));
            int bw = fontRendererObj.getStringWidth("Download") + 14;
            drawEmbedButton(x + 10 + bw + 6, by, "Open", mouseX, mouseY, () -> pendingLinkUrl = e.url);
        } else {
            drawEmbedButton(x + 10, by, "Open", mouseX, mouseY, () -> pendingLinkUrl = e.url);
        }
    }

    /** Saves an image/video/GIF URL to the downloads folder on a background thread and shows a result toast. */
    private void downloadMedia(String url, String name) {
        CompletableFuture.runAsync(() -> {
            try {
                final String path = MediaSaver.save(url, name);
                mc.addScheduledTask(() -> flashDownloadMsg("Saved to " + path));
            } catch (Exception e) {
                Aetheria.logger.warning("[G-Chat] Download failed: " + e.getMessage());
                mc.addScheduledTask(() -> flashDownloadMsg("Download failed: " + e.getMessage()));
            }
        });
    }

    private void flashDownloadMsg(String msg) {
        downloadMsg = msg;
        downloadMsgUntil = System.currentTimeMillis() + 3500;
    }

    private static String formatFileSize(long bytes) {
        if (bytes <= 0) return "";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1048576) return String.format("%.1f KB", bytes / 1024f);
        return String.format("%.1f MB", bytes / 1048576f);
    }

    private void drawEmbedButton(int x, int y, String label, int mouseX, int mouseY, Runnable action) {
        int w = fontRendererObj.getStringWidth(label) + 14;
        boolean hover = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + 16;
        drawRect(x, y, x + w, y + 16, hover ? 0xFF404249 : 0xFF2B2D31);
        fontRendererObj.drawStringWithShadow(label, x + 7, y + 4, hover ? 0xFFFFFFFF : 0xFFB5BAC1);
        clickRects.add(new ClickRect(x, y, w, 16, action));
    }

    // -------------------------------------------------------------- embeds

    /** Embeds to render for a message: server-provided ones, or the auto-detected first link. Cached in {@link LayoutCache}. */
    private List<Embed> computeEmbeds(ChatMessage msg, LayoutCache layout) {
        if (msg.embeds != null && !msg.embeds.isEmpty()) {
            List<Embed> out = new ArrayList<>();
            for (Embed e : msg.embeds) {
                if (e != null && e.url != null && e.url.startsWith("https://discord.com/channels/")) continue;
                if (e != null && "rich".equals(e.type)) {
                    String mediaUrl = mediaUrlOf(e);
                    if (mediaUrl != null) {
                        Embed img = new Embed("image", mediaUrl);
                        img.name = (e.title != null && !e.title.isEmpty()) ? e.title
                                : (e.siteName != null && !e.siteName.isEmpty()) ? e.siteName : null;
                        out.add(img);
                        continue;
                    }
                }
                out.add(e);
            }
            if (!out.isEmpty()) return out;
        }
        Embed e = embedForFirstLink(layout);
        return e == null ? Collections.<Embed>emptyList() : Collections.singletonList(e);
    }

    /**
     * Resolves a "rich" embed to a directly renderable media URL, or null if it should stay a website box.
     * Matches when the embed URL itself is an image, or the embed carries only media (no title/description/fields)
     * with an image thumbnail, or it comes from a known image/gif sharing host (tenor/giphy/imgur).
     */
    private static String mediaUrlOf(Embed e) {
        if (DiscordMarkdown.isImageUrl(e.url)) return e.url;
        if (e.imageUrl == null || !DiscordMarkdown.isImageUrl(e.imageUrl)) return null;
        boolean mediaOnly = (e.title == null || e.title.isEmpty())
                && (e.description == null || e.description.isEmpty())
                && (e.fields == null || e.fields.isEmpty());
        if (mediaOnly) return e.imageUrl;
        String host = hostOf(e.url);
        return "tenor.com".equals(host) || "giphy.com".equals(host) || "imgur.com".equals(host)
                || "media.tenor.com".equals(host) || "i.giphy.com".equals(host) || "i.imgur.com".equals(host)
                ? e.imageUrl : null;
    }

    /** Finds the first hyperlink span in a message's layout and resolves its embed (cached/async for websites and file sizes, sync for images). */
    private Embed embedForFirstLink(LayoutCache layout) {
        for (RenderLine line : layout.lines) {
            for (Span span : line.spans) {
                if (span.linkUrl == null) continue;
                String url = span.linkUrl;
                if (url.startsWith("https://discord.com/channels/")) continue;
                Embed cached = embedCache.get(url);
                if (cached != null) return cached;
                if (DiscordMarkdown.isImageUrl(url)) {
                    Embed e = new Embed("image", url);
                    e.name = span.text;
                    embedCache.put(url, e);
                    return e;
                }
                String ext = DiscordMarkdown.extensionOf(url);
                if (ext != null) {
                    Embed e = new Embed(DiscordMarkdown.isVideoUrl(url) ? "video" : "file", url);
                    e.name = !span.bareLink ? span.text : MediaSaver.fileNameFromUrl(url);
                    e.fileType = ext.toUpperCase();
                    e.loading = true;
                    embedCache.put(url, e);
                    fetchFileInfo(url, e);
                    return e;
                }
                Embed e = new Embed("website", url);
                e.loading = true;
                embedCache.put(url, e);
                fetchWebsiteEmbed(url);
                return e;
            }
        }
        return null;
    }

    /** Asynchronously HEADs a file URL for its size/content-type to fill in a file/video embed. */
    private void fetchFileInfo(String url, Embed embed) {
        CompletableFuture.runAsync(() -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setRequestMethod("HEAD");
                connection.setRequestProperty("User-Agent", "Aetheria/" + Aetheria.VERSION);
                connection.setConnectTimeout(8000);
                connection.setReadTimeout(8000);
                int code = connection.getResponseCode();
                if (code == 200) {
                    long size = connection.getContentLengthLong();
                    if (size > 0) embed.fileSize = size;
                    String contentType = connection.getContentType();
                    if (contentType != null && embed.fileType == null) {
                        int semi = contentType.indexOf(';');
                        embed.fileType = semi > 0 ? contentType.substring(0, semi).trim() : contentType.trim();
                    }
                }
            } catch (Exception ignored) {
            } finally {
                embed.loading = false;
            }
        });
    }

    private void fetchWebsiteEmbed(String url) {
        CompletableFuture.runAsync(() -> {
            try {
                URL u = new URL(CapeAPI.getAPIUrl("embed?url=" + URLEncoder.encode(url, "UTF-8")));
                HttpURLConnection connection = (HttpURLConnection) u.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "Aetheria/" + Aetheria.VERSION);
                connection.setRequestProperty("Accept", "application/json");
                connection.setReadTimeout(8000);
                connection.setConnectTimeout(8000);
                if (connection.getResponseCode() == 200) {
                    Embed e = GlobalChat.GSON.fromJson(ElectionUtils.readResponse(connection), Embed.class);
                    embedCache.put(url, e != null && e.type != null ? e : new Embed("failed", url));
                } else {
                    embedCache.put(url, new Embed("failed", url));
                }
            } catch (Exception ex) {
                embedCache.put(url, new Embed("failed", url));
            }
        });
    }

    /** Discord-style website/rich embed: accent bar, site name, title, description, fields, thumbnail. */
    private void drawWebEmbed(Embed e, int x, int y, int maxWidth, int height, int mouseX, int mouseY) {
        drawRect(x, y, x + maxWidth, y + height, 0xFF232428);
        drawRect(x, y, x + 3, y + height, 0xFF5865F2);

        if (e == null || "failed".equals(e.type)) {
            fontRendererObj.drawStringWithShadow("No preview available", x + 10, y + 8, 0xFF949BA4);
            return;
        }
        if (e.loading) {
            fontRendererObj.drawStringWithShadow("Loading preview...", x + 10, y + 8, 0xFF949BA4);
            return;
        }

        int thumb = 72;
        int textW = maxWidth - thumb - 18;

        String site = (e.siteName == null || e.siteName.isEmpty()) ? hostOf(e.url) : e.siteName;
        if (site == null || site.isEmpty()) site = "Link";
        fontRendererObj.drawStringWithShadow(site, x + 10, y + 6, 0xFF949BA4);
        int ty = y + 16;
        if (e.title != null && !e.title.isEmpty()) {
            ty = drawMarkdown(e.title, x + 10, ty, textW, 2, 0xFFF2F3F5) + 3;
        }
        if (e.description != null && !e.description.isEmpty()) {
            ty = drawMarkdown(e.description, x + 10, ty, textW, 2, 0xFFB5BAC1);
        }
        if (e.fields != null && !e.fields.isEmpty()) {
            ty += 3;
            drawEmbedFields(e, x + 10, ty, textW);
        }
        if (e.imageUrl != null && !e.imageUrl.isEmpty()) {
            drawEmbedThumb(e.imageUrl, x + maxWidth - thumb - 8, y + 8, thumb, mouseX, mouseY);
        }
    }

    /** Renders Discord-style fields; consecutive inline fields share a row (2 columns). */
    private void drawEmbedFields(Embed e, int x, int y, int textW) {
        if (e.fields == null || e.fields.isEmpty()) return;
        int i = 0;
        while (i < e.fields.size()) {
            Embed.EmbedField f = e.fields.get(i);
            if (!f.inline) {
                drawEmbedField(f, x, y, textW);
                y += fieldHeight(f, textW) + 3;
                i++;
                continue;
            }
            Embed.EmbedField second = (i + 1 < e.fields.size() && e.fields.get(i + 1).inline) ? e.fields.get(i + 1) : null;
            int w = second != null ? (textW - 8) / 2 : textW;
            int h = fieldHeight(f, w);
            drawEmbedField(f, x, y, w);
            if (second != null) {
                drawEmbedField(second, x + w + 8, y, textW - w - 8);
                h = Math.max(h, fieldHeight(second, textW - w - 8));
            }
            y += h + 3;
            i += second != null ? 2 : 1;
        }
    }

    private void drawEmbedField(Embed.EmbedField f, int x, int y, int w) {
        if (f.name != null && !f.name.isEmpty()) {
            y = drawMarkdown(f.name, x, y, w, 1, 0xFFF2F3F5) + 2;
        }
        if (f.value != null && !f.value.isEmpty()) {
            drawMarkdown(f.value, x, y, w, 2, 0xFFB5BAC1);
        }
    }

    /** Height of a single field block (name + up to 2 value lines). */
    private int fieldHeight(Embed.EmbedField f, int w) {
        int h = 0;
        if (f.name != null && !f.name.isEmpty()) h += markdownHeight(f.name, w, 1) + 2;
        if (f.value != null && !f.value.isEmpty()) h += markdownHeight(f.value, w, 2);
        return h;
    }

    /** Total measured height for an embed (fields can make rich embeds taller than the base box). */
    private int embedHeight(Embed e, int maxWidth) {
        if (e == null || "failed".equals(e.type)) return WEBSITE_EMBED_H;
        if (e.loading) return WEBSITE_EMBED_H;
        int thumb = 72;
        int textW = maxWidth - thumb - 18;
        int y = 16;
        if (e.title != null && !e.title.isEmpty()) {
            y += markdownHeight(e.title, textW, 2) + 3;
        }
        if (e.description != null && !e.description.isEmpty()) {
            y += markdownHeight(e.description, textW, 2);
        }
        if (e.fields != null && !e.fields.isEmpty()) {
            y += 3;
            int i = 0;
            while (i < e.fields.size()) {
                Embed.EmbedField f = e.fields.get(i);
                if (!f.inline) {
                    y += fieldHeight(f, textW) + 3;
                    i++;
                    continue;
                }
                Embed.EmbedField second = (i + 1 < e.fields.size() && e.fields.get(i + 1).inline) ? e.fields.get(i + 1) : null;
                int w = second != null ? (textW - 8) / 2 : textW;
                int h = fieldHeight(f, w);
                if (second != null) h = Math.max(h, fieldHeight(second, textW - w - 8));
                y += h + 3;
                i += second != null ? 2 : 1;
            }
        }
        int minWithThumb = (e.imageUrl != null && !e.imageUrl.isEmpty()) ? 8 + thumb + 8 : WEBSITE_EMBED_H;
        return Math.max(WEBSITE_EMBED_H, Math.max(minWithThumb, y + 8));
    }

    /**
     * Draws embed text with Discord markdown processed (bold, italic, inline code, links, headers,
     * block quotes) via {@link DiscordMarkdown}, word-wrapped to maxWidth and capped at maxLines.
     * Returns the new y position after the drawn lines.
     */
    private int drawMarkdown(String text, int x, int y, int maxWidth, int maxLines, int color) {
        if (text == null || text.isEmpty()) return y;
        List<RenderLine> lines = DiscordMarkdown.parse(text, null, fontRendererObj, maxWidth);
        int drawn = 0;
        for (RenderLine line : lines) {
            if (drawn >= maxLines) break;
            int lh = DiscordMarkdown.lineHeight(line.type, fontRendererObj);
            if (line.type == LineType.CODE_BLOCK) {
                drawRect(x - 4, y - 1, x + maxWidth + 4, y + lh - 1, 0xFF232428);
            } else if (line.type == LineType.QUOTE) {
                drawRect(x - 8, y, x - 6, y + lh - 2, 0xFF4E5058);
            }
            float scale = DiscordMarkdown.lineScale(line.type);
            if (scale != 1f) {
                GlStateManager.pushMatrix();
                GlStateManager.translate(x, y, 0f);
                GlStateManager.scale(scale, scale, 1f);
                drawSpans(line.spans, 0, 0, color, true);
                GlStateManager.popMatrix();
            } else {
                drawSpans(line.spans, x, y, color, true);
            }
            y += lh;
            drawn++;
        }
        return y;
    }

    /** Pixel height of markdown text (parsed + wrapped), capped at maxLines. */
    private int markdownHeight(String text, int maxWidth, int maxLines) {
        if (text == null || text.isEmpty()) return 0;
        List<RenderLine> lines = DiscordMarkdown.parse(text, null, fontRendererObj, maxWidth);
        int h = 0;
        for (int i = 0; i < lines.size() && i < maxLines; i++) {
            h += DiscordMarkdown.lineHeight(lines.get(i).type, fontRendererObj);
        }
        return h;
    }

    private void drawEmbedThumb(String url, int x, int y, int size, int mouseX, int mouseY) {
        GCImage img = getImage(url, false);
        if (img == null || !img.isLoaded || img.width == 0 || img.height == 0) {
            drawRect(x, y, x + size, y + size, 0xFF2B2D31);
            return;
        }
        float ratio = img.width / (float) img.height;
        int h = size;
        int w = Math.round(h * ratio);
        if (w > size) {
            w = size;
            h = Math.round(w / ratio);
        }
        int ox = x + (size - w) / 2;
        int oy = y + (size - h) / 2;
        ResourceLocation tex = img.getTextureToRender(mouseX >= ox && mouseX <= ox + w && mouseY >= oy && mouseY <= oy + h);
        if (tex == null) return;
        mc.getTextureManager().bindTexture(tex);
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.enableBlend();
        drawScaledCustomSizeModalRect(ox, oy, 0, 0, img.width, img.height, w, h, img.width, img.height);
        GlStateManager.disableBlend();
    }

    private static String hostOf(String url) {
        if (url == null) return "";
        int s = url.indexOf("://");
        int start = s >= 0 ? s + 3 : 0;
        int end = url.indexOf('/', start);
        if (end < 0) end = url.length();
        String host = url.substring(start, end);
        return host.startsWith("www.") ? host.substring(4) : host;
    }

    // -------------------------------------------------------------- helpers

    private static final Map<String, Integer> USER_COLOR_CACHE = new HashMap<>();

    private static int userColor(String username) {
        Integer cached = USER_COLOR_CACHE.get(username);
        if (cached != null) return cached;
        int hash = username.hashCode();
        float hue = ((hash & 0x7FFFFFFF) % 360) / 360f;
        Color c = Color.getHSBColor(hue, 0.45f, 0.95f);
        int color = 0xFF000000 | (c.getRed() << 16) | (c.getGreen() << 8) | c.getBlue();
        if (USER_COLOR_CACHE.size() >= 512) USER_COLOR_CACHE.clear();
        USER_COLOR_CACHE.put(username, color);
        return color;
    }

    /** Number of the local day containing ts (exact per-timestamp timezone offset, so DST-safe). */
    private static long localDay(long ts) {
        return (ts + TimeZone.getDefault().getOffset(ts)) / 86400000L;
    }

    private static String formatTimestamp(ChatMessage msg) {
        long dayNow = localDay(System.currentTimeMillis());
        if (msg.tsHeaderKey != dayNow) {
            if (msg.tsDayNum == Long.MIN_VALUE) msg.tsDayNum = localDay(msg.timestamp);
            msg.tsHeaderText = msg.tsDayNum == dayNow ? "Today at " + timeText(msg) : dateText(msg);
            msg.tsHeaderKey = dayNow;
        }
        return msg.tsHeaderText;
    }

    private static String formatTimeShort(ChatMessage msg) {
        return timeText(msg);
    }

    private static String timeText(ChatMessage msg) {
        if (msg.tsTimeText == null) msg.tsTimeText = TIME_FORMAT.format(new Date(msg.timestamp));
        return msg.tsTimeText;
    }

    private static String dateText(ChatMessage msg) {
        if (msg.tsDateText == null) msg.tsDateText = DATE_FORMAT.format(new Date(msg.timestamp));
        return msg.tsDateText;
    }

    private ChatMessage findByDiscordId(String discordId) {
        if (discordId == null || selectedChannel == null) return null;
        return selectedChannel.byDiscordID.get(discordId);
    }

    private void enableScissor(int x, int y, int w, int h) {
        ScaledResolution sr = new ScaledResolution(mc);
        int scale = sr.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x * scale, mc.displayHeight - (y + h) * scale, w * scale, h * scale);
    }

    private void disableScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private static class ClickRect {
        final int x, y, w, h;
        final Runnable action;
        ClickRect(int x, int y, int w, int h, Runnable action) {
            this.x = x; this.y = y; this.w = w; this.h = h; this.action = action;
        }
        boolean contains(int mx, int my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    private static class ImageRef extends ImageViewerScreen.ImageRef {
        ImageRef(String name, String url) {
            super(name, url);
        }
    }
}