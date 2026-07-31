package io.hamlook.aetheria.features.chat.globalchat.ui;

import net.minecraft.client.gui.GuiScreen;

import java.awt.Desktop;
import java.net.URI;

/** Small confirmation overlay shown before opening an external link from a chat message. */
public class LinkConfirmScreen extends GuiScreen {

    private final String url;

    public LinkConfirmScreen(String url) {
        this.url = url;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawRect(0, 0, width, height, 0x99000000);

        int bw = 320, bh = 110;
        int bx = (width - bw) / 2, by = (height - bh) / 2;
        drawRect(bx, by, bx + bw, by + bh, 0xFF313338);
        drawRect(bx, by, bx + bw, by + 2, 0xFF5865F2);

        drawCenteredString(fontRendererObj, "Open this link?", bx + bw / 2, by + 18, 0xFFFFFFFF);
        drawCenteredString(fontRendererObj, fontRendererObj.trimStringToWidth(url, bw - 24), bx + bw / 2, by + 38, 0xFF949BA4);
        drawCenteredString(fontRendererObj, "(it will open in your browser)", bx + bw / 2, by + 50, 0xFF6D6F78);

        int yw = 90, yh = 20;
        int yesX = bx + bw / 2 - yw - 4, noX = bx + bw / 2 + 4, byy = by + bh - 32;
        boolean yesH = mouseX >= yesX && mouseX <= yesX + yw && mouseY >= byy && mouseY <= byy + yh;
        boolean noH = mouseX >= noX && mouseX <= noX + yw && mouseY >= byy && mouseY <= byy + yh;

        drawRect(yesX, byy, yesX + yw, byy + yh, yesH ? 0xFF5865F2 : 0xFF404249);
        drawRect(noX, byy, noX + yw, byy + yh, noH ? 0xFF35373C : 0xFF2B2D31);
        drawCenteredString(fontRendererObj, "Yes", yesX + yw / 2, byy + 6, 0xFFFFFFFF);
        drawCenteredString(fontRendererObj, "No", noX + yw / 2, byy + 6, 0xFFFFFFFF);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) return;
        int bw = 320, bh = 110;
        int bx = (width - bw) / 2, by = (height - bh) / 2;
        int yw = 90, yh = 20;
        int yesX = bx + bw / 2 - yw - 4, noX = bx + bw / 2 + 4, byy = by + bh - 32;
        if (mouseX >= yesX && mouseX <= yesX + yw && mouseY >= byy && mouseY <= byy + yh) {
            openBrowser();
            mc.displayGuiScreen(null);
        } else if (mouseX >= noX && mouseX <= noX + yw && mouseY >= byy && mouseY <= byy + yh) {
            mc.displayGuiScreen(null);
        }
    }

    private void openBrowser() {
        new Thread(() -> {
            try {
                Desktop.getDesktop().browse(new URI(url));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "Link-Open").start();
    }
}
