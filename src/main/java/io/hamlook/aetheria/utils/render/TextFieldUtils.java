package io.hamlook.aetheria.utils.render;

import io.hamlook.aetheria.utils.compat.DrawContextUtils;
import io.hamlook.aetheria.utils.compat.GuiScreenUtils;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;

public final class TextFieldUtils {

    private TextFieldUtils() {
    }

    private static int innerWidth(int width) {
        return width - 8;
    }

    public static int computeScrollX(FontRenderer fr, String text, int cursorPos, int width, int currentScroll) {
        int innerW = innerWidth(width);
        int prefixWidth = fr.getStringWidth(text.substring(0, Math.min(cursorPos, text.length())));
        if (fr.getStringWidth(text) <= innerW) return 0;
        if (prefixWidth - 4 < currentScroll) return prefixWidth - 4;
        if (prefixWidth + 4 > currentScroll + innerW) return prefixWidth + 4 - innerW;
        return Math.max(0, Math.min(currentScroll, fr.getStringWidth(text) - innerW));
    }

    public static void renderInput(FontRenderer fr, GuiTextField field, int x, int y, int width, int height,
                                   int scrollX) {
        String text = field.getText();
        if (text == null) text = "";
        int cursor = Math.min(GuiScreenUtils.getTextFieldCursor(field), text.length());
        int innerX = x + 4;
        int top = y + (height - 10) / 2;

        //#if MC < 1.16
        int selEnd = field.getSelectionEnd();
        int selStart = Math.min(selEnd, cursor);
        int selFinish = Math.max(selEnd, cursor);
        //#else
        //$$ int selStart = 0;
        //$$ int selFinish = 0;
        //$$ String selectedText = field.getSelectedText();
        //$$ if (selectedText != null && !selectedText.isEmpty()) {
        //$$     int idx = text.indexOf(selectedText);
        //$$     if (idx >= 0) {
        //$$         selStart = idx;
        //$$         selFinish = idx + selectedText.length();
        //$$     }
        //$$ }
        //#endif

        if (selFinish > selStart) {
            int sx = innerX - scrollX + fr.getStringWidth(text.substring(0, Math.min(selStart, text.length())));
            int ex = innerX - scrollX + fr.getStringWidth(text.substring(0, Math.min(selFinish, text.length())));
            DrawContextUtils.fill(sx, top, ex, top + 10, 0x6602AEAE);
        }

        int drawX = innerX - scrollX;
        DrawContextUtils.drawTextWithShadow(fr, text, drawX, y + (height - 8) / 2, 0xFFFFFF);

        if (field.isFocused() && (System.currentTimeMillis() / 400) % 2 == 0) {
            int caretX = drawX + fr.getStringWidth(text.substring(0, cursor));
            DrawContextUtils.fill(caretX, top, caretX + 1, top + 10, 0xFFFFFFFF);
        }
    }
}
