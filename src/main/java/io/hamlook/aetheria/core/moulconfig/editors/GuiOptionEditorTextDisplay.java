package io.hamlook.aetheria.core.moulconfig.editors;

import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigProcessor;
import io.hamlook.aetheria.utils.render.RenderUtils;
import io.hamlook.aetheria.utils.render.TextRenderUtils;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

public class GuiOptionEditorTextDisplay extends GuiOptionEditor {

    private static int HEIGHT = 60;
    private final String text;

    public GuiOptionEditorTextDisplay(ConfigProcessor.ProcessedOption option,String text) {
        super(option);
        this.text = text;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public void render(int x, int y, int width) {
        Minecraft mc = Minecraft.getMinecraft();

        RenderUtils.drawFloatingRectDark(x, y, width, HEIGHT, true);

        String text = this.text;
        List<String> texts = new ArrayList<>();
        int maxLineWidth = (int) (width * 0.9);
        String activeFormatting = "";
        String remaining = text;
        while (!remaining.isEmpty()) {
            if (mc.fontRendererObj.getStringWidth(remaining) <= maxLineWidth) {
                texts.add(activeFormatting + remaining);
                break;
            }
            int splitPos = remaining.length();
            while (splitPos > 0 && mc.fontRendererObj.getStringWidth(remaining.substring(0, splitPos)) > maxLineWidth) {
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
        int fontHeight = mc.fontRendererObj.FONT_HEIGHT;
        HEIGHT = Math.max(HEIGHT, (10 + fontHeight * texts.size()));
        int totalTextHeight = fontHeight * texts.size();
        int startY = y + (HEIGHT - totalTextHeight) / 2;
        for (int i = 0; i < texts.size(); i++) {
            String s = texts.get(i);
            TextRenderUtils.drawStringScaleAware(s, x + 5, startY + fontHeight * i, 1f, false);
        }
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
