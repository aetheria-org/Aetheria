package io.hamlook.aetheria.features.chat.globalchat.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.List;

/**
 * Multi-line chat input with full text selection (mouse drag + keyboard),
 * clipboard shortcuts and internal vertical scrolling. Replaces the vanilla
 * single-line GuiTextField used by the global chat UI.
 */
public class ChatInputField extends Gui {

    public static final int PAD_X = 4;
    public static final int LINE_PAD = 2;
    public static final int MAX_LENGTH = 2000;

    private final FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;

    public int x, y, width, height;

    private final StringBuilder text = new StringBuilder();
    private int caret = 0;
    private int anchor = -1;
    private boolean dragging = false;

    private int scrollLines = 0;
    private int manualScroll = 0;
    private List<String> lines = new ArrayList<>();
    private List<Integer> lineStarts = new ArrayList<>();

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
        ensureCaretVisible();
    }

    /** Returns true if the key was consumed; plain Enter returns false so the caller can send the message. */
    public boolean keyTyped(char typedChar, int keyCode) {
        boolean ctrl = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
        boolean shift = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);

        if (ctrl && keyCode == Keyboard.KEY_A) { selectAll(); return true; }
        if (ctrl && keyCode == Keyboard.KEY_C) {
            if (hasSelection()) GuiScreen.setClipboardString(getSelectedText());
            return true;
        }
        if (ctrl && keyCode == Keyboard.KEY_X) {
            if (hasSelection()) {
                GuiScreen.setClipboardString(getSelectedText());
                replaceSelection("");
            }
            return true;
        }
        if (ctrl && keyCode == Keyboard.KEY_V) {
            String clip = GuiScreen.getClipboardString();
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
                    ensureCaretVisible();
                }
                return true;
            case Keyboard.KEY_DELETE:
                if (hasSelection()) {
                    replaceSelection("");
                } else if (caret < text.length()) {
                    int p = ctrl ? nextWord(caret) : caret + 1;
                    text.delete(caret, p);
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
        if (!Mouse.isButtonDown(0)) {
            dragging = false;
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        int scale = new ScaledResolution(mc).getScaleFactor();
        int mx = Mouse.getX() / scale;
        int my = mc.displayHeight / scale - Mouse.getY() / scale;
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
                    int sx = x + PAD_X + stringWidth(line, selStart - ls);
                    int ex = x + PAD_X + stringWidth(line, selEnd - ls);
                    drawRect(sx, ly, ex, ly + fr.FONT_HEIGHT, 0x50FFFFFF);
                }
            }

            if (i == ci && (System.currentTimeMillis() / 500) % 2 == 0) {
                int cx = x + PAD_X + stringWidth(line, caret - ls);
                drawRect(cx, ly, cx + 1, ly + fr.FONT_HEIGHT, 0xFFE0E0E0);
            }

            fr.drawString(line, x + PAD_X, ly, 0xFFE0E0E0);
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
        lines.clear();
        lineStarts.clear();
        int w = Math.max(20, width - PAD_X * 2);
        int pos = 0;
        for (String raw : text.toString().split("\n", -1)) {
            String rest = raw;
            while (fr.getStringWidth(rest) > w) {
                String fit = fr.trimStringToWidth(rest, w);
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
        int best = line.length();
        for (int ci = 1; ci <= line.length(); ci++) {
            if (fr.getStringWidth(line.substring(0, ci)) >= relX) {
                best = ci;
                break;
            }
        }
        return ls + best;
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

    private int stringWidth(String line, int count) {
        return fr.getStringWidth(line.substring(0, Math.max(0, Math.min(count, line.length()))));
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
}
