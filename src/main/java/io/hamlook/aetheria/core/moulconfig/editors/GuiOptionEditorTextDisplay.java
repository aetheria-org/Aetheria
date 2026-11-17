package io.hamlook.aetheria.core.moulconfig.editors;

import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigProcessor;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.render.RenderUtils;
import io.hamlook.aetheria.utils.render.TextRenderUtils;

import java.util.ArrayList;
import java.util.List;

public class GuiOptionEditorTextDisplay extends GuiOptionEditor {

    private int height = 60;
    private final String text;

    public GuiOptionEditorTextDisplay(ConfigProcessor.ProcessedOption option,String text) {
        super(option);
        this.text = text;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void render(int x, int y, int width) {
        RenderUtils.drawFloatingRectDark(x, y, width, height, true);

        List<String> texts = wrap(Math.max(1, (int) (width * 0.9)));
        int fontHeight = MinecraftCompat.getFontRenderer().FONT_HEIGHT;
        height = Math.max(height, 10 + fontHeight * texts.size());
        int totalTextHeight = fontHeight * texts.size();
        int startY = y + (height - totalTextHeight) / 2;
        for (int i = 0; i < texts.size(); i++) {
            TextRenderUtils.drawStringScaleAware(texts.get(i), x + 5, startY + fontHeight * i, 1f, false);
        }
    }

    private List<String> wrap(int maxLineWidth) {
        List<String> texts = new ArrayList<>();
        String activeFormatting = "";
        String remaining = text;
        while (!remaining.isEmpty()) {
            if (MinecraftCompat.getFontRenderer().getStringWidth(remaining) <= maxLineWidth) {
                texts.add(activeFormatting + remaining);
                break;
            }
            int splitPos = remaining.length();
            while (splitPos > 0 && MinecraftCompat.getFontRenderer().getStringWidth(remaining.substring(0, splitPos)) > maxLineWidth) {
                splitPos--;
            }
            if (splitPos == 0) splitPos = 1;
            int lastSpace = remaining.lastIndexOf(' ', splitPos);
            String fragment;
            if (lastSpace > 0) {
                fragment = remaining.substring(0, lastSpace);
                remaining = remaining.substring(lastSpace + 1).trim();
            } else {
                fragment = remaining.substring(0, splitPos);
                remaining = remaining.substring(splitPos).trim();
            }
            texts.add(activeFormatting + fragment);
            StringBuilder fmtBuilder = new StringBuilder(activeFormatting);
            for (int i = 0; i < fragment.length() - 1; i++) {
                if (fragment.charAt(i) == '§') {
                    char code = fragment.charAt(i + 1);
                    if (code == 'r') {
                        fmtBuilder.setLength(0);
                    } else {
                        fmtBuilder.append('§').append(code);
                    }
                }
            }
            activeFormatting = fmtBuilder.toString();
        }
        return texts;
    }

    @Override
    public boolean mouseInput(int x, int y, int width, int mouseX, int mouseY) {
        return false;
    }

    @Override
    public boolean keyboardInput() {
        return false;
    }
}
