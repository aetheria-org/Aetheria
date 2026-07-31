package io.hamlook.aetheria.features.chat.globalchat.ui;

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
import io.hamlook.aetheria.features.chat.globalchat.vars.Sticker;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private static final int MARKDOWN_BAR_H = 108;
    private static final int ATTACHMENT_BOX_H = 120;
    private static final int EMBED_BOX_H = 56;
    private static final int STICKER_BOX_H = 90;

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

    private final Map<String, String> imageCache = new HashMap<>();
    private final Map<ChatMessage, LayoutCache> layoutCache = new IdentityHashMap<>();
    private final Set<Span> revealedSpoilers = Collections.newSetFromMap(new IdentityHashMap<>());
    private final List<ClickRect> clickRects = new ArrayList<>();

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
        inputField = new ChatInputField(inputX + 8, boxY + 2, inputWidth - 16, INPUT_HEIGHT - 4);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void updateScreen() {
        inputField.updateDrag();
        if (pendingLinkUrl != null) {
            String url = pendingLinkUrl;
            pendingLinkUrl = null;
            mc.displayGuiScreen(new LinkConfirmScreen(url));
        }
    }

    // ---------------------------------------------------------------- input

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (inputField.mouseClicked(mouseX, mouseY, mouseButton)) return;
        if (mouseButton == 0) {
            for (ClickRect cr : clickRects) {
                if (cr.contains(mouseX, mouseY)) {
                    cr.action.run();
                    break;
                }
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(null);
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
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;
        ChatMessage message = new ChatMessage(text, selectedChannel.channelID, null);
        message.sendMessage();
        inputField.setText("");
    }

    // --------------------------------------------------------------- render

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawRect(0, 0, width, height, 0xFF313338);

        clickRects.clear();
        drawSidebar(mouseX, mouseY);
        drawHeader(mouseX, mouseY);
        drawMessages(mouseX, mouseY);
        drawInputArea();

        inputField.draw();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawSidebar(int mouseX, int mouseY) {
        drawRect(0, 0, SIDEBAR_WIDTH, height, 0xFF2B2D31);
        fontRendererObj.drawStringWithShadow("CHANNELS", 10, 12, 0xFF949BA4);

        List<Channel> channelList = new ArrayList<>(GlobalChat.channels.values());
        channelList.sort(Comparator.comparing(a -> a.channelID));

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

        int closeX = width - 24;
        boolean closeHover = mouseX >= closeX && mouseX < closeX + 16 && mouseY >= 7 && mouseY < 23;
        fontRendererObj.drawStringWithShadow("X", closeX + 4, 10, closeHover ? 0xFFFFFFFF : 0xFF949BA4);
        clickRects.add(new ClickRect(closeX, 7, 16, 16, () -> mc.displayGuiScreen(null)));
    }

    private void drawInputArea() {
        int boxY = height - INPUT_HEIGHT - PADDING;
        drawRect(SIDEBAR_WIDTH, boxY - PADDING, width, height, 0xFF313338);
        drawRect(SIDEBAR_WIDTH + PADDING, boxY, width - PADDING, boxY + INPUT_HEIGHT, 0xFF383A40);
    }

    private void drawMessages(int mouseX, int mouseY) {
        int areaX = SIDEBAR_WIDTH;
        int areaY = HEADER_HEIGHT;
        int areaW = width - SIDEBAR_WIDTH;
        int areaBottom = height - INPUT_HEIGHT - PADDING * 2;
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

        for (int i = lines.size() - 1; i >= 0; i--) {
            ChatMessage msg = lines.get(i).message;
            if (msg == null) continue;

            ChatMessage older = (i > 0) ? lines.get(i - 1).message : null;
            boolean groupStart = older == null || older.author == null || msg.author == null
                    || !older.author.equals(msg.author) || (msg.timestamp - older.timestamp) > GROUP_GAP_MS;

            LayoutCache layout = getLayout(msg, contentWidth);
            int bh = blockHeight(msg, layout, groupStart);
            int top = cursorBottom - bh;

            if (top < areaBottom && cursorBottom > areaY) {
                renderMessage(msg, layout, areaX + PADDING, top, contentWidth, groupStart, mouseX, mouseY);
            }

            int gap = groupStart ? MESSAGE_GAP : CONTINUATION_GAP;
            total += bh + gap;
            cursorBottom = top - gap;
        }

        disableScissor();

        int maxScroll = Math.max(0, total + MESSAGE_BOTTOM_PAD - areaH);
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

    private int headerAllowance(boolean groupStart) {
        if (!groupStart) return 2;
        return 2 + fontRendererObj.FONT_HEIGHT + DiscordMarkdown.LINE_SPACING;
    }

    private int blockHeight(ChatMessage msg, LayoutCache layout, boolean groupStart) {
        return replyAllowance(msg) + headerAllowance(groupStart) + layout.totalHeight;
    }

    private void renderMessage(ChatMessage msg, LayoutCache layout, int x, int y, int contentWidth, boolean groupStart, int mouseX, int mouseY) {
        int textX = x + AVATAR_SIZE + 8;
        int replyAlw = replyAllowance(msg);
        int headerAlw = headerAllowance(groupStart);
        int rowHeight = replyAlw + headerAlw + layout.totalHeight;

        boolean hovered = mouseX >= x - 4 && mouseX < x + AVATAR_SIZE + 8 + contentWidth + PADDING
                && mouseY >= y - 2 && mouseY < y + rowHeight;
        if (hovered) {
            drawRect(x - 4, y - 2, x + AVATAR_SIZE + 8 + contentWidth + PADDING, y + rowHeight, 0x14FFFFFF);
        }

        if (msg.replying) {
            ChatMessage original = findByDiscordId(msg.replyingMessage);
            String replyText = "↰ " + (original != null && original.author != null ? original.author : "a message");
            fontRendererObj.drawStringWithShadow(replyText, textX, y + 1, 0xFF949BA4);
        }

        int afterReplyY = y + replyAlw;

        if (groupStart) {
            drawAvatar(msg, x, afterReplyY);
            String author = msg.author == null ? "Unknown" : msg.author;
            int nameColor = userColor(author);
            fontRendererObj.drawStringWithShadow(author, textX, afterReplyY + 2, nameColor);
            int nameWidth = fontRendererObj.getStringWidth(author);
            fontRendererObj.drawStringWithShadow(formatTimestamp(msg.timestamp), textX + nameWidth + 8, afterReplyY + 3, 0xFF949BA4);
        } else if (hovered) {
            fontRendererObj.drawStringWithShadow(formatTimeShort(msg.timestamp), x, afterReplyY + 2, 0xFF6D6F78);
        }

        int cursorY = afterReplyY + headerAlw;

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

        if (!msg.attachments.isEmpty()) {
            if (!layout.lines.isEmpty()) cursorY += 6;
            for (Attachment att : msg.attachments) {
                if (isImageAttachment(att)) {
                    drawImageBlock(att.name, att.url, textX, cursorY, contentWidth, ATTACHMENT_BOX_H, false);
                    cursorY += ATTACHMENT_BOX_H + 6;
                } else {
                    drawEmbedBlock(att, msg, textX, cursorY, Math.min(contentWidth, 360), mouseX, mouseY);
                    cursorY += EMBED_BOX_H + 6;
                }
            }
        }

        if (!msg.stickers.isEmpty()) {
            for (Sticker st : msg.stickers.values()) {
                drawImageBlock(st.name, st.url, textX, cursorY, Math.min(contentWidth, 96), STICKER_BOX_H, false);
                cursorY += STICKER_BOX_H + 6;
            }
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
                drawInlineEmoji(span.imageUrl, cursorX, y);
                cursorX += DiscordMarkdown.EMOJI_SIZE;
                continue;
            }

            String formatted = DiscordMarkdown.toFormatted(span);
            int w = fontRendererObj.getStringWidth(formatted);

            if (span.linkUrl != null) {
                fontRendererObj.drawStringWithShadow(formatted, cursorX, y, 0xFF00A8FC);
                drawRect(cursorX, y + fontRendererObj.FONT_HEIGHT - 1, cursorX + w, y + fontRendererObj.FONT_HEIGHT, 0xFF00A8FC);
                if (registerClicks) {
                    int rx = cursorX, ry = y, rw = w, rh = fontRendererObj.FONT_HEIGHT;
                    clickRects.add(new ClickRect(rx, ry, rw, rh, () -> pendingLinkUrl = span.linkUrl));
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
        List<RenderLine> lines;
        int totalHeight;
    }

    private LayoutCache getLayout(ChatMessage msg, int width) {
        LayoutCache cache = layoutCache.get(msg);
        if (cache != null && cache.width == width) return cache;

        cache = new LayoutCache();
        cache.width = width;
        cache.lines = DiscordMarkdown.parse(msg.content, msg.emojiRefs, fontRendererObj, width);

        int textHeight = 0;
        for (RenderLine line : cache.lines) {
            textHeight += DiscordMarkdown.lineHeight(line.type, fontRendererObj);
        }

        int extra = 0;
        if (msg.attachments != null && !msg.attachments.isEmpty()) {
            extra += textHeight > 0 ? 6 : 0;
            for (Attachment att : msg.attachments) {
                extra += (isImageAttachment(att) ? ATTACHMENT_BOX_H : EMBED_BOX_H) + 6;
            }
        }
        if (msg.stickers != null && !msg.stickers.isEmpty()) {
            extra += msg.stickers.size() * (STICKER_BOX_H + 6);
        }

        cache.totalHeight = textHeight + extra;
        layoutCache.put(msg, cache);
        return cache;
    }

    private GCImage getImage(String url, boolean circular) {
        if (url == null || url.isEmpty()) return null;
        String id = imageCache.get(url);
        if (id == null) {
            id = GCImage.createGCImage(url, circular);
            imageCache.put(url, id);
        }
        return ImageManager.images.get(id);
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

    private void drawInlineEmoji(String url, int x, int y) {
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
    private void drawImageBlock(String name, String url, int x, int y, int maxWidth, int boxHeight, boolean circular) {
        GCImage img = getImage(url, circular);
        int cap = Math.min(maxWidth, 260);

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
        int drawH = boxHeight;
        int drawW = Math.round(drawH * ratio);
        if (drawW > cap) { drawW = cap; drawH = Math.round(drawW / ratio); }

        ResourceLocation tex = img.getTextureToRender(true);
        if (tex != null) {
            mc.getTextureManager().bindTexture(tex);
            GlStateManager.color(1f, 1f, 1f, 1f);
            GlStateManager.enableBlend();
            drawScaledCustomSizeModalRect(x, y, 0, 0, img.width, img.height, drawW, drawH, img.width, img.height);
            GlStateManager.disableBlend();
        }
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

    /** Renders a Discord-style embed for non-image attachments: file name, type, a Download button and an Open in Discord button. */
    private void drawEmbedBlock(Attachment att, ChatMessage msg, int x, int y, int maxWidth, int mouseX, int mouseY) {
        drawRect(x, y, x + maxWidth, y + EMBED_BOX_H, 0xFF232428);
        drawRect(x, y, x + 3, y + EMBED_BOX_H, 0xFF4E5058);

        String name = (att.name == null || att.name.isEmpty()) ? "attachment" : att.name;
        String ext = "";
        int dot = name.lastIndexOf('.');
        if (dot >= 0 && dot < name.length() - 1) ext = name.substring(dot + 1);
        String label = (ext.isEmpty() ? "FILE" : ext.toUpperCase());
        if (label.length() > 4) label = label.substring(0, 4);

        drawRect(x + 10, y + 9, x + 36, y + 35, 0xFF2B2D31);
        fontRendererObj.drawStringWithShadow(label,
                x + 10 + (26 - fontRendererObj.getStringWidth(label)) / 2,
                y + 9 + (26 - fontRendererObj.FONT_HEIGHT) / 2, 0xFFB5BAC1);

        int tx = x + 46;
        fontRendererObj.drawStringWithShadow(fontRendererObj.trimStringToWidth(name, maxWidth - 60), tx, y + 7, 0xFFDCDDDE);
        fontRendererObj.drawStringWithShadow(att.imageType == null || att.imageType.isEmpty() ? ext : att.imageType,
                tx, y + 18, 0xFF949BA4);

        int by = y + 35;
        drawEmbedButton(x + 10, by, "Download", mouseX, mouseY, () -> pendingLinkUrl = att.url);
        int bw = fontRendererObj.getStringWidth("Download") + 14;
        if (msg.discordID != null && !msg.discordID.isEmpty()) {
            drawEmbedButton(x + 10 + bw + 6, by, "Open in Discord", mouseX, mouseY, () ->
                    pendingLinkUrl = "https://discord.com/channels/1479556885769093192/" + msg.channelId + "/" + msg.discordID);
        }
    }

    private void drawEmbedButton(int x, int y, String label, int mouseX, int mouseY, Runnable action) {
        int w = fontRendererObj.getStringWidth(label) + 14;
        boolean hover = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + 16;
        drawRect(x, y, x + w, y + 16, hover ? 0xFF404249 : 0xFF2B2D31);
        fontRendererObj.drawStringWithShadow(label, x + 7, y + 4, hover ? 0xFFFFFFFF : 0xFFB5BAC1);
        clickRects.add(new ClickRect(x, y, w, 16, action));
    }

    // -------------------------------------------------------------- helpers

    private static int userColor(String username) {
        int hash = username.hashCode();
        float hue = ((hash & 0x7FFFFFFF) % 360) / 360f;
        Color c = Color.getHSBColor(hue, 0.45f, 0.95f);
        return 0xFF000000 | (c.getRed() << 16) | (c.getGreen() << 8) | c.getBlue();
    }

    private static String formatTimestamp(long ts) {
        if (sameDay(ts, System.currentTimeMillis())) {
            return "Today at " + TIME_FORMAT.format(new Date(ts));
        }
        return DATE_FORMAT.format(new Date(ts));
    }

    private static String formatTimeShort(long ts) {
        return TIME_FORMAT.format(new Date(ts));
    }

    private static boolean sameDay(long a, long b) {
        Calendar c1 = Calendar.getInstance(); c1.setTimeInMillis(a);
        Calendar c2 = Calendar.getInstance(); c2.setTimeInMillis(b);
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR);
    }

    private ChatMessage findByDiscordId(String discordId) {
        if (discordId == null || selectedChannel == null) return null;
        for (ChatLine line : selectedChannel.messageHistory) {
            if (line.message != null && discordId.equals(line.message.discordID)) return line.message;
        }
        return null;
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
}