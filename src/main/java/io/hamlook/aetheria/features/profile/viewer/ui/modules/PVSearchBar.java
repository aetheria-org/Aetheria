package io.hamlook.aetheria.features.profile.viewer.ui.modules;

import io.hamlook.aetheria.features.profile.viewer.ProfileViewerAPI;
import io.hamlook.aetheria.features.profile.viewer.ui.ProfileViewerGUI;
import io.hamlook.aetheria.utils.compat.GlStateManagerCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.render.NineSliceUtils;
import io.hamlook.aetheria.utils.render.TextRenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.ChatAllowedCharacters;

import java.util.ArrayList;
import java.util.List;

public class PVSearchBar {
    public int x, y, width, height;
    public String text = "";
    public String placeholder = "Search Player...";
    public boolean isFocused = false;
    public List<String> suggestions = new ArrayList<>();

    public PVSearchBar(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void draw(int mouseX, int mouseY, float uiScale) {
        NineSliceUtils.draw(ProfileViewerGUI.CONTAINER_BG, x, y, width, height, 6, 18);
        Gui.drawRect(x + 2, y + 2, x + width - 2, y + height - 2, 0x60000000);

        boolean showCursor = isFocused && (Minecraft.getSystemTime() % 1000 < 500);
        float textScale = Math.max(0.25f, uiScale * 1.8f);
        float textY = y + (height / 2f);

        // Text Start Pos without the buggy icon
        float startX = x + ProfileViewerGUI.getScaledF(12);

        if (text.isEmpty() && !isFocused) {
            TextRenderUtils.drawStringScaleAware("§7" + placeholder, startX, textY - ((MinecraftCompat.getFontRenderer().FONT_HEIGHT * textScale) / 2f), textScale, false);
        } else {
            String displayText = "§f" + text + (showCursor ? "§7|" : "");
            float fontOffset = (MinecraftCompat.getFontRenderer().FONT_HEIGHT * textScale) / 2f;
            TextRenderUtils.drawStringScaleAware(displayText, startX, textY - fontOffset, textScale, false);
        }

        // Draw the Auto-complete Dropdown if suggestions exist and search is active
        GlStateManagerCompat.pushMatrix();
        GlStateManagerCompat.translate(0, 0, 1000f);
        if (isFocused && !suggestions.isEmpty()) {
            float itemH = ProfileViewerGUI.getScaledF(22);
            float dropH = itemH * suggestions.size();
            float dropY = y + height;

            NineSliceUtils.draw(ProfileViewerGUI.CONTAINER_BG, x, (int) dropY, width, (int) dropH, 6, 18);

            for (int i = 0; i < suggestions.size(); i++) {
                String sug = suggestions.get(i);
                float itemY = dropY + (i * itemH);

                boolean isHovered = mouseX >= x && mouseX <= x + width && mouseY >= itemY && mouseY <= itemY + itemH;

                if (isHovered) {
                    Gui.drawRect(x + 4, (int) itemY, x + width - 4, (int) (itemY + itemH), 0x30FFFFFF);
                }

                TextRenderUtils.drawStringScaleAware("§f" + sug, startX, itemY + (itemH / 2f) - ((MinecraftCompat.getFontRenderer().FONT_HEIGHT * textScale) / 2f), textScale, false);
            }
        }
        GlStateManagerCompat.popMatrix();
    }

    private void updateSuggestions() {
        suggestions.clear();
        if (text.isEmpty() || !isFocused) return;

        String lowerText = text.toLowerCase();
        for (String p : ProfileViewerAPI.cachedPlayerList) {
            if (p.toLowerCase().startsWith(lowerText)) {
                suggestions.add(p);
                if (suggestions.size() >= 4) break;
            }
        }
    }

    public String mouseClicked(int mouseX, int mouseY, int button) {
        if (button == 0) {
            if (isFocused && !suggestions.isEmpty()) {
                float itemH = ProfileViewerGUI.getScaledF(22);
                float dropY = y + height;
                if (mouseX >= x && mouseX <= x + width && mouseY >= dropY && mouseY <= dropY + (itemH * suggestions.size())) {
                    int clickedIndex = (int) ((mouseY - dropY) / itemH);
                    if (clickedIndex >= 0 && clickedIndex < suggestions.size()) {
                        isFocused = false;
                        return suggestions.get(clickedIndex);
                    }
                }
            }

            isFocused = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
            if (isFocused) {
                updateSuggestions();
            } else {
                suggestions.clear();
            }
        }
        return null;
    }

    public boolean keyTyped(char typedChar, int keyCode) {
        if (!isFocused) return false;

        if (keyCode == 28 || keyCode == 156) {
            isFocused = false;
            return true;
        }

        if (keyCode == 14) {
            if (!text.isEmpty()) {
                text = text.substring(0, text.length() - 1);
                updateSuggestions();
            }
            return false;
        }

        if (text.length() < 16 && ChatAllowedCharacters.isAllowedCharacter(typedChar)) {
            text += typedChar;
            updateSuggestions();
        }

        return false;
    }
}