package io.hamlook.aetheria.features.dungeons.rooms.report;

import io.hamlook.aetheria.utils.compat.DrawContextUtils;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;

import java.util.ArrayList;
import java.util.List;
import io.hamlook.aetheria.utils.compat.GuiScreenUtils;

public class GuiDropdownTextField extends GuiTextField {
    private final List<String> allOptions;
    private final List<String> filteredOptions;
    private final int maxSuggestions = 5;
    private final int dropdownItemHeight = 12;

    public GuiDropdownTextField(int componentId, FontRenderer fontrendererObj, int x, int y, int width, int height, List<String> options) {
        super(componentId, fontrendererObj, x, y, width, height);
        this.allOptions = options;
        this.filteredOptions = new ArrayList<>();
    }

    @Override
    public boolean textboxKeyTyped(char typedChar, int keyCode) {
        boolean result = super.textboxKeyTyped(typedChar, keyCode);
        if (result || keyCode == org.lwjgl.input.Keyboard.KEY_BACK) {
            updateSuggestions();
        }
        return result;
    }

    private void updateSuggestions() {
        filteredOptions.clear();
        String currentText = this.getText().toLowerCase();

        if (currentText.isEmpty()) {
            return;
        }

        for (String option : allOptions) {
            if (option.toLowerCase().contains(currentText)) {
                filteredOptions.add(option);
            }
        }
    }

    public void drawDropdown(int mouseX, int mouseY) {
        if (!this.isFocused()) return;

        int dropY = GuiScreenUtils.getY(this) + GuiScreenUtils.getH(this);
        int count = Math.min(filteredOptions.size(), maxSuggestions);
        int totalHeight = count * dropdownItemHeight;

        int borderColor = 0xFFA0A0A0;
        DrawContextUtils.fill(GuiScreenUtils.getX(this) - 1, dropY, GuiScreenUtils.getX(this), dropY + totalHeight, borderColor); // Left border
        DrawContextUtils.fill(GuiScreenUtils.getX(this) + GuiScreenUtils.getW(this), dropY, GuiScreenUtils.getX(this) + GuiScreenUtils.getW(this) + 1, dropY + totalHeight, borderColor); // Right border
        DrawContextUtils.fill(GuiScreenUtils.getX(this) - 1, dropY + totalHeight, GuiScreenUtils.getX(this) + GuiScreenUtils.getW(this) + 1, dropY + totalHeight + 1, borderColor); // Bottom border

        DrawContextUtils.fill(GuiScreenUtils.getX(this), dropY, GuiScreenUtils.getX(this) + GuiScreenUtils.getW(this), dropY + totalHeight, 0xE6000000);

        for (int i = 0; i < count; i++) {
            int itemMinY = dropY + (i * dropdownItemHeight);
            int itemMaxY = itemMinY + dropdownItemHeight;

            boolean isHovered = mouseX >= GuiScreenUtils.getX(this) && mouseX <= GuiScreenUtils.getX(this) + GuiScreenUtils.getW(this) &&
                    mouseY >= itemMinY && mouseY < itemMaxY;

            if (isHovered) {
                DrawContextUtils.fill(GuiScreenUtils.getX(this), itemMinY, GuiScreenUtils.getX(this) + GuiScreenUtils.getW(this), itemMaxY, 0x44FFFFFF);
            }

            int textColor = isHovered ? 0xFFFFA0 : 0xFFFFFF;

            MinecraftCompat.getFontRenderer().drawStringWithShadow(
                    filteredOptions.get(i),
                    GuiScreenUtils.getX(this) + 4,
                    itemMinY + 2,
                    textColor
            );
        }
    }

    public boolean mouseClickedDropdown(int mouseX, int mouseY, int mouseButton) {
        if (!this.isFocused() || filteredOptions.isEmpty()) return false;

        int dropY = GuiScreenUtils.getY(this) + GuiScreenUtils.getH(this);
        int count = Math.min(filteredOptions.size(), maxSuggestions);

        if (mouseX >= GuiScreenUtils.getX(this) && mouseX <= GuiScreenUtils.getX(this) + GuiScreenUtils.getW(this) &&
                mouseY >= dropY && mouseY < dropY + (count * dropdownItemHeight)) {

            int index = (mouseY - dropY) / dropdownItemHeight;
            this.setText(filteredOptions.get(index));
            this.filteredOptions.clear();
            this.setFocused(false);
            return true;
        }
        return false;
    }
}
