package io.hamlook.aetheria.features.custommenu.ui.buttons.impl;

import io.hamlook.aetheria.features.custommenu.Position;
import io.hamlook.aetheria.features.custommenu.ui.buttons.CMMButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

public class ActionButton extends CMMButton {

    @Override
    public void onClick(GuiScreen screen) {
        switch (action){
            case EXIT:
                Minecraft.getMinecraft().shutdown();
                break;
        }
    }

    public enum Action {
        EXIT
    }

    public Action action;

    public ActionButton(int xPos, int yPos, String displayString, Action action) {
        super(xPos, yPos, displayString);
        this.action = action;
    }

    public ActionButton(int xPos, int yPos, int width, int height, String displayString, Action action) {
        super(xPos, yPos, width, height, displayString);
        this.action = action;
    }

    public ActionButton(Position position, int width, int height, String displayString, Action action) {
        super(position, width, height, displayString);
        this.action = action;
    }
}
