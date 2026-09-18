package io.hamlook.aetheria.utils.hooks;

import io.hamlook.aetheria.OptionsMenu;
import io.hamlook.aetheria.utils.compat.GuiScreenUtils;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
//#if MC < 1.21
import net.minecraft.client.gui.GuiButton;
import java.util.List;
//#else
//$$ import net.minecraft.client.gui.Element;
//$$ import net.minecraft.client.gui.screen.Screen;
//$$ import net.minecraft.client.gui.widget.ButtonWidget;
//$$ import net.minecraft.client.gui.widget.ClickableWidget;
//$$ import net.minecraft.text.Text;
//#endif

public final class GuiIngameMenuHook {

    public static final int BTN_ATHR = 0x4EF;

    private GuiIngameMenuHook() {
    }

    //#if MC < 1.21
    public static void addButton(List<GuiButton> buttonList, int width, int height) {
        int lowestY = height / 4 + 8;
        for (GuiButton btn : buttonList) {
            lowestY = Math.max(lowestY, GuiScreenUtils.getY(btn) + GuiScreenUtils.getH(btn));
        }
        buttonList.add(new GuiButton(BTN_ATHR, width / 2 - 100, lowestY + 4, 200, 20, "Aetheria Mod"));
    }
    //#else
    //$$ public static void addButton(Screen screen, int width, int height) {
    //$$     int lowestY = height / 4 + 8;
    //$$     for (Element e : screen.children()) {
    //$$         if (e instanceof ClickableWidget w) {
    //$$             lowestY = Math.max(lowestY, GuiScreenUtils.getY(w) + GuiScreenUtils.getH(w));
    //$$         }
    //$$     }
    //$$     GuiScreenUtils.addButton(screen, ButtonWidget.builder(Text.literal("Aetheria Mod"), b -> MinecraftCompat.getMinecraft().displayGuiScreen(new OptionsMenu())).dimensions(width / 2 - 100, lowestY + 4, 200, 20).build());
    //$$ }
    //#endif

    //#if MC < 1.21
    public static void actionPerformed(GuiButton button) {
        if (button.id == BTN_ATHR) {
            MinecraftCompat.getMinecraft().displayGuiScreen(new OptionsMenu());
        }
    }
    //#endif
}
