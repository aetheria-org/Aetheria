package io.hamlook.aetheria.features.chat.globalchat.ui;

import io.hamlook.aetheria.features.chat.globalchat.GlobalChat;
import io.hamlook.aetheria.features.chat.globalchat.image.GCImage;
import io.hamlook.aetheria.features.chat.globalchat.image.ImageManager;
import io.hamlook.aetheria.features.chat.globalchat.util.DiscordMarkdown;
import io.hamlook.aetheria.features.chat.globalchat.vars.IEmoji;
import io.hamlook.aetheria.utils.compat.*;
import io.hamlook.aetheria.utils.compat.ClipboardCompat;
import io.hamlook.aetheria.utils.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Multi-line chat input with full text selection (mouse drag + keyboard),
 * clipboard shortcuts and internal vertical scrolling. Replaces the vanilla
 * single-line GuiTextField used by the global chat UI.
 */
public class ChatInputField extends Gui {

    public static final int PAD_X = 4;
    public static final int LINE_PAD = 2;
    public static final int MAX_LENGTH = 2000;

    private final FontRenderer fr = MinecraftCompat.getFontRenderer();

    public int x, y, width, height;

    private final StringBuilder text = new StringBuilder();
    private int caret = 0;
    private int anchor = -1;
    private boolean dragging = false;

    private int scrollLines = 0;
    private int manualScroll = 0;
    private List<String> lines = new ArrayList<>();
    private List<Integer> lineStarts = new ArrayList<>();
    /** Set on any text mutation so relayout() only recomputes when the content actually changed. */
    private boolean layoutDirty = true;

    private static final Pattern EMOJI_TOKEN = Pattern.compile(":([a-zA-Z0-9_~+-]+):");
    private final Map<String, String> emojiImageCache = new HashMap<>();

