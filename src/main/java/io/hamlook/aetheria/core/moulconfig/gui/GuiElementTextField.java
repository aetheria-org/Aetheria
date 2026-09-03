// SPDX-License-Identifier: LGPL-3.0-only
// Derived from MoulConfig (https://github.com/NotEnoughUpdates/MoulConfig)

package io.hamlook.aetheria.core.moulconfig.gui;

import io.hamlook.aetheria.utils.StringUtils;
import io.hamlook.aetheria.utils.compat.GlStateManagerCompat;
import io.hamlook.aetheria.utils.compat.GuiScreenUtils;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.render.TextRenderUtils;
import lombok.Setter;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;

import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GuiElementTextField {

    public static final int SCISSOR_TEXT = 0b10000000;
    public static final int DISABLE_BG = 0b1000000;
    public static final int SCALE_TEXT = 0b100000;
    public static final int NUM_ONLY = 0b10000;
    public static final int NO_SPACE = 0b01000;
    public static final int FORCE_CAPS = 0b00100;
    public static final int COLOUR = 0b00010;
    public static final int MULTILINE = 0b00001;
    private static final int searchBarPadding = 2;
    private static final Pattern PATTERN_CONTROL_CODE = Pattern.compile("(?i)\\u00A7([^\\u00B6]|$)(?!\\u00B6)");
    private final GuiTextField textField = new GuiTextField(0, MinecraftCompat.getMinecraft().fontRendererObj, 0, 0, 0, 0);
    private int searchBarYSize;
    private int searchBarXSize;
    @Setter
    private int options;
    private boolean focus = false;
    private int x, y;
    @Setter
    private String prependText = "";
    @Setter
    private int customTextColour = 0xffffffff;
    @Setter
    private int customBorderColour = -1;

    public GuiElementTextField(String initialText, int options) {
        this(initialText, 100, 20, options);
    }

    public GuiElementTextField(String initialText, int sizeX, int sizeY, int options) {
        textField.setFocused(true);
        textField.setCanLoseFocus(false);
        textField.setMaxStringLength(999);
        textField.setText(initialText);
        this.searchBarXSize = sizeX;
        this.searchBarYSize = sizeY;
        this.options = options;
    }

    public void setMaxStringLength(int len) {
        textField.setMaxStringLength(len);
    }

    public boolean getFocus() {
        return focus;
    }

    public void setFocus(boolean focus) {
        this.focus = focus;
        if (!focus) textField.setCursorPosition(textField.getCursorPosition());
    }

    public String getText() {
        return textField.getText();
    }

    public void setText(String text) {
        if (textField.getText() == null || !textField.getText().equals(text)) textField.setText(text);
    }

    public String getTextDisplay() {
        String t = getText();
        Matcher m;
        while ((m = PATTERN_CONTROL_CODE.matcher(t)).find()) {
            t = m.replaceFirst("¶" + m.group(1));
        }
        return t;
    }

    public void setSize(int x, int y) {
        searchBarXSize = x;
        searchBarYSize = y;
    }

    public void unfocus() {
        focus = false;
        textField.setSelectionPos(textField.getCursorPosition());
    }

    public int getHeight() {
        int paddingUnscaled = searchBarPadding / GuiScreenUtils.getScaledResolution().getScaleFactor();
        int numLines = org.apache.commons.lang3.StringUtils.countMatches(textField.getText(), "\n") + 1;
        int extraSize = (searchBarYSize - 8) / 2 + 8;
        return searchBarYSize + extraSize * (numLines - 1) + paddingUnscaled * 2;
    }

    public int getWidth() {
        int paddingUnscaled = searchBarPadding / GuiScreenUtils.getScaledResolution().getScaleFactor();
        return searchBarXSize + paddingUnscaled * 2;
    }

    public int strLenNoColor(String str) {
        return StringUtils.cleanColour(str).length();
    }

    public int getCursorPos(int mouseX, int mouseY) {
        int xComp = mouseX - x, yComp = mouseY - y;
        int extraSize = (searchBarYSize - 8) / 2 + 8;
        String renderText = prependText + textField.getText();
        int lineNum = Math.round(((yComp - (searchBarYSize - 8) / 2f)) / extraSize);

        String text = renderText, textNoColour = renderText;
        if ((options & COLOUR) != 0) {
            Matcher m;
            while ((m = PATTERN_CONTROL_CODE.matcher(text)).find()) {
                String code = m.group(1);
                text = m.replaceFirst(code.isEmpty() ? "§r¶" : "§" + code + "¶" + code);
            }
        }
        Matcher m;
        while ((m = PATTERN_CONTROL_CODE.matcher(textNoColour)).find()) {
            textNoColour = m.replaceFirst("¶" + m.group(1));
        }

        int currentLine = 0, cursorIndex = 0;
        for (; cursorIndex < textNoColour.length(); cursorIndex++) {
            if (currentLine == lineNum) break;
            if (textNoColour.charAt(cursorIndex) == '\n') currentLine++;
        }

        String textNC = textNoColour.substring(0, cursorIndex);
        int colorCodes = org.apache.commons.lang3.StringUtils.countMatches(textNC, "¶");
        String line = text.substring(cursorIndex + (((options & COLOUR) != 0) ? colorCodes * 2 : 0)).split("\n")[0];
        int padding = Math.min(5, searchBarXSize - strLenNoColor(line)) / 2;
        String trimmed = MinecraftCompat.getMinecraft().fontRendererObj.trimStringToWidth(line, xComp - padding);
        int linePos = strLenNoColor(trimmed);
        if (linePos != strLenNoColor(line)) {
            char after = line.charAt(linePos);
            if (MinecraftCompat.getMinecraft().fontRendererObj.getStringWidth(trimmed) + MinecraftCompat.getMinecraft().fontRendererObj.getCharWidth(after) / 2 < xComp - padding)
                linePos++;
        }
        cursorIndex += linePos;
        int pre = StringUtils.cleanColour(prependText).length();
        return cursorIndex < pre ? 0 : cursorIndex - pre;
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 1) textField.setText("");
        else textField.setCursorPosition(getCursorPos(mouseX, mouseY));
        focus = true;
    }

    public void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (focus) textField.setSelectionPos(getCursorPos(mouseX, mouseY));
    }

    public void keyTyped(char typedChar, int keyCode) {
        if (!focus) return;

        if ((options & MULTILINE) != 0) {
            if (keyCode == 28) {
                String before = textField.getText().substring(0, textField.getCursorPosition());
                String after = textField.getText().substring(textField.getCursorPosition());
                int pos = textField.getCursorPosition();
                textField.setText(before + "\n" + after);
                textField.setCursorPosition(pos + 1);
                return;
            }
        }

        String old = textField.getText();
        if ((options & FORCE_CAPS) != 0) typedChar = Character.toUpperCase(typedChar);
        if ((options & NO_SPACE) != 0 && typedChar == ' ') return;
        if (typedChar == '¶') typedChar = '§';

        textField.setFocused(true);
        textField.textboxKeyTyped(typedChar, keyCode);

        if ((options & COLOUR) != 0) {
            if (typedChar == '&') {
                int pos = textField.getCursorPosition() - 2;
                if (pos >= 0 && pos < textField.getText().length() && textField.getText().charAt(pos) == '&') {
                    String b = textField.getText().substring(0, pos);
                    String a = pos + 2 < textField.getText().length() ? textField.getText().substring(pos + 2) : "";
                    textField.setText(b + "§" + a);
                    textField.setCursorPosition(pos + 1);
                }
            } else if (typedChar == '*') {
                int pos = textField.getCursorPosition() - 2;
                if (pos >= 0 && pos < textField.getText().length() && textField.getText().charAt(pos) == '*') {
                    String b = textField.getText().substring(0, pos);
                    String a = pos + 2 < textField.getText().length() ? textField.getText().substring(pos + 2) : "";
                    textField.setText(b + "✪" + a);
                    textField.setCursorPosition(pos + 1);
                }
            }
        }

        if ((options & NUM_ONLY) != 0 && textField.getText().matches("[^0-9.]")) textField.setText(old);
    }

    public void render(int x, int y) {
        this.x = x;
        this.y = y;
        drawTextbox(x, y, searchBarXSize, searchBarYSize, searchBarPadding, textField, focus);
    }

    private void drawTextbox(int x, int y, int sizeX, int sizeY, int padding, GuiTextField tf, boolean focused) {
        ScaledResolution sr = GuiScreenUtils.getScaledResolution();
        String renderText = prependText + tf.getText();
        GlStateManagerCompat.disableLighting();

        int paddingUnscaled = Math.max(1, padding / sr.getScaleFactor());
        int numLines = org.apache.commons.lang3.StringUtils.countMatches(renderText, "\n") + 1;
        int extraSize = (sizeY - 8) / 2 + 8;
        int bottomTextBox = y + sizeY + extraSize * (numLines - 1);

        if ((options & DISABLE_BG) == 0) {
            int borderColour = customBorderColour != -1 ? customBorderColour : (focused ? Color.GREEN.getRGB() : Color.WHITE.getRGB());
            Gui.drawRect(x - paddingUnscaled, y - paddingUnscaled, x + sizeX + paddingUnscaled, bottomTextBox + paddingUnscaled, borderColour);
            Gui.drawRect(x, y, x + sizeX, bottomTextBox, Color.BLACK.getRGB());
        }

        String text = renderText, textNoColor = renderText;
        if ((options & COLOUR) != 0) {
            Matcher m;
            while ((m = PATTERN_CONTROL_CODE.matcher(text)).find()) {
                String code = m.group(1);
                text = m.replaceFirst(code.isEmpty() ? "§r¶" : "§" + code + "¶" + code);
            }
        }
        Matcher m;
        while ((m = PATTERN_CONTROL_CODE.matcher(textNoColor)).find()) {
            textNoColor = m.replaceFirst("¶" + m.group(1));
        }

        int xStartOffset = 5;
        float scale = 1;
        String[] texts = text.split("\n");
        for (int i = 0; i < texts.length; i++) {
            int yOff = i * extraSize;
            if (isScaling() && MinecraftCompat.getMinecraft().fontRendererObj.getStringWidth(texts[i]) > sizeX - 10) {
                scale = (sizeX - 2) / (float) MinecraftCompat.getMinecraft().fontRendererObj.getStringWidth(texts[i]);
                if (scale > 1) scale = 1;
                xStartOffset = (int) ((sizeX - MinecraftCompat.getMinecraft().fontRendererObj.getStringWidth(texts[i]) * scale) / 2f);
                TextRenderUtils.drawStringCenteredScaledMaxWidth(texts[i], MinecraftCompat.getMinecraft().fontRendererObj, x + sizeX / 2f, y + sizeY / 2f + yOff, false, sizeX - 2, customTextColour);
            } else {
                if ((options & SCISSOR_TEXT) != 0) {
                    GlScissorStack.push(x + 5, 0, x + sizeX, sr.getScaledHeight(), sr);
                    MinecraftCompat.getMinecraft().fontRendererObj.drawString(texts[i], x + 5, y + (sizeY - 8) / 2 + yOff, customTextColour);
                    GlScissorStack.pop(sr);
                } else {
                    String toRender = MinecraftCompat.getMinecraft().fontRendererObj.trimStringToWidth(texts[i], sizeX - 10);
                    MinecraftCompat.getMinecraft().fontRendererObj.drawString(toRender, x + 5, y + (sizeY - 8) / 2 + yOff, customTextColour);
                }
            }
        }

        if (focused && System.currentTimeMillis() % 1000 > 500) {
            String tNC = textNoColor.substring(0, tf.getCursorPosition() + prependText.length());
            int cc = org.apache.commons.lang3.StringUtils.countMatches(tNC, "¶");
            String tBC = text.substring(0, tf.getCursorPosition() + prependText.length() + (((options & COLOUR) != 0) ? cc * 2 : 0));
            int numLinesBC = org.apache.commons.lang3.StringUtils.countMatches(tBC, "\n");
            int yOff = numLinesBC * extraSize;
            String[] split = tBC.split("\n");
            int tBCW = (split.length <= numLinesBC || split.length == 0) ? 0 : (int) (MinecraftCompat.getMinecraft().fontRendererObj.getStringWidth(split[split.length - 1]) * scale);
            Gui.drawRect(x + xStartOffset + tBCW, y + (sizeY - 8) / 2 - 1 + yOff, x + xStartOffset + tBCW + 1, y + (sizeY - 8) / 2 + 9 + yOff, Color.WHITE.getRGB());
        }
    }

    private boolean isScaling() {
        return (options & SCALE_TEXT) != 0;
    }

    @Override
    public String toString() {
        return textField.getText();
    }
}