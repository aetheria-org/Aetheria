package io.hamlook.aetheria.features.custommenu.editor;

import io.hamlook.aetheria.core.moulconfig.gui.GuiElement;
import io.hamlook.aetheria.features.custommenu.ui.CMMElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;

import java.util.function.Consumer;

/**
 * Right-click command palette for the editor canvas.
 */
public class ContextMenuGui extends GuiElement {
    private final CMMElement target;
    private final Consumer<String> action;
    private int x, y;

    public ContextMenuGui(int x, int y, CMMElement target, Consumer<String> action) {
        this.x = x;
        this.y = y;
        this.target = target;
        this.action = action;
    }

    @Override
    public void render() {
        int h = target == null ? 44 : 64;
        Gui.drawRect(x, y, x + 170, y + h, 0xf01b222c);
        Gui.drawRect(x, y, x + 170, y + 2, 0xff8fd7ff);
        Minecraft.getMinecraft().fontRendererObj.drawString(target == null ? "Background" : "Element", x + 8, y + 8, 0xffffffff);
        Minecraft.getMinecraft().fontRendererObj.drawString(target == null ? "Upload new image" : "Edit parameters", x + 8, y + 25, 0xffdbeeff);
        if (target != null) {
            Minecraft.getMinecraft().fontRendererObj.drawString("Delete element", x + 8, y + 43, 0xffff9999);
        }
    }

    @Override
    public boolean mouseInput(int mx, int my) {
        int h = target == null ? 44 : 64;
        if (mx < x || mx > x + 170 || my < y || my > y + h) {
            action.accept("close");
            return true;
        }
        if (my >= y + 18 && my < y + 38) action.accept(target == null ? "background" : "edit");
        else if (target != null && my >= y + 38) action.accept("delete");
        return true;
    }

    @Override
    public boolean keyboardInput() {
        return false;
    }
}
