package io.hamlook.aetheria.features.dungeons.rooms.report;

import io.hamlook.aetheria.features.dungeons.rooms.DungeonRoom;
import io.hamlook.aetheria.features.dungeons.rooms.DungeonRoomDetector;
import io.hamlook.aetheria.repo.CapeAPI;
import io.hamlook.aetheria.utils.KeybindHelper;
import io.hamlook.aetheria.utils.render.ResolutionUtils;
import io.hamlook.aetheria.utils.render.TextRenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class SecretReportGUI extends GuiScreen {

    public long lastReport = 0;
    public static final long REPORT_INTERVAL = 10000;
    public DungeonRoom room;
    public GuiTextField xField,zField,yField;
    public GuiButton submitButton,cancelButton;
    List<String> secretNames = new ArrayList<>();
    public GuiDropdownTextField searchField;

    public String errorMessage;

    public SecretReportGUI(DungeonRoom room) {
        this.room = room;
    }

    @Override
    public void initGui() {
        if(room == null) {
            Minecraft.getMinecraft().displayGuiScreen(null);
            return;
        }
        secretNames.clear();
        secretNames.addAll(DungeonRoomDetector.getSecretNamesForRoom(room.name));
        Keyboard.enableRepeatEvents(true);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if(button.id == cancelButton.id) Minecraft.getMinecraft().displayGuiScreen(null);
        if(button.id == submitButton.id) errorMessage = submitReport();
    }

    public String submitReport() {
        if(System.currentTimeMillis() - lastReport < REPORT_INTERVAL) return "§cYou cannot submit multiple reports this fast.";

        int xCoord = tryGet(xField.getText());
        int yCoord = tryGet(yField.getText());
        int zCoord = tryGet(zField.getText());

        if(xCoord == -1 || yCoord == -1 || zCoord == -1) {
            return "§cPlease put valid coordinates in the text boxs.";
        }
        if(searchField.getText().isEmpty()){
            return "§cPlease put a valid secret name.";
        }

        String jsonPayload = String.format(
                "{\"data\": {\"secret\": \"%s\", \"room\": \"%s\", \"coords\": {\"x\": %d, \"y\": %d, \"z\": %d}}}",
                searchField.getText(),
                room.name,
                xCoord,
                yCoord,
                zCoord
        );
        try {
            URL url = new URL(CapeAPI.getAPIUrl("/report-secret"));
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

            connection.addRequestProperty("Content-Type", "application/json");
            connection.addRequestProperty("User-Agent", "Java-8-Discord-Webhook");
            connection.setDoOutput(true);
            connection.setRequestProperty("x-mod-secret", CapeAPI.getModSecret());
            connection.setRequestMethod("POST");

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
                os.flush();
            }

            int responseCode = connection.getResponseCode();
            connection.disconnect();
            if (responseCode >= 200 && responseCode <= 300) {
                lastReport = System.currentTimeMillis();
                return "§aSuccessfully submitted report to mod devs!";
            } else {
                return "§cCould not send report to mod devs, Error: " + responseCode;
            }


        } catch (Exception e) {
            return "§cCould not send report to mod devs, Error: " + e.getMessage();
        }
    }

    private int tryGet(String text) {
        try{
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if(this.buttonList.isEmpty()){
            populateButtonList();
        }
        drawDefaultBackground();
        TextRenderUtils.drawCenteredStringScaleAware("X-Rel. Coord", xField.xPosition + xField.width/2f,
                xField.yPosition - 10,1f,false);

        TextRenderUtils.drawCenteredStringScaleAware("Y. Coordinate", yField.xPosition + yField.width/2f,
                yField.yPosition - 10,1f,false);

        TextRenderUtils.drawCenteredStringScaleAware("Z-Rel. Coordinate", zField.xPosition + zField.width/2f,
                zField.yPosition - 10,1f,false);

        TextRenderUtils.drawCenteredStringScaleAware("Secret Name", searchField.xPosition + searchField.width/2f,
                searchField.yPosition - 10,1f,false);
        TextRenderUtils.drawCenteredStringScaleAware(errorMessage,
                width/2f,searchField.yPosition - 22,1f,false);

        super.drawScreen(mouseX, mouseY, partialTicks);
        xField.drawTextBox();
        yField.drawTextBox();
        zField.drawTextBox();
        searchField.drawTextBox();
        searchField.drawDropdown(mouseX,mouseY);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (!KeybindHelper.isKeyValid(keyCode)) return;
        super.keyTyped(typedChar, keyCode);
        searchField.textboxKeyTyped(typedChar, keyCode);

        boolean isNumberRow = (keyCode >= Keyboard.KEY_1 && keyCode <= Keyboard.KEY_0);
        boolean isNumpad = (keyCode >= Keyboard.KEY_NUMPAD7 && keyCode <= Keyboard.KEY_NUMPAD0);

        if (isNumpad || isNumberRow || keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_BACK || keyCode == Keyboard.KEY_MINUS) {
            xField.textboxKeyTyped(typedChar, keyCode);
            yField.textboxKeyTyped(typedChar, keyCode);
            zField.textboxKeyTyped(typedChar, keyCode);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (searchField.mouseClickedDropdown(mouseX, mouseY, mouseButton)) {
            return;
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
        searchField.mouseClicked(mouseX, mouseY, mouseButton);
        xField.mouseClicked(mouseX, mouseY, mouseButton);
        yField.mouseClicked(mouseX, mouseY, mouseButton);
        zField.mouseClicked(mouseX, mouseY, mouseButton);
    }

    public void populateButtonList(){
        int xCenter = this.width/2;
        int yCenter = this.height/2;

        int yOffset = (int) ResolutionUtils.getYStatic(100);
        submitButton = new GuiButton(0,xCenter - 125,yCenter+yOffset,100,20,"Submit");
        cancelButton = new GuiButton(1,xCenter + 25,yCenter+yOffset,100,20,"Cancel");
        this.buttonList.add(submitButton);
        this.buttonList.add(cancelButton);
        searchField = new GuiDropdownTextField(5, mc.fontRendererObj, xCenter - 100, yCenter - 50, 200, 20, secretNames);

        xField = new GuiTextField(2,mc.fontRendererObj,xCenter - 175,yCenter,100,20);
        yField = new GuiTextField(3,mc.fontRendererObj,xCenter - 50, yCenter,100,20);
        zField = new GuiTextField(4,mc.fontRendererObj,xCenter + 75, yCenter,100,20);
    }
}
