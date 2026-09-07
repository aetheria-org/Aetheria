package io.hamlook.aetheria.utils.hooks;

import io.hamlook.aetheria.OptionsMenu;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import net.minecraft.client.gui.GuiButton;

import java.util.List;

public final class GuiIngameMenuHook {

    public static final int BTN_ATHR = 0x4EF;

    private GuiIngameMenuHook() {
    }

    public static void addButton(List<GuiButton> buttonList, int width, int height) {
        int lowestY = height / 4 + 8;
        for (GuiButton btn : buttonList) {
            lowestY = Math.max(lowestY, btn.yPosition + btn.height);
        }
        buttonList.add(new GuiButton(BTN_ATHR, width / 2 - 100, lowestY + 4, 200, 20, "Aetheria Mod"));
    }

    public static void actionPerformed(GuiButton button) {
        if (button.id == BTN_ATHR) {
            MinecraftCompat.getMinecraft().displayGuiScreen(new OptionsMenu());
        }
    }
}
