package io.hamlook.aetheria.features.custommenu.ui.buttons.impl;

import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.features.custommenu.Position;
import io.hamlook.aetheria.features.custommenu.ui.buttons.CMMButton;
import io.hamlook.aetheria.features.custommenu.util.GuiHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

public class GuiButton extends CMMButton {

    public String screen;
    public GuiButton(int xPos, int yPos, String displayString, String screen) {
        super(xPos, yPos, displayString);
        this.screen = screen;
    }
    public GuiButton(int xPos, int yPos,int width,int height, String displayString,String screen) {
        super(xPos, yPos,width,height, displayString);
        this.screen = screen;
    }

    public GuiButton(Position position, int width, int height, String displayString, String screen) {
        super(position, width, height, displayString);
        this.screen = screen;
    }

    @Override
    public void onClick(GuiScreen screen) {
        GuiScreen toOpen = GuiHelper.getMenu(this.screen,screen);
        if(toOpen == null){
            this.displayString = "§cCould not Find GUI: " + this.screen;
            return;
        }
        Aetheria.logger.info("Opening GUI " + this.screen);
        Minecraft.getMinecraft().displayGuiScreen(toOpen);
    }
}
