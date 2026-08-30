package io.hamlook.aetheria.features.custommenu.editor;

import io.hamlook.aetheria.core.moulconfig.gui.GuiElement;
import io.hamlook.aetheria.core.moulconfig.gui.GuiElementTextField;
import io.hamlook.aetheria.features.chat.globalchat.image.GCImage;
import io.hamlook.aetheria.features.custommenu.Position;
import io.hamlook.aetheria.features.custommenu.ui.CMMElement;
import io.hamlook.aetheria.features.custommenu.ui.buttons.ButtonStyle;
import io.hamlook.aetheria.features.custommenu.ui.buttons.impl.ActionButton;
import io.hamlook.aetheria.features.custommenu.ui.buttons.impl.GuiButton;
import io.hamlook.aetheria.features.custommenu.ui.sprites.Sprite;
import io.hamlook.aetheria.features.custommenu.ui.text.Text;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;

import java.util.function.Consumer;

/**
 * Modal MoulConfig-style element creator used by the main-menu editor.
 */
public class AddElementGui extends GuiElement {
    public enum Type {BUTTON, TEXT, IMAGE}

    private final Consumer<CMMElement> onAdd;
    private final Runnable onCancel;
    private Type type = Type.BUTTON;
    private final GuiElementTextField text = new GuiElementTextField("New Button", 180, 20, 0);
    private final GuiElementTextField extra = new GuiElementTextField("", 180, 20, 0);
    private final GuiElementTextField width = new GuiElementTextField("160", 55, 20, GuiElementTextField.NUM_ONLY);
    private final GuiElementTextField height = new GuiElementTextField("24", 55, 20, GuiElementTextField.NUM_ONLY);
    private int actionIndex;
    private int styleIndex;
    private int x, y;

    public AddElementGui(Consumer<CMMElement> onAdd, Runnable onCancel) {
        this.onAdd = onAdd;
        this.onCancel = onCancel;
    }

    public void setType(Type type) {
        this.type = type;
        text.setText(type == Type.BUTTON ? "New Button" : type == Type.TEXT ? "New Text" : "Image file path");
        extra.setText(type == Type.BUTTON ? "ASM Options Menu" : "");
    }

    @Override
    public void render() {
        int w = 300, h = type == Type.IMAGE ? 185 : 220;
        x = (Minecraft.getMinecraft().currentScreen.width - w) / 2;
        y = (Minecraft.getMinecraft().currentScreen.height - h) / 2;
        Gui.drawRect(x, y, x + w, y + h, 0xf0181d26);
        Gui.drawRect(x + 1, y + 1, x + w - 1, y + 2, 0xff8fd7ff);
        Minecraft.getMinecraft().fontRendererObj.drawString("Add " + type.name().toLowerCase(), x + 12, y + 10, 0xffffffff);
        Minecraft.getMinecraft().fontRendererObj.drawString("Type: " + type.name() + "  (click to cycle)", x + 12, y + 30, 0xffaaaaaa);
        Minecraft.getMinecraft().fontRendererObj.drawString(type == Type.BUTTON ? "Display text" : type == Type.TEXT ? "Display text" : "Image path", x + 12, y + 48, 0xffffffff);
        text.render(x + 12, y + 60);
        if (type == Type.BUTTON) {
            Minecraft.getMinecraft().fontRendererObj.drawString("Action: " + actionLabel(), x + 12, y + 88, 0xffffffff);
            Minecraft.getMinecraft().fontRendererObj.drawString("Style: " + ButtonStyle.values()[styleIndex].label, x + 155, y + 88, 0xffffffff);
            extra.render(x + 12, y + 100);
        } else if (type == Type.TEXT) {
            Minecraft.getMinecraft().fontRendererObj.drawString("Scale (use the second field)", x + 12, y + 88, 0xffffffff);
            extra.setText(extra.getText().isEmpty() ? "1.0" : extra.getText());
            extra.render(x + 12, y + 100);
        } else {
            Minecraft.getMinecraft().fontRendererObj.drawString("Image is loaded when you press Add", x + 12, y + 88, 0xffaaaaaa);
        }
        Minecraft.getMinecraft().fontRendererObj.drawString("Width", x + 12, y + (type == Type.IMAGE ? 116 : 148), 0xffffffff);
        Minecraft.getMinecraft().fontRendererObj.drawString("Height", x + 120, y + (type == Type.IMAGE ? 116 : 148), 0xffffffff);
        width.render(x + 55, y + (type == Type.IMAGE ? 110 : 142));
        height.render(x + 120, y + (type == Type.IMAGE ? 110 : 142));
        Minecraft.getMinecraft().fontRendererObj.drawString("[ Add ]", x + 205, y + h - 25, 0xff8fd7ff);
        Minecraft.getMinecraft().fontRendererObj.drawString("[ Cancel ]", x + 245, y + h - 25, 0xffff8888);
    }