    public ChatInputField(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public int lineHeight() {
        return fr.FONT_HEIGHT + LINE_PAD;
    }

    public String getText() {
        return text.toString();
    }

    public void setText(String t) {
        text.setLength(0);
        text.append(t);
        caret = text.length();
        anchor = -1;
        layoutDirty = true;
        ensureCaretVisible();
    }

    public int getCaret() {
        return caret;
    }

    public void setCaret(int pos) {
        caret = Math.max(0, Math.min(text.length(), pos));
        anchor = -1;
        ensureCaretVisible();
    }

    public boolean hasSelection() {
        return anchor != -1 && anchor != caret;
    }

    /** Selects the range [start, end); used by the emoji autocomplete to replace ":prefix". */
    public void select(int start, int end) {
        anchor = Math.max(0, Math.min(text.length(), start));
        caret = Math.max(0, Math.min(text.length(), end));
    }

    public String getSelectedText() {
        if (!hasSelection()) return "";
        return text.substring(Math.min(anchor, caret), Math.max(anchor, caret));
    }

    public void selectAll() {
        anchor = 0;
        caret = text.length();
    }

    public void replaceSelection(String rep) {
        int s = hasSelection() ? Math.min(anchor, caret) : caret;
        int e = hasSelection() ? Math.max(anchor, caret) : caret;
        int room = MAX_LENGTH - (text.length() - (e - s));
        if (rep.length() > room) {
            if (room <= 0) return;
            rep = rep.substring(0, room);
        }
        text.replace(s, e, rep);
        caret = s + rep.length();
        anchor = -1;
        layoutDirty = true;
        ensureCaretVisible();
    }

    /** Returns true if the key was consumed; plain Enter returns false so the caller can send the message. */
    public boolean keyTyped(char typedChar, int keyCode) {
        boolean ctrl = KeyboardCompat.isKeyDown(Keyboard.KEY_LCONTROL) || KeyboardCompat.isKeyDown(Keyboard.KEY_RCONTROL);
        boolean shift = KeyboardCompat.isKeyDown(Keyboard.KEY_LSHIFT) || KeyboardCompat.isKeyDown(Keyboard.KEY_RSHIFT);

        if (ctrl && keyCode == Keyboard.KEY_A) { selectAll(); return true; }
        if (ctrl && keyCode == Keyboard.KEY_C) {
            if (hasSelection()) ClipboardCompat.setClipboard(getSelectedText());
            return true;
        }
        if (ctrl && keyCode == Keyboard.KEY_X) {
            if (hasSelection()) {
                ClipboardCompat.setClipboard(getSelectedText());
                replaceSelection("");
            }
            return true;
        }
        if (ctrl && keyCode == Keyboard.KEY_V) {
            String clip = ClipboardCompat.getClipboard();
            if (clip != null) replaceSelection(clip.replace("\r\n", "\n").replace('\r', '\n'));
            return true;
        }

        switch (keyCode) {
            case Keyboard.KEY_RETURN:
            case Keyboard.KEY_NUMPADENTER:
                if (!shift) return false;
                replaceSelection("\n");
                return true;
            case Keyboard.KEY_BACK:
                if (hasSelection()) {
                    replaceSelection("");
                } else if (caret > 0) {
                    int p = ctrl ? prevWord(caret) : caret - 1;
                    text.delete(p, caret);
                    caret = p;
                    layoutDirty = true;
                    ensureCaretVisible();
                }
                return true;
            case Keyboard.KEY_DELETE:
                if (hasSelection()) {
                    replaceSelection("");
                } else if (caret < text.length()) {
                    int p = ctrl ? nextWord(caret) : caret + 1;
                    text.delete(caret, p);
                    layoutDirty = true;
                    ensureCaretVisible();
                }
                return true;
            case Keyboard.KEY_LEFT:
                if (!hasSelection() && shift) anchor = caret;
                if (hasSelection() && !shift) {
                    caret = Math.min(anchor, caret);
                    anchor = -1;
                } else {
                    caret = ctrl ? prevWord(caret) : Math.max(0, caret - 1);
                }
                ensureCaretVisible();
                return true;
            case Keyboard.KEY_RIGHT:
                if (!hasSelection() && shift) anchor = caret;
                if (hasSelection() && !shift) {
                    caret = Math.max(anchor, caret);
                    anchor = -1;
                } else {
                    caret = ctrl ? nextWord(caret) : Math.min(text.length(), caret + 1);
                }
                ensureCaretVisible();
                return true;
            case Keyboard.KEY_UP: moveVertical(-1, shift); return true;
            case Keyboard.KEY_DOWN: moveVertical(1, shift); return true;
            case Keyboard.KEY_HOME:
                if (!hasSelection() && shift) anchor = caret;
                caret = ctrl ? 0 : lineStart(caret);
                ensureCaretVisible();
                return true;
            case Keyboard.KEY_END:
                if (!hasSelection() && shift) anchor = caret;
                caret = ctrl ? text.length() : lineEnd(caret);
                ensureCaretVisible();
                return true;
            case Keyboard.KEY_TAB:
                replaceSelection("    ");
                return true;
        }

        if (typedChar >= 32 && typedChar != 127) {
            replaceSelection(String.valueOf(typedChar));
            return true;
        }
        return false;
    }

    /** Returns true if the click landed inside the field. */
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0 && mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            dragging = true;
            caret = xyToChar(mouseX, mouseY);
            anchor = caret;
            return true;
        }
        return false;
    }

    /** Called each tick; keeps the selection anchored while the left mouse button is held. */
    public void updateDrag() {
        if (!dragging) return;
        if (!MouseCompat.isButtonDown(0)) {
            dragging = false;
            return;
        }
        Minecraft mc = MinecraftCompat.getMinecraft();
        int scale = GuiScreenUtils.getScaledResolution().getScaleFactor();
        int mx = MouseCompat.getX() / scale;
        int my = GuiScreenUtils.getDisplayHeight() / scale - MouseCompat.getY() / scale;
        caret = xyToChar(mx, my);
    }

    public boolean isHovered(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    /** Scrolls the visible text window (wheel &gt; 0 = up) without moving the caret. */
    public void mouseWheel(int wheel) {
        relayout();
        int visibleLines = Math.max(1, height / lineHeight());
        int maxScroll = Math.max(0, lines.size() - visibleLines);
        manualScroll = Math.max(0, Math.min(maxScroll, manualScroll + (wheel > 0 ? -2 : 2)));
    }

    public void draw() {
        relayout();
        int visibleLines = Math.max(1, height / lineHeight());
        int maxScroll = Math.max(0, lines.size() - visibleLines);
        if (manualScroll > maxScroll) manualScroll = maxScroll;
        scrollLines = manualScroll;

        int ci = charToLine(caret);
        if (ci < scrollLines || ci >= scrollLines + visibleLines) {
            scrollLines = Math.max(0, Math.min(maxScroll, ci < scrollLines ? ci : ci - visibleLines + 1));
            manualScroll = scrollLines;
        }

        int ty = y + 2;
        for (int i = scrollLines; i < lines.size() && i < scrollLines + visibleLines; i++) {
            String line = lines.get(i);
            int ly = ty + (i - scrollLines) * lineHeight();
            int ls = lineStarts.get(i);

            if (hasSelection()) {
                int ss = Math.min(anchor, caret);
                int se = Math.max(anchor, caret);
                int selStart = Math.max(ss, ls);
                int selEnd = Math.min(se, ls + line.length());
                if (selEnd > selStart) {
                    int sx = x + PAD_X + stringWidth(line, selStart - ls, ls);
                    int ex = x + PAD_X + stringWidth(line, selEnd - ls, ls);
                    drawRect(sx, ly, ex, ly + fr.FONT_HEIGHT, 0x50FFFFFF);
                }
            }

            if (i == ci && (System.currentTimeMillis() / 500) % 2 == 0) {
                int cx = x + PAD_X + stringWidth(line, caret - ls, ls);
                drawRect(cx, ly, cx + 1, ly + fr.FONT_HEIGHT, 0xFFE0E0E0);
            }

            drawLineWithEmoji(line, x + PAD_X, ly, ls);
        }

        if (maxScroll > 0) {
            int trackH = height - 4;
            int thumbH = Math.max(8, trackH * visibleLines / lines.size());
            int thumbY = y + 2 + (trackH - thumbH) * scrollLines / maxScroll;
            drawRect(x + width - 3, y + 2, x + width - 1, y + 2 + trackH, 0x22000000);
            drawRect(x + width - 3, thumbY, x + width - 1, thumbY + thumbH, 0x80FFFFFF);
        }
    }

    // ------------------------------------------------------------ helpers

    private void relayout() {
        if (!layoutDirty) return;
        layoutDirty = false;
        lines.clear();
        lineStarts.clear();
        int w = Math.max(20, width - PAD_X * 2);
        int pos = 0;
        for (String raw : text.toString().split("\n", -1)) {
            String rest = raw;
            while (emojiWidth(rest, pos) > w) {
                String fit = fitToWidth(rest, w, pos);
                if (fit.isEmpty()) fit = rest.substring(0, 1);
                lines.add(fit);
                lineStarts.add(pos);
                pos += fit.length();
                rest = rest.substring(fit.length());
            }
            lines.add(rest);
            lineStarts.add(pos);
            pos += rest.length() + 1;
        }
    }

    private int charToLine(int pos) {
        relayout();
        int lo = 0, hi = lineStarts.size() - 1, ans = 0;
        while (lo <= hi) {
            int mid = (lo + hi) / 2;
            if (lineStarts.get(mid) <= pos) {
                ans = mid;
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return ans;
    }

    private int lineStart(int pos) {
        return lineStarts.get(charToLine(pos));
    }

    private int lineEnd(int pos) {
        int li = charToLine(pos);
        if (li + 1 >= lineStarts.size()) return text.length();
        int next = lineStarts.get(li + 1);
        return (next > 0 && text.charAt(next - 1) == '\n') ? next - 1 : next;
    }

    private int xyToChar(int mouseX, int mouseY) {
        relayout();
        int visibleLines = Math.max(1, height / lineHeight());
        int lineIdx = scrollLines + (mouseY - (y + 2)) / lineHeight();
        if (lineIdx < 0) return 0;
        if (lineIdx >= lines.size()) return text.length();
        String line = lines.get(lineIdx);
        int ls = lineStarts.get(lineIdx);
        int relX = mouseX - (x + PAD_X);
        if (relX <= 0) return ls;
        int charIdx = 0;
        int w = 0;
        for (Object[] seg : scanSegments(line, ls)) {
            if ("E".equals(seg[0])) {
                w += DiscordMarkdown.EMOJI_SIZE;
                if (w >= relX) return ls + charIdx + ((String) seg[1]).length() + 2;
                charIdx += ((String) seg[1]).length() + 2;
            } else {
                String t = (String) seg[1];
                for (int k = 0; k < t.length(); k++) {
                    w += fr.getStringWidth(String.valueOf(t.charAt(k)));
                    charIdx++;
                    if (w >= relX) return ls + charIdx;
                }
            }
        }
        return ls + charIdx;
    }

    private void moveVertical(int dir, boolean shift) {
        relayout();
        if (!hasSelection() && shift) anchor = caret;
        if (hasSelection() && !shift) {
            caret = Math.min(anchor, caret);
            anchor = -1;
        }
        int li = charToLine(caret);
        int target = li + dir;
        if (target < 0 || target >= lines.size()) return;
        int xOff = caret - lineStarts.get(li);
        caret = lineStarts.get(target) + Math.min(xOff, lines.get(target).length());
        ensureCaretVisible();
    }

    private int prevWord(int pos) {
        int p = pos - 1;
        while (p >= 0 && text.charAt(p) == ' ') p--;
        while (p >= 0 && text.charAt(p) != ' ') p--;
        return Math.max(0, p + 1);
    }

    private int nextWord(int pos) {
        int p = pos;
        while (p < text.length() && text.charAt(p) == ' ') p++;
        while (p < text.length() && text.charAt(p) != ' ') p++;
        return Math.min(text.length(), p);
    }

    private int stringWidth(String line, int count, int absStart) {
        return emojiWidth(line.substring(0, Math.max(0, Math.min(count, line.length()))), absStart);
    }

    private void ensureCaretVisible() {
        relayout();
        int visibleLines = Math.max(1, height / lineHeight());
        int ci = charToLine(caret);
        if (ci < scrollLines || ci >= scrollLines + visibleLines) {
            scrollLines = Math.max(0, Math.min(Math.max(0, lines.size() - visibleLines),
                    ci < scrollLines ? ci : ci - visibleLines + 1));
            manualScroll = scrollLines;
        }
        if (scrollLines < 0) scrollLines = 0;
    }

    // ------------------------------------------------------------- emojis

    private static boolean isEmojiChar(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_' || c == '~' || c == '+' || c == '-';
    }

    private boolean isEmoji(String name) {
        return name != null && GlobalChat.usableEmojis.containsKey(name);
    }

    private String emojiUrl(String name) {
        IEmoji emoji = GlobalChat.usableEmojis.get(name);
        return emoji == null ? null : emoji.toEmoji().url;
    }

    /** Original (newline-split) line index for an absolute character position. */
    private int originalLineOf(int absPos) {
        int line = 0;
        for (int i = 0; i < absPos && i < text.length(); i++) {
            if (text.charAt(i) == '\n') line++;
        }
        return line;
    }

    /** True if the original line at origLine is inside a fenced ``` code block. */
    private boolean inFenceAt(int origLine) {
        boolean fence = false;
        int start = 0;
        for (int l = 0; l < origLine; l++) {
            int end = text.indexOf("\n", start);
            if (end < 0) end = text.length();
            if (text.substring(start, end).trim().startsWith("```")) fence = !fence;
            start = end + 1;
        }
        return fence;
    }

    /** True if absPos (within its original line) is inside an inline `code` span. */
    private boolean inInlineCodeAt(int origLine, int absPos) {
        int start = 0;
        for (int l = 0; l < origLine; l++) {
            int end = text.indexOf("\n", start);
            if (end < 0) return false;
            start = end + 1;
        }
        boolean code = false;
        for (int i = start; i < absPos && i < text.length(); i++) {
            if (text.charAt(i) == '`') code = !code;
        }
        return code;
    }

    /**
     * Splits a line into text/emoji segments, skipping emoji tokens that are
     * escaped (preceded by a backslash), inside a fenced code block, or inside
     * an inline code span. Consistent with {@link #emojiWidth}.
     */
    private List<Object[]> scanSegments(String line, int absStart) {
        List<Object[]> segs = new ArrayList<>();
        int origLine = originalLineOf(absStart);
        if (inFenceAt(origLine)) {
            segs.add(new Object[]{"T", line});
            return segs;
        }
        boolean code = inInlineCodeAt(origLine, absStart);
        Matcher m = EMOJI_TOKEN.matcher(line);
        int last = 0;
        while (m.find()) {
            for (int k = last; k < m.start(); k++) {
                if (line.charAt(k) == '`') code = !code;
            }
            if (code) continue;
            int absPos = absStart + m.start();
            if (absPos > 0 && text.charAt(absPos - 1) == '\\') continue;
            if (!isEmoji(m.group(1))) continue;
            if (m.start() > last) {
                segs.add(new Object[]{"T", line.substring(last, m.start())});
            }
            segs.add(new Object[]{"E", m.group(1), emojiUrl(m.group(1))});
            last = m.end();
        }
        if (last < line.length()) segs.add(new Object[]{"T", line.substring(last)});
        return segs;
    }

    /** Width of the text treating rendered emoji tokens as a single fixed-size glyph. */
    private int emojiWidth(String s, int absStart) {
        int w = 0;
        for (Object[] seg : scanSegments(s, absStart)) {
            if ("E".equals(seg[0])) {
                w += DiscordMarkdown.EMOJI_SIZE;
            } else {
                w += fr.getStringWidth((String) seg[1]);
            }
        }
        return w;
    }

    /** Shrinks the string until it fits the width, never splitting a rendered emoji token at the cut point. */
    private String fitToWidth(String rest, int w, int absStart) {
        String fit = rest;
        while (fit.length() > 1 && emojiWidth(fit, absStart) > w) {
            int cut = fit.length() - 1;
            if (fit.charAt(cut) == ':') {
                int s = cut - 1;
                while (s >= 0 && isEmojiChar(fit.charAt(s))) s--;
                if (s >= 0 && fit.charAt(s) == ':' && s < cut - 1) {
                    cut = s;
                }
            }
            fit = fit.substring(0, cut);
        }
        return fit;
    }

    /** Draws the line, rendering emoji tokens as images and everything else as text. */
    private void drawLineWithEmoji(String line, int x, int y, int absStart) {
        for (Object[] seg : scanSegments(line, absStart)) {
            if ("E".equals(seg[0])) {
                drawInlineEmoji((String) seg[2], (String) seg[1], x, y - 3);
                x += DiscordMarkdown.EMOJI_SIZE;
            } else {
                String text = (String) seg[1];
                fr.drawString(text, x, y, 0xFFE0E0E0);
                x += fr.getStringWidth(text);
            }
        }
    }

    private void drawInlineEmoji(String url, String name, int x, int y) {
        if (url == null || url.isEmpty()) return;
        IEmoji known = name == null ? null : GlobalChat.usableEmojis.get(name);
        boolean isDefault = known != null && known.surrogates != null && !known.surrogates.isEmpty();
        if (isDefault && RenderUtils.drawEmoji(name, x, y, DiscordMarkdown.EMOJI_SIZE)) return;
        String id = emojiImageCache.get(url);
        if (id == null) {
            id = GCImage.createGCImage(url, false);
            emojiImageCache.put(url, id);
        }
        GCImage img = ImageManager.images.get(id);
        if (img == null || !img.isLoaded || img.width == 0) {
            drawRect(x, y, x + DiscordMarkdown.EMOJI_SIZE, y + DiscordMarkdown.EMOJI_SIZE, 0x22FFFFFF);
            return;
        }
        ResourceLocation tex = img.getTextureToRender(false);
        if (tex == null) return;
        MinecraftCompat.getMinecraft().getTextureManager().bindTexture(tex);
        GlStateManagerCompat.color(1f, 1f, 1f, 1f);
        GlStateManagerCompat.enableBlend();
        drawScaledCustomSizeModalRect(x, y, 0, 0, img.width, img.height,
                DiscordMarkdown.EMOJI_SIZE, DiscordMarkdown.EMOJI_SIZE, img.width, img.height);
        GlStateManagerCompat.disableBlend();
    }
}
