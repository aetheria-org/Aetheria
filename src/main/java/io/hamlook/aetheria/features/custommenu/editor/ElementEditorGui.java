package io.hamlook.aetheria.features.custommenu.editor;

import io.hamlook.aetheria.core.moulconfig.gui.GuiElement;
import io.hamlook.aetheria.core.moulconfig.gui.GuiElementTextField;
import io.hamlook.aetheria.features.chat.globalchat.image.GCImage;
import io.hamlook.aetheria.features.chat.globalchat.image.ImageManager;
import io.hamlook.aetheria.features.custommenu.Position;
import io.hamlook.aetheria.features.custommenu.ui.CMMElement;
import io.hamlook.aetheria.features.custommenu.ui.buttons.ButtonStyle;
import io.hamlook.aetheria.features.custommenu.ui.buttons.CMMButton;
import io.hamlook.aetheria.features.custommenu.ui.sprites.Sprite;
import io.hamlook.aetheria.features.custommenu.ui.text.Text;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;

import java.util.function.Consumer;

/**
 * Shared parameter editor used by right-click and double-click editing.
 */
public class ElementEditorGui extends GuiElement {
    private final CMMElement element;
    private final Consumer<Boolean> close;
    private final GuiElementTextField value;
    private final GuiElementTextField width = new GuiElementTextField("", 65, 20, GuiElementTextField.NUM_ONLY);
    private final GuiElementTextField height = new GuiElementTextField("", 65, 20, GuiElementTextField.NUM_ONLY);
    private final GuiElementTextField scale = new GuiElementTextField("", 65, 20, 0);
    private int x, y;

    public ElementEditorGui(CMMElement element, Consumer<Boolean> close) {
        this.element = element;
        this.close = close;
        value = new GuiElementTextField(element instanceof Text ? ((Text) element).text : element instanceof CMMButton ? ((CMMButton) element).displayString : "", 220, 20, 0);
        width.setText(String.valueOf(Math.max(1, element.width)));
        height.setText(String.valueOf(Math.max(1, element.height)));
        scale.setText(element instanceof Text ? String.valueOf(((Text) element).scale) : "1.0");
    }

    @Override
    public void render() {
        int w = 340, h = element instanceof Text ? 185 : 210;
        x = (Minecraft.getMinecraft().currentScreen.width - w) / 2;
        y = (Minecraft.getMinecraft().currentScreen.height - h) / 2;
        Gui.drawRect(x, y, x + w, y + h, 0xf0181d26);
        Gui.drawRect(x + 1, y + 1, x + w - 1, y + 3, 0xff8fd7ff);
        Minecraft.getMinecraft().fontRendererObj.drawString("Edit element", x + 12, y + 12, 0xffffffff);
        Minecraft.getMinecraft().fontRendererObj.drawString(element instanceof Sprite ? "Image path" : element instanceof Text ? "Display text" : "Display text / GUI action", x + 12, y + 34, 0xffdddddd);
        value.render(x + 12, y + 46);
        if (element instanceof Text) {
            Minecraft.getMinecraft().fontRendererObj.drawString("Scale", x + 12, y + 76, 0xffffffff);
            scale.render(x + 65, y + 70);
            Minecraft.getMinecraft().fontRendererObj.drawString("Placeholders: " + (((Text) element).placeholders ? "ON" : "OFF") + " (click)", x + 12, y + 102, 0xffdbeeff);
        } else {
            Minecraft.getMinecraft().fontRendererObj.drawString("Width", x + 12, y + 78, 0xffffffff);
            width.render(x + 55, y + 72);
            Minecraft.getMinecraft().fontRendererObj.drawString("Height", x + 140, y + 78, 0xffffffff);
            height.render(x + 195, y + 72);
            if (element instanceof CMMButton)
                Minecraft.getMinecraft().fontRendererObj.drawString("Style: " + ((CMMButton) element).style.label + " (click)", x + 12, y + 108, 0xffdbeeff);
        }
        Minecraft.getMinecraft().fontRendererObj.drawString("[ Save ]", x + 245, y + h - 25, 0xff8fd7ff);
        Minecraft.getMinecraft().fontRendererObj.drawString("[ Cancel ]", x + 285, y + h - 25, 0xffff9999);
    }

    @Override
    public boolean mouseInput(int mx, int my) {
        int h = element instanceof Text ? 185 : 210;
        if (my >= y + h - 38 && mx >= x + 235) {
            save();
            return true;
        }
        if (my >= y + h - 38 && mx >= x + 280) {
            close.accept(false);
            return true;
        }
        if (element instanceof Text && my >= y + 90 && my < y + 120) {
            ((Text) element).placeholders = !((Text) element).placeholders;
            return true;
        }
        if (element instanceof CMMButton && my >= y + 98 && my < y + 125) {
            ButtonStyle[] s = ButtonStyle.values();
            ((CMMButton) element).style = s[(((CMMButton) element).style.ordinal() + 1) % s.length];
            return true;
        }
        if (my >= y + 42 && my < y + 72) value.mouseClicked(mx, my, 0);
        if (element instanceof Text && my >= y + 68 && my < y + 100) scale.mouseClicked(mx, my, 0);
        else if (!(element instanceof Text) && my >= y + 68 && my < y + 100) {
            width.mouseClicked(mx, my, 0);
            height.mouseClicked(mx, my, 0);
        }
        return true;
    }

    private void save() {
        if (element instanceof Text) {
            try {
                ((Text) element).scale = Math.max(.25f, Float.parseFloat(scale.getText()));
            } catch (Exception ignored) {
            }
            ((Text) element).text = value.getText();
        } else {
            try {
                element.width = Math.max(8, Integer.parseInt(width.getText()));
                element.height = Math.max(8, Integer.parseInt(height.getText()));
            } catch (Exception ignored) {
            }
            if (element instanceof CMMButton) ((CMMButton) element).displayString = value.getText();
            if (element instanceof Sprite) {
                String path = value.getText().trim();
                if (path.isEmpty()) path = CMMImagePicker.pick();
                if (path != null && !path.isEmpty()) ((Sprite) element).image = ImageManager.images.get(GCImage.createGCImageFromFile(path));
            }
        }
        close.accept(true);
    }

    public void keyTyped(char c, int code) {
        value.keyTyped(c, code);
        width.keyTyped(c, code);
        height.keyTyped(c, code);
        scale.keyTyped(c, code);
    }

    @Override
    public boolean keyboardInput() {
        return true;
    }
}