    private String actionLabel() {
        return actionIndex == 0 ? "Open GUI" : actionIndex == 1 ? "Close Menu" : "Exit Game";
    }

    @Override
    public boolean mouseInput(int mouseX, int mouseY) {
        int w = 300, h = type == Type.IMAGE ? 185 : 220;
        if (mouseX < x || mouseX > x + w || mouseY < y || mouseY > y + h) return true;
        if (mouseY >= y + 25 && mouseY <= y + 42) {
            setType(Type.values()[(type.ordinal() + 1) % Type.values().length]);
            return true;
        }
        if (type == Type.BUTTON && mouseY >= y + 82 && mouseY <= y + 98) {
            if (mouseX < x + 150) actionIndex = (actionIndex + 1) % 3;
            else styleIndex = (styleIndex + 1) % ButtonStyle.values().length;
            return true;
        }
        if (mouseY >= y + h - 38 && mouseX >= x + 195 && mouseX < x + 240) {
            add();
            return true;
        }
        if (mouseY >= y + h - 38 && mouseX >= x + 240) {
            onCancel.run();
            return true;
        }
        if (mouseY >= y + 55 && mouseY < y + 83) text.mouseClicked(mouseX, mouseY, 0);
        if (type == Type.BUTTON && mouseY >= y + 98 && mouseY < y + 128)
            extra.mouseClicked(mouseX, mouseY, 0);
        if (type == Type.TEXT && mouseY >= y + 98 && mouseY < y + 128)
            extra.mouseClicked(mouseX, mouseY, 0);
        return true;
    }

    private void add() {
        int w = number(width.getText(), 160), h = number(height.getText(), 24);
        CMMElement element;
        if (type == Type.BUTTON) {
            if (actionIndex == 0) element = new GuiButton(new Position(), w, h, text.getText(), extra.getText());
            else
                element = new ActionButton(new Position(), w, h, text.getText(), actionIndex == 1 ? ActionButton.Action.CLOSE_MENU : ActionButton.Action.EXIT);
            ((io.hamlook.aetheria.features.custommenu.ui.buttons.CMMButton) element).style = ButtonStyle.values()[styleIndex];
        } else if (type == Type.TEXT) {
            float scale = 1f;
            try {
                scale = Float.parseFloat(extra.getText());
            } catch (Exception ignored) {
            }
            element = new Text(new Position(), false, text.getText(), 0xffffffff, Math.max(.25f, scale));
        } else {
            String path = text.getText().trim();
            if (path.isEmpty() || "Image file path".equals(path)) path = CMMImagePicker.pick();
            element = new Sprite(new Position(), w, h, null, null);
            if (path != null && !path.isEmpty())
                ((Sprite) element).image = io.hamlook.aetheria.features.chat.globalchat.image.ImageManager.images.get(GCImage.createGCImageFromFile(path));
        }
        element.xPos = 0;
        element.yPos = 0;
        element.position = Position.absolute(0, 0);
        onAdd.accept(element);
        onCancel.run();
    }

    private int number(String value, int fallback) {
        try {
            return Math.max(1, Integer.parseInt(value));
        } catch (Exception e) {
            return fallback;
        }
    }

    @Override
    public boolean keyboardInput() {
        return true;
    }

    public void keyTyped(char c, int code) {
        text.keyTyped(c, code);
        extra.keyTyped(c, code);
        width.keyTyped(c, code);
        height.keyTyped(c, code);
    }
}
